package dev.monarchui.components.collaboration.devicepicker

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.feedback.banner.MonarchBannerAnnouncement
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.forms.select.MonarchSelect
import dev.monarchui.internal.monarchString

/**
 * Microphone, camera and audio output menus for a call's lobby or settings, with a notice that explains camera
 * and microphone access and offers the way to fix it.
 *
 * Built from `MonarchSelect` (the Material 3 `ExposedDropdownMenuBox`) fields and a `MonarchInlineNotice`. It is
 * UI only: the app lists the devices (`AudioManager`, `CameraManager`), applies the choice (for example
 * `AudioManager.setCommunicationDevice` from API 31, or the camera id it opens) and owns the permission request,
 * usually with `rememberLauncherForActivityResult(RequestMultiplePermissions())`. A field is shown when its
 * change callback is given; each starts with "System default", reported as `null`. The microphone and camera
 * fields are disabled until [access] is `Granted`; the audio output needs no permission.
 *
 * @param modifier the modifier of the column.
 * @param microphone the chosen microphone's id; `null` is System default.
 * @param onMicrophoneChange shows the microphone field; called with the new id, `null` for System default.
 * @param camera the chosen camera's id; `null` is System default.
 * @param onCameraChange shows the camera field; called with the new id, `null` for System default.
 * @param speaker the chosen audio output's id; `null` is System default.
 * @param onSpeakerChange shows the audio output field; called with the new id, `null` for System default.
 * @param microphones the microphones offered.
 * @param cameras the cameras offered.
 * @param speakers the audio outputs offered, such as the earpiece, the speaker and a headset.
 * @param access the camera and microphone permission state to explain.
 * @param size `Md` for a lobby or settings screen, `Sm` for a sheet during a call.
 * @param enabled whether the fields and the notice's button can be used.
 * @param labels replacements for every built-in string.
 * @param onRequestAccess shows Allow access while access is not asked; the app requests the permissions.
 * @param onOpenSettings replaces what Open Settings does when access is refused. `null` opens the app's page in
 * the system Settings.
 */
@Composable
public fun MonarchDevicePicker(
    modifier: Modifier = Modifier,
    microphone: String? = null,
    onMicrophoneChange: ((String?) -> Unit)? = null,
    camera: String? = null,
    onCameraChange: ((String?) -> Unit)? = null,
    speaker: String? = null,
    onSpeakerChange: ((String?) -> Unit)? = null,
    microphones: List<MonarchDevicePickerDevice> = emptyList(),
    cameras: List<MonarchDevicePickerDevice> = emptyList(),
    speakers: List<MonarchDevicePickerDevice> = emptyList(),
    access: MonarchDevicePickerAccess = MonarchDevicePickerAccess.Granted,
    size: MonarchDevicePickerSize = MonarchDevicePickerSize.Md,
    enabled: Boolean = true,
    labels: MonarchDevicePickerLabels = MonarchDevicePickerLabels(),
    onRequestAccess: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val tokens = MonarchDevicePickerTokens
    val context = LocalContext.current
    val systemDefault = monarchString(labels.systemDefault, R.string.monarch_devicepicker_system_default)
    val inputsShown = onMicrophoneChange != null || onCameraChange != null
    val anyInput = (onMicrophoneChange != null && microphones.any { it.id.isNotEmpty() }) ||
        (onCameraChange != null && cameras.any { it.id.isNotEmpty() })
    val notice = tokens.notice(access, inputsShown, anyInput)
    val inputsEnabled = tokens.inputsEnabled(access, enabled)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tokens.gap(size)),
    ) {
        if (notice != null) {
            DevicePickerNoticeView(
                notice = notice,
                size = size,
                enabled = enabled,
                labels = labels,
                onRequestAccess = onRequestAccess,
                onOpenSettings = onOpenSettings ?: { openAppSettings(context) },
            )
        }
        if (onMicrophoneChange != null) {
            val pattern = stringResource(R.string.monarch_devicepicker_unnamed_microphone)
            DevicePickerField(
                label = monarchString(labels.microphone, R.string.monarch_devicepicker_microphone),
                devices = microphones,
                selected = microphone,
                systemDefault = systemDefault,
                unnamed = labels.unnamedMicrophone ?: { pattern.format(it) },
                enabled = inputsEnabled,
                onChange = onMicrophoneChange,
            )
        }
        if (onCameraChange != null) {
            val pattern = stringResource(R.string.monarch_devicepicker_unnamed_camera)
            DevicePickerField(
                label = monarchString(labels.camera, R.string.monarch_devicepicker_camera),
                devices = cameras,
                selected = camera,
                systemDefault = systemDefault,
                unnamed = labels.unnamedCamera ?: { pattern.format(it) },
                enabled = inputsEnabled,
                onChange = onCameraChange,
            )
        }
        if (onSpeakerChange != null) {
            val pattern = stringResource(R.string.monarch_devicepicker_unnamed_speaker)
            DevicePickerField(
                label = monarchString(labels.speaker, R.string.monarch_devicepicker_speaker),
                devices = speakers,
                selected = speaker,
                systemDefault = systemDefault,
                unnamed = labels.unnamedSpeaker ?: { pattern.format(it) },
                enabled = enabled,
                onChange = onSpeakerChange,
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun DevicePickerField(
    label: String,
    devices: List<MonarchDevicePickerDevice>,
    selected: String?,
    systemDefault: String,
    unnamed: (Int) -> String,
    enabled: Boolean,
    onChange: (String?) -> Unit,
) {
    val tokens = MonarchDevicePickerTokens
    MonarchSelect(
        options = tokens.options(devices, selected, systemDefault, unnamed),
        selected = tokens.selection(selected),
        onSelect = { onChange(tokens.choice(it)) },
        label = label,
        enabled = enabled,
    )
}

@Composable
private fun DevicePickerNoticeView(
    notice: DevicePickerNotice,
    size: MonarchDevicePickerSize,
    enabled: Boolean,
    labels: MonarchDevicePickerLabels,
    onRequestAccess: (() -> Unit)?,
    onOpenSettings: () -> Unit,
) {
    val tokens = MonarchDevicePickerTokens
    val message = when (notice) {
        DevicePickerNotice.NotDetermined ->
            monarchString(labels.notDetermined, R.string.monarch_devicepicker_not_determined)
        DevicePickerNotice.Denied -> monarchString(labels.denied, R.string.monarch_devicepicker_denied)
        DevicePickerNotice.Unavailable -> monarchString(labels.unavailable, R.string.monarch_devicepicker_unavailable)
    }
    val action: (@Composable () -> Unit)? = when {
        notice == DevicePickerNotice.NotDetermined && onRequestAccess != null -> {
            {
                MonarchButton(
                    text = monarchString(labels.allowAccess, R.string.monarch_devicepicker_allow_access),
                    onClick = onRequestAccess,
                    size = tokens.buttonSize(size),
                    enabled = enabled,
                )
            }
        }
        notice == DevicePickerNotice.Denied -> {
            {
                MonarchButton(
                    text = monarchString(labels.openSettings, R.string.monarch_devicepicker_open_settings),
                    onClick = onOpenSettings,
                    size = tokens.buttonSize(size),
                    enabled = enabled,
                )
            }
        }
        else -> null
    }
    MonarchInlineNotice(
        message = message,
        tone = tokens.tone(notice),
        action = action,
        announcement = MonarchBannerAnnouncement.Polite,
    )
}

/** Opens the app's page in the system Settings, where the person can turn camera and microphone access on. */
private fun openAppSettings(context: Context) {
    val uri = Uri.fromParts("package", context.packageName, null)
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // A device without a Settings app: there is nowhere to send the person.
    }
}
