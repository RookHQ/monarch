package dev.monarchui.components.identity.stepupauthdialog

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
