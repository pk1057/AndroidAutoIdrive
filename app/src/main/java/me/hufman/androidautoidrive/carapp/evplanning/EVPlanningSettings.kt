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

import me.hufman.androidautoidrive.AppSettings
import me.hufman.androidautoidrive.MutableAppSettingsObserver
import me.hufman.androidautoidrive.connections.BtStatus

class EVPlanningSettings(val capabilities: Map<String, String?>, val btStatus: BtStatus, val appSettings: MutableAppSettingsObserver) {
	var callback
		get() = appSettings.callback
		set(value) { appSettings.callback = value }

	// car's supported features
	val tts = capabilities["tts"]?.toLowerCase() == "true"

	fun getSettings(): List<AppSettings.KEYS> {
		return listOf()
	}

	fun toggleSetting(setting: AppSettings.KEYS) {
		appSettings[setting] = (!appSettings[setting].toBoolean()).toString()
	}

	fun isChecked(setting: AppSettings.KEYS): Boolean {
		return appSettings[setting].toBoolean()
	}
}