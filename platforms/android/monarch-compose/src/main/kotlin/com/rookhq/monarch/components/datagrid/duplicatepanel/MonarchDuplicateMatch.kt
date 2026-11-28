package com.rookhq.monarch.components.datagrid.duplicatepanel

import androidx.compose.runtime.Immutable

/**
 * A saved record that looks like the one being created or viewed, as shown by
 * `MonarchDuplicatePanel`.
 *
 * @property id a stable identity of the record. It keys the card of the match.
 * @property title the name of the record, for example "Example Ltd".
 * @property kind what kind of record it is, for example "Client". Shown as a badge.
 * @property reasons why it matched, in words, for example "Same email".
 * @property detail a short extra fact, for example "Archived".
 * @property score how confident the match is, from 0 to 1. Shown as a whole percentage. Clamped.
 */
@Immutable
public class MonarchDuplicateMatch(
    public val id: String,
    public val title: String,
    public val kind: String? = null,
    public val reasons: List<String> = emptyList(),
    public val detail: String? = null,
    public val score: Float? = null,
)
