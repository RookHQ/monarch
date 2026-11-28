package com.rookhq.monarch.foundation

/**
 * The meaning or emphasis a toned component carries: a badge, a banner, a
 * tag, a progress bar, a callout. The names match the web and iOS.
 *
 * A tone is never the only carrier of meaning: keep the text.
 *
 * [Accent] and [Ai] were added after the first five. A `when` over
 * `MonarchTone` in an app needs a branch for each, or an `else` branch (see
 * the changelog).
 */
public enum class MonarchTone {
    /** No status. */
    Neutral,

    /** Information. */
    Info,

    /** A positive result. */
    Success,

    /** Something that needs attention. */
    Warning,

    /** An error or a destructive state. */
    Danger,

    /**
     * The product accent: emphasis without a status, such as "today", a
     * selected filter or the newest items. It has no spoken name.
     */
    Accent,

    /** Content made or suggested by an AI model. Say so in the text too. */
    Ai,
}
