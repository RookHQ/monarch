package dev.monarchui.sample.sections.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarGroup
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.identity.presencedot.MonarchPresenceStatus
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sample.CataloguePictures
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of Avatar. */
object MonarchAvatarSection : CataloguePage {
    override val title = "Avatar"

    private const val MAX_VISIBLE = 3
    private val People = listOf("Ana Silva", "Ben Okafor", "Chen Wei", "Dara Kim", "Eli Roth")

    @Composable
    override fun Content() {
        CatalogueCaption("Sizes, with initials")
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchAvatarSize.entries.forEach { size -> MonarchAvatar(image = null, name = "Ana Silva", size = size) }
        }
        CatalogueCaption("A picture from the app")
        MonarchAvatar(
            image = CataloguePictures.landscape(),
            name = "Rook",
            size = MonarchAvatarSize.Lg,
        )
        CatalogueCaption("Presence")
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchPresenceStatus.entries.forEachIndexed { index, status ->
                MonarchAvatar(image = null, name = People[index], status = status, size = MonarchAvatarSize.Lg)
            }
        }
        CatalogueCaption("An organization, with an icon")
        MonarchAvatar(
            image = null,
            name = "Rook Labs",
            shape = MonarchAvatarShape.Square,
            icon = {
                MonarchIcon(imageVector = SampleIcons.Building, contentDescription = null)
            },
        )
        CatalogueCaption("Beside the name")
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchAvatar(image = null, name = "Chen Wei", decorative = true)
            MonarchText(text = "Chen Wei")
        }
        CatalogueCaption("A group")
        MonarchAvatarGroup(items = People, label = "Assignees", maxVisible = MAX_VISIBLE) { name ->
            MonarchAvatar(image = null, name = name)
        }
    }
}
