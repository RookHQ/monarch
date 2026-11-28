/// Why ``MonarchAiGenerativeUi`` did not render a part.
public enum MonarchAiUiRejection: String, Error, Sendable, Hashable, CaseIterable {
    /// The name is not in the registry.
    case unknownComponent
    /// The props are not plain data, do not decode into the component's
    /// props type, or fail its validation.
    case invalidProps
    /// The component's view builder threw.
    case renderError
}
