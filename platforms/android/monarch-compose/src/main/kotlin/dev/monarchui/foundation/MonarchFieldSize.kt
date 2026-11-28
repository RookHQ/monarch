package dev.monarchui.foundation

/**
 * The visible height step of a field. Medium is the default. The values are native, not the web pointer
 * sizes: 40dp, 48dp and 52dp, so a field matches the touch target minimum at the default size.
 */
public enum class MonarchFieldSize {
    /** 40dp, for dense forms. The hit area stays 48dp. */
    Small,

    /** 48dp, the default. */
    Medium,

    /** 52dp, for prominent fields such as a search bar. */
    Large,
}
