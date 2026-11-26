package com.rookhq.monarch.screenshots.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.datadisplay.listrow.MonarchListRow
import com.rookhq.monarch.components.feedback.snackbarhost.MonarchSnackbarHost
import com.rookhq.monarch.components.feedback.snackbarhost.MonarchSnackbarVisuals
import com.rookhq.monarch.components.forms.checkboxrow.MonarchCheckboxRow
import com.rookhq.monarch.components.forms.datefield.MonarchDateField
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuDivider
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.overlays.popover.MonarchPopover
import com.rookhq.monarch.components.overlays.tooltip.MonarchTooltipBox
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.MonarchDatePickerDialog
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 4 March 2026 at midnight UTC: a fixed day, so the date picker image does not change with the calendar. */
private const val March4: Long = 1_772_582_400_000L

/**
 * The presented surfaces, each open in one realistic scenario. A dialog or a sheet is captured as a window
 * over its scrim; a menu, popover or tooltip next to the control that opened it.
 */
@OptIn(ExperimentalMaterial3Api::class)
internal val overlayShowcases: Map<String, @Composable () -> Unit> = mapOf(
    "confirm-dialog" to {
        MonarchDialog(
            onDismissRequest = {},
            title = "Delete project?",
            description = "Atlas and its 214 tasks are deleted for everyone in the workspace. This cannot be undone.",
            actions = {
                MonarchButton(text = "Cancel", onClick = {}, variant = MonarchButtonVariant.Ghost)
                MonarchButton(text = "Delete project", onClick = {}, variant = MonarchButtonVariant.Danger)
            },
        )
    },
    "dialog" to {
        MonarchDialog(
            onDismissRequest = {},
            title = "Rename board",
            description = "Everyone on the board sees the new name.",
            actions = {
                MonarchButton(text = "Cancel", onClick = {}, variant = MonarchButtonVariant.Ghost)
                MonarchButton(text = "Save", onClick = {}, variant = MonarchButtonVariant.Primary)
            },
        ) {
            MonarchTextField(
                value = "Q3 launch plan",
                onValueChange = {},
                label = "Board name",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    },
    "sheet" to {
        MonarchBottomSheet(
            onDismissRequest = {},
            title = "Share report",
            description = "Q3 revenue, updated today at 09:40.",
            fullWidthContent = true,
        ) {
            MonarchListRow(
                headlineContent = { MonarchText(text = "Copy link") },
                supportingContent = { MonarchText(text = "Anyone in Acme with the link can view") },
                leadingContent = { MonarchIcon(SampleIcons.Copy, contentDescription = null) },
                onClick = {},
            )
            MonarchListRow(
                headlineContent = { MonarchText(text = "Send by email") },
                leadingContent = { MonarchIcon(SampleIcons.Send, contentDescription = null) },
                onClick = {},
            )
            MonarchListRow(
                headlineContent = { MonarchText(text = "Invite people") },
                leadingContent = { MonarchIcon(SampleIcons.Users, contentDescription = null) },
                onClick = {},
            )
        }
    },
    "dropdown-menu" to {
        Column(Modifier.fillMaxWidth().height(340.dp)) {
            Box {
                MonarchButton(
                    text = "Actions",
                    onClick = {},
                    trailingIcon = { MonarchIcon(SampleIcons.ChevronDown, contentDescription = null) },
                )
                MonarchMenu(expanded = true, onDismissRequest = {}) {
                    MonarchMenuItem(
                        text = "Rename",
                        onClick = {},
                        leadingIcon = { MonarchIcon(SampleIcons.Pencil, contentDescription = null) },
                    )
                    MonarchMenuItem(
                        text = "Duplicate",
                        onClick = {},
                        shortcut = "Ctrl+D",
                        leadingIcon = { MonarchIcon(SampleIcons.Copy, contentDescription = null) },
                    )
                    MonarchMenuItem(
                        text = "Share",
                        onClick = {},
                        leadingIcon = { MonarchIcon(SampleIcons.Share, contentDescription = null) },
                    )
                    MonarchMenuDivider()
                    MonarchMenuItem(
                        text = "Delete",
                        onClick = {},
                        destructive = true,
                        leadingIcon = { MonarchIcon(SampleIcons.Trash, contentDescription = null) },
                    )
                }
            }
        }
    },
    "popover" to {
        Column(Modifier.fillMaxWidth().height(300.dp)) {
            Box {
                MonarchButton(
                    text = "Filter",
                    onClick = {},
                    leadingIcon = { MonarchIcon(SampleIcons.Filter, contentDescription = null) },
                )
                MonarchPopover(expanded = true, onDismissRequest = {}, label = "Filter invoices") {
                    MonarchText(text = "Filter invoices", style = MonarchTextStyle.Subheading, heading = true)
                    MonarchCheckboxRow(checked = true, onCheckedChange = {}, label = "Only unpaid")
                    MonarchCheckboxRow(checked = false, onCheckedChange = {}, label = "Due this week")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2, Alignment.End),
                    ) {
                        MonarchButton(text = "Reset", onClick = {}, variant = MonarchButtonVariant.Ghost)
                        MonarchButton(text = "Apply", onClick = {}, variant = MonarchButtonVariant.Primary)
                    }
                }
            }
        }
    },
    "tooltip" to {
        val state = rememberTooltipState(isPersistent = true)
        LaunchedEffect(state) { state.show() }
        Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.BottomCenter) {
            MonarchTooltipBox(text = "Add to favourites", state = state) {
                MonarchIconButton(label = "Add to favourites", onClick = {}, variant = MonarchButtonVariant.Secondary) {
                    MonarchIcon(SampleIcons.Star, contentDescription = null)
                }
            }
        }
    },
    "toast" to {
        val state = remember {
            SnackbarHostState().also { state ->
                CoroutineScope(Dispatchers.Unconfined).launch {
                    state.showSnackbar(
                        MonarchSnackbarVisuals(
                            message = "Invoice 1042 sent",
                            tone = MonarchTone.Success,
                            actionLabel = "Undo",
                            withDismissAction = true,
                            duration = SnackbarDuration.Indefinite,
                        ),
                    )
                }
            }
        }
        MonarchSnackbarHost(hostState = state)
    },
    "date-picker" to {
        val state = rememberDatePickerState(initialSelectedDateMillis = March4, initialDisplayedMonthMillis = March4)
        // The field that opened the dialog stays visible under the scrim, above the dialog.
        MonarchDateField(value = March4, onValueChange = {}, label = "Due date")
        MonarchDatePickerDialog(state, confirmLabel = "Set due date", cancelLabel = "Cancel", onPick = {}, onClose = {})
    },
)

