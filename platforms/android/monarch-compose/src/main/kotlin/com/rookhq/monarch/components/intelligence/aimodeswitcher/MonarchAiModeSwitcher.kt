package com.rookhq.monarch.components.intelligence.aimodeswitcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Switches how the assistant works (Ask, Edit, Agent by default) with the
 * selected mode's description underneath, so the consequence of the choice
 * is always visible.
 *
 * Built on `MonarchSegmentedControl`, for two to five short modes. TalkBack
 * reads each segment as a radio button with its position and selected state,
 * and the group by [label]. The selected segment is the raised pill, so the
 * choice does not rest on colour. A disabled mode can never become the
 * selection. The description line is a polite live region, so switching
 * modes also reads what the new mode does.
 *
 * @param selection the id of the selected mode. An id that matches no mode selects nothing.
 * @param onSelectionChange called with the id of the mode the user chose. Never called with a disabled mode.
 * @param modifier the modifier of the switcher.
 * @param modes the modes, two to five. The default is [MonarchAiMode.defaults].
 * @param size [MonarchAiModeSwitcherSize.Sm] for a prompt toolbar, [MonarchAiModeSwitcherSize.Md] elsewhere.
 * @param enabled whether the user can change the mode, for example `false` while a response runs.
 * @param showDescription whether the selected mode's description is shown under the control. Turn it off
 * when it is shown elsewhere.
 * @param label the TalkBack name of the choice. `null` uses `monarch_aimodeswitcher_label`, "Mode".
 */
@Composable
public fun MonarchAiModeSwitcher(
    selection: String,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    modes: List<MonarchAiMode> = MonarchAiMode.defaults(),
    size: MonarchAiModeSwitcherSize = MonarchAiModeSwitcherSize.Sm,
    enabled: Boolean = true,
    showDescription: Boolean = true,
    label: String? = null,
) {
    val name = monarchString(label, R.string.monarch_aimodeswitcher_label)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MonarchAiModeSwitcherTokens.Gap)) {
        MonarchSegmentedControl(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    isTraversalGroup = true
                    contentDescription = name
                },
            size = MonarchAiModeSwitcherTokens.controlSize(size),
            fillWidth = true,
        ) {
            modes.forEach { mode ->
                val selected = mode.id == selection
                val iconOnly = MonarchAiModeSwitcherTokens.showsIconOnly(mode)
                Segment(
                    selected = selected,
                    onClick = {
                        if (!selected && MonarchAiModeSwitcherTokens.accepts(mode.id, modes)) {
                            onSelectionChange(mode.id)
                        }
                    },
                    enabled = enabled && mode.enabled,
                    icon = if (iconOnly) null else mode.icon?.let { icon -> { Decorative(icon) } },
                ) {
                    if (iconOnly) {
                        Box(Modifier.clearAndSetSemantics { contentDescription = mode.label }) {
                            mode.icon?.invoke()
                        }
                    } else {
                        Text(text = mode.label)
                    }
                }
            }
        }
        if (MonarchAiModeSwitcherTokens.describes(modes, showDescription)) {
            Text(
                text = MonarchAiModeSwitcherTokens.description(selection, modes).orEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { liveRegion = LiveRegionMode.Polite },
                style = MonarchTheme.typography.caption,
                color = MonarchAiModeSwitcherTokens.descriptionColor(enabled, MonarchTheme.colors),
            )
        }
    }
}

@Composable
private fun Decorative(icon: @Composable () -> Unit) {
    Box(Modifier.clearAndSetSemantics { }) { icon() }
}
