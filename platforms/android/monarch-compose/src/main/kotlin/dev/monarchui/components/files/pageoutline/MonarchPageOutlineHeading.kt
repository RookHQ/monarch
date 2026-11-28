package dev.monarchui.components.files.pageoutline

import androidx.compose.runtime.Immutable

/**
 * One heading of a document, for `MonarchPageOutline`.
 *
 * @property text the heading's text, already localised.
 * @property level the heading level, 1 through 6. Levels outside that range are clamped.
 * @property id a stable id of the heading, used as the list key and to scroll to it, for example the
 * index of its item in the document's `LazyColumn`.
 */
@Immutable
public class MonarchPageOutlineHeading(public val text: String, public val level: Int, public val id: String)
