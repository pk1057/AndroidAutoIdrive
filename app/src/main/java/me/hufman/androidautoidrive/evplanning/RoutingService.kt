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

import android.content.Context
import android.os.Handler
import me.hufman.androidautoidrive.AppSettings
import me.hufman.androidautoidrive.MutableAppSettingsReceiver
import me.hufman.androidautoidrive.evplanning.iternio.Planning
import me.hufman.androidautoidrive.evplanning.iternio.dto.Destination
import me.hufman.androidautoidrive.evplanning.iternio.dto.PlanRequest
import me.hufman.androidautoidrive.evplanning.iternio.entity.*
import me.hufman.androidautoidrive.evplanning.util.CheapRuler
import me.hufman.androidautoidrive.evplanning.util.Point
import me.hufman.androidautoidrive.evplanning.util.Units
import me.hufman.androidautoidrive.carapp.evplanning.Position
import me.hufman.androidautoidrive.evplanning.RoutingService.RouteDistance.Companion.fromStepSegment
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningDataViewModel
import kotlin.math.sqrt

data class RoutingData(
	val carData: CarData,
	val routeDistances: List<RoutingService.RouteDistance>?,
	val existingWaypointIndices: List<List<Int>?>?
)

interface RoutingDataListener {
	fun onRoutingDataChanged(routingData: RoutingData)
	fun onPlanChanged(plan: Plan?)
	fun onNextChargerPlanChanged(plan: Plan?)
	fun onPlanningTriggered()
	fun onPlanningError(msg: String)
}

class RoutingService(
	private val planning: Planning,
	private val routingDataListener: RoutingDataListener
) {

	var handler: Handler? = null

	private var appSettings: MutableAppSettingsReceiver? = null
	private var routingInProgress = false
	private var carData = CarData()

	private var maxSpeed: Double? = null
		set(value) {
			if (isChangedForReplan(field, value)) {
				field = value
			}
		}

	private var referenceConsumption: Int? = null
		set(value) {
			if (isChangedForReplan(field, value)) {
				field = value
			}
		}

	private var shouldReplan = false

	private var carModel = "bmw:i3:19:38:other"

	private fun resetPlanning() {
		planResult = null
		error = null
	}

	fun planNew() {
		triggerPlanning(find_alts = true)
	}

	fun planNewNext() {
		triggerPlanning(plan_uuid = planResult?.result?.plan_uuid)
	}

	fun planAlternateNext() {
		triggerPlanning(find_next_charger_alts = true, plan_uuid = planResult?.result?.plan_uuid)
	}

	fun onCarDataChanged(data: CarData) {
		if (routingInProgress) return
		val previous = carData
		carData = data

		if (isDriveModeEnabled()) {
			maxSpeed = when (carData.drivingMode) {
				DrivingMode.COMFORT.raw -> getAppSettingDouble(AppSettings.KEYS.EVPLANNING_MAXSPEED_COMFORT)
				DrivingMode.ECOPRO.raw -> getAppSettingDouble(AppSettings.KEYS.EVPLANNING_MAXSPEED_ECO_PRO)
				DrivingMode.ECOPRO_PLUS.raw -> getAppSettingDouble(AppSettings.KEYS.EVPLANNING_MAXSPEED_ECO_PRO_PLUS)
				DrivingMode.SPORT.raw -> getAppSettingDouble(AppSettings.KEYS.EVPLANNING_MAXSPEED_SPORT)
				else -> null
			}
		}

		val previousDestinations = setOf(
			previous.nextDestination,
			previous.finalDestination,
		).filter { it.isValid() }
		val newDestinations = setOf(
			carData.nextDestination,
			carData.finalDestination,
		).filter { it.isValid() }

		var existingWaypointIndices: List<List<Int>>? = null
		val routes = planResult?.result?.routes

		if (newDestinations.isEmpty()) {
			if (previousDestinations.isNotEmpty()) {
				resetPlanning()
			} // else ignore if both are empty
		} else { // new is not empty
			if (routes == null) {
				// no preexisting routing result:
				shouldReplan = true
			} else if (newDestinations.last() != previousDestinations.lastOrNull()) {
				// final destination has changed:
				shouldReplan = true
			} else {
				// final destination is unchanged and next destination exists:
				existingWaypointIndices =
					getExistingWaypointIndices(newDestinations, routes, MAX_WAYPOINT_DISTANCE)
				if (existingWaypointIndices.isEmpty()) {
					// next destination does not belong to an existing route
					shouldReplan = true
				}
			}
		}
		routingDataListener.onRoutingDataChanged(
			RoutingData(
				carData = carData, //TODO: carData will most likely be obsolete, currently used for debugging only
				routeDistances = calcRouteDistances(carData.position, routes),
				existingWaypointIndices,
			)
		)
		if (shouldReplan && isReplanEnabled()) {
			planNew()
		}
	}

	fun isChangedForReplan(old: Any?, new: Any?): Boolean {
		if (old?.equals(new) ?: (new == null)) {
			return false
		}
		shouldReplan = true
		return true
	}

	fun onCreate(context: Context, handler: Handler) {
		this.handler = handler
		appSettings = MutableAppSettingsReceiver(context, handler)
		appSettings?.callback = this::onAppSettingsChanged
	}

	fun onDestroy(context: Context) {
		appSettings?.callback = null
		handler = null
	}

	fun isReplanEnabled() = getAppSettingBoolean(AppSettings.KEYS.EVPLANNING_AUTO_REPLAN)

	fun isDriveModeEnabled() =
		getAppSettingBoolean(AppSettings.KEYS.EVPLANNING_MAXSPEED_DRIVEMODE_ENABLE)

	private fun getAppSettingBoolean(setting: AppSettings.KEYS): Boolean {
		return appSettings?.get(setting)?.toBoolean() == true
	}

	private fun getAppSettingDouble(setting: AppSettings.KEYS): Double? {
		return try {
			appSettings?.get(setting)?.toDouble()
		} catch (e: NumberFormatException) {
			null
		}
	}

	private fun getAppSettingInt(setting: AppSettings.KEYS): Int? {
		return try {
			appSettings?.get(setting)?.toInt()
		} catch (e: NumberFormatException) {
			null
		}
	}

	fun onAppSettingsChanged() {
		if (!isDriveModeEnabled()) {
			maxSpeed = getAppSettingDouble(AppSettings.KEYS.EVPLANNING_MAXSPEED)
		}
		referenceConsumption = getAppSettingInt(AppSettings.KEYS.EVPLANNING_REFERENCE_CONSUMPTION)

		if (shouldReplan && isReplanEnabled()) {
			planNew()
		}
	}

	private var error: String? = null
		set(value) {
			field = value
			if (value != null) {
				routingDataListener.onPlanningError(value)
			}
			EVPlanningDataViewModel.setError(value ?: "no error")
		}

	private var planResult: PlanResult? = null
		set(value) {
			field = value
			routingDataListener.onPlanChanged(value?.result)
		}

	private var nextChargerResult: PlanResult? = null
		set(value) {
			field = value
			routingDataListener.onNextChargerPlanChanged(value?.result)
		}

	private fun triggerPlanning(
		find_alts: Boolean? = null,
		find_next_charger_alts: Boolean? = null,
		plan_uuid: String? = null
	) {
		//TODO remove, only for debugging:
		EVPlanningDataViewModel.setMaxSpeed(maxSpeed)
		setOf(
			carData.position,
			carData.nextDestination,
			carData.finalDestination
		).filter { it.isValid() }
			.takeIf { it.size > 1 }
			?.mapIndexed { index, it ->
				Destination(
					it.latitude,
					it.longitude,
					is_my_pos = if (index == 0) true else null
				)
			}
			?.let { destinations ->
				routingInProgress = true
				routingDataListener.onPlanningTriggered()
				shouldReplan = false
				planning.plan(
					PlanRequest(
						car_model = carModel,
						destinations = destinations,
						initial_soc_perc = carData.soc,
						max_speed = maxSpeed,
						ref_consumption = referenceConsumption,
						outside_temp = carData.externalTemperature.toDouble(),
						find_alts = find_alts,
						find_next_charger_alts = find_next_charger_alts,
						plan_uuid = plan_uuid
					),
					{
						if (find_next_charger_alts == true) {
							handler?.post {
								nextChargerResult = it
								error = null
								routingInProgress = false
							}
						} else {
							handler?.post {
								nextChargerResult = null
								planResult = it
								error = null
								routingInProgress = false
							}
						}
					},
					{
						handler?.post {
							error = it
							routingInProgress = false
						}
					})
			}
	}

	class PositionRuler private constructor(
		private val point: Point,
		private val ruler: CheapRuler
	) {

		companion object {
			fun of(position: Position): PositionRuler? {
				return if (position.isValid()) {
					PositionRuler(
						Point(position.latitude, position.longitude),
						CheapRuler.fromLatitude(position.latitude, Units.METRES)
					)
				} else null
			}
		}

		fun squareDistance(other: Step): Double {
			return ruler.squareDistance(point, Point(other.lat, other.lon))
		}

		fun squareDistance(other: PathStep): Double {
			return ruler.squareDistance(point, Point(other.lat, other.lon))
		}

		fun pointToSegmentDistance(s1: Step, s2: Step): Double {
			return ruler.pointToSegmentDistance(point, Point(s1.lat, s1.lon), Point(s2.lat, s2.lon))
		}
	}

	class PathStepDistance(val stepIndex: Int, val pathStepIndex: Int, val distance: Double)
	class StepSegmentDistance(val stepIndex: Int, val distance: Double)
	class StepDistance(val stepIndex: Int, val distance: Double)

	class RouteDistance private constructor(
		val routeIndex: Int,
		val stepIndex: Int,
		val pathStepIndex: Int?,
		val distance: Double
	) {
		companion object {
			fun fromPathStep(routeIndex: Int, pathStepDistance: PathStepDistance): RouteDistance {
				return RouteDistance(
					routeIndex,
					pathStepDistance.stepIndex,
					pathStepDistance.pathStepIndex,
					pathStepDistance.distance
				)
			}

			fun fromStepSegment(
				routeIndex: Int,
				stepSegmentDistance: StepSegmentDistance
			): RouteDistance {
				return RouteDistance(
					routeIndex,
					stepSegmentDistance.stepIndex,
					null,
					stepSegmentDistance.distance
				)
			}
		}
	}

	enum class DrivingMode(val raw: Int) {
		COMFORT(2),
		COMFORT_PLUS(9),
		BASIC(3),
		SPORT(4),
		SPORT_PLUS(5),
		RACE(6),
		ECOPRO(7),
		ECOPRO_PLUS(8),
	}

	companion object {

		const val MAX_WAYPOINT_DISTANCE = 50.0

		fun calcRouteDistances(position: Position, routes: List<Route>?): List<RouteDistance>? {
			return PositionRuler.of(position)?.let { ruler ->
				routes?.mapIndexed { index, route ->
					closestPathStep(ruler, route)?.let { RouteDistance.fromPathStep(index, it) }
						?: closestStepSegment(ruler, route)?.let { fromStepSegment(index, it) }
				}?.filterNotNull()
			}
		}

		fun closestPathStep(ruler: PositionRuler, route: Route): PathStepDistance? {
			return route.steps?.mapIndexed { stepIndex, step ->
				step.path?.mapIndexed { pathStepIndex, pathStep ->
					PathStepDistance(stepIndex, pathStepIndex, ruler.squareDistance(pathStep))
				}?.minWithOrNull { o1, o2 ->
					o1.distance.compareTo(o2.distance)
				}
			}?.filterNotNull()?.minWithOrNull { o1, o2 ->
				o1.distance.compareTo(o2.distance)
			}?.let {
				PathStepDistance(it.stepIndex, it.pathStepIndex, sqrt(it.distance))
			}
		}

		fun closestStepSegment(ruler: PositionRuler, route: Route): StepSegmentDistance? {

			class Previous(
				val step: Step? = null,
				val stepSegmentDistance: StepSegmentDistance? = null
			)

			return route.steps?.foldRightIndexed(Previous(), { index, step, previous ->
				if (previous.step == null) {
					Previous(step, null)
				} else {
					val distance = ruler.pointToSegmentDistance(step, previous.step)
					Previous(
						step,
						if (previous.stepSegmentDistance == null || previous.stepSegmentDistance.distance > distance) {
							StepSegmentDistance(index, distance)
						} else {
							previous.stepSegmentDistance
						}
					)
				}
			})?.stepSegmentDistance
		}

		fun getExistingWaypointIndices(
			positions: List<Position>,
			routes: List<Route>,
			maxDistance: Double
		): List<List<Int>> {
			val maxSquareDistance = maxDistance.times(maxDistance)
			return positions.map { position ->
				PositionRuler.of(position)?.let { ruler ->
					routes.map { route ->
						route.steps?.mapIndexed { stepIndex, step ->
							StepDistance(stepIndex, ruler.squareDistance(step))
						}?.minWithOrNull { o1, o2 ->
							o1.distance.compareTo(o2.distance)
						}?.let {
							if (it.distance < maxSquareDistance) {
								it.stepIndex
							} else null
						}
					}
				}
			}.let { result ->
				routes.mapIndexed { routeIndex, _ ->
					positions.mapIndexed { positionIndex, _ ->
						result[positionIndex]?.get(routeIndex)
					}.filterNotNull()
				}
			}
		}
	}
}
