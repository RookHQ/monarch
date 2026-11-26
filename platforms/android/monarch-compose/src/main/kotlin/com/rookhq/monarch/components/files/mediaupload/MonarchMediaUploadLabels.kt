package com.rookhq.monarch.components.files.mediaupload

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.files.fileupload.MonarchFileUploadLabels

/**
 * Replacements for the built-in strings of a `MonarchMediaUpload`. Every
 * `null` uses the built-in string named after it in `strings_mediaupload.xml`.
 *
 * @property addImage the main text of the empty area when only images are taken: "Add an image".
 * @property addMedia the main text of the empty area when videos are also taken: "Add an image or video".
 * @property chooseImage the button that opens the photo picker for images: "Choose image".
 * @property chooseMedia the button that opens the photo picker for images and videos: "Choose image or video".
 * @property imageHint writes the hint about images from the formatted largest size, or `null`.
 * @property videoHint writes the hint about videos from the formatted largest size and length, each or `null`.
 * @property videoNotAllowed writes the refusal of a video where only images are taken, from the file name.
 * @property tooLarge writes the refusal of a file over the limit, from the file name and the formatted limit.
 * @property tooLong writes the refusal of a video over the length limit, from the file name and the length.
 * @property durationUnreadable writes the refusal of a video whose length could not be read, from the file name.
 * @property checking writes the text while a video's length is read, from the file name.
 * @property uploading writes the text while a file uploads, from the file name and the percentage or `null`.
 * @property uploadFailed shown when an upload failed without a message: "The upload failed. Try again.".
 * @property cancel the button that cancels an upload: "Cancel upload".
 * @property cancelled announced after an upload is cancelled: "Upload cancelled".
 * @property imageUploaded announced after an image is uploaded: "Image uploaded".
 * @property videoUploaded announced after a video is uploaded: "Video uploaded".
 * @property replaceImage the button that picks another image: "Replace image".
 * @property replaceVideo the button that picks another video: "Replace video".
 * @property removeImage the button that removes the image: "Remove image".
 * @property removeVideo the button that removes the video: "Remove video".
 * @property imageRemoved announced after the image is removed: "Image removed".
 * @property videoRemoved announced after the video is removed: "Video removed".
 * @property previewVideo the spoken name of the video preview: "Uploaded video".
 * @property play the spoken name of the video preview when it plays: "Play video".
 * @property warnings the first line of the notice after an upload with warnings: "Uploaded with warnings.".
 * @property formatDuration writes a length in milliseconds: "1:30 min" or "45 s".
 * @property fileUpload the strings of the file picker area, including `formatSize`.
 */
@Immutable
public class MonarchMediaUploadLabels(
    public val addImage: String? = null,
    public val addMedia: String? = null,
    public val chooseImage: String? = null,
    public val chooseMedia: String? = null,
    public val imageHint: ((maxSize: String?) -> String)? = null,
    public val videoHint: ((maxSize: String?, maxDuration: String?) -> String)? = null,
    public val videoNotAllowed: ((name: String) -> String)? = null,
    public val tooLarge: ((name: String, maxSize: String) -> String)? = null,
    public val tooLong: ((name: String, maxDuration: String) -> String)? = null,
    public val durationUnreadable: ((name: String) -> String)? = null,
    public val checking: ((name: String) -> String)? = null,
    public val uploading: ((name: String, percent: Int?) -> String)? = null,
    public val uploadFailed: String? = null,
    public val cancel: String? = null,
    public val cancelled: String? = null,
    public val imageUploaded: String? = null,
    public val videoUploaded: String? = null,
    public val replaceImage: String? = null,
    public val replaceVideo: String? = null,
    public val removeImage: String? = null,
    public val removeVideo: String? = null,
    public val imageRemoved: String? = null,
    public val videoRemoved: String? = null,
    public val previewVideo: String? = null,
    public val play: String? = null,
    public val warnings: String? = null,
    public val formatDuration: ((millis: Long) -> String)? = null,
    public val fileUpload: MonarchFileUploadLabels = MonarchFileUploadLabels(),
)
