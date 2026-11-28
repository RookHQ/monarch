package com.rookhq.monarch.components.forms.termsacceptance

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchTermsAcceptance`. Every
 * `null` keeps the built-in string `monarch_termsacceptance_<key>`.
 *
 * @property label builds the checkbox sentence from the joined document titles: "I agree to the %s".
 * @property requiredMessage the error while the box is required and not ticked, "Accept the terms to continue".
 * @property required the note under the sentence of a required box, "Required".
 */
@Immutable
public class MonarchTermsAcceptanceLabels(
    public val label: ((String) -> String)? = null,
    public val requiredMessage: String? = null,
    public val required: String? = null,
)
