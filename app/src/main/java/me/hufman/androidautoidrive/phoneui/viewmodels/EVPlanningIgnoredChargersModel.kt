package me.hufman.androidautoidrive.phoneui.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import me.hufman.androidautoidrive.AppSettings
import me.hufman.androidautoidrive.LiveSetting
import me.hufman.androidautoidrive.StringLiveSetting
import me.hufman.androidautoidrive.evplanning.ChargerRouteData
import me.hufman.androidautoidrive.evplanning.PreferenceUtils
import me.hufman.androidautoidrive.evplanning.iternio.entity.Charger

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

