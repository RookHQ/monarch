package dev.monarchui.components.feedback.calloutbanner

/** Where a `MonarchCalloutBanner` sits. The same two names exist on every platform. */
public enum class MonarchCalloutBannerPlacement {
    /** A rounded callout inside the content of a screen or a section. The default. */
    Section,

    /**
     * A full-width strip across the top of a screen, for a trial, maintenance or a billing problem:
     * square corners and a border at the bottom only.
     */
    Page,
}
