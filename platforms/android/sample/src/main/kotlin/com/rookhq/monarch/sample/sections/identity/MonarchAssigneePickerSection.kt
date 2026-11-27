package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.identity.assigneepicker.MonarchAssigneePerson
import com.rookhq.monarch.components.identity.assigneepicker.MonarchAssigneePicker
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val SamplePeople = listOf(
    MonarchAssigneePerson("ben", "Ben Okafor", email = "ben@example.com", hint = "3 open"),
    MonarchAssigneePerson("ada", "Ada Moss", email = "ada@example.com"),
    MonarchAssigneePerson("chloe", "Chloé Martin", email = "chloe@example.com", hint = "Away"),
    MonarchAssigneePerson("dan", "Dan Iwu", email = "dan@example.com", enabled = false),
)

/** The catalogue page of AssigneePicker. */
object MonarchAssigneePickerSection : CataloguePage {
    override val title = "AssigneePicker"

    @Composable
    override fun Content() {
        var one by rememberSaveable { mutableStateOf<String?>(null) }
        var several by rememberSaveable { mutableStateOf(listOf("ben")) }
        CatalogueCaption("One assignee; you are Ada")
        MonarchAssigneePicker(
            people = SamplePeople,
            selected = one,
            onSelectedChange = { one = it },
            currentUserId = "ada",
        )
        CatalogueCaption("Several assignees")
        MonarchAssigneePicker(
            people = SamplePeople,
            selected = several,
            onSelectedChange = { several = it },
            currentUserId = "ada",
        )
        CatalogueCaption("Required, without Unassigned")
        MonarchAssigneePicker(
            people = SamplePeople,
            selected = null,
            onSelectedChange = {},
            required = true,
            allowUnassigned = false,
            errorText = "Choose someone to own this task.",
        )
        CatalogueCaption("Disabled")
        MonarchAssigneePicker(people = SamplePeople, selected = "chloe", onSelectedChange = {}, enabled = false)
    }
}
