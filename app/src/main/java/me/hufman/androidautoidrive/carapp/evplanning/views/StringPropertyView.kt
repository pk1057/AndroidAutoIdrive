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

package me.hufman.androidautoidrive.carapp.evplanning.views

import io.bimmergestalt.idriveconnectkit.rhmi.RHMIState
import me.hufman.androidautoidrive.carapp.InputState

class StringPropertyView(
	destState: RHMIState,
	inputState: RHMIState,
	val values: List<String>,
	val setInput: (value: String) -> Unit
) : InputState<String>(inputState) {

	init {
		inputComponent.getSuggestAction()?.asHMIAction()?.getTargetModel()?.asRaIntModel()?.value =
			destState.id
	}

	override fun onEntry(input: String) {
		if (input == "") {
			sendSuggestions(values)
		} else {
			values.filter {
				it.contains(input)
			}.let { sendSuggestions(it) }
		}
	}

	override fun onSelect(item: String, index: Int) {
		setInput(item)
	}

	override fun convertRow(row: String): String {
		return row
	}
}
