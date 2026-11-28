package dev.monarchui.components.workflow.onboardingchecklist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.datadisplay.collapsible.MonarchCollapsible
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * A first-run checklist: steps with a done state and an optional call to
 * action, a progress summary, and collapse and dismiss controls.
 *
 * Built from `MonarchCard`, `MonarchProgress` (the Material 3
 * `LinearProgressIndicator`) and `MonarchCollapsible`, whose header is the
 * progress text: "2 of 5 done". The app defines the steps, decides when each
 * is done and keeps the dismissed state; the checklist does not hide itself.
 *
 * Monarch bundles no icons, so the chevron, the dismiss icon and the check
 * mark are slots. Without [dismissIcon] the dismiss button shows its label;
 * without [doneIcon] a done step shows a filled success circle in place of
 * its numbered ring. Either way TalkBack reads each step as "Done" or "Not done".
 *
 * @param title the heading, such as "Get started". Read as a heading.
 * @param steps the steps in order. Ids must be unique.
 * @param modifier the modifier of the card.
 * @param description text under the heading.
 * @param completeMessage replaces "All done. You are set up.", shown when every step is done.
 * @param collapsible makes the progress row a button that shows and hides the steps.
 * @param expanded whether the steps are shown. `null` lets the checklist keep this itself (saved across
 * configuration changes), starting expanded. Hoist it to keep the state across launches.
 * @param onExpandedChange called with the new state when the user uses the progress row.
 * @param onDismiss shows a dismiss button that calls this. The app hides the checklist and remembers it.
 * @param expandIcon an icon after the progress text, for example a chevron that points down. It turns half a
 * turn while expanded.
 * @param dismissIcon an icon for the dismiss button, for example a close mark. The button then shows only the
 * icon and keeps its spoken name.
 * @param doneIcon a check mark drawn in the marker of a done step and before the complete message.
 * @param labels replacements for the built-in strings.
 * @param action the call to action of a step, such as a small `MonarchButton`. Shown only while the step is
 * not done.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchOnboardingChecklist(
    title: String,
    steps: List<MonarchOnboardingStep>,
    modifier: Modifier = Modifier,
    description: String? = null,
    completeMessage: String? = null,
    collapsible: Boolean = true,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    expandIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    dismissIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    doneIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Check,
    labels: MonarchOnboardingChecklistLabels = MonarchOnboardingChecklistLabels(),
    action: (@Composable (MonarchOnboardingStep) -> Unit)? = null,
) {
    val colors = MonarchTheme.colors
    var ownExpanded by rememberSaveable { mutableStateOf(true) }
    val shown = expanded ?: ownExpanded
    val done = steps.count { it.done }
    val progressText = labels.progress?.invoke(done, steps.size)
        ?: pluralStringResource(R.plurals.monarch_onboardingchecklist_progress, steps.size, done, steps.size)
    val body: @Composable () -> Unit = {
        ChecklistBody(steps, completeMessage, doneIcon, labels, action)
    }
    MonarchCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchOnboardingChecklistTokens.SectionGap)) {
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchOnboardingChecklistTokens.SectionGap)) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MonarchOnboardingChecklistTokens.HeadingGap),
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.semantics { heading() },
                        style = MonarchTheme.typography.bodyStrong,
                        color = colors.textDefault,
                    )
                    if (description != null) {
                        Text(text = description, style = MonarchTheme.typography.body, color = colors.textMuted)
                    }
                }
                if (onDismiss != null) {
                    DismissButton(
                        label = monarchString(labels.dismiss, R.string.monarch_onboardingchecklist_dismiss),
                        icon = dismissIcon,
                        onDismiss = onDismiss,
                    )
                }
            }
            MonarchProgress(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { stateDescription = progressText },
                progress = MonarchOnboardingChecklistTokens.fraction(done, steps.size),
                label = monarchString(labels.progressLabel, R.string.monarch_onboardingchecklist_progress_label),
            )
            if (collapsible) {
                MonarchCollapsible(
                    expanded = shown,
                    onExpandedChange = { next ->
                        if (expanded == null) ownExpanded = next
                        onExpandedChange?.invoke(next)
                    },
                    label = progressText,
                    trailingIcon = expandIcon,
                    expandedLabel = labels.expanded,
                    collapsedLabel = labels.collapsed,
                ) { body() }
            } else {
                // The progress bar already reads this text; here it is for the eye only.
                Text(
                    text = progressText,
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MonarchTheme.typography.label,
                    color = colors.textMuted,
                )
                body()
            }
        }
    }
}

@Composable
private fun DismissButton(label: String, icon: (@Composable () -> Unit)?, onDismiss: () -> Unit) {
    if (icon == null) {
        MonarchButton(
            text = label,
            onClick = onDismiss,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
        )
    } else {
        MonarchIconButton(label = label, onClick = onDismiss) {
            Box(Modifier.clearAndSetSemantics { }) { icon() }
        }
    }
}

@Composable
private fun ChecklistBody(
    steps: List<MonarchOnboardingStep>,
    completeMessage: String?,
    doneIcon: (@Composable () -> Unit)?,
    labels: MonarchOnboardingChecklistLabels,
    action: (@Composable (MonarchOnboardingStep) -> Unit)?,
) {
    val current = MonarchOnboardingChecklistTokens.current(steps)
    Column(modifier = Modifier.fillMaxWidth()) {
        if (MonarchOnboardingChecklistTokens.complete(steps)) {
            CompleteMessage(
                text = monarchString(completeMessage ?: labels.complete, R.string.monarch_onboardingchecklist_complete),
                icon = doneIcon,
            )
        }
        val doneText = monarchString(labels.done, R.string.monarch_onboardingchecklist_done)
        val notDoneText = monarchString(labels.notDone, R.string.monarch_onboardingchecklist_not_done)
        val currentText = stringResource(
            R.string.monarch_onboardingchecklist_current_state,
            notDoneText,
            monarchString(labels.current, R.string.monarch_onboardingchecklist_current),
        )
        steps.forEachIndexed { index, step ->
            key(step.id) {
                val state = when {
                    step.done -> doneText
                    index == current -> currentText
                    else -> notDoneText
                }
                StepRow(step, number = index + 1, current = index == current, state = state, doneIcon, action)
            }
        }
    }
}

@Composable
private fun CompleteMessage(text: String, icon: (@Composable () -> Unit)?) {
    val surface = MonarchOnboardingChecklistTokens.completeColors(MonarchTheme.colors)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MonarchOnboardingChecklistTokens.CompleteGap)
            .monarchSurface(surface, MonarchShapeDefaults.Control)
            .padding(
                horizontal = MonarchOnboardingChecklistTokens.CompletePaddingHorizontal,
                vertical = MonarchOnboardingChecklistTokens.CompletePaddingVertical,
            ),
        horizontalArrangement = Arrangement.spacedBy(MonarchOnboardingChecklistTokens.CompleteGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides surface.content) {
                Box(Modifier.clearAndSetSemantics { }) { icon() }
            }
        }
        Text(text = text, style = MonarchTheme.typography.label, color = surface.content)
    }
}

@Composable
@Suppress("LongParameterList")
private fun StepRow(
    step: MonarchOnboardingStep,
    number: Int,
    current: Boolean,
    state: String,
    doneIcon: (@Composable () -> Unit)?,
    action: (@Composable (MonarchOnboardingStep) -> Unit)?,
) {
    val colors = MonarchTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MonarchShapeDefaults.Control)
            .background(MonarchOnboardingChecklistTokens.stepBackground(current, colors))
            .padding(
                horizontal = MonarchOnboardingChecklistTokens.StepPaddingHorizontal,
                vertical = MonarchOnboardingChecklistTokens.StepPaddingVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(MonarchOnboardingChecklistTokens.StepActionGap),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { stateDescription = state },
            horizontalArrangement = Arrangement.spacedBy(MonarchOnboardingChecklistTokens.StepGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepMarker(number = number, done = step.done, current = current, doneIcon = doneIcon)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.title,
                    style = MonarchTheme.typography.label,
                    color = MonarchOnboardingChecklistTokens.stepTitle(step.done, colors),
                )
                if (!step.done && step.description != null) {
                    Text(text = step.description, style = MonarchTheme.typography.caption, color = colors.textMuted)
                }
            }
            // As the web: the step's action sits at the end of its row.
            if (!step.done && action != null) action(step)
        }
    }
}

/** The numbered ring, or the filled circle of a done step. A circle that grows with the font size. */
@Composable
private fun StepMarker(number: Int, done: Boolean, current: Boolean, doneIcon: (@Composable () -> Unit)?) {
    val surface = MonarchOnboardingChecklistTokens.marker(done, current, MonarchTheme.colors)
    val shape = MonarchShapeDefaults.Mark
    val ring = surface.border
    Box(
        modifier = Modifier
            .clearAndSetSemantics { }
            .monarchSurface(surface.copy(border = null), shape)
            .then(
                if (ring != null) {
                    Modifier.border(MonarchOnboardingChecklistTokens.MarkerBorder, ring, shape)
                } else {
                    Modifier
                },
            )
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                val side = maxOf(
                    placeable.width,
                    placeable.height,
                    MonarchOnboardingChecklistTokens.MarkerSize.roundToPx(),
                )
                layout(side, side) { placeable.place((side - placeable.width) / 2, (side - placeable.height) / 2) }
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides surface.content) {
            when {
                done -> doneIcon?.invoke()
                else -> Text(
                    text = number.toString(),
                    style = MonarchTheme.typography.caption,
                    color = surface.content,
                )
            }
        }
    }
}
