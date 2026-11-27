package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.intelligence.aicodeblock.MonarchAiCodeBlock
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

private const val SampleCode = "import { createServer } from 'node:http';\n\n" +
    "export const port = Number(process.env.PORT ?? 8080);\n\n" +
    "createServer((req, res) => res.end('ok')).listen(port);\n"
private const val SampleDiff = "@@ -1,3 +1,3 @@\n import { createServer } from 'node:http';\n" +
    "-export const port = 3000;\n+export const port = Number(process.env.PORT ?? 8080);\n"

/** The catalogue page of AiCodeBlock. */
object MonarchAiCodeBlockSection : CataloguePage {
    override val title = "AiCodeBlock"

    @Composable
    override fun Content() {
        var result by rememberSaveable { mutableStateOf<String?>(null) }
        var incomplete by rememberSaveable { mutableStateOf(true) }
        CatalogueCaption("Apply and Insert hand the code to the app")
        MonarchAiCodeBlock(
            code = SampleCode,
            language = "ts",
            title = "server.ts",
            showLineNumbers = true,
            onInsert = { result = "Inserted ${it.lines().size} lines" },
            onApply = { result = "Applied server.ts" },
            actions = {
                MonarchButton(
                    text = "Explain",
                    onClick = { result = "Explain was pressed" },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            },
        )
        result?.let { MonarchText(text = it) }
        CatalogueCaption("A diff that is still arriving")
        MonarchAiCodeBlock(code = SampleDiff, language = "diff", incomplete = incomplete, onApply = {})
        MonarchButton(
            text = if (incomplete) "Finish streaming" else "Stream again",
            onClick = { incomplete = !incomplete },
        )
    }
}
