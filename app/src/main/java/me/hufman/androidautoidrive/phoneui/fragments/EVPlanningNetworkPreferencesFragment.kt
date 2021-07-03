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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_evplanning_network_preferences.*
import me.hufman.androidautoidrive.R
import me.hufman.androidautoidrive.phoneui.adapters.EVPlanningNetworkPreferencesAdapter
import me.hufman.androidautoidrive.phoneui.adapters.NetworkPreferencesSwypeHelper
import me.hufman.androidautoidrive.phoneui.controllers.EVPlanningNetworkPreferencesController
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningNetworkPreferencesModel

class EVPlanningNetworkPreferencesFragment: Fragment() {

	lateinit var controller: EVPlanningNetworkPreferencesController

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		return inflater.inflate(R.layout.fragment_evplanning_network_preferences, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val adapter = EVPlanningNetworkPreferencesAdapter()

		val viewModel = ViewModelProvider(this, EVPlanningNetworkPreferencesModel.Factory(requireContext().applicationContext))
				.get(EVPlanningNetworkPreferencesModel::class.java)
				.apply {
					preferencesData.observe(
						viewLifecycleOwner,
						adapter::onPreferecesDataChanged
					)
				}

		controller = EVPlanningNetworkPreferencesController(viewModel)

		adapter.onItemRemove = { id ->
			controller.removePreference(id)
		}

		listNetworkPreferences.layoutManager = LinearLayoutManager(requireActivity())
		listNetworkPreferences.adapter = adapter

		ItemTouchHelper(NetworkPreferencesSwypeHelper()).attachToRecyclerView(listNetworkPreferences)
	}
}