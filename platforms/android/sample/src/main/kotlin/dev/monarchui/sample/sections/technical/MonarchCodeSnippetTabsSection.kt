package dev.monarchui.sample.sections.technical

import androidx.compose.runtime.Composable
import dev.monarchui.components.technical.codesnippettabs.MonarchCodeSnippet
import dev.monarchui.components.technical.codesnippettabs.MonarchCodeSnippetTabs
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private const val SyncKey = "catalogue-api-docs"

private val ListItems = listOf(
    MonarchCodeSnippet(
        "cURL",
        code = "curl https://api.example.com/v1/items \\\n  -H \"Authorization: Bearer \$KEY\"",
        language = "bash",
    ),
    MonarchCodeSnippet("Kotlin", code = "val items = client.items.list()", language = "kotlin"),
    MonarchCodeSnippet("Swift", code = "let items = try await client.items.list()", language = "swift"),
)

private val CreateItem = listOf(
    MonarchCodeSnippet("cURL", code = "curl -X POST https://api.example.com/v1/items -d name=Pen", language = "bash"),
    MonarchCodeSnippet("Kotlin", code = "client.items.create(name = \"Pen\")", language = "kotlin"),
    MonarchCodeSnippet("Swift", code = "try await client.items.create(name: \"Pen\")", language = "swift"),
)

/** The catalogue page of CodeSnippetTabs: two sets that stay on the same language. */
object MonarchCodeSnippetTabsSection : CataloguePage {
    override val title = "CodeSnippetTabs"

    @Composable
    override fun Content() {
        CatalogueCaption("List items (synced)")
        MonarchCodeSnippetTabs(snippets = ListItems, syncKey = SyncKey)
        CatalogueCaption("Create an item (synced)")
        MonarchCodeSnippetTabs(snippets = CreateItem, syncKey = SyncKey, showLineNumbers = true)
        CatalogueCaption("One snippet")
        MonarchCodeSnippetTabs(snippets = ListItems.take(1))
    }
}
