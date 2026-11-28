package dev.monarchui.components.files.avatarupload

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import dev.monarchui.components.files.fileupload.MonarchPickedFile

/**
 * What a `MonarchAvatarUpload` hands over after the crop is saved.
 *
 * Encode [image] for upload with `image.asAndroidBitmap().compress(...)`, or
 * upload [file] with [crop] when the server crops.
 *
 * @property image the cropped square picture, `outputSize` pixels on each side.
 * @property file the picked file: its content URI, name, type and size.
 * @property crop the square kept, in the pixels of the decoded image.
 */
@Immutable
public class MonarchAvatarUploadResult(
    public val image: ImageBitmap,
    public val file: MonarchPickedFile,
    public val crop: MonarchAvatarCrop,
)
