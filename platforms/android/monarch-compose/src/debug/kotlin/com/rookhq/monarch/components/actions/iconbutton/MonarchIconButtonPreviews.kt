package com.rookhq.monarch.components.actions.iconbutton

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
