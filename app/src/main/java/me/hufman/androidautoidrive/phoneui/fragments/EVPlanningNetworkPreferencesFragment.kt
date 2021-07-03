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
import kotlinx.android.synthetic.main.fragment_evplanning_network_preferences.*
import me.hufman.androidautoidrive.R
import me.hufman.androidautoidrive.databinding.EVPlanningNetworkPreferencesHolderBinding
import me.hufman.androidautoidrive.evplanning.NetworkPreferenceData
import me.hufman.androidautoidrive.phoneui.controllers.EVPlanningNetworkPreferencesController
import me.hufman.androidautoidrive.phoneui.viewmodels.EVPlanningNetworkPreferencesModel

class EVPlanningNetworkPreferencesFragment: Fragment() {

	lateinit var controller: EVPlanningNetworkPreferencesController

	class NetworkPreferenceViewHolder(val binding: EVPlanningNetworkPreferencesHolderBinding) : RecyclerView.ViewHolder(binding.root)

	class NetworkPreferencesAdapter: RecyclerView.Adapter<NetworkPreferenceViewHolder>() {

		var networkPreferencesData: List<NetworkPreferenceData> = emptyList()
			set(value) {
				field = value
				notifyDataSetChanged()
			}

		lateinit var onItemRemove: (Long) -> Unit

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkPreferenceViewHolder {
			return LayoutInflater.from(parent.context)
				.let {
					EVPlanningNetworkPreferencesHolderBinding.inflate(it, parent, false)
				}
				.let {
					NetworkPreferenceViewHolder(it)
				}
		}

		override fun onBindViewHolder(holder: NetworkPreferenceViewHolder, position: Int) {
			holder.binding.data = networkPreferencesData[position].let {
				"${it.network.name} ${it.preference.name}"
			}
		}

		override fun getItemCount(): Int {
			return networkPreferencesData.size
		}

		fun removeItem(position: Int) {
			networkPreferencesData.getOrNull(position)?.let {
				it.network.id?.let {
					onItemRemove(it)
				}
			}
			notifyItemRemoved(position)
		}
	}

	class NetworkPreferencesSwypeHelper: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {

		override fun onMove(
			recyclerView: RecyclerView,
			viewHolder: RecyclerView.ViewHolder,
			target: RecyclerView.ViewHolder
		): Boolean {
			return false
		}

		override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
			((viewHolder.itemView.parent as? RecyclerView)
				?.adapter as? NetworkPreferencesAdapter)
				?.removeItem(viewHolder.bindingAdapterPosition)
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		return inflater.inflate(R.layout.fragment_evplanning_network_preferences, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val adapter = NetworkPreferencesAdapter()

		val viewModel = ViewModelProvider(this, EVPlanningNetworkPreferencesModel.Factory(requireContext().applicationContext))
				.get(EVPlanningNetworkPreferencesModel::class.java)
				.apply {
					preferencesData.observe(
						viewLifecycleOwner,
						{
							adapter.networkPreferencesData = it.values.toList()
						})
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