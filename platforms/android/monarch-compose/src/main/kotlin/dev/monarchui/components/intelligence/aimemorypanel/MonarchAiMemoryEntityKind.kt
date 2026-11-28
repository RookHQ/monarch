package dev.monarchui.components.intelligence.aimemorypanel

/**
 * The kind of thing a memory is about. `MonarchAiMemoryPanel` groups people
 * first, then organizations, projects and other.
 */
public enum class MonarchAiMemoryEntityKind {
    /** A person, such as a colleague or a customer contact. */
    Person,

    /** A company, a team or another organization. */
    Organization,

    /** A project, a deal or another piece of work. */
    Project,

    /** Anything else, such as the person's own preferences. */
    Other,
}
