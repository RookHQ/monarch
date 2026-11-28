package dev.monarchui.sample.sections.feedback

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.feedback.banner.MonarchBannerAnnouncement
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CataloguePage

/** The catalogue page of Alert: the web's primary example, each tone with its default icon. */
object MonarchBannerSection : CataloguePage {
    override val title = "Alert"

    @Composable
    override fun Content() {
        MonarchBanner(
            modifier = Modifier.fillMaxWidth(),
            title = "Sync scheduled",
            message = "Rook ID members refresh every 15 minutes.",
            tone = MonarchTone.Info,
        )
        MonarchBanner(modifier = Modifier.fillMaxWidth(), title = "Saved", message = null, tone = MonarchTone.Success)
        MonarchBanner(
            modifier = Modifier.fillMaxWidth(),
            title = "Domain not verified",
            message = null,
            tone = MonarchTone.Warning,
            action = {
                MonarchButton(
                    text = "Verify",
                    onClick = {},
                    variant = MonarchButtonVariant.Secondary,
                    size = MonarchButtonSize.Sm,
                )
            },
        )
        MonarchBanner(
            modifier = Modifier.fillMaxWidth(),
            title = "Payment failed",
            message = "Update the card to keep the workspace active.",
            tone = MonarchTone.Danger,
            announcement = MonarchBannerAnnouncement.Assertive,
        )
        MonarchBanner(
            modifier = Modifier.fillMaxWidth(),
            message = "Exports run overnight.",
            tone = MonarchTone.Neutral,
        )
    }
}
