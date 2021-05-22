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

data class NavigationEntry(
		val icon: Drawable? = null,   // the icon in the main list
		val title: String,            // first line of text
		val operator: String? = null,
		val type: String? = null,     // details
		val address: String,
		val trip_dst: String? = null, // distance
		val step_dst: String? = null, // distance since last stop
		val soc_ariv: String? = null, // estimated state of charge on arrival
		val soc_dep: String? = null,  // recommended state of charge at departure
		val eta: String? = null,      // estimated time of arrival
		val etd: String? = null,      // estimated time of departure
		val duration: String? = null,
		val lat: Double,
		val lon: Double,
)