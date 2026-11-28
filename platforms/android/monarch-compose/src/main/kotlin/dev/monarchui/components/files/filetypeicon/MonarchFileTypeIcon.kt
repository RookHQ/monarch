package dev.monarchui.components.files.filetypeicon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A glyph for the family of a file, worked out from its name and MIME type,
 * with a spoken name, for example "PDF document".
 *
 * Built by Monarch: Android has no file-type icon set. The glyph is a page
 * outline with a mark for the family, drawn as on the web, so the type is
 * told by the mark and the spoken name, never by the tint alone.
 *
 * @param fileName the file name or path; its extension decides the family when [contentType] does not.
 * @param modifier the modifier of the icon.
 * @param contentType the MIME type, for example from `ContentResolver.getType`. A specific type takes
 * precedence over the extension.
 * @param size the size of the glyph; [MonarchFileTypeIconSize.Lg] adds a tinted tile.
 * @param decorative hides the icon from TalkBack when nearby text already says the type.
 * @param label replaces the built-in spoken name of the family.
 */
@Composable
public fun MonarchFileTypeIcon(
    fileName: String?,
    modifier: Modifier = Modifier,
    contentType: String? = null,
    size: MonarchFileTypeIconSize = MonarchFileTypeIconSize.Md,
    decorative: Boolean = false,
    label: String? = null,
) {
    val family = remember(fileName, contentType) { MonarchFileTypeFamily.of(fileName, contentType) }
    MonarchFileTypeIcon(family = family, modifier = modifier, size = size, decorative = decorative, label = label)
}

/**
 * A glyph for a file family that is already known.
 *
 * @param family the family to draw.
 * @param modifier the modifier of the icon.
 * @param size the size of the glyph; [MonarchFileTypeIconSize.Lg] adds a tinted tile.
 * @param decorative hides the icon from TalkBack when nearby text already says the type.
 * @param label replaces the built-in spoken name of the family.
 */
@Composable
public fun MonarchFileTypeIcon(
    family: MonarchFileTypeFamily,
    modifier: Modifier = Modifier,
    size: MonarchFileTypeIconSize = MonarchFileTypeIconSize.Md,
    decorative: Boolean = false,
    label: String? = null,
) {
    val colors = MonarchTheme.colors
    val spoken = monarchString(label, MonarchFileTypeIconTokens.label(family))
    val semantics = if (decorative) {
        Modifier.clearAndSetSemantics { }
    } else {
        Modifier.clearAndSetSemantics {
            contentDescription = spoken
            role = Role.Image
        }
    }
    val glyph = MonarchFileTypeIconTokens.glyphSize(size)
    val tint = MonarchFileTypeIconTokens.color(family, colors)
    if (size == MonarchFileTypeIconSize.Lg) {
        Box(
            modifier = modifier
                .then(semantics)
                .size(MonarchFileTypeIconTokens.TileSize)
                .background(MonarchFileTypeIconTokens.tileColor(colors), MonarchShapeDefaults.Control),
            contentAlignment = Alignment.Center,
        ) {
            FileTypeGlyph(family, tint, Modifier.size(glyph))
        }
    } else {
        FileTypeGlyph(family, tint, modifier.then(semantics).size(glyph))
    }
}

/** The page and the family's mark, stroked in [color]. Not mirrored: a page has no reading direction. */
@Composable
private fun FileTypeGlyph(family: MonarchFileTypeFamily, color: Color, modifier: Modifier) {
    val paths = remember(family) {
        listOfNotNull(MonarchFileTypeIconTokens.PAGE, MonarchFileTypeIconTokens.mark(family)).map(::parse)
    }
    Canvas(modifier = modifier) {
        val unit = size.minDimension / MonarchFileTypeIconTokens.VIEWPORT
        val stroke = Stroke(
            width = MonarchFileTypeIconTokens.STROKE_WIDTH,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        scale(unit, pivot = Offset.Zero) {
            paths.forEach { drawPath(it, color, style = stroke) }
        }
    }
}

private fun parse(pathData: String): Path = PathParser().parsePathString(pathData).toPath()
