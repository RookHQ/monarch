package dev.monarchui.components.files.avatarupload

import androidx.compose.runtime.Immutable
import dev.monarchui.components.files.fileupload.MonarchFileUploadLabels

/**
 * Replacements for the built-in strings of a `MonarchAvatarUpload`. Every
 * `null` uses the built-in string named after it in `strings_avatarupload.xml`.
 *
 * @property upload the button that opens the photo picker when there is no picture: "Upload photo".
 * @property change the same button when there is a picture: "Change photo".
 * @property remove the button that removes the picture: "Remove photo".
 * @property cropArea the TalkBack name of the crop area: "Photo position".
 * @property cropHint the instructions under the crop area.
 * @property zoom the label of the zoom slider: "Zoom".
 * @property zoomValue writes the zoom as a percentage: "150%".
 * @property moveLeft the TalkBack action that moves the photo left: "Move left".
 * @property moveRight the TalkBack action that moves the photo right: "Move right".
 * @property moveUp the TalkBack action that moves the photo up: "Move up".
 * @property moveDown the TalkBack action that moves the photo down: "Move down".
 * @property zoomIn the TalkBack action that zooms in: "Zoom in".
 * @property zoomOut the TalkBack action that zooms out: "Zoom out".
 * @property save the button that saves the crop: "Save photo".
 * @property cancel the button that leaves the crop without saving: "Cancel".
 * @property saveFailed the message when saving fails: "The photo could not be saved. Try again.".
 * @property openFailed the message when the picked file cannot be read as an image: "The photo could not be opened.".
 * @property validation the type and size messages, shared with `MonarchFileUpload`.
 */
@Immutable
public class MonarchAvatarUploadLabels(
    public val upload: String? = null,
    public val change: String? = null,
    public val remove: String? = null,
    public val cropArea: String? = null,
    public val cropHint: String? = null,
    public val zoom: String? = null,
    public val zoomValue: ((percent: Int) -> String)? = null,
    public val moveLeft: String? = null,
    public val moveRight: String? = null,
    public val moveUp: String? = null,
    public val moveDown: String? = null,
    public val zoomIn: String? = null,
    public val zoomOut: String? = null,
    public val save: String? = null,
    public val cancel: String? = null,
    public val saveFailed: String? = null,
    public val openFailed: String? = null,
    public val validation: MonarchFileUploadLabels = MonarchFileUploadLabels(),
)
