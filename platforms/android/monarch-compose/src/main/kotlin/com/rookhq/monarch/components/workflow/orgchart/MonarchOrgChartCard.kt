package com.rookhq.monarch.components.workflow.orgchart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarTokens
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadgeSize
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchGlyphs
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

/** The words TalkBack reads for [node]: name, title, meta, then the notes that apply. */
internal fun monarchOrgChartDescription(node: MonarchOrgChartNode, context: MonarchOrgChartContext): String {
    val strings = context.strings
    return listOfNotNull(
        node.name,
        node.title,
        node.meta,
        strings.vacant.takeIf { node.kind == MonarchOrgChartNodeKind.Vacancy },
        strings.dashed.takeIf { node.dashed },
        context.dotted[node.id]?.let { strings.alsoReportsTo(it.joinToString(", ")) },
    ).joinToString(", ")
}

/** The state TalkBack reads for [node]: whether its team is shown, and how many report to it. */
internal fun monarchOrgChartState(node: MonarchOrgChartNode, context: MonarchOrgChartContext): String? {
    val count = context.tree.reportCount(node)
    if (count == 0) return null
    val strings = context.strings
    val shown = when (node.id) {
        in context.loading -> strings.loading
        in context.open -> strings.expanded
        else -> strings.collapsed
    }
    return "$shown, ${strings.reports(count)}"
}

/**
 * One person or open position: an avatar (or a muted tile), the name, title,
 * meta and notes, read by TalkBack as one element with actions.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun MonarchOrgChartCard(
    node: MonarchOrgChartNode,
    context: MonarchOrgChartContext,
    modifier: Modifier = Modifier,
) {
    val positions = context.positions
    val focused = node.id == context.focusId
    val vacancy = node.kind == MonarchOrgChartNodeKind.Vacancy
    val colors = MonarchOrgChartTokens.cardColors(
        colors = MonarchTheme.colors,
        vacancy = vacancy,
        dashed = node.dashed,
        focused = focused,
        dropTarget = positions.target == node.id,
        enabled = context.enabled,
    )
    val shape = MonarchTheme.shapes.md
    val description = monarchOrgChartDescription(node, context)
    val state = monarchOrgChartState(node, context)
    val actions = cardActions(node, context)
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(focused) { if (focused) requester.bringIntoView() }
    DisposableEffect(node.id) { onDispose { positions.cards.remove(node.id) } }
    val dragged = positions.dragged == node.id
    val select = context.onSelect?.takeIf { context.enabled }
    Row(
        modifier = modifier
            .onGloballyPositioned { positions.place(positions.cards, node.id, it) }
            .bringIntoViewRequester(requester)
            .zIndex(if (dragged) 1f else 0f)
            .then(if (context.movable(node)) Modifier.monarchOrgChartDrag(node, context) else Modifier)
            .heightIn(min = MonarchTouchTarget.Minimum)
            .background(colors.container, shape)
            .then(
                if (node.dashed || vacancy) {
                    Modifier.drawBehind {
                        val width = colors.borderWidth.toPx()
                        val radius = shape.topStart.toPx(size, this)
                        drawRoundRect(
                            color = colors.border,
                            topLeft = Offset(width / 2, width / 2),
                            size = Size(size.width - width, size.height - width),
                            cornerRadius = CornerRadius(radius),
                            style = Stroke(
                                width = width,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(
                                        MonarchOrgChartTokens.DASH * width,
                                        MonarchOrgChartTokens.DASH_GAP * width,
                                    ),
                                ),
                            ),
                        )
                    }
                } else {
                    Modifier.border(colors.borderWidth, colors.border, shape)
                },
            )
            .then(
                if (select != null) {
                    Modifier
                        .monarchFocusRing(shape)
                        .clickable(role = Role.Button, interactionSource = null, indication = ripple()) { select(node) }
                } else {
                    Modifier
                },
            )
            .semantics(mergeDescendants = true) {
                contentDescription = description
                state?.let { stateDescription = it }
                if (focused) selected = true
                if (actions.isNotEmpty()) customActions = actions
            }
            .padding(MonarchOrgChartTokens.CardPadding),
        horizontalArrangement = Arrangement.spacedBy(MonarchOrgChartTokens.CardGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f, fill = false)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchOrgChartTokens.CardGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            if (vacancy) {
                Box(
                    Modifier
                        .size(MonarchAvatarTokens.side(MonarchAvatarSize.Md))
                        .background(MonarchOrgChartTokens.vacancyTile(MonarchTheme.colors), MonarchShapeDefaults.Mark),
                    contentAlignment = Alignment.Center,
                ) {
                    MonarchIcon(
                        MonarchGlyphs.Work,
                        contentDescription = null,
                        size = MonarchIconSize.Sm,
                        tint = MonarchTheme.colors.textMuted,
                    )
                }
            } else {
                MonarchAvatar(image = node.image, name = node.name, size = MonarchAvatarSize.Md, decorative = true)
            }
            Column(Modifier.weight(1f, fill = false)) {
                MonarchText(text = node.name, style = MonarchTextStyle.BodyStrong, color = colors.name)
                val notes = listOfNotNull(
                    node.title,
                )
                notes.forEach { MonarchText(text = it, style = MonarchTextStyle.Caption, color = colors.secondary) }
            }
            }
            node.meta?.let {
                MonarchBadge(
                    text = it,
                    modifier = Modifier.padding(top = MonarchOrgChartTokens.ChipGap),
                    tone = node.metaTone ?: MonarchTone.Neutral,
                    size = MonarchBadgeSize.Sm,
                )
            }
        }
    }
}

/** The TalkBack actions of a card: show or hide the team, and move. */
@Composable
private fun cardActions(node: MonarchOrgChartNode, context: MonarchOrgChartContext): List<CustomAccessibilityAction> {
    val toggle by rememberUpdatedState(context.onToggle)
    val move by rememberUpdatedState(context.onMove)
    val strings = context.strings
    val actions = mutableListOf<CustomAccessibilityAction>()
    if (context.enabled && context.tree.reportCount(node) > 0 && node.id !in context.loading) {
        val label = if (node.id in context.open) strings.collapse(node.name) else strings.expand(node.name)
        actions += CustomAccessibilityAction(label) {
            toggle(node)
            true
        }
    }
    if (context.movable(node)) {
        actions += CustomAccessibilityAction(strings.move) {
            move?.invoke(node, MonarchOrgChartMoveTarget.Unspecified)
            true
        }
    }
    return actions
}

/** Touch and hold, then drag: the card follows the finger and is dropped on a manager or the background. */
@Composable
private fun Modifier.monarchOrgChartDrag(node: MonarchOrgChartNode, context: MonarchOrgChartContext): Modifier {
    val positions = context.positions
    val haptics = LocalHapticFeedback.current
    val tree by rememberUpdatedState(context.tree)
    val move by rememberUpdatedState(context.onMove)
    val inner = remember { arrayOfNulls<LayoutCoordinates>(1) }
    return this
        .graphicsLayer {
            if (positions.dragged == node.id) {
                translationX = positions.offset.x
                translationY = positions.offset.y
            }
        }
        .onGloballyPositioned { inner[0] = it }
        .pointerInput(node.id) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    positions.start(node.id)
                },
                onDrag = { change, amount ->
                    change.consume()
                    val coordinates = inner[0]?.takeIf { it.isAttached } ?: return@detectDragGesturesAfterLongPress
                    val pointer = coordinates.localToWindow(change.position)
                    positions.move(amount, pointer, tree)
                },
                onDragEnd = { positions.finish(tree)?.let { target -> move?.invoke(node, target) } },
                onDragCancel = { positions.cancel() },
            )
        }
}

/**
 * The chip under a manager that shows or hides the team: the number of
 * reports and a chevron that turns when the team is shown, or a spinner while
 * it loads. A button with a 48dp touch target.
 */
@Composable
internal fun MonarchOrgChartChip(
    node: MonarchOrgChartNode,
    context: MonarchOrgChartContext,
    modifier: Modifier = Modifier,
) {
    val positions = context.positions
    val open = node.id in context.open
    val loading = node.id in context.loading
    val strings = context.strings
    val name = if (open) strings.collapse(node.name) else strings.expand(node.name)
    val state = if (loading) strings.loading else if (open) strings.expanded else strings.collapsed
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val turn = if (rtl) -MonarchOrgChartTokens.EXPANDED_ROTATION else MonarchOrgChartTokens.EXPANDED_ROTATION
    val rotation by animateFloatAsState(
        targetValue = if (open) turn else 0f,
        animationSpec = MonarchTheme.motion.spec(MonarchTheme.motion.fast),
        label = "org chart chip chevron",
    )
    DisposableEffect(node.id) { onDispose { positions.chips.remove(node.id) } }
    val colors = MonarchTheme.colors
    val chipColor = MonarchOrgChartTokens.chipContent(colors, context.enabled && !loading)
    Box(
        modifier = modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Mark)
            .clickable(
                enabled = context.enabled && !loading,
                role = Role.Button,
                interactionSource = null,
                indication = ripple(),
            ) { context.onToggle(node) }
            .semantics {
                contentDescription = name
                stateDescription = state
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .onGloballyPositioned { positions.place(positions.chips, node.id, it) }
                .background(colors.bgSurface, MonarchShapeDefaults.Mark)
                .border(
                    MonarchOrgChartTokens.BorderWidth,
                    MonarchOrgChartTokens.lineColor(colors),
                    MonarchShapeDefaults.Mark,
                )
                .padding(horizontal = MonarchOrgChartTokens.CardGap, vertical = MonarchOrgChartTokens.ChipGap)
                .clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(MonarchOrgChartTokens.ChipGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading && !MonarchTheme.motion.reducedMotion) {
                CircularProgressIndicator(
                    modifier = Modifier.size(MonarchOrgChartTokens.SpinnerSize),
                    color = chipColor,
                    strokeWidth = MonarchOrgChartTokens.SpinnerStroke,
                )
            }
            MonarchText(
                text = context.tree.reportCount(node).toString(),
                style = MonarchTextStyle.Caption,
                color = chipColor,
            )
            CompositionLocalProvider(LocalContentColor provides chipColor) {
                Box(Modifier.graphicsLayer { rotationZ = rotation }) { context.expandIcon() }
            }
        }
    }
}
