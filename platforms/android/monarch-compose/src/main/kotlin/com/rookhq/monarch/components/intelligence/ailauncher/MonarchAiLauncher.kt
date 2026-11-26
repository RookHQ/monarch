package com.rookhq.monarch.components.intelligence.ailauncher

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.foundation.monarchShadow
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The button that opens and closes an AI assistant: a round floating action
 * button or a quieter toolbar button, with an unread dot, a count or a
 * working ring.
 *
 * The solid variant is a Material 3 `FloatingActionButton`, or an
 * `ExtendedFloatingActionButton` when the label shows; the subtle variant is a
 * Monarch button drawn in the assistant colours. The dot and the count
 * are a Material 3 `Badge` in a `BadgedBox` at the top end corner. The button
 * toggles [open]. TalkBack reads its name ("Assistant"), then "Open" while open
 * and the badge ("New messages", "Working" or the count), so nothing rests on
 * the dot alone. The working ring travels around the button and stands still
 * when animations are off.
 *
 * Place the launcher yourself, for example in the `floatingActionButton` slot of a `Scaffold`, which keeps it
 * clear of bars and the keyboard.
 *
 * @param open whether the assistant the launcher controls is open.
 * @param onOpenChange called with the new state when the user activates the button.
 * @param icon the assistant glyph, for example a `MonarchIcon` with a `null` content description. Drawn by default,
 * as on the web; `null` draws none.
 * @param modifier the modifier of the launcher.
 * @param variant a solid floating button or a subtle toolbar button.
 * @param badge the unread dot, the working ring or a count. `null` for none.
 * @param enabled whether the user can activate the launcher.
 * @param showLabel shows the label beside the icon. `null` shows it for the subtle variant only.
 * @param closeIcon replaces [icon] on the solid variant while the assistant is open, for example a close mark.
 * @param label the name of the button and its visible label. `null` uses `monarch_ailauncher_label`,
 * "Assistant".
 * @param badgeLabel replaces the state TalkBack reads for [badge]. `null` uses `monarch_ailauncher_unread`,
 * `monarch_ailauncher_working` or the plural `monarch_ailauncher_count`.
 * @param openLabel replaces the state TalkBack reads while open. `null` uses `monarch_ailauncher_open`, "Open".
 * @param interactionSource the source of the press, focus and hover interactions of the button.
 */
@Composable
@Suppress("ModifierParameter") // The icon keeps its place before the modifier, so positional callers still compile.
public fun MonarchAiLauncher(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    icon: @Composable () -> Unit = MonarchDefaultIcons.Bot,
    modifier: Modifier = Modifier,
    variant: MonarchAiLauncherVariant = MonarchAiLauncherVariant.Solid,
    badge: MonarchAiLauncherBadge? = null,
    enabled: Boolean = true,
    showLabel: Boolean? = null,
    closeIcon: (@Composable () -> Unit)? = null,
    label: String? = null,
    badgeLabel: String? = null,
    openLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val name = monarchString(label, R.string.monarch_ailauncher_label)
    val state = MonarchAiLauncherTokens.stateDescription(
        open = open,
        openLabel = monarchString(openLabel, R.string.monarch_ailauncher_open),
        badgeLabel = badgeLabel ?: badgeText(badge),
    )
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val colors = MonarchAiLauncherTokens.colors(variant, open, enabled, pressed, MonarchTheme.colors)
    val labelShown = MonarchAiLauncherTokens.showsLabel(variant, showLabel)
    val glyph = glyph(open, variant, icon, closeIcon)
    val shape = if (variant == MonarchAiLauncherVariant.Solid) MonarchTheme.shapes.full else MonarchTheme.shapes.md
    val ring = if (badge == MonarchAiLauncherBadge.Working) Modifier.workingRing(shape) else Modifier
    val minimumInteractiveSize = LocalMinimumInteractiveComponentSize.current
    BadgedBox(
        modifier = modifier,
        badge = { LauncherBadge(badge) },
    ) {
        // Monarch reserves the touch target outside the focus ring, as MonarchButton does.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            LauncherButton(
                style = LauncherStyle(variant, labelShown, enabled, shape, colors),
                onClick = { onOpenChange(!open) },
                modifier = Modifier
                    .monarchMinimumTouchTarget()
                    .then(ring)
                    .monarchFocusRing(shape)
                    .semantics {
                        contentDescription = name
                        state?.let { stateDescription = it }
                    },
                interactionSource = source,
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides minimumInteractiveSize) {
                    LauncherContent(glyph = glyph, text = name.takeIf { labelShown })
                }
            }
        }
    }
}

private fun glyph(
    open: Boolean,
    variant: MonarchAiLauncherVariant,
    icon: @Composable () -> Unit,
    closeIcon: (@Composable () -> Unit)?,
): @Composable () -> Unit =
    if (open && variant == MonarchAiLauncherVariant.Solid && closeIcon != null) closeIcon else icon

/** The look of the button, decided once by the launcher. */
private class LauncherStyle(
    val variant: MonarchAiLauncherVariant,
    val labelShown: Boolean,
    val enabled: Boolean,
    val shape: Shape,
    val colors: MonarchSurfaceColors,
)

/** The button of the variant: a Material 3 floating action button, or the subtle Monarch button. */
@Composable
private fun LauncherButton(
    style: LauncherStyle,
    onClick: () -> Unit,
    modifier: Modifier,
    interactionSource: MutableInteractionSource,
    content: @Composable () -> Unit,
) {
    val colors = style.colors
    val floating = modifier.monarchShadow(MonarchTheme.elevation.md, style.shape).disabledWhen(!style.enabled)
    // A floating action button has no disabled state in Material 3; the click is ignored instead.
    val floatingClick = { if (style.enabled) onClick() }
    when {
        style.variant == MonarchAiLauncherVariant.Subtle -> Row(
            modifier = modifier
                .heightIn(min = MonarchAiLauncherTokens.SubtleHeight)
                .clip(style.shape)
                .background(colors.container, style.shape)
                .then(
                    colors.border?.let { Modifier.border(MonarchAiLauncherTokens.BorderWidth, it, style.shape) }
                        ?: Modifier,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    enabled = style.enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(horizontal = MonarchAiLauncherTokens.SubtlePadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.content, content = content)
        }
        style.labelShown -> ExtendedFloatingActionButton(
            onClick = floatingClick,
            modifier = floating,
            shape = style.shape,
            containerColor = colors.container,
            contentColor = colors.content,
            elevation = noElevation(),
            interactionSource = interactionSource,
        ) { content() }
        else -> FloatingActionButton(
            onClick = floatingClick,
            modifier = floating,
            shape = style.shape,
            containerColor = colors.container,
            contentColor = colors.content,
            elevation = noElevation(),
            interactionSource = interactionSource,
        ) { content() }
    }
}

@Composable
private fun LauncherContent(glyph: @Composable () -> Unit, text: String?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchAiLauncherTokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clearAndSetSemantics { }) { glyph() }
        if (text != null) Text(text = text, style = MonarchTheme.typography.label, maxLines = 1)
    }
}

@Composable
private fun LauncherBadge(badge: MonarchAiLauncherBadge?) {
    val colors = MonarchAiLauncherTokens.badgeColors(MonarchTheme.colors)
    when (badge) {
        MonarchAiLauncherBadge.Unread ->
            Badge(Modifier.clearAndSetSemantics { }, containerColor = colors.container, contentColor = colors.content)
        is MonarchAiLauncherBadge.Count -> MonarchAiLauncherTokens.countText(badge.count)?.let { count ->
            Badge(Modifier.clearAndSetSemantics { }, containerColor = colors.container, contentColor = colors.content) {
                Text(count)
            }
        }
        MonarchAiLauncherBadge.Working, null -> Unit
    }
}

@Composable
private fun badgeText(badge: MonarchAiLauncherBadge?): String? = when (badge) {
    MonarchAiLauncherBadge.Unread -> monarchString(null, R.string.monarch_ailauncher_unread)
    MonarchAiLauncherBadge.Working -> monarchString(null, R.string.monarch_ailauncher_working)
    is MonarchAiLauncherBadge.Count ->
        pluralStringResource(R.plurals.monarch_ailauncher_count, badge.count, badge.count).takeIf { badge.count > 0 }
    null -> null
}

@Composable
private fun noElevation() = FloatingActionButtonDefaults.elevation(
    defaultElevation = MonarchTheme.spacing.space0,
    pressedElevation = MonarchTheme.spacing.space0,
    focusedElevation = MonarchTheme.spacing.space0,
    hoveredElevation = MonarchTheme.spacing.space0,
)

private fun Modifier.disabledWhen(disabled: Boolean): Modifier =
    if (disabled) semantics { disabled() } else this

/** The working ring: half of the outline travels around the button, or all of it stands still. */
@Composable
private fun Modifier.workingRing(shape: Shape): Modifier {
    val color = MonarchAiLauncherTokens.ringColor(MonarchTheme.colors)
    val still = MonarchTheme.motion.reducedMotion
    val phase: State<Float>? = if (still) {
        null
    } else {
        rememberInfiniteTransition(label = "launcher ring").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(MonarchAiLauncherTokens.RING_PERIOD_MILLIS, easing = LinearEasing),
                RepeatMode.Restart,
            ),
            label = "ring phase",
        )
    }
    return drawWithContent {
        drawContent()
        val stroke = MonarchAiLauncherTokens.RingWidth.toPx()
        val outset = MonarchAiLauncherTokens.RingOutset.toPx() + stroke / 2
        val ringSize = Size(size.width + outset * 2, size.height + outset * 2)
        val outline = Path().apply { addOutline(shape.createOutline(ringSize, layoutDirection, this@drawWithContent)) }
        val path = phase?.let { segment(outline, it.value) } ?: outline
        translate(-outset, -outset) { drawPath(path, color, style = Stroke(stroke)) }
    }
}

/** The part of [outline] the moving ring covers at [phase], a share of one turn. */
private fun segment(outline: Path, phase: Float): Path {
    val measure = PathMeasure().apply { setPath(outline, true) }
    val length = measure.length
    val start = phase * length
    val end = start + MonarchAiLauncherTokens.RING_ARC * length
    return Path().apply {
        measure.getSegment(start, minOf(end, length), this, true)
        if (end > length) measure.getSegment(0f, end - length, this, true)
    }
}

