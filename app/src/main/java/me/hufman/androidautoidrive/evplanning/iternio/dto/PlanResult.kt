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

package me.hufman.androidautoidrive.evplanning.iternio.dto

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

data class PlanMetadata(
        val server_type: String?, // "planner"
        val server: String?,      // "se-la-be-01"
        val frontend: String?,    // se-la-fe-01"
        val plan_time: Double?,   // 8.143033027648926
        val log_id: Long?,        // 108304459
)

data class Plan(
		val plan_uuid: String?,          // "e2c2af36-e814-4717-8d00-35fe86699bc5" The unique plan UUID which identifies this plan. Use this to reproduce the plan in ABRP or when using the refresh_plan endpoint.
		val car_model: String?,          // "car_model": "bmw:i3:19:38:other", The car model typecode for which the route was planned.
		val routes: List<Route>?,        // An array of routes, of which the first is the fastest found option. The others are alternative routes which are defined as being reasonably close in time to the best, and significantly different when it comes to route.
		val path_indices: PathIndices?, // An object defining the column index definition of the path array in the steps object. This is only to compact the size of the path array.
		val plan_log_id: Long?,          // 108304459 The incremental log ID number for the call - refer to this when submitting issues or bug reports.
)

data class PathIndices(
        val lat: Int?,
        val lon: Int?,
        val soc_perc: Int?,
        val cons_per_km: Int?,
        val speed: Int?,
        val remaining_time: Int?,
        val remaining_dist: Int?,
        val instruction: Int?,
        val speed_limit: Int?,
        val elevation: Int?,
        val path_distance: Int?,
        val jam_factor: Int?,
        val instruction_obj: Int?,
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
		val name: String?,                     // "Udby Kro", The name of this charger/waypoint.
		val id: Long?,                         // 3660371, integer The waypoint/charger id in the Iternio database. This can be used for planning destinations.
		val lat: Double?,                      // 55.080424, The latitude of the waypoint/charger.
		val lon: Double?,                      // 11.9595256, The longitude of the waypoint/charger.
		val utc_offset: Int?,                  // 3600, [s]: The current offset from UTC for the local time zone at the waypoint/charger.
		val wp_type: Int?,                     // 0 ?
		val is_charger: Boolean?,              // true, [boolean]: True if this step is a charger.
		val is_station: Boolean?,              // false, [boolean]: True if this step was inserted because it is a ferry terminal or railway station, and it is the departure station
		val is_end_station: Boolean?,          // false, [boolean]: True if this step was inserted because it is a ferry terminal or railway station, and it is the arrival station
		val charger_type: String?,             // "ccs", 0 if not a charger
		val is_waypoint: Boolean?,             // false, [boolean]: True if this step is a given input destination.
		val is_new_waypoint: Boolean?,         // false, ?
		val waypoint_idx: Int?,                // 0 [integer]: The index in the input destination array for this destination
		val is_amenity_charger: Boolean?,      // ?
		val is_destcharger: Boolean?,          // false, ?
		val arrival_perc: Double?,                // 46, [SoC %]: The planned arrival SoC at this step (before any charging).
		val departure_perc: Double?,              // 83, [SoC %]: The planned departure SoC at this step (after any charging).
		val departure_duration: Int?,          // 12866, [s]: The total time left for the whole route when departing this step.
		val departure_dist: Int?,              // 243479, [m]: The total distance left for the whole route when departing this step.
		val arrival_dist: Int?,                // 243479,
		val arrival_duration: Int?,            // 14929, [s]: The total time left for the whole route when arriving at this step.
		val max_speed: Double?,                // 41.666666666666664, [m/s]: The planned maximum driving speed for path to the next step (if allowed by adjust_speed)
		val is_mod_speed: Boolean?,            // false, [boolean]: If the maximum speed has been reduced to reach the next step.
		val is_valid_step: Boolean?,           // true,  [boolean]: If false, this step cannot be driven according to the consumption model. The plan is returned with status "invalid", however, the plan can be displayed to the user to indicate where it does not work out. This particular step should be marked as being broken. The arrival_perc of the next step will likely be below the arrival_soc_perc parameter.
		val country_3: String?,                // "DEU", "AUT"...
		val region: String?,                   // "europe", ?
		val stay_duration: Int?,               // 0 ?
		val charge_duration: Int?,            // 2063, [s]: The charging time at this step before leaving for the next.
		val charge_energy: Double?,           // 25.12435127952713 [kWh]: The charging energy added (if charger)
		val charge_cost: Double?,             // 0.0
		val charge_cost_currency: String?,    //
		val charge_profile: List<List<Int>>?, //
		val charger: Charger?,                // [charger object]: If this step is a charger, this is a copy of the charger object (as given by the get_chargers endpoint)
		val drive_duration: Int?,              // 2518, [s]: The driving time from this step to the next.
		val wait_duration: Int?,               // 0, [s]: The expected waiting time at this step before starting to drive. This can be for e.g. ferry terminals.
		val drive_dist: Int?,                  // 72909 [m]: The driving distance from this step to the next.
		val path: List<Any>?                  // [array of path steps]: This is the path between two steps in resolution about 200m steps or less. Each path steps is represented as an array (instead of an object, for compression) defined by the path_indices in the plan result. We will describe it as if each path step is an object. If you do not need this information, you can disable the this field in the plan method by setting the parameter path_steps=false.
)
