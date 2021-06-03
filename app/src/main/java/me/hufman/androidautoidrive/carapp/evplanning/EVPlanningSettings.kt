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
		set(value) {
			appSettings.callback = value
		}

	// car's supported features
	val tts = capabilities["tts"]?.toLowerCase() == "true"

	val booleanSettings = listOf(
			AppSettings.KEYS.EVPLANNING_AUTO_REPLAN,
			AppSettings.KEYS.EVPLANNING_MAXSPEED_DRIVEMODE_ENABLE,
	)

	val stringSettings = listOf(
			AppSettings.KEYS.EVPLANNING_MAXSPEED,
			AppSettings.KEYS.EVPLANNING_MAXSPEED_COMFORT,
			AppSettings.KEYS.EVPLANNING_MAXSPEED_ECO_PRO,
			AppSettings.KEYS.EVPLANNING_MAXSPEED_ECO_PRO_PLUS,
			AppSettings.KEYS.EVPLANNING_REFERENCE_CONSUMPTION,
			AppSettings.KEYS.EVPLANNING_MIN_SOC_CHARGER,
			AppSettings.KEYS.EVPLANNING_MIN_SOC_FINAL,
	)

	fun getSettings(): List<AppSettings.KEYS> {
		return booleanSettings + stringSettings
	}

	fun isBooleanSetting(setting: AppSettings.KEYS): Boolean {
		return booleanSettings.contains(setting)
	}

	fun isStringSetting(setting: AppSettings.KEYS): Boolean {
		return stringSettings.contains(setting)
	}

	fun toggleSetting(setting: AppSettings.KEYS) {
		appSettings[setting] = (!appSettings[setting].toBoolean()).toString()
	}

	fun isChecked(setting: AppSettings.KEYS): Boolean {
		return appSettings[setting].toBoolean()
	}

	fun getStringSetting(setting: AppSettings.KEYS): String {
		return appSettings[setting]
	}

	fun setStringSetting(setting: AppSettings.KEYS, value: String) {
		appSettings[setting] = value
	}
}