package dev.monarchui.components.identity.consenthistory

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchConsentHistory`. Every
 * `null` keeps the built-in string `monarch_consenthistory_<key>`.
 *
 * @property method the name of a method, such as "Custom choice".
 * @property regime the name of a regime, such as "Asked before use (opt-in)".
 * @property methodTitle the name of the method line, "Choice".
 * @property policyVersionTitle the name of the policy line, "Policy version".
 * @property regimeTitle the name of the rules line, "Rules applied".
 * @property categoriesTitle the name of the categories list, "Categories".
 * @property allowed the state of an allowed category, "Allowed".
 * @property denied the state of a refused category, "Not allowed".
 * @property signalPresent the note on a decision with a privacy signal, "Browser privacy signal present".
 * @property emptyTitle the empty state, "No consent choices recorded".
 * @property emptyMessage the line under the empty state.
 * @property exportFailed shown and announced when the download failed, "The download did not start. Try again."
 */
@Immutable
public class MonarchConsentHistoryLabels(
    public val method: ((MonarchConsentMethod) -> String)? = null,
    public val regime: ((MonarchConsentRegime) -> String)? = null,
    public val methodTitle: String? = null,
    public val policyVersionTitle: String? = null,
    public val regimeTitle: String? = null,
    public val categoriesTitle: String? = null,
    public val allowed: String? = null,
    public val denied: String? = null,
    public val signalPresent: String? = null,
    public val emptyTitle: String? = null,
    public val emptyMessage: String? = null,
    public val exportFailed: String? = null,
)
