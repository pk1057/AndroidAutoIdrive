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

package me.hufman.androidautoidrive.carapp.evplanning

import me.hufman.androidautoidrive.CarThread
import me.hufman.androidautoidrive.evplanning.*
import me.hufman.androidautoidrive.evplanning.iternio.entity.Plan
import me.hufman.androidautoidrive.evplanning.iternio.entity.Route
import me.hufman.androidautoidrive.evplanning.iternio.entity.Step
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningDataViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/*
 * The purpose of the NavigationModelUpdater is to post updates
 * of aggregated data from RoutingService to the carapp model
 * on the carapp's thread.
 */
class NavigationModelUpdater() {

	var threadCarApp: CarThread? = null
	var navigationModelController: NavigationModelController? = null
	var existingPlan: Plan? = null
	var nextChargerPlan: Plan? = null

	val routingDataListener = object : RoutingDataListener {

		override fun onRoutingDataChanged(routingData: RoutingData) {

			//TODO: for debugging only, remove later:
			EVPlanningDataViewModel.setRoutingData(routingData)

			val waypoints = routingData.routeData?.let { routeData ->
				nextChargerPlan?.routes?.let {
					parseRouteData(it, routeData)
				}
			}?.let { extractNextChargerWaypoints(it) }
			threadCarApp?.post {
				navigationModelController?.setNextChargerWaypoints(waypoints)
			}
			val displayRoutes = routingData.routeData?.let { routeData ->
				existingPlan?.routes?.let { parseRouteData(it, routeData) }
			}
			threadCarApp?.post {
				navigationModelController?.setDisplayRoutes(displayRoutes)
			}
		}

		override fun onPlanChanged(plan: Plan?) {
			existingPlan = plan
			nextChargerPlan = null
			threadCarApp?.post {
				navigationModelController?.planningFinished()
			}
		}

		override fun onNextChargerPlanChanged(plan: Plan?) {
			nextChargerPlan = plan
			threadCarApp?.post {
				navigationModelController?.nextChargerFinished()
			}
		}

		override fun onPlanningTriggered() {
			threadCarApp?.post {
				navigationModelController?.planningTriggered()
			}
		}

		override fun onPlanningError(msg: String) {
			threadCarApp?.post {
				navigationModelController?.planningError(msg)
			}
		}
	}

	fun onCreateCarApp(threadCarApp: CarThread?) {
		this.threadCarApp = threadCarApp
	}

	fun onDestroy() {
		this.threadCarApp = null
		this.navigationModelController = null
	}

	companion object {

		val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
		val NAME_MATCHER = Regex("^(.*[^\\s])\\s*\\[([^\\[]*)\\]\$")

		class Acc(
			var previous: Step? = null,
			val displayWaypoints: MutableList<DisplayWaypoint> = mutableListOf(),
			var arrival_duration: Int? = null,
			var num_charges: Int = 0,
			var charge_duration: Int = 0,
			var trip_dst: Int? = null,
		)

		fun extractNextChargerWaypoints(displayRoutes: List<DisplayRoute>): List<DisplayWaypoint> {
			val min_duration = displayRoutes.map { it.arrival_duration }.filterNotNull().minOrNull()
			val min_distance = displayRoutes.map { it.trip_dst }.filterNotNull().minOrNull()
			return displayRoutes.map { displayRoute ->
				displayRoute.displayWaypoints?.firstOrNull()?.let {
					if (it.is_initial_charger) {
						displayRoute.displayWaypoints.getOrNull(1)
					} else {
						it
					}
				}?.let {
					DisplayWaypoint(
						icon = it.icon,
						title = it.title,
						operator = it.operator,
						charger_type = it.charger_type,
						is_waypoint = it.is_waypoint,
						is_initial_charger = it.is_initial_charger,
						address = it.address,
						trip_dst = it.trip_dst,
						step_dst = it.step_dst,
						soc_ariv = it.soc_ariv,
						soc_dep = it.soc_dep,
						eta = it.eta,
						etd = it.etd,
						duration = it.duration,
						num_chargers = it.num_chargers,
						free_chargers = it.free_chargers,
						delta_duration = min_duration?.let { min_dur ->
							displayRoute.arrival_duration?.minus(
								min_dur
							)
						},
						delta_dst = min_distance?.let { min_dst ->
							displayRoute.trip_dst?.minus(
								min_dst
							)
						},
						final_num_charges = displayRoute.num_charges,
						lat = it.lat,
						lon = it.lon,
					)
				}
			}.filterNotNull()
		}

		fun parseRouteData(routes: List<Route>, routeData: List<RouteData>): List<DisplayRoute>? {
			val now = LocalDateTime.now()
			return routeData.filterNot {
				it.stepIndex == null
			}.mapNotNull { data ->
				routes.getOrNull(data.routeIndex)?.let { route ->
					val (start_dist, start_time) = deriveStartDistanceTime(data, route)
					route.steps?.filterIndexed { index, _ ->
						index >= data.stepIndex!!
					}?.let { steps ->
						accumulateDisplayData(
							steps,
							data.pathStepIndex,
							data.existingWaypointIndices.map { it - data.stepIndex!! },
							start_time,
							start_dist,
							now
						)
					}?.let {
						DisplayRoute(
							trip_dst = it.trip_dst,
							arrival_duration = it.arrival_duration,
							num_charges = it.num_charges,
							charge_duration = it.charge_duration,
							deviation = data.distance?.toInt(),
							contains_waypoint = it.displayWaypoints.filter { waypoint -> waypoint.is_waypoint }.size > 1,
							displayWaypoints = it.displayWaypoints
						)
					}
				}
			}
		}

		private fun deriveStartDistanceTime(
			data: RouteData,
			route: Route
		) = data.stepIndex?.let { stepIndex ->
			data.pathStepIndex?.let {
				route.steps?.getOrNull(stepIndex)?.path?.getOrNull(it)
			}?.let {
				Pair(it.remaining_dist?.times(1000)?.toInt(), it.remaining_time)
			} ?: route.steps?.getOrNull(stepIndex)?.let {
				Pair(it.departure_dist, it.departure_duration)
			}
		} ?: route.steps?.firstOrNull()?.let {
			Pair(it.departure_dist, it.departure_duration)
		} ?: Pair(null, null)

		private fun accumulateDisplayData(
			steps: Iterable<Step>,
			pathStepIndex: Int?,
			wayPointIndices: List<Int>?,
			start_time: Int?,
			start_dist: Int?,
			now: LocalDateTime
		) = steps.foldIndexed(Acc(), { index, acc, step ->
			// include starting-point only if it's a charger and it is very close:
			if (index > 0 || step.is_charger == true && (pathStepIndex == null || pathStepIndex < 2)) {

				// on return trip_dst and arrival_duration in accumulator belong to last waypoint:
				acc.trip_dst = step.departure_dist?.let { start_dist?.minus(it) }

				acc.arrival_duration = when (index) {
					0 -> 0
					else -> step.arrival_duration?.let { start_time?.minus(it) }
				}

				val eta = acc.arrival_duration?.let { now.plusSeconds(it.toLong()) }

				val nameParts = step.name?.let { NAME_MATCHER.matchEntire(it) }?.groupValues
				val operator = nameParts?.getOrNull(2)

				if (step.is_charger == true) {
					acc.num_charges++
					step.charge_duration?.let { acc.charge_duration += it }
				}

				val (numChargers, freeChargers) = step.charger_type?.let {
					if (it.equals("0")) null else it
				}?.let { chargerType ->
					step.charger?.outlets?.filter {
						chargerType.equals(it.type)
					}?.let {
						Pair(it.size, it.count { "OPERATIONAL".equals(it.status) })
					}
				} ?: Pair(Int.MIN_VALUE, Int.MIN_VALUE)

				acc.displayWaypoints.add(DisplayWaypoint(
					title = nameParts?.getOrNull(1)?.takeIf { it.isNotEmpty() }
						?: step.name?.takeIf { it.isNotEmpty() },
					operator = step.charger?.network_name
						?: operator?.takeIf { it.isNotEmpty() },
					charger_type = step.charger_type?.let {
						if (it.equals("0")) null else it.toUpperCase(
							Locale.getDefault()
						)
					},
					is_waypoint = wayPointIndices?.contains(index) == true,
					is_initial_charger = (index == 0),
					address = step.charger?.address
						?: String.format("%.5f %.5f", step.lat, step.lon),
					trip_dst = acc.trip_dst,
					step_dst = when (index) {
						0 -> step.departure_dist?.let { start_dist?.minus(it) }
						1 -> step.arrival_dist?.let { start_dist?.minus(it) }
						else -> step.arrival_dist?.let {
							acc.previous?.departure_dist?.minus(
								it
							)
						}
							?: acc.previous?.drive_dist
					},
					soc_ariv = step.arrival_perc,
					soc_dep = if (step.is_charger == true) {
						step.departure_perc
					} else null,
					eta = eta,
					etd = if (step.is_charger == true) {
						step.charge_duration?.let { eta?.plusSeconds(it.toLong()) }
					} else null,
					duration = if (step.is_charger == true) {
						step.charge_duration?.div(60)
					} else null,
					num_chargers = numChargers,
					free_chargers = freeChargers,
					lat = step.lat,
					lon = step.lon,
				))
			}
			acc.previous = step
			acc
		})

		fun formatDistance(dist: Int) = if (dist < 10000) {
			String.format("%.1f", dist.div(1000.0))
		} else {
			dist.div(1000).toString()
		} + "km"

		fun formatDistanceDetailed(dist: Int) = when {
			dist < 1000 -> {
				String.format("%d", dist) + "m"
			}
			dist < 10000 -> {
				String.format("%.1f", dist.div(1000.0)) + "km"
			}
			else -> String.format("%d", dist.div(1000)) + "km"
		}

		fun formatTime(seconds: Int): String {
			val hours = seconds.div(3600).toString()
			val minutes = String.format("%02d", seconds.rem(3600).div(60))
			return "${hours}:${minutes}"
		}

		fun formatTimeDifference(seconds: Int): String {
			val hours = seconds.div(3600)
			val minutes = seconds.rem(3600).div(60)
			return if (hours > 0) {
				"${hours}:${String.format("%02d", minutes)}h"
			} else {
				"${minutes}min"
			}
		}
	}
}