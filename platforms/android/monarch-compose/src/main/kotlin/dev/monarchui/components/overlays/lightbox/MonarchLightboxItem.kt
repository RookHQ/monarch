package dev.monarchui.components.overlays.lightbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One image or video in a `MonarchLightbox`.
 *
 * Monarch loads nothing: the app passes a [Painter] it has loaded with its
 * own pipeline (a resource, Coil's `rememberAsyncImagePainter`), or composes
 * its own video player in [video]. While [image] is `null` and there is no
 * [video], the viewer shows "This file can't be shown."
 *
 * @property image the picture, or `null` while it is not available.
 * @property alt what the picture or video shows, for people who cannot see it: "Whiteboard sketch of the checkout
 * flow". Required; TalkBack reads it as the name of the item.
 * @property caption a visible caption under the media, or `null`.
 * @property video the app's video player (for example a Media3 `PlayerSurface` with its controls), composed in place
 * of [image]. A video cannot be zoomed.
 */
@Stable
public class MonarchLightboxItem(
    public val image: Painter?,
    public val alt: String,
    public val caption: String? = null,
    public val video: (@Composable () -> Unit)? = null,
)
