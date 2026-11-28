package dev.monarchui.components.files.avatarupload

/**
 * The square of the picked image a `MonarchAvatarUpload` kept, in the
 * image's own pixels after decoding (large images are scaled down first).
 *
 * @property x the left edge of the square.
 * @property y the top edge of the square.
 * @property size the side of the square.
 */
public data class MonarchAvatarCrop(
    val x: Float,
    val y: Float,
    val size: Float,
)
