package com.rookhq.monarch.components.technical.dnsrecordtable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** How many records were checked and how many of them public DNS shows. */
internal data class MonarchDnsRecordTableCounts(val found: Int, val checked: Int)

/** The optional columns of the table. */
internal data class MonarchDnsRecordTableColumns(val priority: Boolean, val ttl: Boolean, val status: Boolean)

/** Component tokens and the pure rules of MonarchDnsRecordTable. */
internal object MonarchDnsRecordTableTokens {
    /** The gap between cards. */
    val CardGap: Dp = MonarchTokens.Space.space3.dp

    /** The padding inside a card and around the table. */
    val Padding: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the parts of a card. */
    val PartGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between a term and its value. */
    val TermGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The gap between columns, and between a value and its copy button. */
    val ColumnGap: Dp = MonarchTokens.Space.space3.dp

    /** The padding above and below a table row. */
    val RowPadding: Dp = MonarchTokens.Space.space2.dp

    /** The narrowest width at which the automatic layout is a table: the small breakpoint, 640dp. */
    val TableMinWidth: Dp = MonarchTokens.Breakpoint.sm.dp

    /** The largest font scale at which the automatic layout is a table. */
    const val TableMaxFontScale: Float = 1.3f

    /** The share of the row of each table column: type, name, value, priority, TTL and status. */
    const val TypeWeight: Float = 0.6f
    const val NameWeight: Float = 1.2f
    const val ValueWeight: Float = 1.8f
    const val NumberWeight: Float = 0.6f
    const val StatusWeight: Float = 0.8f

    /** The separator between a spoken name and its text, which TalkBack reads together. */
    const val Separator: String = ", "

    /** Whether to draw the table: as asked, or when automatic, at [width] of at least 640dp and a small font scale. */
    fun showsTable(layout: MonarchDnsRecordTableLayout, width: Dp, fontScale: Float): Boolean = when (layout) {
        MonarchDnsRecordTableLayout.Cards -> false
        MonarchDnsRecordTableLayout.Table -> true
        MonarchDnsRecordTableLayout.Automatic -> width >= TableMinWidth && fontScale <= TableMaxFontScale
    }

    /**
     * The record name as most DNS providers want it typed: relative to [zone] (`book` for `book.example.com` in
     * `example.com`), `@` for the zone apex, and [name] unchanged when it is not inside the zone. Case and a trailing
     * dot are ignored when comparing.
     */
    fun relativeName(name: String, zone: String?): String {
        if (zone.isNullOrEmpty()) return name
        val trimmedName = name.removeSuffix(".")
        val trimmedZone = zone.removeSuffix(".")
        val lowerName = trimmedName.lowercase()
        val lowerZone = trimmedZone.lowercase()
        return when {
            lowerName == lowerZone -> "@"
            lowerName.endsWith(".$lowerZone") -> trimmedName.substring(0, trimmedName.length - trimmedZone.length - 1)
            else -> name
        }
    }

    /** How many records were checked and how many of them were found. */
    fun counts(records: List<MonarchDnsRecord>): MonarchDnsRecordTableCounts {
        val checked = records.filter { it.found != null }
        return MonarchDnsRecordTableCounts(found = checked.count { it.found == true }, checked = checked.size)
    }

    /** The optional columns the table needs: only those a record uses. */
    fun columns(records: List<MonarchDnsRecord>): MonarchDnsRecordTableColumns = MonarchDnsRecordTableColumns(
        priority = records.any { it.priority != null },
        ttl = records.any { it.ttl != null },
        status = records.any { it.found != null },
    )

    /** The status badge of [record]: Found in the success tone, Waiting in the neutral tone, `null` if not checked. */
    fun status(record: MonarchDnsRecord, found: String, waiting: String): MonarchStatusDefinition? =
        when (record.found) {
            true -> MonarchStatusDefinition(found, MonarchTone.Success)
            false -> MonarchStatusDefinition(waiting, MonarchTone.Neutral)
            null -> null
        }

    /** The fill of a card and of the table: the surface colour. */
    fun surface(colors: MonarchColors): Color = colors.bgSurface

    /** The border of a card and of the table, and the rules between table rows: the default border. */
    fun border(colors: MonarchColors): Color = colors.borderDefault

    /** The terms, headings of the table and the full name line: muted text. */
    fun muted(colors: MonarchColors): Color = colors.textMuted

    /** The card heading, the type, the name, the value, the TTL and the priority: the default text. */
    fun text(colors: MonarchColors): Color = colors.textDefault
}
