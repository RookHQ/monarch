package dev.monarchui.components.collaboration.activitytimeline

/** Who did something in a `MonarchActivityTimeline`. Automations and the system get an "Automated" tag. */
public enum class MonarchActivityTimelineActorKind {
    /** A person. */
    Person,

    /** A rule or a workflow the app runs. */
    Automation,

    /** The system itself. */
    System,
}
