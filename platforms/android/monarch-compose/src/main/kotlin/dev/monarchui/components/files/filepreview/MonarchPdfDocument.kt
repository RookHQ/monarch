package dev.monarchui.components.files.filepreview

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.IOException

/** An open PDF. `PdfRenderer` renders one page at a time, so rendering is serialised. */
internal class MonarchPdfDocument private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) : Closeable {
    private val mutex = Mutex()

    /** The number of pages. */
    val pageCount: Int get() = renderer.pageCount

    /**
     * Renders page [index] at [widthPx] wide. A PDF page is paper: it is
     * drawn on white, whatever the theme, like every PDF viewer does.
     */
    suspend fun render(index: Int, widthPx: Int): ImageBitmap = mutex.withLock {
        withContext(Dispatchers.IO) {
            renderer.openPage(index).use { page ->
                val height = (widthPx.toLong() * page.height / page.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
                val bitmap = createBitmap(widthPx.coerceAtLeast(1), height)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap.asImageBitmap()
            }
        }
    }

    override fun close() {
        renderer.close()
        descriptor.close()
    }

    companion object {
        /** Opens [uri], or returns `null` when it cannot be read as a PDF. */
        suspend fun open(context: Context, uri: Uri): MonarchPdfDocument? = withContext(Dispatchers.IO) {
            val descriptor = try {
                context.contentResolver.openFileDescriptor(uri, "r")
            } catch (ignored: IOException) {
                null
            } catch (ignored: SecurityException) {
                null
            } ?: return@withContext null
            try {
                MonarchPdfDocument(descriptor, PdfRenderer(descriptor))
            } catch (ignored: IOException) {
                descriptor.close()
                null
            } catch (ignored: SecurityException) {
                descriptor.close()
                null
            }
        }
    }
}
