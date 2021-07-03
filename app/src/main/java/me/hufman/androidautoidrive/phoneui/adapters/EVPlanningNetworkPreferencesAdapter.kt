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

package me.hufman.androidautoidrive.phoneui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import me.hufman.androidautoidrive.databinding.EVPlanningNetworkPreferencesHolderBinding
import me.hufman.androidautoidrive.evplanning.NetworkPreferenceData

class EVPlanningNetworkPreferencesAdapter: RecyclerView.Adapter<NetworkPreferenceViewHolder>() {

	fun onPreferecesDataChanged(it: Map<Long, NetworkPreferenceData>) {
		networkPreferencesData = it.values.toList()
	}

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

class NetworkPreferenceViewHolder(val binding: EVPlanningNetworkPreferencesHolderBinding) : RecyclerView.ViewHolder(binding.root)

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
			?.adapter as? EVPlanningNetworkPreferencesAdapter)
			?.removeItem(viewHolder.bindingAdapterPosition)
	}
}
