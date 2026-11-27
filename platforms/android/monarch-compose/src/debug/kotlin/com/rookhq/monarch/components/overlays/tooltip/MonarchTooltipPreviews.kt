package com.rookhq.monarch.components.overlays.tooltip

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchTooltipPreview() {
    PreviewFrame {
        MonarchTooltip(text = "Add to favourites") {
            MonarchIconButton(label = "Add to favourites", onClick = {}) {
                MonarchIcon(imageVector = PreviewIcon, contentDescription = null)
            }
        }
    }
}
