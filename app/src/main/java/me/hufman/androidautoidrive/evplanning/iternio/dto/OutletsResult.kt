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

data class OutletsResult(
		val status: String?,                  // "ok"
		val metadata: OutletsResultMetadata?,
		val result: List<String>?,           // ["ccs","cee_blue",...]
)

data class OutletsResultMetadata(
        val server_type: String?, // "backend",
        val server: String?,      //"se-la-be-01",
        val frontend: String?,    // "se-la-fe-01"
)

