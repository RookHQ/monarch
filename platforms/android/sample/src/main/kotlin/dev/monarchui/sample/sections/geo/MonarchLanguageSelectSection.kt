package dev.monarchui.sample.sections.geo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.geo.languageselect.MonarchLanguageSelect
import dev.monarchui.components.geo.languageselect.MonarchLanguageSelectLabelStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of LanguageSelect. */
object MonarchLanguageSelectSection : CataloguePage {
    override val title = "LanguageSelect"

    @Composable
    override fun Content() {
        var language by rememberSaveable { mutableStateOf<String?>("de") }
        var translation by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Each language under its own name")
        MonarchLanguageSelect(
            selected = language,
            onSelectedChange = { language = it },
            modifier = Modifier.fillMaxWidth(),
            priority = listOf("en", "es"),
        )
        CatalogueCaption("The product's languages, by interface name, with tags")
        MonarchLanguageSelect(
            selected = translation,
            onSelectedChange = { translation = it },
            modifier = Modifier.fillMaxWidth(),
            languages = listOf("en", "fr", "pt-BR", "ja"),
            labelStyle = MonarchLanguageSelectLabelStyle.Localized,
            showTag = true,
        )
    }
}
