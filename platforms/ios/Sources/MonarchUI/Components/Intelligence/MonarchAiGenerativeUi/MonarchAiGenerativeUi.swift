import SwiftUI

/// Generative UI: renders a component the model asked for in a `ui` part,
/// but only when its name is in the app's allow-listed registry.
///
/// ```swift
/// MonarchAiGenerativeUi(part, registry: registry) { action in
///     conversation.submitToolResult(action.toolCallId, output: action.payload)
/// }
/// ```
///
/// Props are untrusted data: they are sanitised (unsafe links dropped, sizes
/// limited), decoded into the component's own `Decodable` props type and
/// checked by its validator. Nothing is evaluated and model output is never
/// rendered as markup. An unknown name, invalid props or a view builder that
/// throws all show a short neutral notice instead, so one bad part never
/// breaks the thread. While props stream, the component renders as soon as
/// they decode, or shows a progress indicator.
///
/// Accessibility: the rendered component brings its own accessibility. The
/// notice is static text with a decorative icon. The progress indicator is
/// named "Loading". Each refusal is reported once through `onReject`, for
/// logging.
public struct MonarchAiGenerativeUi: View {
    private let part: MonarchAiUiPart
    private let registry: MonarchAiUiRegistry
    private let labels: MonarchAiGenerativeUiLabels
    private let onReject: (@MainActor (MonarchAiUiRejection, MonarchAiUiPart) -> Void)?
    private let onAction: (@MainActor (MonarchAiUiAction) -> Void)?

    /// A generated component.
    ///
    /// - Parameters:
    ///   - part: The `ui` part of the message: a component name and its props.
    ///   - registry: The allow-list. Only these names render.
    ///   - labels: Replacements for the built-in strings.
    ///   - onReject: Runs once when a part is refused, for logging.
    ///   - onAction: Receives actions the component sends through its ``MonarchAiUiEmit``,
    ///     with the payload sanitised. Without it the channel is not connected.
    public init(
        _ part: MonarchAiUiPart,
        registry: MonarchAiUiRegistry,
        labels: MonarchAiGenerativeUiLabels = MonarchAiGenerativeUiLabels(),
        onReject: (@MainActor (MonarchAiUiRejection, MonarchAiUiPart) -> Void)? = nil,
        onAction: (@MainActor (MonarchAiUiAction) -> Void)? = nil
    ) {
        self.part = part
        self.registry = registry
        self.labels = labels
        self.onReject = onReject
        self.onAction = onAction
    }

    private var emit: MonarchAiUiEmit {
        guard let onAction else { return MonarchAiUiEmit() }
        let part = part
        return MonarchAiUiEmit { type, payload in
            onAction(MonarchAiGenerativeUiTokens.action(type: type, payload: payload, part: part))
        }
    }

    /// The content of the view.
    public var body: some View {
        let outcome = MonarchAiGenerativeUiTokens.outcome(part: part, registry: registry, emit: emit)
        let rejection = outcome.rejection
        let part = part
        Group {
            switch outcome {
            case .rendered(let view):
                view
            case .pending:
                ProgressView()
                    .frame(maxWidth: .infinity, minHeight: MonarchAiGenerativeUiTokens.pendingMinHeight)
                    .accessibilityLabel(labels.loading ?? MonarchAiGenerativeUiStrings.loading)
            case .rejected(let reason):
                MonarchInlineNotice(
                    MonarchAiGenerativeUiTokens.notice(reason, labels: labels),
                    systemImage: MonarchAiGenerativeUiTokens.noticeSymbol,
                    tone: .neutral
                )
            }
        }
        .task(id: RejectionReport(reason: rejection, part: part)) {
            if let reason = rejection {
                onReject?(reason, part)
            }
        }
    }

    /// Identifies one outcome of one part, so a refusal is reported once.
    private struct RejectionReport: Equatable {
        let reason: MonarchAiUiRejection?
        let part: MonarchAiUiPart
    }
}

#if DEBUG
    private struct MonarchAiGenerativeUiPreviewProps: Decodable, Sendable {
        let city: String
        let temperature: Double?
    }

    #Preview("MonarchAiGenerativeUi") {
        var registry = MonarchAiUiRegistry()
        registry.register("Weather", props: MonarchAiGenerativeUiPreviewProps.self) { props, _ in
            MonarchCard {
                Text(props.city).monarchTextStyle(.subheading)
                Text(props.temperature.map { "\(Int($0))°" } ?? "…").monarchTextStyle(.title)
            }
        }
        return MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiGenerativeUi(
                    MonarchAiUiPart(component: "Weather", props: ["city": "Lisbon", "temperature": 21]),
                    registry: registry
                )
                MonarchAiGenerativeUi(MonarchAiUiPart(component: "Script", props: [:]), registry: registry)
                MonarchAiGenerativeUi(
                    MonarchAiUiPart(component: "Weather", props: ["temperature": 21]),
                    registry: registry
                )
                MonarchAiGenerativeUi(
                    MonarchAiUiPart(component: "Weather", props: "{\"city\": \"Lis", state: .streaming),
                    registry: registry
                )
            }
        }
    }
#endif
