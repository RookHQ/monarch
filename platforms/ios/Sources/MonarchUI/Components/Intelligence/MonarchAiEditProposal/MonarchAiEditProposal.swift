import SwiftUI

/// A reviewable diff of edits an AI proposes to a document or record. Each
/// change shows a word-level diff and can be accepted or rejected on its
/// own, or all at once; once applied the edits can be reverted.
///
/// ```swift
/// MonarchAiEditProposal(
///     part,
///     hunks: hunks,
///     onAcceptAll: { apply(all: true) },
///     onRejectAll: { discard() },
///     onAcceptHunk: { id in apply(id) },
///     onRejectHunk: { id in skip(id) }
/// )
/// ```
///
/// Nothing changes until a person decides, so AI edits are never applied
/// silently. The part's `state` decides whether the proposal can still be
/// changed: while `proposed` each change has Accept and Reject, and the card
/// has Accept all and Reject all; once `applied` it has Revert. Decisions
/// are remembered per proposal id unless the app passes each change's
/// `status`. Removed words are struck through and added words underlined,
/// so a change never depends on colour.
///
/// Accessibility: the card is a container named by its title. Each change
/// is one element read as its name, its decision and the diff with
/// "Deleted:" and "Inserted:" before the removed and added words; its
/// buttons are named with the change ("Accept, Change 2 of 3"). After a
/// decision the result is announced ("Change accepted. 2 left.") and
/// VoiceOver focus moves to the next undecided change, or to the title.
@_spi(Experimental) public struct MonarchAiEditProposal: View {
    private let part: MonarchAiEditProposalPart
    private let hunks: [MonarchAiEditHunk]
    private let title: String?
    private let labels: MonarchAiEditProposalLabels
    private let onAcceptAll: (@MainActor () -> Void)?
    private let onRejectAll: (@MainActor () -> Void)?
    private let onAcceptHunk: (@MainActor (String) -> Void)?
    private let onRejectHunk: (@MainActor (String) -> Void)?
    private let onRevert: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @State private var decisions: [String: MonarchAiEditHunkStatus] = [:]
    @State private var decisionsPartID: String?
    @AccessibilityFocusState private var focus: MonarchAiEditProposalFocus?

    /// An edit proposal.
    ///
    /// - Parameters:
    ///   - part: The edit-proposal part of the message. Its `state` decides
    ///     whether the edits can still be changed; its `id` scopes remembered decisions.
    ///   - hunks: The changes, in document order.
    ///   - title: Replaces the built-in title "Suggested edits".
    ///   - labels: Replacements for the built-in strings.
    ///   - onAcceptAll: Accepts every undecided change. Shows Accept all while proposed.
    ///   - onRejectAll: Rejects every undecided change. Shows Reject all while proposed.
    ///   - onAcceptHunk: Accepts one change, by id. Shows Accept on each change while proposed.
    ///   - onRejectHunk: Rejects one change, by id. Shows Reject on each change while proposed.
    ///   - onRevert: Undoes applied edits. Shows Revert once applied.
    public init(
        _ part: MonarchAiEditProposalPart,
        hunks: [MonarchAiEditHunk],
        title: String? = nil,
        labels: MonarchAiEditProposalLabels = MonarchAiEditProposalLabels(),
        onAcceptAll: (@MainActor () -> Void)? = nil,
        onRejectAll: (@MainActor () -> Void)? = nil,
        onAcceptHunk: (@MainActor (String) -> Void)? = nil,
        onRejectHunk: (@MainActor (String) -> Void)? = nil,
        onRevert: (@MainActor () -> Void)? = nil
    ) {
        self.part = part
        self.hunks = hunks
        self.title = title
        self.labels = labels
        self.onAcceptAll = onAcceptAll
        self.onRejectAll = onRejectAll
        self.onAcceptHunk = onAcceptHunk
        self.onRejectHunk = onRejectHunk
        self.onRevert = onRevert
    }

    private typealias Tokens = MonarchAiEditProposalTokens

    private var decided: [String: MonarchAiEditHunkStatus] {
        decisionsPartID == part.id ? decisions : [:]
    }

    private var statuses: [MonarchAiEditHunkStatus] {
        hunks.map { Tokens.status(of: $0, decided: decided, state: part.state) }
    }

    /// The content of the view.
    public var body: some View {
        let statuses = statuses
        let pending = statuses.count { $0 == .proposed }
        let shape = RoundedRectangle(cornerRadius: Tokens.cornerRadius, style: .continuous)
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            header(pending: pending)
            ForEach(Array(hunks.enumerated()), id: \.element.id) { index, hunk in
                hunkView(hunk, index: index, status: statuses[index])
            }
            actions(pending: pending)
        }
        .padding(Tokens.padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors.bgSurface))
        .overlay(alignment: .leading) {
            Rectangle()
                .fill(theme.colors[role: Tokens.bar])
                .frame(width: Tokens.barWidth)
                .accessibilityHidden(true)
        }
        .clipShape(shape)
        .overlay(shape.strokeBorder(theme.colors[role: Tokens.border], lineWidth: MonarchShapes.borderWidth))
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title ?? MonarchAiEditProposalStrings.title)
    }

    private func header(pending: Int) -> some View {
        VStack(alignment: .leading, spacing: Tokens.headerSpacing) {
            MonarchFlowLayout(spacing: Tokens.badgeSpacing) {
                Text(title ?? MonarchAiEditProposalStrings.title)
                    .monarchTextStyle(.body, weight: Tokens.titleWeight)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityFocused($focus, equals: .title)
                MonarchAiGeneratedBadge(size: .sm, label: labels.badge ?? MonarchAiEditProposalStrings.badge)
                MonarchBadge(Tokens.stateName(part.state, labels: labels), tone: Tokens.tone(part.state))
            }
            Text(Tokens.summary(state: part.state, total: hunks.count, pending: pending, labels: labels))
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
        }
    }

    private func hunkView(_ hunk: MonarchAiEditHunk, index: Int, status: MonarchAiEditHunkStatus) -> some View {
        let name = Tokens.hunkName(hunk, index: index, total: hunks.count, labels: labels)
        let segments = MonarchAiEditProposalDiff.words(hunk.before, hunk.after)
        let isChanged = Tokens.isChanged(segments)
        let unchanged = labels.unchanged ?? MonarchAiEditProposalStrings.unchanged
        let spoken =
            isChanged
            ? Tokens.spokenDiff(
                segments,
                deleted: labels.deleted ?? MonarchAiEditProposalStrings.deleted,
                inserted: labels.inserted ?? MonarchAiEditProposalStrings.inserted
            )
            : "\(hunk.after) (\(unchanged))"
        let statusName = status == .proposed ? nil : Tokens.hunkStateName(status, labels: labels)
        let shape = RoundedRectangle(cornerRadius: Tokens.hunkCornerRadius, style: .continuous)
        return VStack(alignment: .leading, spacing: Tokens.hunkSpacing) {
            VStack(alignment: .leading, spacing: Tokens.hunkSpacing) {
                HStack(alignment: .firstTextBaseline, spacing: Tokens.hunkSpacing) {
                    Text(name)
                        .monarchTextStyle(.caption, weight: Tokens.labelWeight)
                        .foregroundStyle(theme.colors.textMuted)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if let statusName {
                        HStack(spacing: Tokens.statusSpacing) {
                            MonarchIcon(
                                systemName: status == .accepted ? Tokens.acceptSymbol : Tokens.rejectSymbol,
                                size: .sm
                            )
                            Text(statusName).monarchTextStyle(.caption, weight: Tokens.labelWeight)
                        }
                        .foregroundStyle(theme.colors[role: Tokens.statusRole(status)])
                    }
                }
                Group {
                    if isChanged {
                        Text(attributed(segments, status: status))
                    } else {
                        Text(hunk.after) + Text(" (\(unchanged))").foregroundStyle(theme.colors.textMuted)
                    }
                }
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
                .fixedSize(horizontal: false, vertical: true)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Tokens.hunkLabel(name: name, status: statusName, diff: spoken))
            if Tokens.showsHunkActions(
                state: part.state,
                status: status,
                hasHunkActions: onAcceptHunk != nil || onRejectHunk != nil
            ) {
                MonarchFlowLayout(spacing: Tokens.actionSpacing) {
                    if onAcceptHunk != nil {
                        button(
                            labels.accept ?? MonarchAiEditProposalStrings.accept,
                            systemImage: Tokens.acceptSymbol,
                            variant: .secondary,
                            hunkName: name
                        ) { decide(index, as: .accepted) }
                        .accessibilityFocused($focus, equals: .hunk(index))
                    }
                    if onRejectHunk != nil {
                        button(
                            labels.reject ?? MonarchAiEditProposalStrings.reject,
                            systemImage: Tokens.rejectSymbol,
                            variant: .ghost,
                            hunkName: name
                        ) { decide(index, as: .rejected) }
                    }
                }
            }
        }
        .padding(Tokens.hunkPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors[role: Tokens.hunkBackground]))
        .overlay(shape.strokeBorder(theme.colors[role: Tokens.hunkBorder], lineWidth: MonarchShapes.borderWidth))
    }

    private func button(
        _ title: String,
        systemImage: String,
        variant: MonarchButtonVariant,
        hunkName: String,
        action: @escaping @MainActor () -> Void
    ) -> some View {
        MonarchButton(title, systemImage: systemImage, variant: variant, size: .sm, action: action)
            .accessibilityLabel(MonarchAiEditProposalStrings.action(title, hunkName))
    }

    @ViewBuilder private func actions(pending: Int) -> some View {
        let showsAll = Tokens.showsAll(
            state: part.state,
            pending: pending,
            hasAllActions: onAcceptAll != nil || onRejectAll != nil
        )
        let showsRevert = part.state == .applied && onRevert != nil
        if showsAll || showsRevert {
            MonarchFlowLayout(spacing: Tokens.actionSpacing) {
                if showsAll, onAcceptAll != nil {
                    MonarchButton(
                        labels.acceptAll ?? MonarchAiEditProposalStrings.acceptAll,
                        systemImage: Tokens.acceptSymbol,
                        variant: .primary,
                        size: .sm
                    ) { decideAll(as: .accepted) }
                }
                if showsAll, onRejectAll != nil {
                    MonarchButton(labels.rejectAll ?? MonarchAiEditProposalStrings.rejectAll, size: .sm) {
                        decideAll(as: .rejected)
                    }
                }
                if showsRevert, let onRevert {
                    MonarchButton(
                        labels.revert ?? MonarchAiEditProposalStrings.revert,
                        systemImage: Tokens.revertSymbol,
                        size: .sm,
                        action: onRevert
                    )
                }
            }
        }
    }

    private func attributed(_ segments: [MonarchAiEditProposalSegment], status: MonarchAiEditHunkStatus)
        -> AttributedString
    {
        var result = AttributedString()
        for segment in segments {
            var piece = AttributedString(segment.text)
            let style = Tokens.style(segment.kind, status: status)
            if let background = style.background {
                piece[AttributeScopes.SwiftUIAttributes.BackgroundColorAttribute.self] = theme.colors[role: background]
            }
            if let foreground = style.foreground {
                piece[AttributeScopes.SwiftUIAttributes.ForegroundColorAttribute.self] = theme.colors[role: foreground]
            }
            if style.isStruck {
                piece[AttributeScopes.SwiftUIAttributes.StrikethroughStyleAttribute.self] = .single
            }
            if style.isUnderlined {
                piece[AttributeScopes.SwiftUIAttributes.UnderlineStyleAttribute.self] = .single
            }
            result += piece
        }
        return result
    }

    private func remember(_ update: ([String: MonarchAiEditHunkStatus]) -> [String: MonarchAiEditHunkStatus]) {
        decisions = update(decided)
        decisionsPartID = part.id
    }

    private func decide(_ index: Int, as status: MonarchAiEditHunkStatus) {
        guard hunks.indices.contains(index) else { return }
        let hunk = hunks[index]
        var after = statuses
        after[index] = status
        let left = after.count { $0 == .proposed }
        remember { current in
            var next = current
            next[hunk.id] = status
            return next
        }
        AccessibilityNotification.Announcement(
            Tokens.announcement(status == .accepted ? .accepted : .rejected, left: left, labels: labels)
        )
        .post()
        if status == .accepted { onAcceptHunk?(hunk.id) } else { onRejectHunk?(hunk.id) }
        focus = Tokens.nextFocus(after: index, statuses: after).map { .hunk($0) } ?? .title
    }

    private func decideAll(as status: MonarchAiEditHunkStatus) {
        let current = statuses
        remember { Tokens.decideAll(hunks, statuses: current, as: status, decided: $0) }
        AccessibilityNotification.Announcement(
            Tokens.announcement(status == .accepted ? .acceptedAll : .rejectedAll, left: 0, labels: labels)
        )
        .post()
        if status == .accepted { onAcceptAll?() } else { onRejectAll?() }
        focus = .title
    }
}

/// Where VoiceOver focus goes after a decision of ``MonarchAiEditProposal``.
enum MonarchAiEditProposalFocus: Hashable {
    /// The title, when no change is left.
    case title
    /// The accept button of a change.
    case hunk(Int)
}

#if DEBUG
    #Preview("MonarchAiEditProposal") {
        let hunks = [
            MonarchAiEditHunk(id: "1", before: "The quick brown fox", after: "The slow brown fox", label: "Title"),
            MonarchAiEditHunk(id: "2", before: "Ships on Friday.", after: "Ships on Monday after review."),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchAiEditProposal(
                    MonarchAiEditProposalPart(id: "p", target: "doc", diff: .null),
                    hunks: hunks,
                    onAcceptAll: {},
                    onRejectAll: {},
                    onAcceptHunk: { _ in },
                    onRejectHunk: { _ in }
                )
                MonarchAiEditProposal(
                    MonarchAiEditProposalPart(id: "q", target: "doc", diff: .null, state: .applied),
                    hunks: hunks,
                    onRevert: {}
                )
            }
        }
    }
#endif
