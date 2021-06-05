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

import io.sentry.Sentry
import me.hufman.androidautoidrive.utils.GraphicsHelpers
import me.hufman.androidautoidrive.PhoneAppResources
import me.hufman.androidautoidrive.carapp.FocusTriggerController
import me.hufman.androidautoidrive.carapp.evplanning.*
import me.hufman.androidautoidrive.evplanning.DisplayWaypoint
import me.hufman.androidautoidrive.carapp.RHMIModelType
import me.hufman.androidautoidrive.carapp.evplanning.NavigationModelUpdater.Companion.TIME_FMT
import me.hufman.androidautoidrive.carapp.evplanning.NavigationModelUpdater.Companion.formatDistance
import me.hufman.idriveconnectionkit.rhmi.*
import java.util.*

class DetailsView(
	val state: RHMIState,
	val phoneAppResources: PhoneAppResources,
	val graphicsHelpers: GraphicsHelpers,
	val settings: EVPlanningSettings,
	val focusTriggerController: FocusTriggerController,
	val navigationModel: NavigationModel,
	carAppAssetsIcons: Map<String, ByteArray>,
) {
	companion object {
		fun fits(state: RHMIState): Boolean {
			return state is RHMIState.ToolbarState &&
					state.componentsList.filterIsInstance<RHMIComponent.List>().firstOrNull {
						it.getModel()?.modelType?.let { RHMIModelType.of(it) } == RHMIModelType.RICHTEXT
					} != null
		}

		const val MAX_LENGTH = 10000
	}

	var listState: RHMIState =
		state        // where to set the focus when the active notification disappears, linked during initWidgets
	val nameWidget: RHMIComponent.List     // the widget to display the charging-points name and operator's icon
	val addressWidget: RHMIComponent.List    // the widget to display the title in
	val descriptionWidget: RHMIComponent.List     // the widget to display the text
	val imageWidget: RHMIComponent.Image
//	lateinit var inputView: RHMIState

	var visible = false

	var onAddressClicked: (() -> Unit)? = null

	init {
		nameWidget = state.componentsList.filterIsInstance<RHMIComponent.List>().first()
		addressWidget = state.componentsList.filterIsInstance<RHMIComponent.List>()[1]
		descriptionWidget = state.componentsList.filterIsInstance<RHMIComponent.List>().first {
			RHMIModelType.of(it.getModel()?.modelType) == RHMIModelType.RICHTEXT
		}
		imageWidget = state.componentsList.filterIsInstance<RHMIComponent.Image>().first()
	}

	fun initWidgets(listView: WaypointsListView) { //, inputState: RHMIState) {
		state as RHMIState.ToolbarState
//		this.inputView = inputState

		state.focusCallback = FocusCallback { focused ->
			visible = focused
			if (focused) {
				show()

				// read out
//				val selectedEntry = navigationModel.selectedWaypoint
//				if (selectedEntry != null) {
//					readoutInteractions.triggerDisplayReadout(selectedNotification)
//				}
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
		state.componentsList.filterIsInstance<RHMIComponent.Separator>()
			.forEach { it.setVisible(true) }
		nameWidget.apply {
			// operator's icon and charging-stops name
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
		addressWidget.getAction()?.asRAAction()?.rhmiActionCallback =
			object : RHMIActionListCallback {
				override fun onAction(index: Int, invokedBy: Int?) {
					onAddressClicked?.invoke()
				}
			}

		this.listState = listView.state
	}

	fun hide() {
		val emptyList = RHMIModel.RaListModel.RHMIListConcrete(1)
		nameWidget.getModel()?.setValue(emptyList, 0, 0, 0)
		addressWidget.getModel()?.setValue(emptyList, 0, 0, 0)
		descriptionWidget.getModel()?.setValue(emptyList, 0, 0, 0)
		imageWidget.setVisible(false)
	}

	fun show() {
		// set the focus to the first button
		state as RHMIState.ToolbarState
		val buttons =
			ArrayList(state.toolbarComponentsList).filterIsInstance<RHMIComponent.ToolbarButton>()
				.filter { it.action > 0 }
		focusTriggerController.focusComponent(buttons[0])

		redraw()
	}

	fun redraw() {
		state as RHMIState.ToolbarState

		if (!visible) {
			// not visible, skip the redraw
			return
		}

		try {
			// find the notification, or bail to the list
			//TODO: implement retrival of navigationEntry from list
			//val notification = NotificationsState.getNotificationByKey(selectedNavigationEntry?.key)
			val wp: DisplayWaypoint? = navigationModel.selectedWaypoint
			if (wp == null) {
				focusTriggerController.focusState(listState, false)
				return
			}

			// prepare the app icon and title
			val icon = wp.icon?.let { graphicsHelpers.compress(it, 48, 48) } ?: ""

			val title = listOfNotNull(
				L.EVPLANNING_TITLE_WAYPOINT,
				when {
					navigationModel.isPlanning -> "[${L.EVPLANNING_REPLANNING}...]"
					navigationModel.isError -> "[${L.EVPLANNING_ERROR}]"
					navigationModel.shouldReplan -> "[${L.EVPLANNING_SHOULD_REPLAN}]"
					else -> null
				},
			).joinToString(" ")

			val name = listOfNotNull(
				wp.title ?: L.EVPLANNING_UNKNOWN_LOC,
				if (!navigationModel.selectedWaypointValid) {
					"[${L.EVPLANNING_INVALID}]"
				} else null,
			).joinToString(" ")

			val nameListData = RHMIModel.RaListModel.RHMIListConcrete(3)
			nameListData.addRow(arrayOf(icon, "", name))

			// prepare the title data
			var sidePictureWidth = 0
			val addressListData = RHMIModel.RaListModel.RHMIListConcrete(2)
			//		if (navigationEntry.sidePicture == null || navigationEntry.sidePicture.intrinsicHeight <= 0) {
			addressListData.addRow(arrayOf("", wp.address))
			//		} else {
			//			val sidePictureHeight = 96  // force the side picture to be this tall
			//			sidePictureWidth = (sidePictureHeight.toFloat() / navigationEntry.sidePicture.intrinsicHeight * navigationEntry.sidePicture.intrinsicWidth).toInt()
			//			val sidePicture = graphicsHelpers.compress(navigationEntry.sidePicture, sidePictureWidth, sidePictureHeight)
			//			titleListData.addRow(arrayOf(BMWRemoting.RHMIResourceData(BMWRemoting.RHMIResourceType.IMAGEDATA, sidePicture), navigationEntry.title + "\n"))
			//		}
			addressWidget.setProperty(
				RHMIProperty.PropertyId.LIST_COLUMNWIDTH.id,
				"$sidePictureWidth,*"
			)

			// prepare the notification text
			val descriptionListData = RHMIModel.RaListModel.RHMIListConcrete(1)
			val numChargers = wp.num_chargers?.let {
				if (it > 0) {
					"${it}(${
						wp.free_chargers?.let {
							if (it >= 0) {
								it
							} else null
						} ?: "-"
					})"
				} else {
					null
				}
			}
			val dist = when {
				wp.step_dst == null && wp.trip_dst != null -> formatDistance(wp.trip_dst)
				wp.step_dst != null && (wp.trip_dst == null || wp.step_dst == wp.trip_dst) -> formatDistance(
					wp.step_dst
				)
				wp.step_dst != null && wp.trip_dst != null -> "${formatDistance(wp.step_dst)} (${
					formatDistance(
						wp.trip_dst
					)
				})"
				else -> null
			}
			val charger = listOfNotNull(
				numChargers,
				wp.charger_type?.toUpperCase(Locale.ROOT),
			).joinToString(" ").takeIf { it.isNotEmpty() }
			val soc = if (wp.soc_ariv == null) {
				when {
					wp.soc_planned != null && wp.soc_dep == null -> "${
						String.format(
							"%.0f",
							wp.soc_planned
						)
					}%"
					wp.soc_planned == null && wp.soc_dep != null -> "${
						String.format(
							"%.0f",
							wp.soc_dep
						)
					}%"
					wp.soc_planned != null && wp.soc_dep != null -> "${
						String.format(
							"%.0f",
							wp.soc_planned
						)
					}%-${String.format("%.0f", wp.soc_dep)}%"
					else -> null
				}
			} else {
				when {
					wp.soc_planned != null && wp.soc_dep == null -> "${
						String.format(
							"%.1f",
							wp.soc_ariv
						)
					}% (${
						String.format(
							"%.0f",
							wp.soc_planned
						)
					}%)"
					wp.soc_planned == null && wp.soc_dep != null -> "${
						String.format(
							"%.1f",
							wp.soc_ariv
						)
					}% (${
						String.format(
							"%.0f",
							wp.soc_dep
						)
					}%)"
					wp.soc_planned != null && wp.soc_dep != null -> "${
						String.format(
							"%.1f",
							wp.soc_ariv
						)
					}% (${
						String.format(
							"%.0f",
							wp.soc_planned
						)
					}%-${String.format("%.0f", wp.soc_dep)}%)"
					else -> null
				}
			}
			val time = listOfNotNull(
				wp.duration?.let { "${it}min" },
				when {
					wp.eta != null && wp.etd == null -> "${wp.eta.format(TIME_FMT)}Uhr"
					wp.eta == null && wp.etd != null -> "${wp.etd.format(TIME_FMT)}Uhr"
					wp.eta != null && wp.etd != null -> "${wp.eta.format(TIME_FMT)}-${
						wp.etd.format(
							TIME_FMT
						)
					}Uhr"
					else -> null
				}
			).joinToString(" ").takeIf { it.isNotEmpty() }
			val text = listOfNotNull(
				if (wp.is_waypoint) L.EVPLANNING_WAYPOINT else null,
				wp.operator,
				charger,
				dist,
				soc,
				time,
			).joinToString("\n")
			descriptionListData.addRow(arrayOf(text))

			state.getTextModel()?.asRaDataModel()?.value = title
			nameWidget.getModel()?.value = nameListData
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
		} catch (t: Throwable) {
			Sentry.capture(t)
		}
	}
}
