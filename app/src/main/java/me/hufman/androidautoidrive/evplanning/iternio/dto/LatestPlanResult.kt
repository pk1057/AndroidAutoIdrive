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

data class LatestPlanResult(
		val status: String?, // "ok" - everything is OK, a valid plan returned
        // "invalid" - we could not find a working plan, but something which fails at some step. We still return the invalid plan, indicating which step fails with an is_valid_step=False. Showing this to the user usually helps the user to understand what makes the trip impossible.
        // "notfound" - we could not find anything close to a working plan. No plan returned.
        // "address_not_found" - One or more of the addresses supplied could not be mapped to a lat/lon position
        // "address_different_regions" - The addresses are in different regions of the world
        // "error" - planning failed for abnormal reason (such as an internal crash)
		val error_type: String?, // "last_plan_id_not_found"
		val result: Plan?,
)
