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
import me.hufman.androidautoidrive.carapp.navigation.NavigationParser
import me.hufman.androidautoidrive.carapp.navigation.NavigationTriggerSender
import me.hufman.androidautoidrive.evplanning.DisplayRoute
import me.hufman.androidautoidrive.evplanning.DisplayWaypoint
import java.util.*

class NavigationModel {

	var isPlanning: Boolean = false
	var isNextChargerMode: Boolean = false

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

	fun selectRoute(index: Int): Boolean {
		with(navigationModel) {
			selectedRouteIndex = index
			selectedRoute = displayRoutes?.getOrNull(index)?.displayWaypoints
			selectedRouteValid = displayRoutesValid
			waypointListObserver?.invoke()
			return selectedRoute != null
		}
	}

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

	fun setDisplayRoutes(newDisplayRoutes: List<DisplayRoute>?) {
		navigationModel.apply {
			displayRoutes = newDisplayRoutes
			displayRoutesValid = true
			routesListObserver?.invoke()
			if (!isNextChargerMode) {
				selectedRouteIndex?.let {
					selectedRoute = displayRoutes?.getOrNull(it)?.displayWaypoints
					selectedRouteValid = true
					waypointListObserver?.invoke()
				}
				selectedWaypointIndex?.let {
					selectedWaypoint = selectedRoute?.getOrNull(it)
					selectedWaypointValid = true
					selectedWaypointObserver?.invoke()
				}
			}
		}
	}

	fun setNextChargerWaypoints(displayWaypoints: List<DisplayWaypoint>?) {
		navigationModel.apply {
			if (displayWaypoints == null) {
				if (isNextChargerMode) {
					isNextChargerMode = false
					selectedWaypointIndex = null
					selectedWaypointValid = false
					selectedWaypointObserver?.invoke()
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
					selectedWaypointObserver?.invoke()
				}
				waypointListObserver?.invoke()
			}
		}
	}

	fun switchToAllWaypoints() {
		navigationModel.apply {
			isNextChargerMode = false
			waypointListObserver?.invoke()
		}
	}

	fun switchToAlternatives() {
		navigationModel.apply {
			isNextChargerMode = true
			waypointListObserver?.invoke()
		}
	}

	fun planningFinished() {
		navigationModel.apply {
			isPlanning = false
			displayRoutesValid = false
			selectedRouteValid = false
			selectedWaypointValid = false
			selectedRouteIndex = null // delete the indices only so anything that is on screen will stay for now
			selectedWaypointIndex = null
			isNextChargerMode = false
			routesListObserver?.invoke()
			waypointListObserver?.invoke()
			selectedWaypointObserver?.invoke()
		}
	}

	fun nextChargerFinished() {
		navigationModel.apply {
			isPlanning = false
			selectedWaypointIndex = null
			selectedWaypointValid = false
			routesListObserver?.invoke()
			waypointListObserver?.invoke()
			selectedWaypointObserver?.invoke()
		}
	}

	fun planningTriggered() {
		navigationModel.apply {
			isPlanning = true
			routesListObserver?.invoke()
			waypointListObserver?.invoke()
			selectedWaypointObserver?.invoke()
		}
	}
}