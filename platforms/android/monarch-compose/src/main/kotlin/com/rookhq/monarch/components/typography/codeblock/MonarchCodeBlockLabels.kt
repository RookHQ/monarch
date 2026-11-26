package com.rookhq.monarch.components.typography.codeblock

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchCodeBlock`. Every `null`
 * uses the built-in string: the copy strings of `MonarchCopyButton`
 * (`strings_copybutton.xml`) and the others from `strings_codeblock.xml`.
 *
 * @property copy the label of the copy button: "Copy".
 * @property copied the label of the copy button after a copy: "Copied".
 * @property copiedAnnouncement the state TalkBack announces after a copy: "Copied to clipboard".
 * @property wrap the label of the wrap toggle: "Wrap lines".
 * @property added the word TalkBack reads before an added diff line: "Added".
 * @property removed the word TalkBack reads before a removed diff line: "Removed".
 * @property codeName names the code from the title and the language id, for the code region and the copy
 * button. The default is the title, else "TypeScript code" from the language, else "Code".
 */
@Immutable
public class MonarchCodeBlockLabels(
    public val copy: String? = null,
    public val copied: String? = null,
    public val copiedAnnouncement: String? = null,
    public val wrap: String? = null,
    public val added: String? = null,
    public val removed: String? = null,
    public val codeName: ((title: String?, language: String?) -> String)? = null,
)
