package com.rookhq.monarch.components.intelligence.aiinlinediff

import android.content.res.Resources
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * AI-suggested changes shown in place, inside the text, as marked insertions
 * and deletions. One change at a time is active: it is outlined, the bar
 * above reads "Change 2 of 5" with Previous and Next, and Accept and Reject
 * below the text decide it. Read-only text for now; marks inside an editor
 * are a later addition.
 *
 * Built by Monarch on a Compose `Text` with an `AnnotatedString`: removed
 * text is struck through and added text underlined in the AI diff colours,
 * so nothing depends on colour. Tapping a change makes it active. The
 * component is controlled: it reports decisions with [onAccept] and
 * [onReject] and the app updates [segments], for example with
 * [MonarchAiDiffSegment.resolve].
 *
 * TalkBack reads the text as one element with "Deleted:" and "Inserted:"
 * before removed and added runs and the position as its state; its custom
 * actions move between changes and decide the active one, so a tap on the
 * text is never the only way. Accept and Reject are named with the change,
 * "Accept change 2 of 5", and each decision is announced through a polite
 * live region that is always present.
 *
 * @param segments the text as unchanged, added and removed runs in reading order, for example from
 * [MonarchAiDiffSegment.diffWords].
 * @param modifier the modifier of the diff.
 * @param onAccept accepts a change by its id; shows Accept for the active change.
 * @param onReject rejects a change by its id; shows Reject for the active change.
 * @param showNavigation shows the bar with the position (or "No suggested changes") and Previous and Next.
 * @param labels replacements for the built-in strings.
 */
@OptIn(ExperimentalLayoutApi::class)
@ExperimentalMonarchApi
@Composable
public fun MonarchAiInlineDiff(
    segments: List<MonarchAiDiffSegment>,
    modifier: Modifier = Modifier,
    onAccept: ((id: String) -> Unit)? = null,
    onReject: ((id: String) -> Unit)? = null,
    showNavigation: Boolean = true,
    labels: MonarchAiInlineDiffLabels = MonarchAiInlineDiffLabels(),
) {
    val colors = MonarchTheme.colors
    val resources = LocalResources.current
    val changes = remember(segments) { MonarchAiDiffSegment.changes(segments) }
    val total = changes.size
    var activeRaw by rememberSaveable { mutableIntStateOf(0) }
    val active = activeRaw.coerceAtMost((total - 1).coerceAtLeast(0))
    var announcement by remember { mutableStateOf("") }
    val styled = remember(segments, changes, colors) { AiInlineDiffText.of(segments, changes, colors) }
    val texts = aiInlineDiffTexts(labels, active + 1, total)
    val go: (Int) -> Unit = { step -> activeRaw = AiInlineDiffText.move(active, step, total) }
    val decide: (Boolean) -> Unit = { accepted ->
        changes.getOrNull(active)?.let { change ->
            announcement = announce(resources, labels, accepted, total - 1)
            if (accepted) onAccept?.invoke(change.id) else onReject?.invoke(change.id)
        }
    }
    Column(
        modifier = modifier.semantics {
            isTraversalGroup = true
            contentDescription = texts.label
        },
        verticalArrangement = Arrangement.spacedBy(MonarchAiInlineDiffTokens.Gap),
    ) {
        if (showNavigation) NavigationBar(total, texts, go)
        DiffText(
            styled = styled,
            spoken = AiInlineDiffText.spoken(segments, texts.deleted, texts.inserted),
            active = active.takeIf { total > 0 },
            texts = texts,
            actions = AiInlineDiffActions(
                onMove = go.takeIf { total > 1 },
                onAccept = onAccept?.let { { decide(true) } }.takeIf { total > 0 },
                onReject = onReject?.let { { decide(false) } }.takeIf { total > 0 },
            ),
            onTapChange = { activeRaw = it },
        )
        if (total > 0 && (onAccept != null || onReject != null)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MonarchAiInlineDiffTokens.ButtonGap),
                verticalArrangement = Arrangement.spacedBy(MonarchAiInlineDiffTokens.ButtonGap),
            ) {
                if (onAccept != null) {
                    MonarchButton(
                        text = texts.accept,
                        leadingIcon = MonarchDefaultIcons.Check,
                        onClick = { decide(true) },
                        modifier = Modifier.semantics { contentDescription = texts.acceptChange },
                        variant = MonarchButtonVariant.Primary,
                        size = MonarchButtonSize.Sm,
                    )
                }
                if (onReject != null) {
                    MonarchButton(
                        text = texts.reject,
                        leadingIcon = MonarchDefaultIcons.Close,
                        onClick = { decide(false) },
                        modifier = Modifier.semantics { contentDescription = texts.rejectChange },
                        size = MonarchButtonSize.Sm,
                    )
                }
            }
        }
        // Always present, so each decision is announced once as a change of this live region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** What the text element can do: move between changes and decide the active one. */
private class AiInlineDiffActions(
    val onMove: ((Int) -> Unit)?,
    val onAccept: (() -> Unit)?,
    val onReject: (() -> Unit)?,
)

@Composable
private fun NavigationBar(total: Int, texts: AiInlineDiffTexts, onMove: (Int) -> Unit) {
    // As on the web: the position at the start, the two arrows at the end, with no words.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MonarchAiInlineDiffTokens.ButtonGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (total > 0) texts.position else texts.none,
            modifier = Modifier.weight(1f),
            style = MonarchTheme.typography.caption,
            color = MonarchAiInlineDiffTokens.muted(MonarchTheme.colors),
        )
        if (total > 0) {
            MonarchIconButton(
                label = texts.previous,
                onClick = { onMove(-1) },
                size = MonarchButtonSize.Sm,
                enabled = total > 1,
                icon = MonarchDefaultIcons.ChevronUp,
            )
            MonarchIconButton(
                label = texts.next,
                onClick = { onMove(1) },
                size = MonarchButtonSize.Sm,
                enabled = total > 1,
                icon = MonarchDefaultIcons.ChevronDown,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun DiffText(
    styled: AiInlineDiffText,
    spoken: String,
    active: Int?,
    texts: AiInlineDiffTexts,
    actions: AiInlineDiffActions,
    onTapChange: (Int) -> Unit,
) {
    val outline = MonarchAiInlineDiffTokens.activeOutline(MonarchTheme.colors)
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = styled.text,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val range = active?.let { styled.ranges.getOrNull(it) } ?: return@drawBehind
                val result = layout ?: return@drawBehind
                if (range.isEmpty() || range.last >= result.layoutInput.text.length) return@drawBehind
                val path = result.getPathForRange(range.first, range.last + 1)
                drawPath(path, outline, style = Stroke(MonarchAiInlineDiffTokens.OutlineWidth.toPx()))
            }
            .pointerInput(styled) {
                detectTapGestures { position ->
                    val result = layout ?: return@detectTapGestures
                    styled.changeAt(result.getOffsetForPosition(position))?.let(onTapChange)
                }
            }
            .semantics {
                contentDescription = spoken
                if (active != null) stateDescription = texts.position
                customActions = listOfNotNull(
                    actions.onMove?.let { move -> CustomAccessibilityAction(texts.next) { move(1); true } },
                    actions.onMove?.let { move -> CustomAccessibilityAction(texts.previous) { move(-1); true } },
                    actions.onAccept?.let { accept ->
                        CustomAccessibilityAction(texts.acceptChange) { accept(); true }
                    },
                    actions.onReject?.let { reject ->
                        CustomAccessibilityAction(texts.rejectChange) { reject(); true }
                    },
                )
            },
        style = MonarchTheme.typography.body,
        color = MonarchAiInlineDiffTokens.text(MonarchTheme.colors),
        onTextLayout = { layout = it },
    )
}

/** The strings of one composition, with the labels applied. */
@Suppress("LongParameterList")
private class AiInlineDiffTexts(
    val label: String,
    val deleted: String,
    val inserted: String,
    val position: String,
    val none: String,
    val previous: String,
    val next: String,
    val accept: String,
    val reject: String,
    val acceptChange: String,
    val rejectChange: String,
)

@Composable
private fun aiInlineDiffTexts(labels: MonarchAiInlineDiffLabels, index: Int, total: Int): AiInlineDiffTexts =
    AiInlineDiffTexts(
        label = monarchString(labels.label, R.string.monarch_aiinlinediff_label),
        deleted = monarchString(labels.deleted, R.string.monarch_aiinlinediff_deleted),
        inserted = monarchString(labels.inserted, R.string.monarch_aiinlinediff_inserted),
        position = labels.position?.invoke(index, total)
            ?: stringResource(R.string.monarch_aiinlinediff_position, index, total),
        none = monarchString(labels.none, R.string.monarch_aiinlinediff_none),
        previous = monarchString(labels.previous, R.string.monarch_aiinlinediff_previous),
        next = monarchString(labels.next, R.string.monarch_aiinlinediff_next),
        accept = monarchString(labels.accept, R.string.monarch_aiinlinediff_accept),
        reject = monarchString(labels.reject, R.string.monarch_aiinlinediff_reject),
        acceptChange = labels.acceptChange?.invoke(index, total)
            ?: stringResource(R.string.monarch_aiinlinediff_accept_change, index, total),
        rejectChange = labels.rejectChange?.invoke(index, total)
            ?: stringResource(R.string.monarch_aiinlinediff_reject_change, index, total),
    )

private fun announce(resources: Resources, labels: MonarchAiInlineDiffLabels, accepted: Boolean, left: Int): String =
    labels.announcement?.invoke(accepted, left) ?: when {
        accepted && left > 0 ->
            resources.getQuantityString(R.plurals.monarch_aiinlinediff_announce_accepted, left, left)
        accepted -> resources.getString(R.string.monarch_aiinlinediff_announce_accepted_last)
        left > 0 -> resources.getQuantityString(R.plurals.monarch_aiinlinediff_announce_rejected, left, left)
        else -> resources.getString(R.string.monarch_aiinlinediff_announce_rejected_last)
    }
