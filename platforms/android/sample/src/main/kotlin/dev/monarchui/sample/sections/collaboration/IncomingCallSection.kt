package dev.monarchui.sample.sections.collaboration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.feedback.banner.MonarchBannerAnnouncement
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/**
 * The catalogue page of IncomingCall. The system draws the ringing screen from
 * a NotificationCompat.CallStyle notification; the catalogue does not hold the
 * notification permissions, so it shows the in-app banner only.
 */
object IncomingCallSection : CataloguePage {
    override val title = "IncomingCall"

    @Composable
    override fun Content() {
        var ringing by rememberSaveable { mutableStateOf(true) }
        var status by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("The system ringing screen")
        MonarchText(
            text = "The app posts NotificationCompat.CallStyle.forIncomingCall; Android shows it as a heads-up " +
                "or full-screen call, also on the lock screen.",
            style = MonarchTextStyle.Caption,
            color = MonarchTheme.colors.textMuted,
        )
        CatalogueCaption("In-app banner, while the app is in the foreground")
        if (ringing) {
            MonarchBanner(
                message = "Ana Flores is calling.",
                title = "Incoming call",
                announcement = MonarchBannerAnnouncement.Polite,
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                        MonarchButton(
                            text = "Decline",
                            onClick = {
                                ringing = false
                                status = "Declined"
                            },
                            variant = MonarchButtonVariant.Danger,
                            size = MonarchButtonSize.Sm,
                        )
                        MonarchButton(
                            text = "Answer",
                            onClick = {
                                ringing = false
                                status = "Answered"
                            },
                            variant = MonarchButtonVariant.Primary,
                            size = MonarchButtonSize.Sm,
                        )
                    }
                },
            )
        } else {
            MonarchText(text = status)
            MonarchButton(text = "Ring again", onClick = { ringing = true })
        }
    }
}
