package com.rookhq.monarch.components.datadisplay.provenance

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
