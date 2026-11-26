package com.rookhq.monarch.components.intelligence.aiknowledgescope

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiKnowledgeScope`. Every
 * `null` uses the built-in string in `strings_aiknowledgescope.xml`.
 *
 * @property label the name TalkBack reads for the tree: "Knowledge sources".
 * @property kind the visible name of a kind: "Space", "Document", "App".
 * @property itemLabel writes the spoken name of a source from its label and kind name: "Handbook, Space".
 * @property summary writes the summary under the tree from the number of selected sources: "3 sources selected",
 * or "No sources selected" for zero.
 * @property empty the text shown when there are no sources: "No sources available".
 * @property expand writes the spoken name of the button that opens a group: "Show contents of Handbook".
 * @property collapse writes the spoken name of the button that closes a group: "Hide contents of Handbook".
 */
@Immutable
public class MonarchAiKnowledgeScopeLabels(
    public val label: String? = null,
    public val kind: ((MonarchAiKnowledgeSourceKind) -> String)? = null,
    public val itemLabel: ((label: String, kind: String) -> String)? = null,
    public val summary: ((count: Int) -> String)? = null,
    public val empty: String? = null,
    public val expand: ((label: String) -> String)? = null,
    public val collapse: ((label: String) -> String)? = null,
)
