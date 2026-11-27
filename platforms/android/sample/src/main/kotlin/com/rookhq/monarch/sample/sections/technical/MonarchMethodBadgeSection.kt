package com.rookhq.monarch.sample.sections.technical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.rookhq.monarch.components.technical.methodbadge.MonarchMethodBadge
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

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
