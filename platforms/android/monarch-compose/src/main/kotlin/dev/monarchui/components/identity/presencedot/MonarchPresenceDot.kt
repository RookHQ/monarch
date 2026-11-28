package dev.monarchui.components.identity.presencedot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.R
import dev.monarchui.internal.MonarchPresenceStrings
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme

/**
 * A small dot for a person's presence: online, away, busy, do not disturb or offline.
 *
 * Built by Monarch from a Compose `Canvas`: Material 3 has no equivalent. The
 * dot is one TalkBack element read as its text, for example "Away · In a
 * meeting", so colour is never the only signal. Do not disturb has a bar and
 * offline is a ring in every theme; in the high-contrast themes away is
 * half-filled and busy is a square, so all five differ by form. The dot grows
 * with the font size setting. It is not interactive.
 *
 * Place it on an avatar with `Modifier.align(Alignment.BottomEnd)` inside a
 * `Box` and set [overlay].
 *
 * @param status the presence to show.
 * @param modifier the modifier of the dot.
 * @param size the diameter: 8, 10 or 12dp at the default font size.
 * @param customStatus the person's own status message, for example "In a meeting". Added to the text
 * the dot is read as. Blank text is ignored.
 * @param overlay draws a ring in the surface colour around the dot, for a dot on an avatar.
 * @param decorative hides the dot from TalkBack, when nearby text already says the same.
 * @param statusLabel the name of the presence. `null` uses the built-in string of the status, for example
 * `monarch_presencedot_away`.
 */
@Composable
public fun MonarchPresenceDot(
    status: MonarchPresenceStatus,
    modifier: Modifier = Modifier,
    size: MonarchPresenceDotSize = MonarchPresenceDotSize.Md,
    customStatus: String? = null,
    overlay: Boolean = false,
    decorative: Boolean = false,
    statusLabel: String? = null,
) {
    val colors = MonarchTheme.colors
    val form = MonarchPresenceDotTokens.form(status, MonarchTheme.isHighContrast)
    val presence = monarchString(statusLabel, MonarchPresenceStrings.resource(status))
    val custom = MonarchPresenceDotTokens.customText(customStatus)
    val label = if (custom == null) {
        presence
    } else {
        stringResource(R.string.monarch_presencedot_with_status, presence, custom)
    }
    val diameter = MonarchPresenceDotTokens.diameter(size) * LocalDensity.current.fontScale
    val ring = MonarchPresenceDotTokens.StrokeWidth
    val outer = if (overlay) diameter + ring * 2 else diameter
    Canvas(
        modifier = modifier
            .size(outer)
            .clearAndSetSemantics { if (!decorative) contentDescription = label },
    ) {
        val inset = if (overlay) ring.toPx() else 0f
        if (overlay) drawCircle(MonarchPresenceDotTokens.surfaceColor(colors))
        drawPresence(status, form, colors, inset, ring.toPx())
    }
}


/** Draws the dot inside the canvas, [inset] from its edge. */
private fun DrawScope.drawPresence(
    status: MonarchPresenceStatus,
    form: MonarchPresenceDotForm,
    colors: MonarchColors,
    inset: Float,
    stroke: Float,
) {
    val color = MonarchPresenceDotTokens.color(status, colors)
    val surface = MonarchPresenceDotTokens.surfaceColor(colors)
    val dot = size.minDimension - inset * 2
    val radius = dot / 2
    val centre = Offset(size.width / 2, size.height / 2)
    when (form) {
        MonarchPresenceDotForm.Filled -> drawCircle(color, radius, centre)
        MonarchPresenceDotForm.Square -> drawRoundRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(dot, dot),
            cornerRadius = CornerRadius(MonarchPresenceDotTokens.SquareCorner.toPx()),
        )
        MonarchPresenceDotForm.Barred -> {
            drawCircle(color, radius, centre)
            val barInset = dot * MonarchPresenceDotTokens.BAR_INSET
            drawLine(
                color = MonarchPresenceDotTokens.barColor(colors),
                start = Offset(inset + barInset, centre.y),
                end = Offset(inset + dot - barInset, centre.y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        MonarchPresenceDotForm.Ring -> drawRing(color, surface, radius, centre, stroke)
        MonarchPresenceDotForm.HalfFilled -> {
            drawRing(color, surface, radius, centre, stroke)
            // The leading half, so it mirrors in right-to-left layouts.
            val leadingLeft = if (layoutDirection == LayoutDirection.Ltr) inset else centre.x
            val half = Rect(leadingLeft, inset, leadingLeft + dot / 2, inset + dot)
            clipRect(half.left, half.top, half.right, half.bottom) { drawCircle(color, radius, centre) }
        }
    }
}

private fun DrawScope.drawRing(color: Color, fill: Color, radius: Float, centre: Offset, stroke: Float) {
    drawCircle(fill, radius, centre)
    drawCircle(color, radius - stroke / 2, centre, style = Stroke(width = stroke))
}
