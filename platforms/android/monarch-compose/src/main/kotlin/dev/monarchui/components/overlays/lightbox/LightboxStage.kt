package dev.monarchui.components.overlays.lightbox

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.launch

/**
 * One item of the viewer: the image (zoomable when [zoom] is not `null`),
 * the app's video player, or the unavailable text.
 */
@Composable
internal fun LightboxStage(
    item: MonarchLightboxItem?,
    zoom: LightboxZoomState?,
    labels: ResolvedLightboxLabels,
    modifier: Modifier = Modifier,
) {
    val image = item?.image
    val video = item?.video
    when {
        item != null && video != null -> Box(
            modifier = modifier.fillMaxSize().semantics { contentDescription = item.alt },
            contentAlignment = Alignment.Center,
        ) { video() }
        item != null && image != null -> ZoomableImage(item, zoom, labels, modifier)
        else -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = labels.unavailable,
                modifier = Modifier.padding(MonarchLightboxTokens.Padding),
                style = MonarchTheme.typography.body,
                color = MonarchTheme.colors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ZoomableImage(
    item: MonarchLightboxItem,
    zoom: LightboxZoomState?,
    labels: ResolvedLightboxLabels,
    modifier: Modifier,
) {
    val scope = rememberCoroutineScope()
    val motion = MonarchTheme.motion
    val gestures = if (zoom != null && zoom.zoomable) {
        val transformState = rememberTransformableState { _, zoomChange, pan, _ -> zoom.transform(zoomChange, pan) }
        Modifier
            .pointerInput(zoom) {
                detectTapGestures(
                    onDoubleTap = {
                        val target = MonarchLightboxTokens.doubleTapZoom(zoom.zoom, zoom.limit)
                        scope.launch { zoom.animateTo(target, motion.spec()) }
                    },
                )
            }
            // A one-finger drag pans only while zoomed; otherwise it reaches the pager and changes the item.
            .transformable(transformState, canPan = { zoom.zoom > 1f })
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { zoom?.stageSize = it }
            .then(gestures)
            .clearAndSetSemantics {
                contentDescription = item.alt
                role = Role.Image
                if (zoom != null && zoom.zoomable) {
                    stateDescription = zoomPercent(zoom.zoom)
                    customActions = listOf(
                        CustomAccessibilityAction(labels.zoomIn) {
                            scope.launch { zoom.animateTo(zoom.zoom + MonarchLightboxTokens.ZOOM_STEP, motion.spec()) }
                            zoom.zoom < zoom.limit
                        },
                        CustomAccessibilityAction(labels.zoomOut) {
                            scope.launch { zoom.animateTo(zoom.zoom - MonarchLightboxTokens.ZOOM_STEP, motion.spec()) }
                            zoom.zoom > 1f
                        },
                    )
                }
            },
    ) {
        Image(
            painter = checkNotNull(item.image),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val level = zoom?.zoom ?: 1f
                    scaleX = level
                    scaleY = level
                    translationX = zoom?.offset?.x ?: 0f
                    translationY = zoom?.offset?.y ?: 0f
                },
            contentScale = ContentScale.Fit,
        )
    }
}
