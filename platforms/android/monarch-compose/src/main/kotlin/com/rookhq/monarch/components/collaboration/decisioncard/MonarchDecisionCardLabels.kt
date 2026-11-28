package com.rookhq.monarch.components.collaboration.decisioncard

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchDecisionCard`. Every
 * `null` keeps the built-in string `monarch_decisioncard_<key>`.
 *
 * @property choices the name of the group of choices, shown above them.
 * @property none the choice that clears the decision.
 * @property reason the label of the reason field.
 * @property optional the mark added to the label when a reason is not required.
 * @property reasonPlaceholder the placeholder of the reason field.
 * @property reasonRequired the error when a required reason is missing.
 * @property charactersLeft the count shown near the limit, given the characters left.
 * @property save the save button.
 * @property saved shown and announced after a save.
 * @property decided the word before the time of the decision.
 * @property decidedBy the words after the time, given the person who decided.
 * @property undecided the read-only card when nothing has been decided.
 */
@Immutable
public class MonarchDecisionCardLabels(
    public val choices: String? = null,
    public val none: String? = null,
    public val reason: String? = null,
    public val optional: String? = null,
    public val reasonPlaceholder: String? = null,
    public val reasonRequired: String? = null,
    public val charactersLeft: ((Int) -> String)? = null,
    public val save: String? = null,
    public val saved: String? = null,
    public val decided: String? = null,
    public val decidedBy: ((String) -> String)? = null,
    public val undecided: String? = null,
)
