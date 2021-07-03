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
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import me.hufman.androidautoidrive.databinding.EVPlanningSettingsBinding
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningSettingsModel
import me.hufman.androidautoidrive.phoneui.viewmodels.CarCapabilitiesViewModel

class EVPlanningSettingsFragment: Fragment() {

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		val capabilities by viewModels<CarCapabilitiesViewModel> { CarCapabilitiesViewModel.Factory(requireContext().applicationContext) }
		val settingsModel = ViewModelProvider(this, EVPlanningSettingsModel.Factory(requireContext().applicationContext)).get(EVPlanningSettingsModel::class.java)
		val binding = EVPlanningSettingsBinding.inflate(inflater, container, false)
		binding.lifecycleOwner = viewLifecycleOwner
		binding.capabilities = capabilities
		binding.settings = settingsModel
		return binding.root
	}
}