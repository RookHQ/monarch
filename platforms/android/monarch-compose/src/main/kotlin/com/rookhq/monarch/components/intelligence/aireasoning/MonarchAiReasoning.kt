package com.rookhq.monarch.components.intelligence.aireasoning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.collapsible.MonarchCollapsible
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPartState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiReasoningPart
import com.rookhq.monarch.components.intelligence.aithinkingindicator.MonarchAiThinkingIndicator
import com.rookhq.monarch.components.intelligence.aithinkingindicator.MonarchAiThinkingIndicatorVariant
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

/**
 * The model's reasoning for one answer, as a disclosure: "Thinking…" with a
 * live timer while it streams, then "Thought for 12s".
 *
 * Built on `MonarchCollapsible`. Streaming reasoning starts open and closes
 * when it finishes, unless the person opened or closed it meanwhile; when a
 * finished part streams again (a regeneration) it opens and measures again.
 * The header shows the elapsed time after three seconds; the timer is not
 * announced. Before any text arrives it is a `MonarchAiThinkingIndicator`
 * with the assistant mark. Redacted reasoning is a still row that reads
 * "Reasoning hidden". The whole is a TalkBack group named "Assistant
 * reasoning"; the text can be selected and copied.
 *
 * @param part the reasoning part from the message model: its text, whether it streams, its duration and whether
 * it is redacted.
 * @param modifier the modifier of the reasoning.
 * @param startedAt when the reasoning started, in epoch milliseconds (UTC). Pass it when the view can leave the
 * composition while the part streams, for example in a lazy list, so the timer does not restart. `null` counts
 * from when streaming was first seen.
 * @param initiallyExpanded whether finished reasoning starts open. Streaming reasoning always starts open.
 * @param onExpandedChange called when the disclosure opens or closes, by the person or automatically when the
 * reasoning finishes or starts again.
 * @param icon the assistant mark of the thinking indicator and the still row, for example a `MonarchIcon` with a
 * `null` content description, tinted with the strong assistant colour. Monarch bundles no icons.
 * @param expandIcon an icon after the header, for example a chevron that points down; it turns while open.
 * @param labels replacements for the built-in strings, including the duration format.
 */
@Composable
public fun MonarchAiReasoning(
    part: MonarchAiReasoningPart,
    modifier: Modifier = Modifier,
    startedAt: Long? = null,
    initiallyExpanded: Boolean = false,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    expandIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    labels: MonarchAiReasoningLabels = MonarchAiReasoningLabels(),
) {
    AiReasoning(
        part = part,
        modifier = modifier,
        startedAt = startedAt,
        initiallyExpanded = initiallyExpanded,
        onExpandedChange = onExpandedChange,
        icon = icon,
        expandIcon = expandIcon,
        labels = labels,
        clock = System::currentTimeMillis,
    )
}

/** The reasoning with its clock injected, so tests drive the timer without waiting. */
@Composable
internal fun AiReasoning(
    part: MonarchAiReasoningPart,
    modifier: Modifier,
    startedAt: Long?,
    initiallyExpanded: Boolean,
    onExpandedChange: ((Boolean) -> Unit)?,
    icon: (@Composable () -> Unit)?,
    expandIcon: (@Composable () -> Unit)?,
    labels: MonarchAiReasoningLabels,
    clock: () -> Long,
) {
    val tokens = MonarchAiReasoningTokens
    val streaming = part.state == MonarchAiPartState.Streaming
    val state = rememberReasoningState(streaming, startedAt, initiallyExpanded, onExpandedChange, clock)
    val locale = LocalConfiguration.current.locales.get(0)
    val formatDuration: (Long) -> String = labels.duration ?: { tokens.duration(it, locale) }
    val prefix = monarchString(labels.prefix, R.string.monarch_aireasoning_prefix)
    val header = reasoningHeader(part, streaming, state, formatDuration, labels)
    when {
        tokens.hasBody(part) -> Column(modifier = modifier.groupNamed(prefix)) {
            MonarchCollapsible(
                expanded = state.expanded,
                onExpandedChange = { state.toggle(it, streaming) },
                // As the web: the chevron comes first, in the assistant colour, and turns down while open.
                header = {
                    if (expandIcon != null) {
                        val turn = if (expandIcon === MonarchDefaultIcons.ChevronDown) {
                            if (state.expanded) MonarchAiReasoningTokens.EXPANDED_TURN else 0f
                        } else {
                            0f
                        }
                        val quiet = LocalMonarchAiReasoningQuiet.current
                        CompositionLocalProvider(
                            LocalContentColor provides
                                if (quiet) MonarchTheme.colors.textMuted else MonarchTheme.colors.aiStrong,
                        ) {
                            Box(modifier = Modifier.graphicsLayer { rotationZ = turn }) {
                                if (expandIcon === MonarchDefaultIcons.ChevronDown) {
                                    MonarchDefaultIcons.ChevronRight()
                                } else {
                                    expandIcon()
                                }
                            }
                        }
                    }
                    if (LocalMonarchAiReasoningQuiet.current) {
                        Text(
                            text = header,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MonarchTheme.typography.label,
                            color = tokens.textColor(MonarchTheme.colors),
                        )
                    } else {
                        Text(text = header, modifier = Modifier.weight(1f, fill = false))
                    }
                },
            ) {
                SelectionContainer {
                    Text(
                        text = part.text,
                        style = MonarchTheme.typography.body,
                        color = tokens.textColor(MonarchTheme.colors),
                    )
                }
            }
        }
        streaming -> Box(modifier = modifier.groupNamed(prefix)) {
            MonarchAiThinkingIndicator(
                variant = MonarchAiThinkingIndicatorVariant.Mark,
                label = monarchString(labels.thinking, R.string.monarch_aireasoning_thinking),
                startedAt = state.start,
                showElapsedAfterMillis = tokens.SHOW_ELAPSED_AFTER_MILLIS,
                announce = false,
                icon = icon,
                elapsedLabel = { seconds -> formatDuration(seconds * tokens.MILLIS_PER_SECOND) },
            )
        }
        else -> StillRow(
            spoken = stringResource(R.string.monarch_aireasoning_spoken, prefix, header),
            header = header,
            icon = icon,
            modifier = modifier,
        )
    }
}

/** A TalkBack group named [name]. */
private fun Modifier.groupNamed(name: String): Modifier = semantics {
    isTraversalGroup = true
    contentDescription = name
}

/** The header text: thinking with the elapsed time, hidden, or thought for the duration. */
@Composable
private fun reasoningHeader(
    part: MonarchAiReasoningPart,
    streaming: Boolean,
    state: ReasoningState,
    formatDuration: (Long) -> String,
    labels: MonarchAiReasoningLabels,
): String {
    val tokens = MonarchAiReasoningTokens
    val thinking = monarchString(labels.thinking, R.string.monarch_aireasoning_thinking)
    val elapsed = state.elapsed()
    val duration = tokens.shownDuration(part, state.measured)
    return when {
        streaming && elapsed >= tokens.SHOW_ELAPSED_AFTER_MILLIS ->
            stringResource(R.string.monarch_aireasoning_thinking_elapsed, thinking, formatDuration(elapsed))
        streaming -> thinking
        part.redacted -> monarchString(labels.reasoningHidden, R.string.monarch_aireasoning_reasoning_hidden)
        duration != null -> {
            val text = formatDuration(duration)
            labels.thoughtFor?.invoke(text) ?: stringResource(R.string.monarch_aireasoning_thought_for, text)
        }
        else -> monarchString(labels.thought, R.string.monarch_aireasoning_thought)
    }
}

/** Whether the disclosure is open, whether the person touched it, and the timer. */
private class ReasoningState(
    initiallyExpanded: Boolean,
    start: Long?,
    now: Long,
    private val onExpandedChange: () -> ((Boolean) -> Unit)?,
) {
    var expanded by mutableStateOf(initiallyExpanded)
    var touched by mutableStateOf(false)
    var start by mutableStateOf(start)
    var measured by mutableStateOf<Long?>(null)
    var now by mutableLongStateOf(now)

    /** How long the current streaming has lasted, in milliseconds. */
    fun elapsed(): Long = start?.let { (now - it).coerceAtLeast(0L) } ?: 0L

    /** The person opened or closed the disclosure. */
    fun toggle(next: Boolean, streaming: Boolean) {
        if (streaming) touched = true
        expanded = next
        onExpandedChange()?.invoke(next)
    }

    /** Opens or closes the disclosure by itself. */
    fun setAutomatically(next: Boolean) {
        if (next != expanded) {
            expanded = next
            onExpandedChange()?.invoke(next)
        }
    }
}

@Composable
private fun rememberReasoningState(
    streaming: Boolean,
    startedAt: Long?,
    initiallyExpanded: Boolean,
    onExpandedChange: ((Boolean) -> Unit)?,
    clock: () -> Long,
): ReasoningState {
    val tokens = MonarchAiReasoningTokens
    val callback by rememberUpdatedState(onExpandedChange)
    val state = remember {
        ReasoningState(
            initiallyExpanded = streaming || initiallyExpanded,
            start = if (streaming) startedAt ?: clock() else null,
            now = clock(),
            onExpandedChange = { callback },
        )
    }
    var wasStreaming by remember { mutableStateOf(streaming) }
    LaunchedEffect(streaming) {
        if (streaming && state.start == null) state.start = startedAt ?: clock()
        if (!streaming && wasStreaming) {
            state.measured = state.start?.let { clock() - it }
            state.start = null
        }
        if (streaming && !wasStreaming) state.touched = false
        tokens.expandedAfter(wasStreaming, streaming, state.touched)?.let(state::setAutomatically)
        wasStreaming = streaming
        while (streaming) {
            state.now = clock()
            delay(tokens.TICK_MILLIS)
        }
    }
    return state
}

/** Reasoning without text to show: the mark and the header, read as one element. */
@Composable
private fun StillRow(spoken: String, header: String, icon: (@Composable () -> Unit)?, modifier: Modifier) {
    val tokens = MonarchAiReasoningTokens
    val colors = MonarchTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = tokens.StillRowMinHeight)
            .clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(tokens.IconGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides tokens.markColor(colors)) { icon() }
        }
        Text(text = header, style = MonarchTheme.typography.label, color = tokens.textColor(colors))
    }
}
