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
import me.hufman.androidautoidrive.evplanning.iternio.Planning
import me.hufman.androidautoidrive.evplanning.iternio.dto.Destination
import me.hufman.androidautoidrive.evplanning.iternio.dto.PlanRequest
import me.hufman.androidautoidrive.evplanning.iternio.entity.*
import me.hufman.androidautoidrive.evplanning.util.CheapRuler
import me.hufman.androidautoidrive.evplanning.util.Point
import me.hufman.androidautoidrive.evplanning.util.Units
import me.hufman.androidautoidrive.carapp.evplanning.Position
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningDataViewModel
import kotlin.math.sqrt

data class RoutingData(val carData: CarData, val plan: Plan?, val routeDistance: RoutingService.RouteDistance?, val trip: Int?)

interface RoutingDataListener {
	fun onRoutingDataChanged(routingData: RoutingData)
}

class RoutingService(private val planning: Planning, private val routingDataListener: RoutingDataListener) {

	var handler: Handler? = null

	private var carData = CarData()
		set(value) {
			val previousNextDestination = field.nextDestination
			val previousFinalDestination = field.finalDestination
			field = value
			if (!(value.position.isValid() && (value.finalDestination.isValid() || value.nextDestination.isValid()))) {
				resetPlanning()
			} else {
				if (value.finalDestination != previousFinalDestination || value.nextDestination != previousNextDestination) {
					triggerPlanning()
					return
				}
			}
			calcValues()
		}

	private var currentRoute = Int.MIN_VALUE

	fun setCurrentRoute(current: Int) {
		if (currentRoute != current) {
			currentRoute = current
			calcValues()
		}
	}

	fun onCarDataChanged(data: CarData) {
		carData = data
	}

	fun onCreate(context: Context, handler: Handler) {
		this.handler = handler
	}

	fun onDestroy(context: Context) {
		handler = null
	}

	var carModel = "bmw:i3:19:38:other"

	private var error: String? = null
		set(value) {
			field = value
			EVPlanningDataViewModel.setError(value ?: "no error")
		}

	private var odometerstart: Int? = null

	private var planResult: PlanResult? = null

	private fun resetPlanning() {
		planResult = null
		currentRoute = Int.MIN_VALUE
		odometerstart = null
		error = null
		calcValues()
	}

	private fun triggerPlanning() {
		odometerstart = carData.odometer
		listOf(
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
					planning.plan(
							PlanRequest(
									car_model = carModel,
									destinations = destinations,
									initial_soc_perc = carData.soc,
									find_alts = true,
									outside_temp = carData.externalTemperature.toDouble(),
							),
							{
								handler?.post {
									planResult = it
									currentRoute = 0
									error = null
									calcValues()
								}
							},
							{
								handler?.post {
									error = it
								}
							})
				}
	}

	private fun calcValues() {
		val routeDistance = planResult?.result?.let { plan ->
			PositionRuler.of(carData.position)?.let { ruler ->
				plan.routes?.getOrNull(currentRoute)?.let { route ->
					closestStepByPath(ruler, route)?.let { RouteDistance.fromStep(currentRoute, it) }
				}
						?: closestRoute(ruler, plan)
			}
		}
		currentRoute = routeDistance?.routeIndex ?: Int.MIN_VALUE
		routingDataListener.onRoutingDataChanged(RoutingData(
				carData = carData,
				plan = planResult?.result,
				routeDistance = routeDistance,
				trip = odometerstart?.let { carData.odometer - it }
		))
	}

	class PositionRuler private constructor(private val point: Point, private val ruler: CheapRuler) {

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

		fun squareDistance(other: PathStep): Double {
			return ruler.squareDistance(point, Point(other.lat, other.lon))
		}

		fun pointToSegmentDistance(s1: Step, s2: Step): Double {
			return ruler.pointToSegmentDistance(point, Point(s1.lat, s1.lon), Point(s2.lat, s2.lon))
		}
	}

	class PathStepDistance(val pathStepIndex: Int, val distance: Double)
	class StepDistance private constructor(val stepIndex: Int, val pathStepIndex: Int? = null, val distance: Double) {
		companion object {
			fun fromStep(index: Int, distance: Double): StepDistance {
				return StepDistance(index, distance = distance)
			}

			fun fromPath(index: Int, pathStepDistance: PathStepDistance): StepDistance {
				return StepDistance(index, pathStepDistance.pathStepIndex, pathStepDistance.distance)
			}
		}
	}

	class RouteDistance private constructor(val routeIndex: Int, val stepIndex: Int, val pathStepIndex: Int?, val distance: Double) {
		companion object {
			fun fromStep(index: Int, stepDistance: StepDistance): RouteDistance {
				return RouteDistance(index, stepDistance.stepIndex, stepDistance.pathStepIndex, stepDistance.distance)
			}
		}
	}

	private fun closestRoute(ruler: PositionRuler, plan: Plan): RouteDistance? {
		return plan.routes?.mapIndexed { index, route ->
			(closestStepByPath(ruler, route)
					?: closestStepByStep(ruler, route))
					?.let { RouteDistance.fromStep(index, it) }
		}?.filterNotNull()?.minWithOrNull { o1, o2 ->
			o1.distance.compareTo(o2.distance)
		}
	}

	private fun closestStepByPath(ruler: PositionRuler, route: Route): StepDistance? {

		return route.steps?.mapIndexed { index, step ->
			closestPathStep(ruler, step)?.let { StepDistance.fromPath(index, it) }
		}?.filterNotNull()?.minWithOrNull { o1, o2 ->
			o1.distance.compareTo(o2.distance)
		}
	}

	private fun closestStepByStep(ruler: PositionRuler, route: Route): StepDistance? {

		class Previous(val step: Step? = null, val stepDistance: StepDistance? = null)

		return route.steps?.foldRightIndexed(Previous(), { index, step, previous ->
			if (previous.step == null) {
				Previous(step, null)
			} else {
				val distance = ruler.pointToSegmentDistance(step, previous.step)
				Previous(
						step,
						if (previous.stepDistance == null || previous.stepDistance.distance > distance) {
							StepDistance.fromStep(index, distance)
						} else {
							previous.stepDistance
						}
				)
			}
		})?.stepDistance
	}

	private fun closestPathStep(ruler: PositionRuler, step: Step): PathStepDistance? {
		return step.path?.mapIndexed { index, pathStep ->
			PathStepDistance(index, ruler.squareDistance(pathStep))
		}?.minWithOrNull { o1, o2 -> o1.distance.compareTo(o2.distance) }?.let {
			PathStepDistance(it.pathStepIndex, sqrt(it.distance))
		}
	}
}