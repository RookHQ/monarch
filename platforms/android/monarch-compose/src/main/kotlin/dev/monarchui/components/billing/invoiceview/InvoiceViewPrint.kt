package dev.monarchui.components.billing.invoiceview

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import dev.monarchui.theme.MonarchContrast
import dev.monarchui.theme.MonarchScheme
import dev.monarchui.theme.MonarchTheme
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.ceil

/**
 * Prints an invoice: on request it composes a copy of the invoice off screen,
 * in the light theme and without the header actions, records it into a
 * graphics layer, and hands the picture to the system print dialog, which can
 * also save it as a PDF.
 */
internal class InvoiceViewPrinter(val layer: GraphicsLayer) {
    /** The off-screen copy is composed and waiting to be captured. */
    var printing: Boolean by mutableStateOf(false)
}

/** A printer for one invoice; [jobName] names the print job and the PDF. */
@Composable
internal fun rememberInvoiceViewPrinter(jobName: String): InvoiceViewPrinter {
    val layer = rememberGraphicsLayer()
    val context = LocalContext.current
    val name by rememberUpdatedState(jobName)
    val printer = remember(layer) { InvoiceViewPrinter(layer) }
    LaunchedEffect(printer.printing) {
        if (!printer.printing) return@LaunchedEffect
        // One frame to lay out and draw the copy, one more so the recording is complete.
        withFrameNanos { }
        withFrameNanos { }
        val picture = layer.toImageBitmap().asAndroidBitmap()
        // A hardware bitmap cannot be drawn on the software canvas of a PDF page.
        val bitmap = picture.copy(Bitmap.Config.ARGB_8888, false)
        printer.printing = false
        InvoiceViewPrinting.print(context, bitmap, name)
    }
    return printer
}

/**
 * The off-screen copy that is printed. It takes no space, is not drawn on
 * screen and is hidden from TalkBack; it exists only while printing.
 */
@Composable
internal fun InvoiceViewPrintCopy(printer: InvoiceViewPrinter, content: @Composable () -> Unit) {
    if (!printer.printing) return
    Box(
        Modifier
            .clearAndSetSemantics { }
            .layout { measurable, _ ->
                val width = MonarchInvoiceViewTokens.PrintWidth.roundToPx()
                val placeable = measurable.measure(Constraints(minWidth = width, maxWidth = width))
                layout(0, 0) { placeable.place(0, 0) }
            },
    ) {
        Box(Modifier.drawWithContent { printer.layer.record { this@drawWithContent.drawContent() } }) {
            MonarchTheme(scheme = MonarchScheme.Light, contrast = MonarchContrast.Standard) {
                Surface(color = MonarchTheme.colors.bgSurface, contentColor = MonarchTheme.colors.textDefault) {
                    Box(Modifier.padding(MonarchInvoiceViewTokens.PrintPadding)) { content() }
                }
            }
        }
    }
}

/** Sending a captured invoice to the system print service. */
internal object InvoiceViewPrinting {
    private const val POINTS_PER_INCH = 72f
    private const val MILS_PER_INCH = 1000f

    /** How many pages a picture [imageWidth] by [imageHeight] fills when scaled to the page's width. */
    fun pageCount(imageWidth: Int, imageHeight: Int, pageWidth: Float, pageHeight: Float): Int {
        val emptyImage = imageWidth <= 0 || imageHeight <= 0
        if (emptyImage || pageWidth <= 0f || pageHeight <= 0f) return 1
        val slice = pageHeight * imageWidth / pageWidth
        return ceil(imageHeight / slice).toInt().coerceAtLeast(1)
    }

    /** Opens the system print dialog for [bitmap]; does nothing outside an activity. */
    fun print(context: Context, bitmap: Bitmap, name: String) {
        val activity = context.findActivity() ?: return
        val manager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        manager.print(name, Adapter(activity, bitmap, name), null)
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private fun milsToPoints(mils: Int): Float = mils * POINTS_PER_INCH / MILS_PER_INCH

    private class Adapter(
        private val context: Context,
        private val bitmap: Bitmap,
        private val name: String,
    ) : PrintDocumentAdapter() {
        private var attributes: PrintAttributes? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            attributes = newAttributes
            val media = newAttributes.mediaSize ?: PrintAttributes.MediaSize.ISO_A4
            val margins = newAttributes.minMargins ?: PrintAttributes.Margins.NO_MARGINS
            val width = milsToPoints(media.widthMils - margins.leftMils - margins.rightMils)
            val height = milsToPoints(media.heightMils - margins.topMils - margins.bottomMils)
            val info = PrintDocumentInfo.Builder("$name.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pageCount(bitmap.width, bitmap.height, width, height))
                .build()
            callback.onLayoutFinished(info, newAttributes != oldAttributes)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback,
        ) {
            val document = PrintedPdfDocument(context, attributes ?: return callback.onWriteFailed(null))
            try {
                var index = 0
                var count = 1
                while (index < count) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        return
                    }
                    val page = document.startPage(index)
                    val area = page.info.contentRect
                    count = pageCount(bitmap.width, bitmap.height, area.width().toFloat(), area.height().toFloat())
                    val scale = area.width().toFloat() / bitmap.width
                    page.canvas.apply {
                        clipRect(area)
                        translate(area.left.toFloat(), area.top.toFloat())
                        scale(scale, scale)
                        translate(0f, -index * area.height() / scale)
                        drawBitmap(bitmap, 0f, 0f, null)
                    }
                    document.finishPage(page)
                    index++
                }
                FileOutputStream(destination.fileDescriptor).use { document.writeTo(it) }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (failure: IOException) {
                callback.onWriteFailed(failure.message)
            } finally {
                document.close()
            }
        }
    }
}
