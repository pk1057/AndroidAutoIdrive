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
		fun parseRoutingData(routingData: RoutingData): List<NavigationEntry>? {
			val dtFmt = DateTimeFormatter.ofPattern("HH:mm")

			class Acc(
					var previous: Step? = null,
					val list: MutableList<NavigationEntry> = mutableListOf(),
					var time: LocalDateTime? = LocalDateTime.now(),
			)
			return routingData.routeDistance?.let { routeDistance ->
				routingData.plan?.routes?.get(routeDistance.routeIndex)
			}?.steps?.fold(Acc(), { acc, step ->
				with(acc) {
					previous?.let { privstep ->
						time = privstep.drive_duration?.let { time?.plusSeconds(it.toLong()) }
					}
					if (step.is_charger == true || step.is_end_station == true) {
						list.add(NavigationEntry(
								title = step.name ?: "- unknown -",
								text = step.charger_type ?: "",
								address = step.charger?.address ?: "- no address -",
								distance = (previous?.drive_dist?.div(1000)?.toString()
										?: "--") + "km",
								soc = step.arrival_perc?.toString()?.plus("%") ?: "",
								eta = time?.format(dtFmt) ?: "--:--",
								duration = "(" + (step.charge_duration?.div(60)?.toString()
										?: "- ") + "min)",
								lat = step.lat,
								lon = step.lon,
						))
					}
					step.charge_duration?.let { time = time?.plusSeconds(it.toLong()) }
					step.wait_duration?.let { time = time?.plusSeconds(it.toLong()) }
					previous = step
				}
				acc
			})?.list
		}
	}
}