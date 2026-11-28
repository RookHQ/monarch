package dev.monarchui.sample.sections.files

import androidx.compose.runtime.Composable
import dev.monarchui.components.files.columnsblock.MonarchColumnsBlock
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of ColumnsBlock. */
object MonarchColumnsBlockSection : CataloguePage {
    override val title = "ColumnsBlock"

    private const val RATIO = 0.35f

    @Composable
    override fun Content() {
        CatalogueCaption("Two columns, the first at 35%")
        MonarchColumnsBlock(ratio = RATIO) {
            column { MonarchText(text = "Before the launch", style = MonarchTextStyle.BodyStrong) }
            column {
                MonarchText(text = "Sign-ups doubled after the launch, mostly from the new referral page.")
            }
        }
        CatalogueCaption("Three equal columns (stacked on a phone)")
        MonarchColumnsBlock {
            column {
                MonarchText(text = "Plan", style = MonarchTextStyle.BodyStrong)
                MonarchText(text = "Agree the scope.")
            }
            column {
                MonarchText(text = "Build", style = MonarchTextStyle.BodyStrong)
                MonarchText(text = "Two sprints.")
            }
            column {
                MonarchText(text = "Ship", style = MonarchTextStyle.BodyStrong)
                MonarchText(text = "Release to everyone.")
            }
        }
    }
}
