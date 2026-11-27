package com.rookhq.monarch.components.intelligence.aichainofsteps

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.collapsible.MonarchCollapsible
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStep
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStepState
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The steps an assistant is working through, as an ordered list that updates
 * live and folds under a summary such as "4 steps" once every step has
 * finished.
 *
 * Built by Monarch from a column of step rows: Material 3 has no step list.
 * The running step shows a Material 3 `CircularProgressIndicator`, or a still
 * filled mark when the user has removed animations. The other states show a
 * drawn mark (open for not started and skipped, filled for done and failed)
 * or the app's [stateIcon]; failed and skipped steps also say their state in
 * words. When folded the list sits in a `MonarchCollapsible` whose header is
 * the summary.
 *
 * Each step is one TalkBack element: the label, then the state and detail.
 * A step that starts or fails, and the end of the run, are announced once
 * through a polite live region; the state at first appearance is not.
 *
 * @param steps the steps in order, for example from a `MonarchAiPlanPart`. Update each state as the
 * assistant works.
 * @param modifier the modifier of the list.
 * @param details an optional detail under a step, such as a result or an error, by step id. Already localised.
 * @param collapsible whether the list folds under a summary once every step has finished.
 * @param initiallyExpanded whether the folded list starts open.
 * @param announces whether starts, failures and completion are announced to TalkBack. Turn it off when the
 * thread announces progress itself.
 * @param stateIcon an icon for every state but the running one, for example a check mark for done, as a
 * `MonarchIcon` of size `Sm` with a `null` content description. It is tinted by the state. Monarch bundles
 * no icons; without it a drawn mark is shown.
 * @param expandIcon the chevron of the folded summary, passed to `MonarchCollapsible`.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchAiChainOfSteps(
    steps: List<MonarchAiPlanStep>,
    modifier: Modifier = Modifier,
    details: Map<String, String> = emptyMap(),
    collapsible: Boolean = true,
    initiallyExpanded: Boolean = false,
    announces: Boolean = true,
    stateIcon: (@Composable (MonarchAiPlanStepState) -> Unit)? = MonarchDefaultIcons.planStep,
    expandIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    labels: MonarchAiChainOfStepsLabels = MonarchAiChainOfStepsLabels(),
) {
    val announcement = rememberStepAnnouncement(steps, announces, labels)
    val listLabel = monarchString(labels.label, R.string.monarch_aichainofsteps_label)
    Column(modifier = modifier) {
        if (MonarchAiChainOfStepsLogic.folded(steps, collapsible)) {
            var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
            MonarchCollapsible(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                label = summaryText(steps, labels),
                trailingIcon = expandIcon,
            ) {
                StepList(steps, details, listLabel, stateIcon, labels)
            }
        } else if (steps.isNotEmpty()) {
            StepList(steps, details, listLabel, stateIcon, labels)
        }
        // Always present, so each change is announced once as a change of this live region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun StepList(
    steps: List<MonarchAiPlanStep>,
    details: Map<String, String>,
    listLabel: String,
    stateIcon: (@Composable (MonarchAiPlanStepState) -> Unit)?,
    labels: MonarchAiChainOfStepsLabels,
) {
    Column(
        modifier = Modifier.semantics {
            contentDescription = listLabel
            isTraversalGroup = true
        },
        verticalArrangement = Arrangement.spacedBy(MonarchAiChainOfStepsTokens.StepGap),
    ) {
        steps.forEach { step -> StepRow(step, details[step.id], stateIcon, labels) }
    }
}

@Composable
private fun StepRow(
    step: MonarchAiPlanStep,
    detail: String?,
    stateIcon: (@Composable (MonarchAiPlanStepState) -> Unit)?,
    labels: MonarchAiChainOfStepsLabels,
) {
    val tokens = MonarchAiChainOfStepsTokens
    val colors = MonarchTheme.colors
    val state = step.state
    val stateName = labels.state?.invoke(state) ?: stringResource(tokens.stateName(state))
    val spoken = MonarchAiChainOfStepsLogic.spokenState(stateName, detail)
    Row(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = step.label
            stateDescription = spoken
        },
        horizontalArrangement = Arrangement.spacedBy(tokens.MarkGap),
    ) {
        Box(modifier = Modifier.size(tokens.MarkSize), contentAlignment = Alignment.Center) {
            StepMark(state, stateIcon)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
            Text(
                text = step.label,
                style = MonarchTheme.typography.body,
                color = tokens.labelColor(state, colors),
                textDecoration = if (state == MonarchAiPlanStepState.Skipped) TextDecoration.LineThrough else null,
            )
            if (tokens.showsStateText(state)) {
                Text(
                    text = stateName,
                    style = MonarchTheme.typography.caption,
                    color = tokens.stateColor(state, colors),
                )
            }
            if (!detail.isNullOrBlank()) {
                Text(text = detail, style = MonarchTheme.typography.caption, color = colors.textMuted)
            }
        }
    }
}

@Composable
private fun StepMark(state: MonarchAiPlanStepState, stateIcon: (@Composable (MonarchAiPlanStepState) -> Unit)?) {
    val tokens = MonarchAiChainOfStepsTokens
    val color = tokens.markColor(state, MonarchTheme.colors)
    val running = state == MonarchAiPlanStepState.Active
    when {
        running && tokens.runningMark(MonarchTheme.motion.reducedMotion) == AiChainRunningMark.Spinner ->
            CircularProgressIndicator(
            modifier = Modifier.size(tokens.DotSize),
            color = color,
            strokeWidth = tokens.StrokeWidth,
        )
        // Reduced motion: the web's active glyph, a ring with a centre dot, still and in the AI colour.
        running -> Box(
            modifier = Modifier
                .size(tokens.DotSize)
                .border(tokens.StrokeWidth, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(tokens.RunningDotSize).background(color, CircleShape))
        }
        stateIcon != null && !running -> CompositionLocalProvider(LocalContentColor provides color) {
            stateIcon(state)
        }
        tokens.filledMark(state) -> Box(Modifier.size(tokens.DotSize).background(color, CircleShape))
        else -> Box(Modifier.size(tokens.DotSize).border(tokens.StrokeWidth, color, CircleShape))
    }
}

@Composable
private fun summaryText(steps: List<MonarchAiPlanStep>, labels: MonarchAiChainOfStepsLabels): String {
    val failed = MonarchAiChainOfStepsLogic.failedCount(steps)
    val count = steps.size
    return labels.summary?.invoke(count, failed) ?: if (failed > 0) {
        pluralStringResource(R.plurals.monarch_aichainofsteps_summary_failed, count, count, failed)
    } else {
        pluralStringResource(R.plurals.monarch_aichainofsteps_summary, count, count)
    }
}

/** The latest announcement, updated when the steps change. Empty until something happens. */
@Composable
private fun rememberStepAnnouncement(
    steps: List<MonarchAiPlanStep>,
    announces: Boolean,
    labels: MonarchAiChainOfStepsLabels,
): String {
    val resources = LocalResources.current
    val currentLabels by rememberUpdatedState(labels)
    var previous by remember { mutableStateOf<Map<String, MonarchAiPlanStepState>?>(null) }
    var announcement by remember { mutableStateOf("") }
    LaunchedEffect(steps, announces) {
        val texts = announcementTexts(resources, currentLabels)
        val lines = MonarchAiChainOfStepsLogic.announcements(previous, steps, texts)
        previous = MonarchAiChainOfStepsLogic.snapshot(steps)
        if (announces && lines.isNotEmpty()) announcement = lines.joinToString(". ")
    }
    return announcement
}

private fun announcementTexts(resources: Resources, labels: MonarchAiChainOfStepsLabels) =
    MonarchAiChainOfStepsAnnouncementTexts(
        started = { label, position, total ->
            labels.started?.invoke(label, position, total)
                ?: resources.getString(R.string.monarch_aichainofsteps_started, position, total, label)
        },
        failedStep = { label ->
            labels.failedStep?.invoke(label) ?: resources.getString(R.string.monarch_aichainofsteps_failed_step, label)
        },
        finished = { count, failed ->
            labels.finished?.invoke(count, failed) ?: if (failed > 0) {
                resources.getQuantityString(R.plurals.monarch_aichainofsteps_finished_failed, count, count, failed)
            } else {
                resources.getQuantityString(R.plurals.monarch_aichainofsteps_finished, count, count)
            }
        },
    )
