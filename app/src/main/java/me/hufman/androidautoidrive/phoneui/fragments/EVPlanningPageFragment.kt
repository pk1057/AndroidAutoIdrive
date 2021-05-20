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
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_evplanningpage.*
import me.hufman.androidautoidrive.*
import me.hufman.androidautoidrive.phoneui.visible

class EVPlanningPageFragment: Fragment() {

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_evplanningpage, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val evPlanningEnabledSetting = BooleanLiveSetting(requireContext().applicationContext, AppSettings.KEYS.ENABLED_EVPLANNING)
		evPlanningEnabledSetting.observe(viewLifecycleOwner) {
			swEVPlanningEnabled.isChecked = it
			paneEVPlanningSettings.visible = it
			paneEVPlanningData.visible = it
		}
		swEVPlanningEnabled.setOnCheckedChangeListener { _, isChecked ->
			onChangedSwitchEVPlanning(evPlanningEnabledSetting, isChecked)
		}
	}

	override fun onResume() {
		super.onResume()

//		viewModel.update()
	}

	private fun onChangedSwitchEVPlanning(appSetting: BooleanLiveSetting, isChecked: Boolean) {
		appSetting.setValue(isChecked)
		if (isChecked) {
			// make sure we have permissions to read the notifications
//			if (viewModel.hasNotificationPermission.value != true) {
//				permissionsController.promptNotification()
//			}
		}
	}
}