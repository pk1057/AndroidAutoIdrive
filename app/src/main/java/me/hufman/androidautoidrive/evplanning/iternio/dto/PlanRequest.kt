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

data class PlanRequest(
        // mandatory:
		val car_model: String,                        // "bmw:i3:19:38:other",
		val destinations: List<Destination>,          // [{"address":"Goethestraße, 85386 Eching, Germany","lat":48.30384,"lon":11.62581,"id":null,"charge_power":null},{"address":"Hornoulická (Hornoulicka), 972 01 Bojnice, Slovakia","lat":48.78301,"lon":18.58081,"id":null}],
        // optional:
		val initial_soc_perc: Double? = null,                // 90,
		val find_alts: Boolean? = null,                      // true,
		val path_steps: Boolean? = null,                     // false - If given and false, the plan output will not include the path object in each step object. This will make the plan object much smaller.
		val path_polyline: Boolean? = null,                  // false - Return only a polyline (the first two elements of the path output), skip the remaining output details between steps. If path_steps is strue, this parameter is ignored.
		val amenity_offset: Int? = null,                     // 0,
		val vehicle_id: Int? = null,                         // 26589,
		val vehicle_config: String? = null,                  // "default",
		val ref_consumption: Int? = null,                    // 165,
		val fast_chargers: List<String>? = null,             // ["ccs"], Comma-separated string of allowed outlet types for planning. The default value is taken from the rec_fast_chargers property of the carmodel.
		val find_amenity_alts: Boolean? = null,              // false,
		val find_next_charger_alts: Boolean? = null,         // false
		val charger_soc_perc: Double? = null,                // 10,
		val charger_max_soc_perc: Double? = null,            // 100,
		val arrival_soc_perc: Double? = null,                // 10,
		val charge_overhead: Int? = null,                    // [s] 300,
		val adjust_speed: Boolean? = null,                   // true,
		val realtime_traffic: Boolean? = null,               // false,
		val speed_factor_perc: Double? = null,               // 90,
		val max_speed: Double? = null,                       // 149.99976,
		val allow_ferry: Boolean? = null,                    // true,
		val allow_motorway: Boolean? = null,                 // true,
		val allow_toll: Boolean? = null,                     // true,
		val allow_border: Boolean? = null,                   // true,
		val wind_speed: Double? = null,                      // 0,
		val wind_dir: String? = null,                        // "head", ["head"/"tail"] The assumed wind direction for the plan.
		val road_condition: String? = null,                  // "normal", ["normal"/"rain"/"heavy_rain"] Anything other than normal will increase the assumed consumption of the vehicle.
		val extra_weight: Int? = null,                       // 0, [kg]
		val outside_temp: Double? = null,                    // 4,
		val battery_degradation_perc: Double? = null,        // 5,
		val network_preferences: Map<String, Int>? = null,   // {} // A JSON object of network preferences. The properties in the object are string network IDs, and the value of each property can be one of:
        // -2: This network is never included in the plan
        // 0: Don't care
        // +1: This network is preferred, meaning there will be a certain advantage for the planner to choose chargers from this network. This preference is controlled by the preferred_charge_cost_multiplier and nonpreferred_charge_cost_addition paremters.
        // +2: This network is used exclusively (together with other networks in this object).
        // +3: This network is used exclusively and preferred.

		val preferred_charge_cost_multiplier: Double? = null,//  0.7 - If given, the charge time of a preferred network charge is multiplied by this factor. For example, 0.7 means that only 70% of the charging time is accounted for as cost in the optimization. This does not affect the charging time presented to the user.
		val nonpreferred_charge_cost_addition: Int? = null,  // 0 [s] If given, charging at a non-preferred network charge costs this many seconds extra. For example, a value of 600 means that charging at a non-preferred network charger is assumed to take 5 extra minutes (to make it less likely to be chosen). This does not affect the charging time presented to the user.
		val group_preferences: Map<String, Int>? = null,     // {} - A JSON object of group preferences. A charger may have a list of groups associated with it (defined when importing a specific charger database). The meaning of these groups is up to the customer to define. The plan can select chargers based on charger membership of these groups using the group_preferences object, defined similar to network_preferences, but instead of network IDs as keys, this is group names. The values can be anything from -2 to +3.
		val access_preferences: Map<String, Int>? = null,    // {} - A JSON object of charger access preferences. A charger may have a list of accesses associated with it (defined when importing a specific charger database). The meaning of these accesses is up to the customer to define. The plan can select chargers based on charger membership of these access groups using the access_preferences object, defined similar to network_preferences, but instead of network IDs as keys, this is access names. The values can be anything from -2 to +3.

		val exclude_ids: List<Long>? = null,                 // [10141046],
		val realtime_weather: Boolean? = null,               // false,
		val realtime_chargers: Boolean? = null,              // false,
		val client: String? = null,                          // "abrp-web",
		val session_id: String? = null,                      // "1122e0e05da7069b5247cd87194965ffa8905275c68ca495",
		val plan_uuid: String? = null,                       // null, ?
		val units: String? = null,                           // "metric"
		val allowed_dbs: String? = null                      // "ocm" - Comma-separated string of charger database identifiers to be used in the planning. For example "ocm,sc" would include chargers only from OpenChargeMap and Tesla Superchargers.
)

data class Destination(
        val lat: Double? = null,            // 48.30384, either provide lat+lon, address or id (of charger)
        val lon: Double? = null,            // 11.62581,
        val address: String? = null,        // "Goethestraße, 85386 Eching, Germany",
        val id: Long? = null,               // null - If given, this ID number corresponds to a charger in the Iternio database.
        val bearing: Int? = null,           // If given, this is the bearing (heading) of the vehicle in degrees with zero being north.
        val is_my_pos: Boolean? = null,     // If given and true this represents the user's current location.
        val charge_power: Int? = null,      // null - [kW] If given, this destination will be used for charging. Must be combined with charge_time or charge_to_perc.
        val charge_to_perc: Double? = null, // 80.5 - [SoC %] Charge to this SoC. The time will be calculated by the planner.
        val charge_time: Int? = null,       // 5400 - [s] If given, this destination will be used for charging, for this time.
        val departure_time: Long? = null,   // 1616483100 - If given, this is the UTC epoch time at which the user is departing from this waypoint The destination may also contain the following properties:
        val arrive_perc: Double? = null,    // 65.3 - [SoC %] Arrive at this destination with at least this SoC.
)

