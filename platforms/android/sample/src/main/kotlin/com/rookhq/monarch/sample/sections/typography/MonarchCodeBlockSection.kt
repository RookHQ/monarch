package com.rookhq.monarch.sample.sections.typography

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.typography.codeblock.MonarchCodeBlock
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of CodeBlock. */
object MonarchCodeBlockSection : CataloguePage {
    override val title = "CodeBlock"

    @Composable
    override fun Content() {
        var wrapped by rememberSaveable { mutableStateOf(false) }
        var copies by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("An API request")
        MonarchCodeBlock(
            code = "curl https://api.example.com/v1/items \\\n  -H \"Authorization: Bearer \$TOKEN\"",
            language = "bash",
            title = "List items",
            onCopy = { copies++ },
        )
        CatalogueCaption("Line numbers, wrap state held by the page")
        MonarchCodeBlock(
            code = "export const client = createClient({ retries: 3, timeout: 2500, " +
                "baseURL: \"https://api.example.com\" });\n" +
                "// Reuse one client per app.\nexport default client;",
            language = "ts",
            title = "client.ts",
            showLineNumbers = true,
            wrapped = wrapped,
            onWrappedChange = { wrapped = it },
        )
        CatalogueCaption("A diff")
        MonarchCodeBlock(
            code = "--- a/server.ts\n+++ b/server.ts\n@@ -1,2 +1,2 @@\n" +
                "-const port = 3000;\n+const port = 8080;\n app.listen(port);",
            language = "diff",
            title = "server.ts",
        )
        CatalogueCaption("Markup and JSON")
        MonarchCodeBlock(code = "<a href=\"/docs\" class=\"link\">Docs</a> <!-- nav -->", language = "html")
        MonarchCodeBlock(code = "{ \"name\": \"monarch\", \"private\": true, \"version\": 1 }", language = "json")
        CatalogueCaption("No header")
        MonarchCodeBlock(code = "SELECT id, email FROM users WHERE active;", showWrapToggle = false, copyable = false)
        MonarchText(text = "Copies: $copies", color = MonarchTheme.colors.textMuted)
    }
}
