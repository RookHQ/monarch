package com.rookhq.monarch.components.feedback.banner

/**
 * Whether TalkBack announces a `MonarchBanner` when it appears or its text
 * changes, without the user moving to it.
 */
public enum class MonarchBannerAnnouncement {
    /** Not announced. For a banner that is on the screen from the start. The default. */
    None,

    /** Announced when TalkBack is idle. For a banner that appears as the result of a user action. */
    Polite,

    /** Announced immediately, interrupting speech. Only for an error that blocks the user. */
    Assertive,
}
