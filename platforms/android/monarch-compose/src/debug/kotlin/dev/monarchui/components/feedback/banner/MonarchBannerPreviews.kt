package dev.monarchui.components.feedback.banner

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchBannerTonesPreview() {
    PreviewFrame {
        MonarchTone.entries.forEach { tone ->
            MonarchBanner(message = "The ${tone.name.lowercase()} tone.", tone = tone)
        }
    }
}

@MonarchPreviews
@Composable
internal fun MonarchBannerFullPreview() {
    PreviewFrame {
        MonarchBanner(
            title = "Payment failed",
            message = "Your card was declined. Try another payment method.",
            tone = MonarchTone.Danger,
            icon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
            action = {
                MonarchButton(
                    text = "Retry",
                    onClick = {},
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            },
        )
    }
}
