package dev.monarchui.components.overlays.tooltip

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

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
