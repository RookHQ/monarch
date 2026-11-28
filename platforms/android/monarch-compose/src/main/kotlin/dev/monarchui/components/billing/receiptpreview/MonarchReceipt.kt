package dev.monarchui.components.billing.receiptpreview

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * A receipt file the app has loaded, ready to show in a `MonarchReceiptPreview`.
 *
 * Monarch does no networking: the app fetches the private file with its own
 * authorisation and gives either a picture or a local PDF.
 *
 * @property name the file name, such as "receipt-1042.jpg". Decides the kind of preview with [contentType].
 * @property contentType the MIME type, such as "image/jpeg" or "application/pdf".
 * @property image the picture of an image receipt, decoded by the app, for example a `BitmapPainter`.
 * @property pdf a `file:` or `content:` URI of a PDF receipt on the device.
 */
@Immutable
public class MonarchReceipt(
    public val name: String,
    public val contentType: String? = null,
    public val image: Painter? = null,
    public val pdf: Uri? = null,
)
