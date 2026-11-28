package dev.monarchui.components.identity.consenthistory

import androidx.compose.runtime.Immutable

/**
 * A consent category of the product, for its name and its place in the list.
 *
 * @property id the id used as the key of `MonarchConsentRecord.categories`.
 * @property label the name shown, such as "Analytics". Already localised.
 */
@Immutable
public class MonarchConsentCategory(public val id: String, public val label: String)
