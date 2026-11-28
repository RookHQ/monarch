import SwiftUI

/// AI-suggested edits marked inside the text: deletions struck through,
/// insertions underlined, with "Change 2 of 5", previous and next, and Accept
/// and Reject for the change on show.
///
/// ```swift
/// @State private var segments = MonarchAiDiffSegment.words(
///     from: "The meeting is on Monday.",
///     to: "The planning meeting is on Tuesday."
/// )
///
/// MonarchAiInlineDiff(
///     segments,
///     onAccept: { id in segments = MonarchAiInlineDiff.resolve(segments, change: id, decision: .accept) },
///     onReject: { id in segments = MonarchAiInlineDiff.resolve(segments, change: id, decision: .reject) }
/// )
/// ```
///
/// It is controlled and editor-agnostic: it shows the segments and reports
/// decisions, and the app applies them, usually with ``resolve(_:change:decision:)``.
/// After a decision the change that takes its place becomes the one on show.
/// Without `onAccept` and `onReject` the changes can be stepped through but
/// not decided. A tap on a change in the text makes it the one on show.
///
/// Accessibility: the diff is a group named "AI suggested changes". The text
/// is one element that reads unchanged text as it is and each removal and
/// addition after "Deleted:" or "Inserted:"; its value is "Change 2 of 5",
/// and it has the named actions Next change, Previous change, Accept and
/// Reject, so nothing needs a drag or a precise tap. Accept and Reject are
/// buttons named "Accept change 2 of 5" with 44-point touch targets.
/// Decisions are announced ("Change accepted. 3 left."). Changes are never
/// marked by colour alone: strike-through, underline and bold carry them.
@_spi(Experimental) public struct MonarchAiInlineDiff: View {
    private let segments: [MonarchAiDiffSegment]
    private let showsNavigation: Bool
    private let labels: MonarchAiInlineDiffLabels
    private let onAccept: (@MainActor (String) -> Void)?
    private let onReject: (@MainActor (String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @State private var active = 0

    private typealias Tokens = MonarchAiInlineDiffTokens

    /// An inline diff.
    ///
    /// - Parameters:
    ///   - segments: The text as unchanged, inserted and deleted runs. Update them after a decision.
    ///   - showsNavigation: Whether the position and the previous and next buttons show.
    ///   - labels: Replacements for the built-in strings.
    ///   - onAccept: Accepts a change, given its id. Shows Accept.
    ///   - onReject: Rejects a change, given its id. Shows Reject.
    public init(
        _ segments: [MonarchAiDiffSegment],
        showsNavigation: Bool = true,
        labels: MonarchAiInlineDiffLabels = MonarchAiInlineDiffLabels(),
        onAccept: (@MainActor (String) -> Void)? = nil,
        onReject: (@MainActor (String) -> Void)? = nil
    ) {
        self.segments = segments
        self.showsNavigation = showsNavigation
        self.labels = labels
        self.onAccept = onAccept
        self.onReject = onReject
    }

    /// The changes in `segments`: each run of adjacent deleted and inserted
    /// segments that share an id (or all have none).
    ///
    /// - Parameter segments: The runs of the text.
    /// - Returns: The changes, in reading order.
    nonisolated public static func changes(in segments: [MonarchAiDiffSegment]) -> [MonarchAiInlineDiffChange] {
        MonarchAiInlineDiffTokens.changes(segments)
    }

    /// The segments with one change applied (accept) or dropped (reject).
    /// Adjacent unchanged text is merged.
    ///
    /// - Parameters:
    ///   - segments: The runs of the text.
    ///   - id: The id of the change.
    ///   - decision: Accept or reject.
    /// - Returns: The new runs; the same runs when no change has that id.
    nonisolated public static func resolve(
        _ segments: [MonarchAiDiffSegment],
        change id: String,
        decision: MonarchAiInlineDiffDecision
    ) -> [MonarchAiDiffSegment] {
        MonarchAiInlineDiffTokens.resolve(segments, id: id, decision: decision)
    }

    /// The content of the view.
    public var body: some View {
        let changes = Tokens.changes(segments)
        let total = changes.count
        let current = Tokens.clamp(active, total: total)
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            if showsNavigation {
                navigation(current: current, total: total)
            }
            text(changes: changes, current: current)
            if total > 0, onAccept != nil || onReject != nil {
                decisions(changes[current], current: current, total: total)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.label ?? MonarchAiInlineDiffStrings.label)
    }

    @ViewBuilder
    private func navigation(current: Int, total: Int) -> some View {
        if total > 0 {
            let labels = labels
            MonarchAiMessageBranch(
                selection: Binding(get: { current }, set: { active = $0 }),
                count: total,
                showsSingle: true,
                label: Tokens.position(current, total: total, labels: labels),
                previousLabel: labels.previous ?? MonarchAiInlineDiffStrings.previous,
                nextLabel: labels.next ?? MonarchAiInlineDiffStrings.next,
                formatPosition: { index, count in Tokens.position(index - 1, total: count, labels: labels) },
                formatAnnouncement: { index, count in Tokens.position(index - 1, total: count, labels: labels) }
            )
            .environment(\.monarchBranchIsVertical, true)
        } else {
            Text(Tokens.position(0, total: 0, labels: labels))
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
        }
    }

    private func text(changes: [MonarchAiInlineDiffChange], current: Int) -> some View {
        let total = changes.count
        return Text(Tokens.attributed(segments, changes: changes, active: current, colors: theme.colors))
            .monarchTextStyle(.body)
            .tint(theme.colors.textDefault)
            .multilineTextAlignment(.leading)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
            .environment(
                \.openURL,
                OpenURLAction { url in
                    guard let index = Tokens.change(from: url) else { return .systemAction }
                    if index < total { active = index }
                    return .handled
                }
            )
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Tokens.spokenText(segments, labels: labels))
            .accessibilityValue(total > 0 ? Tokens.position(current, total: total, labels: labels) : "")
            .accessibilityActions {
                if total > 1 {
                    Button(labels.next ?? MonarchAiInlineDiffStrings.next) {
                        active = (current + 1) % total
                    }
                    Button(labels.previous ?? MonarchAiInlineDiffStrings.previous) {
                        active = (current - 1 + total) % total
                    }
                }
                if total > 0 {
                    if onAccept != nil {
                        Button(Tokens.buttonLabel(.accept, index: current, total: total, labels: labels)) {
                            decide(.accept, change: changes[current], total: total)
                        }
                    }
                    if onReject != nil {
                        Button(Tokens.buttonLabel(.reject, index: current, total: total, labels: labels)) {
                            decide(.reject, change: changes[current], total: total)
                        }
                    }
                }
            }
    }

    private func decisions(_ change: MonarchAiInlineDiffChange, current: Int, total: Int) -> some View {
        HStack(spacing: Tokens.buttonSpacing) {
            if onAccept != nil {
                MonarchButton(
                    labels.acceptTitle ?? MonarchAiInlineDiffStrings.acceptTitle,
                    systemImage: Tokens.acceptSymbol,
                    variant: .ghost,
                    size: .sm,
                    isIconOnly: true
                ) {
                    decide(.accept, change: change, total: total)
                }
                .accessibilityLabel(Tokens.buttonLabel(.accept, index: current, total: total, labels: labels))
            }
            if onReject != nil {
                MonarchButton(
                    labels.rejectTitle ?? MonarchAiInlineDiffStrings.rejectTitle,
                    systemImage: Tokens.rejectSymbol,
                    variant: .ghost,
                    size: .sm,
                    isIconOnly: true
                ) {
                    decide(.reject, change: change, total: total)
                }
                .accessibilityLabel(Tokens.buttonLabel(.reject, index: current, total: total, labels: labels))
            }
        }
    }

    private func decide(_ decision: MonarchAiInlineDiffDecision, change: MonarchAiInlineDiffChange, total: Int) {
        AccessibilityNotification.Announcement(
            Tokens.announcement(decision, left: total - 1, labels: labels)
        ).post()
        switch decision {
        case .accept: onAccept?(change.id)
        case .reject: onReject?(change.id)
        }
    }
}

#if DEBUG
    #Preview("MonarchAiInlineDiff") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiInlineDiff(
                    MonarchAiDiffSegment.words(
                        from: "The meeting is on Monday at noon.",
                        to: "The planning meeting is on Tuesday at noon."
                    ),
                    onAccept: { _ in },
                    onReject: { _ in }
                )
                MonarchAiInlineDiff(MonarchAiDiffSegment.words(from: "Read only.", to: "Read-only text."))
                MonarchAiInlineDiff([MonarchAiDiffSegment(.equal, text: "No changes here.")])
            }
        }
    }
#endif
