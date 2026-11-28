package com.rookhq.monarch.components.intelligence.aicomputerview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import com.rookhq.monarch.components.overlays.lightbox.MonarchLightbox
import com.rookhq.monarch.components.overlays.lightbox.MonarchLightboxItem
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The window title and address, the screen box with the screenshot (or the
 * live picture) and its markers, and the capture time. A tap on a shown
 * screenshot opens it in `MonarchLightbox` to zoom.
 */
@Composable
internal fun AiComputerViewScreen(
    frame: MonarchAiComputerFrame?,
    actions: List<MonarchAiComputerAction>,
    currentUrl: String?,
    windowTitle: String?,
    pointer: MonarchAiComputerPoint?,
    selectedActionId: String?,
    live: (@Composable () -> Unit)?,
    liveSensitive: Boolean,
    texts: AiComputerViewTexts,
    clock: AiComputerViewClock,
    agentActing: Boolean,
) {
    val tokens = MonarchAiComputerViewTokens
    val picture = aiComputerViewPicture(frame, actions.lastOrNull(), live != null, liveSensitive, texts)
    val image = picture.image
    var zoomed by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
        AiComputerViewChrome(windowTitle, currentUrl, texts)
        AiComputerViewFrameBox(
            picture = picture,
            aspectRatio = AiComputerViewContent.aspectRatio(frame),
            onZoom = { zoomed = true },
            viewLarger = texts.viewLarger,
            agentActing = agentActing,
        ) {
            if (picture.showLive) {
                Box(Modifier.matchParentSize().clearAndSetSemantics { }) { live?.invoke() }
            }
            if (picture.showLive || image != null) {
                AiComputerViewMarkers(frame, actions, pointer, selectedActionId, MonarchTheme.colors)
            }
        }
        val caption = when {
            picture.showLive -> texts.live
            frame != null && image != null -> texts.capturedAt(clock.time(frame.capturedAt))
            else -> null
        }
        if (caption != null) {
            Text(
                text = caption,
                style = MonarchTheme.typography.caption,
                color = tokens.mutedColor(MonarchTheme.colors),
            )
        }
    }
    if (zoomed && image != null) {
        MonarchLightbox(
            items = listOf(MonarchLightboxItem(image = image, alt = picture.alt)),
            selectedIndex = 0,
            onSelectedIndexChange = {},
            onDismissRequest = { zoomed = false },
        )
    }
}

/** The window title, and the address as left-to-right text read after "Address". */
@Composable
private fun AiComputerViewChrome(windowTitle: String?, currentUrl: String?, texts: AiComputerViewTexts) {
    val tokens = MonarchAiComputerViewTokens
    val colors = MonarchTheme.colors
    if (!windowTitle.isNullOrBlank()) {
        Text(text = windowTitle, style = MonarchTheme.typography.label, color = tokens.textColor(colors))
    }
    if (!currentUrl.isNullOrBlank()) {
        val shown = AiComputerViewContent.displayUrl(currentUrl)
        Text(
            text = shown,
            modifier = Modifier.semantics { contentDescription = "${texts.address}: $shown" },
            style = MonarchTheme.typography.caption.copy(textDirection = TextDirection.Ltr),
            color = tokens.mutedColor(colors),
        )
    }
}

/** The screen box: one image element that opens larger when a screenshot shows. */
@Composable
private fun AiComputerViewFrameBox(
    picture: AiComputerViewPicture,
    aspectRatio: Float,
    onZoom: () -> Unit,
    viewLarger: String,
    agentActing: Boolean,
    overlay: @Composable BoxScope.() -> Unit,
) {
    val tokens = MonarchAiComputerViewTokens
    val colors = MonarchTheme.colors
    val shape = MonarchShapeDefaults.Container
    val image = picture.image
    val zoom = if (image != null) Modifier.clickable(onClickLabel = viewLarger, onClick = onZoom) else Modifier
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(shape)
            .background(colors.bgMuted)
            .border(MonarchShapeDefaults.BorderWidth, tokens.frameBorder(agentActing, colors), shape)
            .then(zoom)
            .semantics {
                role = Role.Image
                contentDescription = picture.spoken
            },
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                painter = image,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds,
            )
        } else if (!picture.showLive) {
            Text(
                text = picture.placeholder.orEmpty(),
                modifier = Modifier.padding(tokens.PlaceholderPadding).clearAndSetSemantics { },
                style = MonarchTheme.typography.caption,
                color = tokens.mutedColor(colors),
            )
        }
        overlay()
    }
}

/**
 * The selected action, the newest action with a point and the agent's
 * pointer, drawn over the picture at their screenshot positions. They are
 * decorative: the log and the screen's description say the same in words.
 */
@Composable
private fun BoxScope.AiComputerViewMarkers(
    frame: MonarchAiComputerFrame?,
    actions: List<MonarchAiComputerAction>,
    pointer: MonarchAiComputerPoint?,
    selectedActionId: String?,
    colors: MonarchColors,
) {
    val tokens = MonarchAiComputerViewTokens
    val width = frame?.width ?: 0
    val height = frame?.height ?: 0
    val selected = actions.firstOrNull { it.id == selectedActionId }
    val last = AiComputerViewContent.lastPointAction(actions)?.takeIf { it.id != selected?.id }
    val selectedAt = AiComputerViewContent.position(selected?.point, width, height)
    val lastAt = AiComputerViewContent.position(last?.point, width, height)
    val pointerAt = AiComputerViewContent.position(pointer, width, height)
    if (selectedAt == null && lastAt == null && pointerAt == null) return
    Canvas(modifier = Modifier.matchParentSize().clearAndSetSemantics { }) {
        // Physical on purpose: screenshot pixels count from the left edge in every layout direction.
        val marks = tokens.markerColors(colors)
        val radius = tokens.MarkerRadius.toPx()
        val stroke = Stroke(width = tokens.MarkerStroke.toPx())
        lastAt?.let {
            drawCircle(marks.last, radius, at(it), style = stroke)
        }
        selectedAt?.let {
            drawCircle(marks.selected, radius, at(it), style = stroke)
            drawCircle(marks.selected, tokens.PointerRadius.toPx(), at(it))
        }
        pointerAt?.let {
            drawCircle(marks.pointerOutline, tokens.PointerRadius.toPx() + stroke.width, at(it))
            drawCircle(marks.pointer, tokens.PointerRadius.toPx(), at(it))
        }
    }
}

private fun DrawScope.at(fraction: Offset): Offset = Offset(fraction.x * size.width, fraction.y * size.height)
