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
import me.hufman.androidautoidrive.carapp.evplanning.PositionDetailedInfo
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningDataViewModel
import kotlin.math.sqrt

data class RoutingData(
	val carData: CarData,
	val routeData: List<RouteData>?,
	val shouldReplan: Boolean
)

data class DestinationInfo(
	val position: Position,
	val details: PositionDetailedInfo
)

data class WaypointIndexInfo(
	val index: Int,
	val info: DestinationInfo,
)

data class RouteData(
	val routeIndex: Int,
	val stepIndex: Int? = null,
	val pathStepIndex: Int? = null,
	val distance: Double? = null,
	val soc_ariv: Double? = null,
	val waypointIndexInfos: List<WaypointIndexInfo>
) {
	companion object {
		fun of(
			position: Position,
			soc_car: Double,
			destinations: List<DestinationInfo>,
			routes: List<Route>
		): List<RouteData> {
			val positionRuler = RoutingService.PositionRuler.of(position)
			val destinationsRuler =
				destinations.map { RoutingService.PositionRuler.of(it.position)?.let { ruler ->
					Pair(ruler,it)
				} }.filterNotNull()

			return routes.mapIndexed { routeIndex, route ->
				val waypointIndexInfos = RoutingService.getMatchingIndexInfos(
					destinationsRuler,
					route,
					RoutingService.MAX_WAYPOINT_SQUARE_DISTANCE
				)
				positionRuler?.let { ruler ->
					RoutingService.closestPathStepTriple(ruler, route)?.let {
						val (stepIndex, pathStepIndex, distance) = it
						val soc_ariv =
							route.steps!![stepIndex].path!![pathStepIndex].soc_perc?.let { soc_perc ->
								soc_car.minus(soc_perc)
							}?.let {
								route.steps.getOrNull(stepIndex + 1)?.arrival_perc?.plus(it)
							}
						RouteData(
							routeIndex = routeIndex,
							stepIndex = stepIndex,
							pathStepIndex = pathStepIndex,
							distance = distance,
							soc_ariv = soc_ariv,
							waypointIndexInfos = waypointIndexInfos,
						)
					} ?: RoutingService.closestStepSegmentPair(ruler, route)?.let {
						RouteData(
							routeIndex = routeIndex,
							stepIndex = it.first,
							distance = it.second,
							waypointIndexInfos = waypointIndexInfos,
						)
					}
				} ?: RouteData(
					routeIndex = routeIndex,
					waypointIndexInfos = waypointIndexInfos,
				)
			}
		}
	}
}

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

	private var minSocCharger: Double? = null
		set(value) {
			if (isChangedForReplan(field, value)) {
				field = value
			}
		}

	private var minSocDestination: Double? = null
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
		val newDestinationInfos = setOf(
			DestinationInfo(carData.nextDestination,carData.nextDestinationDetails),
			DestinationInfo(carData.finalDestination,carData.finalDestinationDetails),
		).filter { it.position.isValid() }

		val routes = planResult?.result?.routes

		if (newDestinationInfos.isEmpty()) {
			if (previousDestinations.isNotEmpty()) {
				resetPlanning()
			} // else ignore if both are empty
		} else { // new is not empty
			if (routes == null) {
				// no preexisting routing result:
				shouldReplan = true
			} else if (newDestinationInfos.last().position != previousDestinations.lastOrNull()) {
				// TODO: suggest replan if nextDestination does not match any existing waypoint
				// final destination has changed:
				shouldReplan = true
			}
		}

		val routeData = routes?.let { routesList ->
			RouteData.of(carData.position, carData.soc, newDestinationInfos, routesList)
				.also { routeDataList ->
					// none of the new destinations matches an existing routepoint -> replan
					shouldReplan =
						shouldReplan || routeDataList.all { it.waypointIndexInfos.isEmpty() }

					shouldReplan = shouldReplan || routeDataList.filter {
						it.distance == null ||
								it.distance < MAX_STEP_OFFSET
					}.let {
						it.isEmpty() || //no route is close -> replan
								it.filter {
									it.soc_ariv != null &&
											it.stepIndex != null
								}.let {
									it.isNotEmpty() && // there are close routes and for all of them the soc_ariv is lower then the minimum -> replan
											it.all {
												if (it.waypointIndexInfos.lastOrNull()
														?.index?.equals(it.stepIndex!! + 1) == true
												) {
													minSocDestination
												} else {
													minSocCharger
												}?.let { minSoc ->
													it.soc_ariv!! < minSoc
												} ?: (it.soc_ariv!! < 0.0)
											}
								}
					}
				}
		}

		routingDataListener.onRoutingDataChanged(
			RoutingData(
				carData,
				routeData,
				shouldReplan,
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
		minSocCharger = getAppSettingDouble(AppSettings.KEYS.EVPLANNING_MIN_SOC_CHARGER)
		minSocDestination = getAppSettingDouble(AppSettings.KEYS.EVPLANNING_MIN_SOC_FINAL)

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
						charger_soc_perc = minSocCharger,
						arrival_soc_perc = minSocDestination,
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

		const val MAX_WAYPOINT_SQUARE_DISTANCE = 50.0 * 50.0
		const val MAX_STEP_OFFSET = 500

		// returns triple of stepIndex, pathStepIndex and distance
		fun closestPathStepTriple(ruler: PositionRuler, route: Route): Triple<Int, Int, Double>? {
			return route.steps?.mapIndexedNotNull { stepIndex, step ->
				step.path?.mapIndexed { pathStepIndex, pathStep ->
					Triple(stepIndex, pathStepIndex, ruler.squareDistance(pathStep))
				}?.minWithOrNull { o1, o2 ->
					o1.third.compareTo(o2.third)
				}
			}?.minWithOrNull { o1, o2 ->
				o1.third.compareTo(o2.third)
			}?.let {
				Triple(it.first, it.second, sqrt(it.third))
			}
		}

		// returns pair of stepIndex and distance
		fun closestStepSegmentPair(ruler: PositionRuler, route: Route): Pair<Int, Double>? {
			return route.steps?.foldRightIndexed(
				Pair<Step?, Pair<Int, Double>?>(null, null),
				{ stepIndex, step, previous ->
					if (previous.first == null) {
						Pair(step, null)
					} else {
						ruler.pointToSegmentDistance(step, previous.first!!).let { distance ->
							Pair(
								step,
								if (previous.second == null || previous.second!!.second > distance) {
									Pair(stepIndex, distance)
								} else {
									previous.second
								}
							)
						}
					}
				})?.second
		}

		fun getMatchingIndexInfos(
			rulerInfoPairs: List<Pair<PositionRuler,DestinationInfo>>,
			route: Route,
			maxSquareDistance: Double
		): List<WaypointIndexInfo> {
			return rulerInfoPairs.mapNotNull { rulerInfoPair ->
				route.steps?.mapIndexed { stepIndex, step ->
					Triple(stepIndex, rulerInfoPair.first.squareDistance(step),rulerInfoPair.second)
				}?.filter {
					it.second < maxSquareDistance
				}?.minWithOrNull { o1, o2 ->
					o1.second.compareTo(o2.second)
				}?.let { WaypointIndexInfo(it.first,it.third) }
			}
		}
	}
}
