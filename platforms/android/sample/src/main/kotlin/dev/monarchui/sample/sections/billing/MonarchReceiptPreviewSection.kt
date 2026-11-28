package dev.monarchui.sample.sections.billing

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.ColorPainter
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.billing.receiptpreview.MonarchReceipt
import dev.monarchui.components.billing.receiptpreview.MonarchReceiptPreview
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of ReceiptPreview. */
object MonarchReceiptPreviewSection : CataloguePage {
    override val title = "ReceiptPreview"

    private const val LOAD_MILLIS = 1200L

    @Composable
    override fun Content() {
        val picture = ColorPainter(MonarchTheme.colors.bgMuted)
        CatalogueCaption("A receipt the app already has")
        MonarchReceiptPreview(
            receipt = MonarchReceipt(name = "receipt-1042.jpg", contentType = "image/jpeg", image = picture),
            icon = { MonarchIcon(imageVector = SampleIcons.Document, contentDescription = null) },
        )
        CatalogueCaption("Loaded when pressed")
        MonarchReceiptPreview(
            load = {
                delay(LOAD_MILLIS)
                MonarchReceipt(name = "receipt-1043.jpg", contentType = "image/jpeg", image = picture)
            },
            variant = MonarchButtonVariant.Secondary,
        )
        CatalogueCaption("Loading fails")
        MonarchReceiptPreview(load = {
            delay(LOAD_MILLIS)
            error("The catalogue has no network")
        })
        CatalogueCaption("No receipt")
        MonarchReceiptPreview(receipt = null)
    }
}
