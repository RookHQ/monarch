package com.rookhq.monarch.sample.sections.overlays

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
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.navigation.link.MonarchLink
import com.rookhq.monarch.components.navigation.link.MonarchLinkVariant
import com.rookhq.monarch.components.overlays.popover.MonarchPopover
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

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
