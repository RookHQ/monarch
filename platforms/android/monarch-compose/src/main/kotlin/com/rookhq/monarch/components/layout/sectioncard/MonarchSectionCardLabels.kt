package com.rookhq.monarch.components.layout.sectioncard

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchSectionCard`. Every
 * `null` uses the built-in string named after it in `strings_sectioncard.xml`.
 *
 * @property loading the label of the loading state, also read as the title's state while loading or busy:
 * "Loading".
 * @property emptyTitle the title of the empty state: "Nothing here yet".
 * @property errorTitle the title of the error state: "This section didn't load".
 * @property expanded the state of a collapsible title while the body shows: "Expanded".
 * @property collapsed the state of a collapsible title while the body is hidden: "Collapsed".
 * @property tone the spoken name of a tone other than neutral, such as "Danger".
 */
@Immutable
public class MonarchSectionCardLabels(
    public val loading: String? = null,
    public val emptyTitle: String? = null,
    public val errorTitle: String? = null,
    public val expanded: String? = null,
    public val collapsed: String? = null,
    public val tone: String? = null,
)
