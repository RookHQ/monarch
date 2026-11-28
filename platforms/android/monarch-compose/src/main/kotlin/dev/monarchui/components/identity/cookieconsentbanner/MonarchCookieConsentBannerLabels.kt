package dev.monarchui.components.identity.cookieconsentbanner

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchCookieConsentBanner`. Every `null` uses the built-in string
 * in `strings_cookieconsentbanner.xml`; the title and description defaults depend on the regime.
 *
 * @property title the title: "Cookies on this site".
 * @property description the explanation of the regime.
 * @property acceptAll "Accept all".
 * @property rejectAll "Reject all".
 * @property customize "Customize".
 * @property acknowledge "OK".
 * @property doNotSell "Do not sell or share my personal information".
 * @property policy the policy link after the description: "Cookie policy".
 */
@Immutable
public class MonarchCookieConsentBannerLabels(
    public val title: String? = null,
    public val description: String? = null,
    public val acceptAll: String? = null,
    public val rejectAll: String? = null,
    public val customize: String? = null,
    public val acknowledge: String? = null,
    public val doNotSell: String? = null,
    public val policy: String? = null,
)
