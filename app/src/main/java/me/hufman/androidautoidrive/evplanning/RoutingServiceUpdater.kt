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
import me.hufman.androidautoidrive.carapp.evplanning.CarApplicationListener
import me.hufman.androidautoidrive.carapp.evplanning.NavigationModelUpdater
import me.hufman.androidautoidrive.carapp.evplanning.Position
import me.hufman.androidautoidrive.carapp.evplanning.PositionDetailedInfo
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningDataViewModel

data class CarData(
	val position: Position = Position(),
	val nextDestination: Position = Position(),
	val nextDestinationDetails: PositionDetailedInfo = PositionDetailedInfo(),
	val finalDestination: Position = Position(),
	val finalDestinationDetails: PositionDetailedInfo = PositionDetailedInfo(),
	val odometer: Int = 0,
	val soc: Double = 0.0,
	val drivingMode: DrivingMode = DrivingMode.UNDEFINED,
	val speed: Int = 0,
	val torque: Int = 0,
	val batteryTemperatureval: Int = Int.MIN_VALUE,
	val internalTemperature: Int = Int.MIN_VALUE,
	val externalTemperature: Int = Int.MIN_VALUE,
)

enum class DrivingMode(val raw: Int) {
	UNDEFINED(0),
	COMFORT(2),
	COMFORT_PLUS(9),
	BASIC(3),
	SPORT(4),
	SPORT_PLUS(5),
	RACE(6),
	ECOPRO(7),
	ECOPRO_PLUS(8);

	companion object {
		val map = values().asSequence().map { it.raw to it }.toMap()
		fun of(value: Int): DrivingMode {
			return map.getOrElse(value) { throw IllegalArgumentException("$value is not a valid DrivingMode") }
		}
	}
}

/*
 * The purpose of the RoutingServiceUpdater is to post aggregated data
 * from the carapp to the RoutingService running on it's own thread
 * Aggregated data is posted once a second
 */
class RoutingServiceUpdater(private val updateScheduleMillis: Long) {

	var threadRouting: CarThread? = null
	var threadCarApp: CarThread? = null

	var routingService: RoutingService? = null
	var navigationModelUpdater: NavigationModelUpdater? = null

	private var nextDestination = Position()
	private var position = Position()
	private var nextDestinationDetails = PositionDetailedInfo()
	private var finalDestination = Position()
	private var finalDestinationDetails = PositionDetailedInfo()
	private var odometer: Int = 0
	private var soc: Double = 0.0
	private var drivingMode: DrivingMode = DrivingMode.UNDEFINED
	private var speed: Int = 0
	private var torque: Int = 0
	private var batteryTemperature: Int = Int.MIN_VALUE
	private var internalTemperature: Int = Int.MIN_VALUE
	private var externalTemperature: Int = Int.MIN_VALUE

	val rawCarDataListener = object : CarApplicationListener {
		override fun onPositionChanged(position: Position) {
			this@RoutingServiceUpdater.position = position
		}

		override fun onNextDestinationChanged(position: Position) {
			nextDestination = position
		}

		override fun onNextDestinationDetailsChanged(info: PositionDetailedInfo) {
			nextDestinationDetails = info
		}

		override fun onFinalDestinationChanged(position: Position) {
			finalDestination = position
		}

		override fun onFinalDestinationDetailsChanged(info: PositionDetailedInfo) {
			finalDestinationDetails = info
		}

		override fun onSOCChanged(soc: Double) {
			this@RoutingServiceUpdater.soc = soc
		}

		override fun onDrivingModeChanged(drivingMode: Int) {
			this@RoutingServiceUpdater.drivingMode = try {
				DrivingMode.of(drivingMode)
			} catch (iae: IllegalArgumentException) {
				DrivingMode.UNDEFINED
			}
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

		override fun triggerNewPlanning() {
			threadRouting?.post {
				routingService?.planNew()
			}
		}

		override fun triggerAlternativesPlanning() {
			threadRouting?.post {
				routingService?.planAlternateNext()
			}
		}

		override fun triggerCheckReloadDetails() {
			threadRouting?.post {
				routingService?.checkNetworkPreferencesDetails()
				routingService?.checkIgnoredChargerDetails()
			}
		}
	}

	private fun doUpdate() {
		threadRouting?.post {
			routingService?.onCarDataChanged(
				CarData(
					position,
					nextDestination,
					nextDestinationDetails,
					finalDestination,
					finalDestinationDetails,
					odometer,
					soc,
					drivingMode,
					speed,
					torque,
					batteryTemperature,
					internalTemperature,
					externalTemperature,
				)
			)
		}
		triggerNextUpdate()
	}

	var counter: Int = 0
	fun triggerNextUpdate() {

		//TODO: for debugging, remove later:
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