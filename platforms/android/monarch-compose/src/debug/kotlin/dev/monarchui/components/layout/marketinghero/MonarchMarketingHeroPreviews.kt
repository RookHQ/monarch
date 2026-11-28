package dev.monarchui.components.layout.marketinghero

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

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
