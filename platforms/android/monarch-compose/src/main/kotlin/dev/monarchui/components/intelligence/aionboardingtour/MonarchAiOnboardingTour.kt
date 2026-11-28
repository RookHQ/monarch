package dev.monarchui.components.intelligence.aionboardingtour

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.intelligence.aionboardingtour.MonarchAiOnboardingTourTokens as Tokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A short, step-by-step introduction to AI in a product (what it can see,
 * what it can do, how to turn it off) with Back, Next and Turn off AI on
 * every step.
 *
 * Built by Monarch from a `HorizontalPager` of steps, pips and
 * `MonarchButton`s. Present it in a `MonarchBottomSheet` (or at the
 * top of a screen); it does not dismiss itself. Steps can be swiped as well
 * as moved with Back and Next. Turn off AI is on every step, so the opt-out
 * is always reachable. Each step's title is a heading; when the step
 * changes, "Step 2 of 4" and its title are announced through a polite live
 * region.
 *
 * This overload is controlled: the app keeps [step]. Use the overload without
 * `step` to let the tour keep it.
 *
 * @param steps the steps. Cover what the AI sees, what it can do and how to turn it off.
 * @param step the position of the current step, from 0. Kept inside the steps.
 * @param onStepChange called with the new position by Back, Next and a swipe.
 * @param onDisable called by Turn off AI. Turn AI off (or open its settings) and hide the tour.
 * @param onFinish called by the last step's Got it button.
 * @param modifier the modifier of the tour.
 * @param icon an AI glyph beside the eyebrow, as a `MonarchIcon` with a `null` content description. Drawn by default,
 * as on the web; `null` draws none.
 * @param skipIcon the icon of the skip button, for example a close mark. Without it the button shows the
 * words "Skip introduction".
 * @param labels replacements for the built-in strings.
 * @param onSkip adds a button that skips the tour.
 */
@Composable
public fun MonarchAiOnboardingTour(
    steps: List<MonarchAiOnboardingTourStep>,
    step: Int,
    onStepChange: (Int) -> Unit,
    onDisable: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.Bot,
    skipIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchAiOnboardingTourLabels = MonarchAiOnboardingTourLabels(),
    onSkip: (() -> Unit)? = null,
) {
    val current = Tokens.clamp(step, steps.size)
    val pager = rememberPagerState(initialPage = current) { steps.size }
    SyncPager(pager, current, onStepChange)
    val stepOf: (Int, Int) -> String = stepOfText(labels)
    Column(
        modifier = modifier.padding(Tokens.Padding),
        verticalArrangement = Arrangement.spacedBy(Tokens.SectionGap),
    ) {
        TopBar(icon, skipIcon, labels, onSkip, stepOf(current + 1, steps.size).takeIf { steps.isNotEmpty() })
        if (steps.isNotEmpty()) {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxWidth(),
                beyondViewportPageCount = steps.size,
                verticalAlignment = Alignment.Top,
                key = { index -> steps[index].id ?: index },
            ) { page -> StepPage(steps[page]) }
        }
        if (Tokens.showsStepper(steps.size)) Pips(current, steps.size)
        Buttons(current, steps.size, labels, onStepChange, onDisable, onFinish)
        Announcement(steps, current, stepOf)
    }
}

/**
 * A short, step-by-step introduction to AI in a product, which keeps its own
 * current step. See the controlled overload for the behavior.
 *
 * @param steps the steps. Cover what the AI sees, what it can do and how to turn it off.
 * @param onDisable called by Turn off AI. Turn AI off (or open its settings) and hide the tour.
 * @param onFinish called by the last step's Got it button.
 * @param modifier the modifier of the tour.
 * @param icon an AI glyph beside the eyebrow. Drawn by default, as on the web; `null` draws none.
 * @param skipIcon the icon of the skip button. Without it the button shows the words "Skip introduction".
 * @param labels replacements for the built-in strings.
 * @param onSkip adds a button that skips the tour.
 */
@Composable
public fun MonarchAiOnboardingTour(
    steps: List<MonarchAiOnboardingTourStep>,
    onDisable: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.Bot,
    skipIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchAiOnboardingTourLabels = MonarchAiOnboardingTourLabels(),
    onSkip: (() -> Unit)? = null,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    MonarchAiOnboardingTour(
        steps = steps,
        step = step,
        onStepChange = { step = it },
        onDisable = onDisable,
        onFinish = onFinish,
        modifier = modifier,
        icon = icon,
        skipIcon = skipIcon,
        labels = labels,
        onSkip = onSkip,
    )
}

/** Moves the pager to [current] and reports swipes, once a swipe has settled, through [onStepChange]. */
@Composable
private fun SyncPager(pager: PagerState, current: Int, onStepChange: (Int) -> Unit) {
    val motion = MonarchTheme.motion
    val latestStep by rememberUpdatedState(current)
    val latestChange by rememberUpdatedState(onStepChange)
    LaunchedEffect(current, pager.pageCount) {
        if (pager.pageCount > 0 && pager.currentPage != current) {
            pager.animateScrollToPage(current, animationSpec = motion.spec())
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { page ->
            if (page != latestStep) latestChange(page)
        }
    }
}

@Composable
private fun stepOfText(labels: MonarchAiOnboardingTourLabels): (Int, Int) -> String {
    val resources = LocalResources.current
    return { step, total ->
        labels.stepOf?.invoke(step, total)
            ?: resources.getString(R.string.monarch_aionboardingtour_step_of, step, total)
    }
}

@Composable
private fun TopBar(
    icon: (@Composable () -> Unit)?,
    skipIcon: (@Composable () -> Unit)?,
    labels: MonarchAiOnboardingTourLabels,
    onSkip: (() -> Unit)?,
    step: String?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Tokens.EyebrowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The web shows the eyebrow as an AI badge with the assistant mark, then the step.
            MonarchBadge(
                text = monarchString(labels.eyebrow, R.string.monarch_aionboardingtour_eyebrow),
                tone = MonarchTone.Ai,
                leadingIcon = icon,
            )
            if (step != null) {
                Text(
                    text = step,
                    style = MonarchTheme.typography.caption,
                    color = Tokens.stepColor(MonarchTheme.colors),
                )
            }
        }
        if (onSkip != null) {
            val skip = monarchString(labels.skip, R.string.monarch_aionboardingtour_skip)
            if (skipIcon != null) {
                MonarchIconButton(label = skip, onClick = onSkip, icon = skipIcon)
            } else {
                MonarchButton(text = skip, onClick = onSkip, variant = MonarchButtonVariant.Ghost)
            }
        }
    }
}

/** One pip per step, the current one drawn as a longer AI-coloured pill; decorative, the step text says the rest. */
@Composable
private fun Pips(current: Int, count: Int) {
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(Tokens.PipGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == current
            Box(
                Modifier
                    .size(width = Tokens.pipWidth(active), height = Tokens.PipSize)
                    .background(Tokens.pipColor(active, colors), MonarchShapeDefaults.Mark),
            )
        }
    }
}

@Composable
private fun StepPage(step: MonarchAiOnboardingTourStep) {
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.TextGap)) {
        Text(
            text = step.title,
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.heading,
            color = MonarchTheme.colors.textDefault,
        )
        Text(text = step.body, style = MonarchTheme.typography.body, color = MonarchTheme.colors.textMuted)
    }
}

@Suppress("LongParameterList")
@Composable
private fun Buttons(
    current: Int,
    count: Int,
    labels: MonarchAiOnboardingTourLabels,
    onStepChange: (Int) -> Unit,
    onDisable: () -> Unit,
    onFinish: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Tokens.ActionGap),
        verticalArrangement = Arrangement.spacedBy(Tokens.ActionGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchButton(
            text = monarchString(labels.disable, R.string.monarch_aionboardingtour_disable),
            onClick = onDisable,
            variant = MonarchButtonVariant.Ghost,
        )
        Spacer(Modifier.weight(1f))
        if (Tokens.hasBack(current)) {
            MonarchButton(
                text = monarchString(labels.back, R.string.monarch_aionboardingtour_back),
                onClick = { onStepChange(current - 1) },
            )
        }
        if (Tokens.isLast(current, count)) {
            MonarchButton(
                text = monarchString(labels.finish, R.string.monarch_aionboardingtour_finish),
                onClick = onFinish,
                variant = MonarchButtonVariant.Primary,
            )
        } else {
            MonarchButton(
                text = monarchString(labels.next, R.string.monarch_aionboardingtour_next),
                onClick = { onStepChange(current + 1) },
                variant = MonarchButtonVariant.Primary,
            )
        }
    }
}

/** Announces the step and its title after the step changes; nothing is announced when the tour appears. */
@Composable
private fun Announcement(steps: List<MonarchAiOnboardingTourStep>, current: Int, stepOf: (Int, Int) -> String) {
    var first by remember { mutableStateOf(true) }
    var text by remember { mutableStateOf("") }
    LaunchedEffect(current) {
        if (first) {
            first = false
        } else {
            text = steps.getOrNull(current)?.let { "${stepOf(current + 1, steps.size)}: ${it.title}" }.orEmpty()
        }
    }
    MonarchLiveStatus(text = text.takeIf { it.isNotEmpty() }, visible = false)
}
