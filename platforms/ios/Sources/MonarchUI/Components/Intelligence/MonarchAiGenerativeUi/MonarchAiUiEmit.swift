/// The action channel a generated component receives: call it to send an
/// action, such as a form submission, back to the host.
///
/// ```swift
/// Button("Book") { emit("submit", payload: ["slot": .string(props.slot)]) }
/// ```
///
/// The payload is sanitised before it reaches the host. When the host does
/// not listen for actions, calling it does nothing; `isConnected` says which.
public struct MonarchAiUiEmit: Sendable {
    private let handler: (@MainActor @Sendable (String, MonarchAiJSON?) -> Void)?

    /// An action channel. Monarch creates it; build one yourself only in tests and previews.
    ///
    /// - Parameter handler: Receives the type and the payload of each action.
    public init(_ handler: (@MainActor @Sendable (String, MonarchAiJSON?) -> Void)? = nil) {
        self.handler = handler
    }

    /// Whether the host listens for actions. Hide controls that only send actions when it does not.
    public var isConnected: Bool { handler != nil }

    /// Sends an action. An empty type is ignored.
    ///
    /// - Parameters:
    ///   - type: What happened, such as `submit`.
    ///   - payload: Plain data to send with it.
    @MainActor public func callAsFunction(_ type: String, payload: MonarchAiJSON? = nil) {
        guard !type.isEmpty else { return }
        handler?(type, payload)
    }
}
