/**********************************************************************************************
Copyright (C) 2021 Norbert Truchsess norbert.truchsess@t-online.de

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **********************************************************************************************/

package me.hufman.androidautoidrive.carapp.evplanning

import android.content.Context
import android.os.Handler
import android.util.Log
import com.google.gson.Gson
import de.bmw.idrive.BMWRemoting
import de.bmw.idrive.BMWRemotingServer
import de.bmw.idrive.BaseBMWRemotingClient
import me.hufman.androidautoidrive.*
import me.hufman.androidautoidrive.carapp.*
import me.hufman.androidautoidrive.carapp.evplanning.views.*
import me.hufman.androidautoidrive.utils.GraphicsHelpers
import me.hufman.androidautoidrive.utils.Utils
import me.hufman.androidautoidrive.utils.removeFirst
import me.hufman.idriveconnectionkit.CDS
import me.hufman.idriveconnectionkit.IDriveConnection
import me.hufman.idriveconnectionkit.rhmi.RHMIApplicationIdempotent
import me.hufman.idriveconnectionkit.rhmi.RHMIApplicationSynchronized
import me.hufman.idriveconnectionkit.android.CarAppResources
import me.hufman.idriveconnectionkit.android.IDriveConnectionStatus
import me.hufman.idriveconnectionkit.android.security.SecurityAccess
import me.hufman.idriveconnectionkit.rhmi.*
import java.lang.RuntimeException
import java.util.*

const val TAG = "EVPlanning"
const val HMI_CONTEXT_THRESHOLD = 5000L

data class Position(val latitude: Double = Double.NaN, val longitude: Double = Double.NaN) {
	fun isValid(): Boolean {
		// the car sends 0.0/0.0 if a destination is chosen but routing not yet activated
		return !(latitude.isNaN() || longitude.isNaN() || (latitude == 0.0 && longitude == 0.0))
	}
}

interface CarDataListenerRaw {

	fun onPositionChanged(position: Position)
	fun onNextDestinationChanged(position: Position)
	fun onFinalDestinationChanged(position: Position)
	fun onSOCChanged(soc: Double)
	fun onDrivingModeChanged(drivingMode: Int)
	fun onOdometerChanged(odometer: Int)
	fun onSpeedChanged(speed: Int)
	fun onTorqueChanged(torque: Int)
	fun onBatteryTemperatureChanged(batteryTemperature: Int)
	fun onInternalTemperatureChanged(internalTemperature: Int)
	fun onExternalTemperatureChanged(externalTemperature: Int)
}

class EVPlanningApplication(val iDriveConnectionStatus: IDriveConnectionStatus, val securityAccess: SecurityAccess, val carAppAssets: CarAppResources, val carAppAssetsIcons: CarAppResources, val phoneAppResources: PhoneAppResources, val graphicsHelpers: GraphicsHelpers, private val cardataListenerRaw: CarDataListenerRaw, val settings: EVPlanningSettings, val navigationController: NavigationController) {
	var handler: Handler? = null
	val carappListener: CarAppListener
	var rhmiHandle: Int = -1
	val carConnection: BMWRemotingServer
	val carAppSwappable: RHMIApplicationSwappable
	val carApp: RHMIApplicationSynchronized
	val amHandle: Int
	val focusTriggerController: FocusTriggerController
	val focusedStateTracker = FocusedStateTracker()
	var hmiContextChangedTime = 0L
	var hmiContextWidgetType: String = ""

	// carapp views
	val viewRoutesList: RoutesListView      // show a list of active notifications
	val viewWaypointList: WaypointsListView      // show a list of active notifications
	val viewDetails: DetailsView            // view a notification with actions to do
//	val stateInput: RHMIState.PlainState    // show a reply input form

	// to suppress redundant calls to the listener car data being received from the car is cached to detect changes
	var drivingMode: Int = 0
	var odometer: Int = 0
	var speed: Int = 0
	var torque: Int = 0
	var position: Position = Position(0.0, 0.0)
	var nextDestination: Position = Position(0.0, 0.0)
	var finalDestination: Position = Position(0.0, 0.0)
	var batteryTemperature: Int = Int.MIN_VALUE
	var socBattery: Double = 0.0
	var externalTemperature: Int = Int.MIN_VALUE
	var internalTemperature: Int = Int.MIN_VALUE

	val carAppImages: Map<String, ByteArray>

	init {
		val cdsData = CDSDataProvider()
		carappListener = CarAppListener(cdsData)
		carConnection = IDriveConnection.getEtchConnection(iDriveConnectionStatus.host
				?: "127.0.0.1", iDriveConnectionStatus.port ?: 8003, carappListener)
		val appCert = carAppAssets.getAppCertificate(iDriveConnectionStatus.brand
				?: "")?.readBytes() as ByteArray
		val sas_challenge = carConnection.sas_certificate(appCert)
		val sas_login = securityAccess.signChallenge(challenge = sas_challenge)
		carConnection.sas_login(sas_login)
		carappListener.server = carConnection

		// set up the app in the car
		// synchronized to ensure that events happen after we are done
		synchronized(carConnection) {
			carAppSwappable = RHMIApplicationSwappable(createRhmiApp())
			carApp = RHMIApplicationSynchronized(carAppSwappable, carConnection)
			carappListener.app = carApp
			carApp.loadFromXML(carAppAssets.getUiDescription()?.readBytes() as ByteArray)

			val focusEvent = carApp.events.values.filterIsInstance<RHMIEvent.FocusEvent>().first()
			focusTriggerController = FocusTriggerController(focusEvent) {
				recreateRhmiApp()
			}

//			val notificationIconEvent = carApp.events.values.filterIsInstance<RHMIEvent.NotificationIconEvent>().first()

			val unclaimedStates = LinkedList(carApp.states.values)

			carAppImages = Utils.loadZipfile(carAppAssetsIcons.getImagesDB(iDriveConnectionStatus.brand ?: "common"))

			// figure out which views to use
			viewRoutesList = RoutesListView(unclaimedStates.removeFirst { RoutesListView.fits(it) }, graphicsHelpers, settings, focusTriggerController, navigationController.navigationModel, carAppImages)
			viewWaypointList = WaypointsListView(unclaimedStates.removeFirst { WaypointsListView.fits(it) }, graphicsHelpers, settings, focusTriggerController, navigationController.navigationModel, carAppImages)
			viewDetails = DetailsView(unclaimedStates.removeFirst { DetailsView.fits(it) }, phoneAppResources, graphicsHelpers, settings, focusTriggerController, navigationController.navigationModel, carAppImages)

//			stateInput = carApp.states.values.filterIsInstance<RHMIState.PlainState>().first {
//				it.componentsList.filterIsInstance<RHMIComponent.Input>().isNotEmpty()
//			}

			carApp.components.values.filterIsInstance<RHMIComponent.EntryButton>().forEach {
				it.getAction()?.asHMIAction()?.getTargetModel()?.asRaIntModel()?.value = viewWaypointList.state.id
				it.getAction()?.asRAAction()?.rhmiActionCallback = RHMIActionButtonCallback {
					viewWaypointList.entryButtonTimestamp = System.currentTimeMillis()
				}
			}

			amHandle = carConnection.am_create("0", "\u0000\u0000\u0000\u0000\u0000\u0002\u0000\u0000".toByteArray())
			carConnection.am_addAppEventHandler(amHandle, "me.hufman.androidautoidrive.evplanning")
			// set up the AM icon in the Navigation section
			createAmApp()

			// set up the lists
			viewRoutesList.initWidgets()
			viewWaypointList.initWidgets()

			// set up the details view
			viewDetails.initWidgets(viewWaypointList) //, stateInput)

			// subscribe to CDS for passenger seat info
			cdsData.setConnection(CDSConnectionEtch(carConnection))
			cdsData.subscriptions.defaultIntervalLimit = 2200

			cdsData.subscriptions[CDS.DRIVING.MODE] = {
				it["mode"]?.asInt?.let { drivingMode ->
					if (drivingMode != this.drivingMode) {
						this.drivingMode = drivingMode
						cardataListenerRaw.onDrivingModeChanged(drivingMode)
					}
				}
			}
			cdsData.subscriptions[CDS.DRIVING.ODOMETER] = {
				it["odometer"]?.asInt?.let { odometer ->
					if (odometer != this.odometer) {
						this.odometer = odometer
						cardataListenerRaw.onOdometerChanged(odometer)
					}
				}
			}
			cdsData.subscriptions[CDS.DRIVING.SPEEDACTUAL] = {
				it["speedActual"]?.asInt?.let { speed ->
					if (speed != this.speed) {
						this.speed = speed
						cardataListenerRaw.onSpeedChanged(speed)
					}
				}
			}
			cdsData.subscriptions[CDS.ENGINE.TORQUE] = {
				it["torque"]?.asInt?.let { torque ->
					if (torque != this.torque) {
						this.torque = torque
						cardataListenerRaw.onTorqueChanged(torque)
					}
				}
			}
//			cdsData.subscriptions[CDS.NAVIGATION.GPSEXTENDEDINFO] = {
//				val gpsExtendedInfo = it["GPSExtendedInfo"]?.asJsonObject
//			}
			cdsData.subscriptions[CDS.NAVIGATION.GPSPOSITION] = {
				it["GPSPosition"]?.asJsonObject?.let { dest ->
					Gson().fromJson(dest, Position::class.java)
				}?.let { position ->
					if (position != this.position) {
						this.position = position
						cardataListenerRaw.onPositionChanged(position)
					}
				}
			}
			cdsData.subscriptions[CDS.NAVIGATION.NEXTDESTINATION] = {
				it["nextDestination"]?.let { dest ->
					Gson().fromJson(dest, Position::class.java)
				}?.let { destination ->
					if (destination != nextDestination) {
						nextDestination = destination
						cardataListenerRaw.onNextDestinationChanged(destination)
					}
				}
			}
			cdsData.subscriptions[CDS.NAVIGATION.FINALDESTINATION] = {
				it["finalDestination"]?.let { dest ->
					Gson().fromJson(dest, Position::class.java)
				}?.let { destination ->
					if (destination != finalDestination) {
						finalDestination = destination
						cardataListenerRaw.onFinalDestinationChanged(destination)
					}
				}
			}
			cdsData.subscriptions[CDS.SENSORS.BATTERYTEMP] = {
				it["batteryTemp"]?.asInt?.let { temperature ->
					if (temperature != batteryTemperature) {
						batteryTemperature = temperature
						cardataListenerRaw.onBatteryTemperatureChanged(temperature)
					}
				}
			}
			cdsData.subscriptions[CDS.SENSORS.SOCBATTERYHYBRID] = {
				it["SOCBatteryHybrid"]?.asDouble?.let { soc ->
					if (soc != socBattery) {
						socBattery = soc
						cardataListenerRaw.onSOCChanged(soc)
					}
				}
			}
			cdsData.subscriptions[CDS.SENSORS.TEMPERATUREEXTERIOR] = {
				it["temperatureExterior"]?.asInt?.let { temperature ->
					if (temperature != externalTemperature) {
						externalTemperature = temperature
						cardataListenerRaw.onExternalTemperatureChanged(temperature)
					}
				}
			}
			cdsData.subscriptions[CDS.SENSORS.TEMPERATUREINTERIOR] = {
				it["temperatureInterior"]?.asInt?.let { temperature ->
					if (temperature != internalTemperature) {
						internalTemperature = temperature
						cardataListenerRaw.onInternalTemperatureChanged(temperature)
					}
				}
			}
			try {
				// not sure if this works for id4
				cdsData.subscriptions[CDS.HMI.GRAPHICALCONTEXT] = {
					hmiContextChangedTime = System.currentTimeMillis()
					if (it.has("graphicalContext")) {
						val graphicalContext = it.getAsJsonObject("graphicalContext")
						if (graphicalContext.has("widgetType")) {
							hmiContextWidgetType = graphicalContext.getAsJsonPrimitive("widgetType").asString
						}
					}
				}
			} catch (e: BMWRemoting.ServiceException) {
			}
		}

		with(navigationController) {

			navigationModel.displayRoutesObserver = {
				viewRoutesList.redrawRoutes()
			}

			navigationModel.selectedRouteObserver = {
				viewWaypointList.redrawWaypoints()
			}

			navigationModel.selectedWaypointObserver = {
				viewDetails.redraw()
			}
		}

		viewRoutesList.onRoutesListClicked = {
			if (navigationController.selectRoute(it)) {
				showWaypointsViewFromListAction()
			} else {
				hideWaypointsViewFromListAction()
			}
		}

		viewWaypointList.onWaypointListClicked = {
			if (navigationController.selectWaypoint(it)) {
				showDetailsViewFromListAction()
			} else {
				hideDetailsViewFromListAction()
			}
		}

		viewDetails.onAddressClicked = {
			navigationController.navigateToWaypoint()
		}
	}

	/**
	 * Check if we should recreate the app
	 * Is called from within an HMI focused event handler,
	 * so sleeping here is inside a background thread
	 * */
	fun checkRecreate() {
		val interval = 500
		val waitDelay = 10000
		if (focusTriggerController.hasFocusedState && focusedStateTracker.getFocused() == null) {
			for (i in 0..waitDelay step interval) {
				Thread.sleep(500)
				if (focusedStateTracker.getFocused() != null) {
					return
				}
			}
			// waited the entire time without getting focused, recreate
			recreateRhmiApp()
		}
	}

	/** creates the app in the car */
	fun createRhmiApp(): RHMIApplication {
		// load the resources
		rhmiHandle = carConnection.rhmi_create(null, BMWRemoting.RHMIMetaData("me.hufman.androidautoidrive.evplanning", BMWRemoting.VersionInfo(0, 1, 0), "me.hufman.androidautoidrive.evplanning", "me.hufman"))
		RHMIUtils.rhmi_setResourceCached(carConnection, rhmiHandle, BMWRemoting.RHMIResourceType.DESCRIPTION, carAppAssets.getUiDescription())
		RHMIUtils.rhmi_setResourceCached(carConnection, rhmiHandle, BMWRemoting.RHMIResourceType.TEXTDB, carAppAssets.getTextsDB(iDriveConnectionStatus.brand
				?: "common"))
		RHMIUtils.rhmi_setResourceCached(carConnection, rhmiHandle, BMWRemoting.RHMIResourceType.IMAGEDB, carAppAssets.getImagesDB(iDriveConnectionStatus.brand
				?: "common"))
		carConnection.rhmi_initialize(rhmiHandle)

		// register for events from the car
		carConnection.rhmi_addActionEventHandler(rhmiHandle, "me.hufman.androidautoidrive.evplanning", -1)
		carConnection.rhmi_addHmiEventHandler(rhmiHandle, "me.hufman.androidautoidrive.evplanning", -1, -1)

		return RHMIApplicationIdempotent(RHMIApplicationEtch(carConnection, rhmiHandle))
	}

	/** Recreates the RHMI app in the car */
	fun recreateRhmiApp() {
		synchronized(carConnection) {
			// pause events to the underlying connection
			carAppSwappable.isConnected = false
			// destroy the previous RHMI app
			carConnection.rhmi_dispose(rhmiHandle)
			// create a new one
			carAppSwappable.app = createRhmiApp()
			// clear FocusTriggerController because of the new rhmi app
			focusTriggerController.hasFocusedState = false
			// reconnect, triggering a sync down to the new RHMI Etch app
			carAppSwappable.isConnected = true
		}
	}

	fun createAmApp() {
		val name = L.EVPLANNING_TITLE
		val amInfo = mutableMapOf<Int, Any>(
				0 to 145,   // basecore version
				1 to name,  // app name
				2 to (carAppImages["153.png"] ?: ""),
				3 to AMCategory.NAVIGATION.value,   // section
				4 to true,
				5 to 800,   // weight
				8 to viewRoutesList.state.id  // mainstateId
		)
		// language translations, dunno which one is which
		for (languageCode in 101..123) {
			amInfo[languageCode] = name
		}

		synchronized(carConnection) {
			carConnection.am_registerApp(amHandle, "androidautoidrive.evplanning", amInfo)
		}
	}

	inner class CarAppListener(val cdsEventHandler: CDSEventHandler) : BaseBMWRemotingClient() {
		var server: BMWRemotingServer? = null
		var app: RHMIApplication? = null

		fun synced() {
			synchronized(server!!) {
				// the RHMI was definitely initialized, we can continue
			}
		}

		override fun am_onAppEvent(handle: Int?, ident: String?, appId: String?, event: BMWRemoting.AMEvent?) {
			synced()
			viewWaypointList.entryButtonTimestamp = System.currentTimeMillis()
			focusTriggerController.focusState(viewRoutesList.state, true)
			createAmApp()
		}

		override fun rhmi_onActionEvent(handle: Int?, ident: String?, actionId: Int?, args: MutableMap<*, *>?) {
			Log.w(TAG, "Received rhmi_onActionEvent: handle=$handle ident=$ident actionId=$actionId")
			synced()
			try {
				app?.actions?.get(actionId)?.asRAAction()?.rhmiActionCallback?.onActionEvent(args)
				synchronized(server!!) {
					server?.rhmi_ackActionEvent(handle, actionId, 1, true)
				}
			} catch (e: RHMIActionAbort) {
				// Action handler requested that we don't claim success
				synchronized(server!!) {
					server?.rhmi_ackActionEvent(handle, actionId, 1, false)
				}
			} catch (e: Exception) {
				Log.e(TAG, "Exception while calling onActionEvent handler! $e")
				synchronized(server!!) {
					server?.rhmi_ackActionEvent(handle, actionId, 1, true)
				}
			}
		}

		override fun rhmi_onHmiEvent(handle: Int?, ident: String?, componentId: Int?, eventId: Int?, args: MutableMap<*, *>?) {
			val msg = "Received rhmi_onHmiEvent: handle=$handle ident=$ident componentId=$componentId eventId=$eventId args=${args?.toString()}"
			Log.w(TAG, msg)
			synced()

			val state = app?.states?.get(componentId)
			state?.onHmiEvent(eventId, args)

			if (state != null && eventId == 1) {
				val focused = args?.get(4.toByte()) as? Boolean ?: false
				focusedStateTracker.onFocus(state.id, focused)
				checkRecreate()
			}

			val component = app?.components?.get(componentId)
			component?.onHmiEvent(eventId, args)
		}

		override fun cds_onPropertyChangedEvent(handle: Int?, ident: String?, propertyName: String?, propertyValue: String?) {
			cdsEventHandler.onPropertyChangedEvent(ident, propertyValue)
		}
	}

	fun onCreate(context: Context, handler: Handler) {
		this.handler = handler
		viewRoutesList.onCreate(handler)
		viewWaypointList.onCreate(handler)
	}

	fun onDestroy(context: Context) {
		handler = null
	}

	fun disconnect() {
		try {
			Log.i(TAG, "Trying to shut down etch connection")
			IDriveConnection.disconnectEtchConnection(carConnection)
		} catch (e: java.io.IOError) {
		} catch (e: RuntimeException) {
		}
	}

	fun showWaypointsViewFromListAction() {
		viewRoutesList.routesList.getAction()?.asHMIAction()?.getTargetModel()?.asRaIntModel()?.value = viewWaypointList.state.id
	}

	fun hideWaypointsViewFromListAction() {
		viewRoutesList.routesList.getAction()?.asHMIAction()?.getTargetModel()?.asRaIntModel()?.value = 0
	}

	fun showDetailsViewFromListAction() {
		viewWaypointList.waypointsList.getAction()?.asHMIAction()?.getTargetModel()?.asRaIntModel()?.value = viewDetails.state.id
	}

	fun hideDetailsViewFromListAction() {
		viewWaypointList.waypointsList.getAction()?.asHMIAction()?.getTargetModel()?.asRaIntModel()?.value = 0
	}
}