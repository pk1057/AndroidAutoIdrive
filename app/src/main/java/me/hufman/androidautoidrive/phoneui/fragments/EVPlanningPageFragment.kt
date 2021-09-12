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

package me.hufman.androidautoidrive.phoneui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import me.hufman.androidautoidrive.AppSettings
import me.hufman.androidautoidrive.BooleanLiveSetting
import me.hufman.androidautoidrive.R
import me.hufman.androidautoidrive.phoneui.ViewHelpers.visible

class EVPlanningPageFragment: Fragment() {

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_evplanningpage, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val swEVPlanningEnabled = view.findViewById<SwitchCompat>(R.id.swEVPlanningEnabled)
		val paneEVPlanningSettings = view.findViewById<FragmentContainerView>(R.id.paneEVPlanningSettings)
		val paneEVPlanningData = view.findViewById<FragmentContainerView>(R.id.paneEVPlanningData)
		val paneEVPlanningIgnoredChargers = view.findViewById<FragmentContainerView>(R.id.paneEVPlanningIgnoredChargers)
		val paneEVPlanningNetworkPreferences = view.findViewById<FragmentContainerView>(R.id.paneEVPlanningNetworkPreferences)

		BooleanLiveSetting(requireContext().applicationContext, AppSettings.KEYS.EVPLANNING_ENABLED)
			.apply {
				observe(viewLifecycleOwner) {
					swEVPlanningEnabled.isChecked = it
					paneEVPlanningSettings.visible = it
					paneEVPlanningData.visible = it
					paneEVPlanningIgnoredChargers.visible = it
					paneEVPlanningNetworkPreferences.visible = it
				}
			}
			.also {
				swEVPlanningEnabled.setOnCheckedChangeListener { _, isChecked ->
					it.setValue(isChecked)
				}
			}
	}
}