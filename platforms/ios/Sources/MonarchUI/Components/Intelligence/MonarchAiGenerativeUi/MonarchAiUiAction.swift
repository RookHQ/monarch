/// An action from a generated component, as the host's `onAction` receives it.
///
/// Send it back to the agent, for example as the result of `toolCallId` or
/// as a new message.
public struct MonarchAiUiAction: Sendable, Hashable {
    /// What happened, such as `submit`, `select` or `confirm`.
    public var type: String
    /// Plain data from the component, sanitised like props.
    public var payload: MonarchAiJSON?
    /// The registered name of the component that sent it.
    public var component: String
    /// The id of the part, when it has one.
    public var partId: String?
    /// The tool call the part answers, when it has one.
    public var toolCallId: String?

    /// An action.
    ///
    /// - Parameters:
    ///   - type: What happened.
    ///   - payload: Plain data from the component.
    ///   - component: The registered name of the component.
    ///   - partId: The id of the part.
    ///   - toolCallId: The tool call the part answers.
    public init(
        type: String,
        payload: MonarchAiJSON? = nil,
        component: String,
        partId: String? = nil,
        toolCallId: String? = nil
    ) {
        self.type = type
        self.payload = payload
        self.component = component
        self.partId = partId
        self.toolCallId = toolCallId
    }
}
