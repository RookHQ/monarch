package com.rookhq.monarch.components.files.avatarupload

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.forms.slider.MonarchSlider
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarShape
import com.rookhq.monarch.theme.MonarchTheme
import kotlin.math.roundToInt

/**
 * The crop editor of a `MonarchAvatarUpload`: the picture in a square area
 * masked to the avatar's shape, moved by dragging, zoomed by pinching or the
 * slider, and moved and zoomed by keys and TalkBack actions.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun AvatarUploadEditor(
    image: ImageBitmap,
    view: AvatarUploadView,
    onViewChange: (AvatarUploadView) -> Unit,
    shape: MonarchAvatarShape,
    saving: Boolean,
    enabled: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    strings: AvatarUploadEditorStrings,
    focusRequester: FocusRequester,
) {
    val tokens = MonarchAvatarUploadTokens
    val colors = MonarchTheme.colors
    val locked = saving || !enabled
    val current by rememberUpdatedState(view)
    val update: (AvatarUploadView) -> Unit = { next ->
        if (!locked) onViewChange(tokens.clamp(image.width, image.height, next))
    }
    var areaSide by remember { mutableFloatStateOf(1f) }
    val transform = rememberTransformableState { _: Offset, zoomChange: Float, pan: Offset, _: Float ->
        val v = current
        update(v.copy(zoom = v.zoom * zoomChange, x = v.x + pan.x / areaSide, y = v.y + pan.y / areaSide))
    }
    val move: (Float, Float) -> Boolean = { dx, dy ->
        update(current.copy(x = current.x + dx, y = current.y + dy))
        true
    }
    val zoomBy: (Float) -> Boolean = { dz ->
        update(current.copy(zoom = current.zoom + dz))
        true
    }
    val zoomText = strings.zoomValue(tokens.percent(view.zoom))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            modifier = Modifier
                .widthIn(max = tokens.CropAreaMax)
                .fillMaxWidth()
                .aspectRatio(1f)
                .onSizeChanged { areaSide = it.width.coerceAtLeast(1).toFloat() }
                .monarchFocusRing(MonarchTheme.shapes.md)
                .focusRequester(focusRequester)
                .onKeyEvent { event ->
                    event.type == KeyEventType.KeyDown && !locked &&
                        avatarUploadKey(event.key, move, zoomBy, { update(AvatarUploadView()) }, onSave, onCancel)
                }
                .focusable()
                .transformable(transform, enabled = !locked)
                .semantics {
                    contentDescription = strings.cropArea
                    stateDescription = zoomText
                    if (!locked) {
                        customActions = listOf(
                            CustomAccessibilityAction(strings.moveLeft) { move(-tokens.MOVE_STEP, 0f) },
                            CustomAccessibilityAction(strings.moveRight) { move(tokens.MOVE_STEP, 0f) },
                            CustomAccessibilityAction(strings.moveUp) { move(0f, -tokens.MOVE_STEP) },
                            CustomAccessibilityAction(strings.moveDown) { move(0f, tokens.MOVE_STEP) },
                            CustomAccessibilityAction(strings.zoomIn) { zoomBy(tokens.ZOOM_STEP) },
                            CustomAccessibilityAction(strings.zoomOut) { zoomBy(-tokens.ZOOM_STEP) },
                        )
                    }
                },
        ) {
            drawAvatarUploadCrop(image, view, shape, tokens.maskColor(colors), tokens.ringColor(colors))
        }
        Text(text = strings.cropHint, style = MonarchTheme.typography.caption, color = tokens.hintColor(colors))
        Row(
            modifier = Modifier.widthIn(max = tokens.CropAreaMax).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.ButtonGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = strings.zoom, style = MonarchTheme.typography.label, color = colors.textDefault)
            MonarchSlider(
                value = view.zoom,
                onValueChange = { update(current.copy(zoom = it)) },
                modifier = Modifier.weight(1f).semantics {
                    contentDescription = strings.zoom
                    stateDescription = zoomText
                },
                enabled = !locked,
                valueRange = tokens.MIN_ZOOM..tokens.MAX_ZOOM,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.ButtonGap)) {
            MonarchButton(
                text = strings.save,
                onClick = onSave,
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Sm,
                enabled = enabled,
                loading = saving,
            )
            MonarchButton(
                text = strings.cancel,
                onClick = onCancel,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = !saving,
            )
        }
    }
}

/** Handles a key of the crop area: arrows move, plus and minus zoom, Home resets, Enter saves, Escape cancels. */
@Suppress("LongParameterList")
private fun avatarUploadKey(
    key: Key,
    move: (Float, Float) -> Boolean,
    zoomBy: (Float) -> Boolean,
    reset: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
): Boolean {
    val step = MonarchAvatarUploadTokens.MOVE_STEP
    val zoom = MonarchAvatarUploadTokens.ZOOM_STEP
    return when (key) {
        Key.DirectionLeft -> move(-step, 0f)
        Key.DirectionRight -> move(step, 0f)
        Key.DirectionUp -> move(0f, -step)
        Key.DirectionDown -> move(0f, step)
        Key.Plus, Key.Equals, Key.NumPadAdd -> zoomBy(zoom)
        Key.Minus, Key.NumPadSubtract -> zoomBy(-zoom)
        Key.MoveHome -> true.also { reset() }
        Key.Enter, Key.NumPadEnter -> true.also { onSave() }
        Key.Escape -> true.also { onCancel() }
        else -> false
    }
}

/** Draws the picture at [view], hides what falls outside the kept shape and rings the shape. */
private fun DrawScope.drawAvatarUploadCrop(
    image: ImageBitmap,
    view: AvatarUploadView,
    shape: MonarchAvatarShape,
    mask: Color,
    ring: Color,
) {
    val box = MonarchAvatarUploadTokens.imageBox(image.width, image.height, view, size.width)
    drawImage(
        image = image,
        dstOffset = IntOffset(box.left.roundToInt(), box.top.roundToInt()),
        dstSize = IntSize(box.width.roundToInt(), box.height.roundToInt()),
    )
    val circle = shape == MonarchAvatarShape.Circle
    val kept = Path().apply { if (circle) addOval(Rect(Offset.Zero, size)) else addRect(Rect(Offset.Zero, size)) }
    // Outside the kept shape the canvas colour hides the picture: no dimming with alpha.
    clipPath(kept, clipOp = ClipOp.Difference) { drawRect(mask) }
    val width = MonarchAvatarUploadTokens.RingWidth.toPx()
    val inset = Offset(width / 2, width / 2)
    val ringSize = Size(size.width - width, size.height - width)
    if (circle) {
        drawOval(ring, inset, ringSize, style = Stroke(width))
    } else {
        drawRect(ring, inset, ringSize, style = Stroke(width))
    }
}
