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

import me.hufman.androidautoidrive.carapp.evplanning.Position
import me.hufman.androidautoidrive.carapp.evplanning.PositionDetailedInfo
import me.hufman.androidautoidrive.evplanning.iternio.entity.PathStep
import me.hufman.androidautoidrive.evplanning.iternio.entity.Route
import me.hufman.androidautoidrive.evplanning.iternio.entity.Step
import me.hufman.androidautoidrive.evplanning.util.CheapRuler
import me.hufman.androidautoidrive.evplanning.util.Point
import me.hufman.androidautoidrive.evplanning.util.Units
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
	val positionRouteData: PositionRouteData? = null,
	val soc_ariv: Double? = null,
	val waypointIndexInfos: List<WaypointIndexInfo>
) {
	companion object {

		const val MAX_WAYPOINT_SQUARE_DISTANCE = 50.0 * 50.0
		const val MAX_STEP_OFFSET = 500

		fun of(
			position: Position,
			soc_car: Double,
			destinations: List<DestinationInfo>,
			routes: List<Route>
		): List<RouteData> {
			val positionRuler = PositionRuler.of(position)
			val destinationsRuler =
				destinations.map {
					PositionRuler.of(it.position)?.let { ruler ->
						Pair(ruler, it)
					}
				}.filterNotNull()

			return routes.mapIndexed { routeIndex, route ->
				val waypointIndexInfos = getMatchingIndexInfos(
					destinationsRuler,
					route,
					MAX_WAYPOINT_SQUARE_DISTANCE
				)
				positionRuler?.let { ruler ->
					closestPathStepTriple(ruler, route)?.let {
						val (stepIndex, pathStepIndex, distance) = it
						val soc_ariv =
							route.steps!![stepIndex].path!![pathStepIndex].soc_perc?.let { soc_perc ->
								soc_car.minus(soc_perc)
							}?.let {
								route.steps.getOrNull(stepIndex + 1)?.arrival_perc?.plus(it)
							}
						RouteData(
							routeIndex = routeIndex,
							PositionRouteData(
								stepIndex = stepIndex,
								pathStepIndex = pathStepIndex,
								distance = distance.toInt(),
							),
							soc_ariv = soc_ariv,
							waypointIndexInfos = waypointIndexInfos,
						)
					} ?: closestStepSegmentPair(ruler, route)?.let {
						RouteData(
							routeIndex = routeIndex,
							PositionRouteData(
								stepIndex = it.first,
								distance = it.second.toInt(),
							),
							waypointIndexInfos = waypointIndexInfos,
						)
					}
				} ?: RouteData(
					routeIndex = routeIndex,
					waypointIndexInfos = waypointIndexInfos,
				)
			}
		}

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
			rulerInfoPairs: List<Pair<PositionRuler, DestinationInfo>>,
			route: Route,
			maxSquareDistance: Double
		): List<WaypointIndexInfo> {
			return rulerInfoPairs.mapNotNull { rulerInfoPair ->
				route.steps?.mapIndexed { stepIndex, step ->
					Triple(stepIndex, rulerInfoPair.first.squareDistance(step), rulerInfoPair.second)
				}?.filter {
					it.second < maxSquareDistance
				}?.minWithOrNull { o1, o2 ->
					o1.second.compareTo(o2.second)
				}?.let { WaypointIndexInfo(it.first, it.third) }
			}
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
}

data class PositionRouteData(
	val stepIndex: Int? = null,
	val pathStepIndex: Int? = null,
	val distance: Int? = null,
) {
	companion object {
		fun of(
			ruler: RouteData.PositionRuler,
			route: Route
		): PositionRouteData? {
			return RouteData.closestPathStepTriple(ruler, route)?.let {
				PositionRouteData(
					stepIndex = it.first,
					pathStepIndex = it.second,
					distance = it.third.toInt(),
				)
			} ?: RouteData.closestStepSegmentPair(ruler, route)?.let {
				PositionRouteData(
					stepIndex = it.first,
					distance = it.second.toInt(),
				)
			}
		}

		fun routeIndexDistanceMapOf(
			one: Map<Int, PositionRouteData>,
			other: Map<Int, PositionRouteData>,
			routes: List<Route>,
		): Map<Int, Int> {

			fun remainingDist(steps: List<Step>, data: PositionRouteData): Int? {
				return data.stepIndex?.let { stepIndex ->
					steps.getOrNull(stepIndex)
				}?.let { step ->
					data.pathStepIndex?.let { pathIndex ->
						step.path?.getOrNull(pathIndex)
					}?.remaining_dist?.toInt()
						?: step.departure_dist
				}
			}

			return routes
				.asSequence()
				.mapIndexed route@{ routeIndex, route ->

					val steps = route.steps ?: return@route null

					val oneData = one[routeIndex] ?: return@route null
					val oneRemain = remainingDist(steps, oneData) ?: return@route null

					val otherData = other[routeIndex] ?: return@route null
					val otherRemain = remainingDist(steps, otherData) ?: return@route null

					return@route otherRemain.minus(oneRemain).let {
						if (it >= 0) {
							routeIndex to it.plus(oneData.distance ?: 0)
								.plus(otherData.distance ?: 0)
						} else {
							routeIndex to it.minus(oneData.distance ?: 0)
								.minus(otherData.distance ?: 0)
						}
					}
				}
				.filterNotNull()
				.toMap()
		}
	}
}