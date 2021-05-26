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
import java.util.*

class NavigationController(val context: Context, val navigationModel: NavigationModel) {

	fun selectRoute(index: Int): Boolean {
		with(navigationModel) {
			selectedRouteIndex = index
			selectedRoute = displayRoutes?.getOrNull(index)?.displayWaypoints
			selectedRouteValid = displayRoutesValid
			selectedRouteObserver?.invoke()
			return selectedRoute != null
		}
	}

	fun selectWaypoint(index: Int): Boolean {
		with(navigationModel) {
			selectedWaypointIndex = index
			selectedWaypoint = selectedRoute?.getOrNull(index)
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
			displayRoutesObserver?.invoke()
			selectedRouteIndex?.let {
				selectedRoute = displayRoutes?.getOrNull(it)?.displayWaypoints
				selectedRouteValid = true
				selectedRouteObserver?.invoke()
			}
			selectedWaypointIndex?.let {
				selectedWaypoint = selectedRoute?.getOrNull(it)
				selectedWaypointValid = true
				selectedWaypointObserver?.invoke()
			}
		}
	}

	fun invalidateAll() {
		navigationModel.apply {
			displayRoutesValid = false
			selectedRouteValid = false
			selectedWaypointValid = false
			selectedRouteIndex = null // delete the indices only so anything that is on screen will stay for now
			selectedWaypointIndex = null
			displayRoutesObserver?.invoke()
			selectedRouteObserver?.invoke()
			selectedWaypointObserver?.invoke()
		}
	}
}