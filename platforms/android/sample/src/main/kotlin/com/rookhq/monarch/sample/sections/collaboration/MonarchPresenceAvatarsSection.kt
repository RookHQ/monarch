package com.rookhq.monarch.sample.sections.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.collaboration.presenceavatars.MonarchPresenceAvatars
import com.rookhq.monarch.components.collaboration.presenceavatars.MonarchPresencePerson
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of PresenceAvatars. */
object MonarchPresenceAvatarsSection : CataloguePage {
    override val title = "PresenceAvatars"

    private const val FewPeople = 3

    private val people = listOf(
        MonarchPresencePerson(id = "1", name = "Ana Silva", activity = "Editing"),
        MonarchPresencePerson(
            id = "2",
            name = "Ben Okafor",
            status = MonarchPresenceStatus.Away,
            customStatus = "Lunch",
        ),
        MonarchPresencePerson(
            id = "3",
            name = "Chloé Martin",
            status = MonarchPresenceStatus.Busy,
            activity = "Viewing slide 4",
        ),
        MonarchPresencePerson(id = "4", name = "Dev Patel", status = MonarchPresenceStatus.DoNotDisturb),
        MonarchPresencePerson(id = "5", name = "Eun-ji Park", status = MonarchPresenceStatus.Offline),
        MonarchPresencePerson(id = "6", name = "Femi Adeyemi", activity = "Commenting"),
    )

    @Composable
    override fun Content() {
        var followed by rememberSaveable { mutableStateOf("nobody") }
        CatalogueCaption("Everyone shown")
        MonarchPresenceAvatars(people = people.take(FewPeople))
        CatalogueCaption("More people than room: tap to list everyone")
        MonarchPresenceAvatars(people = people)
        CatalogueCaption("Tap a person in the list to follow them")
        MonarchPresenceAvatars(
            people = people,
            size = MonarchAvatarSize.Md,
            onPersonClick = { followed = it.name },
        )
        MonarchText(text = "Following $followed")
    }
}
