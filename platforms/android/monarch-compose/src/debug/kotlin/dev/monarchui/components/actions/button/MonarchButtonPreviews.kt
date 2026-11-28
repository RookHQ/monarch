package dev.monarchui.components.actions.button

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

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
