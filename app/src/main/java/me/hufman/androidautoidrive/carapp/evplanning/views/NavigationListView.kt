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

import android.os.Handler
import android.util.Log
import de.bmw.idrive.BMWRemoting
import me.hufman.androidautoidrive.*
import me.hufman.androidautoidrive.carapp.FocusTriggerController
import me.hufman.androidautoidrive.carapp.RHMIActionAbort
import me.hufman.androidautoidrive.carapp.RHMIListAdapter
import me.hufman.androidautoidrive.carapp.evplanning.*
import me.hufman.androidautoidrive.carapp.evplanning.TAG
import me.hufman.androidautoidrive.evplanning.NavigationEntry
import me.hufman.androidautoidrive.utils.GraphicsHelpers
import me.hufman.idriveconnectionkit.rhmi.*

class NavigationListView(val state: RHMIState, val graphicsHelpers: GraphicsHelpers, val settings: EVPlanningSettings,
                         val focusTriggerController: FocusTriggerController, val navigationModel: NavigationModel) {
	companion object {
		const val INTERACTION_DEBOUNCE_MS = 2000              // how long to wait after lastInteractionTime to update the list
		const val SKIPTHROUGH_THRESHOLD = 2000                // how long after an entrybutton push to allow skipping through to a current notification
		const val ARRIVAL_THRESHOLD = 8000                    // how long after a new notification should it skip through

		fun fits(state: RHMIState): Boolean {
			return state is RHMIState.PlainState &&
					state.componentsList.filterIsInstance<RHMIComponent.List>().size >= 2 &&
					state.componentsList.indexOfLast { it is RHMIComponent.Label } < state.componentsList.indexOfLast { it is RHMIComponent.List }
		}
		const val IMAGEID_CHECKMARK = 150
	}

	val navigationListView: RHMIComponent.List    // the list component of notifications
	val settingsListView: RHMIComponent.List    // the list component of notifications

	var onNavigationListViewClicked: ((Int) -> Unit)? = null

	var visible = false                 // whether the notification list is showing

	var entryButtonTimestamp = 0L   // when the user pushed the entryButton
	val timeSinceEntryButton: Long
		get() = System.currentTimeMillis() - entryButtonTimestamp

	var deferredUpdate: DeferredUpdate? = null  // wrapper object to help debounce user inputs
	var lastInteractionIndex: Int = -1       // what index the user last selected

	val emptyListData = RHMIModel.RaListModel.RHMIListConcrete(5).apply {
		addRow(arrayOf("", L.EVPLANNING_EMPTY_LIST, "", "", ""))
	}

	val menuSettingsListData = object: RHMIListAdapter<AppSettings.KEYS>(3, settings.getSettings()) {
		override fun convertRow(index: Int, item: AppSettings.KEYS): Array<Any> {
			val checked = settings.isChecked(item)
			val checkmark = if (checked) BMWRemoting.RHMIResourceIdentifier(BMWRemoting.RHMIResourceType.IMAGEID, IMAGEID_CHECKMARK) else ""
			val name = when (item) {
				AppSettings.KEYS.ENABLED_NOTIFICATIONS_POPUP -> L.NOTIFICATION_POPUPS
				AppSettings.KEYS.ENABLED_NOTIFICATIONS_POPUP_PASSENGER -> L.NOTIFICATION_POPUPS_PASSENGER
				AppSettings.KEYS.NOTIFICATIONS_SOUND -> L.NOTIFICATION_SOUND
				AppSettings.KEYS.NOTIFICATIONS_READOUT -> L.NOTIFICATION_READOUT
				AppSettings.KEYS.NOTIFICATIONS_READOUT_POPUP -> L.NOTIFICATION_READOUT_POPUP
				AppSettings.KEYS.NOTIFICATIONS_READOUT_POPUP_PASSENGER -> L.NOTIFICATION_READOUT_POPUP_PASSENGER
				else -> ""
			}
			return arrayOf(checkmark, "", name)
		}
	}

	init {
		navigationListView = state.componentsList.filterIsInstance<RHMIComponent.List>().first()
		settingsListView = state.componentsList.filterIsInstance<RHMIComponent.List>().last()
	}

	fun initWidgets() { // showNavigationEntryController: ShowNavigationEntryController) {
		// refresh the list when we are displayed
		state.focusCallback = FocusCallback { focused ->
			visible = focused
			if (focused) {
				val didEntryButton = timeSinceEntryButton < SKIPTHROUGH_THRESHOLD
				// if we did not skip through, refresh:
				redrawNavigationEntryList()

				// if a notification is speaking, pre-select it
				// otherwise pre-select the most recent notification that showed up or was selected
				// and only if the user is freshly arriving, not backing out of a deeper view
				val index = 0
				if (didEntryButton && index >= 0) {
					focusTriggerController.focusComponent(navigationListView, index)
				}

				redrawSettingsList()
				settings.callback = {
					redrawSettingsList()
				}
			} else {
				settings.callback = null
			}
		}

		state.getTextModel()?.asRaDataModel()?.value = L.EVPLANNING_TITLE
		state.setProperty(RHMIProperty.PropertyId.HMISTATE_TABLETYPE, 3)
		state.componentsList.forEach { it.setVisible(false) }

		navigationListView.setVisible(true)
		navigationListView.setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH.id, "55,200,50,50,50")
		navigationListView.setProperty(RHMIProperty.PropertyId.BOOKMARKABLE, true)
		navigationListView.getAction()?.asRAAction()?.rhmiActionCallback = object: RHMIActionListCallback {
			override fun onAction(index: Int, invokedBy: Int?) {
				if (invokedBy != 2) {       // don't change the navigationEntry
					onNavigationListViewClicked?.invoke(index)
				}
			}
		}

		navigationListView.getSelectAction()?.asRAAction()?.rhmiActionCallback = RHMIActionListCallback {
			if (it != lastInteractionIndex) {
				lastInteractionIndex = it
				deferredUpdate?.defer(INTERACTION_DEBOUNCE_MS.toLong())
			}
		}

		if (settings.getSettings().isNotEmpty()) {
			state.componentsList.filterIsInstance<RHMIComponent.Label>().lastOrNull()?.let {
				it.getModel()?.asRaDataModel()?.value = L.EVPLANNING_OPTIONS
				it.setVisible(true)
				it.setEnabled(false)
				it.setSelectable(false)
			}

			settingsListView.setVisible(true)
			settingsListView.setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH.id, "55,0,*")
			settingsListView.getAction()?.asRAAction()?.rhmiActionCallback = RHMIActionListCallback { index ->
				val setting = menuSettingsListData.realData.getOrNull(index)
				if (setting != null) {
					settings.toggleSetting(setting)
				}
				throw RHMIActionAbort()
			}
		}
	}

	fun onCreate(handler: Handler) {
		deferredUpdate = DeferredUpdate(handler)
	}

	/** Only redraw if the user hasn't clicked it recently
	 *  Gets called whenever the notification list changes
	 */
	fun gentlyUpdateNotificationList() {
		if (!visible) {
			return
		}

		val deferredUpdate = this.deferredUpdate
		if (deferredUpdate == null) {
			Log.w(TAG, "DeferredUpdate not built yet, redrawing immediately")
			redrawNavigationEntryList()
		} else {
			deferredUpdate.trigger(0) {
				if (visible) {
					Log.i(TAG, "Updating list of notifications")
					redrawNavigationEntryList()
				} else {
					Log.i(TAG, "Notification list is not on screen, skipping update")
				}
				deferredUpdate.defer(INTERACTION_DEBOUNCE_MS.toLong())   // wait at least this long before doing another update
			}
		}
	}

	// should only be run from the DeferredUpdate thread, once at a time, but synchronize just in case
	fun redrawNavigationEntryList() {

		val entries = navigationModel.navigationEntries
		if (entries.isNullOrEmpty()) {
			navigationListView.getModel()?.value = emptyListData
		} else {
			//5 columns: icon, title, dist, soc, eta
			navigationListView.getModel()?.value = object : RHMIListAdapter<NavigationEntry>(5, entries) {
				override fun convertRow(index: Int, item: NavigationEntry): Array<Any> {
					val icon = item.icon?.let { graphicsHelpers.compress(it, 48, 48) } ?: ""
					val text = "${item.title}\n${item.text.trim().split(Regex("\n")).lastOrNull() ?: ""}"
					return arrayOf(icon, text, item.distance, item.soc, item.eta)
				}
			}
		}
	}

	fun redrawSettingsList() {
		settingsListView.getModel()?.value = menuSettingsListData
	}
}