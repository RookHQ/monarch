package com.rookhq.monarch.components.overlays.lightbox

import android.content.res.Resources
import androidx.compose.runtime.Immutable
import com.rookhq.monarch.R

/** The built-in strings of the viewer with the caller's replacements applied. */
@Immutable
internal class ResolvedLightboxLabels(private val labels: MonarchLightboxLabels, private val resources: Resources) {
    val title: String get() = labels.title ?: resources.getString(R.string.monarch_lightbox_title)
    val previous: String get() = labels.previous ?: resources.getString(R.string.monarch_lightbox_previous)
    val next: String get() = labels.next ?: resources.getString(R.string.monarch_lightbox_next)
    val zoomIn: String get() = labels.zoomIn ?: resources.getString(R.string.monarch_lightbox_zoom_in)
    val zoomOut: String get() = labels.zoomOut ?: resources.getString(R.string.monarch_lightbox_zoom_out)
    val resetZoom: String get() = labels.resetZoom ?: resources.getString(R.string.monarch_lightbox_reset_zoom)
    val close: String get() = labels.close ?: resources.getString(R.string.monarch_lightbox_close)
    val share: String get() = labels.share ?: resources.getString(R.string.monarch_lightbox_share)
    val unavailable: String get() = labels.unavailable ?: resources.getString(R.string.monarch_lightbox_unavailable)

    /** Where the item sits, given its number from 1 and the number of items. */
    fun position(number: Int, count: Int): String =
        labels.position?.invoke(number, count) ?: resources.getString(R.string.monarch_lightbox_position, number, count)
}
