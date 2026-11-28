@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.collaboration.prejoinlobby

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchPreJoinLobbyPreview() {
    PreviewFrame {
        MonarchPreJoinLobby(
            micOn = true,
            onMicOnChange = {},
            onJoin = { _, _ -> },
            icon = { _, _ -> },
            cameraOn = false,
            onCameraOnChange = {},
            audioLevel = 0.4f,
            participants = listOf("Ana Ruiz", "Ben Okafor").map { MonarchPreJoinLobbyParticipant(it) },
            onCancel = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchPreJoinLobbyErrorPreview() {
    PreviewFrame {
        MonarchPreJoinLobby(
            micOn = false,
            onMicOnChange = {},
            onJoin = { _, _ -> },
            icon = { _, _ -> },
            cameraOn = true,
            onCameraOnChange = {},
            joining = true,
            error = "Camera access is blocked. Allow it in Settings.",
            devices = { MonarchText("Devices") },
        )
    }
}
