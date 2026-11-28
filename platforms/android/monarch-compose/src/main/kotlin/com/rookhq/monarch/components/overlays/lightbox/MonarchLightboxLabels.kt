package com.rookhq.monarch.components.overlays.lightbox

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchLightbox`. Every `null`
 * keeps the built-in string `monarch_lightbox_<key>`.
 *
 * @property title the spoken name of the viewer: "Media viewer".
 * @property previous the button that shows the previous item: "Previous".
 * @property next the button that shows the next item: "Next".
 * @property zoomIn the spoken name of the zoom-in button: "Zoom in".
 * @property zoomOut the spoken name of the zoom-out button: "Zoom out".
 * @property resetZoom the spoken name of the button that shows the zoom level and resets it: "Reset zoom".
 * @property close the button that closes the viewer: "Close".
 * @property share the button that shares the item, shown when the viewer has `onShare`: "Share".
 * @property position where the item sits, given its number from 1 and the number of items: "2 of 5".
 * @property unavailable shown in place of an item that has no picture and no video: "This file can’t be shown."
 */
@Immutable
public class MonarchLightboxLabels(
    public val title: String? = null,
    public val previous: String? = null,
    public val next: String? = null,
    public val zoomIn: String? = null,
    public val zoomOut: String? = null,
    public val resetZoom: String? = null,
    public val close: String? = null,
    public val share: String? = null,
    public val position: ((number: Int, count: Int) -> String)? = null,
    public val unavailable: String? = null,
)
