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
import io.sentry.Sentry
import me.hufman.androidautoidrive.*
import me.hufman.androidautoidrive.carapp.FocusTriggerController
import me.hufman.androidautoidrive.carapp.RHMIActionAbort
import me.hufman.androidautoidrive.carapp.RHMIListAdapter
import me.hufman.androidautoidrive.carapp.evplanning.*
import me.hufman.androidautoidrive.carapp.evplanning.NavigationModelUpdater.Companion.TIME_FMT
import me.hufman.androidautoidrive.carapp.evplanning.NavigationModelUpdater.Companion.formatDistance
import me.hufman.androidautoidrive.carapp.evplanning.NavigationModelUpdater.Companion.formatTimeDifference
import me.hufman.androidautoidrive.carapp.evplanning.TAG
import me.hufman.androidautoidrive.evplanning.DisplayWaypoint
import me.hufman.androidautoidrive.utils.GraphicsHelpers
import me.hufman.idriveconnectionkit.rhmi.*
import java.util.*
import kotlin.reflect.KClass

class WaypointsListView(
	val state: RHMIState,
	val graphicsHelpers: GraphicsHelpers,
	val settings: EVPlanningSettings,
	val focusTriggerController: FocusTriggerController,
	val navigationModel: NavigationModel,
	val carAppImages: Map<String, ByteArray>
) {
	companion object {
		const val INTERACTION_DEBOUNCE_MS =
			2000              // how long to wait after lastInteractionTime to update the list
		const val SKIPTHROUGH_THRESHOLD =
			2000                // how long after an entrybutton push to allow skipping through to a current notification
		const val ARRIVAL_THRESHOLD =
			8000                    // how long after a new notification should it skip through

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

	val waypointsList: RHMIComponent.List
	val actionsLabel: RHMIComponent.Label
	val actionsList: RHMIComponent.List
	val settingsLabel: RHMIComponent.Label
	val settingsList: RHMIComponent.List

	var onWaypointListClicked: ((Int) -> Unit)? = null
	var onActionPlanAlternativesClicked: (() -> Unit)? = null
	var onActionShowAlternativesClicked: (() -> Unit)? = null
	var onActionShowAllWaypointsClicked: (() -> Unit)? = null

	var visible = false                 // whether the notification list is showing

	var entryButtonTimestamp = 0L   // when the user pushed the entryButton
	val timeSinceEntryButton: Long
		get() = System.currentTimeMillis() - entryButtonTimestamp

	var deferredUpdate: DeferredUpdate? = null  // wrapper object to help debounce user inputs
	var lastInteractionIndex: Int = -1       // what index the user last selected

	val iconFlag: ByteArray?

	val emptyListData = RHMIModel.RaListModel.RHMIListConcrete(5).apply {
		addRow(arrayOf("", L.EVPLANNING_EMPTY_LIST, "", "", ""))
	}

	init {
		class Acc(
			val result: MutableList<RHMIComponent>,
			val req: MutableList<KClass<out RHMIComponent>>
		)

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
		waypointsList = components.removeFirst() as RHMIComponent.List
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
				redraw()

				// if a notification is speaking, pre-select it
				// otherwise pre-select the most recent notification that showed up or was selected
				// and only if the user is freshly arriving, not backing out of a deeper view
				val index = 0
				if (didEntryButton && index >= 0) {
					focusTriggerController.focusComponent(waypointsList, index)
				}

				redrawActionsList()
			} else {
				settings.callback = null
			}
		}

		state.getTextModel()?.asRaDataModel()?.value = L.EVPLANNING_TITLE_WAYPOINTS
		state.setProperty(RHMIProperty.PropertyId.HMISTATE_TABLETYPE, 3)
		state.componentsList.forEach { it.setVisible(false) }

		waypointsList.setVisible(true)
		waypointsList.setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH.id, "55,0,*,0,150")
		waypointsList.setProperty(RHMIProperty.PropertyId.BOOKMARKABLE, true)
		waypointsList.getAction()?.asRAAction()?.rhmiActionCallback =
			object : RHMIActionListCallback {
				override fun onAction(index: Int, invokedBy: Int?) {
					if (invokedBy != 2) {       // don't change the navigationEntry
						onWaypointListClicked?.invoke(index)
					}
				}
			}

		waypointsList.getSelectAction()?.asRAAction()?.rhmiActionCallback = RHMIActionListCallback {
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
			setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH.id, "55,0,*")
			getAction()?.asRAAction()?.rhmiActionCallback = RHMIActionListCallback { index ->
				currentActions?.getOrNull(index)?.let {
					when (it) {
						WaypointListActions.PLAN_ALTERNATIVES -> onActionPlanAlternativesClicked?.invoke()
						WaypointListActions.SHOW_ALTERNATIVES -> onActionShowAlternativesClicked?.invoke()
						WaypointListActions.SHOW_ALL_WAYPOINTS -> onActionShowAllWaypointsClicked?.invoke()
					}
				} ?: throw RHMIActionAbort()
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
			redraw()
		} else {
			deferredUpdate.trigger(0) {
				if (visible) {
					Log.i(TAG, "Updating list of notifications")
					redraw()
				} else {
					Log.i(TAG, "Notification list is not on screen, skipping update")
				}
				deferredUpdate.defer(INTERACTION_DEBOUNCE_MS.toLong())   // wait at least this long before doing another update
			}
		}
	}

	fun redraw() {
		if (!visible) {
			return
		}
		redrawWaypoints()
		redrawActionsList()
	}

	// should only be run from the DeferredUpdate thread, once at a time, but synchronize just in case
	fun redrawWaypoints() {
		if (!visible) {
			return
		}

		if (navigationModel.isNextChargerMode) {
			drawNextChargers()
		} else {
			drawSelectedRoute()
		}
	}

	fun drawNextChargers() {
		try {
			state.getTextModel()?.asRaDataModel()?.value = listOfNotNull(
				L.EVPLANNING_TITLE_ALTERNATIVES,
				when {
					navigationModel.isPlanning -> "[${L.EVPLANNING_REPLANNING}...]"
					navigationModel.isError -> "[${L.EVPLANNING_ERROR}]"
					navigationModel.shouldReplan -> "[${L.EVPLANNING_SHOULD_REPLAN}]"
					else -> null
				}
			).joinToString(" ")

			waypointsList.getModel()?.value = if (navigationModel.isError) {
				RHMIModel.RaListModel.RHMIListConcrete(5).apply {
					addRow(
						arrayOf(
							"",
							navigationModel.errorMessage ?: L.EVPLANNING_ERROR,
							"",
							"",
							""
						)
					)
				}
			} else {
				val waypoints = navigationModel.nextChargerWaypoints
				if (waypoints.isNullOrEmpty()) {
					emptyListData
				} else {
					val addition = if (!navigationModel.selectedRouteValid) {
						"[${L.EVPLANNING_INVALID}]"
					} else null
					//5 columns: icon, title, dist, soc, eta
					object : RHMIListAdapter<DisplayWaypoint>(5, waypoints) {
						override fun convertRow(index: Int, wp: DisplayWaypoint): Array<Any> {
							val icon = if (wp.is_waypoint) iconFlag ?: "" else ""
							val firstLine = listOfNotNull(
								wp.title ?: L.EVPLANNING_UNKNOWN_LOC,
								addition,
							).joinToString(" ")
							val secondLine = listOfNotNull(
								wp.operator?.let { "[${it}]" },
								wp.num_chargers?.let {
									if (it > 0) {
										"$it"
									} else {
										null
									}
								},
								wp.charger_type?.toUpperCase(Locale.ROOT),
								wp.trip_dst?.let { formatDistance(it) },
								wp.soc_ariv?.let { "${String.format("%.1f", it)}%" },
								when {
									wp.soc_planned != null && wp.final_num_charges == null -> "(${
										String.format(
											"%.0f",
											wp.soc_planned
										)
									}%)"
									wp.soc_planned == null && wp.final_num_charges != null -> "(${wp.final_num_charges} Charges)"
									wp.soc_planned != null && wp.final_num_charges != null -> "(${
										String.format(
											"%.0f",
											wp.soc_planned
										)
									}%, ${wp.final_num_charges} Charges)"
									else -> null
								},
							).joinToString(" ")
							val delta_dst =
								wp.delta_dst?.let { "+${formatDistance(it)}" } ?: ""
							val delta_dur =
								wp.delta_duration?.let { "+${formatTimeDifference(it)}" }
									?: "--:--"
							return arrayOf(
								icon,
								"",
								"${firstLine}\n${secondLine}",
								"",
								"${delta_dst}\n${delta_dur}"
							)
						}
					}
				}
			}
		} catch (t: Throwable) {
			Sentry.capture(t)
		}
	}

	fun drawSelectedRoute() {
		try {
			state.getTextModel()?.asRaDataModel()?.value = listOfNotNull(
				L.EVPLANNING_TITLE_WAYPOINTS,
				when {
					navigationModel.isPlanning -> "[${L.EVPLANNING_REPLANNING}...]"
					navigationModel.isError -> "[${L.EVPLANNING_ERROR}]"
					navigationModel.shouldReplan -> "[${L.EVPLANNING_SHOULD_REPLAN}]"
					else -> null
				}
			).joinToString(" ")

			waypointsList.getModel()?.value = if (navigationModel.isError) {
				RHMIModel.RaListModel.RHMIListConcrete(5).apply {
					addRow(
						arrayOf(
							"",
							navigationModel.errorMessage ?: L.EVPLANNING_ERROR,
							"",
							"",
							""
						)
					)
				}
			} else {
				val waypoints = navigationModel.selectedRoute
				if (waypoints.isNullOrEmpty()) {
					emptyListData
				} else {
					val addition = if (!navigationModel.selectedRouteValid) {
						"[${L.EVPLANNING_INVALID}]"
					} else null
					//5 columns: icon, title, dist, soc, eta
					object : RHMIListAdapter<DisplayWaypoint>(5, waypoints) {
						override fun convertRow(index: Int, wp: DisplayWaypoint): Array<Any> {
							val icon = if (wp.is_waypoint) iconFlag ?: "" else ""
							val firstLine = listOfNotNull(
								wp.title ?: L.EVPLANNING_UNKNOWN_LOC,
								addition
							).joinToString(" ")
							val secondLine = listOfNotNull(
								wp.operator?.let { "[${it}]" },
								wp.num_chargers?.let {
									if (it > 0) {
										"${it}"
									} else {
										null
									}
								},
								wp.charger_type?.toUpperCase(Locale.ROOT),
								if (index > 0) {
									wp.step_dst?.let { formatDistance(it) }
								} else null,
								wp.soc_ariv?.let { "${String.format("%.1f", it)}%" }
									?: wp.soc_planned?.let { "${String.format("%.0f", it)}%" },
							).joinToString(" ")
							val trip_dst = wp.trip_dst?.let { formatDistance(it) } ?: "-"
							val eta = wp.eta?.format(TIME_FMT) ?: "--:--"
							return arrayOf(
								icon,
								"",
								"${firstLine}\n${secondLine}",
								"",
								"${trip_dst}\n${eta}"
							)
						}
					}
				}
			}
		} catch (t: Throwable) {
			Sentry.capture(t)
		}
	}

	enum class WaypointListActions {
		SHOW_ALL_WAYPOINTS,
		PLAN_ALTERNATIVES,
		SHOW_ALTERNATIVES,
	}

	val actions = mapOf(
		WaypointListActions.PLAN_ALTERNATIVES to L.EVPLANNING_ACTION_PLAN_ALTERNATIVES,
		WaypointListActions.SHOW_ALTERNATIVES to L.EVPLANNING_ACTION_SHOW_ALTERNATIVES,
		WaypointListActions.SHOW_ALL_WAYPOINTS to L.EVPLANNING_ACTION_SHOW_ALL_WAYPOINTS,
	)

	var currentActions: List<WaypointListActions>? = null

	fun redrawActionsList() {

		val actionsList = if (navigationModel.isNextChargerMode) {
			listOf(
				WaypointListActions.SHOW_ALL_WAYPOINTS,
				WaypointListActions.PLAN_ALTERNATIVES,
			)
		} else {
			if (navigationModel.nextChargerWaypoints?.isNotEmpty() == true) {
				listOf(
					WaypointListActions.SHOW_ALTERNATIVES,
					WaypointListActions.PLAN_ALTERNATIVES,
				)
			} else {
				listOf(
					WaypointListActions.PLAN_ALTERNATIVES,
				)
			}
		}
		this.actionsList.getModel()?.value =
			object : RHMIListAdapter<WaypointListActions>(3, actionsList) {
				override fun convertRow(index: Int, item: WaypointListActions): Array<Any> {
					return arrayOf("", "", actions[item] ?: "")
				}
			}

		currentActions = actionsList
	}
}
