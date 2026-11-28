package dev.monarchui.sample.sections.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.forms.combobox.MonarchCombobox
import dev.monarchui.components.forms.combobox.MonarchComboboxOption
import dev.monarchui.components.forms.combobox.MonarchComboboxPresentation
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import java.util.Locale

/** The catalogue page of Combobox. */
object MonarchComboboxSection : CataloguePage {
    override val title = "Combobox"

    private const val COUNTRY_LIMIT = 50

    private val people = listOf(
        MonarchComboboxOption("ada", "Ada Lovelace", hint = "ada@example.com", group = "Team"),
        MonarchComboboxOption("alan", "Alan Turing", description = "On leave", enabled = false, group = "Team"),
        MonarchComboboxOption("grace", "Grace Hopper", hint = "grace@example.com", group = "Team"),
        MonarchComboboxOption("katherine", "Katherine Johnson", group = "Guests"),
    )

    private val countries = Locale.getISOCountries().map { code ->
        MonarchComboboxOption(code, Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.US), hint = code)
    }.sortedBy { it.label }

    @Composable
    override fun Content() {
        var assignee by rememberSaveable { mutableStateOf<String?>(null) }
        var country by rememberSaveable { mutableStateOf<String?>("DE") }
        var label by rememberSaveable { mutableStateOf<String?>(null) }
        val labels = remember {
            mutableStateListOf(MonarchComboboxOption("bug", "Bug"), MonarchComboboxOption("docs", "Docs"))
        }
        CatalogueCaption("A short list in a menu, with a clear row")
        MonarchCombobox(
            label = "Assignee",
            options = people,
            selected = assignee,
            onSelectedChange = { assignee = it },
            modifier = Modifier.fillMaxWidth(),
            clearOptionLabel = "Unassigned",
        )
        CatalogueCaption("A long list in a sheet, best matches first")
        MonarchCombobox(
            label = "Country",
            options = countries,
            selected = country,
            onSelectedChange = { country = it },
            modifier = Modifier.fillMaxWidth(),
            sortByMatch = true,
            limit = COUNTRY_LIMIT,
        )
        CatalogueCaption("Create a value that is not in the list")
        MonarchCombobox(
            label = "Label",
            options = labels,
            selected = label,
            onSelectedChange = { label = it },
            modifier = Modifier.fillMaxWidth(),
            presentation = MonarchComboboxPresentation.Sheet,
            onCreate = { text ->
                MonarchComboboxOption(text.lowercase(), text).also { labels += it }
            },
        )
    }
}
