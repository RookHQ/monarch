package com.rookhq.monarch.components.layout.availabilitytoggle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.forms.switch.MonarchSwitch
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * Whether automatic assignment may give someone new work: a status word
 * ("Available" or "Away") with a mark, an optional hint and a
 * `MonarchSwitch`, as one row that toggles as a whole.
 *
 * The app owns the value: it saves the change, rolls back and tells the person
 * when saving fails, and sets [pending] while it saves. The row is one
 * TalkBack element with the switch role, named "Available for new work"; the
 * status word is its state, so the state is read in the same words that are
 * shown. The mark is a filled dot while available and a ring while away, so
 * the state never depends on colour. Show the toggle only once the value is
 * known.
 *
 * @param available whether the person is available for new work.
 * @param onAvailableChange called with the new value when the person toggles the row.
 * @param modifier the modifier of the row.
 * @param enabled whether the person may change it. `false` when they have no permission to.
 * @param pending a change is being saved: the switch cannot be toggled, a progress ring is shown and
 * TalkBack adds "Saving" to the state.
 * @param showHint shows one line under the status word that says what being available or away changes.
 * TalkBack reads it after the name.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the interaction source of the row.
 */
@Composable
public fun MonarchAvailabilityToggle(
    available: Boolean,
    onAvailableChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pending: Boolean = false,
    showHint: Boolean = false,
    labels: MonarchAvailabilityToggleLabels = MonarchAvailabilityToggleLabels(),
    interactionSource: MutableInteractionSource? = null,
) {
    val tokens = MonarchAvailabilityToggleTokens
    val colors = MonarchTheme.colors
    val interactive = tokens.interactive(enabled, pending)
    val name = monarchString(labels.label, R.string.monarch_availabilitytoggle_label)
    val status = if (available) {
        monarchString(labels.available, R.string.monarch_availabilitytoggle_available)
    } else {
        monarchString(labels.away, R.string.monarch_availabilitytoggle_away)
    }
    val savingWord = monarchString(labels.pending, R.string.monarch_availabilitytoggle_pending)
    val state = if (pending) {
        stringResource(R.string.monarch_availabilitytoggle_spoken_pending, status, savingWord)
    } else {
        status
    }
    val hint = if (available) {
        monarchString(labels.availableHint, R.string.monarch_availabilitytoggle_available_hint)
    } else {
        monarchString(labels.awayHint, R.string.monarch_availabilitytoggle_away_hint)
    }
    Row(
        modifier = modifier
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .toggleable(
                value = available,
                enabled = interactive,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onValueChange = onAvailableChange,
            )
            .semantics {
                contentDescription = name
                stateDescription = state
            }
            .heightIn(min = tokens.MinHeight)
            .padding(vertical = tokens.VerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
            Row(
                // The status word is the state of the row; it is not read a second time.
                modifier = Modifier.clearAndSetSemantics {},
                horizontalArrangement = Arrangement.spacedBy(tokens.MarkGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvailabilityMark(available = available, color = tokens.markColor(available, colors))
                Text(
                    text = status,
                    style = MonarchTheme.typography.label,
                    color = tokens.statusColor(enabled, colors),
                )
            }
            if (showHint) {
                Text(
                    text = hint,
                    style = MonarchTheme.typography.caption,
                    color = tokens.hintColor(enabled, colors),
                )
            }
        }
        if (pending) SavingRing()
        MonarchSwitch(checked = available, onCheckedChange = null, enabled = interactive)
    }
}

/** The decorative mark: a filled dot while available, a ring while away. */
@Composable
private fun AvailabilityMark(available: Boolean, color: Color) {
    val tokens = MonarchAvailabilityToggleTokens
    val size = tokens.MarkSize * LocalDensity.current.fontScale
    Canvas(modifier = Modifier.size(size)) {
        if (tokens.markFilled(available)) {
            drawCircle(color)
        } else {
            val stroke = tokens.MarkStroke.toPx()
            drawCircle(color = color, radius = (this.size.minDimension - stroke) / 2, style = Stroke(stroke))
        }
    }
}

/** The progress ring while saving; still when animations are off. Hidden from TalkBack, which reads the state. */
@Composable
private fun SavingRing() {
    val tokens = MonarchAvailabilityToggleTokens
    val modifier = Modifier.size(tokens.SpinnerSize).clearAndSetSemantics {}
    val color = MonarchTheme.colors.accentSolid
    val track = MonarchTheme.colors.bgEmphasis
    if (MonarchTheme.motion.reducedMotion) {
        CircularProgressIndicator(
            progress = { tokens.STILL_SPINNER_FRACTION },
            modifier = modifier,
            color = color,
            trackColor = track,
            strokeWidth = tokens.SpinnerStroke,
            gapSize = MonarchTokens.Space.space0.dp,
        )
    } else {
        CircularProgressIndicator(
            modifier = modifier,
            color = color,
            trackColor = track,
            strokeWidth = tokens.SpinnerStroke,
            gapSize = MonarchTokens.Space.space0.dp,
        )
    }
}
