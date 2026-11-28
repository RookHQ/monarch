package com.rookhq.monarch.components.intelligence.aiautofillfield

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiAutofillField`. Every
 * `null` keeps the built-in string `monarch_aiautofillfield_<key>`.
 *
 * @property badge the text of the AI badge for a state: "AI suggested", "AI filled", "Edited by you".
 * @property accept the visible text of the accept button, "Accept".
 * @property clear the visible text of the clear button, "Clear".
 * @property acceptField the name TalkBack reads for the accept button, from the field name:
 * "Accept suggested Company".
 * @property clearField the name TalkBack reads for the clear button: "Clear suggested Company".
 * @property source the source line, from the source: "From invoice.pdf".
 * @property original the line with the original suggestion once edited: "AI suggested: Acme Ltd".
 * @property accepted announced after Accept, "Suggestion accepted".
 * @property cleared announced after Clear, "Suggestion cleared".
 */
@Immutable
public class MonarchAiAutofillFieldLabels(
    public val badge: ((MonarchAiAutofillState) -> String)? = null,
    public val accept: String? = null,
    public val clear: String? = null,
    public val acceptField: ((String) -> String)? = null,
    public val clearField: ((String) -> String)? = null,
    public val source: ((String) -> String)? = null,
    public val original: ((String) -> String)? = null,
    public val accepted: String? = null,
    public val cleared: String? = null,
)
