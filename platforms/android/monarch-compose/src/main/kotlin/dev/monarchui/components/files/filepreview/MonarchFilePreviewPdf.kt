package dev.monarchui.components.files.filepreview

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp

/**
 * The pages of the PDF at [uri], rendered as they scroll into view, in a
 * column at most [maxHeight] high. Calls [onFailed] when the file cannot be
 * opened as a PDF.
 */
@Composable
internal fun MonarchFilePreviewPdf(
    uri: Uri,
    label: String,
    pageLabel: (number: Int, count: Int) -> String,
    maxHeight: Dp,
    surface: Color,
    onFailed: () -> Unit,
) {
    val context = LocalContext.current
    val document by produceState<MonarchPdfDocument?>(null, uri) {
        val opened = MonarchPdfDocument.open(context, uri)
        if (opened == null) onFailed() else value = opened
        awaitDispose { opened?.close() }
    }
    val current = document
    if (current == null) {
        Box(Modifier.fillMaxWidth().heightIn(max = maxHeight))
        return
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth
        val pageCount = current.pageCount
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .semantics { contentDescription = label },
            verticalArrangement = Arrangement.spacedBy(MonarchFilePreviewTokens.PageGap),
        ) {
            items(pageCount) { index ->
                PdfPage(current, index, widthPx, pageLabel(index + 1, pageCount), maxWidth, surface)
            }
        }
    }
}

@Composable
private fun PdfPage(
    document: MonarchPdfDocument,
    index: Int,
    widthPx: Int,
    label: String,
    width: Dp,
    surface: Color,
) {
    val bitmap by produceState<ImageBitmap?>(null, document, index, widthPx) {
        value = try {
            document.render(index, widthPx)
        } catch (ignored: IllegalStateException) {
            null
        }
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = label,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(width * MonarchFilePreviewTokens.PAGE_PLACEHOLDER_RATIO)
                .background(surface)
                .semantics { contentDescription = label },
        )
    }
}
