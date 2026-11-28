package dev.monarchui.components.feedback.emptystate

/**
 * The size of a `MonarchEmptyState` or a `MonarchErrorState`. The web calls
 * them compact, inline and page.
 */
public enum class MonarchEmptyStateSize {
    /** Tight spacing and a small title, for a table body, a side panel or a small card. */
    Sm,

    /** Inside a card, a list or a section. The default. */
    Md,

    /** Fills an empty screen, with generous space above and below. */
    Lg,
}
