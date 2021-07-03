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
	val referenceConsumption = StringLiveSetting(appContext,AppSettings.KEYS.EVPLANNING_REFERENCE_CONSUMPTION)
	val minSocCharger = StringLiveSetting(appContext,AppSettings.KEYS.EVPLANNING_MIN_SOC_CHARGER)
	val minSocFinal = StringLiveSetting(appContext,AppSettings.KEYS.EVPLANNING_MIN_SOC_FINAL)
}

