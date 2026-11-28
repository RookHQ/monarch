/// Generative UI: a component the reply asks to show. Render it only from an
/// allow-listed registry, and treat the props as untrusted data.
public struct MonarchAiUiPart: Sendable, Hashable, Codable {
    /// The name of the component in the registry.
    public var component: String
    /// The props, or, while `state` is `streaming`, JSON text that is still arriving.
    public var props: MonarchAiJSON
    /// A stable id, so actions from the component can name the part.
    public var id: String?
    /// `streaming` while the props are still arriving.
    public var state: MonarchAiPartState?
    /// The tool call this UI answers, so an action can be sent back as its result.
    public var toolCallId: String?

    /// A generative UI part.
    ///
    /// - Parameters:
    ///   - component: The name of the component in the registry.
    ///   - props: The props.
    ///   - id: A stable id.
    ///   - state: `streaming` while the props are still arriving.
    ///   - toolCallId: The tool call this UI answers.
    public init(
        component: String,
        props: MonarchAiJSON,
        id: String? = nil,
        state: MonarchAiPartState? = nil,
        toolCallId: String? = nil
    ) {
        self.component = component
        self.props = props
        self.id = id
        self.state = state
        self.toolCallId = toolCallId
    }
}
