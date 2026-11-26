import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchAiGenerativeUi``.
enum MonarchAiGenerativeUiTokens {
    /// The SF Symbol of the notice shown instead of a refused part.
    static let noticeSymbol = "info.circle"
    /// The least height of the progress indicator while props stream.
    static let pendingMinHeight: CGFloat = MonarchAccessibility.minimumHitTarget

    /// What the view shows for a part.
    enum Outcome {
        /// The component.
        case rendered(AnyView)
        /// A progress indicator: the props are still arriving.
        case pending
        /// A notice: the part was refused.
        case rejected(MonarchAiUiRejection)

        /// The rejection, when the part was refused.
        var rejection: MonarchAiUiRejection? {
            if case .rejected(let reason) = self { return reason }
            return nil
        }

        /// Whether the part is waiting for more props.
        var isPending: Bool {
            if case .pending = self { return true }
            return false
        }
    }

    /// The props to decode: streaming JSON text is parsed as far as it goes;
    /// complete props must already be data.
    static func rawProps(_ part: MonarchAiUiPart) -> MonarchAiJSON {
        guard part.state == .streaming, case .string(let text) = part.props else { return part.props }
        return MonarchAiPartialJSON.parse(text) ?? .object([:])
    }

    /// Resolves a part against the registry. A part that is still streaming
    /// is never refused for its props, because they may still become valid.
    @MainActor static func outcome(
        part: MonarchAiUiPart,
        registry: MonarchAiUiRegistry,
        emit: MonarchAiUiEmit
    ) -> Outcome {
        guard let entry = registry.entry(part.component) else { return .rejected(.unknownComponent) }
        let isStreaming = part.state == .streaming
        if isStreaming && entry.streaming == .onComplete { return .pending }
        guard let props = MonarchAiUiSanitizer.sanitize(rawProps(part)) else {
            return isStreaming ? .pending : .rejected(.invalidProps)
        }
        switch entry.render(props, emit) {
        case .success(let view):
            return .rendered(view)
        case .failure(let reason):
            return isStreaming && reason == .invalidProps ? .pending : .rejected(reason)
        }
    }

    /// The notice for a rejection.
    static func notice(_ reason: MonarchAiUiRejection, labels: MonarchAiGenerativeUiLabels) -> String {
        let custom: String? =
            switch reason {
            case .unknownComponent: labels.unknown
            case .invalidProps: labels.invalid
            case .renderError: labels.failed
            }
        return custom ?? MonarchAiGenerativeUiStrings.notice(reason)
    }

    /// The action a component sent, with its payload sanitised and the part named.
    static func action(type: String, payload: MonarchAiJSON?, part: MonarchAiUiPart) -> MonarchAiUiAction {
        MonarchAiUiAction(
            type: type,
            payload: payload.flatMap { MonarchAiUiSanitizer.clean($0, key: "payload", depth: 0) },
            component: part.component,
            partId: part.id,
            toolCallId: part.toolCallId
        )
    }

    /// The manifest lines: `- Name: description`, or `- Name` without one.
    static func manifest(_ entries: [(name: String, description: String?)]) -> String {
        entries.map { entry in
            guard let description = entry.description, !description.isEmpty else { return "- \(entry.name)" }
            return "- \(entry.name): \(description)"
        }
        .joined(separator: "\n")
    }
}
