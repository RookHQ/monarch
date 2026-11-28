package com.rookhq.monarch.components.workflow.actioneditor

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchActionEditor. */
internal object MonarchActionEditorTokens {
    /** The gap between the actions and the add button. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the parts of one action. */
    val ControlGap: Dp = MonarchTokens.Space.space2.dp

    /** The space inside an action's outline. */
    val CardPadding: Dp = MonarchTokens.Space.space3.dp

    /** The default most actions, as on the web. */
    const val DEFAULT_MAX: Int = 20

    /** The outline of an action: the subtle border, or the danger border while it shows a problem. */
    fun cardColors(colors: MonarchColors, invalid: Boolean): MonarchSurfaceColors = MonarchSurfaceColors(
        container = colors.bgSurface,
        content = colors.textDefault,
        border = if (invalid) colors.dangerBorder else colors.borderSubtle,
    )

    /** The colour of an action's heading: muted, or danger while the action shows a problem. */
    fun headingColor(colors: MonarchColors, invalid: Boolean) = if (invalid) colors.dangerStrong else colors.textMuted

    /** The colour of the empty and limit notes. */
    fun noteColor(colors: MonarchColors) = colors.textMuted

    /** The first id `"action-<n>"`, counting from 1, that is not in [existing]. */
    fun freshId(existing: Collection<String>): String {
        val taken = existing.toHashSet()
        return generateSequence(1) { it + 1 }.map { "action-$it" }.first { it !in taken }
    }

    /**
     * The options of the type menu: ungrouped types first, then each group in order. A disabled type is listed
     * with its reason and cannot be picked, unless it is [current]. A stored type the list no longer has comes
     * first, named by [unknown].
     */
    fun typeOptions(
        types: List<MonarchActionType>,
        current: String,
        unknown: (String) -> String,
    ): List<MonarchSelectOption<String>> {
        val groups = types.mapNotNull { it.group }.distinct()
        val ordered = types.filter { it.group == null } + groups.flatMap { g -> types.filter { it.group == g } }
        val known = ordered.map {
            MonarchSelectOption(
                it.key,
                it.label,
                description = it.disabledReason.takeIf { _ -> !it.enabled },
                enabled = it.enabled || it.key == current,
                group = it.group,
            )
        }
        val missing = current.isNotEmpty() && types.none { it.key == current }
        return if (missing) listOf(MonarchSelectOption(current, unknown(current))) + known else known
    }

    /** The type new actions start as: [defaultType] when it exists, else the first enabled type. */
    fun startType(types: List<MonarchActionType>, defaultType: String?): MonarchActionType? =
        types.firstOrNull { it.key == defaultType } ?: types.firstOrNull { it.enabled }

    /** [actions] with the one at [from] moved to [to]. */
    fun moved(actions: List<MonarchRuleAction>, from: Int, to: Int): List<MonarchRuleAction> =
        actions.toMutableList().apply { add(to, removeAt(from)) }

    /** [parameters] with [key] set to [value], or removed when [value] is `null`. */
    fun withParam(
        parameters: Map<String, MonarchActionValue>,
        key: String,
        value: MonarchActionValue?,
    ): Map<String, MonarchActionValue> = if (value == null) parameters - key else parameters + (key to value)
}
