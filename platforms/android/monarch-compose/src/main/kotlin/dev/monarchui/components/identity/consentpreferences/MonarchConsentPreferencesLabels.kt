package dev.monarchui.components.identity.consentpreferences

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchConsentPreferences`, to
 * translate or to use the legal team's wording. Every `null` keeps the
 * built-in string `monarch_consentpreferences_<key>`.
 *
 * @property title the heading, "Privacy choices".
 * @property description the sentence under the heading.
 * @property policy the policy link, "Privacy policy".
 * @property alwaysOn the note on a strictly necessary category, "Always on".
 * @property globalPrivacyControl the note on a category a Global Privacy Control signal keeps off.
 * @property on the status of an allowed category in choice mode, "On".
 * @property off the status of a refused category in choice mode, "Off".
 * @property rejectAll the button that turns every optional category off, "Reject all".
 * @property acceptAll the button that turns every optional category on, "Accept all".
 * @property save the button that saves the switches as they are, "Save choices".
 * @property withdraw the button that withdraws the saved decision, "Withdraw consent".
 * @property policyVersion before the saved policy version, "Policy version".
 * @property saved before the time of the saved decision, "Saved".
 */
@Immutable
public class MonarchConsentPreferencesLabels(
    public val title: String? = null,
    public val description: String? = null,
    public val policy: String? = null,
    public val alwaysOn: String? = null,
    public val globalPrivacyControl: String? = null,
    public val on: String? = null,
    public val off: String? = null,
    public val rejectAll: String? = null,
    public val acceptAll: String? = null,
    public val save: String? = null,
    public val withdraw: String? = null,
    public val policyVersion: String? = null,
    public val saved: String? = null,
)
