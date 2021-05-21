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

package me.hufman.androidautoidrive

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import io.sentry.Sentry
import me.hufman.androidautoidrive.connections.BtStatus
import me.hufman.androidautoidrive.evplanning.RoutingService
import me.hufman.androidautoidrive.evplanning.RoutingServiceUpdater
import me.hufman.androidautoidrive.evplanning.iternio.PlanningImpl
import me.hufman.androidautoidrive.carapp.evplanning.*
import me.hufman.androidautoidrive.utils.GraphicsHelpersAndroid
import me.hufman.idriveconnectionkit.android.IDriveConnectionStatus
import me.hufman.idriveconnectionkit.android.security.SecurityAccess
import java.lang.Exception

const val AUTHBASE="APIKEY "
const val BASEURL="https://api.iternio.com/"

class EVPlanningService(val context: Context, val iDriveConnectionStatus: IDriveConnectionStatus, val securityAccess: SecurityAccess, val carInformationObserver: CarInformationObserver) {

	// carApplication runs in threadCarApp and uses navigationModel and navigationController
	// navigationModel can be updated from other threads through navigationModelUpdater
	var threadCarApp: CarThread? = null
	var carApplication: EVPlanningApplication? = null

	// routingService runs in threadRouting and can be updated from other threads through routingServiceUpdater
	var threadRouting: CarThread? = null
	var routingService: RoutingService? = null

	var running = false

	// the updaters do the inter-thread-communication and are therefore created once and before creation and start of other threads
	// routingServiceUpdater receives raw CarData from the carApplication and posts this to routingService on the threadRouting thread
	val routingServiceUpdater = RoutingServiceUpdater(1000)
	// navigationModelUpdater receives new RoutingData from routingService and posts this to carApplication on the threadCarApp thread
	val navigationModelUpdater = NavigationModelUpdater()

	fun start(): Boolean {
		if (AppSettings[AppSettings.KEYS.ENABLED_EVPLANNING].toBoolean()) {
			try {
				running = true
				synchronized(this) {
					if (carInformationObserver.capabilities.isNotEmpty()) {
						if (threadCarApp?.isAlive != true) {
							threadCarApp = CarThread("EVPlanning-carapp") {
								try {
									Log.i(MainService.TAG, "Starting EVPlanning carapp")
									val handler = threadCarApp?.handler
									if (handler == null) {
										Log.e(MainService.TAG, "CarThread Handler is null?")
									} else {
										val settings = EVPlanningSettings(carInformationObserver.capabilities, BtStatus(context) {}, MutableAppSettingsReceiver(context, handler))
										settings.btStatus.register()

										// both navigationModel and navigationController shall only be synchronously accessed within carAppThread:
										val navigationModel = NavigationModel()
										val navigationController = NavigationController(context, navigationModel)
										// other threads use navigationModelUpdater for asynchronous updates
										navigationModelUpdater.navigationModel = navigationModel

										carApplication = EVPlanningApplication(iDriveConnectionStatus, securityAccess,
												CarAppAssetManager(context, "basecoreOnlineServices"),
												CarAppAssetManager(context, "bmwone"),
												PhoneAppResourcesAndroid(context),
												GraphicsHelpersAndroid(),
												routingServiceUpdater.rawCarDataListener,
												settings, navigationController)

										carApplication?.onCreate(context, handler)
										// routingServiceUpdater will post aggregated CarData to routingService once a second.
										// as this scheduler is running on the threadCarApp it must be triggered for the first
										// time when the thread is already started
										routingServiceUpdater.triggerNextUpdate()
									}
								} catch (t: Throwable) {
									Sentry.capture(t)
								}
							}
							// navigationModelUpdater needs a reference to threadCarApp to post async updates to the model on this thread
							navigationModelUpdater.onCreateCarApp(threadCarApp)
							// routingServiceUpdater needs a reference to threadCarApp to schedule the once per second updates on this thread
							// (the update itself is then done asynchronously by posting on threadRouting
							routingServiceUpdater.onCreateCarApp(threadCarApp)
							threadCarApp?.start()
						}
						if (threadRouting?.isAlive != true) {
							threadRouting = CarThread("EVPlanning-routing") {
								try {
									Log.i(MainService.TAG, "Starting EVPlanning routing")
									val handler = threadRouting?.handler
									if (handler == null) {
										Log.e(MainService.TAG, "CarThread Handler is null?")
									} else {
										val authorization = AUTHBASE + context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
												.metaData.getString("com.iternio.planning.API_KEY")
										routingService = RoutingService(PlanningImpl(BASEURL, authorization), navigationModelUpdater.routingDataListener)
										routingService?.onCreate(context, handler)
										routingServiceUpdater.registerRoutingService(routingService)
									}
								} catch (t: Throwable) {
									Sentry.capture(t)
								}
							}
							// routingServiceUpdater needs a reference to threadRouting to post asynchronous updates to routingService on it.
							routingServiceUpdater.onCreateRouting(threadRouting)
							threadRouting?.start()
						}
					}
				}
			} catch (t: Throwable) {
				Sentry.capture(t)
			}
			return true
		} else {    // we should not run the service
			if (threadCarApp != null || threadRouting != null) {
				Log.i(MainService.TAG, "EVPlanning app needs to be shut down...")
				stop()
			}
			return false
		}
	}

	fun stop() {
		running = false
		// post it to the thread to run after initialization finishes
		threadCarApp?.post {
			routingServiceUpdater.onDestroy()
			carApplication?.onDestroy(context)
			carApplication?.disconnect()
			carApplication = null
			threadCarApp?.quit()
			threadCarApp = null

			// if we started up again during shutdown
			if (running) {
				start()
			}
		}

		threadRouting?.post {
			navigationModelUpdater.onDestroy()
			routingService?.onDestroy(context)
			threadRouting?.quit()
			threadRouting = null
			if (running) {
				start()
			}
		}

		// unregister in the main thread
		// when the car disconnects, the threadNotifications handler shuts down
		try {
			carApplication?.settings?.btStatus?.unregister()
			carApplication?.onDestroy(context)
		} catch (e: Exception) {
			Log.w(TAG, "Encountered an exception while shutting down", e)
		}
	}
}