package dev.monarchui.sample.sections.forms

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import dev.monarchui.components.forms.entitypicker.MonarchEntityPicker
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerEntity
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import kotlinx.coroutines.delay

/** The catalogue page of EntityPicker: a local list, and a search that waits like a server. */
object MonarchEntityPickerSection : CataloguePage {
    override val title = "EntityPicker"

    private const val CLOSE_ANGLE = 45f
    private const val SERVER_DELAY = 600L

    private val people = listOf(
        MonarchEntityPickerEntity("ada", "Ada Lovelace", description = "Engineering", hint = "ada@example.com"),
        MonarchEntityPickerEntity("grace", "Grace Hopper", description = "Research", hint = "grace@example.com"),
        MonarchEntityPickerEntity("katherine", "Katherine Johnson", description = "Flight"),
        MonarchEntityPickerEntity("alan", "Alan Turing", description = "On leave", enabled = false),
    )

    private val companies = listOf(
        MonarchEntityPickerEntity("acme", "Acme", description = "Customer", shape = MonarchAvatarShape.Square),
        MonarchEntityPickerEntity("globex", "Globex", description = "Prospect", shape = MonarchAvatarShape.Square),
        MonarchEntityPickerEntity("initech", "Initech", description = "Partner", shape = MonarchAvatarShape.Square),
    )

    @Composable
    override fun Content() {
        var owner by rememberSaveable { mutableStateOf<String?>("ada") }
        var reviewers by rememberSaveable { mutableStateOf(listOf<String>()) }
        var company by rememberSaveable { mutableStateOf<String?>(null) }
        MonarchEntityPicker(
            label = "Owner",
            entities = people,
            selected = owner,
            onSelectedChange = { owner = it },
            clearOptionLabel = "Unassigned",
            clearIcon = {
                Icon(SampleIcons.Close, contentDescription = null, modifier = Modifier.rotate(CLOSE_ANGLE))
            },
        )
        CatalogueCaption("Several")
        MonarchEntityPicker(
            label = "Reviewers",
            entities = people,
            selected = reviewers,
            onSelectedChange = { reviewers = it },
            helperText = "They are notified when the draft is ready.",
        )
        CatalogueCaption("Searched on a server, with create")
        MonarchEntityPicker(
            label = "Company",
            entities = companies,
            selected = company,
            onSelectedChange = { company = it },
            minSearchLength = 2,
            search = { text ->
                delay(SERVER_DELAY)
                companies.filter { it.name.contains(text, ignoreCase = true) }
            },
            onCreate = { name -> MonarchEntityPickerEntity(name.lowercase(), name, shape = MonarchAvatarShape.Square) },
        )
        CatalogueCaption("Disabled")
        MonarchEntityPicker(
            label = "Owner",
            entities = people,
            selected = "grace",
            onSelectedChange = { _: String? -> },
            enabled = false,
        )
    }
}
