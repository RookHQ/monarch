package dev.monarchui.components.intelligence.aimarkdown

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val Answer = "## Revenue\n\n**Revenue** grew *12%* in Q3, " +
    "see [the report](https://example.com/q3).[^1]\n\n" +
    "1. Nordics led\n2. Pricing changed\n   - in August\n\n- [x] Draft sent\n- [ ] Board review\n\n" +
    "> Keep the summary short.\n\n| Region | Growth |\n| :- | -: |\n| Nordics | 18% |\n| South | 7% |\n\n" +
    "```ts\nconst growth = 0.12;\n```\n\n[^1]: Finance dashboard, 30 September."

@MonarchPreviews
@Composable
internal fun MonarchAiMarkdownPreview() {
    PreviewFrame {
        MonarchAiMarkdown(text = Answer)
    }
}
