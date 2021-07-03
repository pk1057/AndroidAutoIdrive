package me.hufman.androidautoidrive.phoneui.controllers

import me.hufman.androidautoidrive.evplanning.PreferenceUtils
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningIgnoredChargersModel

class EVPlanningIgnoredChargersController(val model: EVPlanningIgnoredChargersModel) {

	fun removeCharger(id: Long) {
		model.settingIgnoredChargers.apply {
			value?.let {
				setValue(PreferenceUtils.removeLongJsonString(it,id))
			}
		}
	}
}