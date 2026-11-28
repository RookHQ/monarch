package dev.monarchui.components.technical.jsonviewer

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.sampleicons.SampleIcons

private val payload = MonarchJsonValue.parse(
    """{"id": "evt_1", "type": "invoice.paid", "amount": 4200, "paid": true, "refund": null,
       "lines": [{"sku": "A1", "qty": 2}, {"sku": "B7", "qty": 1}], "customer name": "Ada Lovelace"}""",
)

@Composable
private fun Chevron() {
    MonarchIcon(
        imageVector = SampleIcons.ChevronRight,
        contentDescription = null,
        size = MonarchIconSize.Sm,
    )
}

@MonarchPreviews
@Composable
internal fun MonarchJsonViewerPreview() {
    PreviewFrame {
        MonarchJsonViewer(
            value = payload,
            expandIcon = { Chevron() },
            modifier = Modifier.height(420.dp),
            expandDepth = 2,
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchJsonViewerShowMorePreview() {
    PreviewFrame {
        MonarchJsonViewer(
            value = MonarchJsonValue.from(listOf("a", "b", "c", "d")),
            expandIcon = { Chevron() },
            modifier = Modifier.height(240.dp),
            maxChildren = 2,
            searchable = false,
            copyable = false,
        )
    }
}
