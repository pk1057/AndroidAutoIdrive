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

package me.hufman.androidautoidrive.evplanning

import me.hufman.androidautoidrive.CarThread
import me.hufman.androidautoidrive.carapp.evplanning.CarDataListenerRaw
import me.hufman.androidautoidrive.carapp.evplanning.Position
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningDataViewModel

data class CarData(
		val position: Position = Position(),
		val nextDestination: Position = Position(),
		val finalDestination: Position = Position(),
		val odometer: Int = 0,
		val soc: Double = 0.0,
		val drivingMode: Int = 0,
		val speed: Int = 0,
		val torque: Int = 0,
		val batteryTemperatureval: Int = Int.MIN_VALUE,
		val internalTemperature: Int = Int.MIN_VALUE,
		val externalTemperature: Int = Int.MIN_VALUE,
)

class RoutingServiceUpdater(private val updateScheduleMillis: Long) {

	var threadRouting: CarThread? = null
	var threadCarApp: CarThread? = null

	var routingService: RoutingService? = null

	private var position: Position = Position()
	private var nextDestination: Position = Position()
	private var finalDestination: Position = Position()
	private var odometer: Int = 0
	private var soc: Double = 0.0
	private var drivingMode: Int = 0
	private var speed: Int = 0
	private var torque: Int = 0
	private var batteryTemperature: Int = Int.MIN_VALUE
	private var internalTemperature: Int = Int.MIN_VALUE
	private var externalTemperature: Int = Int.MIN_VALUE

	val rawCarDataListener = object : CarDataListenerRaw {
		override fun onPositionChanged(position: Position) {
			this@RoutingServiceUpdater.position = position
		}

		override fun onNextDestinationChanged(position: Position) {
			nextDestination = position
		}

		override fun onFinalDestinationChanged(position: Position) {
			finalDestination = position
		}

		override fun onSOCChanged(soc: Double) {
			this@RoutingServiceUpdater.soc = soc
		}

		override fun onDrivingModeChanged(drivingMode: Int) {
			this@RoutingServiceUpdater.drivingMode = drivingMode
		}

		override fun onOdometerChanged(odometer: Int) {
			this@RoutingServiceUpdater.odometer = odometer
		}

		override fun onSpeedChanged(speed: Int) {
			this@RoutingServiceUpdater.speed = speed
		}

		override fun onTorqueChanged(torque: Int) {
			this@RoutingServiceUpdater.torque = torque
		}

		override fun onBatteryTemperatureChanged(batteryTemperature: Int) {
			this@RoutingServiceUpdater.batteryTemperature = batteryTemperature
		}

		override fun onInternalTemperatureChanged(internalTemperature: Int) {
			this@RoutingServiceUpdater.internalTemperature = internalTemperature
		}

		override fun onExternalTemperatureChanged(externalTemperature: Int) {
			this@RoutingServiceUpdater.externalTemperature = externalTemperature
		}
	}

	private fun doUpdate() {
		threadRouting?.post {
			routingService?.onCarDataChanged(
					CarData(
							position,
							nextDestination,
							finalDestination,
							odometer,
							soc,
							drivingMode,
							speed,
							torque,
							batteryTemperature,
							internalTemperature,
							externalTemperature,
					))
		}
		triggerNextUpdate()
	}

	var counter: Int=0
	fun triggerNextUpdate() {
		counter++
		EVPlanningDataViewModel.setCardataUpdates(counter)
		threadCarApp?.handler?.postDelayed(this::doUpdate, updateScheduleMillis)
	}

	fun onCreateCarApp(carThread: CarThread?) {
		threadCarApp = carThread
	}

	fun onCreateRouting(carThread: CarThread?) {
		threadRouting = carThread
	}

	fun onDestroy() {
		routingService = null
		threadCarApp = null
		threadRouting = null
	}

	fun registerRoutingService(service: RoutingService?) {
		routingService = service
	}
}