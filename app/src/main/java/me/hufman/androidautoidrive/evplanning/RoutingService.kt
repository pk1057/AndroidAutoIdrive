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
import me.hufman.androidautoidrive.carapp.evplanning.Position
import me.hufman.androidautoidrive.evplanning.iternio.Planning
import me.hufman.androidautoidrive.evplanning.iternio.dto.Destination
import me.hufman.androidautoidrive.evplanning.iternio.dto.PlanRequest
import me.hufman.androidautoidrive.evplanning.iternio.entity.*
import me.hufman.androidautoidrive.evplanning.PreferenceUtils.Companion.jsonToIgnoreChargers
import me.hufman.androidautoidrive.evplanning.PreferenceUtils.Companion.jsonToNetworkPreferences
import me.hufman.androidautoidrive.evplanning.iternio.GetChargerArgs
import me.hufman.androidautoidrive.evplanning.iternio.dto.Network
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningDataViewModel

interface RoutingDataListener {
	fun onRoutingDataChanged(routingData: RoutingData)
	fun onPlanChanged(plan: Plan?)
	fun onNextChargerPlanChanged(plan: Plan?)
	fun onPlanningTriggered()
	fun onPlanningError(msg: String)
	fun onIgnoredChargersChanged(chargers: Map<Long,ChargerRouteData?>)
	fun onNetworkPreferencesChanged(preferences: Map<Long,NetworkPreferenceData>)
}

class RoutingService(
		private val planning: Planning,
		private val routingDataListener: RoutingDataListener
) {

	var handler: Handler? = null

	private var appSettings: MutableAppSettingsReceiver? = null
	private var routingInProgress = false
	private var carData = CarData()

	private var ignoreChargers: Set<Long>? = null
		set(value) {
			if (checkChangedForReplan(field, value)) {
				field = value
				checkIgnoredChargerDetails()
				notifyIgnoreChargersChanged()
			}
		}

	private val chargerRouteData = mutableMapOf<Long, ChargerRouteData>()

	private var networkPreferences: Map<Long, NetworkPreference>? = null
		set(value) {
			if (checkChangedForReplan(field, value)) {
				field = value
				checkNetworkPreferencesDetails()
				notifyNetworkPreferencesChanged()
			}
		}

	private var networkDetails: Map<Long, Network>? = null
		set(value) {
			field = value
			notifyNetworkPreferencesChanged()
		}

	private var maxSpeed: Double? = null
		set(value) {
			if (checkChangedForReplan(field, value)) {
				field = value
			}
		}

	private var referenceConsumption: Int? = null
		set(value) {
			if (checkChangedForReplan(field, value)) {
				field = value
			}
		}

	private var minSocCharger: Double? = null
		set(value) {
			if (checkChangedForReplan(field, value)) {
				field = value
			}
		}

	private var minSocDestination: Double? = null
		set(value) {
			if (checkChangedForReplan(field, value)) {
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
				DrivingMode.COMFORT -> getAppSettingDouble(AppSettings.KEYS.EVPLANNING_MAXSPEED_COMFORT)
				DrivingMode.ECOPRO -> getAppSettingDouble(AppSettings.KEYS.EVPLANNING_MAXSPEED_ECO_PRO)
				DrivingMode.ECOPRO_PLUS -> getAppSettingDouble(AppSettings.KEYS.EVPLANNING_MAXSPEED_ECO_PRO_PLUS)
				DrivingMode.SPORT -> getAppSettingDouble(AppSettings.KEYS.EVPLANNING_MAXSPEED_SPORT)
				else -> null
			}
		}

		val previousDestinations = setOf(
			previous.nextDestination,
			previous.finalDestination,
		).filter { it.isValid() }
		val newDestinationInfos = setOf(
			DestinationInfo(carData.nextDestination, carData.nextDestinationDetails),
			DestinationInfo(carData.finalDestination, carData.finalDestinationDetails),
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
						it.positionRouteData != null && ( it.positionRouteData.distance == null ||
								it.positionRouteData.distance < RouteData.MAX_STEP_OFFSET )
					}.let {
						it.isEmpty() || //no route is close -> replan
								it.filter { routeData ->
									routeData.soc_ariv != null &&
											routeData.positionRouteData!!.stepIndex != null
								}.let { routeDataList ->
									routeDataList.isNotEmpty() && // there are close routes and for all of them the soc_ariv is lower then the minimum -> replan
											routeDataList.all { routeData ->
												if (routeData.waypointIndexInfos.lastOrNull()
														?.index?.equals(routeData.positionRouteData!!.stepIndex!! + 1) == true
												) {
													minSocDestination
												} else {
													minSocCharger
												}?.let { minSoc ->
													routeData.soc_ariv!! < minSoc
												} ?: (routeData.soc_ariv!! < 0.0)
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

	fun checkChangedForReplan(old: Any?, new: Any?): Boolean {
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
		ignoreChargers = appSettings?.get(AppSettings.KEYS.EVPLANNING_IGNORE_CHARGERS)?.let {
			jsonToIgnoreChargers(it)
		}
		networkPreferences = appSettings?.get(AppSettings.KEYS.EVPLANNING_NETWORK_PREFERENCES)?.let {
			jsonToNetworkPreferences(it)
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
			handler?.post {
				updateIgnoredChargerRouteData()
			}
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
						network_preferences = networkPreferences?.map {
							it.key.toString() to it.value.value
						}?.toMap(),
						exclude_ids = this.ignoreChargers?.toList(),
						plan_uuid = plan_uuid
					),
					{
						if (find_next_charger_alts == true) {
							handler?.post {
								error = null
								nextChargerResult = it
								routingInProgress = false
							}
						} else {
							handler?.post {
								error = null
								nextChargerResult = null
								planResult = it
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

	fun updateIgnoredChargerRouteData() {
		planResult?.result?.routes?.also { routes ->
			chargerRouteData.values.forEach {
				it.updatePositionRouteData(routes)
			}
		} ?: run {
			chargerRouteData.values.forEach {
				it.positionRouteData = null
			}
		}
		notifyIgnoreChargersChanged()
	}

	fun checkIgnoredChargerDetails() {
		var changed = false
		ignoreChargers
			?.subtract(chargerRouteData.keys)
			?.forEach { id ->
				planResult?.result?.routes?.let { routes ->
					routes
						.asSequence()
						.map { route ->
							route.steps?.asSequence()
								?.filter { it.charger?.id == id }
								?.map { it.charger }
								?.firstOrNull()
						}
						.firstOrNull()
						?.let { charger ->
							chargerRouteData[id] = ChargerRouteData.of(charger, routes)
							changed = true
						}
				}
			}

		ignoreChargers
			?.subtract(chargerRouteData.keys)
			?.let {
				if (it.isNotEmpty()) {
					loadChargerDetails(it)
				}
			}

		chargerRouteData.keys.subtract(ignoreChargers ?: emptySet()).forEach {
			chargerRouteData.remove(it)
			changed = true
		}

		if (changed) {
			notifyIgnoreChargersChanged()
		}
	}

	fun loadChargerDetails(ids: Set<Long>) {
		planning.getChargers(
			GetChargerArgs(
				ids = ids.toList()
			),
			{
				handler?.post {
					error = null
					it.result?.asSequence()?.filter {
						it.id != null && ignoreChargers?.contains(it.id) ?: false
					}?.forEach {
						chargerRouteData[it.id!!] = ChargerRouteData.of(
							Charger.of(it),
							planResult?.result?.routes
						)
					}
					notifyIgnoreChargersChanged()
				}
			},
			{
				handler?.post {
					error = it
				}
			}
		)
	}

	fun checkNetworkPreferencesDetails() {
		if (networkDetails == null) {
			loadNetworkDetails()
		}
	}

	fun loadNetworkDetails() {
		planning.getNetworks(
			{
				handler?.post {
					error = null
					networkDetails = it.result
						?.asSequence()
						?.filter {
							it.id != null
						}
						?.map { it.id!! to it }
						?.toMap()
				}
			},
			{
				handler?.post {
					error = it
				}
			}
		)
	}

	fun notifyIgnoreChargersChanged() {
		(ignoreChargers
			?.map {
				it to chargerRouteData[it]
			}
			?.toMap()
			?: emptyMap())
			.let { routingDataListener.onIgnoredChargersChanged(it) }
	}

	fun notifyNetworkPreferencesChanged() {
		(networkPreferences
			?.asSequence()
			?.mapNotNull { entry ->
				networkDetails?.get(entry.key)?.let { network ->
					entry.key to NetworkPreferenceData(network, entry.value)
				}
			}
			?.toMap()
			?: emptyMap())
			.let { routingDataListener.onNetworkPreferencesChanged(it) }
	}
}
