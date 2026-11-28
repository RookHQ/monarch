package dev.monarchui.components.technical.codesnippettabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.navigation.tabs.MonarchTabs
import dev.monarchui.components.typography.codeblock.MonarchCodeBlock
import dev.monarchui.components.typography.codeblock.MonarchCodeBlockLabels
import dev.monarchui.internal.monarchString

/**
 * The same code example in several languages or tools, one tab each, with
 * the copy button of `MonarchCodeBlock`, optionally kept on the reader's
 * language across every set that shares a [syncKey].
 *
 * Built on `MonarchTabs` over a
 * `MonarchCodeBlock`. With one snippet only the code block is shown; with
 * none, nothing. With [syncKey], choosing a tab switches every set on screen
 * with the same key that has a snippet with that id, and the choice is kept
 * in `SharedPreferences`, so it is restored on the next launch. The app's
 * [selection] wins over the synced choice.
 *
 * @param snippets the same example in each language, in tab order. A repeated id is left out.
 * @param modifier the modifier of the tabs and the code.
 * @param label the spoken name of the tab row. Built in: "Language".
 * @param selection the id of the snippet to show, when the app controls it. An id no snippet has falls back to
 * the synced or own choice, then the first snippet.
 * @param onSelectionChange called with the id of the snippet the reader chose.
 * @param syncKey keeps every set with this key on the same language and remembers it under
 * `monarch.codeSnippetTabs.<key>` in the `monarch_code_snippet_tabs` preferences.
 * @param showLineNumbers whether the code shows line numbers.
 * @param onCopy called with the copied code after each copy.
 * @param codeBlockLabels replacements for the code block's built-in strings (Copy, Copied, Wrap, …).
 */
@Composable
public fun MonarchCodeSnippetTabs(
    snippets: List<MonarchCodeSnippet>,
    modifier: Modifier = Modifier,
    label: String? = null,
    selection: String? = null,
    onSelectionChange: ((String) -> Unit)? = null,
    syncKey: String? = null,
    showLineNumbers: Boolean = false,
    onCopy: ((String) -> Unit)? = null,
    codeBlockLabels: MonarchCodeBlockLabels = MonarchCodeBlockLabels(),
) {
    val tokens = MonarchCodeSnippetTabsTokens
    val context = LocalContext.current
    val distinct = tokens.distinct(snippets)
    var own by rememberSaveable { mutableStateOf<String?>(null) }
    val synced = syncKey?.let { MonarchCodeSnippetTabsSync.choice(context, it) }
    val current = tokens.resolve(distinct, selection, synced, own) ?: return
    val snippet = distinct.first { it.id == current }
    val choose: (String) -> Unit = { id ->
        own = id
        syncKey?.let { MonarchCodeSnippetTabsSync.choose(context, it, id) }
        onSelectionChange?.invoke(id)
    }
    val name = monarchString(label, R.string.monarch_codesnippettabs_label)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        if (distinct.size > 1) {
            MonarchTabs(
                selectedIndex = distinct.indexOf(snippet),
                onSelectedIndexChange = { index -> choose(distinct[index].id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = name },
            ) {
                distinct.forEach { item -> tab(item.label) }
            }
        }
        // A new code block per snippet, so its wrap toggle starts afresh.
        key(snippet.id) {
            MonarchCodeBlock(
                code = snippet.code,
                modifier = Modifier.fillMaxWidth(),
                language = snippet.language,
                title = snippet.title ?: snippet.label,
                showLineNumbers = showLineNumbers,
                onCopy = onCopy,
                labels = codeBlockLabels,
            )
        }
    }
}
