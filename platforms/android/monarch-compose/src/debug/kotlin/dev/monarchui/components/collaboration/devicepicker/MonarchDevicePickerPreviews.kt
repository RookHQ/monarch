package dev.monarchui.components.collaboration.devicepicker

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val previewMicrophones = listOf(
    MonarchDevicePickerDevice("desk", "Desk microphone"),
    MonarchDevicePickerDevice("headset", "Headset microphone"),
)
private val previewCameras = listOf(MonarchDevicePickerDevice("front", "Front camera"))
private val previewSpeakers = listOf(
    MonarchDevicePickerDevice("earpiece", "Earpiece"),
    MonarchDevicePickerDevice("speaker", "Speaker phone"),
)

@MonarchPreviews
@Composable
internal fun MonarchDevicePickerPreview() {
    PreviewFrame {
        MonarchDevicePicker(
            microphone = "desk",
            onMicrophoneChange = {},
            onCameraChange = {},
            speaker = "earpiece",
            onSpeakerChange = {},
            microphones = previewMicrophones,
            cameras = previewCameras,
            speakers = previewSpeakers,
        )
        MonarchDevicePicker(
            onMicrophoneChange = {},
            microphones = previewMicrophones,
            access = MonarchDevicePickerAccess.NotDetermined,
            size = MonarchDevicePickerSize.Sm,
            onRequestAccess = {},
        )
        MonarchDevicePicker(
            onCameraChange = {},
            cameras = previewCameras,
            access = MonarchDevicePickerAccess.Denied,
            onOpenSettings = {},
        )
    }
}
