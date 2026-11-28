package dev.monarchui.components.collaboration.screenshareview

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.toSize
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * Shows a screen someone shares in a call, with who is presenting, fit and
 * zoom (pinch, double tap or the buttons; drag to pan when zoomed), and
 * buttons for picture in picture and full screen.
 *
 * Built by Monarch: Material 3 has no equivalent. The picture is a slot for
 * the view of the app's call SDK. Picture in picture
 * (`Activity.enterPictureInPictureMode`) and full screen belong to the
 * activity, so the app starts them through [onPictureInPicture] and
 * [onFullScreen]; the view shows the buttons and their state. When the person
 * using the app is the presenter ([local]), their own picture is hidden until
 * they ask for it, to avoid a mirror effect, and Stop sharing can be offered.
 * Capturing a screen (MediaProjection) stays in the app.
 *
 * @param presenterName who is sharing, as it should be read.
 * @param zoom the zoom factor, from 1 (fit) to 3. Values outside are clamped.
 * @param onZoomChange called with the new zoom from the buttons, gestures and TalkBack actions.
 * @param modifier the modifier of the screen share view.
 * @param local the person using the app is sharing.
 * @param waiting the shared screen has not arrived: a waiting text replaces the picture.
 * @param pictureInPictureActive switches the picture-in-picture button to its exit form.
 * @param fullScreen switches the full-screen button to its exit form.
 * @param showControls shows the zoom, picture-in-picture and full-screen buttons.
 * @param onPictureInPicture starts or ends picture in picture. Adds the button.
 * @param onFullScreen enters or leaves full screen. Adds the button.
 * @param onStopSharing stops sharing. Adds the button when [local].
 * @param labels replacements for the built-in strings.
 * @param video the shared picture, from the app's call SDK. It fills the 16 by 9 area.
 */
@Composable
public fun MonarchScreenShareView(
    presenterName: String,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    local: Boolean = false,
    waiting: Boolean = false,
    pictureInPictureActive: Boolean = false,
    fullScreen: Boolean = false,
    showControls: Boolean = true,
    onPictureInPicture: (() -> Unit)? = null,
    onFullScreen: (() -> Unit)? = null,
    onStopSharing: (() -> Unit)? = null,
    labels: MonarchScreenShareViewLabels = MonarchScreenShareViewLabels(),
    video: @Composable () -> Unit,
) {
    val tokens = MonarchScreenShareViewTokens
    val level = tokens.clamp(zoom)
    var showOwn by rememberSaveable { mutableStateOf(false) }
    val hidden = local && !showOwn
    val frame = tokens.frameColors(MonarchTheme.colors)
    Column(
        modifier = modifier
            .monarchSurface(frame, MonarchShapeDefaults.Container)
            .clip(MonarchShapeDefaults.Container),
    ) {
        ShareBar(
            presenter = if (local) {
                monarchString(labels.youArePresenting, R.string.monarch_screenshareview_you_are_presenting)
            } else {
                labels.presenting?.invoke(presenterName)
                    ?: stringResource(R.string.monarch_screenshareview_presenting, presenterName)
            },
            zoom = level,
            showZoom = showControls && !hidden && !waiting,
            onZoomChange = onZoomChange,
            pictureInPicture = onPictureInPicture.takeIf { showControls }
                ?.let { PictureButton(pictureInPictureActive, it) },
            fullScreen = onFullScreen.takeIf { showControls }?.let { PictureButton(fullScreen, it) },
            onStopSharing = onStopSharing.takeIf { local },
            labels = labels,
        )
        MonarchDivider()
        if (hidden) {
            LocalNotice(showPreview = if (waiting) null else ({ showOwn = true }), labels = labels)
        } else {
            Viewport(
                presenterName = presenterName,
                zoom = level,
                waiting = waiting,
                onZoomChange = onZoomChange,
                labels = labels,
                video = video,
            )
        }
    }
}

/**
 * A screen share view that keeps its zoom itself, starting at fit. Use the
 * overload with `zoom` and `onZoomChange` when the app needs the zoom.
 *
 * @param presenterName who is sharing, as it should be read.
 * @param modifier the modifier of the screen share view.
 * @param local the person using the app is sharing.
 * @param waiting the shared screen has not arrived.
 * @param pictureInPictureActive switches the picture-in-picture button to its exit form.
 * @param fullScreen switches the full-screen button to its exit form.
 * @param showControls shows the zoom, picture-in-picture and full-screen buttons.
 * @param onPictureInPicture starts or ends picture in picture. Adds the button.
 * @param onFullScreen enters or leaves full screen. Adds the button.
 * @param onStopSharing stops sharing. Adds the button when [local].
 * @param labels replacements for the built-in strings.
 * @param video the shared picture, from the app's call SDK.
 */
@Composable
public fun MonarchScreenShareView(
    presenterName: String,
    modifier: Modifier = Modifier,
    local: Boolean = false,
    waiting: Boolean = false,
    pictureInPictureActive: Boolean = false,
    fullScreen: Boolean = false,
    showControls: Boolean = true,
    onPictureInPicture: (() -> Unit)? = null,
    onFullScreen: (() -> Unit)? = null,
    onStopSharing: (() -> Unit)? = null,
    labels: MonarchScreenShareViewLabels = MonarchScreenShareViewLabels(),
    video: @Composable () -> Unit,
) {
    var zoom by rememberSaveable { mutableFloatStateOf(MonarchScreenShareViewTokens.MIN_ZOOM) }
    MonarchScreenShareView(
        presenterName = presenterName,
        zoom = zoom,
        onZoomChange = { zoom = it },
        modifier = modifier,
        local = local,
        waiting = waiting,
        pictureInPictureActive = pictureInPictureActive,
        fullScreen = fullScreen,
        showControls = showControls,
        onPictureInPicture = onPictureInPicture,
        onFullScreen = onFullScreen,
        onStopSharing = onStopSharing,
        labels = labels,
        video = video,
    )
}

/** A button that toggles a picture mode of the activity, with its current state. */
private class PictureButton(val active: Boolean, val onClick: () -> Unit)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShareBar(
    presenter: String,
    zoom: Float,
    showZoom: Boolean,
    onZoomChange: (Float) -> Unit,
    pictureInPicture: PictureButton?,
    fullScreen: PictureButton?,
    onStopSharing: (() -> Unit)?,
    labels: MonarchScreenShareViewLabels,
) {
    val tokens = MonarchScreenShareViewTokens
    val ghost = MonarchButtonVariant.Ghost
    val small = MonarchButtonSize.Sm
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(tokens.BarPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = tokens.BarPadding),
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.clearAndSetSemantics { }) { MonarchDefaultIcons.Cast() }
            Text(
                text = presenter,
                style = MonarchTheme.typography.label,
                color = MonarchTheme.colors.textDefault,
            )
        }
        if (showZoom) {
            MonarchIconButton(
                label = monarchString(labels.zoomOut, R.string.monarch_screenshareview_zoom_out),
                onClick = { tokens.zoomOut(zoom)?.let(onZoomChange) },
                variant = ghost,
                size = small,
                enabled = tokens.zoomOut(zoom) != null,
            ) { MonarchDefaultIcons.Dash() }
            Text(
                text = labels.zoomLevel?.invoke(tokens.percent(zoom)) ?: tokens.percentText(zoom),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MonarchTheme.typography.label,
                color = MonarchTheme.colors.textMuted,
            )
            MonarchIconButton(
                label = monarchString(labels.zoomIn, R.string.monarch_screenshareview_zoom_in),
                onClick = { tokens.zoomIn(zoom)?.let(onZoomChange) },
                variant = ghost,
                size = small,
                enabled = tokens.zoomIn(zoom) != null,
            ) { MonarchDefaultIcons.Plus() }
            MonarchIconButton(
                label = monarchString(labels.fit, R.string.monarch_screenshareview_fit),
                onClick = { onZoomChange(tokens.MIN_ZOOM) },
                variant = ghost,
                size = small,
                enabled = zoom > tokens.MIN_ZOOM,
            ) { MonarchDefaultIcons.Expand() }
        }
        if (pictureInPicture != null) {
            MonarchButton(
                text = if (pictureInPicture.active) {
                    monarchString(labels.exitPictureInPicture, R.string.monarch_screenshareview_exit_picture_in_picture)
                } else {
                    monarchString(labels.pictureInPicture, R.string.monarch_screenshareview_picture_in_picture)
                },
                onClick = pictureInPicture.onClick,
                modifier = Modifier.semantics { selected = pictureInPicture.active },
                variant = ghost,
                size = small,
            )
        }
        if (fullScreen != null) {
            MonarchIconButton(
                label = if (fullScreen.active) {
                    monarchString(labels.exitFullScreen, R.string.monarch_screenshareview_exit_full_screen)
                } else {
                    monarchString(labels.fullScreen, R.string.monarch_screenshareview_full_screen)
                },
                onClick = fullScreen.onClick,
                modifier = Modifier.semantics { selected = fullScreen.active },
                variant = ghost,
                size = small,
            ) { MonarchDefaultIcons.Fullscreen() }
        }
        if (onStopSharing != null) {
            MonarchButton(
                text = monarchString(labels.stopSharing, R.string.monarch_screenshareview_stop_sharing),
                onClick = onStopSharing,
                variant = MonarchButtonVariant.Danger,
                size = small,
            )
        }
    }
}

@Composable
private fun Viewport(
    presenterName: String,
    zoom: Float,
    waiting: Boolean,
    onZoomChange: (Float) -> Unit,
    labels: MonarchScreenShareViewLabels,
    video: @Composable () -> Unit,
) {
    val tokens = MonarchScreenShareViewTokens
    val colors = tokens.viewportColors(MonarchTheme.colors)
    var size by remember { mutableStateOf(Size.Zero) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val offset = tokens.clampOffset(pan, size, zoom)
    val currentZoom by rememberUpdatedState(zoom)
    val changeZoom by rememberUpdatedState(onZoomChange)
    val transform = rememberTransformableState { _, zoomChange, panChange, _ ->
        val next = tokens.clamp(currentZoom * zoomChange)
        if (next != currentZoom) changeZoom(next)
        pan = tokens.clampOffset(pan + panChange, size, next)
    }
    val region = labels.region?.invoke(presenterName)
        ?: stringResource(R.string.monarch_screenshareview_region, presenterName)
    val levelText = labels.zoomLevel?.invoke(tokens.percent(zoom)) ?: tokens.percentText(zoom)
    val zoomInLabel = monarchString(labels.zoomIn, R.string.monarch_screenshareview_zoom_in)
    val zoomOutLabel = monarchString(labels.zoomOut, R.string.monarch_screenshareview_zoom_out)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(tokens.ASPECT_RATIO)
            .background(colors.container)
            .clipToBounds()
            .onSizeChanged { size = it.toSize() },
        contentAlignment = Alignment.Center,
    ) {
        if (waiting) {
            Text(
                text = monarchString(labels.waiting, R.string.monarch_screenshareview_waiting),
                modifier = Modifier.padding(tokens.NoticePadding),
                style = MonarchTheme.typography.body,
                color = colors.content,
                textAlign = TextAlign.Center,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {
                        contentDescription = region
                        role = Role.Image
                        stateDescription = levelText
                        customActions = listOfNotNull(
                            tokens.zoomIn(zoom)?.let { next ->
                                CustomAccessibilityAction(zoomInLabel) {
                                    onZoomChange(next)
                                    true
                                }
                            },
                            tokens.zoomOut(zoom)?.let { next ->
                                CustomAccessibilityAction(zoomOutLabel) {
                                    onZoomChange(next)
                                    true
                                }
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { changeZoom(tokens.doubleTap(currentZoom)) })
                    }
                    .transformable(state = transform, canPan = { currentZoom > tokens.MIN_ZOOM })
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = offset.x
                        translationY = offset.y
                    },
            ) { video() }
        }
    }
}

@Composable
private fun LocalNotice(showPreview: (() -> Unit)?, labels: MonarchScreenShareViewLabels) {
    val tokens = MonarchScreenShareViewTokens
    val colors = tokens.noticeColors(MonarchTheme.colors)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.container)
            .padding(tokens.NoticePadding),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = monarchString(labels.localPreviewHidden, R.string.monarch_screenshareview_local_preview_hidden),
            style = MonarchTheme.typography.body,
            color = colors.content,
            textAlign = TextAlign.Center,
        )
        if (showPreview != null) {
            MonarchButton(
                text = monarchString(labels.showPreview, R.string.monarch_screenshareview_show_preview),
                onClick = showPreview,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}
