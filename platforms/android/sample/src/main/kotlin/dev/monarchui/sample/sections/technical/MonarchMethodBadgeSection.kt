package dev.monarchui.sample.sections.technical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import dev.monarchui.components.technical.methodbadge.MonarchMethodBadge
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of MethodBadge. */
object MonarchMethodBadgeSection : CataloguePage {
    override val title = "MethodBadge"

    @Composable
    override fun Content() {
        CatalogueCaption("Endpoints")
        listOf(
            "GET" to "/v1/invoices",
            "POST" to "/v1/invoices",
            "PUT" to "/v1/invoices/{id}",
            "PATCH" to "/v1/invoices/{id}",
            "DELETE" to "/v1/invoices/{id}",
            "HEAD" to "/v1/health",
        ).forEach { (method, path) -> Endpoint(method, path) }
        CatalogueCaption("Tone override")
        MonarchMethodBadge(method = "purge", tone = MonarchTone.Danger)
    }

    @Composable
    private fun Endpoint(method: String, path: String) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchMethodBadge(method = method)
            MonarchText(text = path, style = MonarchTextStyle.Code)
        }
    }
}
