package dev.monarchui.components.actions.splitbutton

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.sampleicons.SampleIcons

@MonarchPreviews
@Composable
internal fun MonarchSplitButtonPreview() {
    val arrow: @Composable () -> Unit = {
        MonarchIcon(SampleIcons.ChevronDown, null, size = MonarchIconSize.Sm)
    }
    PreviewFrame {
        MonarchButtonVariant.entries.forEach { variant ->
            MonarchSplitButton(text = "Save", onClick = {}, arrowIcon = arrow, variant = variant) { close ->
                MonarchMenuItem(text = "Save as draft", onClick = close)
            }
        }
        MonarchSplitButton("Save", onClick = {}, arrowIcon = arrow, size = MonarchButtonSize.Sm, loading = true) {}
        MonarchSplitButton(text = "Save", onClick = {}, arrowIcon = arrow, menuEnabled = false) {}
    }
}
