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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.hufman.androidautoidrive.AppSettings
import me.hufman.androidautoidrive.StringLiveSetting
import me.hufman.androidautoidrive.evplanning.ChargerRouteData

class EVPlanningIgnoredChargersModel(appContext: Context): ViewModel() {
	class Factory(val appContext: Context): ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return EVPlanningIgnoredChargersModel(appContext) as T
		}
	}

	companion object {
		private val _chargerData = MutableLiveData<Map<Long,ChargerRouteData?>>()
		fun setChargerData(chargerData: Map<Long,ChargerRouteData?>) {
			_chargerData.postValue(chargerData)
		}
	}

	val chargerData: LiveData<Map<Long, ChargerRouteData?>> = _chargerData
	val settingIgnoredChargers = StringLiveSetting(appContext,AppSettings.KEYS.EVPLANNING_IGNORE_CHARGERS)
}

