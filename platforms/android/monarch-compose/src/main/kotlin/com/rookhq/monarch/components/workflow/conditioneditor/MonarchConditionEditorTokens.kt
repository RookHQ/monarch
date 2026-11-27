package com.rookhq.monarch.components.workflow.conditioneditor

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchConditionEditor. */
internal object MonarchConditionEditorTokens {
    /** The gap between the match switch, the conditions and the add button. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the controls of one condition. */
    val ControlGap: Dp = MonarchTokens.Space.space2.dp

    /** The space inside a condition's outline. */
    val RowPadding: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the words and the segments of the match switch. */
    val MatchGap: Dp = MonarchTokens.Space.space2.dp

    /** The default most conditions, as on the web. */
    const val DEFAULT_MAX: Int = 20

    /** The outline of a condition: the subtle border, or the danger border while it shows a problem. */
    fun rowColors(colors: MonarchColors, invalid: Boolean): MonarchSurfaceColors = MonarchSurfaceColors(
        container = colors.bgSurface,
        content = colors.textDefault,
        border = if (invalid) colors.dangerBorder else colors.borderSubtle,
    )

    /** The colour of the empty and limit notes. */
    fun noteColor(colors: MonarchColors) = colors.textMuted

    /** The first id `"<prefix>-<n>"`, counting from 1, that is not in [existing]. */
    fun freshId(prefix: String, existing: Collection<String>): String {
        val taken = existing.toHashSet()
        return generateSequence(1) { it + 1 }.map { "$prefix-$it" }.first { it !in taken }
    }

    /**
     * The options of the field menu: ungrouped fields first, then each group in order of first appearance. A
     * disabled field shows its reason as the option's description. A stored field the list no longer has comes
     * first, named by [unknown].
     */
    fun fieldOptions(
        fields: List<MonarchConditionField>,
        current: String,
        unknown: (String) -> String,
    ): List<MonarchSelectOption<String>> {
        val groups = fields.mapNotNull { it.group }.distinct()
        val ordered = fields.filter { it.group == null } + groups.flatMap { g -> fields.filter { it.group == g } }
        val known = ordered.map {
            MonarchSelectOption(
                it.key,
                it.label,
                description = it.disabledReason,
                enabled = it.enabled,
                group = it.group,
            )
        }
        val missing = current.isNotEmpty() && fields.none { it.key == current }
        return if (missing) listOf(MonarchSelectOption(current, unknown(current))) + known else known
    }

    /** The operators to list: the field's operators, with a stored operator the field no longer offers first. */
    fun operatorChoices(
        field: MonarchConditionField?,
        current: MonarchConditionOperator,
    ): List<MonarchConditionOperator> {
        val offered = MonarchConditionOperator.forField(field)
        return if (current in offered) offered else listOf(current) + offered
    }

    /** The field a new condition starts on: [defaultField] when it exists, else the first enabled field. */
    fun startField(fields: List<MonarchConditionField>, defaultField: String?): MonarchConditionField? =
        fields.firstOrNull { it.key == defaultField } ?: fields.firstOrNull { it.enabled }
}
