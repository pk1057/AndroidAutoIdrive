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
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_evplanning_ignored_chargers.*
import me.hufman.androidautoidrive.R
import me.hufman.androidautoidrive.databinding.EVPlanningIgnoredChargerHolderBinding
import me.hufman.androidautoidrive.evplanning.iternio.entity.Charger
import me.hufman.androidautoidrive.phoneui.controllers.EVPlanningIgnoredChargersController
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningIgnoredChargersModel

class EVPlanningIgnoredChargersFragment: Fragment() {

	lateinit var controller: EVPlanningIgnoredChargersController

	class IgnoredChargerViewHolder(val binding: EVPlanningIgnoredChargerHolderBinding) : RecyclerView.ViewHolder(binding.root)

	class IgnoredChargersAdapter: RecyclerView.Adapter<IgnoredChargerViewHolder>() {

		var ignoredChargerData: List<Charger> = emptyList()
			set(value) {
				field = value
				notifyDataSetChanged()
			}

		lateinit var onItemRemove: (Long) -> Unit

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IgnoredChargerViewHolder {
			return LayoutInflater.from(parent.context)
				.let {
					EVPlanningIgnoredChargerHolderBinding.inflate(it, parent, false)
				}
				.let {
					IgnoredChargerViewHolder(it)
				}
		}

		override fun onBindViewHolder(holder: IgnoredChargerViewHolder, position: Int) {
			holder.binding.data = ignoredChargerData[position].name
		}

		override fun getItemCount(): Int {
			return ignoredChargerData.size
		}

		fun removeItem(position: Int) {
			ignoredChargerData.getOrNull(position)?.let {
				it.id?.let {
					onItemRemove(it)
				}
			}
			notifyItemRemoved(position)
		}
	}

	class IgnoredChargersSwypeHelper: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {

		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder
		): Boolean {
			return false
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
			((viewHolder.itemView.parent as? RecyclerView)
				?.adapter as? IgnoredChargersAdapter)
				?.removeItem(viewHolder.bindingAdapterPosition)
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		return inflater.inflate(R.layout.fragment_evplanning_ignored_chargers, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val adapter = IgnoredChargersAdapter()

		val viewModel = ViewModelProvider(this, EVPlanningIgnoredChargersModel.Factory(requireContext().applicationContext))
				.get(EVPlanningIgnoredChargersModel::class.java)
				.apply {
					chargerData.observe(
						viewLifecycleOwner,
						{
							adapter.ignoredChargerData = it.values.mapNotNull { data ->
								data?.charger
							}
						})
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