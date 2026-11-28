package dev.monarchui.components.collaboration.readreceipt

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewReaders = listOf(
    MonarchReadReceiptReader(name = "Ana Lima"),
    MonarchReadReceiptReader(name = "Ben Okafor"),
    MonarchReadReceiptReader(name = "Chloé Martin"),
    MonarchReadReceiptReader(name = "Dev Patel"),
    MonarchReadReceiptReader(name = "Eun-ji Park"),
)

@MonarchPreviews
@Composable
internal fun MonarchReadReceiptPreview() {
    PreviewFrame {
        MonarchReadReceipt(readers = PreviewReaders.take(1))
        MonarchReadReceipt(readers = PreviewReaders, display = MonarchReadReceiptDisplay.Count)
        MonarchReadReceipt(readers = PreviewReaders, display = MonarchReadReceiptDisplay.Avatars)
        MonarchReadReceipt(readers = PreviewReaders.take(2), onShowReaders = {})
    }
}
