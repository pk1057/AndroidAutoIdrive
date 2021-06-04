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
import android.location.Address
import io.sentry.Sentry
import me.hufman.androidautoidrive.carapp.navigation.NavigationParser
import me.hufman.androidautoidrive.carapp.navigation.NavigationTriggerSender
import me.hufman.androidautoidrive.evplanning.DisplayRoute
import me.hufman.androidautoidrive.evplanning.DisplayWaypoint
import java.util.*

class NavigationModel {

	var isPlanning: Boolean = false
	var isError: Boolean = false
	var shouldReplan: Boolean = false
	var isNextChargerMode: Boolean = false

	var errorMessage: String? = null

	var displayRoutesValid: Boolean = false
	var selectedRouteValid: Boolean = false
	var selectedWaypointValid: Boolean = false

	var displayRoutes: List<DisplayRoute>? = null
	var selectedRoute: List<DisplayWaypoint>? = null
	var nextChargerWaypoints: List<DisplayWaypoint>? = null
	var nextChargerWaypointsValid: Boolean = false
	var selectedWaypoint: DisplayWaypoint? = null

	var selectedRouteIndex: Int? = null
	var selectedWaypointIndex: Int? = null

	var routesListObserver: (() -> Unit)? = null
	var waypointListObserver: (() -> Unit)? = null
	var selectedWaypointObserver: (() -> Unit)? = null
}

class NavigationModelController(val context: Context) {

	val navigationModel = NavigationModel()

	// to be called from ui:
	fun selectRoute(index: Int): Boolean {
		with(navigationModel) {
			selectedRouteIndex = index
			selectedRoute = displayRoutes?.getOrNull(index)?.displayWaypoints
			selectedRouteValid = displayRoutesValid
			waypointListObserver?.invoke()
			return selectedRoute != null
		}
	}

	// to be called from ui:
	fun selectWaypoint(index: Int): Boolean {
		with(navigationModel) {
			selectedWaypointIndex = index
			selectedWaypoint = if (isNextChargerMode) {
				nextChargerWaypoints?.getOrNull(index)
			} else {
				selectedRoute?.getOrNull(index)
			}
			selectedWaypointValid = selectedRouteValid
			selectedWaypointObserver?.invoke()
			return selectedWaypoint != null
		}
	}

	// to be called from ui:
	fun switchToAllWaypoints() {
		navigationModel.apply {
			isNextChargerMode = false
			waypointListObserver?.invoke()
		}
	}

	// to be called from ui:
	fun switchToAlternatives() {
		navigationModel.apply {
			isNextChargerMode = true
			waypointListObserver?.invoke()
		}
	}

	// to be called from ui:
	fun navigateToWaypoint() {
		navigationModel.selectedWaypoint?.let { entry ->
			Address(Locale.getDefault()).apply {
				thoroughfare = entry.address
				featureName = "${entry.title} [${entry.operator}] ${entry.charger_type}"
				latitude = entry.lat
				longitude = entry.lon
			}.let {
				NavigationParser.addressToRHMI(it)
			}.let {
				NavigationTriggerSender(context).triggerNavigation(it)
			}
		}
	}

	// to be called from RoutingService through NavigationModelUpdater.onRoutingDataChanged
	fun setDisplayRoutes(newDisplayRoutes: List<DisplayRoute>?) {
		try {
			navigationModel.apply {
				displayRoutes = newDisplayRoutes
				displayRoutesValid = true
				routesListObserver?.invoke()
				if (!isNextChargerMode) {
					selectedRouteIndex?.let {
						selectedRoute = displayRoutes?.getOrNull(it)?.displayWaypoints
						selectedRouteValid = true
					}
					selectedWaypointIndex?.let {
						selectedWaypoint = selectedRoute?.getOrNull(it)
						selectedWaypointValid = true
					}
				}
			}
		} catch (t: Throwable) {
			Sentry.capture(t)
		}
	}

	// to be called from RoutingService through NavigationModelUpdater.onRoutingDataChanged
	fun setNextChargerWaypoints(displayWaypoints: List<DisplayWaypoint>?) {
		try {
			navigationModel.apply {
				if (displayWaypoints == null) {
					if (isNextChargerMode) {
						isNextChargerMode = false
						selectedWaypointIndex = null
						selectedWaypointValid = false
					}
				} else {
					if (nextChargerWaypoints == null) {
						isNextChargerMode = true
						selectedWaypointIndex = null
					}
				}
				nextChargerWaypoints = displayWaypoints
				if (isNextChargerMode) {
					selectedWaypointIndex?.let {
						selectedWaypoint = nextChargerWaypoints?.getOrNull(it)
						selectedWaypointValid = true
					}
				}
			}
		} catch (t: Throwable) {
			Sentry.capture(t)
		}
	}

	// to be called from RoutingService through NavigationModelUpdater.onRoutingDataChanged
	fun setShouldReplan(value: Boolean) {
		navigationModel.apply {
			shouldReplan = value
		}
	}

	// to be called from RoutingService through NavigationModelUpdater.onPlanChanged
	fun planningFinished() {
		navigationModel.apply {
			isPlanning = false
			isError = false
			displayRoutesValid = false
			selectedRouteValid = false
			selectedWaypointValid = false
			selectedRouteIndex = null // delete the indices only so anything that is on screen will stay for now
			selectedWaypointIndex = null
			isNextChargerMode = false
			invokeAllObservers()
		}
	}

	// to be called from RoutingService through NavigationModelUpdater.onNextChargerPlanChanged
	fun nextChargerFinished() {
		navigationModel.apply {
			isPlanning = false
			isError = false
			selectedWaypointIndex = null
			selectedWaypointValid = false
			invokeAllObservers()
		}
	}

	// to be called from RoutingService through NavigationModelUpdater.onPlanningTriggered
	fun planningTriggered() {
		navigationModel.apply {
			isPlanning = true
			invokeAllObservers()
		}
	}

	// to be called from RoutingService through NavigationModelUpdater.onPlanningError
	fun planningError(msg: String) {
		navigationModel.apply {
			isPlanning = false
			isError = true
			errorMessage = msg
			invokeAllObservers()
		}
	}

	fun invokeAllObservers() {
		navigationModel.apply {
			routesListObserver?.invoke()
			waypointListObserver?.invoke()
			selectedWaypointObserver?.invoke()
		}
	}
}