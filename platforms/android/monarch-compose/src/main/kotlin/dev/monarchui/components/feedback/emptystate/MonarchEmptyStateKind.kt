package dev.monarchui.components.feedback.emptystate

/**
 * Why a view is empty. It sets the colour of the icon tile and, for
 * [Filtered], adds a "Clear filters" button. The copy always comes from the app.
 */
public enum class MonarchEmptyStateKind {
    /** Nothing has been created yet. The default. */
    FirstRun,

    /** A search or a filter matched nothing; offer a way back. */
    Filtered,

    /** An "all clear" state, such as an empty queue or nothing to fix. */
    Success,
}
