package dev.monarchui.sample.sections.collaboration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.collaboration.readreceipt.MonarchReadReceipt
import dev.monarchui.components.collaboration.readreceipt.MonarchReadReceiptDisplay
import dev.monarchui.components.collaboration.readreceipt.MonarchReadReceiptReader
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of ReadReceipt. */
object MonarchReadReceiptSection : CataloguePage {
    override val title = "ReadReceipt"

    private const val ButtonReaders = 3

    private val readers = listOf(
        MonarchReadReceiptReader(name = "Ana Lima", id = "u1"),
        MonarchReadReceiptReader(name = "Ben Okafor", id = "u2"),
        MonarchReadReceiptReader(name = "Chloé Martin", id = "u3"),
        MonarchReadReceiptReader(name = "Dev Patel", id = "u4"),
        MonarchReadReceiptReader(name = "Eun-ji Park", id = "u5"),
    )

    @Composable
    override fun Content() {
        var opened by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("One-to-one: text")
        MonarchReadReceipt(readers = readers.take(1))
        CatalogueCaption("Group: count")
        MonarchReadReceipt(readers = readers, display = MonarchReadReceiptDisplay.Count)
        CatalogueCaption("Group: avatars, at most 3")
        MonarchReadReceipt(readers = readers, display = MonarchReadReceiptDisplay.Avatars)
        CatalogueCaption("As a button")
        MonarchReadReceipt(
            readers = readers.take(ButtonReaders),
            display = MonarchReadReceiptDisplay.Avatars,
            onShowReaders = { opened++ },
        )
        MonarchText(text = "Reader list opened $opened times")
    }
}
