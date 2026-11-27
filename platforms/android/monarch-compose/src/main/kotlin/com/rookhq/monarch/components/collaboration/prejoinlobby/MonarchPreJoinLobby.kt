package com.rookhq.monarch.components.collaboration.prejoinlobby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.collaboration.audiolevelmeter.MonarchAudioLevelMeter
import com.rookhq.monarch.components.collaboration.audiolevelmeter.MonarchAudioLevelMeterVariant
import com.rookhq.monarch.components.collaboration.callcontrols.MonarchCallControl
import com.rookhq.monarch.components.collaboration.callcontrols.MonarchCallControls
import com.rookhq.monarch.components.feedback.banner.MonarchBanner
import com.rookhq.monarch.components.feedback.banner.MonarchBannerAnnouncement
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarGroup
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The screen before joining a call: see yourself, check that the microphone
 * picks you up, choose devices, and join with the microphone or camera off.
 *
 * A Monarch pattern built from `MonarchCallControls` (the microphone and
 * camera toggles), `MonarchAudioLevelMeter`, `MonarchAvatarGroup`,
 * `MonarchBanner` and `MonarchButton`s. It never captures media: the app
 * owns the camera and microphone permissions, shows its CameraX
 * `PreviewView` (or a call SDK's view) in [preview], measures the
 * microphone into [audioLevel], and puts a `MonarchDevicePicker` in
 * [devices]. Place it in a screen, a dialog or a sheet.
 *
 * @param micOn whether the person joins with the microphone on.
 * @param onMicOnChange receives the new microphone choice.
 * @param onJoin joins with the chosen settings.
 * @param icon draws the glyph of the microphone and camera toggles, given whether each is on, as for
 * `MonarchCallControls`. Monarch bundles no icons, so it is required.
 * @param modifier the modifier of the lobby.
 * @param cameraOn whether the person joins with the camera on.
 * @param onCameraOnChange shows the camera toggle. Leave it out for audio-only calls.
 * @param audioLevel the microphone loudness from 0 to 1, measured by the app.
 * @param participants the people already in the call.
 * @param joining the join is in progress: the join button shows progress and the other controls are off.
 * @param showTitle shows the heading. Turn it off when the screen or dialog already has a title.
 * @param error a problem getting the preview, such as camera access denied, shown in a danger banner.
 * @param joinLabel the join button's text, such as "Start call". "Join now" by default.
 * @param labels replacements for the built-in strings, including the toggles'.
 * @param onCancel shows a Cancel button.
 * @param devices the device choice under the preview, usually a `MonarchDevicePicker` with the small size.
 * @param preview the app's camera preview, shown while [cameraOn] is on. It fills the preview area.
 */
@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalLayoutApi::class)
@ExperimentalMonarchApi
@Composable
public fun MonarchPreJoinLobby(
    micOn: Boolean,
    onMicOnChange: (Boolean) -> Unit,
    onJoin: (micOn: Boolean, cameraOn: Boolean) -> Unit,
    icon: @Composable (control: MonarchCallControl, on: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    cameraOn: Boolean = false,
    onCameraOnChange: ((Boolean) -> Unit)? = null,
    audioLevel: Float = 0f,
    participants: List<MonarchPreJoinLobbyParticipant> = emptyList(),
    joining: Boolean = false,
    showTitle: Boolean = true,
    error: String? = null,
    joinLabel: String? = null,
    labels: MonarchPreJoinLobbyLabels = MonarchPreJoinLobbyLabels(),
    onCancel: (() -> Unit)? = null,
    devices: (@Composable () -> Unit)? = null,
    preview: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchPreJoinLobbyTokens
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        if (showTitle) {
            MonarchText(
                text = monarchString(labels.title, R.string.monarch_prejoinlobby_title),
                modifier = Modifier.semantics { heading() },
                style = MonarchTextStyle.Heading,
            )
        }
        PreJoinLobbyPreview(
            shown = tokens.preview(cameraOn, preview != null, error != null),
            labels = labels,
            preview = preview,
            // As on the web: the level bar sits at the top start of the picture and the toggles over its bottom.
            level = {
                MonarchAudioLevelMeter(
                    level = audioLevel,
                    variant = MonarchAudioLevelMeterVariant.Bar,
                    muted = !micOn,
                    label = monarchString(labels.micLevel, R.string.monarch_prejoinlobby_mic_level),
                    mutedLabel = monarchString(labels.muted, R.string.monarch_prejoinlobby_muted),
                )
            },
            controls = {
                MonarchCallControls(
                    icon = icon,
                    micOn = micOn,
                    onMicOnChange = onMicOnChange,
                    cameraOn = cameraOn,
                    onCameraOnChange = onCameraOnChange,
                    reactions = emptyList(),
                    minimal = true,
                    enabled = !joining,
                    labels = labels.callControls,
                )
            },
        )
        if (error != null) {
            MonarchBanner(message = error, tone = MonarchTone.Danger, announcement = MonarchBannerAnnouncement.Polite)
        }
        devices?.invoke()
        if (participants.isNotEmpty()) PreJoinLobbyParticipants(participants, labels)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.InnerGap, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(tokens.InnerGap),
        ) {
            if (onCancel != null) {
                MonarchButton(
                    text = monarchString(labels.cancel, R.string.monarch_prejoinlobby_cancel),
                    onClick = onCancel,
                    variant = MonarchButtonVariant.Ghost,
                    enabled = !joining,
                )
            }
            MonarchButton(
                text = joinLabel ?: monarchString(labels.join, R.string.monarch_prejoinlobby_join),
                onClick = { onJoin(micOn, cameraOn) },
                variant = MonarchButtonVariant.Primary,
                loading = joining,
                loadingLabel = monarchString(labels.joining, R.string.monarch_prejoinlobby_joining),
            )
        }
    }
}

/** The preview area: the app's video, or a line saying why there is none. Read as one element. */
@Composable
private fun PreJoinLobbyPreview(
    shown: MonarchPreJoinLobbyTokens.Preview,
    labels: MonarchPreJoinLobbyLabels,
    preview: (@Composable () -> Unit)?,
    level: @Composable () -> Unit,
    controls: @Composable () -> Unit,
) {
    val tokens = MonarchPreJoinLobbyTokens
    val colors = MonarchTheme.colors
    val name = monarchString(labels.preview, R.string.monarch_prejoinlobby_preview)
    val placeholder = when (shown) {
        MonarchPreJoinLobbyTokens.Preview.Video -> null
        MonarchPreJoinLobbyTokens.Preview.CameraOff ->
            monarchString(labels.cameraOff, R.string.monarch_prejoinlobby_camera_off)
        MonarchPreJoinLobbyTokens.Preview.Starting ->
            monarchString(labels.startingCamera, R.string.monarch_prejoinlobby_starting_camera)
        MonarchPreJoinLobbyTokens.Preview.Unavailable ->
            monarchString(labels.noPreview, R.string.monarch_prejoinlobby_no_preview)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // At least a 16:9 picture of the width; taller when the placeholder text needs it.
        val minHeight = maxWidth / tokens.PREVIEW_ASPECT
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .clip(MonarchShapeDefaults.Container)
                    .background(tokens.previewBackground(colors))
                    .semantics(mergeDescendants = true) { contentDescription = name },
                contentAlignment = Alignment.Center,
            ) {
                if (placeholder == null) {
                    Box(Modifier.matchParentSize()) { preview?.invoke() }
                } else {
                    MonarchText(
                        text = placeholder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(tokens.PreviewPadding),
                        color = tokens.placeholderColor(colors),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(tokens.PreviewPadding)
                    .width(tokens.LevelWidth),
            ) { level() }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(tokens.PreviewPadding),
            ) { controls() }
        }
    }
}

/** The avatars of the people already in the call and how many they are. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreJoinLobbyParticipants(
    participants: List<MonarchPreJoinLobbyParticipant>,
    labels: MonarchPreJoinLobbyLabels,
) {
    val tokens = MonarchPreJoinLobbyTokens
    val count = participants.size
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.InnerGap),
        verticalArrangement = Arrangement.spacedBy(tokens.InnerGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchAvatarGroup(
            items = participants,
            label = monarchString(labels.participants, R.string.monarch_prejoinlobby_participants),
            maxVisible = tokens.MAX_AVATARS,
            size = MonarchAvatarSize.Sm,
        ) { person ->
            MonarchAvatar(image = person.image, name = person.name, size = MonarchAvatarSize.Sm)
        }
        MonarchText(
            text = labels.alreadyIn?.invoke(count)
                ?: pluralStringResource(R.plurals.monarch_prejoinlobby_already_in, count, count),
            style = MonarchTextStyle.Caption,
            color = tokens.mutedColor(MonarchTheme.colors),
        )
    }
}
