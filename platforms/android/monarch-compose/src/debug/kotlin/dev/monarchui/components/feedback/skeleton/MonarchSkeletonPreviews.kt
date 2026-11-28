package dev.monarchui.components.feedback.skeleton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchSkeletonPreview() {
    PreviewFrame {
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
            MonarchSkeleton(shape = MonarchSkeletonShape.Circle)
            Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchSkeleton(shape = MonarchSkeletonShape.Text)
                MonarchSkeleton(modifier = Modifier.fillMaxWidth(0.6f), shape = MonarchSkeletonShape.Text)
            }
        }
        MonarchSkeleton()
    }
}
