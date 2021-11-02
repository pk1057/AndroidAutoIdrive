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

package me.hufman.androidautoidrive.carapp.evplanning.views

import android.os.Handler
import android.util.Log
import de.bmw.idrive.BMWRemoting
import io.bimmergestalt.idriveconnectkit.rhmi.*
import io.sentry.Sentry
import me.hufman.androidautoidrive.AppSettings
import me.hufman.androidautoidrive.DeferredUpdate
import me.hufman.androidautoidrive.carapp.FocusTriggerController
import me.hufman.androidautoidrive.carapp.L
import me.hufman.androidautoidrive.carapp.RHMIActionAbort
import me.hufman.androidautoidrive.carapp.evplanning.DisplayRoute
import me.hufman.androidautoidrive.carapp.evplanning.EVPlanningSettings
import me.hufman.androidautoidrive.carapp.evplanning.NavigationModel
import me.hufman.androidautoidrive.carapp.evplanning.NavigationModelUpdater.Companion.formatDistance
import me.hufman.androidautoidrive.carapp.evplanning.NavigationModelUpdater.Companion.formatDistanceDetailed
import me.hufman.androidautoidrive.carapp.evplanning.NavigationModelUpdater.Companion.formatTime
import me.hufman.androidautoidrive.carapp.evplanning.TAG
import me.hufman.androidautoidrive.evplanning.RouteData.Companion.MAX_STEP_OFFSET
import me.hufman.androidautoidrive.utils.GraphicsHelpers
import kotlin.reflect.KClass

class RoutesListView(val state: RHMIState, val graphicsHelpers: GraphicsHelpers, val settings: EVPlanningSettings,
                     val focusTriggerController: FocusTriggerController, val navigationModel: NavigationModel, val carAppImages: Map<String, ByteArray>) {

	companion object {
		const val INTERACTION_DEBOUNCE_MS = 2000              // how long to wait after lastInteractionTime to update the list
		const val SKIPTHROUGH_THRESHOLD = 2000                // how long after an entrybutton push to allow skipping through to a current notification
		const val ARRIVAL_THRESHOLD = 8000                    // how long after a new notification should it skip through

		val required = listOf(
				RHMIComponent.List::class,
				RHMIComponent.Label::class,
				RHMIComponent.List::class,
				RHMIComponent.Label::class,
				RHMIComponent.List::class,
		)

		fun fits(state: RHMIState): Boolean {
			return state.componentsList.fold(
					required.toMutableList(),
					{ acc, comp ->
						if (acc.firstOrNull()?.isInstance(comp) == true) {
							acc.removeFirst()
						}
						acc
					}
			).isEmpty()
		}

		const val IMAGEID_CHECKMARK = 150
	}

	val routesList: RHMIComponent.List
	val actionsLabel: RHMIComponent.Label
	val actionsList: RHMIComponent.List
	val settingsLabel: RHMIComponent.Label
	val settingsList: RHMIComponent.List

	lateinit var inputView: RHMIState

	// handlers shall return true if view is changed
	var onRoutesListClicked: ((Int) -> Unit)? = null
	var onActionPlanClicked: (() -> Unit)? = null
	var onSettingClicked: ((AppSettings.KEYS) -> Unit)? = null

	var visible = false                 // whether the notification list is showing

	var entryButtonTimestamp = 0L   // when the user pushed the entryButton
	val timeSinceEntryButton: Long
		get() = System.currentTimeMillis() - entryButtonTimestamp

	var deferredUpdate: DeferredUpdate? = null  // wrapper object to help debounce user inputs
	var lastInteractionIndex: Int = -1       // what index the user last selected

	val emptyListData = RHMIModel.RaListModel.RHMIListConcrete(3).apply {
		addRow(arrayOf("", L.EVPLANNING_EMPTY_LIST, ""))
	}

	val iconFlag: ByteArray?

	val actions = listOf(L.EVPLANNING_ACTION_PLAN )

	val actionsListData = object : RHMIModel.RaListModel.RHMIListAdapter<String>(3, actions ) {
		override fun convertRow(index: Int, item: String): Array<Any> {
			return arrayOf("","",item)
		}
	}

	val settingsListData = object : RHMIModel.RaListModel.RHMIListAdapter<AppSettings.KEYS>(5, settings.getSettings()) {
		override fun convertRow(index: Int, item: AppSettings.KEYS): Array<Any> {
			val isString = settings.isStringSetting(item)
			val value = if (isString) { settings.getStringSetting(item) } else ""
			val checkmark = if (!isString && settings.isChecked(item)) {
				BMWRemoting.RHMIResourceIdentifier(BMWRemoting.RHMIResourceType.IMAGEID, IMAGEID_CHECKMARK)
			} else ""
			val name = when (item) {
				AppSettings.KEYS.EVPLANNING_AUTO_REPLAN -> L.EVPLANNING_AUTO_REPLAN_ENABLE
				AppSettings.KEYS.EVPLANNING_MAXSPEED_DRIVEMODE_ENABLE -> L.EVPLANNING_MAX_SPEED_DRIVEMODE_ENABLE
				AppSettings.KEYS.EVPLANNING_MAXSPEED -> L.EVPLANNING_MAX_SPEED
				AppSettings.KEYS.EVPLANNING_MAXSPEED_COMFORT -> L.EVPLANNING_MAX_SPEED_COMFORT
				AppSettings.KEYS.EVPLANNING_MAXSPEED_ECO_PRO -> L.EVPLANNING_MAX_SPEED_ECO_PRO
				AppSettings.KEYS.EVPLANNING_MAXSPEED_ECO_PRO_PLUS -> L.EVPLANNING_MAX_SPEED_ECO_PRO_PLUS
				AppSettings.KEYS.EVPLANNING_MAXSPEED_SPORT -> L.EVPLANNING_MAX_SPEED_SPORT
				AppSettings.KEYS.EVPLANNING_REFERENCE_CONSUMPTION -> L.EVPLANNING_REFERENCE_CONSUMPTION
				AppSettings.KEYS.EVPLANNING_MIN_SOC_CHARGER -> L.EVPLANNING_MIN_SOC_CHARGER
				AppSettings.KEYS.EVPLANNING_MIN_SOC_FINAL -> L.EVPLANNING_MIN_SOC_FINAL
				else -> ""
			}
			return arrayOf(checkmark, "", name, "", value)
		}
	}

	init {
		class Acc(val result: MutableList<RHMIComponent>, val req: MutableList<KClass<out RHMIComponent>>)
		val components = state.componentsList.fold(
				Acc(mutableListOf(), required.toMutableList()),
				{ acc, comp ->
					if (acc.req.firstOrNull()?.isInstance(comp) == true) {
						acc.req.removeFirst()
						acc.result.add(comp)
					}
					acc
				}
		).result
		routesList = components.removeFirst() as RHMIComponent.List
		actionsLabel = components.removeFirst() as RHMIComponent.Label
		actionsList = components.removeFirst() as RHMIComponent.List
		settingsLabel = components.removeFirst() as RHMIComponent.Label
		settingsList = components.removeFirst() as RHMIComponent.List

		iconFlag = carAppImages["153.png"]
	}

	fun initWidgets() { // showNavigationEntryController: ShowNavigationEntryController) {

		// refresh the list when we are displayed
		state.focusCallback = FocusCallback { focused ->
			visible = focused
			if (focused) {
				val didEntryButton = timeSinceEntryButton < SKIPTHROUGH_THRESHOLD
				// if we did not skip through, refresh:
				redrawRoutes()

				// if a notification is speaking, pre-select it
				// otherwise pre-select the most recent notification that showed up or was selected
				// and only if the user is freshly arriving, not backing out of a deeper view
				val index = 0
				if (didEntryButton && index >= 0) {
					focusTriggerController.focusComponent(routesList, index)
				}

				redrawSettingsList()
				settings.callback = {
					redrawSettingsList()
				}

				redrawActionsList()
			} else {
				settings.callback = null
			}
		}

		state.getTextModel()?.asRaDataModel()?.value = L.EVPLANNING_TITLE_ROUTES
		state.setProperty(RHMIProperty.PropertyId.HMISTATE_TABLETYPE, 3)
		state.componentsList.forEach { it.setVisible(false) }

		routesList.apply {
			routesList.setVisible(true)
			routesList.setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH.id, "55,0,*")
			routesList.setProperty(RHMIProperty.PropertyId.BOOKMARKABLE, true)
			routesList.getAction()?.asRAAction()?.rhmiActionCallback =
				object : RHMIActionListCallback {
					override fun onAction(index: Int, invokedBy: Int?) {
						if (invokedBy != 2) {       // don't change the navigationEntry
							onRoutesListClicked?.invoke(index)
						} else {
							throw RHMIActionAbort()
						}
					}
				}
		}

		routesList.getSelectAction()?.asRAAction()?.rhmiActionCallback = RHMIActionListCallback {
			if (it != lastInteractionIndex) {
				lastInteractionIndex = it
				deferredUpdate?.defer(INTERACTION_DEBOUNCE_MS.toLong())
			}
		}

		actionsLabel.apply {
			getModel()?.asRaDataModel()?.value = L.EVPLANNING_ACTIONS
			setVisible(true)
			setEnabled(false)
			setSelectable(false)
		}

		actionsList.apply {
			setVisible(true)
			setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH.id,"55,0,*")
			getAction()?.asRAAction()?.rhmiActionCallback = RHMIActionListCallback { index ->
				when(index) {
					0 -> onActionPlanClicked?.invoke()
					else -> throw RHMIActionAbort()
				}
			}
		}

		if (settings.getSettings().isNotEmpty()) {
			settingsLabel.apply {
				getModel()?.asRaDataModel()?.value = L.EVPLANNING_OPTIONS
				setVisible(true)
				setEnabled(false)
				setSelectable(false)
			}
			settingsList.apply {
				setVisible(true)
				setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH.id, "55,0,*,0,100")
				getAction()?.asRAAction()?.rhmiActionCallback = RHMIActionListCallback { index ->
					settingsListData.realData.getOrNull(index)?.let {
						onSettingClicked?.invoke(it)
					} ?: throw RHMIActionAbort()
				}
			}
		}
	}

	fun onCreate(handler: Handler) {
		deferredUpdate = DeferredUpdate(handler)
	}

	/** Only redraw if the user hasn't clicked it recently
	 *  Gets called whenever the notification list changes
	 */
	fun gentlyUpdateList() {
		if (!visible) {
			return
		}

		val deferredUpdate = this.deferredUpdate
		if (deferredUpdate == null) {
			Log.w(TAG, "DeferredUpdate not built yet, redrawing immediately")
			redrawRoutes()
		} else {
			deferredUpdate.trigger(0) {
				if (visible) {
					Log.i(TAG, "Updating list of notifications")
					redrawRoutes()
				} else {
					Log.i(TAG, "Notification list is not on screen, skipping update")
				}
				deferredUpdate.defer(INTERACTION_DEBOUNCE_MS.toLong())   // wait at least this long before doing another update
			}
		}
	}

	// should only be run from the DeferredUpdate thread, once at a time, but synchronize just in case
	fun redrawRoutes() {
		if (!visible) {
			return
		}
		try {
			state.getTextModel()?.asRaDataModel()?.value = listOfNotNull(
				L.EVPLANNING_TITLE_ROUTES,
				when {
					navigationModel.isPlanning -> "[${L.EVPLANNING_REPLANNING}...]"
					navigationModel.isError -> "[${L.EVPLANNING_ERROR}]"
					navigationModel.shouldReplan -> "[${L.EVPLANNING_SHOULD_REPLAN}]"
					else -> null
				}
			).joinToString(" ")

			routesList.getModel()?.value = if (navigationModel.isError) {
				RHMIModel.RaListModel.RHMIListConcrete(3).apply {
					addRow(arrayOf("", navigationModel.errorMessage ?: L.EVPLANNING_ERROR, ""))
				}
			} else {
				val routes = navigationModel.displayRoutes
				if (routes.isNullOrEmpty()) {
					emptyListData
				} else {
					//5 columns: icon, title, dist, soc, eta
					object : RHMIModel.RaListModel.RHMIListAdapter<DisplayRoute>(3, routes) {
						override fun convertRow(index: Int, item: DisplayRoute): Array<Any> {
							val icon = if (item.contains_waypoint) iconFlag ?: "" else ""
							val addition = when {
								!navigationModel.displayRoutesValid -> "[${L.EVPLANNING_INVALID}]"
								item.deviation != null && item.deviation > MAX_STEP_OFFSET -> "${L.EVPLANNING_OFFSET}: ${formatDistanceDetailed(item.deviation)}"
								else -> null
							}
							val firstLine = listOfNotNull(
								item.trip_dst?.let { formatDistance(it) },
								item.arrival_duration?.let { "(${formatTime(it)}h)" },
								addition,
							).joinToString(" ")
							val secondLine = listOfNotNull(
								item.num_charges?.let { "$it charges" },
								item.charge_duration?.let { "(${formatTime(it)}h)" },
							).joinToString(" ")
							return arrayOf(icon, "", "${firstLine}\n${secondLine}")
						}
					}
				}
			}
		} catch (t: Throwable) {
			Sentry.capture(t)
		}
	}

	fun redrawSettingsList() {
		settingsList.getModel()?.value = settingsListData
	}

	fun redrawActionsList() {
		actionsList.getModel()?.value = actionsListData
	}
}