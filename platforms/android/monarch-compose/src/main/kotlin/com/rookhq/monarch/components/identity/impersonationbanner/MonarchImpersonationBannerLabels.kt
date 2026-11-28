package com.rookhq.monarch.components.identity.impersonationbanner

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchImpersonationBanner`.
 * Every `null` keeps the built-in string `monarch_impersonationbanner_<key>`.
 *
 * @property viewingAs the main line, from the name: "You are viewing as Sam Lee". A replacement is shown
 * without the bold name, which needs the built-in format.
 * @property started the word before the start time, "Started".
 * @property reason the word before the reason, "Reason:".
 * @property stop the visible title of the stop button, "Stop".
 * @property stopLabel the TalkBack name of the stop button, from the name: "Stop viewing as Sam Lee".
 * @property stopError shown and announced when stopping failed, "Couldn’t stop. Try again."
 */
@Immutable
public class MonarchImpersonationBannerLabels(
    public val viewingAs: ((name: String) -> String)? = null,
    public val started: String? = null,
    public val reason: String? = null,
    public val stop: String? = null,
    public val stopLabel: ((name: String) -> String)? = null,
    public val stopError: String? = null,
)
