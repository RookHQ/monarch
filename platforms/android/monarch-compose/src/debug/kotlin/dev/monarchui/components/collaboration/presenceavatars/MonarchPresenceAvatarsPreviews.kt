package dev.monarchui.components.collaboration.presenceavatars

import androidx.compose.runtime.Composable
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.identity.presencedot.MonarchPresenceStatus
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewPeople = listOf(
    MonarchPresencePerson(id = "1", name = "Ana Silva", activity = "Editing"),
    MonarchPresencePerson(id = "2", name = "Ben Okafor", status = MonarchPresenceStatus.Away),
    MonarchPresencePerson(id = "3", name = "Chloé Martin", status = MonarchPresenceStatus.Busy),
    MonarchPresencePerson(id = "4", name = "Dev Patel", status = MonarchPresenceStatus.DoNotDisturb),
    MonarchPresencePerson(id = "5", name = "Eun-ji Park", status = MonarchPresenceStatus.Offline),
    MonarchPresencePerson(id = "6", name = "Femi Adeyemi"),
)

@MonarchPreviews
@Composable
internal fun MonarchPresenceAvatarsPreview() {
    PreviewFrame {
        MonarchPresenceAvatars(people = PreviewPeople.take(3))
        MonarchPresenceAvatars(people = PreviewPeople)
        MonarchPresenceAvatars(people = PreviewPeople, size = MonarchAvatarSize.Lg, maxVisible = 3)
    }
}
