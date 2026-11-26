package com.rookhq.monarch.components.workflow.stepper

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.internal.MonarchProgressRing
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Multi-step progress for wizards, onboarding and setup flows: a row or a column of numbered steps, or a compact
 * "Step 2 of 5" with a progress bar when there is no room.
 *
 * Built by Monarch from `Row` and `Column` of drawn step markers: Material 3 has no stepper. Each step is one
 * TalkBack element that reads its position, its name and its state, and the stepper is a collection named
 * "Progress". Steps before [current] are finished. With [onSelect], steps the user has reached (finished ones and
 * any up to [furthest]) become buttons, except the current and disabled ones. The compact form uses
 * `MonarchProgress`.
 *
 * @param steps the steps, in order. No steps draws nothing.
 * @param current the position of the current step, from 0. A position outside the steps is moved inside.
 * @param modifier the modifier of the stepper.
 * @param furthest the position of the furthest step reached, when the user has gone back. Steps up to it stay
 * selectable. `null`, or a position before [current], uses [current].
 * @param orientation a row or a column of steps.
 * @param compact when to show the compact summary instead of every step.
 * @param onSelect makes reached steps buttons; receives the step and its position. Without it the stepper only
 * shows progress.
 * @param labels replaces any of the built-in strings.
 * @param finished every step is done: all markers show the check mark (a step with an error keeps it), no step is
 * current, and the compact summary reads the last step as completed with a full bar.
 * @param busy the current step is working, for example while it saves: its marker shows a progress ring in place
 * of its number (still when animations are off) and TalkBack adds "in progress". Ignored when [finished].
 */
@Composable
public fun MonarchStepper(
    steps: List<MonarchStepperStep>,
    current: Int,
    modifier: Modifier = Modifier,
    furthest: Int? = null,
    orientation: MonarchStepperOrientation = MonarchStepperOrientation.Horizontal,
    compact: MonarchStepperCompactMode = MonarchStepperCompactMode.Automatic,
    onSelect: ((step: MonarchStepperStep, index: Int) -> Unit)? = null,
    labels: MonarchStepperLabels = MonarchStepperLabels(),
    finished: Boolean = false,
    busy: Boolean = false,
) {
    if (steps.isEmpty()) return
    // A finished stepper has its current position one past the last step, so every step reads as done.
    val currentIndex = if (finished) steps.size else MonarchStepperTokens.clamp(current, steps.size)
    val reached = furthest?.let { MonarchStepperTokens.clamp(it, steps.size) } ?: currentIndex
    val furthestIndex = maxOf(currentIndex, reached)
    StepperBody(
        steps, currentIndex, furthestIndex, modifier, orientation, compact, onSelect, labels, busy && !finished,
    )
}

/**
 * Multi-step progress for wizards, onboarding and setup flows, with the current and furthest steps given by
 * their identifiers. See the overload that takes positions.
 *
 * @param steps the steps, in order. No steps draws nothing.
 * @param currentId the identifier of the current step. An unknown identifier selects the first step.
 * @param modifier the modifier of the stepper.
 * @param furthestId the identifier of the furthest step reached. `null` or an unknown identifier uses the current
 * step.
 * @param orientation a row or a column of steps.
 * @param compact when to show the compact summary instead of every step.
 * @param onSelect makes reached steps buttons; receives the step and its position.
 * @param labels replaces any of the built-in strings.
 * @param finished every step is done. See the overload that takes positions.
 * @param busy the current step is working. See the overload that takes positions.
 */
@Composable
public fun MonarchStepper(
    steps: List<MonarchStepperStep>,
    currentId: String,
    modifier: Modifier = Modifier,
    furthestId: String? = null,
    orientation: MonarchStepperOrientation = MonarchStepperOrientation.Horizontal,
    compact: MonarchStepperCompactMode = MonarchStepperCompactMode.Automatic,
    onSelect: ((step: MonarchStepperStep, index: Int) -> Unit)? = null,
    labels: MonarchStepperLabels = MonarchStepperLabels(),
    finished: Boolean = false,
    busy: Boolean = false,
) {
    val currentIndex = MonarchStepperTokens.indexOf(steps, currentId, 0)
    MonarchStepper(
        steps = steps,
        current = currentIndex,
        modifier = modifier,
        furthest = MonarchStepperTokens.indexOf(steps, furthestId, currentIndex),
        orientation = orientation,
        compact = compact,
        onSelect = onSelect,
        labels = labels,
        finished = finished,
        busy = busy,
    )
}

/** The built-in strings, resolved once per stepper. */
private class StepperStrings(
    val progress: String,
    val stepOf: (Int, Int) -> String,
    val complete: String,
    val error: String,
    val current: String,
    val optional: String,
    val busy: String,
)

@Composable
private fun rememberStepperStrings(labels: MonarchStepperLabels): StepperStrings {
    val resources = LocalResources.current
    return StepperStrings(
        progress = monarchString(labels.progress, R.string.monarch_stepper_progress),
        stepOf = labels.stepOf ?: { step, total -> resources.getString(R.string.monarch_stepper_step_of, step, total) },
        complete = monarchString(labels.complete, R.string.monarch_stepper_complete),
        error = monarchString(labels.error, R.string.monarch_stepper_error),
        current = monarchString(labels.current, R.string.monarch_stepper_current),
        optional = monarchString(labels.optional, R.string.monarch_stepper_optional),
        busy = monarchString(labels.busy, R.string.monarch_stepper_busy),
    )
}

@Composable
private fun StepperBody(
    steps: List<MonarchStepperStep>,
    current: Int,
    furthest: Int,
    modifier: Modifier,
    orientation: MonarchStepperOrientation,
    compact: MonarchStepperCompactMode,
    onSelect: ((MonarchStepperStep, Int) -> Unit)?,
    labels: MonarchStepperLabels,
    busy: Boolean,
) {
    val strings = rememberStepperStrings(labels)
    val markerSize = MonarchStepperTokens.markerSize(LocalDensity.current.fontScale)
    val measures = compact != MonarchStepperCompactMode.Always && orientation == MonarchStepperOrientation.Horizontal
    val idealWidth = if (measures) rememberIdealRowWidth(steps, current, markerSize, strings.optional) else null
    BoxWithConstraints(modifier = modifier) {
        val fits = !constraints.hasBoundedWidth || idealWidth == null || idealWidth <= maxWidth
        if (MonarchStepperTokens.showsCompact(compact, orientation, fits)) {
            CompactSummary(steps, current, strings, busy)
        } else {
            val step: @Composable (Int, Modifier) -> Unit = { index, itemModifier ->
                StepItem(steps, index, current, furthest, markerSize, onSelect, strings, busy, itemModifier)
            }
            if (monarchStepperIsRow(compact, orientation, fits)) {
                val bounded = constraints.hasBoundedWidth
                HorizontalSteps(steps, current, markerSize, strings.progress, maxWidth, bounded, step)
            } else {
                VerticalSteps(steps, current, markerSize, strings.progress, step)
            }
        }
    }
}

/** The width the steps of a horizontal stepper need with each name on one line. */
@Composable
private fun rememberIdealRowWidth(
    steps: List<MonarchStepperStep>,
    current: Int,
    markerSize: Dp,
    optional: String,
): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val label = MonarchTheme.typography.label
    val caption = MonarchTheme.typography.caption
    return remember(steps, current, markerSize, optional, label, caption, density) {
        val gap = MonarchStepperTokens.Gap
        val perStep = MonarchStepperTokens.ItemPadding * 2 + markerSize + gap
        val betweenSteps = gap + MonarchStepperTokens.LineMinimum + gap
        val texts = steps.mapIndexed { index, step ->
            val weight = if (index == current) FontWeight.SemiBold else FontWeight.Medium
            val widths = listOfNotNull(
                measurer.measure(step.label, label.copy(fontWeight = weight), softWrap = false).size.width,
                step.description?.let { measurer.measure(it, caption, softWrap = false).size.width },
                if (step.optional) measurer.measure(optional, caption, softWrap = false).size.width else null,
            )
            with(density) { widths.max().toDp() }
        }
        texts.fold(betweenSteps * (steps.size - 1)) { total, text -> total + perStep + text }
    }
}

@Composable
private fun HorizontalSteps(
    steps: List<MonarchStepperStep>,
    current: Int,
    markerSize: Dp,
    name: String,
    maxWidth: Dp,
    bounded: Boolean,
    step: @Composable (Int, Modifier) -> Unit,
) {
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = name
                isTraversalGroup = true
                collectionInfo = CollectionInfo(rowCount = 1, columnCount = steps.size)
            },
        horizontalArrangement = Arrangement.spacedBy(MonarchStepperTokens.Gap),
        verticalAlignment = Alignment.Top,
    ) {
        steps.indices.forEach { index ->
            val item = Modifier.semantics {
                collectionItemInfo = CollectionItemInfo(0, 1, index, 1)
            }
            if (index == steps.lastIndex) {
                val last = if (bounded) Modifier.widthIn(max = maxWidth / steps.size) else Modifier
                Box(modifier = last) { step(index, item) }
            } else {
                val status = MonarchStepperTokens.status(index, current, steps[index])
                val line by animateColorAsState(
                    MonarchStepperTokens.lineColor(status, colors),
                    MonarchTheme.motion.spec(MonarchTheme.motion.fast),
                    label = "stepper line",
                )
                StepWithLine(
                    markerSize = markerSize,
                    lineColor = line,
                    modifier = if (bounded) Modifier.weight(1f) else Modifier,
                ) { step(index, item) }
            }
        }
    }
}

/** A step followed by the line to the next one, which takes the space the step leaves. */
@Composable
private fun StepWithLine(
    markerSize: Dp,
    lineColor: Color,
    modifier: Modifier,
    item: @Composable () -> Unit,
) {
    Layout(
        content = {
            item()
            Box(Modifier.background(lineColor, MonarchShapeDefaults.Mark))
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val gap = MonarchStepperTokens.Gap.roundToPx()
        val minimumLine = MonarchStepperTokens.LineMinimum.roundToPx()
        val thickness = MonarchStepperTokens.Line.roundToPx()
        val itemMax = if (constraints.hasBoundedWidth) {
            (constraints.maxWidth - gap - minimumLine).coerceAtLeast(0)
        } else {
            Constraints.Infinity
        }
        val itemPlaceable = measurables[0].measure(Constraints(maxWidth = itemMax, maxHeight = constraints.maxHeight))
        val lineWidth = if (constraints.hasBoundedWidth) {
            (constraints.maxWidth - itemPlaceable.width - gap).coerceAtLeast(minimumLine)
        } else {
            minimumLine
        }
        val linePlaceable = measurables[1].measure(Constraints.fixed(lineWidth, thickness))
        val lineTop = (MonarchStepperTokens.ItemPadding + markerSize / 2).roundToPx() - thickness / 2
        val width = itemPlaceable.width + gap + lineWidth
        val height = maxOf(itemPlaceable.height, lineTop + thickness)
        layout(width, height) {
            itemPlaceable.placeRelative(0, 0)
            linePlaceable.placeRelative(itemPlaceable.width + gap, lineTop)
        }
    }
}

@Composable
private fun VerticalSteps(
    steps: List<MonarchStepperStep>,
    current: Int,
    markerSize: Dp,
    name: String,
    step: @Composable (Int, Modifier) -> Unit,
) {
    val colors = MonarchTheme.colors
    Column(
        modifier = Modifier.semantics {
            contentDescription = name
            isTraversalGroup = true
            collectionInfo = CollectionInfo(rowCount = steps.size, columnCount = 1)
        },
    ) {
        steps.indices.forEach { index ->
            step(index, Modifier.semantics { collectionItemInfo = CollectionItemInfo(index, 1, 0, 1) })
            if (index != steps.lastIndex) {
                val status = MonarchStepperTokens.status(index, current, steps[index])
                val line by animateColorAsState(
                    MonarchStepperTokens.lineColor(status, colors),
                    MonarchTheme.motion.spec(MonarchTheme.motion.fast),
                    label = "stepper line",
                )
                Box(
                    Modifier
                        .padding(
                            start = MonarchStepperTokens.ItemPadding + markerSize / 2 - MonarchStepperTokens.Line / 2,
                            top = MonarchStepperTokens.LineMargin,
                            bottom = MonarchStepperTokens.LineMargin,
                        )
                        .size(width = MonarchStepperTokens.Line, height = MonarchStepperTokens.LineMinimum)
                        .background(line, MonarchShapeDefaults.Mark),
                )
            }
        }
    }
}

@Composable
private fun StepItem(
    steps: List<MonarchStepperStep>,
    index: Int,
    current: Int,
    furthest: Int,
    markerSize: Dp,
    onSelect: ((MonarchStepperStep, Int) -> Unit)?,
    strings: StepperStrings,
    busy: Boolean,
    modifier: Modifier,
) {
    val step = steps[index]
    val colors = MonarchTheme.colors
    val status = MonarchStepperTokens.status(index, current, step)
    val selectable = MonarchStepperTokens.selectable(index, current, furthest, step, onSelect != null)
    val disabled = onSelect != null && !step.enabled
    val name = listOfNotNull(strings.stepOf(index + 1, steps.size), step.label, step.description).joinToString(", ")
    val state = listOfNotNull(
        when (status) {
            MonarchStepperStatus.Complete -> strings.complete
            MonarchStepperStatus.Error -> strings.error
            MonarchStepperStatus.Current -> if (busy) "${strings.current}, ${strings.busy}" else strings.current
            MonarchStepperStatus.Upcoming -> null
        },
        if (step.optional) strings.optional else null,
    ).joinToString(", ").ifEmpty { null }
    val shape = MonarchTheme.shapes.md
    val target = if (onSelect != null) {
        Modifier.sizeIn(minWidth = MonarchTouchTarget.Minimum, minHeight = MonarchTouchTarget.Minimum)
    } else {
        Modifier
    }
    val interaction = if (selectable && onSelect != null) {
        Modifier
            .monarchFocusRing(shape)
            .clip(shape)
            .clickable(role = Role.Button) { onSelect(step, index) }
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .then(interaction)
            .semantics(mergeDescendants = true) {
                contentDescription = name
                state?.let { stateDescription = it }
                if (disabled) disabled()
            }
            .then(target)
            .padding(MonarchStepperTokens.ItemPadding),
        horizontalArrangement = Arrangement.spacedBy(MonarchStepperTokens.Gap),
        verticalAlignment = Alignment.Top,
    ) {
        Marker(index, status, markerSize, busy && status == MonarchStepperStatus.Current)
        StepText(step, status, disabled, strings.optional, MonarchStepperTokens.secondaryColor(colors))
    }
}

@Composable
private fun Marker(index: Int, status: MonarchStepperStatus, markerSize: Dp, busy: Boolean) {
    val target = MonarchStepperTokens.markerColors(status, MonarchTheme.colors)
    val spec = MonarchTheme.motion.spec<Color>(MonarchTheme.motion.fast)
    val container by animateColorAsState(target.container, spec, label = "marker fill")
    val content by animateColorAsState(target.content, spec, label = "marker content")
    val border by animateColorAsState(target.border, spec, label = "marker border")
    val ring by animateColorAsState(target.ring, spec, label = "marker ring")
    val ringWidth = MonarchStepperTokens.Ring
    Box(
        modifier = Modifier
            .clearAndSetSemantics { }
            .size(markerSize)
            .drawBehind {
                val stroke = ringWidth.toPx()
                drawCircle(color = ring, radius = size.minDimension / 2 + stroke / 2, style = Stroke(stroke))
            }
            .background(container, CircleShape)
            .border(MonarchStepperTokens.MarkerBorder, border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            busy -> MonarchProgressRing(
                progress = null,
                diameter = markerSize * MonarchStepperTokens.GLYPH_RATIO,
                color = content,
                trackColor = Color.Transparent,
                strokeWidth = MonarchStepperTokens.MarkerBorder,
            )
            status == MonarchStepperStatus.Complete || status == MonarchStepperStatus.Error ->
                Glyph(status, content, markerSize)
            else -> Text(
                text = (index + 1).toString(),
                color = content,
                style = MonarchTheme.typography.caption.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}

/** The check mark of a finished step or the exclamation mark of a failed one, drawn as the web's SVG. */
@Composable
private fun Glyph(status: MonarchStepperStatus, color: Color, markerSize: Dp) {
    Canvas(modifier = Modifier.size(markerSize * MonarchStepperTokens.GLYPH_RATIO)) {
        val unit = size.width / MonarchStepperTokens.GRID
        val stroke = Stroke(
            width = MonarchStepperTokens.GLYPH_STROKE * unit,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        if (status == MonarchStepperStatus.Complete) {
            val points = MonarchStepperTokens.CheckPoints.map { it * unit }
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, color, style = stroke)
        } else {
            val (top, bottom) = MonarchStepperTokens.ErrorBar
            drawLine(color, top * unit, bottom * unit, stroke.width, StrokeCap.Round)
            drawCircle(color, radius = stroke.width / 2, center = MonarchStepperTokens.ErrorDot * unit)
        }
    }
}

@Composable
private fun StepText(
    step: MonarchStepperStep,
    status: MonarchStepperStatus,
    disabled: Boolean,
    optional: String,
    secondary: Color,
) {
    val colors = MonarchTheme.colors
    val caption = MonarchTheme.typography.caption.copy(color = secondary)
    Column(
        modifier = Modifier.padding(top = MonarchStepperTokens.TextGap),
        verticalArrangement = Arrangement.spacedBy(MonarchStepperTokens.TextGap),
    ) {
        Text(
            text = step.label,
            style = MonarchTheme.typography.label.copy(
                color = MonarchStepperTokens.labelColor(status, disabled, colors),
                fontWeight = if (status == MonarchStepperStatus.Current) FontWeight.SemiBold else FontWeight.Medium,
            ),
        )
        step.description?.let { Text(text = it, style = caption) }
        if (step.optional) Text(text = optional, style = caption.copy(fontStyle = FontStyle.Italic))
    }
}

@Composable
private fun CompactSummary(steps: List<MonarchStepperStep>, current: Int, strings: StepperStrings, busy: Boolean) {
    val finished = current >= steps.size
    val shown = current.coerceAtMost(steps.lastIndex)
    val count = strings.stepOf(shown + 1, steps.size)
    val label = steps[shown].label
    val state = listOfNotNull(count, label, strings.complete.takeIf { finished }, strings.busy.takeIf { busy })
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = strings.progress
                stateDescription = state.joinToString(", ")
            },
        verticalArrangement = Arrangement.spacedBy(MonarchStepperTokens.CompactGap),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MonarchStepperTokens.Gap),
            verticalArrangement = Arrangement.spacedBy(MonarchStepperTokens.SummaryRowGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = count,
                style = MonarchTheme.typography.caption.copy(
                    color = MonarchStepperTokens.secondaryColor(MonarchTheme.colors),
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = label,
                style = MonarchTheme.typography.label.copy(
                    color = MonarchTheme.colors.textDefault,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
        MonarchProgress(
            progress = (shown + 1).toFloat() / steps.size,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Whether the steps are drawn in a row. A horizontal stepper that may not be compact
 * ([MonarchStepperCompactMode.Never]) and whose names do not fit on one line each is drawn as a column, so
 * that no name is broken into single letters.
 */
internal fun monarchStepperIsRow(
    mode: MonarchStepperCompactMode,
    orientation: MonarchStepperOrientation,
    fits: Boolean,
): Boolean = orientation == MonarchStepperOrientation.Horizontal && (fits || mode != MonarchStepperCompactMode.Never)
