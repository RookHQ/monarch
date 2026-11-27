package com.rookhq.monarch.components.datagrid.mergedialog

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The fields whose values differ between the records, and those that match. */
internal data class MergeDialogFieldSplit(val differing: List<MonarchMergeField>, val same: List<MonarchMergeField>)

/** Component tokens and rules of MonarchMergeDialog, built from primitives and roles only. */
internal object MonarchMergeDialogTokens {
    val Padding: Dp = MonarchTokens.Space.space4.dp
    val SectionGap: Dp = MonarchTokens.Space.space5.dp
    /** The choices of a record or a field side by side before they wrap. */
    const val CHOICE_COLUMNS: Int = 2

    val Gap: Dp = MonarchTokens.Space.space2.dp
    val HeaderGap: Dp = MonarchTokens.Space.space1.dp

    /** A missing or blank value is empty. */
    fun isEmpty(value: String?): Boolean = value.isNullOrBlank()

    /** The value compared between records: trimmed, with every empty value equal. */
    private fun key(value: String?): String = value?.trim().orEmpty()

    /** Splits [fields] into those whose values differ between [records] and those that match. */
    fun split(fields: List<MonarchMergeField>, records: List<MonarchMergeRecord>): MergeDialogFieldSplit {
        val (same, differing) = fields.partition { field ->
            records.map { key(it.values[field.id]) }.distinct().size <= 1
        }
        return MergeDialogFieldSplit(differing, same)
    }

    /** The kept record: [primaryId] when it is one of [records], otherwise the first. */
    fun primary(records: List<MonarchMergeRecord>, primaryId: String?): MonarchMergeRecord? =
        records.firstOrNull { it.id == primaryId } ?: records.firstOrNull()

    /**
     * The record whose value of [field] is kept by default: the kept record's,
     * or the first other record's when the kept one is empty.
     */
    fun defaultChoice(field: String, records: List<MonarchMergeRecord>, primaryId: String): String {
        val primary = records.firstOrNull { it.id == primaryId }
        if (primary != null && !isEmpty(primary.values[field])) return primaryId
        return records.firstOrNull { it.id != primaryId && !isEmpty(it.values[field]) }?.id ?: primaryId
    }

    /** The chosen record of [field]: what the person picked, or the default for the kept record. */
    fun choice(
        field: String,
        records: List<MonarchMergeRecord>,
        primaryId: String,
        picked: Map<String, String>,
    ): String = picked[field]?.takeIf { id -> records.any { it.id == id } } ?: defaultChoice(field, records, primaryId)

    /** The selection handed to `onMerge`. */
    fun selection(
        records: List<MonarchMergeRecord>,
        fields: List<MonarchMergeField>,
        primaryId: String,
        picked: Map<String, String>,
    ): MonarchMergeSelection = MonarchMergeSelection(
        primaryId = primaryId,
        mergedIds = records.filter { it.id != primaryId }.map { it.id },
        fields = fields.associate { it.id to choice(it.id, records, primaryId, picked) },
    )

    /** Whether the merge button can be pressed: two or more records, no blockers, and nothing running. */
    fun canMerge(recordCount: Int, blockerCount: Int, merging: Boolean): Boolean =
        recordCount >= 2 && blockerCount == 0 && !merging

    /** Headings use the default text role. */
    fun headingColor(colors: MonarchColors): Color = colors.textDefault

    /** The description and the field names in the list of matching fields use the muted role. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted
}
