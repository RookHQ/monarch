package com.rookhq.monarch.components.forms.combobox

/** One row of a combobox list, in the menu or the sheet. */
internal sealed interface ComboboxEntry {
    /** The row that clears the selection, such as "Unassigned". */
    data class Clear(val label: String) : ComboboxEntry

    /** A group heading. */
    data class Header(val text: String) : ComboboxEntry

    /** An option. */
    data class Option(val option: MonarchComboboxOption) : ComboboxEntry

    /** The row that creates an option from the typed text. */
    data class Create(val text: String, val label: String) : ComboboxEntry

    /**
     * A message: loading, no matches, a list cut short, or a load error
     * ([failed]) with the label of its retry button ([retry]) when the app can
     * load again.
     */
    data class Message(
        val text: String,
        val loading: Boolean = false,
        val failed: Boolean = false,
        val retry: String? = null,
    ) : ComboboxEntry
}

/** A load error of the options: its message, and whether the app can load again. */
internal class ComboboxFailure(val message: String, val canRetry: Boolean)

/** The resolved messages of a combobox list. */
internal class ComboboxMessages(
    val loading: String,
    val empty: String,
    val create: (String) -> String,
    val more: (shown: Int, total: Int) -> String,
    val retry: String = "",
)

/** Builds the rows of a combobox list. */
internal object ComboboxEntries {
    /**
     * The rows for [results]: the clear row while nothing is typed, the
     * options under their group headings, the Create row, then a message: Loading while [loading] and nothing
     * matches, No matches when nothing matches and nothing can be created, or the cut-short note. A [failure]
     * replaces the message with the error and its retry button.
     */
    fun build(
        results: ComboboxResults,
        query: String,
        clearOptionLabel: String?,
        loading: Boolean,
        messages: ComboboxMessages,
        failure: ComboboxFailure? = null,
    ): List<ComboboxEntry> {
        val trimmed = query.trim()
        val rows = mutableListOf<ComboboxEntry>()
        if (trimmed.isEmpty() && clearOptionLabel != null) rows += ComboboxEntry.Clear(clearOptionLabel)
        ComboboxFilter.sections(results.shown).forEach { (group, options) ->
            if (group != null) rows += ComboboxEntry.Header(group)
            options.forEach { rows += ComboboxEntry.Option(it) }
        }
        if (results.showCreate) rows += ComboboxEntry.Create(trimmed, messages.create(trimmed))
        when {
            failure != null ->
                rows += ComboboxEntry.Message(
                    text = failure.message,
                    failed = true,
                    retry = messages.retry.takeIf { failure.canRetry },
                )
            results.shown.isEmpty() && loading -> rows += ComboboxEntry.Message(messages.loading, loading = true)
            results.shown.isEmpty() && !results.showCreate -> rows += ComboboxEntry.Message(messages.empty)
            results.total > results.shown.size ->
                rows += ComboboxEntry.Message(messages.more(results.shown.size, results.total))
        }
        return rows
    }
}
