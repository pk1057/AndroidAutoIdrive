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

import me.hufman.androidautoidrive.utils.GraphicsHelpers
import me.hufman.androidautoidrive.PhoneAppResources
import me.hufman.androidautoidrive.carapp.FocusTriggerController
import me.hufman.androidautoidrive.carapp.evplanning.*
import me.hufman.androidautoidrive.evplanning.NavigationEntry
import me.hufman.androidautoidrive.carapp.RHMIModelType
import me.hufman.idriveconnectionkit.android.CarAppResources
import me.hufman.idriveconnectionkit.rhmi.*
import java.util.ArrayList
import kotlin.math.min

interface DetailsListener {
	fun onListentryAction(index: Int, invokedBy: Int?)
}

class DetailsView(val state: RHMIState, val carAppResources: CarAppResources, val phoneAppResources: PhoneAppResources, val graphicsHelpers: GraphicsHelpers,
                  val EVPlanningSettings: EVPlanningSettings,
                  val listener: DetailsListener,
                  val focusTriggerController: FocusTriggerController,
                  val navigationModel: NavigationModel,
) {
	companion object {
		fun fits(state: RHMIState): Boolean {
			return state is RHMIState.ToolbarState &&
					state.componentsList.filterIsInstance<RHMIComponent.List>().firstOrNull {
						it.getModel()?.modelType == "Richtext"
					} != null
		}

		const val MAX_LENGTH = 10000
	}

	var listState: RHMIState = state        // where to set the focus when the active notification disappears, linked during initWidgets
	val titleWidget: RHMIComponent.List     // the widget to display the notification app's icon
	val addressWidget: RHMIComponent.List    // the widget to display the title in
	val descriptionWidget: RHMIComponent.List     // the widget to display the text
	val imageWidget: RHMIComponent.Image
	lateinit var inputView: RHMIState

	var visible = false

	var onAddressClicked: (() -> Unit)? = null

	init {
		titleWidget = state.componentsList.filterIsInstance<RHMIComponent.List>().first()
		addressWidget = state.componentsList.filterIsInstance<RHMIComponent.List>()[1]
		descriptionWidget = state.componentsList.filterIsInstance<RHMIComponent.List>().first {
			RHMIModelType.of(it.getModel()?.modelType) == RHMIModelType.RICHTEXT
		}
		imageWidget = state.componentsList.filterIsInstance<RHMIComponent.Image>().first()
		carAppResources.getImagesDB("common")
		listener.onListentryAction(-1,-1)
	}

	fun initWidgets(listView: NavigationListView, inputState: RHMIState) {
		state as RHMIState.ToolbarState
		this.inputView = inputState

		state.focusCallback = FocusCallback { focused ->
			visible = focused
			if (focused) {
				show()

				// read out
				val selectedEntry = navigationModel.selectedNavigationEntry
				if (selectedEntry != null) {
//					readoutInteractions.triggerDisplayReadout(selectedNotification)
				}
			}
		}
		state.visibleCallback = VisibleCallback { visible ->
			if (!visible) {
				hide()
			}
		}

		state.setProperty(RHMIProperty.PropertyId.HMISTATE_TABLETYPE, 3)
		state.componentsList.forEach { it.setVisible(false) }
		// separator below the title
		state.componentsList.filterIsInstance<RHMIComponent.Separator>().forEach { it.setVisible(true) }
		titleWidget.apply {
			// app icon and notification title
			setVisible(true)
			setEnabled(true)
			setSelectable(true)
			setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH.id, "55,0,*")
		}
		addressWidget.apply {
			// the title and any side icon
			setVisible(true)
			setEnabled(true)
			setSelectable(true)
		}
		imageWidget.apply {
			setProperty(RHMIProperty.PropertyId.WIDTH.id, 400)
			setProperty(RHMIProperty.PropertyId.HEIGHT.id, 300)
		}
		descriptionWidget.apply {
			// text
			setVisible(true)
			setEnabled(true)
			setSelectable(true)
		}
		titleWidget.getAction()?.asRAAction()?.rhmiActionCallback = object: RHMIActionListCallback {
			override fun onAction(index: Int, invokedBy: Int?) {
				listener.onListentryAction(index,invokedBy)
			}
		}
		addressWidget.getAction()?.asRAAction()?.rhmiActionCallback = object: RHMIActionListCallback {
			override fun onAction(index: Int, invokedBy: Int?) {
				onAddressClicked?.invoke()
			}
		}
		descriptionWidget.getAction()?.asRAAction()?.rhmiActionCallback = object: RHMIActionListCallback {
			override fun onAction(index: Int, invokedBy: Int?) {
				listener.onListentryAction(index,invokedBy)
			}
		}

		val buttons = ArrayList(state.toolbarComponentsList).filterIsInstance<RHMIComponent.ToolbarButton>().filter { it.action > 0}
		state.toolbarComponentsList.forEach {
			if (it.getAction() != null) {
				it.setSelectable(false)
				it.setEnabled(false)
				it.setVisible(true)
			}
		}
		buttons[0].getImageModel()?.asImageIdModel()?.imageId = 150
		buttons[0].setVisible(true)
		buttons[0].setSelectable(true)
		buttons.subList(1, 6).forEach {
			it.getImageModel()?.asImageIdModel()?.imageId = 158
		}
		buttons.forEach {
			// go back to the main list when an action is clicked
			it.getAction()?.asHMIAction()?.getTargetModel()?.asRaIntModel()?.value = listView.state.id
		}

		this.listState = listView.state
	}

	/**
	 * When we detect that the car is in Parked mode, lock the SpeedLock setting to stay unlocked
	 */
	fun lockSpeedLock() {
		state.setProperty(RHMIProperty.PropertyId.SPEEDLOCK, false)
	}

	fun hide() {
		val emptyList = RHMIModel.RaListModel.RHMIListConcrete(1)
		titleWidget.getModel()?.setValue(emptyList, 0, 0, 0)
		addressWidget.getModel()?.setValue(emptyList, 0, 0, 0)
		descriptionWidget.getModel()?.setValue(emptyList, 0, 0, 0)
		imageWidget.setVisible(false)
	}

	fun show() {
		// set the focus to the first button
		state as RHMIState.ToolbarState
		val buttons = ArrayList(state.toolbarComponentsList).filterIsInstance<RHMIComponent.ToolbarButton>().filter { it.action > 0}
		focusTriggerController.focusComponent(buttons[0])

		redraw()
	}

	fun redraw() {
		state as RHMIState.ToolbarState

		if (!visible) {
			// not visible, skip the redraw
			return
		}

		// find the notification, or bail to the list
		//TODO: implement retrival of navigationEntry from list
		//val notification = NotificationsState.getNotificationByKey(selectedNavigationEntry?.key)
		val navigationEntry: NavigationEntry? = navigationModel.selectedNavigationEntry
		if (navigationEntry == null) {
			focusTriggerController.focusState(listState, false)
			return
		}

		// prepare the app icon and title
		val icon = navigationEntry.icon?.let {graphicsHelpers.compress(it, 48, 48)} ?: ""
		val title = navigationEntry.title
		val titleListData = RHMIModel.RaListModel.RHMIListConcrete(3)
		titleListData.addRow(arrayOf(icon, "", title))

		// prepare the title data
		var sidePictureWidth = 0
		val addressListData = RHMIModel.RaListModel.RHMIListConcrete(2)
//		if (navigationEntry.sidePicture == null || navigationEntry.sidePicture.intrinsicHeight <= 0) {
			addressListData.addRow(arrayOf("", navigationEntry.address))
//		} else {
//			val sidePictureHeight = 96  // force the side picture to be this tall
//			sidePictureWidth = (sidePictureHeight.toFloat() / navigationEntry.sidePicture.intrinsicHeight * navigationEntry.sidePicture.intrinsicWidth).toInt()
//			val sidePicture = graphicsHelpers.compress(navigationEntry.sidePicture, sidePictureWidth, sidePictureHeight)
//			titleListData.addRow(arrayOf(BMWRemoting.RHMIResourceData(BMWRemoting.RHMIResourceType.IMAGEDATA, sidePicture), navigationEntry.title + "\n"))
//		}
		addressWidget.setProperty(RHMIProperty.PropertyId.LIST_COLUMNWIDTH.id, "$sidePictureWidth,*")

		// prepare the notification text
		val descriptionListData = RHMIModel.RaListModel.RHMIListConcrete(1)
		val trimmedText = navigationEntry.text.substring(0, min(MAX_LENGTH, navigationEntry.text.length))
		descriptionListData.addRow(arrayOf(trimmedText))

		state.getTextModel()?.asRaDataModel()?.value = title
		titleWidget.getModel()?.value = titleListData
		addressWidget.getModel()?.value = addressListData
		descriptionWidget.getModel()?.value = descriptionListData

		// try to load a picture from the notification
		var pictureWidth = 400
		var pictureHeight = 300
		val pictureDrawable = try {
//			navigationEntry.picture ?: navigationEntry.pictureUri?.let { phoneAppResources.getUriDrawable(it) }
		} catch (e: Exception) {
//			Log.w(TAG, "Failed to open picture from ${navigationEntry.pictureUri}", e)
			null
		}
//		val picture = if (pictureDrawable != null && pictureDrawable.intrinsicHeight > 0) {
//			pictureHeight = min(300, pictureDrawable.intrinsicHeight)
//			pictureWidth = (pictureHeight.toFloat() / pictureDrawable.intrinsicHeight * pictureDrawable.intrinsicWidth).toInt()
//			graphicsHelpers.compress(pictureDrawable, pictureWidth, pictureHeight, quality = 65)
		//} else { null }
		val picture = null
		// if we have a picture to display
		if (picture != null) {
			// set the dimensions, ID4 clips images to this rectangle
			imageWidget.setProperty(RHMIProperty.PropertyId.HEIGHT, pictureHeight)
			imageWidget.setProperty(RHMIProperty.PropertyId.WIDTH, pictureWidth)
			imageWidget.setVisible(true)
			imageWidget.getModel()?.asRaImageModel()?.value = picture
		} else {
			imageWidget.setVisible(false)
		}

		// find and enable the clear button
		val buttons = ArrayList(state.toolbarComponentsList).filterIsInstance<RHMIComponent.ToolbarButton>().filter { it.action > 0}
//		val clearButton = buttons[0]
//		if (navigationEntry.isClearable) {
//			clearButton.setEnabled(true)
//			clearButton.getTooltipModel()?.asRaDataModel()?.value = L.NOTIFICATION_CLEAR_ACTION
//			clearButton.getAction()?.asRAAction()?.rhmiActionCallback = RHMIActionButtonCallback {
//				controller.clear(navigationEntry.key)
//			}
//		} else {
//			clearButton.setEnabled(false)
//		}

		// enable any custom actions
//		(0..4).forEach {i ->
//			val action = navigationEntry.actions.getOrNull(i)
//			val button = buttons[1+i]
//			if (action == null) {
//				button.setEnabled(false)
//				button.setSelectable(false)
//				button.getAction()?.asRAAction()?.rhmiActionCallback = null // don't leak memory
//			} else {
//				button.setEnabled(true)
//				button.setSelectable(true)
//				button.getTooltipModel()?.asRaDataModel()?.value = action.name.toString()
//				button.getAction()?.asRAAction()?.rhmiActionCallback = RHMIActionButtonCallback {
//					if (action.supportsReply ) {
//						// show input to reply
//						button.getAction()?.asHMIAction()?.getTargetModel()?.asRaIntModel()?.value = inputView.id
//						val replyController = ReplyControllerNotification(notification, action, controller, ABRPSettings.quickReplies)
//						ReplyView(listState, inputView, replyController)
//						readoutInteractions.cancel()
//					} else {
//						// trigger the custom action
//						controller.action(navigationEntry.key, action.name.toString())
//						button.getAction()?.asHMIAction()?.getTargetModel()?.asRaIntModel()?.value = listState.id
//					}
//				}
//			}
//		}
	}
}