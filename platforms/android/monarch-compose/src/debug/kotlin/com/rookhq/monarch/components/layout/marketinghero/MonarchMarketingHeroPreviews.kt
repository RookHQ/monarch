package com.rookhq.monarch.components.layout.marketinghero

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchMarketingHeroPreview() {
    PreviewFrame {
        MonarchMarketingHero(
            title = "Write it once. Find it whenever.",
            eyebrow = "Rook Example",
            lead = "Structured documents for teams whose decisions need to stay connected.",
            note = "Set up in under five minutes.",
        ) {
            MonarchButton(
                text = "Start writing",
                onClick = {},
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Lg,
                shape = MonarchTheme.shapes.full,
            )
        }
    }
}
