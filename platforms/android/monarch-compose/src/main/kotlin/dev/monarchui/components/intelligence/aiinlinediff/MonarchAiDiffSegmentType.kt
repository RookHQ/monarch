package dev.monarchui.components.intelligence.aiinlinediff

/** What a [MonarchAiDiffSegment] is: text that stays, text the suggestion adds, or text it removes. */
public enum class MonarchAiDiffSegmentType {
    /** Text that is in both versions. */
    Equal,

    /** Text the suggestion adds. */
    Insert,

    /** Text the suggestion removes. */
    Delete,
}
