package com.rookhq.monarch.components.forms.segmentedcontrol

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.foundation.MonarchControlMetrics
import com.rookhq.monarch.foundation.MonarchMotion
import com.rookhq.monarch.internal.choice.drawChoiceFocusRing
import com.rookhq.monarch.internal.choice.indicatorSpring
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * A segmented control: a muted track with a raised white pill under the chosen segment, for switching between a
 * few views of the same content ("List", "Board", "Grid"). Single choice, two to five short options.
 *
 * It replaces the Material 3 `SegmentedButton`, whose outlined buttons with a check mark are not the Monarch
 * look. Behaviour is the platform's: every segment is `Modifier.selectable` with `Role.RadioButton` inside a
 * `selectableGroup`, so TalkBack reads "Week, radio button, selected, 2 of 3"; the arrow keys and Tab move focus
 * with the Monarch focus ring, the space bar or Enter chooses; a segment keeps a 48dp touch target around its
 * visible pill; text wraps at large font sizes and the track grows.
 *
 * ```
 * MonarchSegmentedControl {
 *     Segment(selected = view == "list", onClick = { view = "list" }, label = "List")
 *     Segment(selected = view == "board", onClick = { view = "board" }, label = "Board")
 * }
 * ```
 *
 * @param modifier the modifier of the control.
 * @param size the visible height of the track.
 * @param fillWidth `true` gives every segment the same share of the width; `false` sizes the track to its content.
 * @param content the segments, as calls of [MonarchSegmentedControlScope.Segment].
 */
@Composable
public fun MonarchSegmentedControl(
    modifier: Modifier = Modifier,
    size: MonarchSegmentedControlSize = MonarchSegmentedControlSize.Md,
    fillWidth: Boolean = false,
    content: @Composable MonarchSegmentedControlScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    val motion = MonarchTheme.motion
    val dark = MonarchTheme.isDark
    val trackColor = MonarchSegmentedControlTokens.track(colors)
    val indicatorFill = MonarchSegmentedControlTokens.indicator(colors, dark)
    val indicatorBorder = MonarchSegmentedControlTokens.indicatorBorder(colors, dark)
    val trackRadius = MonarchTokens.Radius.md.dp
    val pillRadius = MonarchTokens.Radius.sm.dp
    val pad = MonarchSegmentedControlTokens.Pad
    val scope = remember(size, fillWidth) { SegmentScope(size, fillWidth) }
    val indicator = rememberIndicator(scope, motion)
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .onPlaced { scope.origin = it.positionInRoot() }
            .drawBehind {
                // The track is the visible height; the 48dp of the segments around it is touch target.
                val visible = MonarchSegmentedControlTokens.trackHeight(size).toPx()
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(0f, (this.size.height - visible) / 2),
                    size = Size(this.size.width, visible),
                    cornerRadius = CornerRadius(trackRadius.toPx()),
                )
                // One indicator for the whole control: it slides to the chosen segment, the layout never moves.
                val rect = indicator.value ?: return@drawBehind
                val corner = CornerRadius(pillRadius.toPx())
                drawRoundRect(indicatorFill, rect.topLeft, rect.size, corner)
                val hairline = MonarchControlMetrics.BorderWidth.toPx()
                drawRoundRect(
                    color = indicatorBorder,
                    topLeft = rect.topLeft + Offset(hairline / 2, hairline / 2),
                    size = Size(rect.width - hairline, rect.height - hairline),
                    cornerRadius = CornerRadius(corner.x - hairline / 2),
                    style = Stroke(hairline),
                )
            }
            .padding(horizontal = pad)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(pad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        scope.row = this
        scope.content()
    }
}

/**
 * The animated bounds of the indicator, relative to the control. The first placement and a change of layout (a new
 * width, a font scale) snap; only a change of the chosen segment slides, on a spring without overshoot.
 */
@Composable
private fun rememberIndicator(scope: SegmentScope, motion: MonarchMotion): State<Rect?> {
    val target = scope.selectedKey?.let { key -> scope.bounds[key] }?.translate(-scope.origin)
    val animated = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
    val shown = remember { mutableStateOf<Rect?>(null) }
    val lastKey = remember { arrayOf<Any?>(null) }
    LaunchedEffect(target, scope.selectedKey) {
        if (target == null) {
            shown.value = null
            return@LaunchedEffect
        }
        val moved = lastKey[0] != null && lastKey[0] != scope.selectedKey && shown.value != null
        lastKey[0] = scope.selectedKey
        if (moved) {
            animated.animateTo(target, motion.indicatorSpring()) { shown.value = value }
        } else {
            animated.snapTo(target)
            shown.value = target
        }
    }
    return shown
}

/**
 * A segmented control of text options, for the common case.
 *
 * @param options the labels, in order.
 * @param selectedIndex the index of the chosen option.
 * @param onSelect called with the index the user chooses.
 * @param modifier the modifier of the control.
 * @param size the visible height of the track.
 * @param fillWidth whether the segments share the width equally.
 * @param disabledIndices the options that cannot be chosen.
 */
@Composable
public fun MonarchSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: MonarchSegmentedControlSize = MonarchSegmentedControlSize.Md,
    fillWidth: Boolean = false,
    disabledIndices: Set<Int> = emptySet(),
) {
    MonarchSegmentedControl(modifier = modifier, size = size, fillWidth = fillWidth) {
        options.forEachIndexed { index, option ->
            Segment(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                label = option,
                enabled = index !in disabledIndices,
            )
        }
    }
}

private class SegmentScope(
    private val size: MonarchSegmentedControlSize,
    private val fillWidth: Boolean,
) : MonarchSegmentedControlScope {
    lateinit var row: RowScope

    /** Where the control is on screen, to place the indicator in its own coordinates. */
    var origin: Offset by mutableStateOf(Offset.Zero)

    /** The pill bounds of every segment, on screen, by a key each segment remembers. */
    val bounds = mutableStateMapOf<Any, Rect>()

    /** The key of the chosen segment, or null when none is chosen. */
    var selectedKey: Any? by mutableStateOf(null)

    @Composable
    override fun Segment(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        icon: (@Composable () -> Unit)?,
        count: Int?,
        suffix: String?,
        interactionSource: MutableInteractionSource?,
        label: @Composable () -> Unit,
    ) {
        val key = remember { Any() }
        SideEffect {
            if (selected) selectedKey = key else if (selectedKey == key) selectedKey = null
        }
        DisposableEffect(key) {
            onDispose {
                bounds.remove(key)
                if (selectedKey == key) selectedKey = null
            }
        }
        val weighted = if (fillWidth) with(row) { Modifier.weight(1f) } else Modifier
        // With fillWidth the pill (and so the indicator) fills its equal share, as on the web.
        val placed = (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .onPlaced { bounds[key] = Rect(it.positionInRoot(), it.size.toSize()) }
        SegmentedItem(
            selected, onClick, weighted.then(modifier), enabled, size, icon, count, suffix, interactionSource, placed,
            label,
        )
    }

    @Composable
    override fun Segment(
        selected: Boolean,
        onClick: () -> Unit,
        label: String,
        modifier: Modifier,
        enabled: Boolean,
        icon: (@Composable () -> Unit)?,
        count: Int?,
        suffix: String?,
        interactionSource: MutableInteractionSource?,
    ) {
        Segment(selected, onClick, modifier, enabled, icon, count, suffix, interactionSource) {
            Text(text = label)
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun SegmentedItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    size: MonarchSegmentedControlSize,
    icon: (@Composable () -> Unit)?,
    count: Int?,
    suffix: String?,
    interactionSource: MutableInteractionSource?,
    pillModifier: Modifier,
    label: @Composable () -> Unit,
) {
    val colors = MonarchTheme.colors
    val motion = MonarchTheme.motion
    val haptics = LocalHapticFeedback.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val pressed by source.collectIsPressedAsState()
    val content by animateColorAsState(
        MonarchSegmentedControlTokens.content(selected, enabled, pressed, colors),
        motion.spec(motion.fast),
        label = "segment content",
    )
    val ring = colors.focusRing
    val ringWidth = MonarchTheme.focusRingWidth
    val style = MonarchSegmentedControlTokens.textStyle(size, MonarchTheme.typography)
    // The semantic node is 48dp high; the pill inside it is the visible segment. The control draws the indicator.
    Box(
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .widthIn(min = MonarchTouchTarget.Minimum)
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = {
                    if (!selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .then(pillModifier)
                .heightIn(min = MonarchSegmentedControlTokens.pillHeight(size))
                .drawWithContent {
                    drawContent()
                    if (focused && enabled) {
                        drawChoiceFocusRing(ring, ringWidth.toPx(), MonarchTokens.Radius.sm.dp.toPx())
                    }
                }
                .padding(horizontal = MonarchSegmentedControlTokens.horizontalPadding(size)),
            horizontalArrangement = Arrangement.spacedBy(
                MonarchSegmentedControlTokens.ContentGap,
                Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides content) {
                ProvideTextStyle(style) {
                    if (icon != null) {
                        Box(Modifier.size(MonarchSegmentedControlTokens.IconSize).clearAndSetSemantics { }) { icon() }
                    }
                    label()
                    if (count != null) CountPill(count, selected)
                    if (suffix != null) {
                        Text(
                            text = suffix,
                            style = MonarchTheme.typography.caption.copy(fontSize = MonarchTokens.FontSize.xs2.sp),
                            color = colors.successStrong,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountPill(count: Int, selected: Boolean) {
    val colors = MonarchTheme.colors
    Text(
        text = count.toString(),
        modifier = Modifier
            .background(if (selected) colors.bgMuted else colors.bgEmphasis, MonarchTheme.shapes.full)
            .widthIn(min = MonarchControlMetrics.CountPillMinWidth)
            .padding(horizontal = MonarchTokens.Space.space1_5.dp, vertical = MonarchTokens.Space.space0_5.dp),
        style = MonarchTheme.typography.caption,
        color = if (selected) colors.textDefault else colors.textMuted,
        textAlign = TextAlign.Center,
    )
}
