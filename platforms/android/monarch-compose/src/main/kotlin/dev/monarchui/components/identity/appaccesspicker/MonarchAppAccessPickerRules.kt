package dev.monarchui.components.identity.appaccesspicker

import java.text.Collator
import java.util.Locale

/** The selection logic of MonarchAppAccessPicker and MonarchAppAccessSummary, as pure functions. */
internal object MonarchAppAccessPickerRules {
    /** [value] with [id] added at the end or removed. */
    fun toggle(value: List<String>, id: String, on: Boolean): List<String> =
        if (on) value.filterNot { it == id } + id else value.filterNot { it == id }

    /** The apps that can be changed here. */
    fun changeable(apps: List<MonarchAppAccessOption>): List<MonarchAppAccessOption> = apps.filter { it.enabled }

    /** Whether every changeable app is selected; `false` when none can be changed. */
    fun allSelected(apps: List<MonarchAppAccessOption>, value: List<String>): Boolean {
        val changeable = changeable(apps)
        return changeable.isNotEmpty() && changeable.all { it.id in value }
    }

    /**
     * Select all, or clear all when everything changeable is selected. Ids of apps that cannot be changed,
     * and ids not in [apps], are kept.
     */
    fun toggleAll(apps: List<MonarchAppAccessOption>, value: List<String>): List<String> {
        val changeable = changeable(apps).map { it.id }
        val fixed = value.filterNot { it in changeable }
        return if (allSelected(apps, value)) fixed else fixed + changeable
    }

    /** How many of [apps] are selected. */
    fun count(apps: List<MonarchAppAccessOption>, value: List<String>): Int = apps.count { it.id in value }

    /** The names of the granted apps (the id when no app matches), sorted for [locale]. */
    fun names(apps: List<MonarchAppAccessOption>, value: List<String>, locale: Locale): List<String> {
        val collator = Collator.getInstance(locale)
        return value.distinct().map { id -> apps.firstOrNull { it.id == id }?.name ?: id }.sortedWith(collator)
    }

    /** [names] split into the ones shown (at least one) and the ones behind "+N". */
    fun split(names: List<String>, max: Int): Pair<List<String>, List<String>> {
        val shown = names.take(maxOf(1, max))
        return shown to names.drop(shown.size)
    }
}
