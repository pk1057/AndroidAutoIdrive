package me.hufman.androidautoidrive.carapp.evplanning

import me.hufman.androidautoidrive.evplanning.NavigationEntry

/**********************************************************************************************
Copyright (C) 2018 Norbert Truchsess norbert.truchsess@t-online.de

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

	// navigation data
	var navigationEntries: List<NavigationEntry>? = null
		set(value) {
			field = value
			navigationEntriesObserver?.invoke()
		}

	var selectedNavigationEntry: NavigationEntry? = null
		set(value) {
			field = value
			selectedNavigationEntryObserver?.invoke()
		}

	var navigationEntriesObserver: (() -> Unit)? = null
	var selectedNavigationEntryObserver: (() -> Unit)? = null
}
