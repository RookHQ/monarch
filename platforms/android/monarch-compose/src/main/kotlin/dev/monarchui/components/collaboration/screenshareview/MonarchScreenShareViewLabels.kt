package dev.monarchui.components.collaboration.screenshareview

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchScreenShareView`. Every
 * `null` keeps the built-in string `monarch_screenshareview_<key>`.
 *
 * @property presenting the bar when someone else shares, given the presenter's name.
 * @property youArePresenting the bar when the person using the app shares.
 * @property region what TalkBack reads for the picture, given the presenter's name.
 * @property waiting shown while the shared screen has not arrived.
 * @property localPreviewHidden shown instead of the person's own shared screen.
 * @property showPreview the button that shows it anyway.
 * @property fit the button that fits the whole screen.
 * @property zoomIn the button and TalkBack action that zoom in one step.
 * @property zoomOut the button and TalkBack action that zoom out one step.
 * @property zoomLevel the zoom level shown and read, given the percentage. The default uses the
 * locale's percent format.
 * @property pictureInPicture the button that starts picture in picture.
 * @property exitPictureInPicture the same button while picture in picture is active.
 * @property fullScreen the button that enters full screen.
 * @property exitFullScreen the same button while in full screen.
 * @property stopSharing the button that stops sharing.
 */
@Immutable
public class MonarchScreenShareViewLabels(
    public val presenting: ((String) -> String)? = null,
    public val youArePresenting: String? = null,
    public val region: ((String) -> String)? = null,
    public val waiting: String? = null,
    public val localPreviewHidden: String? = null,
    public val showPreview: String? = null,
    public val fit: String? = null,
    public val zoomIn: String? = null,
    public val zoomOut: String? = null,
    public val zoomLevel: ((Int) -> String)? = null,
    public val pictureInPicture: String? = null,
    public val exitPictureInPicture: String? = null,
    public val fullScreen: String? = null,
    public val exitFullScreen: String? = null,
    public val stopSharing: String? = null,
)
