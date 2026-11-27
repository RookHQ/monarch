package com.rookhq.monarch.components.navigation.megamenu

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchMegaMenu`. Every `null` uses the built-in string named after
 * it in `strings_megamenu.xml`.
 *
 * @property expanded the state TalkBack reads for an open entry: "Expanded".
 * @property collapsed the state TalkBack reads for a closed entry: "Collapsed".
 * @property explore the default call to action of a featured card: "Explore".
 * @property external what TalkBack adds to a destination outside the app: "Opens outside the app".
 */
@Immutable
public class MonarchMegaMenuLabels(
    public val expanded: String? = null,
    public val collapsed: String? = null,
    public val explore: String? = null,
    public val external: String? = null,
)
