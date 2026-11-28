package dev.monarchui.components.datadisplay.favicon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.monarchui.internal.MonarchImage
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/**
 * A site's icon at a fixed size, with a letter tile while there is no image.
 *
 * Built by Monarch on the shared picture convention: Monarch ships no image
 * loader, so the app loads the icon with its own pipeline and passes a
 * [Painter]. While [image] is `null` (loading, failed, or no icon) a tile with
 * the first letter of [name] is shown. Failure is not an error state.
 *
 * The favicon is decoration and hidden from TalkBack, because the site name is
 * normally written next to it. Give a [label] when it stands alone.
 *
 * @param image the loaded icon, or `null` to show the letter tile.
 * @param name the site's name or domain, used for the letter of the tile, for example "example.com".
 * @param modifier the modifier of the favicon.
 * @param label what TalkBack reads. With a label the favicon is one element with the image role;
 * without one it is hidden from TalkBack.
 * @param size the side of the favicon.
 * @param shape the corners of the favicon.
 */
@Composable
public fun MonarchFavicon(
    image: Painter?,
    name: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    size: MonarchFaviconSize = MonarchFaviconSize.Sm,
    shape: MonarchFaviconShape = MonarchFaviconShape.Rounded,
) {
    val side = MonarchFaviconTokens.size(size)
    val clip = MonarchFaviconTokens.shape(shape)
    val fallback = MonarchFaviconTokens.fallbackColors(MonarchTheme.colors)
    val semantics = if (label != null) {
        Modifier.clearAndSetSemantics {
            contentDescription = label
            role = Role.Image
        }
    } else {
        Modifier.clearAndSetSemantics { }
    }
    val tileBorder = if (image == null) {
        Modifier.border(MonarchShapeDefaults.BorderWidth, fallback.border ?: Color.Transparent, clip)
    } else {
        Modifier
    }
    // The letter keeps its physical size: the tile is a picture and does not follow the font size setting.
    val letterSize = with(LocalDensity.current) { (side * MonarchFaviconTokens.LETTER_SCALE).toSp() }
    MonarchImage(
        painter = image,
        contentDescription = null,
        modifier = modifier
            .then(semantics)
            .size(side)
            .then(tileBorder),
        shape = clip,
        contentScale = ContentScale.Fit,
    ) {
        Box(modifier = Modifier.fillMaxSize().background(fallback.container), contentAlignment = Alignment.Center) {
            Text(
                text = MonarchFaviconTokens.letter(name),
                style = MonarchTheme.typography.caption.copy(
                    fontSize = letterSize,
                    lineHeight = letterSize,
                    fontWeight = FontWeight(MonarchFaviconTokens.LETTER_WEIGHT),
                    textAlign = TextAlign.Center,
                ),
                color = fallback.content,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
