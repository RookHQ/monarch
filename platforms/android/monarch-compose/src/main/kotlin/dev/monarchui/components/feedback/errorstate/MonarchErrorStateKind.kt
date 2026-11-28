package dev.monarchui.components.feedback.errorstate

/**
 * What went wrong, in terms the interface can act on. The app maps its own
 * errors to a kind and still writes the copy. The kind sets the colour of the
 * icon tile and whether a retry is offered.
 */
public enum class MonarchErrorStateKind {
    /** Something failed; retrying may help. The default. */
    Error,

    /** The user may not see this; retrying will not help, so no retry button is shown. */
    Forbidden,

    /** The thing does not exist, or no longer does; no retry button is shown. */
    NotFound,

    /** The request never reached the server. */
    Offline,

    /** The server failed; retrying later may help. */
    Server,

    /** The service is deliberately unavailable for a while. */
    Maintenance,
}
