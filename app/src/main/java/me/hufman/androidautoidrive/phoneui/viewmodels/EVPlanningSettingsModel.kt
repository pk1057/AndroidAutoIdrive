package me.hufman.androidautoidrive.phoneui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.hufman.androidautoidrive.AppSettings
import me.hufman.androidautoidrive.BooleanLiveSetting
import me.hufman.androidautoidrive.StringLiveSetting

class EVPlanningSettingsModel(appContext: Context): ViewModel() {
	class Factory(val appContext: Context): ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return EVPlanningSettingsModel(appContext) as T
		}
	}

	val enableReplan = BooleanLiveSetting(appContext,AppSettings.KEYS.EVPLANNING_AUTO_REPLAN)
	val maxSpeedComfort = StringLiveSetting(appContext,AppSettings.KEYS.EVPLANNING_MAXSPEED_COMFORT)
	val maxSpeedEco = StringLiveSetting(appContext,AppSettings.KEYS.EVPLANNING_MAXSPEED_ECO_PRO)
	val maxSpeedEcoPlus = StringLiveSetting(appContext,AppSettings.KEYS.EVPLANNING_MAXSPEED_ECO_PRO_PLUS)
	val maxSpeed = StringLiveSetting(appContext,AppSettings.KEYS.EVPLANNING_MAXSPEED)
	val maxSpeedDrivemodeEnable = BooleanLiveSetting(appContext,AppSettings.KEYS.EVPLANNING_MAXSPEED_DRIVEMODE_ENABLE)
}