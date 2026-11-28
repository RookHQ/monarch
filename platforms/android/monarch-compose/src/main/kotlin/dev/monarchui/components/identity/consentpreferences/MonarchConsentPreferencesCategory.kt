package dev.monarchui.components.identity.consentpreferences

import androidx.compose.runtime.Immutable

/**
 * A category of optional storage or processing in a
 * `MonarchConsentPreferences`, such as analytics or advertising.
 *
 * @property id the id used as the key of the choices and of `MonarchConsentRecord.categories`.
 * @property label the category's name, already localised, such as "Analytics".
 * @property description what the category is used for, in one or two sentences.
 * @property required whether it is strictly necessary: always on and cannot be turned off.
 * @property saleOrSharing whether it sells or shares personal data, so a Global Privacy Control signal keeps it off.
 */
@Immutable
public class MonarchConsentPreferencesCategory(
    public val id: String,
    public val label: String,
    public val description: String,
    public val required: Boolean = false,
    public val saleOrSharing: Boolean = false,
)
