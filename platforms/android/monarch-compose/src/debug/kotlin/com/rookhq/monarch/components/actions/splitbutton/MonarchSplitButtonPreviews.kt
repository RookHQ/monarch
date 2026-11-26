package com.rookhq.monarch.components.actions.splitbutton

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.sampleicons.SampleIcons

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
