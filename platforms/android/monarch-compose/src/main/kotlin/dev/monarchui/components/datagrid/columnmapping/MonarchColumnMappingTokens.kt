package dev.monarchui.components.datagrid.columnmapping

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** What one row of the mapping shows, worked out from the mapping. */
internal data class MonarchColumnMappingRow(
    val index: Int,
    val source: MonarchColumnMappingSource,
    val fieldId: String,
    val field: MonarchColumnMappingField?,
    val suggestion: MonarchColumnMappingSuggestion?,
    val others: List<String>,
    val samples: List<String>,
)

/** Component tokens and logic of MonarchColumnMapping, built from primitives and roles only. */
internal object MonarchColumnMappingTokens {
    /** The space around a row and the summary. */
    val Padding: Dp = MonarchTokens.Space.space4.dp

    /** The space between the parts of a row. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The padding inside the missing-fields error. */
    val NoticePadding: Dp = MonarchTokens.Space.space3.dp

    /** The width of the file column. */
    val SourceWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space4).dp

    /** The width of the example values column. */
    val SampleWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space6).dp

    /** The width of the target column: the picker and its badge. */
    val TargetWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space10).dp

    /** The gap between columns. */
    val ColumnGap: Dp = MonarchTokens.Space.space2.dp

    /** The space above the text of the first two columns, so it lines up with the picker's text. */
    val CellTop: Dp = MonarchTokens.Space.space3.dp

    /** The width of the table: three columns, two gaps and the padding at both ends. */
    val TableWidth: Dp = SourceWidth + SampleWidth + TargetWidth + ColumnGap * 2 + Padding * 2

    /** How many example values are shown. */
    const val MAX_SAMPLES: Int = 3

    /** Column headers in the default text colour. */
    fun headerColor(colors: MonarchColors): Color = colors.textDefault

    /** Example values, the count and help in the muted text colour. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The missing-fields error: the danger subtle fill, danger border and strong danger text. */
    fun notice(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(
            container = colors.dangerSubtle,
            content = colors.dangerStrong,
            border = colors.dangerBorder,
        )

    /** The tone of a suggestion badge: success for an exact match, info for a close one. */
    fun badgeTone(confidence: MonarchColumnMappingConfidence): MonarchTone = when (confidence) {
        MonarchColumnMappingConfidence.Exact -> MonarchTone.Success
        MonarchColumnMappingConfidence.Similar -> MonarchTone.Info
    }

    /**
     * The rows: each column with its chosen field, its suggestion when it is
     * chosen, and the other columns with the same field.
     */
    fun rows(
        sources: List<MonarchColumnMappingSource>,
        mapping: Map<String, String>,
        fields: List<MonarchColumnMappingField>,
        suggestions: Map<String, MonarchColumnMappingSuggestion>,
        showIssues: Boolean,
    ): List<MonarchColumnMappingRow> {
        val byId = fields.associateBy { it.id }
        val duplicates = MonarchColumnMappingDefaults.issues(mapping, fields).duplicates
        return sources.mapIndexed { index, source ->
            val fieldId = mapping[source.name].orEmpty()
            val field = byId[fieldId]
            val suggestion = suggestions[source.name]?.takeIf { it.fieldId == fieldId && fieldId.isNotEmpty() }
            val others = if (showIssues && field != null) {
                duplicates[field.id].orEmpty().filter { it != source.name }
            } else {
                emptyList()
            }
            MonarchColumnMappingRow(
                index = index,
                source = source,
                fieldId = fieldId,
                field = field,
                suggestion = suggestion,
                others = others,
                samples = source.samples.filter { it.isNotBlank() }.take(MAX_SAMPLES),
            )
        }
    }

    /** The number of columns mapped to a field that exists. */
    fun mappedCount(
        sources: List<MonarchColumnMappingSource>,
        mapping: Map<String, String>,
        fields: List<MonarchColumnMappingField>,
    ): Int {
        val ids = fields.mapTo(mutableSetOf()) { it.id }
        return sources.count { mapping[it.name] in ids }
    }

    /** Whether the mapping, for these columns, equals the suggestions. */
    fun sameAsSuggested(
        sources: List<MonarchColumnMappingSource>,
        mapping: Map<String, String>,
        suggestions: Map<String, MonarchColumnMappingSuggestion>,
    ): Boolean = sources.all { mapping[it.name].orEmpty() == suggestions[it.name]?.fieldId.orEmpty() }

    /** [mapping] with [source] set to [fieldId], or left out when [fieldId] is empty. */
    fun choose(mapping: Map<String, String>, source: String, fieldId: String): Map<String, String> =
        if (fieldId.isEmpty()) mapping - source else mapping + (source to fieldId)
}
