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