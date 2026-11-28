package dev.monarchui.components.feedback.calloutbanner

/** The form of a `MonarchCalloutBanner`. The same two names exist on every platform. */
public enum class MonarchCalloutBannerVariant {
    /** A message with an icon, an optional title and an action under the text. The default. */
    Callout,

    /**
     * A compact one-line notice that the view is narrowed, for example "Showing tickets in Support",
     * with the action at the end. The tone is not read by TalkBack.
     */
    Scope,
}
