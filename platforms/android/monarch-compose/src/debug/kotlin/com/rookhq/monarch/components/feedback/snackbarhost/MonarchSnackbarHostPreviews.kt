package com.rookhq.monarch.components.feedback.snackbarhost

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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

