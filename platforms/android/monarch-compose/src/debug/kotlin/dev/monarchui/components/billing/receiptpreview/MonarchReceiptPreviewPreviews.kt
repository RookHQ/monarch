package dev.monarchui.components.billing.receiptpreview

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchReceiptPreviewPreview() {
    PreviewFrame {
        MonarchReceiptPreview(
            receipt = MonarchReceipt(name = "receipt.jpg", contentType = "image/jpeg"),
            icon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        )
        MonarchReceiptPreview(
            load = { MonarchReceipt(name = "receipt.pdf", contentType = "application/pdf") },
            variant = MonarchButtonVariant.Secondary,
        )
        MonarchReceiptPreview(receipt = null)
    }
}
