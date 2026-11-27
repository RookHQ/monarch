package com.rookhq.monarch.components.intelligence.aisourcelist

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourceKind

/**
 * Replacements for the built-in strings of a `MonarchAiSourceList`. Every
 * `null` keeps the built-in string `monarch_aisourcelist_<key>`.
 *
 * @property heading the heading of the list, "Sources".
 * @property used the heading of the used group, from its count: "Used (3)".
 * @property considered the heading of the considered group, from its count: "Considered (2)".
 * @property showAll the toggle that expands the list, from the number of sources: "Show all 5 sources".
 * @property showFewer the toggle that collapses the list, "Show fewer".
 * @property source what TalkBack reads first for a source, from its number and title: "Source 1: Q3 report".
 * @property openSource what TalkBack reads as the tap action of a source: "double tap to open the source".
 * @property kind the name of each kind of source: "Web page", "Document", "Record", "File".
 */
@Immutable
public class MonarchAiSourceListLabels(
    public val heading: String? = null,
    public val used: ((count: Int) -> String)? = null,
    public val considered: ((count: Int) -> String)? = null,
    public val showAll: ((count: Int) -> String)? = null,
    public val showFewer: String? = null,
    public val source: ((number: Int, title: String) -> String)? = null,
    public val openSource: String? = null,
    public val kind: ((kind: MonarchAiSourceKind) -> String)? = null,
)
