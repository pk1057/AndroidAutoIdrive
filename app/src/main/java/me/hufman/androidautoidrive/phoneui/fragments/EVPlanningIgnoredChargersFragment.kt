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
import kotlinx.android.synthetic.main.fragment_evplanning_ignored_chargers.*
import me.hufman.androidautoidrive.R
import me.hufman.androidautoidrive.phoneui.adapters.EVPlanningIgnoredChargersAdapter
import me.hufman.androidautoidrive.phoneui.adapters.IgnoredChargersSwypeHelper
import me.hufman.androidautoidrive.phoneui.controllers.EVPlanningIgnoredChargersController
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningIgnoredChargersModel

class EVPlanningIgnoredChargersFragment: Fragment() {

	lateinit var controller: EVPlanningIgnoredChargersController

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		return inflater.inflate(R.layout.fragment_evplanning_ignored_chargers, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val adapter = EVPlanningIgnoredChargersAdapter()

		val viewModel = ViewModelProvider(this, EVPlanningIgnoredChargersModel.Factory(requireContext().applicationContext))
				.get(EVPlanningIgnoredChargersModel::class.java)
				.apply {
					chargerData.observe(
						viewLifecycleOwner,
						adapter::onChargerDataChanged
					)
				}

		controller = EVPlanningIgnoredChargersController(viewModel)

		adapter.onItemRemove = { id ->
			controller.removeCharger(id)
		}

		listIgnoredChargers.layoutManager = LinearLayoutManager(requireActivity())
		listIgnoredChargers.adapter = adapter

		ItemTouchHelper(IgnoredChargersSwypeHelper()).attachToRecyclerView(listIgnoredChargers)
	}
}