package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.intelligence.aimarkdown.MonarchAiMarkdown
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private val AnswerParts = listOf(
    "## Revenue in Q3\n\n**Revenue** grew *12%* over the quarter, see [the report](https://example.com/q3).[^1]\n\n",
    "1. The Nordics led with 18%\n2. Pricing changed in August\n   - Team plan +€2\n\n",
    "| Region | Growth |\n| :- | -: |\n| Nordics | 18% |\n| South | 7% |\n\n",
    "- [x] Draft sent to finance\n- [ ] Board review\n\n> Keep the board summary under a page.\n\n",
    "```sql\nselect region, sum(amount) from invoices group by region;\n```\n\n",
    "[^1]: Finance dashboard, 30 September.",
)

/** The catalogue page of AiMarkdown. */
object MonarchAiMarkdownSection : CataloguePage {
    override val title = "AiMarkdown"

    @Composable
    override fun Content() {
        var parts by rememberSaveable { mutableIntStateOf(2) }
        val streaming = parts < AnswerParts.size
        CatalogueCaption("A model answer, streamed one part at a time")
        MonarchButton(
            text = if (streaming) "Stream the next part" else "Start again",
            onClick = { parts = if (streaming) parts + 1 else 1 },
        )
        MonarchAiMarkdown(text = AnswerParts.take(parts).joinToString(""), streaming = streaming)
        CatalogueCaption("Untrusted input: raw HTML stays text, unsafe links stay text, hosts are shown")
        MonarchAiMarkdown(
            text = "<b>bold?</b> [Run me](javascript:alert(1)) [paypal.com](https://evil.test/login)",
        )
    }
}
