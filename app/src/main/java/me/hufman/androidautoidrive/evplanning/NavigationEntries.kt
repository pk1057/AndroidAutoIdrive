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

package me.hufman.androidautoidrive.evplanning

import android.graphics.drawable.Drawable
import java.time.LocalDateTime

data class DisplayRoute(
		val icon: Drawable? = null,
		val trip_dst: Int? = null,              // total length of remaining trip in meter
		val arrival_duration: Int? = null,      // total remaining time in minutes
		val num_charges: Int? = null,
		val charge_duration: Int? = null,       // total charging-time in minutes
		val deviation: Int? = null,             // distance to closest step or pathstep of current route in meter
		val contains_waypoint: Boolean = false, // true if route contains more than just the final iDrive waypoint
		val displayWaypoints: List<DisplayWaypoint>? = null,
)

data class DisplayWaypoint(
	val icon: Drawable? = null,          // the icon in the main list
	val title: String? = null,           // name of current location
	val operator: String? = null,
	val charger_type: String? = null,    // CCS, Chademo or Type2
	val is_waypoint: Boolean,            // is this an iDrive Waypoint?
	val is_initial_charger: Boolean,     // is this the current (or very close by) position?
	val address: String,
	val trip_dst: Int? = null,           // distance to current location in meter
	val step_dst: Int? = null,           // distance since last stop in meter
	val soc_ariv: Double? = null,           // estimated state of charge on arrival (as of real soc) in percent
	val soc_planned: Double? = null,        // estimated state of charge on arrival (as of planning) in percent
	val soc_dep: Double? = null,            // recommended state of charge at departure in percent
	val eta: LocalDateTime? = null,      // estimated time of arrival
	val etd: LocalDateTime? = null,      // estimated time of departure
	val duration: Int? = null,           // charging duration in minutes
	val num_chargers: Int? = null,
	val free_chargers: Int? = null,
	val delta_duration: Int? = null,     // difference in total trip time in minutes (for next charger view)
	val delta_dst: Int? = null,          // difference in total trip length in meter (for next charger view)
	val final_num_charges: Int? = null,  // number of charging stops until final dest (for next charger view)
	val lat: Double,
	val lon: Double,
)
