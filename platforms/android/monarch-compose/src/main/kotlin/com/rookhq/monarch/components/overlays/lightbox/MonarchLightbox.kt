package com.rookhq.monarch.components.overlays.lightbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rookhq.monarch.theme.MonarchDarkSurface

/**
 * A full-screen viewer for images and videos: swipe or step between items,
 * pinch, double-tap or use the buttons to zoom, with a caption and a
 * position count.
 *
 * Built by Monarch on a full-screen Compose `Dialog`, a `HorizontalPager` and
 * `Modifier.transformable`; Material 3 has no media viewer. Show it while it
 * should be on screen, as with `AlertDialog`:
 *
 * ```
 * if (viewing) MonarchLightbox(items, index, { index = it }, onDismissRequest = { viewing = false })
 * ```
 *
 * The toolbar has text buttons (−, the zoom level, +, Share, Close), because
 * Monarch bundles no icons; previous and next sit under the media. Zoom
 * belongs to one item: showing another item starts it fitted. While zoomed, a
 * one-finger drag pans the image instead of changing the item. With a hardware
 * keyboard the arrow keys step, +, - and 0 zoom and Escape closes.
 *
 * @param items the images and videos, in order. Each has alt text.
 * @param selectedIndex the index of the item on screen, from 0. An index outside the items shows the nearest
 * item; it is reported back only when the person moves.
 * @param onSelectedIndexChange called with the new index when the person swipes or steps.
 * @param onDismissRequest called by the Close button, the back gesture and Escape. Stop showing the viewer in it.
 * @param modifier the modifier of the viewer's surface.
 * @param looping whether Next on the last item goes to the first, and Previous on the first to the last. Swiping
 * does not wrap.
 * @param maximumZoom the largest zoom factor of an image. Values below 1 count as 1, which turns zoom off.
 * @param onShare when not `null`, a Share button calls it with the index of the item on screen; the app starts
 * its share sheet, for example with `Intent.ACTION_SEND`.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchLightbox(
    items: List<MonarchLightboxItem>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    looping: Boolean = false,
    maximumZoom: Float = DefaultMaximumZoom,
    onShare: ((index: Int) -> Unit)? = null,
    labels: MonarchLightboxLabels = MonarchLightboxLabels(),
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // A viewer for photos is dark in every theme: a light canvas around an image reads as a white void.
        MonarchDarkSurface {
            MonarchLightboxContent(
                items = items,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = onSelectedIndexChange,
                onDismissRequest = onDismissRequest,
                modifier = modifier,
                looping = looping,
                maximumZoom = maximumZoom,
                onShare = onShare,
                labels = labels,
            )
        }
    }
}

/** The default largest zoom factor, as on the web. */
private const val DefaultMaximumZoom: Float = 4f
