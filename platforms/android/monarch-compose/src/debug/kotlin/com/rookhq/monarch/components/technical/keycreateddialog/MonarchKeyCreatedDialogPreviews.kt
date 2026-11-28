package com.rookhq.monarch.components.technical.keycreateddialog

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchKeyCreatedDialogPreview() {
    PreviewFrame {
        MonarchKeyCreatedDialog(
            secret = "sk_live_51Hx9a2Lr8f3",
            onDone = {},
            name = "Production server",
            note = "The old key stops working in 24 hours.",
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchKeyCreatedDialogPublicPreview() {
    PreviewFrame {
        MonarchKeyCreatedDialog(
            secret = "pk_live_8Kd02",
            onDone = {},
            kind = MonarchKeyCreatedDialogKind.Public,
            label = "Publishable key",
        )
    }
}
