package dev.monarchui.components.technical.installverify

import androidx.compose.runtime.Immutable

/**
 * One way to install: an HTML tag, a framework component, a plugin.
 *
 * @property id a stable id, unique among the snippets.
 * @property label the tab label, such as "HTML" or "React".
 * @property code the code the person copies.
 * @property language the language of [code] for the code block, such as "html".
 * @property description one line on where the code goes.
 */
@Immutable
public data class MonarchInstallSnippet(
    public val id: String,
    public val label: String,
    public val code: String,
    public val language: String? = null,
    public val description: String? = null,
)
