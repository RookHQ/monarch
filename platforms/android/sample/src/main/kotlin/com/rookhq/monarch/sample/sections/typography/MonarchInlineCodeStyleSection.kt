package com.rookhq.monarch.sample.sections.typography

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import com.rookhq.monarch.components.typography.inlinecodestyle.MonarchInlineCodeStyle
import com.rookhq.monarch.components.typography.inlinecodestyle.MonarchInlineCodeVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of InlineCode. */
object MonarchInlineCodeStyleSection : CataloguePage {
    override val title = "InlineCode"

    @Composable
    override fun Content() {
        CatalogueCaption("Subtle, from backticks")
        MonarchText(text = MonarchInlineCodeStyle.markdown("Run `./gradlew build` before you push."))
        CatalogueCaption("Plain, in a caption")
        MonarchText(
            text = MonarchInlineCodeStyle.markdown("Exit code `127`", MonarchInlineCodeVariant.Plain),
            style = MonarchTextStyle.Caption,
        )
        CatalogueCaption("Built by hand")
        val code = MonarchInlineCodeStyle.annotatedString("MonarchTheme")
        MonarchText(
            text = buildAnnotatedString {
                append("Wrap the app in ")
                append(code)
                append(".")
            },
        )
    }
}
