package dev.monarchui.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.identity.spaceswitcher.MonarchSpaceOption
import dev.monarchui.components.identity.spaceswitcher.MonarchSpaceSwitcher
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val SampleSpaces = listOf(
    MonarchSpaceOption("eng", "Engineering", "18 members"),
    MonarchSpaceOption("mkt", "Marketing", "9 members"),
    MonarchSpaceOption("ops", "Operações", "4 members"),
    MonarchSpaceOption("fin", "Finance", "6 members"),
)

/** The catalogue page of SpaceSwitcher. */
object MonarchSpaceSwitcherSection : CataloguePage {
    override val title = "SpaceSwitcher"

    @Composable
    override fun Content() {
        var space by rememberSaveable { mutableStateOf<String?>(null) }
        var spaces by rememberSaveable { mutableStateOf(listOf<String>()) }
        CatalogueCaption("All spaces or one Space, with New Space and Manage Spaces")
        MonarchSpaceSwitcher(
            spaces = SampleSpaces + spaces.map { MonarchSpaceOption(it, it) },
            selection = space,
            onSelectionChange = { space = it },
            onCreate = { spaces = spaces + "Space ${spaces.size + 1}" },
            onManage = {},
        )
        CatalogueCaption("A Space must stay chosen; icon only")
        MonarchSpaceSwitcher(
            spaces = SampleSpaces,
            selection = space,
            onSelectionChange = { space = it },
            allowsAll = false,
            iconOnly = true,
        )
        CatalogueCaption("Loading")
        MonarchSpaceSwitcher(spaces = emptyList(), selection = null, onSelectionChange = {}, loading = true)
    }
}
