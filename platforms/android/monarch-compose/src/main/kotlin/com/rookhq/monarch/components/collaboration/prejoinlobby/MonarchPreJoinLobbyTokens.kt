package com.rookhq.monarch.components.collaboration.prejoinlobby

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and pure logic of MonarchPreJoinLobby, built from primitives and roles only. */
internal object MonarchPreJoinLobbyTokens {
    val Gap: Dp = MonarchTokens.Space.space4.dp
    val InnerGap: Dp = MonarchTokens.Space.space2.dp
    val PreviewPadding: Dp = MonarchTokens.Space.space4.dp

    /** The width of the microphone level bar at the top start of the picture. */
    val LevelWidth: Dp = MonarchTokens.Space.space12.dp

    /** The preview is at least as tall as a 16:9 picture of its width. */
    const val PREVIEW_ASPECT: Float = 16f / 9f

    /** The avatars shown before the "+N" tile. */
    const val MAX_AVATARS: Int = 5

    /** What the preview area shows. */
    enum class Preview { Video, CameraOff, Starting, Unavailable }

    /**
     * The preview to show: the app's video while the camera is on and the app
     * gives one; "No preview" while there is an error; otherwise whether the
     * camera is off or starting.
     */
    fun preview(cameraOn: Boolean, hasVideo: Boolean, hasError: Boolean): Preview = when {
        hasError -> Preview.Unavailable
        cameraOn && hasVideo -> Preview.Video
        cameraOn -> Preview.Starting
        else -> Preview.CameraOff
    }

    /** The fill of the preview area behind the placeholder and the video. */
    fun previewBackground(colors: MonarchColors): Color = colors.bgMuted

    /** The colour of the placeholder text in the preview. */
    fun placeholderColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the line about who is already in the call. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted
}
