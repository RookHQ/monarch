package dev.monarchui.sample.sections.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.files.embedblock.MonarchEmbedBlock
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of EmbedBlock: paste a link to see whether it is framed, carded or refused. */
object MonarchEmbedBlockSection : CataloguePage {
    override val title = "EmbedBlock"

    @Composable
    override fun Content() {
        var url by rememberSaveable { mutableStateOf("https://vimeo.com/76979871") }
        MonarchTextField(value = url, onValueChange = { url = it }, label = "Link to embed")
        MonarchEmbedBlock(url = url, caption = "Pasted link")
        CatalogueCaption("Any other web link")
        MonarchEmbedBlock(url = "https://example.com/changelog")
        CatalogueCaption("A link that cannot be shown")
        MonarchEmbedBlock(url = "javascript:alert(1)")
    }
}
