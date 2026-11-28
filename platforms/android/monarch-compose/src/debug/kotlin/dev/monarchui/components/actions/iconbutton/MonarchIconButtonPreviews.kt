package dev.monarchui.components.actions.iconbutton

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchIconButtonPreview() {
    PreviewFrame {
        MonarchButtonSize.entries.forEach { size ->
            MonarchIconButton(label = size.name, onClick = {}, size = size) {
                MonarchIcon(imageVector = PreviewIcon, contentDescription = null)
            }
        }
        MonarchIconButton(label = "Add", onClick = {}, variant = MonarchButtonVariant.Primary) {
            MonarchIcon(imageVector = PreviewIcon, contentDescription = null)
        }
        MonarchIconButton(label = "Disabled", onClick = {}, enabled = false) {
            MonarchIcon(imageVector = PreviewIcon, contentDescription = null)
        }
    }
}
