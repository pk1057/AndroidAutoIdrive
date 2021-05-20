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
import java.util.*

class NavigationController(val context: Context, val navigationModel: NavigationModel) {

	fun selectNavigationEntry(index: Int): Boolean {
		with(navigationModel) {
			selectedNavigationEntry = navigationEntries?.getOrNull(index)
			return selectedNavigationEntry != null
		}
	}

	fun navigateToSelectedEntry() {
		navigationModel.selectedNavigationEntry?.let { entry ->
			Address(Locale.getDefault()).apply {
				thoroughfare = entry.address
				featureName = entry.title
				latitude = entry.lat
				longitude = entry.lon
			}.let {
				NavigationParser.addressToRHMI(it)
			}.let {
				NavigationTriggerSender(context).triggerNavigation(it)
			}
		}
	}
}