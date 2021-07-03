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
import me.hufman.androidautoidrive.databinding.EVPlanningIgnoredChargerHolderBinding
import me.hufman.androidautoidrive.evplanning.ChargerRouteData
import me.hufman.androidautoidrive.evplanning.iternio.entity.Charger

class EVPlanningIgnoredChargersAdapter: RecyclerView.Adapter<IgnoredChargerViewHolder>() {

	fun onChargerDataChanged(it: Map<Long,ChargerRouteData?>) {
		ignoredChargerData = it.values.mapNotNull { data ->
			data?.charger
		}
	}

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

class IgnoredChargerViewHolder(val binding: EVPlanningIgnoredChargerHolderBinding) : RecyclerView.ViewHolder(binding.root)

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
			?.adapter as? EVPlanningIgnoredChargersAdapter)
			?.removeItem(viewHolder.bindingAdapterPosition)
	}
}
