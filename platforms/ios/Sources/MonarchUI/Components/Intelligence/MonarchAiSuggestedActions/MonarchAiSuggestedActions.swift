import SwiftUI

/// The next best actions for a record, each with Run, an optional approval
/// step and dismiss, and where the suggestions came from.
///
/// ```swift
/// MonarchAiSuggestedActions(
///     actions,
///     onDismiss: { id in actions.removeAll { $0.id == id } },
///     onRun: { id in try await api.perform(id) }
/// )
/// ```
///
/// Run calls `onRun` and shows "Running…" until it returns, then "Done";
/// if it throws, a failure line appears and Run is available again. An
/// action that needs approval (`requiresApproval`, or a destructive risk)
/// opens a system alert first with its description and consequences; a
/// destructive action's confirm button has the destructive role. Set an
/// action's `status` when the app tracks runs itself. The rows are a plain
/// stack, not a `List`, because the list is short and sits on a scrolling
/// record screen.
///
/// Accessibility: the title is a header followed by the origin, which is
/// text as well as colour. Each row's label, description and tags are read
/// together; Run is named "Run “…”" and reads "Running…" as its value
/// while it runs; dismiss is named "Dismiss “…”". "“…” done." is announced
/// when a run finishes and the failure is announced; the success and error
/// haptics play. Every button is at least 44 points and the buttons sit
/// under the text, so long labels wrap at the largest text sizes.
@_spi(Experimental) public struct MonarchAiSuggestedActions: View {
    private let actions: [MonarchAiSuggestedAction]
    private let title: String?
    private let origin: MonarchAiSuggestedActionsOrigin
    private let labels: MonarchAiSuggestedActionsLabels
    private let onDismiss: (@MainActor (String) -> Void)?
    private let onRun: @MainActor (String) async throws -> Void

    @State private var tracked: [String: MonarchAiSuggestedActionStatus] = [:]
    @State private var approving: MonarchAiSuggestedAction?
    @State private var successes = 0
    @State private var failures = 0

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private typealias Tokens = MonarchAiSuggestedActionsTokens

    /// A list of suggested actions.
    ///
    /// - Parameters:
    ///   - actions: The suggestions, most useful first.
    ///   - title: The heading. Built in: "Suggested actions".
    ///   - origin: Where the suggestions came from, always shown.
    ///   - labels: Replacements for the built-in strings.
    ///   - onDismiss: Adds a dismiss button to each action. Remove the action from `actions`.
    ///   - onRun: Runs an action by id, after approval when it needs it. Throw to mark it failed.
    public init(
        _ actions: [MonarchAiSuggestedAction],
        title: String? = nil,
        origin: MonarchAiSuggestedActionsOrigin = .ai,
        labels: MonarchAiSuggestedActionsLabels = MonarchAiSuggestedActionsLabels(),
        onDismiss: (@MainActor (String) -> Void)? = nil,
        onRun: @escaping @MainActor (String) async throws -> Void
    ) {
        self.actions = actions
        self.title = title
        self.origin = origin
        self.labels = labels
        self.onDismiss = onDismiss
        self.onRun = onRun
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            heading
            if actions.isEmpty {
                Text(labels.empty ?? MonarchAiSuggestedActionsStrings.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            } else {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(actions) { action in
                        row(action)
                        if action.id != actions.last?.id {
                            Divider().overlay(theme.colors.borderSubtle)
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .sensoryFeedback(.success, trigger: successes)
        .sensoryFeedback(.error, trigger: failures)
        .alert(
            approving.map { Tokens.approvalTitle($0, labels: labels) } ?? "",
            isPresented: Binding(get: { approving != nil }, set: { if !$0 { approving = nil } }),
            presenting: approving,
            actions: { action in
                Button(Tokens.confirmTitle(action, labels: labels), role: Tokens.confirmRole(action)) {
                    run(action)
                }
                Button(labels.cancel ?? MonarchAiSuggestedActionsStrings.cancel, role: .cancel) {}
            },
            message: { action in
                Text(Tokens.approvalMessage(action, labels: labels))
            }
        )
    }

    private var heading: some View {
        MonarchFlowLayout(spacing: Tokens.itemSpacing, rowSpacing: 0, alignment: .firstTextBaseline) {
            Text(title ?? MonarchAiSuggestedActionsStrings.title)
                .monarchTextStyle(.heading)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            Label {
                Text(Tokens.originText(origin, labels: labels))
                    .monarchTextStyle(.caption)
            } icon: {
                MonarchIcon(systemName: Tokens.originSymbol(origin), size: .sm)
            }
            .foregroundStyle(theme.colors[role: Tokens.originColor(origin)])
            .accessibilityElement(children: .combine)
        }
    }

    private func row(_ action: MonarchAiSuggestedAction) -> some View {
        let status = Tokens.status(action, tracked: tracked)
        return VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            // The text and the Run and dismiss buttons share a row, as on the web; at accessibility sizes they stack.
            let layout =
                dynamicTypeSize.isAccessibilitySize
                ? AnyLayout(VStackLayout(alignment: .leading, spacing: Tokens.rowSpacing))
                : AnyLayout(HStackLayout(alignment: .top, spacing: Tokens.itemSpacing))
            layout {
                VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
                    Text(action.label)
                        .monarchTextStyle(.bodyStrong)
                        .foregroundStyle(theme.colors[role: Tokens.labelColor(status)])
                    if let description = action.description, !description.isEmpty {
                        Text(description)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                    let risk = Tokens.riskTag(action.risk, labels: labels)
                    if action.needsApproval || risk != nil {
                        MonarchFlowLayout(spacing: Tokens.itemSpacing, rowSpacing: Tokens.itemSpacing) {
                            if let risk {
                                MonarchBadge(risk.text, tone: risk.tone, systemImage: Tokens.riskSymbol)
                            }
                            if action.needsApproval {
                                MonarchBadge(
                                    labels.needsApproval ?? MonarchAiSuggestedActionsStrings.needsApproval,
                                    systemImage: Tokens.approvalSymbol
                                )
                            }
                        }
                    }
                }
                .accessibilityElement(children: .combine)
                .frame(maxWidth: .infinity, alignment: .leading)
                HStack(spacing: Tokens.itemSpacing) {
                    if status == .done {
                        Label {
                            Text(labels.done ?? MonarchAiSuggestedActionsStrings.done)
                                .monarchTextStyle(.label)
                        } icon: {
                            MonarchIcon(systemName: Tokens.doneSymbol, size: .sm)
                        }
                        .foregroundStyle(theme.colors.successStrong)
                        .monarchMinimumHitTarget()
                        .accessibilityElement(children: .combine)
                    } else {
                        MonarchButton(
                            labels.run ?? MonarchAiSuggestedActionsStrings.run,
                            variant: .secondary,
                            size: .sm,
                            isLoading: status == .running,
                            loadingLabel: labels.running ?? MonarchAiSuggestedActionsStrings.running
                        ) {
                            guard Tokens.canRun(Tokens.status(action, tracked: tracked)) else { return }
                            if action.needsApproval {
                                approving = action
                            } else {
                                run(action)
                            }
                        }
                        .accessibilityLabel(Tokens.runLabel(action, labels: labels))
                    }
                    if let onDismiss {
                        Button {
                            onDismiss(action.id)
                        } label: {
                            Label(Tokens.dismissLabel(action, labels: labels), systemImage: Tokens.dismissSymbol)
                                .labelStyle(.iconOnly)
                        }
                        .buttonStyle(.monarch(.ghost, size: .sm))
                    }
                }
            }
            if status == .failed {
                Label {
                    Text(labels.failed ?? MonarchAiSuggestedActionsStrings.failed)
                        .monarchTextStyle(.caption)
                } icon: {
                    MonarchIcon(systemName: Tokens.failedSymbol, size: .sm)
                }
                .foregroundStyle(theme.colors.dangerStrong)
                .accessibilityElement(children: .combine)
            }
        }
        .padding(.vertical, Tokens.rowPadding)
    }

    private func run(_ action: MonarchAiSuggestedAction) {
        tracked[action.id] = .running
        Task { @MainActor in
            do {
                try await onRun(action.id)
                tracked[action.id] = .done
                successes += 1
                AccessibilityNotification.Announcement(Tokens.doneAnnouncement(action, labels: labels)).post()
            } catch {
                tracked[action.id] = .failed
                failures += 1
                AccessibilityNotification.Announcement(labels.failed ?? MonarchAiSuggestedActionsStrings.failed).post()
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchAiSuggestedActions") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiSuggestedActions(
                    [
                        MonarchAiSuggestedAction(
                            id: "remind",
                            label: "Send the renewal reminder",
                            description: "The contract ends in 14 days."
                        ),
                        MonarchAiSuggestedAction(
                            id: "close",
                            label: "Close 4 stale tickets",
                            risk: .destructive,
                            consequences: ["4 tickets will be closed"],
                            confirmLabel: "Close tickets"
                        ),
                        MonarchAiSuggestedAction(id: "call", label: "Call the customer back", status: .done),
                    ],
                    onDismiss: { _ in },
                    onRun: { _ in }
                )
                MonarchAiSuggestedActions([], origin: .rule) { _ in }
            }
        }
    }
#endif
