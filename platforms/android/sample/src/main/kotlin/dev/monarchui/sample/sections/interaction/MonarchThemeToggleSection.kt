package dev.monarchui.sample.sections.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.interaction.themetoggle.MonarchThemeToggle
import dev.monarchui.components.interaction.themetoggle.MonarchThemeToggleSize
import dev.monarchui.components.interaction.themetoggle.MonarchThemeToggleVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchScheme

/**
 * The catalogue page of ThemeToggle. The catalogue pins its own theme, so the
 * choice is shown as text here; an app passes it to `MonarchTheme(scheme = …)`.
 */
object MonarchThemeToggleSection : CataloguePage {
    override val title = "ThemeToggle"

    @Composable
    override fun Content() {
        var scheme by rememberSaveable { mutableStateOf(MonarchScheme.System) }
        CatalogueCaption("Segmented, for a settings screen")
        MonarchThemeToggle(scheme = scheme, onSchemeChange = { scheme = it })
        MonarchText(text = "Chosen: ${scheme.name}")
        CatalogueCaption("Small")
        MonarchThemeToggle(scheme = scheme, onSchemeChange = { scheme = it }, size = MonarchThemeToggleSize.Sm)
        CatalogueCaption("Menu, for a toolbar")
        MonarchThemeToggle(
            scheme = scheme,
            onSchemeChange = { scheme = it },
            variant = MonarchThemeToggleVariant.Menu,
        )
        CatalogueCaption("Disabled")
        MonarchThemeToggle(scheme = scheme, onSchemeChange = {}, enabled = false)
    }
}
