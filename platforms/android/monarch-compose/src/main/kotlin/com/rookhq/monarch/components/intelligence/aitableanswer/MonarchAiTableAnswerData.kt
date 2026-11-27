package com.rookhq.monarch.components.intelligence.aitableanswer

import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import java.util.Locale

/** The columns, sorting, CSV and spoken rows of MonarchAiTableAnswer. Every function is pure. */
internal object MonarchAiTableAnswerData {
    private const val FORMULA_STARTS = "=+-@\t\r"

    /** A column with every default decided. */
    data class Column(
        val key: String,
        val label: String,
        val type: MonarchAiTableColumnType,
        val alignment: MonarchAiTableColumnAlignment,
    )

    /** The column the table is sorted by and the direction. */
    data class Sort(val key: String, val ascending: Boolean)

    /**
     * The columns to show: the given ones, or every key of the rows in the
     * order it first appears, with labels, types and alignments filled in.
     */
    fun columns(columns: List<MonarchAiTableColumn>?, rows: List<Map<String, MonarchAiJSON>>): List<Column> {
        val base = columns ?: rows.flatMap { it.keys }.distinct().map { MonarchAiTableColumn(it) }
        return base.map { column ->
            val type = column.type ?: MonarchAiTableAnswerValues.inferType(rows.map { it[column.key] })
            Column(
                key = column.key,
                label = column.label ?: column.key,
                type = type,
                alignment = column.alignment ?: if (type == MonarchAiTableColumnType.Number) {
                    MonarchAiTableColumnAlignment.End
                } else {
                    MonarchAiTableColumnAlignment.Start
                },
            )
        }
    }

    /** The sort after a header is activated: ascending for a new column, the other direction for the same one. */
    fun toggled(sort: Sort?, key: String): Sort = Sort(key, ascending = !(sort?.key == key && sort.ascending))

    /**
     * The order of the rows as indexes. Blanks always go last, whatever the
     * direction, and equal values keep the order the model gave.
     */
    fun order(rows: List<Map<String, MonarchAiJSON>>, sort: Sort?, columns: List<Column>, locale: Locale): List<Int> {
        val indexes = rows.indices.toList()
        val column = sort?.let { s -> columns.firstOrNull { it.key == s.key } } ?: return indexes
        val values = rows.map { it[column.key] }
        val comparator: Comparator<Int> = when (column.type) {
            MonarchAiTableColumnType.Number -> compareBy { MonarchAiTableAnswerValues.number(values[it]) ?: 0.0 }
            MonarchAiTableColumnType.Date -> compareBy { MonarchAiTableAnswerValues.date(values[it]) ?: Long.MIN_VALUE }
            MonarchAiTableColumnType.Text -> Comparator { a, b ->
                MonarchAiTableAnswerValues.compareText(
                    MonarchAiTableAnswerValues.text(values[a]),
                    MonarchAiTableAnswerValues.text(values[b]),
                    locale,
                )
            }
        }
        val directed = if (sort.ascending) comparator else comparator.reversed()
        val (blank, present) = indexes.partition { MonarchAiTableAnswerValues.isBlank(values[it]) }
        // sortedWith is stable, so equal values keep the model's order.
        return present.sortedWith(directed) + blank
    }

    /**
     * One CSV field: quoted when it holds a comma, a quote, a line break or
     * space at either end, with quotes doubled. Text a spreadsheet would run
     * as a formula (`=`, `+`, `-`, `@`, tab, return) is prefixed with `'`,
     * but numbers such as "-3" are kept.
     */
    fun csvField(value: MonarchAiJSON?): String {
        var field = MonarchAiTableAnswerValues.text(value)
        val formula = field.firstOrNull()?.let { it in FORMULA_STARTS } == true
        if (formula && value is MonarchAiJSON.Text && MonarchAiTableAnswerValues.number(value) == null) {
            field = "'$field"
        }
        val quoted = field.any { it == ',' || it == '"' || it == '\n' || it == '\r' } ||
            field.firstOrNull()?.isWhitespace() == true || field.lastOrNull()?.isWhitespace() == true
        return if (quoted) "\"" + field.replace("\"", "\"\"") + "\"" else field
    }

    /** The rows as RFC 4180 CSV: a header row of labels, then one line per row, with CRLF line endings. */
    fun csv(rows: List<Map<String, MonarchAiJSON>>, columns: List<Column>): String =
        (
            listOf(columns.joinToString(",") { csvField(MonarchAiJSON.Text(it.label)) }) +
                rows.map { row -> columns.joinToString(",") { csvField(row[it.key]) } }
            ).joinToString("\r\n")

    /** What TalkBack reads for a row: each header and its value, with [emptyLabel] for a missing value. */
    fun rowLabel(row: Map<String, MonarchAiJSON>, columns: List<Column>, locale: Locale, emptyLabel: String): String =
        columns.joinToString(", ") { column ->
            val value = row[column.key]
            val text = if (MonarchAiTableAnswerValues.isBlank(value)) {
                emptyLabel
            } else {
                MonarchAiTableAnswerValues.display(value, column.type, locale)
            }
            "${column.label}: $text"
        }

    /** How many rows are drawn: all once shown, otherwise at most [visibleRows] (at least one). */
    fun shownCount(total: Int, visibleRows: Int, showingAll: Boolean): Int =
        if (showingAll) total else minOf(total, maxOf(1, visibleRows))
}
