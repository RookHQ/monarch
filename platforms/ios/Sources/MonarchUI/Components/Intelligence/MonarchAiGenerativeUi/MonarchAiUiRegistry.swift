import SwiftUI

/// The allow-list of components that generative UI may render, by the name
/// the model uses.
///
/// ```swift
/// struct WeatherProps: Decodable, Sendable {
///     let city: String
///     let temperature: Double?
/// }
///
/// var registry = MonarchAiUiRegistry()
/// registry.register("Weather", props: WeatherProps.self, description: "Current weather for a city.") { props, _ in
///     WeatherCard(city: props.city, temperature: props.temperature)
/// }
/// ```
///
/// Only names registered here resolve; any other name is refused. Props are
/// untrusted data: they are sanitised, then decoded into the component's own
/// `Decodable` type, then checked by the optional `validate`. Nothing the
/// model sends is evaluated, and no view is chosen by anything but the exact
/// name.
public struct MonarchAiUiRegistry: Sendable {
    /// One registered component, with its props type erased.
    struct Entry: Sendable {
        /// One sentence for the model.
        let description: String?
        /// How it renders while props stream.
        let streaming: MonarchAiUiStreaming
        /// Decodes, validates and builds the view.
        let render: @MainActor @Sendable (MonarchAiJSON, MonarchAiUiEmit) -> Result<AnyView, MonarchAiUiRejection>
    }

    private var entries: [String: Entry] = [:]
    private var order: [String] = []

    /// An empty registry.
    public init() {}

    /// The registered names, in the order they were first registered.
    public var names: [String] { order }

    /// Whether a name is registered.
    public func contains(_ name: String) -> Bool {
        entries[name] != nil
    }

    /// The entry of a name.
    func entry(_ name: String) -> Entry? {
        entries[name]
    }

    /// Registers a component.
    ///
    /// Registering a name again replaces the earlier component. An empty name is ignored.
    ///
    /// - Parameters:
    ///   - name: The name the model uses, such as `"Weather"`. Matched exactly.
    ///   - props: The `Decodable` type the props must decode into. Make props that may
    ///     arrive late optional, so the component can render while they stream.
    ///   - description: One sentence for the model: when to use this component. Listed by `manifest()`.
    ///   - streaming: Whether it renders while props stream, or waits for the complete part.
    ///   - validate: Checks the decoded props. Return `false` to refuse them.
    ///   - content: Builds the view from the props and the action channel. It may throw, which
    ///     shows the "couldn’t be displayed" notice.
    public mutating func register<Props: Decodable & Sendable, Content: View>(
        _ name: String,
        props: Props.Type,
        description: String? = nil,
        streaming: MonarchAiUiStreaming = .progressive,
        validate: (@Sendable (Props) -> Bool)? = nil,
        @ViewBuilder content: @escaping @MainActor @Sendable (Props, MonarchAiUiEmit) throws -> Content
    ) {
        guard !name.isEmpty else { return }
        let render: @MainActor @Sendable (MonarchAiJSON, MonarchAiUiEmit) -> Result<AnyView, MonarchAiUiRejection> = {
            json,
            emit in
            guard let decoded = try? MonarchAiUiSanitizer.decode(props, from: json) else {
                return .failure(.invalidProps)
            }
            if let validate, !validate(decoded) { return .failure(.invalidProps) }
            do {
                // The registry holds components of different types under one name lookup,
                // so the built view is type-erased here and nowhere else.
                return .success(AnyView(try content(decoded, emit)))
            } catch {
                return .failure(.renderError)
            }
        }
        if entries[name] == nil { order.append(name) }
        entries[name] = Entry(description: description, streaming: streaming, render: render)
    }

    /// A compact list of what may be rendered, one line per component, to put
    /// in the model's system prompt: `- Weather: Current weather for a city.`
    public func manifest() -> String {
        MonarchAiGenerativeUiTokens.manifest(order.map { ($0, entries[$0]?.description) })
    }
}
