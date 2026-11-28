package dev.monarchui.components.technical.lifecyclebadge

/**
 * The release stages `MonarchLifecycleBadge` knows: an API, SDK or feature
 * moves from planned to deprecated. The same four names exist on every
 * platform.
 *
 * @property key the stage key, as the web and the API write it.
 */
public enum class MonarchLifecycle(public val key: String) {
    /** Announced, not yet usable. Neutral. */
    Planned("planned"),

    /** Usable, may still change. Info. */
    Preview("preview"),

    /** Supported and not expected to change. Success. */
    Stable("stable"),

    /** Still works, due to be removed. Warning. */
    Deprecated("deprecated"),
}
