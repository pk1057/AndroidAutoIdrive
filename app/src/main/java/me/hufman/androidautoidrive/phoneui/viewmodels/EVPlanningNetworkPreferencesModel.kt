package me.hufman.androidautoidrive.phoneui.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.hufman.androidautoidrive.AppSettings
import me.hufman.androidautoidrive.StringLiveSetting
import me.hufman.androidautoidrive.evplanning.ChargerRouteData
import me.hufman.androidautoidrive.evplanning.NetworkPreferenceData

class EVPlanningNetworkPreferencesModel(appContext: Context): ViewModel() {
	class Factory(val appContext: Context): ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return EVPlanningNetworkPreferencesModel(appContext) as T
		}
	}

	companion object {
		private val _preferencesData = MutableLiveData<Map<Long, NetworkPreferenceData>>()
		fun setPreferencesData(preferencesData: Map<Long, NetworkPreferenceData>) {
			_preferencesData.postValue(preferencesData)
		}
	}

	val preferencesData: LiveData<Map<Long, NetworkPreferenceData>> = _preferencesData
	val settingsNetworkPreferences = StringLiveSetting(appContext,AppSettings.KEYS.EVPLANNING_NETWORK_PREFERENCES)
}

