package dev.monarchui.components.feedback.snackbarhost

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchSnackbarHostPreview() {
    PreviewFrame {
        MonarchTone.entries.forEach { tone ->
            val state = remember { SnackbarHostState() }
            LaunchedEffect(state) {
                state.showMonarchSnackbar(message = "${tone.name}: ticket archived.", tone = tone, actionLabel = "Undo")
            }
            MonarchSnackbarHost(hostState = state)
        }
    }
}

