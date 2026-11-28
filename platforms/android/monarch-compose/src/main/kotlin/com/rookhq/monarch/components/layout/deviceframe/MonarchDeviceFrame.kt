package com.rookhq.monarch.components.layout.deviceframe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.foundation.monarchShadow
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * A phone or tablet outline around a preview: a link page, an email, a
 * mobile layout.
 *
 * Built by Monarch from Compose shapes: Material 3 has no equivalent. The
 * frame takes the width it is offered up to the device's screen width and
 * keeps the device's aspect ratio; the content is laid out in the screen area
 * and clipped to it, not scaled. By default the preview is one TalkBack
 * element with the image role, named by [label], and its content takes no
 * touches and no focus. With [interactive] it is a group named by [label]
 * whose content can be used. The island, the status bar and the side buttons
 * are decoration and hidden from TalkBack.
 *
 * @param label what the frame shows, for example "Preview of your link page on a phone".
 * @param modifier the modifier of the frame.
 * @param device a phone with a camera island, or a tablet.
 * @param screenSize the width the frame never exceeds and, with the height, its aspect ratio. `null` uses
 * 390 by 844dp for a phone and 820 by 1180dp for a tablet.
 * @param statusBarTime shows a decorative status bar with this time text, for example "9:41".
 * @param interactive lets people scroll and use the content. Put a scrolling container in [content] when
 * it is taller than the screen.
 * @param flat leaves out the side buttons and the shadow, for dense layouts.
 * @param content the screen content.
 */
@Composable
public fun MonarchDeviceFrame(
    label: String,
    modifier: Modifier = Modifier,
    device: MonarchDeviceFrameDevice = MonarchDeviceFrameDevice.Phone,
    screenSize: DpSize? = null,
    statusBarTime: String? = null,
    interactive: Boolean = false,
    flat: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = MonarchDeviceFrameTokens
    val geometry = tokens.geometry(device)
    val screen = screenSize ?: geometry.screen
    val colors = tokens.colors(MonarchTheme.colors, MonarchTheme.isDark)
    val outer = RoundedCornerShape(geometry.corner)
    val inner = RoundedCornerShape(geometry.screenCorner)
    val shadow = MonarchTheme.elevation.xl
    val semantics = if (interactive) {
        Modifier.semantics {
            contentDescription = label
            isTraversalGroup = true
        }
    } else {
        Modifier.clearAndSetSemantics {
            contentDescription = label
            role = Role.Image
        }
    }
    Box(
        modifier = modifier
            .widthIn(max = screen.width)
            .fillMaxWidth()
            .aspectRatio(screen.width / screen.height)
            .then(if (flat) Modifier else Modifier.sideButtons(device, colors.frame).monarchShadow(shadow, outer))
            .background(colors.frame, outer)
            .then(semantics)
            .padding(geometry.bezel),
    ) {
        Box(modifier = Modifier.fillMaxSize().clip(inner).background(colors.screen)) {
            CompositionLocalProvider(LocalContentColor provides colors.content) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (statusBarTime != null) StatusBar(statusBarTime, colors.content)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(if (interactive) Modifier else Modifier.noFocus()),
                        content = content,
                    )
                }
            }
            if (tokens.drawsIsland(device)) {
                Island(color = colors.frame, modifier = Modifier.align(Alignment.TopCenter))
            }
            // A preview takes no touches: this layer on top receives them and does nothing.
            if (!interactive) Box(modifier = Modifier.matchParentSize().blockTouches())
        }
    }
}

/** The camera island of a phone, centred at the top of the screen. */
@Composable
private fun Island(color: Color, modifier: Modifier) {
    val tokens = MonarchDeviceFrameTokens
    Box(
        modifier = modifier
            .padding(top = tokens.IslandTop)
            .fillMaxWidth(tokens.ISLAND_WIDTH_FRACTION)
            .widthIn(max = tokens.IslandMaxWidth)
            .height(tokens.IslandHeight)
            .clearAndSetSemantics {}
            .background(color, MonarchTheme.shapes.full),
    )
}

/** The decorative status bar: the time at the start and signal bars at the end. */
@Composable
private fun StatusBar(time: String, color: Color) {
    val tokens = MonarchDeviceFrameTokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.StatusBarMinHeight)
            .padding(horizontal = tokens.StatusBarHorizontalPadding)
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = time,
            style = MonarchTheme.typography.caption.copy(fontFeatureSettings = TabularFigures),
            fontWeight = FontWeight(MonarchTokens.FontWeight.semibold),
            color = color,
        )
        val bars = tokens.SignalHeights.size
        Canvas(
            modifier = Modifier.size(
                width = tokens.SignalBarWidth * bars + tokens.SignalBarGap * (bars - 1),
                height = tokens.SignalHeight,
            ),
        ) {
            val barWidth = tokens.SignalBarWidth.toPx()
            val gap = tokens.SignalBarGap.toPx()
            tokens.SignalHeights.forEachIndexed { index, fraction ->
                val barHeight = size.height * fraction
                drawRoundRect(
                    color = color,
                    topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2),
                )
            }
        }
    }
}

/** Draws the side buttons just outside the frame; the volume buttons are on the start edge. */
private fun Modifier.sideButtons(device: MonarchDeviceFrameDevice, color: Color): Modifier = drawBehind {
    val tokens = MonarchDeviceFrameTokens
    val width = tokens.SideButtonWidth.toPx()
    val startIsLeft = layoutDirection == LayoutDirection.Ltr
    fun button(atStart: Boolean, top: Float, height: Float) {
        val left = if (atStart == startIsLeft) -width else size.width
        drawRoundRect(
            color = color,
            topLeft = Offset(left, size.height * top),
            size = Size(width, size.height * height),
            cornerRadius = CornerRadius(width / 2),
        )
    }
    if (tokens.drawsStartButtons(device)) tokens.StartButtons.forEach { (top, height) -> button(true, top, height) }
    button(false, tokens.EndButton.first, tokens.EndButton.second)
}

/** Keeps keyboard focus out of the content of a preview. */
private fun Modifier.noFocus(): Modifier = focusProperties { onEnter = { cancelFocusChange() } }.focusGroup()

/** Receives every touch and does nothing with it, so the content below takes none. */
private fun Modifier.blockTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}

private const val TabularFigures = "tnum"
