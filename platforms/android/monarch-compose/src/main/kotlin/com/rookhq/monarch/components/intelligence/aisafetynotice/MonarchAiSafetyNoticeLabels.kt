package com.rookhq.monarch.components.intelligence.aisafetynotice

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiSafetyNotice`. Every
 * `null` keeps the built-in string `monarch_aisafetynotice_<key>`. To change
 * the title or the explanation of one notice only, use its `title` and
 * `message` parameters.
 *
 * @property title the title of each kind: "This request wasn’t answered", "This response was adjusted",
 * "Some content was hidden".
 * @property message the explanation of each kind.
 * @property reason the bold label before the reason, "Reason:".
 * @property editPrompt the button that puts the prompt back in the composer, "Edit prompt".
 * @property learnMore the link to the content guidelines, "Content guidelines".
 */
@Immutable
public class MonarchAiSafetyNoticeLabels(
    public val title: ((kind: MonarchAiSafetyNoticeKind) -> String)? = null,
    public val message: ((kind: MonarchAiSafetyNoticeKind) -> String)? = null,
    public val reason: String? = null,
    public val editPrompt: String? = null,
    public val learnMore: String? = null,
)
