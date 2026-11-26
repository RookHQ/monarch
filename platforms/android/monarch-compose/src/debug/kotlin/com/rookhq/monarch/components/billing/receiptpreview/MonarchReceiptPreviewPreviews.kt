package com.rookhq.monarch.components.billing.receiptpreview

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
