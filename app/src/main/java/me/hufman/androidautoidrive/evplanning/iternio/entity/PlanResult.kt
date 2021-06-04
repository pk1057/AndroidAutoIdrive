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

package me.hufman.androidautoidrive.evplanning.iternio.entity

import me.hufman.androidautoidrive.evplanning.iternio.dto.PathIndices

data class PlanResult(
		val status: String?, // "ok" - everything is OK, a valid plan returned
		// "invalid" - we could not find a working plan, but something which fails at some step. We still return the invalid plan, indicating which step fails with an is_valid_step=False. Showing this to the user usually helps the user to understand what makes the trip impossible.
		// "notfound" - we could not find anything close to a working plan. No plan returned.
		// "address_not_found" - One or more of the addresses supplied could not be mapped to a lat/lon position
		// "address_different_regions" - The addresses are in different regions of the world
		// "error" - planning failed for abnormal reason (such as an internal crash)
		val fail_reason: String?,
		val metadata: Any?,
		val result: Plan?,
)

data class Plan(
		val plan_uuid: String?,          // "e2c2af36-e814-4717-8d00-35fe86699bc5" The unique plan UUID which identifies this plan. Use this to reproduce the plan in ABRP or when using the refresh_plan endpoint.
		val car_model: String?,          // "car_model": "bmw:i3:19:38:other", The car model typecode for which the route was planned.
		val routes: List<Route>?,        // An array of routes, of which the first is the fastest found option. The others are alternative routes which are defined as being reasonably close in time to the best, and significantly different when it comes to route.
		val plan_log_id: Long?,          // 108304459 The incremental log ID number for the call - refer to this when submitting issues or bug reports.
)

data class Route(
		val steps: List<Step>?,            // [array of steps]: Each step is a destination or charger plus the path between the present destination and the next one. The last step will therefore not contain a path.
		val total_dist: Int?,              // 692583 [m]: The total distance for this route.
		val total_charge_duration: Int?,   // 14371 [s]: The total time spent charging the vehicle in this route.
		val total_drive_duration: Int?,    // 29344 [s]: The total time spent driving the vehicle in this route.
		val total_drive_distance: Double?, // 692418.3228063583,
		val average_consumption: Double?,  // 193.09959186929814,[Wh/km]: The estimated average consumption for this route.
		val total_energy_used: Double?,    // 133.7058886363236
		val is_valid_route: Boolean?,      // true if all steps in the route have is_valid_step set to true.
)

data class Step(
		val name: String?,                    // "Udby Kro", The name of this charger/waypoint.
		val id: Long?,                        // 3660371, integer The waypoint/charger id in the Iternio database. This can be used for planning destinations.
		val lat: Double,                      // 55.080424, The latitude of the waypoint/charger.
		val lon: Double,                      // 11.9595256, The longitude of the waypoint/charger.
		val utc_offset: Int?,                 // 3600, [s]: The current offset from UTC for the local time zone at the waypoint/charger.
		val wp_type: Int?,                    // 0 ?
		val is_charger: Boolean?,             // true, [boolean]: True if this step is a charger.
		val is_station: Boolean?,             // false, [boolean]: True if this step was inserted because it is a ferry terminal or railway station, and it is the departure station
		val is_end_station: Boolean?,         // false, [boolean]: True if this step was inserted because it is a ferry terminal or railway station, and it is the arrival station
		val charger_type: String?,            // "ccs", 0 if not a charger
		val is_waypoint: Boolean?,            // false, [boolean]: True if this step is a given input destination.
		val is_new_waypoint: Boolean?,        // false, ?
		val waypoint_idx: Int?,               // 0 [integer]: The index in the input destination array for this destination
		val is_amenity_charger: Boolean?,     // ?
		val is_destcharger: Boolean?,         // false, ?
		val arrival_perc: Double?,               // 46, [SoC %]: The planned arrival SoC at this step (before any charging).
		val departure_perc: Double?,             // 83, [SoC %]: The planned departure SoC at this step (after any charging).
		val departure_duration: Int?,         // 12866, [s]: The total time left for the whole route when departing this step.
		val departure_dist: Int?,             // 243479, [m]: The total distance left for the whole route when departing this step.
		val arrival_dist: Int?,               // 243479,
		val arrival_duration: Int?,           // 14929, [s]: The total time left for the whole route when arriving at this step.
		val max_speed: Double?,               // 41.666666666666664, [m/s]: The planned maximum driving speed for path to the next step (if allowed by adjust_speed)
		val is_mod_speed: Boolean?,           // false, [boolean]: If the maximum speed has been reduced to reach the next step.
		val is_valid_step: Boolean?,          // true,  [boolean]: If false, this step cannot be driven according to the consumption model. The plan is returned with status "invalid", however, the plan can be displayed to the user to indicate where it does not work out. This particular step should be marked as being broken. The arrival_perc of the next step will likely be below the arrival_soc_perc parameter.
		val country_3: String?,               // "DEU", "AUT"...
		val region: String?,                  // "europe", ?
		val stay_duration: Int?,              // 0 ?
		val charge_duration: Int?,            // 2063, [s]: The charging time at this step before leaving for the next.
		val charge_energy: Double?,           // 25.12435127952713 [kWh]: The charging energy added (if charger)
		val charge_cost: Double?,             // 0.0
		val charge_cost_currency: String?,    //
		val charge_profile: List<List<Int>>?, //
		val charger: Charger?,                // [charger object]: If this step is a charger, this is a copy of the charger object (as given by the get_chargers endpoint)
		val drive_duration: Int?,             // 2518, [s]: The driving time from this step to the next.
		val wait_duration: Int?,              // 0, [s]: The expected waiting time at this step before starting to drive. This can be for e.g. ferry terminals.
		val drive_dist: Int?,                 // 72909 [m]: The driving distance from this step to the next.
		val path: List<PathStep>?             // [array of path steps]: This is the path between two steps in resolution about 200m steps or less. Each path steps is represented as an array (instead of an object, for compression) defined by the path_indices in the plan result. We will describe it as if each path step is an object. If you do not need this information, you can disable the this field in the plan method by setting the parameter path_steps=false.
)

// And finally, the path step object (which is encoded as an array for compression) is defined by:
data class PathStep(
		val lat: Double,             // Latitude of the path step.
		val lon: Double,             // lon: Longitude of the pat step.
		val soc_perc: Double?,       // [SoC %]: The remaining estimated SoC.
		val cons_per_km: Int?,       // [Wh/km]: The instantaneous estimated consumption.
		val speed: Int?,             // [m/s]: The estimated speed of the vehicle (note the unit!)
		val remaining_time: Int?,    // [s]: The remaining time for the whole route.
		val remaining_dist: Double?, // [km]: The remaining distance for the whole route.
		val instruction: String?,    // An English turn instruction.
		val speed_limit: Double?,    // [m/s]: The present speed limit, if known (note the unit!)
		val elevation: Int?,         // [m]: The elevation over mean sea level.
		val path_distance: Int?,     // [m]: The travel distance for this path step only, always 200 m or less.
		val jam_factor: Double?,
		val instruction_obj: Instruction?,
)

data class Instruction(
		val type: String?,      // "off ramp",
		val direction: String?, // "slight right",
		val lanes: List<Lane>?, //
		val name: String?,      // "B 8, B 27: Marktheidenfeld, Fulda, Nürnberg, Bamberg"
)

data class Lane(
		val valid: Boolean?,            // false,
		val indications: List<String>?, // [ "straight" ]
)

data class Charger(
		val name: String?,                    // "name": "Ionity - Greve, DK [Ionity]",
		val id: Long?,                        // "id": 4288487,
		val address: String?,                 // "address": "Mosede Landevej 64, Greve",
		val lat: Double,                      // "lat": 55.5847374120105,
		val lon: Double,                      // "lon": 12.2576344182219,
		val url: String?,                     // "url": null,
		val comment: String?,                 // null -
		val status: String,                   // "OPEN", "CONSTRUCTION" (not yet open), "CLOSED", "LIMITED"
		val region: String?,                  // "region": "europe",
		val country_3: String?,               // "DEU"
		val network_id: Int?,                 // "network_id": 85,
		val network_name: String?,            // "network_name": "Ionity",
		val network_icon: String?,            // null - "ionity.png"
		val source_attribution_logo: String?, // "https://abetterrouteplanner.com/icon/ionity_logo.png"
		val source_attribution_url: String?,  // "https://ionity.eu",
		val outlets: List<Outlet>?,           //
		val locationid: String?,              // "locationid": "ionity_8769da75-24f9-476d-b29d-08fc7fe371a4"
)

data class Outlet(
		val type: String?,          // "type": "ccs",
		val stalls: Int?,           // "stalls": 10,
		val power: Double?,         // "power": 350.0
		val status: String?,        // "OPERATIONAL"
)

fun toPlanResult(planResult: me.hufman.androidautoidrive.evplanning.iternio.dto.PlanResult): PlanResult {

	return PlanResult(
			planResult.status,
			planResult.fail_reason,
			planResult.metadata,
			planResult.result?.let { plan ->
				Plan(
						plan.plan_uuid,
						plan.car_model,
						plan.routes?.map {
							toRoute(it, plan.path_indices)
						},
						plan.plan_log_id
				)
			}
	)
}

fun toRoute(route: me.hufman.androidautoidrive.evplanning.iternio.dto.Route, indices: PathIndices?): Route {

	return Route(
			route.steps?.mapNotNull { toStep(it, indices) },
			route.total_dist,
			route.total_charge_duration,
			route.total_drive_duration,
			route.total_drive_distance,
			route.average_consumption,
			route.total_energy_used,
			route.is_valid_route,
	)
}

fun toStep(step: me.hufman.androidautoidrive.evplanning.iternio.dto.Step, indices: PathIndices?): Step? {
	return if (step.lat != null && step.lon != null)
		Step(
				step.name,
				step.id,
				step.lat,
				step.lon,
				step.utc_offset,
				step.wp_type,
				step.is_charger,
				step.is_station,
				step.is_end_station,
				step.charger_type,
				step.is_waypoint,
				step.is_new_waypoint,
				step.waypoint_idx,
				step.is_amenity_charger,
				step.is_destcharger,
				step.arrival_perc,
				step.departure_perc,
				step.departure_duration,
				step.departure_dist,
				step.arrival_dist,
				step.arrival_duration,
				step.max_speed,
				step.is_mod_speed,
				step.is_valid_step,
				step.country_3,
				step.region,
				step.stay_duration,
				step.charge_duration,
				step.charge_energy,
				step.charge_cost,
				step.charge_cost_currency,
				step.charge_profile,
				step.charger?.let { charger ->
					if (charger.lat != null && charger.lon != null) {
						Charger(
								charger.name,
								charger.id,
								charger.address,
								charger.lat,
								charger.lon,
								charger.url,
								charger.comment,
								charger.status,
								charger.region,
								charger.country_3,
								charger.network_id,
								charger.network_name,
								charger.network_icon,
								charger.source_attribution_logo,
								charger.source_attribution_url,
								charger.outlets?.mapNotNull { outlet ->
									with(outlet) {
										if (type != null || stalls != null || power != null || status != null) {
											Outlet(type, stalls, power, status)
										} else null
									}
								},
								charger.locationid
						)
					} else null
				},
				step.drive_duration,
				step.wait_duration,
				step.drive_dist,
				indices?.let { indice ->
					step.path?.filterIsInstance<List<*>>()?.mapNotNull { pathstep ->
						toPathStep(pathstep, indice)
					}
				}
		) else null
}

fun toPathStep(pathstep: List<*>, indices: PathIndices): PathStep? {
	return if (indices.lat != null && indices.lon != null) {
		val lat = pathstep.getOrNull(indices.lat) as? Double
		val lon = pathstep.getOrNull(indices.lon) as? Double
		return if (lat != null && lon != null) {
			PathStep(
					lat,
					lon,
					indices.soc_perc?.let { soc_index -> pathstep.getOrNull(soc_index) as? Double },
					indices.cons_per_km?.let { cons_index -> pathstep.getOrNull(cons_index) as? Double }?.toInt(),
					indices.speed?.let { speed_index -> pathstep.getOrNull(speed_index) as? Double }?.toInt(),
					indices.remaining_time?.let { remain_time_index -> pathstep.getOrNull(remain_time_index) as? Double }?.toInt(),
					indices.remaining_dist?.let { remain_dist_index -> pathstep.getOrNull(remain_dist_index) as? Double },
					indices.instruction?.let { instruct_index -> pathstep.getOrNull(instruct_index) as? String },
					indices.speed_limit?.let { limit_index -> pathstep.getOrNull(limit_index) as? Double },
					indices.elevation?.let { ele_index -> pathstep.getOrNull(ele_index) as? Double }?.toInt(),
					indices.path_distance?.let { dist_index -> pathstep.getOrNull(dist_index) as? Double }?.toInt(),
					indices.jam_factor?.let { jam_index -> pathstep.getOrNull(jam_index) as? Double },
					indices.instruction_obj?.let { instr_obj_index -> pathstep.getOrNull(instr_obj_index) as? Map<*, *> }?.let { instr_obj ->
						val type = instr_obj["type"] as? String
						val direction = instr_obj["direction"] as? String
						val lanes = (instr_obj["lanes"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.mapNotNull { lane ->
							val valid = lane["valid"] as? Boolean
							val indications = (lane["indications"] as? List<*>?)?.filterIsInstance<String>()
							if (valid != null && !indications.isNullOrEmpty()) {
								Lane(
										valid,
										indications
								)
							} else null
						}
						val name = instr_obj["name"] as? String
						if (type != null && direction != null && !lanes.isNullOrEmpty() && name != null) {
							Instruction(
									type,
									direction,
									lanes,
									name,
							)
						} else null
					})
		} else null
	} else null
}

fun toResult(planResult: me.hufman.androidautoidrive.evplanning.iternio.dto.PlanResult): PlanResult {

	return PlanResult(
			planResult.status,
			planResult.fail_reason,
			planResult.metadata,
			planResult.result?.let { plan ->
				Plan(
						plan.plan_uuid,
						plan.car_model,
						plan.routes?.map { route ->
							Route(
									route.steps?.mapNotNull { step ->
										if (step.lat != null && step.lon != null) {
											Step(
													step.name,
													step.id,
													step.lat,
													step.lon,
													step.utc_offset,
													step.wp_type,
													step.is_charger,
													step.is_station,
													step.is_end_station,
													step.charger_type,
													step.is_waypoint,
													step.is_new_waypoint,
													step.waypoint_idx,
													step.is_amenity_charger,
													step.is_destcharger,
													step.arrival_perc,
													step.departure_perc,
													step.departure_duration,
													step.departure_dist,
													step.arrival_dist,
													step.arrival_duration,
													step.max_speed,
													step.is_mod_speed,
													step.is_valid_step,
													step.country_3,
													step.region,
													step.stay_duration,
													step.charge_duration,
													step.charge_energy,
													step.charge_cost,
													step.charge_cost_currency,
													step.charge_profile,
													step.charger?.let { charger ->
														if (charger.lat != null && charger.lon != null) {
															Charger(
																	charger.name,
																	charger.id,
																	charger.address,
																	charger.lat,
																	charger.lon,
																	charger.url,
																	charger.comment,
																	charger.status,
																	charger.region,
																	charger.country_3,
																	charger.network_id,
																	charger.network_name,
																	charger.network_icon,
																	charger.source_attribution_logo,
																	charger.source_attribution_url,
																	charger.outlets?.mapNotNull { outlet ->
																		with(outlet) {
																			if (type != null && stalls != null && power != null && status != null) {
																				Outlet(type, stalls, power, status)
																			} else null
																		}
																	},
																	charger.locationid
															)
														} else null
													},
													step.drive_duration,
													step.wait_duration,
													step.drive_dist,
													plan.path_indices?.let { indices ->
														step.path?.filterIsInstance<List<*>>()?.mapNotNull { pathstep ->
															val lat = indices.lat?.let { lat_index -> pathstep.getOrNull(lat_index) as? Double }
															val lon = indices.lon?.let { lon_index -> pathstep.getOrNull(lon_index) as? Double }
															if (lat != null && lon != null) {
																PathStep(
																		lat,
																		lon,
																		indices.soc_perc?.let { soc_index -> pathstep.getOrNull(soc_index) as? Double },
																		indices.cons_per_km?.let { cons_index -> pathstep.getOrNull(cons_index) as? Double }?.toInt(),
																		indices.speed?.let { speed_index -> pathstep.getOrNull(speed_index) as? Double }?.toInt(),
																		indices.remaining_time?.let { remain_time_index -> pathstep.getOrNull(remain_time_index) as? Double }?.toInt(),
																		indices.remaining_dist?.let { remain_dist_index -> pathstep.getOrNull(remain_dist_index) as? Double },
																		indices.instruction?.let { instr_index -> pathstep.getOrNull(instr_index) as? String },
																		indices.speed_limit?.let { limit_index -> pathstep.getOrNull(limit_index) as? Double },
																		indices.elevation?.let { ele_index -> pathstep.getOrNull(ele_index) as? Double }?.toInt(),
																		indices.path_distance?.let { dist_index -> pathstep.getOrNull(dist_index) as? Double }?.toInt(),
																		indices.jam_factor?.let { jam_index -> pathstep.getOrNull(jam_index) as? Double },
																		indices.instruction_obj?.let { instr_obj_index -> pathstep.getOrNull(instr_obj_index) as? Map<*, *> }?.let { instr_obj ->
																			val type = instr_obj["type"] as? String
																			val direction = instr_obj["direction"] as? String
																			val lanes = (instr_obj["lanes"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.mapNotNull { lane ->
																				val valid = lane["valid"] as? Boolean
																				val indications = (lane["indications"] as? List<*>)?.filterIsInstance<String>()
																				if (valid != null && !indications.isNullOrEmpty()) {
																					Lane(
																							valid,
																							indications
																					)
																				} else null
																			}
																			val name = instr_obj["name"] as? String
																			if (type != null && direction != null && !lanes.isNullOrEmpty() && name != null) {
																				Instruction(
																						type,
																						direction,
																						lanes,
																						name,
																				)
																			} else null
																		})
															} else null
														}
													}
											)
										} else null
									},
									route.total_dist,
									route.total_charge_duration,
									route.total_drive_duration,
									route.total_drive_distance,
									route.average_consumption,
									route.total_energy_used,
									route.is_valid_route,
							)
						},
						plan.plan_log_id
				)
			})
}
