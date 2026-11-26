/// Whether the connection behind a ``MonarchAiConnector`` works right now.
public enum MonarchAiConnectorStatus: String, Sendable, Hashable, CaseIterable {
    /// Connected. Only a connected connector can be turned on.
    case connected
    /// The person has to sign in to the app first.
    case needsSignIn
    /// The connection failed and has to be reconnected.
    case error
    /// The connector cannot be used right now.
    case unavailable
}
