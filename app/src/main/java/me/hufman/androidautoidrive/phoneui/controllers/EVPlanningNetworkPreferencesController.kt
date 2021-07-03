package me.hufman.androidautoidrive.phoneui.controllers

import me.hufman.androidautoidrive.evplanning.NetworkPreference
import me.hufman.androidautoidrive.evplanning.PreferenceUtils
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningIgnoredChargersModel
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningNetworkPreferencesModel

class EVPlanningNetworkPreferencesController(val model: EVPlanningNetworkPreferencesModel) {

	fun removePreference(id: Long) {
		model.settingsNetworkPreferences.apply {
			value?.let {
				setValue(PreferenceUtils.setNetworkPreferenceJsonString(it,id,NetworkPreference.DONTCARE))
			}
		}
	}
}