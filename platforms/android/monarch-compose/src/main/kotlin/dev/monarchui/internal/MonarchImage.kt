package dev.monarchui.internal

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * The one way a Monarch component shows a picture (avatar, link card, file card).
 *
 * Monarch ships no image loader and no networking. The app loads the image
 * with its own pipeline and passes a [Painter]; while it has none (loading,
 * failed, or no source) it passes `null` and the [placeholder] is shown.
 * Failure is not an error state.
 *
 * The public API of a component that shows a picture therefore has
 * `image: Painter?` (and may add an `image: @Composable () -> Unit` slot
 * overload), never a URL.
 *
 * @param painter the loaded image, or `null` to show the placeholder.
 * @param contentDescription what the picture shows, or `null` when the component names it in text.
 * @param modifier sets the size; the image fills it.
 * @param shape the clip of the image and the placeholder.
 * @param contentScale how the image fills the box. Crop by default.
 * @param placeholder shown when [painter] is `null`: initials, an icon or a tinted tile. It is decoration
 * and hidden from TalkBack.
 */
@Composable
internal fun MonarchImage(
    painter: Painter?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MonarchShapeDefaults.Control,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.clip(shape), contentAlignment = Alignment.Center) {
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
                content = placeholder,
            )
        }
    }
}
