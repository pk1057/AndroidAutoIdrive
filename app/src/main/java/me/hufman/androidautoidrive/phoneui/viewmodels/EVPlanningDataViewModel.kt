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
import me.hufman.androidautoidrive.evplanning.RoutingData

class EVPlanningDataViewModel(): ViewModel() {
	class Factory(val context: Context) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel?> create(modelClass: Class<T>): T {
			return EVPlanningDataViewModel() as T
		}
	}

	companion object {
		private val _routingData = MutableLiveData<RoutingData>()
		fun setRoutingData(routingData: RoutingData) {
			_routingData.postValue(routingData)
		}
		private val _status = MutableLiveData<String>()
		fun setStatus(status: String) {
			_status.postValue(status)
		}
		private val _cardataUpdates = MutableLiveData<Int>()
		fun setCardataUpdates(updates: Int) {
			_cardataUpdates.postValue(updates)
		}
		private val _error = MutableLiveData<String>()
		fun setError(error: String) {
			_error.postValue(error)
		}
		private val _carappDebug = MutableLiveData<String>()
		fun setCardataDebug(debug: String) {
			_carappDebug.postValue(debug)
		}
		private val _maxSpeed = MutableLiveData<Double?>()
		fun setMaxSpeed(maxSpeed: Double?) {
			_maxSpeed.postValue(maxSpeed)
		}
	}

	val routingData: LiveData<RoutingData> = _routingData
	val status: LiveData<String> = _status
	val cardataUpdates: LiveData<Int> = _cardataUpdates
	val error: LiveData<String> = _error
	val carappDebug: LiveData<String> = _carappDebug
	val maxSpeed: LiveData<Double?> = _maxSpeed
}