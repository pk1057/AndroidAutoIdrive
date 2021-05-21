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
import me.hufman.androidautoidrive.evplanning.iternio.entity.Step
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningDataViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NavigationModelUpdater() {

	var threadCarApp: CarThread? = null
	var navigationModel: NavigationModel? = null

	val routingDataListener = object : RoutingDataListener {
		override fun onRoutingDataChanged(routingData: RoutingData) {
			//TODO: for debugging only, remove later:
			EVPlanningDataViewModel.setRoutingData(routingData)

			val entries = parseRoutingData(routingData)
			threadCarApp?.post {
				navigationModel?.navigationEntries = entries
			}
		}
	}

	fun onCreateCarApp(threadCarApp: CarThread?) {
		this.threadCarApp = threadCarApp
	}

	fun onDestroy() {
		this.threadCarApp = null
		this.navigationModel = null
	}

	companion object {

		val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
		val NAME_MATCHER = Regex("^(.*[^\\s])\\s*\\[([^\\[]*)\\]\$")

		fun parseRoutingData(routingData: RoutingData): List<NavigationEntry>? {

			class Acc(
					var previous: Step? = null,
					val list: MutableList<NavigationEntry> = mutableListOf(),
			)

			return routingData.routeDistance?.let { routeDistance ->
				routingData.plan?.routes?.getOrNull(routeDistance.routeIndex)
			}?.let { route ->
				val (start_dist, start_time) = routingData.routeDistance.pathStepIndex?.let {
					route.steps?.getOrNull(routingData.routeDistance.stepIndex)?.path?.getOrNull(it)
				}?.let {
					Pair(it.remaining_dist?.times(1000)?.toInt(), it.remaining_time)
				} ?: route.steps?.getOrNull(routingData.routeDistance.stepIndex)?.let {
					Pair(it.departure_dist, it.departure_duration)
				} ?: route.steps?.firstOrNull()?.let {
					Pair(it.departure_dist, it.departure_duration)
				} ?: Pair(null, null)

				val now = LocalDateTime.now()

				route.steps?.filterIndexed { index, step ->
					index >= routingData.routeDistance.stepIndex
				}?.foldIndexed(Acc(), { index, acc, step ->
					if ((step.is_charger == true || step.is_end_station == true)
							&& (index > 0 || routingData.routeDistance.pathStepIndex == null || routingData.routeDistance.pathStepIndex < 2)) {

						val nameParts = step.name?.let { NAME_MATCHER.matchEntire(it) }?.groupValues

						val eta = when (index) {
							0 -> 0
							else -> step.arrival_duration?.let { start_time?.minus(it) }
						}?.let { now.plusSeconds(it.toLong()) }

						acc.list.add(NavigationEntry(
								title = nameParts?.getOrNull(1) ?: "- unknown -",
								operator = nameParts?.lastOrNull() ?: "",
								type = step.charger_type?.toUpperCase() ?: "",
								address = step.charger?.address ?: "- no address -",
								trip_dst = step.departure_dist?.let { start_dist?.minus(it) }?.let { formatDistance(it) }
										?: "--",
								step_dst = when (index) {
									0 -> step.departure_dist?.let { start_dist?.minus(it) }
									1 -> step.arrival_dist?.let { start_dist?.minus(it) }
									else -> acc.previous?.drive_dist
								}?.let { formatDistance(it) } ?: "--",
								soc_ariv = step.arrival_perc?.toString() ?: "",
								soc_dep = step.departure_perc?.toString() ?: "",
								eta = eta?.format(TIME_FMT) ?: "--:--",
								etd = step.charge_duration?.let { eta?.plusSeconds(it.toLong()) }?.format(TIME_FMT)
										?: "--:--",
								duration = step.charge_duration?.div(60)?.toString() ?: "-",
								lat = step.lat,
								lon = step.lon,
						))
					}
					acc.previous = step
					acc
				})?.list
			}
		}

		fun formatDistance(dist: Int): String {
			return if (dist < 10000) {
				String.format("%.1f", dist.div(1000.0))
			} else {
				dist.div(1000).toString()
			}
		}
	}
}