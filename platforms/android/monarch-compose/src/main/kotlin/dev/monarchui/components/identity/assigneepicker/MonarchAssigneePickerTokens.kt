package dev.monarchui.components.identity.assigneepicker

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerEntity
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAssigneePicker. */
internal object MonarchAssigneePickerTokens {
    /** The gap between the field and the Assign to me shortcut. */
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /** [people] with the current user first; everyone else keeps the app's order. */
    fun meFirst(people: List<MonarchAssigneePerson>, currentUserId: String?): List<MonarchAssigneePerson> {
        if (people.any { it.group != null }) return people
        val me = people.firstOrNull { it.id == currentUserId } ?: return people
        return listOf(me) + people.filter { it !== me }
    }

    /** A person as an entity of the picker, with "(you)" after the current user's name. */
    fun entity(
        person: MonarchAssigneePerson,
        currentUserId: String?,
        you: (String) -> String,
    ): MonarchEntityPickerEntity =
        MonarchEntityPickerEntity(
            id = person.id,
            name = if (person.id == currentUserId) you(person.name) else person.name,
            description = person.email,
            hint = person.hint,
            image = person.image,
            keywords = listOfNotNull(person.name, person.email).joinToString(" "),
            enabled = person.enabled,
            group = person.group,
            status = person.status,
        )

    /** Whether Assign to me is offered: there is a current user who is not chosen yet. */
    fun offersAssignToMe(currentUserId: String?, show: Boolean, enabled: Boolean, chosen: List<String>): Boolean =
        show && enabled && currentUserId != null && currentUserId !in chosen
}
