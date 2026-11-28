package dev.monarchui.components.identity.signinwithrookbutton

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchSignInWithRookButtonPreview() {
    PreviewFrame {
        MonarchSignInWithRookButtonVariant.entries.forEach { variant ->
            MonarchSignInWithRookButton(onClick = {}, variant = variant)
        }
        MonarchSignInWithRookButtonSize.entries.forEach { size ->
            MonarchSignInWithRookButton(onClick = {}, size = size)
        }
        MonarchSignInWithRookButton(onClick = {}, loading = true)
        MonarchSignInWithRookButton(onClick = {}, enabled = false)
        MonarchSignInWithRookButton(onClick = {}, fullWidth = true)
    }
}
