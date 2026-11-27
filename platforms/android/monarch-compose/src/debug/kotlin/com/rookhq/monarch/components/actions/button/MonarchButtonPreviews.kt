package com.rookhq.monarch.components.actions.button

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchButtonVariantsPreview() {
    PreviewFrame {
        MonarchButtonVariant.entries.forEach { variant ->
            MonarchButton(text = variant.name, onClick = {}, variant = variant)
        }
        MonarchButton(text = "Disabled", onClick = {}, variant = MonarchButtonVariant.Primary, enabled = false)
    }
}

@MonarchPreviews
@Composable
internal fun MonarchButtonSizesPreview() {
    PreviewFrame {
        MonarchButtonSize.entries.forEach { size ->
            MonarchButton(
                text = size.name,
                onClick = {},
                variant = MonarchButtonVariant.Primary,
                size = size,
                leadingIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
            )
        }
        MonarchButton(text = "Saving", onClick = {}, variant = MonarchButtonVariant.Primary, loading = true)
    }
}
