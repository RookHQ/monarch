package com.rookhq.monarch.components.feedback.celebration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.lerp
import com.rookhq.monarch.R
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A success moment for a real milestone: an icon in a tinted tile with
 * confetti that falls once, a title, a message and the next actions.
 *
 * Built by Monarch: Material 3 has no celebration. The confetti is drawn
 * with a Compose `Canvas`; it only moves and fades, falls once over about
 * 1.9 seconds and then stops drawing. When the user has removed animations
 * the tile appears at full size and the pieces rest as a still scatter. Place
 * it inline, or inside an `AlertDialog`, `Dialog` or `ModalBottomSheet` with
 * a Continue button in [actions].
 *
 * @param title what was achieved. A heading.
 * @param modifier the modifier of the panel.
 * @param message what it means or what to do next, in muted text.
 * @param tone the colours of the tile: the tone's subtle fill and strong foreground.
 * @param decoration confetti that falls once behind the icon, or none.
 * @param announces announces the title and message through a polite live region when it appears. Turn it off
 * when the panel is part of the screen from the start.
 * @param icon the icon in the tile, for example a `MonarchIcon` with a `null` content description; it takes the
 * tone's strong colour. Decorative: say everything in [title] and [message]. Without it no tile is drawn,
 * because Monarch bundles no icons.
 * @param actions the next steps, side by side when they fit and wrapped when they do not.
 */
@Composable
public fun MonarchCelebration(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    tone: MonarchTone = MonarchTone.Success,
    decoration: MonarchCelebrationDecoration = MonarchCelebrationDecoration.Confetti,
    announces: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchCelebrationTokens
    val colors = MonarchTheme.colors
    val separator = stringResource(R.string.monarch_celebration_announcement_separator)
    val announcement = celebrationAnnouncement(announces, listOfNotNull(title, message).joinToString(separator))
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = tokens.MaxWidth)
                .fillMaxWidth()
                .padding(horizontal = tokens.HorizontalPadding, vertical = tokens.VerticalPadding),
            verticalArrangement = Arrangement.spacedBy(tokens.Gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CelebrationVisual(tone = tone, decoration = decoration, icon = icon)
            MonarchText(
                text = title,
                style = MonarchTextStyle.Subheading,
                color = tokens.titleColor(colors),
                textAlign = TextAlign.Center,
            )
            if (message != null) {
                MonarchText(
                    text = message,
                    color = tokens.messageColor(colors),
                    textAlign = TextAlign.Center,
                )
            }
            if (actions != null) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = tokens.ActionsTopPadding),
                    horizontalArrangement = Arrangement.spacedBy(tokens.Gap, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(tokens.Gap),
                ) { actions() }
            }
        }
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** The tile and the confetti behind it, hidden from TalkBack. */
@Composable
private fun CelebrationVisual(
    tone: MonarchTone,
    decoration: MonarchCelebrationDecoration,
    icon: (@Composable () -> Unit)?,
) {
    val tokens = MonarchCelebrationTokens
    val colors = MonarchTheme.colors
    val motion = MonarchTheme.motion
    val reduced = motion.reducedMotion
    val elapsed = remember { Animatable(0f) }
    val pop = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) pop.animateTo(1f, motion.spec(motion.slow, motion.emphasized))
    }
    LaunchedEffect(decoration) {
        if (!reduced && decoration == MonarchCelebrationDecoration.Confetti) {
            elapsed.animateTo(
                tokens.TOTAL_MILLIS.toFloat(),
                tween(tokens.TOTAL_MILLIS, easing = LinearEasing),
            )
        }
    }
    Box(
        modifier = Modifier
            .widthIn(max = tokens.VisualMaxWidth)
            .fillMaxWidth()
            .clearAndSetSemantics { },
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(Modifier.matchParentSize().clipToBounds()) {
            val now = elapsed.value.toLong()
            if (tokens.confettiVisible(decoration, now, reduced)) {
                drawConfetti(colors, now, reduced, motion.standard::transform)
            }
        }
        val tile = tokens.tile(tone, colors)
        Box(
            modifier = Modifier
                .padding(top = tokens.VisualTopPadding)
                .size(tokens.TileSize)
                .graphicsLayer {
                    val scale = lerp(tokens.TILE_START_SCALE, 1f, pop.value)
                    scaleX = scale
                    scaleY = scale
                    alpha = pop.value
                }
                .then(if (icon != null) Modifier.background(tile.container, MonarchTheme.shapes.full) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) CompositionLocalProvider(LocalContentColor provides tile.content) { icon() }
        }
    }
}

/** Draws every piece at [elapsedMillis]. Canvas does not mirror; the scatter has no direction. */
private fun DrawScope.drawConfetti(
    colors: MonarchColors,
    elapsedMillis: Long,
    reducedMotion: Boolean,
    easing: (Float) -> Float,
) {
    val tokens = MonarchCelebrationTokens
    val unit = tokens.Unit.toPx()
    val radius = CornerRadius(tokens.PieceRadius.toPx())
    tokens.Pieces.forEach { piece ->
        val frame = tokens.frame(piece, elapsedMillis, reducedMotion, easing)
        if (frame.opacity <= 0f) return@forEach
        val pieceSize = if (piece.dot) {
            Size(tokens.DotSize.toPx(), tokens.DotSize.toPx())
        } else {
            Size(tokens.BarWidth.toPx(), tokens.BarHeight.toPx())
        }
        val topLeft = Offset(size.width * piece.start + frame.dx * unit, frame.dy * unit)
        val centre = topLeft + Offset(pieceSize.width / 2, pieceSize.height / 2)
        rotate(frame.rotation, pivot = centre) {
            drawRoundRect(
                color = tokens.pieceColor(piece.color, colors),
                topLeft = topLeft,
                size = pieceSize,
                cornerRadius = if (piece.dot) CornerRadius(pieceSize.width / 2) else radius,
                alpha = frame.opacity,
            )
        }
    }
}

/** The text of the polite live region: empty at first, then the title and message one frame later. */
@Composable
private fun celebrationAnnouncement(announces: Boolean, text: String): String {
    var announcement by remember { mutableStateOf("") }
    LaunchedEffect(announces, text) {
        if (announces) {
            announcement = ""
            withFrameNanos { }
            announcement = text
        } else {
            announcement = ""
        }
    }
    return announcement
}
