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

data class ChargersResult(
		val status: String?, // "ok"
		val metadata: ChargerResultMetadata?, //
		val result: List<Charger>?,
)

data class ChargerResultMetadata(
        val server_type: String?, // "backend",
        val server: String?,      //"se-la-be-01",
        val frontend: String?,    // "se-la-fe-01"
)

data class Charger(
		val name: String?,                    // "name": "Ionity - Greve, DK [Ionity]",
		val id: Long?,                        // "id": 4288487,
		val address: String?,                 // "address": "Mosede Landevej 64, Greve",
		val lat: Double?,                     // "lat": 55.5847374120105,
		val lon: Double?,                     // "lon": 12.2576344182219,
		val url: String?,                     // "url": null,
		val comment: String?,                 // null -
		val status: String,                   // "OPEN", "CONSTRUCTION" (not yet open), "CLOSED", "LIMITED"
		val region: String?,                  // "region": "europe",
		val country_3: String?,               // "DEU"
		val network_id: Long?,                 // "network_id": 85,
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