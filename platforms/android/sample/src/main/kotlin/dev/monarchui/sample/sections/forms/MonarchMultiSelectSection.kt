package dev.monarchui.sample.sections.forms

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.forms.combobox.MonarchComboboxOption
import dev.monarchui.components.forms.multiselect.MonarchMultiSelect
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of MultiSelect. */
object MonarchMultiSelectSection : CataloguePage {
    override val title = "MultiSelect"

    private const val MAX_TEAMS = 3

    private val teams = listOf(
        MonarchComboboxOption("design", "Design", group = "Product"),
        MonarchComboboxOption("eng", "Engineering", description = "Web and mobile", group = "Product"),
        MonarchComboboxOption("research", "Research", group = "Product"),
        MonarchComboboxOption("finance", "Finance", group = "Company"),
        MonarchComboboxOption(
            "legal",
            "Legal",
            description = "Not taking requests",
            enabled = false,
            group = "Company",
        ),
        MonarchComboboxOption("sales", "Sales", hint = "12 people", group = "Company"),
    )

    @Composable
    override fun Content() {
        var chosen by rememberSaveable { mutableStateOf(listOf("design")) }
        var limited by rememberSaveable { mutableStateOf(listOf<String>()) }
        val remove: @Composable () -> Unit = {
            Icon(SampleIcons.Close, contentDescription = null)
        }
        MonarchMultiSelect(
            label = "Teams",
            options = teams,
            selected = chosen,
            onSelectedChange = { chosen = it },
            helperText = "Everyone in these teams can see the project.",
            removeIcon = remove,
            onCreate = { MonarchComboboxOption(it.lowercase(), it) },
        )
        CatalogueCaption("At most three, required")
        MonarchMultiSelect(
            label = "Reviewing teams",
            options = teams,
            selected = limited,
            onSelectedChange = { limited = it },
            required = true,
            maxSelections = MAX_TEAMS,
            removeIcon = remove,
            errorText = if (limited.isEmpty()) "Choose at least one team." else null,
        )
        CatalogueCaption("Disabled")
        MonarchMultiSelect(
            label = "Teams",
            options = teams,
            selected = listOf("eng"),
            onSelectedChange = {},
            enabled = false,
        )
    }
}
