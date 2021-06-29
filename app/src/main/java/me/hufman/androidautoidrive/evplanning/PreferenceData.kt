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

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import me.hufman.androidautoidrive.carapp.evplanning.Position
import me.hufman.androidautoidrive.evplanning.iternio.dto.Network
import me.hufman.androidautoidrive.evplanning.iternio.entity.Charger
import me.hufman.androidautoidrive.evplanning.iternio.entity.Route

data class ChargerRouteData(
	val charger: Charger,
	var positionRouteData: Map<Int, PositionRouteData>? = null
) {
	fun updatePositionRouteData(routes: List<Route>) {
		positionRouteData =
			RouteData.PositionRuler.of(Position(charger.lat, charger.lon))
			?.let { ruler ->
				routes.mapIndexed { index, route ->
					PositionRouteData.of(ruler, route)
						?.let { index to it }
				}.filterNotNull()
					.toMap()
			}
	}

	fun getRouteDistances(other: Map<Int, PositionRouteData>, routes: List<Route>): Map<Int,Int>? {
		return positionRouteData?.let {
			PositionRouteData.routeIndexDistanceMapOf(it, other, routes)
		}
	}

	companion object {
		fun of(
			charger: Charger,
			routes: List<Route>?
		): ChargerRouteData {
			return ChargerRouteData(charger)
				.apply {
					routes?.let {
						updatePositionRouteData(it)
					}
				}
		}
	}
}

data class NetworkPreferenceData(
	val network: Network,
	val preference: NetworkPreference,
)

enum class NetworkPreference(val value: Int) {
	PREFER_EXCLUSIVE(3),
	EXCLUSIVE(2),
	PREFER(1),
	DONTCARE(0),
	AVOID(-2);

	companion object {
		val map = values().asSequence().map { it.value to it }.toMap()
		fun of(value: Int): NetworkPreference {
			return map.getOrElse(value) { throw IllegalArgumentException("$value is not a valid NetworkPreference") }
		}
	}
}

class PreferenceUtils {
	companion object {

		fun jsonToIgnoreChargers(json: String): Set<Long>? {
			return try {
				Gson().fromJson(
					json,
					Set::class.java
				).asSequence()
					.map {
						(it as Double).toLong()
					}
					.toSet()
			} catch (jse: JsonSyntaxException) {
				null
			} catch (cce: ClassCastException) {
				null
			} catch (npe: NullPointerException) {
				null
			}
		}

		fun jsonToNetworkPreferences(json: String): Map<Long,NetworkPreference>? {
			return try {
				Gson().fromJson(
					json,
					Map::class.java
				).asSequence()
					.map {
						(it.key as String).toLong() to NetworkPreference.of((it.value as Double).toInt())
					}
					.toMap()
			} catch (jse: JsonSyntaxException) {
				null
			} catch (cce: ClassCastException) {
				null
			} catch (npe: NullPointerException) {
				null
			} catch (iae: IllegalArgumentException) {
				null
			}
		}

		fun addLongJsonString(ignoreChargerString: String, id: Long): String {
			return jsonToIgnoreChargers(ignoreChargerString)
				?.toMutableSet()
				?.apply { add(id) }
				?.let { Gson().toJson(it) }
				?: Gson().toJson(listOf(id))
		}

		fun removeLongJsonString(ignoreChargerString: String, id: Long): String {
			return jsonToIgnoreChargers(ignoreChargerString)
				?.toMutableSet()
				?.apply { remove(id) }
				?.let { Gson().toJson(it) }
				?: "[]"
		}

		fun setNetworkPreferenceJsonString(
			networkPreferencesString: String,
			id: Long,
			preference: NetworkPreference
		): String {
			return jsonToNetworkPreferences(networkPreferencesString)
				?.toMutableMap()
				?.apply {
					when (preference) {
						NetworkPreference.DONTCARE -> remove(id)
						else -> put(id, preference)
					}
				}
				?.asSequence()
				?.map { it.key to it.value.value }
				?.toMap()
				?.let { Gson().toJson(it) }
				?: when (preference) {
					NetworkPreference.DONTCARE -> "{}"
					else -> Gson().toJson(mapOf(id to preference.value))
				}
		}
	}
}