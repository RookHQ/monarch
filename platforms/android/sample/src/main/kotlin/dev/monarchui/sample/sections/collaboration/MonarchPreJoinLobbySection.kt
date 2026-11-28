@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.collaboration.callcontrols.MonarchCallControl
import dev.monarchui.components.collaboration.devicepicker.MonarchDevicePicker
import dev.monarchui.components.collaboration.devicepicker.MonarchDevicePickerDevice
import dev.monarchui.components.collaboration.devicepicker.MonarchDevicePickerSize
import dev.monarchui.components.collaboration.prejoinlobby.MonarchPreJoinLobby
import dev.monarchui.components.collaboration.prejoinlobby.MonarchPreJoinLobbyParticipant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import kotlinx.coroutines.delay

/**
 * The catalogue page of PreJoinLobby. The catalogue has no camera, so a line
 * of text stands in for the app's CameraX preview, and the level is fixed.
 */
object MonarchPreJoinLobbySection : CataloguePage {
    override val title = "PreJoinLobby"

    private const val JOIN_MILLIS = 1_500L
    private const val LEVEL = 0.45f

    private val microphones = listOf(
        MonarchDevicePickerDevice("built-in", "Phone microphone"),
        MonarchDevicePickerDevice("headset", "Wired headset"),
    )

    private val people = listOf("Ana Ruiz", "Ben Okafor", "Chloé Martin").map { MonarchPreJoinLobbyParticipant(it) }

    @Composable
    override fun Content() {
        var mic by rememberSaveable { mutableStateOf(true) }
        var camera by rememberSaveable { mutableStateOf(false) }
        var microphone by rememberSaveable { mutableStateOf<String?>(null) }
        var joining by rememberSaveable { mutableStateOf(false) }
        var last by rememberSaveable { mutableStateOf("") }
        LaunchedEffect(joining) {
            if (joining) {
                delay(JOIN_MILLIS)
                joining = false
            }
        }
        MonarchPreJoinLobby(
            micOn = mic,
            onMicOnChange = { mic = it },
            onJoin = { m, c ->
                last = "Joined with the microphone ${if (m) "on" else "off"} and the camera ${if (c) "on" else "off"}"
                joining = true
            },
            icon = { control, on -> MonarchIcon(iconOf(control, on), contentDescription = null) },
            cameraOn = camera,
            onCameraOnChange = { camera = it },
            audioLevel = LEVEL,
            participants = people,
            joining = joining,
            onCancel = { last = "Cancelled" },
            devices = {
                MonarchDevicePicker(
                    microphone = microphone,
                    onMicrophoneChange = { microphone = it },
                    microphones = microphones,
                    size = MonarchDevicePickerSize.Sm,
                )
            },
        ) { MonarchText("The app's camera preview goes here") }
        if (last.isNotEmpty()) MonarchText(last)
        CatalogueCaption("Camera blocked, audio only")
        MonarchPreJoinLobby(
            micOn = false,
            onMicOnChange = {},
            onJoin = { _, _ -> },
            icon = { control, on -> MonarchIcon(iconOf(control, on), contentDescription = null) },
            showTitle = false,
            error = "Camera access is blocked. Allow it in Settings.",
            joinLabel = "Start call",
        )
    }

    private fun iconOf(control: MonarchCallControl, on: Boolean): ImageVector = when (control) {
        MonarchCallControl.Microphone ->
            if (on) SampleIcons.Mic else SampleIcons.MicOff
        MonarchCallControl.Camera ->
            if (on) SampleIcons.Video else SampleIcons.VideoOff
        else -> SampleIcons.More
    }
}
