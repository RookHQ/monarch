package com.rookhq.monarch.components.feedback.banner

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
