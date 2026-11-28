package com.rookhq.monarch.components.forms.checkboxgroup

import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchControlMetrics
import com.rookhq.monarch.internal.choice.MonarchChoiceTokens
import com.rookhq.monarch.tokens.MonarchTokens

/** The choices of a group. */
private typealias Options<T> = List<MonarchCheckboxGroupOption<T>>

/** Component tokens and rules of MonarchCheckboxGroup, built from primitives and roles only. */
internal object MonarchCheckboxGroupTokens {
    /**
     * The start indent of the choices under a Select all row: the box and its gap, so each nested box sits under
     * the Select all label and the group keeps one text edge.
     */
    val NestedIndent: Dp = MonarchControlMetrics.CheckSize + MonarchChoiceTokens.Gap

    /** The gap between choices laid out horizontally. */
    val HorizontalGap: Dp = MonarchTokens.Space.space4.dp

    /** The choices the user can change: none when the whole group is disabled. */
    fun <T> changeable(options: Options<T>, enabled: Boolean): Options<T> =
        if (enabled) options.filter { it.enabled } else emptyList()

    /**
     * The state of Select all: on when every changeable choice is checked,
     * indeterminate when some are, off otherwise (and when none can change).
     */
    fun <T> selectAllState(options: Options<T>, value: List<T>, enabled: Boolean): ToggleableState {
        val changeable = changeable(options, enabled)
        val on = changeable.count { it.value in value }
        return when {
            changeable.isNotEmpty() && on == changeable.size -> ToggleableState.On
            on > 0 -> ToggleableState.Indeterminate
            else -> ToggleableState.Off
        }
    }

    /** The value after one choice is set to [checked], in the order of [options]. */
    fun <T> toggle(options: Options<T>, value: List<T>, option: T, checked: Boolean): List<T> {
        val next = value.toMutableSet()
        if (checked) next.add(option) else next.remove(option)
        return ordered(options, next)
    }

    /**
     * The value after Select all is pressed: when every changeable choice is
     * checked they are all cleared, otherwise they are all checked. Choices
     * that cannot change keep their state.
     */
    fun <T> toggleAll(options: Options<T>, value: List<T>, enabled: Boolean): List<T> {
        val all = selectAllState(options, value, enabled) == ToggleableState.On
        val next = value.toMutableSet()
        changeable(options, enabled).forEach { if (all) next.remove(it.value) else next.add(it.value) }
        return ordered(options, next)
    }

    /** The checked values in the order of the options. */
    private fun <T> ordered(options: Options<T>, checked: Set<T>): List<T> =
        options.map { it.value }.filter { it in checked }
}
