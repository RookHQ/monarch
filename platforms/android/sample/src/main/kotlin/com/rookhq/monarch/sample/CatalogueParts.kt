package com.rookhq.monarch.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import com.rookhq.monarch.components.intelligence.aimessageactions.MonarchAiMessageAction
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

/** A small heading inside a catalogue page. */
@Composable
fun CatalogueCaption(text: String) {
    MonarchText(text = text, style = MonarchTextStyle.Label, color = MonarchTheme.colors.textMuted, heading = true)
}

/**
 * The icon of each action under an AI answer, for the `icon` and `actionIcon` slots. Monarch bundles no
 * icons; the catalogue draws the outlined Material icons of `SampleIcons`.
 */
fun aiActionIcon(action: MonarchAiMessageAction): ImageVector = when (action) {
    MonarchAiMessageAction.Copy -> SampleIcons.Copy
    MonarchAiMessageAction.Regenerate -> SampleIcons.Retry
    MonarchAiMessageAction.Edit -> SampleIcons.Pencil
    MonarchAiMessageAction.Good -> SampleIcons.ThumbUp
    MonarchAiMessageAction.Bad -> SampleIcons.ThumbDown
    MonarchAiMessageAction.Share -> SampleIcons.Share
    MonarchAiMessageAction.ReadAloud -> SampleIcons.ReadAloud
    MonarchAiMessageAction.More -> SampleIcons.More
}

/**
 * Pictures for pages that show a photo (an avatar, a lightbox, a file preview). An app passes its own
 * images; the catalogue draws a sky, a sun and a hill so the slot shows a picture, not an icon.
 */
object CataloguePictures {
    private val Sky = Color(0xFF8DB7E0)
    private val Hill = Color(0xFF5E8F5A)
    private val Sun = Color(0xFFF2C14E)
    private const val SIDE = 96
    private const val SUN_X = 0.7f
    private const val SUN_Y = 0.3f
    private const val SUN_RADIUS = 0.14f
    private const val HILL_X = 0.3f
    private const val HILL_Y = 1.05f
    private const val HILL_RADIUS = 0.6f

    /** A landscape picture, remembered across recompositions. */
    @Composable
    fun landscape(): Painter = remember { BitmapPainter(picture()) }

    private fun picture(): ImageBitmap {
        val bitmap = ImageBitmap(SIDE, SIDE)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val side = SIDE.toFloat()
        paint.color = Sky
        canvas.drawRect(0f, 0f, side, side, paint)
        paint.color = Sun
        canvas.drawCircle(Offset(side * SUN_X, side * SUN_Y), side * SUN_RADIUS, paint)
        paint.color = Hill
        canvas.drawCircle(Offset(side * HILL_X, side * HILL_Y), side * HILL_RADIUS, paint)
        return bitmap
    }
}
