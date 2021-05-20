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

data class TelemetryRequest(
		val tlm: Tlm,
)

data class Tlm(
    // high priority parameters:
    val utc: Long?,            // [s]: Current UTC timestamp (epoch) in seconds (note, not milliseconds!)
    val soc: Double?,          // [SoC %]: State of Charge of the vehicle (what's displayed on the dashboard of the vehicle is preferred)
    val power: Double?,        // [kW]: Instantaneous power output/input to the vehicle. Power output is positive, power input is negative (charging)
    val speed: Double?,        // [km/h]: Vehicle speed
    val lat: Double?,          // [°]: Current vehicle latitude
    val lon: Double?,          // [°]: Current vehicle longitude
    val is_charging: Boolean?, // [bool or 1/0]: Determines vehicle state. 0 is not charging, 1 is charging
    val is_dcfc: Boolean?,     // [bool or 1/0]: If is_charging, indicate if this is DC fast charging
    val is_parked: Boolean?,   // [bool or 1/0]: If the vehicle gear is in P (or the driver has left the car)
    // low priority parameters:
    val capacity: Double?,     // [kWh]: Estimated usable battery capacity (can be given together with soh, but usually not)
    val kwh_charged: Double?,  // [kWh]: Measured energy input while charging. Typically a cumulative total, but also supports individual sessions.
    val soh: Double?,          // [%]: State of Health of the battery. 100 = no degradation
    val heading: Int?,         // [°]: Current heading of the vehicle. This will take priority over phone heading, so don't include if not accurate.
    val elevation: Int?,       // [m]: Vehicle's current elevation. If not given, will be looked up from location (but may miss 3D structures)
    val ext_temp: Int?,        // "5" [°C]: Outside temperature measured by the vehicle
    val batt_temp: Int?,       // [°C]: Battery temperature
    val voltage: Double?,      // [V]: Battery pack voltage
    val current: Double?,      // [A]: Battery pack current (similar to power: output is positive, input (charging) is negative.)
    val odometer: Int?,        // [km]: Current odometer reading in km.
    // additional parameters:
    val activity_id: Long?,           // "66666666",
    val time: String?,                // "2020-02-06 09:03:04",
    val battery_range: String?,       // "" ?
    val ideal_battery_range: String?, // "" ?
    val typecode: String?,            // "" ?
    val battery_capacity: String?,    // "" ?
    val tlm_type: String?,            // "api",
    val charger_id: Long?,
    val charge_energy_added: Double?, // "",
    val weather_temp: Double?,        // "",
    val weather_pressure: Double?,    // "",
    val weather_humidity: Int?,       // ": "",
    val weather_condition: String?,   // "",
    val weather_wind_speed: Double?,  // "",
    val weather_wind_dir: String?,    // "",
)