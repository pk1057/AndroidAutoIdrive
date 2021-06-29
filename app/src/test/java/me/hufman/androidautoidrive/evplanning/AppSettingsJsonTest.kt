package me.hufman.androidautoidrive.evplanning

import me.hufman.androidautoidrive.evplanning.PreferenceUtils.Companion.jsonToIgnoreChargers
import me.hufman.androidautoidrive.evplanning.PreferenceUtils.Companion.jsonToNetworkPreferences
import me.hufman.androidautoidrive.evplanning.PreferenceUtils.Companion.addLongJsonString
import me.hufman.androidautoidrive.evplanning.PreferenceUtils.Companion.removeLongJsonString
import me.hufman.androidautoidrive.evplanning.PreferenceUtils.Companion.setNetworkPreferenceJsonString
import org.junit.Assert.*
import org.junit.Test

class AppSettingsJsonTest {

	@Test
	fun testAppSettingsJson() {

		assertEquals(
			setOf(1L,2L,3L,4L),
			jsonToIgnoreChargers("[1,2,4,3,2]")
		)
		assertNull(jsonToIgnoreChargers(""))
		assertNull(jsonToIgnoreChargers("deadbeef"))
		assertNull(jsonToIgnoreChargers("[1,2,,3]"))
		assertNull(jsonToIgnoreChargers("[1,2,deadbeef,3]"))

		assertEquals(NetworkPreference.PREFER_EXCLUSIVE, NetworkPreference.of(3))
		assertEquals(NetworkPreference.EXCLUSIVE, NetworkPreference.of(2))
		assertEquals(NetworkPreference.PREFER, NetworkPreference.of(1))
		assertEquals(NetworkPreference.DONTCARE, NetworkPreference.of(0))
		assertEquals(NetworkPreference.AVOID, NetworkPreference.of(-2))
		assertThrows(IllegalArgumentException::class.java) { NetworkPreference.of(4) }

		assertEquals(
			mapOf(
				10L to NetworkPreference.of(1),
				20L to NetworkPreference.of(2),
				30L to NetworkPreference.of(3)
			),
			jsonToNetworkPreferences("{10:1,30:3,20:2}")
		)

		assertNull(jsonToNetworkPreferences("{10:1,30:3,20:2,40:4}"))

		assertEquals(
			setOf(1L,2L,3L,5L),
			jsonToIgnoreChargers(addLongJsonString("[1,2,5]",3))
		)
		assertEquals(
			setOf(1L,2L,5L),
			jsonToIgnoreChargers(addLongJsonString("[1,2,5]",2))
		)
		assertEquals(
			setOf(2L),
			jsonToIgnoreChargers(addLongJsonString("[]",2))
		)
		assertEquals(
			setOf(2L),
			jsonToIgnoreChargers(addLongJsonString("deadbeef",2))
		)
		assertEquals(
			setOf(1L,5L),
			jsonToIgnoreChargers(removeLongJsonString("[1,2,5]",2))
		)
		assertEquals(
			setOf(1L,2L,5L),
			jsonToIgnoreChargers(removeLongJsonString("[1,2,5]",3))
		)
		assertEquals(
			emptySet<Long>(),
			jsonToIgnoreChargers(removeLongJsonString("[3]",3))
		)
		assertEquals(
			emptySet<Long>(),
			jsonToIgnoreChargers(removeLongJsonString("[]",3))
		)
		assertEquals(
			emptySet<Long>(),
			jsonToIgnoreChargers(removeLongJsonString("deadbeef",3))
		)

		assertEquals(
			mapOf(
				10L to NetworkPreference.of(1),
				30L to NetworkPreference.PREFER_EXCLUSIVE,
				50L to NetworkPreference.of(-2),
			),
			jsonToNetworkPreferences(
				setNetworkPreferenceJsonString(
					"{10:1,50:-2}",
					30,
					NetworkPreference.PREFER_EXCLUSIVE
				)
			)
		)

		assertEquals(
			mapOf(
				10L to NetworkPreference.of(1),
				50L to NetworkPreference.of(-2),
			),
			jsonToNetworkPreferences(
				setNetworkPreferenceJsonString(
					"{10:1,30:3,50:-2}",
					30,
					NetworkPreference.DONTCARE
				)
			)
		)

		assertEquals(
			emptyMap<Long,Int>(),
			jsonToNetworkPreferences(
				setNetworkPreferenceJsonString(
					"{30:3}",
					30,
					NetworkPreference.DONTCARE
				)
			)
		)

		assertEquals(
			mapOf(30L to NetworkPreference.PREFER_EXCLUSIVE),
			jsonToNetworkPreferences(
				setNetworkPreferenceJsonString(
					"{}",
					30,
					NetworkPreference.PREFER_EXCLUSIVE
				)
			)
		)

		assertEquals(
			mapOf(30L to NetworkPreference.PREFER_EXCLUSIVE),
			jsonToNetworkPreferences(
				setNetworkPreferenceJsonString(
					"deadbeef",
					30,
					NetworkPreference.PREFER_EXCLUSIVE
				)
			)
		)

		assertEquals(
			emptyMap<Long,Int>(),
			jsonToNetworkPreferences(
				setNetworkPreferenceJsonString(
					"{}",
					30,
					NetworkPreference.DONTCARE
				)
			)
		)

		assertEquals(
			emptyMap<Long,Int>(),
			jsonToNetworkPreferences(
				setNetworkPreferenceJsonString(
					"deadbeef",
					30,
					NetworkPreference.DONTCARE
				)
			)
		)
	}
}