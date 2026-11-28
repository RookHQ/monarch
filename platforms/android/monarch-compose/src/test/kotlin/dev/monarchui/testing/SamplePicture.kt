package dev.monarchui.testing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.geometry.Size
import dev.monarchui.theme.MonarchTheme
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale

private val Sky = Color(0xFF5B9BD5)
private val Hill = Color(0xFF4F8A4B)
private val Sun = Color(0xFFF2C14E)

/** A small picture of a sky, a sun and a hill, so that a fixture with a photo shows something in an image. */
internal fun samplePicture(): ImageBitmap {
    val bitmap = ImageBitmap(PictureSide, PictureSide)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    val side = PictureSide.toFloat()
    paint.color = Sky
    canvas.drawRect(0f, 0f, side, side, paint)
    paint.color = Sun
    canvas.drawCircle(Offset(side * 0.7f, side * 0.3f), side * 0.16f, paint)
    paint.color = Hill
    canvas.drawCircle(Offset(side * 0.3f, side * 1.05f), side * 0.6f, paint)
    return bitmap
}

/** A flat blue-grey picture, the plain placeholder the web's image examples draw. */
internal fun sampleFlatPainter(): BitmapPainter {
    val bitmap = ImageBitmap(PictureSide, PictureSide)
    val paint = Paint().apply { color = Color(0xFF94A3BD) }
    val side = PictureSide.toFloat()
    Canvas(bitmap).drawRect(0f, 0f, side, side, paint)
    return BitmapPainter(bitmap)
}

/** [samplePicture] as a painter. */
internal fun samplePainter(): BitmapPainter = BitmapPainter(samplePicture())

/**
 * A glyph as a logo painter in the theme's text colour. A slot that draws a painter as it is (a card network
 * logo) shows a vector icon in black, which disappears on the dark themes; this one follows the theme.
 */
@Composable
internal fun sampleLogoPainter(icon: ImageVector): Painter {
    val base = rememberVectorPainter(icon)
    val color = MonarchTheme.colors.textDefault
    return androidx.compose.runtime.remember(base, color) { TintedPainter(base, color) }
}

private class TintedPainter(private val base: Painter, private val color: Color) : Painter() {
    override val intrinsicSize: Size get() = base.intrinsicSize

    override fun DrawScope.onDraw() {
        with(base) { draw(size, 1f, ColorFilter.tint(color)) }
    }
}

/** The picture filling its parent, for a slot that takes a composable instead of a painter. */
@Composable
internal fun SamplePictureFill(modifier: Modifier = Modifier) {
    Image(
        painter = samplePainter(),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}

private const val PictureSide = 64

/** The web's thumbnail art: a blue-to-green gradient with a pale sun and hills. */
internal fun sampleThumbnailPainter(): Painter = ScenePainter(teal = true)

/** The web's image placeholder: grey with a darker sun and mountains. */
internal fun samplePlaceholderPainter(): Painter = ScenePainter(teal = false)

private class ScenePainter(private val teal: Boolean) : Painter() {
    override val intrinsicSize: Size get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height
        if (teal) {
            drawRect(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(Color(0xFF4A86B8), Color(0xFF2E9A68)),
                    start = Offset.Zero,
                    end = Offset(w, h),
                ),
            )
        } else {
            drawRect(Color(0xFFDDDDDD))
        }
        drawCircle(
            color = if (teal) Color.White.copy(alpha = 0.4f) else Color(0xFFBFBFBF),
            radius = h * 0.17f,
            center = Offset(w * 0.84f, h * 0.3f),
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.9f)
            lineTo(w * 0.31f, h * 0.48f)
            lineTo(w * 0.53f, h * 0.83f)
            lineTo(w * 0.71f, h * 0.61f)
            lineTo(w, h * 0.92f)
            lineTo(w, h)
            close()
        }
        drawPath(path, if (teal) Color.White.copy(alpha = 0.55f) else Color(0xFFAAAAAA))
    }
}

/** A stand-in QR code as the web draws it: three finder squares and a few modules, on white. */
internal fun sampleQrPainter(): Painter = QrPainter()

private class QrPainter : Painter() {
    override val intrinsicSize: Size get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        val w = size.width
        val unit = w / 10f
        drawRect(Color.White)
        val ink = Color(0xFF16181D)
        fun finder(x: Float, y: Float) {
            drawRect(ink, Offset(x * unit, y * unit), Size(3f * unit, 3f * unit))
            drawRect(Color.White, Offset((x + 0.5f) * unit, (y + 0.5f) * unit), Size(2f * unit, 2f * unit))
            drawRect(ink, Offset((x + 1f) * unit, (y + 1f) * unit), Size(unit, unit))
        }
        finder(0.5f, 0.5f)
        finder(6.5f, 0.5f)
        finder(0.5f, 6.5f)
        listOf(
            5f to 3f, 4f to 4.5f, 6f to 4f, 3.5f to 5.5f, 5.5f to 6.5f, 7.5f to 5.5f, 4.5f to 7.5f,
        ).forEach { (x, y) ->
            drawRect(ink, Offset(x * unit, y * unit), Size(0.7f * unit, 0.7f * unit))
        }
    }
}

/** The receipt of the web example: a few monospaced lines on white. */
internal fun sampleReceiptPainter(): Painter = ReceiptPainter()

private class ReceiptPainter : Painter() {
    override val intrinsicSize: Size get() = Size(600f, 340f)

    override fun DrawScope.onDraw() {
        drawRect(Color.White)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            color = 0xFF16181D.toInt()
            textSize = size.height * 0.052f
        }
        val x = size.width * 0.35f
        val lines = listOf("SAMPLE STORE", "", "Coffee 4.50", "Sandwich 8.25", "", "TOTAL 12.75")
        drawIntoCanvas { canvas ->
            lines.forEachIndexed { i, line ->
                if (line.isNotEmpty()) canvas.nativeCanvas.drawText(line, x, size.height * (0.18f + 0.1f * i), paint)
            }
        }
    }
}

/** The web's file preview picture: a pale sky, a yellow sun and green mountains, at 16:9. */
internal fun sampleLandscapePainter(): Painter = LandscapePainter()

private class LandscapePainter : Painter() {
    override val intrinsicSize: Size get() = Size(480f, 270f)

    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height
        drawRect(Color(0xFFAEC4DF))
        drawCircle(Color(0xFFFFD700), radius = h * 0.13f, center = Offset(w * 0.78f, h * 0.2f))
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.78f)
            lineTo(w * 0.28f, h * 0.3f)
            lineTo(w * 0.48f, h * 0.55f)
            lineTo(w * 0.63f, h * 0.38f)
            lineTo(w, h * 0.97f)
            lineTo(w, h)
            close()
        }
        drawPath(path, Color(0xFF2E8B57))
    }
}

/** The web's sign-in screenshot: a grey title bar and a pale card holding two fields and a button, on white. */
internal fun sampleSignInPainter(): Painter = SignInPainter()

private class SignInPainter : Painter() {
    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height
        drawRect(Color.White)
        drawRect(Color(0xFFDFDFDF), Offset.Zero, Size(w, h * 0.1f))
        val card = Size(w * 0.31f, h * 0.55f)
        val origin = Offset((w - card.width) / 2, h * 0.26f)
        drawRect(Color(0xFFEFEFEF), origin, card)
        val field = Size(card.width * 0.78f, card.height * 0.11f)
        val left = origin.x + (card.width - field.width) / 2
        drawRect(Color(0xFFCCCCCC), Offset(left, origin.y + card.height * 0.16f), field)
        drawRect(Color(0xFFCCCCCC), Offset(left, origin.y + card.height * 0.34f), field)
        drawRect(Color(0xFF7A7A7A), Offset(left, origin.y + card.height * 0.6f), field)
    }
}
