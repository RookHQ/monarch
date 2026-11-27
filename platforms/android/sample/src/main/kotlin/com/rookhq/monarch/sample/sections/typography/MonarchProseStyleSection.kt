package com.rookhq.monarch.sample.sections.typography

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.typography.inlinecodestyle.MonarchInlineCodeStyle
import com.rookhq.monarch.components.typography.prosestyle.MonarchProse
import com.rookhq.monarch.components.typography.prosestyle.MonarchProseSize
import com.rookhq.monarch.components.typography.prosestyle.MonarchProseStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of Prose. */
object MonarchProseStyleSection : CataloguePage {
    override val title = "Prose"

    @Composable
    override fun Content() {
        var size by rememberSaveable { mutableStateOf(MonarchProseSize.Md) }
        CatalogueCaption("Base size")
        MonarchSegmentedControl {
            MonarchProseSize.entries.forEachIndexed { index, option ->
                Segment(
                    selected = size == option,
                    onClick = { size = option },
                ) { Text(option.name) }
            }
        }
        val style = MonarchProseStyle(size)
        val links = style.linkStyles()
        val intro = buildAnnotatedString {
            append("Invoices now show the customer's tax number. Read the ")
            withLink(LinkAnnotation.Clickable("guide", links) {}) { append("billing guide") }
            append(" or run ")
            append(MonarchInlineCodeStyle.annotatedString("rook sync"))
            append(" to update.")
        }
        CatalogueCaption("Release notes")
        MonarchProse(style = style) {
            heading("Release notes", level = 1)
            paragraph(intro)
            heading("Fixed", level = 2)
            listItem("Search finds archived projects")
            listItem("Exports keep the time zone of the workspace", depth = 1)
            heading("To update", level = 3)
            listItem("Download the new version", marker = "1.")
            listItem("Restart the app", marker = "2.")
            quote(AnnotatedString("Faster than I expected, and the exports finally match."))
            code("rook sync --workspace acme --since 2026-01-01")
            rule()
            caption("Version 4.2")
        }
    }
}
