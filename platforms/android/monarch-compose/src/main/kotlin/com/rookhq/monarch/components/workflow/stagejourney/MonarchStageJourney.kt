package com.rookhq.monarch.components.workflow.stagejourney

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Where an item is in its pipeline and what can happen next: a strip of
 * stages grouped by phase, a card for the current stage with how long it has
 * been there, and buttons for the moves the product allows. Pair it with
 * [MonarchStageHistory] for the record of past moves.
 *
 * Built by Monarch: the strip is a horizontally scrolling `Row` that starts
 * scrolled to the current stage, the card a `Column` with `MonarchBadge` and
 * `MonarchButton`s in a `FlowRow`. [onTransition] suspends: the chosen
 * move's button shows a spinner, the others are disabled, and a thrown error
 * is shown in the card and announced. The app then changes [currentId]; the
 * change is announced ("Moved to Granted").
 *
 * @param stages every stage in pipeline order.
 * @param currentId the id of the current stage. Stages before it are done. With an unknown id every stage is
 * upcoming and no card is shown.
 * @param modifier the modifier of the journey.
 * @param terminal the current stage ends the journey: success or closed.
 * @param enteredAt when the item entered the current stage, in epoch milliseconds: "since 3 days ago".
 * @param currentMeta a short note shown as an info `MonarchBadge` next to the current stage.
 * @param variant the strip, card and moves, or the strip only.
 * @param transitions the moves from the current stage: forward (the first is primary), exit (quiet) and back.
 * @param now a fixed reference time for [enteredAt], which also stops the updates once a minute.
 * @param labels replacements for the built-in strings.
 * @param onTransition called with the chosen move. Without it no moves are shown.
 * @param content extra content at the end of the card, such as a checklist.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchStageJourney(
    stages: List<MonarchStageJourneyStage>,
    currentId: String,
    modifier: Modifier = Modifier,
    terminal: MonarchStageJourneyTerminal? = null,
    enteredAt: Long? = null,
    currentMeta: String? = null,
    variant: MonarchStageJourneyVariant = MonarchStageJourneyVariant.Full,
    transitions: List<MonarchStageJourneyTransition> = emptyList(),
    now: Long? = null,
    labels: MonarchStageJourneyLabels = MonarchStageJourneyLabels(),
    onTransition: (suspend (MonarchStageJourneyTransition) -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val currentIndex = stages.indexOfFirst { it.id == currentId }
    val current = stages.getOrNull(currentIndex)
    val scope = rememberCoroutineScope()
    val mover = remember(scope) { StageJourneyMover(scope) }
    mover.failed = monarchString(labels.failed, R.string.monarch_stagejourney_failed)
    val moved = current?.label?.let {
        labels.moved?.invoke(it) ?: stringResource(R.string.monarch_stagejourney_moved, it)
    }
    val announcement = rememberStageJourneyAnnouncement(currentId, moved, mover)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchStageJourneyTokens.Gap),
    ) {
        StageStrip(stages, currentIndex, terminal, labels)
        if (variant == MonarchStageJourneyVariant.Full && current != null) {
            StageCard(
                stage = current,
                terminal = terminal,
                enteredAt = enteredAt,
                currentMeta = currentMeta,
                transitions = transitions.takeIf { onTransition != null }.orEmpty(),
                now = rememberMonarchRelativeClock(listOf(enteredAt), fixed = now),
                labels = labels,
                mover = mover,
                onTransition = onTransition,
                content = content,
            )
        }
        // Always present, so that a change of stage is announced.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** Runs one move at a time and keeps its busy state and failure. */
@Stable
internal class StageJourneyMover(private val scope: CoroutineScope) {
    var busyId: String? by mutableStateOf(null)
        private set
    var error: String? by mutableStateOf(null)
    var failed: String = ""

    /** Runs [move] with [transition]; a thrown error becomes [error]. */
    fun run(transition: MonarchStageJourneyTransition, move: suspend (MonarchStageJourneyTransition) -> Unit) {
        if (busyId != null) return
        busyId = transition.id
        error = null
        scope.launch {
            val thrown = runCatching { move(transition) }.exceptionOrNull()
            busyId = null
            if (thrown is CancellationException) throw thrown
            if (thrown != null) error = thrown.message?.takeIf { it.isNotBlank() } ?: failed
        }
    }
}

/** "Moved to …" after the current stage changes; clears a failure. Empty before the first change. */
@Composable
private fun rememberStageJourneyAnnouncement(currentId: String, moved: String?, mover: StageJourneyMover): String {
    var previous by remember { mutableStateOf(currentId) }
    var announcement by remember { mutableStateOf("") }
    LaunchedEffect(currentId) {
        if (previous != currentId) {
            previous = currentId
            mover.error = null
            announcement = moved.orEmpty()
        }
    }
    return announcement
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StageStrip(
    stages: List<MonarchStageJourneyStage>,
    currentIndex: Int,
    terminal: MonarchStageJourneyTerminal?,
    labels: MonarchStageJourneyLabels,
) {
    val tokens = MonarchStageJourneyTokens
    val name = monarchString(labels.stages, R.string.monarch_stagejourney_stages)
    val states = StageJourneyStateText(
        done = monarchString(labels.done, R.string.monarch_stagejourney_done),
        current = monarchString(labels.current, R.string.monarch_stagejourney_current),
        success = monarchString(labels.success, R.string.monarch_stagejourney_success),
        closed = monarchString(labels.closed, R.string.monarch_stagejourney_closed),
    )
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(currentIndex) { if (currentIndex >= 0) requester.bringIntoView() }
    // The groups wrap onto the next line on a narrow screen, as the web's do, so every phase shows.
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = name
                isTraversalGroup = true
            },
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        tokens.groups(stages).forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(tokens.SmallGap)) {
                group.name?.let {
                    Text(
                        text = it,
                        style = MonarchTheme.typography.caption,
                        color = MonarchTheme.colors.textMuted,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.StageGap)) {
                    group.stages.forEach { (index, stage) ->
                        val status = tokens.status(index, currentIndex)
                        StagePill(
                            stage = stage,
                            status = status,
                            terminal = terminal,
                            states = states,
                            modifier = if (status == StageJourneyStatus.Current) {
                                Modifier.bringIntoViewRequester(requester)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        }
    }
}

/** The words of the stage states. */
private class StageJourneyStateText(val done: String, val current: String, val success: String, val closed: String)

@Composable
private fun StagePill(
    stage: MonarchStageJourneyStage,
    status: StageJourneyStatus,
    terminal: MonarchStageJourneyTerminal?,
    states: StageJourneyStateText,
    modifier: Modifier,
) {
    val tokens = MonarchStageJourneyTokens
    val colors = tokens.stageColors(MonarchTheme.colors, status, terminal)
    val shape = MonarchTheme.shapes.full
    val state = when (status) {
        StageJourneyStatus.Done -> states.done
        StageJourneyStatus.Upcoming -> null
        StageJourneyStatus.Current -> when (terminal) {
            MonarchStageJourneyTerminal.Success -> states.success
            MonarchStageJourneyTerminal.Closed -> states.closed
            null -> states.current
        }
    }
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.container, shape)
            .border(MonarchShapeDefaults.BorderWidth, colors.border, shape)
            .padding(horizontal = tokens.StageHorizontalPadding, vertical = tokens.StageVerticalPadding)
            .clearAndSetSemantics {
                contentDescription = stage.label
                state?.let { stateDescription = it }
                selected = status == StageJourneyStatus.Current
            },
        horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status == StageJourneyStatus.Done || (status == StageJourneyStatus.Current && terminal != null)) {
            StageJourneyCheck(colors.content)
        }
        Text(
            text = stage.label,
            style = MonarchTheme.typography.label,
            fontWeight = FontWeight(tokens.weight(status)),
            color = colors.content,
            maxLines = 1,
        )
    }
}

/** The check mark of a done stage, drawn as the web's SVG. Decorative. */
@Composable
private fun StageJourneyCheck(color: Color) {
    val tokens = MonarchStageJourneyTokens
    Canvas(modifier = Modifier.size(tokens.CheckSize)) {
        val unit = size.minDimension / tokens.CHECK_GRID
        val points = tokens.CheckPoints.map { it * unit }
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val stroke = Stroke(tokens.CHECK_STROKE * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(path, color, style = stroke)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
private fun StageCard(
    stage: MonarchStageJourneyStage,
    terminal: MonarchStageJourneyTerminal?,
    enteredAt: Long?,
    currentMeta: String?,
    transitions: List<MonarchStageJourneyTransition>,
    now: Long,
    labels: MonarchStageJourneyLabels,
    mover: StageJourneyMover,
    onTransition: (suspend (MonarchStageJourneyTransition) -> Unit)?,
    content: (@Composable ColumnScope.() -> Unit)?,
) {
    val tokens = MonarchStageJourneyTokens
    val (fill, border) = tokens.cardColors(MonarchTheme.colors, terminal)
    val shape = MonarchTheme.shapes.lg
    val title = labels.now?.invoke(stage.label) ?: stringResource(R.string.monarch_stagejourney_now, stage.label)
    val outcome = when (terminal) {
        MonarchStageJourneyTerminal.Success -> monarchString(labels.success, R.string.monarch_stagejourney_success)
        MonarchStageJourneyTerminal.Closed -> monarchString(labels.closed, R.string.monarch_stagejourney_closed)
        null -> null
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill, shape)
            .border(MonarchShapeDefaults.BorderWidth, border, shape)
            .padding(tokens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.StageGap),
            verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics {
                    heading()
                    if (outcome != null) contentDescription = "$title, $outcome"
                },
                style = MonarchTheme.typography.subheading,
                color = MonarchTheme.colors.textDefault,
            )
            currentMeta?.let { MonarchBadge(text = it, tone = MonarchTone.Info) }
            enteredAt?.let {
                val relative = MonarchRelativeTime.format(it, now, LocalConfiguration.current.locales[0])
                Text(
                    text = labels.since?.invoke(relative)
                        ?: stringResource(R.string.monarch_stagejourney_since, relative),
                    style = MonarchTheme.typography.caption,
                    color = MonarchTheme.colors.textMuted,
                )
            }
        }
        stage.description?.let {
            Text(text = it, style = MonarchTheme.typography.body, color = MonarchTheme.colors.textMuted)
        }
        if (onTransition != null && transitions.isNotEmpty()) StageMoves(transitions, labels, mover, onTransition)
        mover.error?.let {
            Text(
                text = it,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MonarchTheme.typography.caption,
                color = MonarchTheme.colors.dangerStrong,
            )
        }
        content?.invoke(this)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StageMoves(
    transitions: List<MonarchStageJourneyTransition>,
    labels: MonarchStageJourneyLabels,
    mover: StageJourneyMover,
    onTransition: suspend (MonarchStageJourneyTransition) -> Unit,
) {
    val tokens = MonarchStageJourneyTokens
    val moves = tokens.moves(transitions)
    val needsInput = monarchString(labels.needsInput, R.string.monarch_stagejourney_needs_input)
    val moveTo: @Composable (String) -> String = {
        labels.moveTo?.invoke(it) ?: stringResource(R.string.monarch_stagejourney_move_to, it)
    }
    val button: @Composable (
        MonarchStageJourneyTransition,
        String,
        String,
        MonarchButtonVariant,
        (@Composable () -> Unit)?,
    ) -> Unit =
        { move, visible, spoken, variant, icon ->
            MonarchButton(
                onClick = { mover.run(move, onTransition) },
                modifier = Modifier.semantics {
                    contentDescription = if (move.needsInput) "$spoken, $needsInput" else spoken
                },
                variant = variant,
                size = MonarchButtonSize.Sm,
                enabled = move.enabled && (mover.busyId == null || mover.busyId == move.id),
                loading = mover.busyId == move.id,
            ) {
                icon?.invoke()
                Text(visible)
            }
        }
    Column(verticalArrangement = Arrangement.spacedBy(tokens.SmallGap)) {
        Text(
            text = monarchString(labels.nextSteps, R.string.monarch_stagejourney_next_steps),
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.label,
            color = MonarchTheme.colors.textMuted,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.StageGap),
            verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
        ) {
            moves.forward.forEachIndexed { index, move ->
                val variant = if (index == 0) MonarchButtonVariant.Primary else MonarchButtonVariant.Secondary
                button(move, move.label, moveTo(move.label), variant, null)
            }
            moves.exits.forEach { button(it, it.label, moveTo(it.label), MonarchButtonVariant.Ghost, null) }
            moves.back?.let {
                val back = labels.backTo?.invoke(it.label)
                    ?: stringResource(R.string.monarch_stagejourney_back_to, it.label)
                button(it, back, back, MonarchButtonVariant.Ghost, MonarchDefaultIcons.Undo)
            }
        }
    }
}
