package com.rookhq.monarch.sample.sections.collaboration

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.collaboration.devicepicker.MonarchDevicePicker
import com.rookhq.monarch.components.collaboration.devicepicker.MonarchDevicePickerAccess
import com.rookhq.monarch.components.collaboration.devicepicker.MonarchDevicePickerDevice
import com.rookhq.monarch.components.collaboration.devicepicker.MonarchDevicePickerSize
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of DevicePicker. */
object MonarchDevicePickerSection : CataloguePage {
    override val title = "DevicePicker"

    private val microphones = listOf(
        MonarchDevicePickerDevice("desk", "Desk microphone"),
        MonarchDevicePickerDevice("headset", "Headset microphone"),
    )
    private val cameras = listOf(
        MonarchDevicePickerDevice("front", "Front camera"),
        MonarchDevicePickerDevice("back", "Back camera"),
    )
    private val speakers = listOf(
        MonarchDevicePickerDevice("earpiece", "Earpiece"),
        MonarchDevicePickerDevice("speaker", "Speaker phone"),
        MonarchDevicePickerDevice("headset", "Headset"),
    )

    @Composable
    override fun Content() {
        var access by rememberSaveable { mutableIntStateOf(0) }
        var microphone by rememberSaveable { mutableStateOf<String?>(null) }
        var camera by rememberSaveable { mutableStateOf<String?>("front") }
        var speaker by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Access")
        MonarchSegmentedControl {
            MonarchDevicePickerAccess.entries.forEachIndexed { index, option ->
                Segment(
                    selected = index == access,
                    onClick = { access = index },
                    label = { Text(option.name) },
                )
            }
        }
        CatalogueCaption("Lobby")
        MonarchDevicePicker(
            microphone = microphone,
            onMicrophoneChange = { microphone = it },
            camera = camera,
            onCameraChange = { camera = it },
            speaker = speaker,
            onSpeakerChange = { speaker = it },
            microphones = microphones,
            cameras = cameras,
            speakers = speakers,
            access = MonarchDevicePickerAccess.entries[access],
            onRequestAccess = { access = 0 },
            onOpenSettings = { access = 0 },
        )
        CatalogueCaption("During a call, small")
        MonarchDevicePicker(
            microphone = microphone,
            onMicrophoneChange = { microphone = it },
            speaker = speaker,
            onSpeakerChange = { speaker = it },
            microphones = microphones,
            speakers = speakers,
            size = MonarchDevicePickerSize.Sm,
        )
    }
}
