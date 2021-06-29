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
import me.hufman.androidautoidrive.evplanning.NetworkPreference
import me.hufman.androidautoidrive.evplanning.PreferenceUtils.Companion.addLongJsonString
import me.hufman.androidautoidrive.evplanning.PreferenceUtils.Companion.removeLongJsonString
import me.hufman.androidautoidrive.evplanning.PreferenceUtils.Companion.setNetworkPreferenceJsonString
import java.util.*

class EVPlanningSettings(val capabilities: Map<String, String?>, val btStatus: BtStatus, val appSettings: MutableAppSettingsObserver) {
	var callback
		get() = appSettings.callback
		set(value) {
			appSettings.callback = value
		}

	// car's supported features
	val tts = capabilities["tts"]?.toLowerCase(Locale.ROOT) == "true"

	val booleanSettings = listOf(
			AppSettings.KEYS.EVPLANNING_AUTO_REPLAN,
			AppSettings.KEYS.EVPLANNING_MAXSPEED_DRIVEMODE_ENABLE,
	)

	val stringSettings = mapOf(
		AppSettings.KEYS.EVPLANNING_MAXSPEED to (40..160 step 5).map { it.toString() },
		AppSettings.KEYS.EVPLANNING_MAXSPEED_COMFORT to (40..160 step 5).map { it.toString() },
		AppSettings.KEYS.EVPLANNING_MAXSPEED_ECO_PRO to (40..160 step 5).map { it.toString() },
		AppSettings.KEYS.EVPLANNING_MAXSPEED_ECO_PRO_PLUS to (40..160 step 5).map { it.toString() },
		AppSettings.KEYS.EVPLANNING_REFERENCE_CONSUMPTION to (100..200 step 5).map { it.toString() },
		AppSettings.KEYS.EVPLANNING_MIN_SOC_CHARGER to (10..90 step 5).map { it.toString() },
		AppSettings.KEYS.EVPLANNING_MIN_SOC_FINAL to (10..90 step 5).map { it.toString() },
	)

	fun getSettings(): List<AppSettings.KEYS> {
		return booleanSettings + stringSettings.keys
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

	fun getSuggestions(setting: AppSettings.KEYS): List<String> {
		return stringSettings[setting] ?: emptyList()
	}

	fun addIgnoreCharger(id: Long) {
		appSettings[AppSettings.KEYS.EVPLANNING_IGNORE_CHARGERS] =
			addLongJsonString(
				appSettings[AppSettings.KEYS.EVPLANNING_IGNORE_CHARGERS],
				id
			)
	}

	fun removeIgnoreCharger(id: Long) {
		appSettings[AppSettings.KEYS.EVPLANNING_IGNORE_CHARGERS] =
			removeLongJsonString(
				appSettings[AppSettings.KEYS.EVPLANNING_IGNORE_CHARGERS],
				id
			)
	}

	fun setNetworkPreference(id: Long, preference: NetworkPreference) {
		appSettings[AppSettings.KEYS.EVPLANNING_NETWORK_PREFERENCES] =
			setNetworkPreferenceJsonString(
				appSettings[AppSettings.KEYS.EVPLANNING_NETWORK_PREFERENCES],
				id,
				preference
			)
	}
}