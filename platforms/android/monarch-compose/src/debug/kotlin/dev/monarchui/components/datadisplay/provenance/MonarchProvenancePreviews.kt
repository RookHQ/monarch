package dev.monarchui.components.datadisplay.provenance

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchProvenancePreview() {
    val twoHoursAgo = System.currentTimeMillis() - 2 * DateUtils.HOUR_IN_MILLIS
    PreviewFrame {
        MonarchProvenanceVariant.entries.forEach { variant ->
            MonarchProvenance(source = "Registry", variant = variant, checkedAt = twoHoursAgo)
            MonarchProvenance(
                source = "Imported file",
                variant = variant,
                icon = { MonarchIcon(PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
            )
        }
        MonarchProvenance(source = "Companies House", checkedAt = twoHoursAgo, onClick = {})
    }
}
