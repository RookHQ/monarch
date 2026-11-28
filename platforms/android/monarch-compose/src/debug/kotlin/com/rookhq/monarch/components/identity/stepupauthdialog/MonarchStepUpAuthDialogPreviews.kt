package com.rookhq.monarch.components.identity.stepupauthdialog

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchStepUpAuthDialogPreview() {
    PreviewFrame {
        MonarchStepUpAuthDialog(
            onVerify = {},
            description = "Deleting a workspace needs you to confirm it’s you.",
            methods = listOf(MonarchStepUpMethod.Password, MonarchStepUpMethod.Code, MonarchStepUpMethod.Passkey),
            onForgot = {},
            onCancel = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchStepUpAuthDialogDevicePreview() {
    PreviewFrame {
        MonarchStepUpAuthDialog(
            onVerify = {},
            methods = listOf(MonarchStepUpMethod.DeviceOwner),
            onRequestBiometric = { true },
            onCancel = {},
        )
    }
}
