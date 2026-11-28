package dev.monarchui.components.datadisplay.widget

/** What the body of a `MonarchWidget` shows. The same four states exist on every platform. */
public sealed class MonarchWidgetState {
    /** The content. The default. */
    public data object Ready : MonarchWidgetState()

    /** Three placeholder lines, read as "Loading" and the title. The footer is hidden. */
    public data object Loading : MonarchWidgetState()

    /**
     * A small `MonarchEmptyState`.
     *
     * @property message its title; `null` uses the built-in "Nothing to show yet".
     */
    public data class Empty(val message: String? = null) : MonarchWidgetState()

    /**
     * The failure, with Try again when the widget has `onRetry`. It is announced.
     *
     * @property message the failure in plain words; `null` uses the built-in "This section could not be loaded."
     */
    public data class Failed(val message: String? = null) : MonarchWidgetState()
}
