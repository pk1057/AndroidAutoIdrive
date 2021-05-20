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

package com.truchsess.evrouting.iternio.dto

data class VehicleLibraryResult(
        val status: String?, // "ok"
        val result: List<Vehicle>?
)

data class Vehicle(
        val typecode: String?,                      // "tesla:ct:23:mr:d" - The unique ID string of this vehicle. Additional options are added at the end, separated by ':'
        val title: String?,                         // "Dual Motor AWD" - Title appended to the folders to create the full vehicle title
        val ref_cons: Double?,                      // 314.0 - The default reference consumption for the vehicle, i.e fixed speed, flat land, 20C driving at 110 km/h [Wh/km]
        val rec_max_speed: Double?,                 // 150.0 - Recommended max speed in [km/h]
        val fast_chargers: List<String>?,           // ["SC","ccs","tesla_ccs"] - Supported fast chargers for this vehicle (potentially including common adapters)
        val rec_fast_chargers: List<String>?,       // ["SC"] - Recommended fast chargers for this vehicle
        val level2_chargers: List<String>?,         // ["type2","destcharger"] - Supported level 2 chargers for the vehicle
        val deprecated: Boolean?,                   // If true, this vehicle has been deprecated and should not be shown in the listing (but still works in the planner)
        val replaced_by: String?,                   // If deprecated, this is the recommended typecode to replace it with. Can be done automatically without asking the user.
        val usable_battery_wh: Double?,             // The usable battery of the vehicle model [Wh]
        val folder: List<String>?,                  // ["Tesla","Cybertruck"] - A list of folder names for hierarchical display of the vehicle list - typically ['<manufacturer>', '<model>', ...]
        val options: List<VehicleOption>?,              // A list of CarOption objects describing options for this vehicle (described below)
        val manufacturer: String?,                  // "Tesla" - The vehicle manufacturer
        val model: String?,                         // "Cybertruck" - The vehicle model (not including trim)
        val year: String?,                          // "2022-" The vehicle model year(s) (can be an interval)
        val type: String?,                          // "car" - The type of the vehicle ( 'car' | 'truck' | 'mc' )
        val maturity: String?,                      // "alpha" - The maturity of the vehicle model ('mature' | 'alpha' | 'beta')
        val forum_url: String?,                     // "https://forum.abetterrouteplanner.com/topic/..."
)

data class VehicleOption(
        val title: String?,                         // "Wheels" - The title of the option, for example "Wheels" or "Heatpump"
        val choices: List<VehicleOptionChoice>?,         // A list of option choices. If only one choice, this is a binary yes/no option, for example for heatpump.
)

data class VehicleOptionChoice(
        val value: String?,                         // "19wheels" - The option value, added to the typecode string
        val title: String?,                         // "19\"" - A title to display for the choice, if applicable
        val selected: Boolean?,                     // If true, this choice is pre-selected in the UI
        val ref_cons_delta: Int?,                   // 10 - This number should be added to the reference consumption of the vehicle if the choice is selected
        val level2_chargers_delta: List<String>?,   // This set of chargers should be added to the vehicle level2 chargers if the choice is selected
        val fast_chargers_delta: List<String>?,     // This set of chargers should be added to the vehicle fast chargers if the choice is selected
        val rec_fast_chargers_delta: List<String>?, // This set of chargers should be added to the vehicle recommended fast chargers if the choice is selected
)
