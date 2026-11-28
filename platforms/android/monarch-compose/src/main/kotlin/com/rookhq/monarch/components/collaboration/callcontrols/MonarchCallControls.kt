package com.rookhq.monarch.components.collaboration.callcontrols

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.forms.checkbox.MonarchCheckbox
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuDivider
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.overlays.menu.MonarchMenuLabel
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The controls of a call: microphone, camera, screen sharing, raised hand,
 * reactions, a More menu (layout, low data, devices, connection, picture in
 * picture) and Leave.
 *
 * Built from round Monarch toggles, round buttons that open `MonarchMenu`s
 * and a wide danger round control, in a row that wraps. It
 * is UI only: the app owns the call and every state is hoisted. A control is
 * shown only when its callback is given. Each toggle keeps one name
 * ("Microphone") and reports "On" or "Off" as its state; the icon slot is
 * asked for the glyph of each control and state, so a change is never shown by
 * colour alone.
 *
 * @param icon draws the glyph of a control, given whether it is on, for example a slashed microphone when the
 * microphone is off. Monarch bundles no icons, so it is required. Give it a `null` content description: the
 * controls are named by Monarch.
 * @param modifier the modifier of the row.
 * @param micOn whether the microphone is on.
 * @param onMicOnChange shows the microphone toggle; called with the new value.
 * @param cameraOn whether the camera is on.
 * @param onCameraOnChange shows the camera toggle.
 * @param sharing whether the screen is shared.
 * @param onSharingChange shows the screen sharing toggle.
 * @param handRaised whether the hand is raised.
 * @param onHandRaisedChange shows the raised hand toggle (not in the minimal form).
 * @param layout the stage layout.
 * @param onLayoutChange adds the layout choice to More.
 * @param lowData whether audio-only mode is on.
 * @param onLowDataChange adds the low data item to More.
 * @param reactions the emoji offered in the reactions menu. An empty list hides it.
 * @param minimal leaves out the hand, reactions and More, for a small window.
 * @param enabled whether the controls can be used.
 * @param labels replacements for the built-in strings.
 * @param onReact sends a reaction; shows the reactions menu.
 * @param onOpenDevices opens the app's device settings; adds Devices to More.
 * @param onOpenStats opens the app's connection details; adds Connection and data use to More.
 * @param onPictureInPicture continues the call in picture in picture; adds an item to More.
 * @param onLeave leaves the call; shows the Leave button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchCallControls(
    icon: @Composable (control: MonarchCallControl, on: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    micOn: Boolean = true,
    onMicOnChange: ((Boolean) -> Unit)? = null,
    cameraOn: Boolean = true,
    onCameraOnChange: ((Boolean) -> Unit)? = null,
    sharing: Boolean = false,
    onSharingChange: ((Boolean) -> Unit)? = null,
    handRaised: Boolean = false,
    onHandRaisedChange: ((Boolean) -> Unit)? = null,
    layout: MonarchCallControlsLayout = MonarchCallControlsLayout.Automatic,
    onLayoutChange: ((MonarchCallControlsLayout) -> Unit)? = null,
    lowData: Boolean = false,
    onLowDataChange: ((Boolean) -> Unit)? = null,
    reactions: List<String> = MonarchCallControlsDefaults.Reactions,
    minimal: Boolean = false,
    enabled: Boolean = true,
    labels: MonarchCallControlsLabels = MonarchCallControlsLabels(),
    onReact: ((String) -> Unit)? = null,
    onOpenDevices: (() -> Unit)? = null,
    onOpenStats: (() -> Unit)? = null,
    onPictureInPicture: (() -> Unit)? = null,
    onLeave: (() -> Unit)? = null,
) {
    val tokens = MonarchCallControlsTokens
    val groupName = monarchString(labels.toolbar, R.string.monarch_callcontrols_toolbar)
    val toggle = CallToggleScope(icon, enabled, labels)
    FlowRow(
        modifier = modifier.semantics {
            isTraversalGroup = true
            contentDescription = groupName
        },
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        onMicOnChange?.let {
            val name = R.string.monarch_callcontrols_microphone
            toggle.Toggle(MonarchCallControl.Microphone, micOn, it, labels.microphone, name)
        }
        onCameraOnChange?.let {
            toggle.Toggle(MonarchCallControl.Camera, cameraOn, it, labels.camera, R.string.monarch_callcontrols_camera)
        }
        onSharingChange?.let {
            toggle.Toggle(
                MonarchCallControl.ShareScreen,
                sharing,
                it,
                labels.shareScreen,
                R.string.monarch_callcontrols_share_screen,
            )
        }
        if (tokens.showsHand(minimal, onHandRaisedChange != null) && onHandRaisedChange != null) {
            toggle.Toggle(
                MonarchCallControl.RaiseHand,
                handRaised,
                onHandRaisedChange,
                labels.raiseHand,
                R.string.monarch_callcontrols_raise_hand,
            )
        }
        if (tokens.showsReactions(minimal, reactions, onReact != null) && onReact != null) {
            Reactions(icon, reactions, enabled, labels, onReact)
        }
        val moreItems = listOf(onLayoutChange, onLowDataChange, onOpenDevices, onOpenStats, onPictureInPicture)
        val showsMore = tokens.showsMore(minimal, moreItems.count { it != null })
        if (showsMore) {
            More(
                icon = icon,
                enabled = enabled,
                labels = labels,
                layout = layout,
                onLayoutChange = onLayoutChange,
                lowData = lowData,
                onLowDataChange = onLowDataChange,
                onOpenDevices = onOpenDevices,
                onOpenStats = onOpenStats,
                onPictureInPicture = onPictureInPicture,
            )
        }
        if (onLeave != null) {
            val leave = monarchString(labels.leave, R.string.monarch_callcontrols_leave)
            val colors = MonarchTheme.colors
            val leaveColors = if (enabled) {
                MonarchSurfaceColors(colors.dangerSolid, colors.dangerOnSolid, null)
            } else {
                tokens.colors(MonarchCallControl.Leave, on = false, enabled = false, colors = colors)
            }
            RoundControl(
                surface = leaveColors,
                wide = true,
                modifier = Modifier
                    .clickable(enabled = enabled, role = Role.Button, onClick = onLeave)
                    .semantics { contentDescription = leave },
            ) {
                icon(MonarchCallControl.Leave, false)
            }
        }
    }
}

/** What every round toggle of the controls shares. */
private class CallToggleScope(
    val icon: @Composable (control: MonarchCallControl, on: Boolean) -> Unit,
    val enabled: Boolean,
    val labels: MonarchCallControlsLabels,
) {
    @Composable
    fun Toggle(
        control: MonarchCallControl,
        on: Boolean,
        onChange: (Boolean) -> Unit,
        override: String?,
        @StringRes name: Int,
    ) {
        val themeColors = MonarchTheme.colors
        val tokens = MonarchCallControlsTokens
        val off = tokens.colors(control, on = false, enabled = true, colors = themeColors)
        val onColors = tokens.colors(control, on = true, enabled = true, colors = themeColors)
        val disabled = tokens.colors(control, on, enabled = false, colors = themeColors)
        val label = monarchString(override, name)
        val state = if (on) {
            monarchString(labels.on, R.string.monarch_callcontrols_on)
        } else {
            monarchString(labels.off, R.string.monarch_callcontrols_off)
        }
        val surface = when {
            !enabled -> disabled
            on -> onColors
            else -> off
        }
        RoundControl(
            surface = surface,
            modifier = Modifier
                .toggleable(value = on, enabled = enabled, role = Role.Checkbox, onValueChange = onChange)
                .semantics {
                    contentDescription = label
                    stateDescription = state
                },
        ) {
            icon(control, on)
        }
    }
}

/** A round button that opens a menu, named [label]. */
@Composable
private fun MenuButton(
    control: MonarchCallControl,
    label: String,
    icon: @Composable (control: MonarchCallControl, on: Boolean) -> Unit,
    enabled: Boolean,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val tokens = MonarchCallControlsTokens
    val themeColors = MonarchTheme.colors
    val resting = tokens.colors(control, on = false, enabled = true, colors = themeColors)
    val disabled = tokens.colors(control, on = false, enabled = false, colors = themeColors)
    Box {
        RoundControl(
            surface = if (enabled) resting else disabled,
            modifier = Modifier
                .clickable(enabled = enabled, role = Role.Button) { expanded = true }
                .semantics { contentDescription = label },
        ) {
            icon(control, false)
        }
        MonarchMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            content { expanded = false }
        }
    }
}

@Composable
private fun Reactions(
    icon: @Composable (control: MonarchCallControl, on: Boolean) -> Unit,
    reactions: List<String>,
    enabled: Boolean,
    labels: MonarchCallControlsLabels,
    onReact: (String) -> Unit,
) {
    val label = monarchString(labels.react, R.string.monarch_callcontrols_react)
    MenuButton(MonarchCallControl.React, label, icon, enabled) { close ->
        reactions.forEach { emoji ->
            val name = labels.reactWith?.invoke(emoji)
                ?: stringResource(R.string.monarch_callcontrols_react_with, emoji)
            MonarchMenuItem(
                text = emoji,
                onClick = {
                    onReact(emoji)
                    close()
                },
                modifier = Modifier.semantics { contentDescription = name },
            )
        }
    }
}

@Composable
private fun More(
    icon: @Composable (control: MonarchCallControl, on: Boolean) -> Unit,
    enabled: Boolean,
    labels: MonarchCallControlsLabels,
    layout: MonarchCallControlsLayout,
    onLayoutChange: ((MonarchCallControlsLayout) -> Unit)?,
    lowData: Boolean,
    onLowDataChange: ((Boolean) -> Unit)?,
    onOpenDevices: (() -> Unit)?,
    onOpenStats: (() -> Unit)?,
    onPictureInPicture: (() -> Unit)?,
) {
    val label = monarchString(labels.more, R.string.monarch_callcontrols_more)
    MenuButton(MonarchCallControl.More, label, icon, enabled) { close ->
        if (onLayoutChange != null) {
            LayoutChoices(layout, onLayoutChange, labels, close)
            MonarchMenuDivider()
        }
        if (onLowDataChange != null) {
            MonarchMenuItem(
                onClick = { onLowDataChange(!lowData) },
                modifier = Modifier.semantics {
                    role = Role.Checkbox
                    toggleableState = ToggleableState(lowData)
                },
                // The item carries the state; the check box only shows it.
                trailingContent = { MonarchCheckbox(checked = lowData, onCheckedChange = null) },
            ) { Text(monarchString(labels.lowData, R.string.monarch_callcontrols_low_data)) }
        }
        listOfNotNull(
            onOpenDevices?.let { monarchString(labels.devices, R.string.monarch_callcontrols_devices) to it },
            onOpenStats?.let { monarchString(labels.stats, R.string.monarch_callcontrols_stats) to it },
            onPictureInPicture?.let {
                monarchString(labels.pictureInPicture, R.string.monarch_callcontrols_picture_in_picture) to it
            },
        ).forEach { (text, action) ->
            MonarchMenuItem(
                text = text,
                onClick = {
                    action()
                    close()
                },
            )
        }
    }
}

@Composable
private fun LayoutChoices(
    layout: MonarchCallControlsLayout,
    onLayoutChange: (MonarchCallControlsLayout) -> Unit,
    labels: MonarchCallControlsLabels,
    close: () -> Unit,
) {
    MonarchMenuLabel(text = monarchString(labels.layout, R.string.monarch_callcontrols_layout))
    MonarchCallControlsLayout.entries.forEach { choice ->
        val name = labels.layoutName?.invoke(choice) ?: stringResource(layoutName(choice))
        MonarchMenuItem(
            text = name,
            onClick = {
                onLayoutChange(choice)
                close()
            },
            modifier = Modifier.semantics { role = Role.RadioButton },
            selected = choice == layout,
        )
    }
}

@StringRes
private fun layoutName(layout: MonarchCallControlsLayout): Int = when (layout) {
    MonarchCallControlsLayout.Automatic -> R.string.monarch_callcontrols_layout_automatic
    MonarchCallControlsLayout.Grid -> R.string.monarch_callcontrols_layout_grid
    MonarchCallControlsLayout.Speaker -> R.string.monarch_callcontrols_layout_speaker
}

/** A round control: its fill and icon colour from the tokens, the focus ring and the platform ripple. */
@Composable
private fun RoundControl(
    surface: MonarchSurfaceColors,
    modifier: Modifier,
    wide: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .monarchFocusRing(CircleShape)
            .then(modifier)
            .size(
                width = if (wide) MonarchCallControlsTokens.WideWidth else MonarchCallControlsTokens.ControlSize,
                height = MonarchCallControlsTokens.ControlSize,
            )
            .clip(CircleShape)
            .background(surface.container),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides surface.content, content = content)
    }
}
