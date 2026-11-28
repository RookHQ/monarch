package dev.monarchui.components.workflow.roadmap

/** The health last reported for a project or an initiative on a `MonarchRoadmap`. */
public enum class MonarchRoadmapHealth {
    /** Going as planned. Written as "On track"; the bar keeps the accent colour. */
    OnTrack,

    /** Likely to slip. Written as "At risk"; the bar takes the warning tone. */
    AtRisk,

    /** Behind plan. Written as "Off track"; the bar takes the danger tone. */
    OffTrack,

    /** Health is tracked but nobody has reported it yet. Written as "No health reported". */
    NotReported,
}
