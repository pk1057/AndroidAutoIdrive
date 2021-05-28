package me.hufman.androidautoidrive.carapp.evplanning

import me.hufman.androidautoidrive.evplanning.DisplayRoute
import me.hufman.androidautoidrive.evplanning.DisplayWaypoint

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

class NavigationModel {

	var isPlanning: Boolean = false

	var displayRoutesValid: Boolean = false
	var selectedRouteValid: Boolean = false
	var selectedWaypointValid: Boolean = false

	var displayRoutes: List<DisplayRoute>? = null
	var selectedRoute: List<DisplayWaypoint>? = null
	var selectedWaypoint: DisplayWaypoint? = null

	var selectedRouteIndex: Int? = null
	var selectedWaypointIndex: Int? = null

	var displayRoutesObserver: (() -> Unit)? = null
	var selectedRouteObserver: (() -> Unit)? = null
	var selectedWaypointObserver: (() -> Unit)? = null

}
