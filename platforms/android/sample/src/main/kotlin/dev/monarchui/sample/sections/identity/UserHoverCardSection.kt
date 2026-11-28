package dev.monarchui.sample.sections.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.components.feedback.skeleton.MonarchSkeletonShape
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.identity.presencedot.MonarchPresenceStatus
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.overlays.popover.MonarchPopover
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

private const val SimulatedFetchMillis = 700L

/** The catalogue page of UserHoverCard: tap the name; details load after a moment. */
object UserHoverCardSection : CataloguePage {
    override val title = "UserHoverCard"

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var loading by remember { mutableStateOf(true) }
        LaunchedEffect(open) {
            if (open) {
                loading = true
                delay(SimulatedFetchMillis)
                loading = false
            }
        }
        CatalogueCaption("Assigned to")
        Box {
            MonarchLink(text = "Priya Raman", onClick = { open = true }, actionLabel = "show profile preview")
            MonarchPopover(expanded = open, onDismissRequest = { open = false }, label = "Profile of Priya Raman") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonarchAvatar(
                        image = null,
                        name = "Priya Raman",
                        size = MonarchAvatarSize.Lg,
                        status = MonarchPresenceStatus.Online,
                        seed = "user_42",
                    )
                    Column {
                        MonarchText(text = "Priya Raman", style = MonarchTextStyle.Subheading, heading = true)
                        MonarchText(text = "@priya · Design lead", style = MonarchTextStyle.Caption)
                    }
                }
                if (loading) {
                    MonarchSkeleton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Loading profile" },
                        shape = MonarchSkeletonShape.Text,
                    )
                } else {
                    MonarchText(text = "🌴 On holiday until Monday", style = MonarchTextStyle.Caption)
                    MonarchText(text = "Lisbon · 14:05 local time", style = MonarchTextStyle.Caption)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                    MonarchButton(text = "Message", onClick = { open = false }, size = MonarchButtonSize.Sm)
                    MonarchButton(
                        text = "View profile",
                        onClick = { open = false },
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                }
            }
        }
    }
}
