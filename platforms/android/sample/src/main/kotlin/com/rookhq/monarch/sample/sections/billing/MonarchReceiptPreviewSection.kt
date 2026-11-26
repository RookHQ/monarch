package com.rookhq.monarch.sample.sections.billing

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.ColorPainter
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.billing.receiptpreview.MonarchReceipt
import com.rookhq.monarch.components.billing.receiptpreview.MonarchReceiptPreview
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme
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
