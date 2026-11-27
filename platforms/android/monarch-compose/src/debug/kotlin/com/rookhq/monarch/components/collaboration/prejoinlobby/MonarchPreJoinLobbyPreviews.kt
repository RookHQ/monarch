@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.collaboration.prejoinlobby

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
