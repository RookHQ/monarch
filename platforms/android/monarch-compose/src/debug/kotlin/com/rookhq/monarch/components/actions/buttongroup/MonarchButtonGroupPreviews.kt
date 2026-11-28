package com.rookhq.monarch.components.actions.buttongroup

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchButtonGroupVariantsPreview() {
    PreviewFrame {
        MonarchButtonVariant.entries.forEach { variant ->
            MonarchButtonGroup(variant = variant, label = "Pages") {
                button(text = "Previous", onClick = {})
                button(text = "Today", onClick = {})
                button(text = "Next", onClick = {}, enabled = false)
            }
        }
    }
}

@MonarchPreviews
@Composable
internal fun MonarchButtonGroupLayoutsPreview() {
    PreviewFrame {
        MonarchButtonGroup(orientation = Orientation.Vertical, label = "Zoom") {
            iconButton(contentDescription = "Zoom in", onClick = {}) {
                MonarchIcon(imageVector = PreviewIcon, contentDescription = null)
            }
            iconButton(contentDescription = "Zoom out", onClick = {}) {
                MonarchIcon(imageVector = PreviewIcon, contentDescription = null)
            }
        }
        MonarchButtonGroup(attached = false, fullWidth = true) {
            button(text = "Cancel", onClick = {})
            button(text = "Save and continue to the next step", onClick = {})
        }
    }
}
