package com.rookhq.monarch.components.intelligence.aitoolcall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadge
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallState
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme
import java.text.NumberFormat

/**
 * One tool call of an assistant turn as a card that opens: what was done in
 * plain words, the state as a word in a status badge, how long it took and,
 * on demand, the exact input and output.
 *
 * Built by Monarch from a clickable header with the expand and collapse
 * semantics of `MonarchCollapsible`, `AnimatedVisibility`, a
 * `MonarchStatusBadge` for the state and `MonarchAiCodeBlock` for the input
 * and output. `MonarchCollapsible` takes only a text label, so the header,
 * which also holds the state and the duration, is built here the same way.
 * A failed call shows its error under the card without opening it. A change
 * of state after the card first appears is announced as "<tool>: <state>"
 * through a polite live region.
 *
 * @param part the call from the conversation model: name, input, state, output, error, times and streamed input.
 * @param modifier the modifier of the card.
 * @param summary the one-line summary in words, such as "Searched documents for “Q3 plan”". Wins over [describeTool].
 * @param toolLabels display names by tool name. Unlisted tools get a humanized name: `search_documents` is
 * "Search documents".
 * @param expanded whether the details are shown, when the app controls it. `null` keeps the state in the card,
 * closed at first.
 * @param onExpandedChange called with the new state when the header is activated.
 * @param announcesChanges whether a change of state is announced. Turn it off when the thread already announces it.
 * @param maxValueLength the longest input or output shown before Show all, in characters. `0` shows everything.
 * @param icon an icon before the title, for example a wrench as a `MonarchIcon` with a `null` content description.
 * It takes the AI tool colour and is decorative. Monarch bundles no icons, so there is no default.
 * @param stateIcon an icon inside the state badge for a state. Decorative: the state is always a word too.
 * @param toggleIcon an icon at the start of the header, for example a chevron that points to the end. It turns a
 * quarter turn while expanded.
 * @param labels replacements for every built-in string.
 * @param compact shows the call on one line, as a chat message does: a check for a finished call, the tool's name in
 * a monospace face, then the state word and the duration.
 * @param describeTool builds the summary from the call, for example from its input. Return `null` to fall back to
 * the tool's label.
 */
@Composable
public fun MonarchAiToolCall(
    part: MonarchAiToolCallPart,
    modifier: Modifier = Modifier,
    summary: String? = null,
    toolLabels: Map<String, String> = emptyMap(),
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    announcesChanges: Boolean = true,
    maxValueLength: Int = MonarchAiToolCallTokens.DEFAULT_MAX_VALUE_LENGTH,
    icon: (@Composable () -> Unit)? = null,
    stateIcon: (@Composable (MonarchAiToolCallState) -> Unit)? = null,
    toggleIcon: (@Composable () -> Unit)? = null,
    labels: MonarchAiToolCallLabels = MonarchAiToolCallLabels(),
    describeTool: ((MonarchAiToolCallPart) -> String?)? = null,
    compact: Boolean = false,
) {
    val tokens = MonarchAiToolCallTokens
    var expandedInside by rememberSaveable { mutableStateOf(false) }
    val open = expanded ?: expandedInside
    val setOpen: (Boolean) -> Unit = {
        if (expanded == null) expandedInside = it
        onExpandedChange?.invoke(it)
    }
    val label = tokens.label(part.name, toolLabels)
    val title = summary ?: describeTool?.invoke(part) ?: label
    val stateText = labels.state?.invoke(part.state) ?: stringResource(tokens.stateText(part.state))
    val announcement = stateAnnouncement(part.state, label, stateText, labels).takeIf { announcesChanges }
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .monarchSurface(tokens.cardColors(MonarchTheme.colors), MonarchTheme.shapes.md),
        ) {
            ToolCallHeader(part, title, stateText, open, setOpen, icon, stateIcon, toggleIcon, labels, compact)
            AnimatedVisibility(
                visible = open,
                enter = expandVertically(MonarchTheme.motion.spec()) + fadeIn(MonarchTheme.motion.spec()),
                exit = shrinkVertically(MonarchTheme.motion.spec()) + fadeOut(MonarchTheme.motion.spec()),
            ) {
                MonarchAiToolCallDetails(part, maxValueLength, labels)
            }
        }
        val error = part.error
        if (part.state == MonarchAiToolCallState.Failed && !error.isNullOrBlank()) {
            val errorLabel = stringResource(
                R.string.monarch_aitoolcall_labelled,
                monarchString(labels.error, R.string.monarch_aitoolcall_error),
                error,
            )
            Text(
                text = error,
                modifier = Modifier
                    .padding(top = tokens.LineGap)
                    .semantics { contentDescription = errorLabel },
                style = MonarchTheme.typography.caption,
                color = MonarchTheme.colors.dangerStrong,
            )
        }
        // Always present, so a new state is announced as a change of this live region.
        Box(
            Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (!announcement.isNullOrEmpty()) contentDescription = announcement
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolCallHeader(
    part: MonarchAiToolCallPart,
    title: String,
    stateText: String,
    open: Boolean,
    setOpen: (Boolean) -> Unit,
    icon: (@Composable () -> Unit)?,
    stateIcon: (@Composable (MonarchAiToolCallState) -> Unit)?,
    toggleIcon: (@Composable () -> Unit)?,
    labels: MonarchAiToolCallLabels,
    compact: Boolean,
) {
    val tokens = MonarchAiToolCallTokens
    val colors = MonarchTheme.colors
    val state = if (open) {
        monarchString(labels.expanded, R.string.monarch_aitoolcall_expanded)
    } else {
        monarchString(labels.collapsed, R.string.monarch_aitoolcall_collapsed)
    }
    val rotation by animateFloatAsState(
        targetValue = if (open) tokens.EXPANDED_ROTATION else 0f,
        animationSpec = MonarchTheme.motion.spec(),
        label = "tool call toggle",
    )
    val duration = tokens.duration(part)?.let { durationText(it, labels) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .monarchFocusRing(MonarchTheme.shapes.md)
            .clip(MonarchTheme.shapes.md)
            .clickable(indication = ripple(), interactionSource = null, role = Role.Button) { setOpen(!open) }
            .semantics {
                stateDescription = state
                if (open) {
                    collapse {
                        setOpen(false)
                        true
                    }
                } else {
                    expand {
                        setOpen(true)
                        true
                    }
                }
            }
            .heightIn(min = tokens.HeaderMinHeight)
            .padding(tokens.Padding),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (toggleIcon != null) {
            CompositionLocalProvider(LocalContentColor provides colors.textMuted) {
                Box(Modifier.graphicsLayer { rotationZ = rotation }) { toggleIcon() }
            }
        }
        if (icon != null) CompositionLocalProvider(LocalContentColor provides colors.aiTool, content = icon)
        if (compact) {
            FlowRow(
                modifier = Modifier.weight(1f).clearAndSetSemantics { text = AnnotatedString("$title, $stateText") },
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (part.state == MonarchAiToolCallState.Succeeded) {
                    CompositionLocalProvider(LocalContentColor provides colors.successStrong) {
                        MonarchDefaultIcons.Check()
                    }
                }
                Text(text = title, style = MonarchTheme.typography.code, color = colors.textDefault)
                Text(text = stateText, style = MonarchTheme.typography.caption, color = colors.textMuted)
                if (duration != null) {
                    Text(text = duration, style = MonarchTheme.typography.caption, color = colors.textMuted)
                }
            }
        } else Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
            Text(text = title, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                // The badge is its own TalkBack element; here its word is read as part of the header instead.
                Box(Modifier.clearAndSetSemantics { text = AnnotatedString(stateText) }) {
                    MonarchStatusBadge(
                        definition = MonarchStatusDefinition(
                            label = stateText,
                            tone = tokens.tone(part.state),
                            icon = stateIcon?.let { slot -> { slot(part.state) } },
                        ),
                        size = MonarchStatusBadgeSize.Sm,
                    )
                }
                if (duration != null) {
                    Text(text = duration, style = MonarchTheme.typography.caption, color = colors.textMuted)
                }
            }
        }
    }
}

@Composable
private fun durationText(millis: Long, labels: MonarchAiToolCallLabels): String {
    labels.formatDuration?.let { return it(millis) }
    val (seconds, minutes, rest) = MonarchAiToolCallTokens.durationParts(millis)
    if (seconds != null) {
        val locale = LocalConfiguration.current.locales[0]
        val number = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }.format(seconds)
        return stringResource(R.string.monarch_aitoolcall_duration_seconds, number)
    }
    return if (minutes == 0L) {
        stringResource(R.string.monarch_aitoolcall_duration_seconds, rest.toString())
    } else {
        stringResource(R.string.monarch_aitoolcall_duration_minutes, minutes, rest)
    }
}

/** The announcement of the current state: empty until the state first changes after the card appeared. */
@Composable
private fun stateAnnouncement(
    state: MonarchAiToolCallState,
    label: String,
    stateText: String,
    labels: MonarchAiToolCallLabels,
): String {
    val first = remember { state }
    var changed by remember { mutableStateOf(false) }
    LaunchedEffect(state) { if (state != first) changed = true }
    if (!changed) return ""
    return labels.stateChanged?.invoke(label, stateText)
        ?: stringResource(R.string.monarch_aitoolcall_state_changed, label, stateText)
}
