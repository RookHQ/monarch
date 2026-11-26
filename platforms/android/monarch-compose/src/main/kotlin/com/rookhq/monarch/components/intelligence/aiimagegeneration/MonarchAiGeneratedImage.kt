package com.rookhq.monarch.components.intelligence.aiimagegeneration

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One generated image, or the slot of one that is still generating, shown by
 * `MonarchAiImageGeneration`.
 *
 * Monarch loads no images: the app loads each one with its own pipeline and
 * passes the `Painter`. Leave [image] `null` while it is generating or when it
 * could not be loaded.
 *
 * @property id a stable id, unique among the images. It is the value of the selection and is passed to the
 * callbacks.
 * @property image the image, or `null` while it is generating or could not be loaded.
 * @property alt the alt text written by the person. Required: the field shows an error while it is empty.
 */
@Immutable
public class MonarchAiGeneratedImage(
    public val id: String,
    public val image: Painter? = null,
    public val alt: String = "",
) {
    /** A copy with some properties replaced, for example the alt text after an edit. */
    public fun copy(
        id: String = this.id,
        image: Painter? = this.image,
        alt: String = this.alt,
    ): MonarchAiGeneratedImage = MonarchAiGeneratedImage(id, image, alt)

    override fun equals(other: Any?): Boolean =
        other is MonarchAiGeneratedImage && other.id == id && other.image === image && other.alt == alt

    override fun hashCode(): Int = (id.hashCode() * HASH + alt.hashCode()) * HASH + System.identityHashCode(image)

    override fun toString(): String = "MonarchAiGeneratedImage(id=$id, loaded=${image != null}, alt=$alt)"

    private companion object {
        const val HASH = 31
    }
}
