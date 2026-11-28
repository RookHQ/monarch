package dev.monarchui.sample.sections.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.navigation.link.MonarchLinkVariant
import dev.monarchui.components.overlays.popover.MonarchPopover
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of HoverCard, a recipe: tapping a name opens its preview in MonarchPopover. */
object HoverCardSection : CataloguePage {
    override val title = "HoverCard"

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var messaged by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Tap the name to preview the person")
        Box {
            MonarchLink(text = "Ada Lovelace", onClick = { open = true }, variant = MonarchLinkVariant.Standalone)
            MonarchPopover(expanded = open, onDismissRequest = { open = false }, label = "Ada Lovelace") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonarchAvatar(image = null, name = "Ada Lovelace", size = MonarchAvatarSize.Lg, decorative = true)
                    Column {
                        MonarchText(text = "Ada Lovelace", style = MonarchTextStyle.BodyStrong)
                        MonarchText(
                            text = "Engineering lead, London",
                            style = MonarchTextStyle.Caption,
                            color = MonarchTheme.colors.textMuted,
                        )
                    }
                }
                MonarchButton(
                    text = "Send message",
                    onClick = {
                        messaged = true
                        open = false
                    },
                    size = MonarchButtonSize.Sm,
                )
            }
        }
        if (messaged) {
            MonarchText(text = "Message window opened.", style = MonarchTextStyle.Caption)
        }
    }
}
