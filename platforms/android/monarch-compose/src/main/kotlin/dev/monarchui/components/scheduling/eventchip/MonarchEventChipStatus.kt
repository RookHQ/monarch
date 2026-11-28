package dev.monarchui.components.scheduling.eventchip

/**
 * How an event reads at a glance. Every status other than confirmed is also
 * a word in the chip and in what TalkBack reads, so it never relies on the
 * marker's shape or colour alone. The same four names exist on every
 * platform.
 */
public enum class MonarchEventChipStatus {
    /** The event takes place. A filled dot or a solid bar. The default. */
    Confirmed,

    /** Not yet accepted. A hollow dot or a dashed bar, a lighter tint and a border. */
    Tentative,

    /** The event was cancelled. Muted text, the title struck through. */
    Cancelled,

    /** The person declined. Muted text, the title struck through. */
    Declined,
}
