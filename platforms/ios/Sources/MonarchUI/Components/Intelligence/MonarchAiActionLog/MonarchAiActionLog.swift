import SwiftUI

/// An audit list of what AI agents did: who acted, marked as an AI agent so
/// it is never mistaken for a person, what they did and to what, when, and
/// Undo for actions that can be reversed.
///
/// ```swift
/// MonarchAiActionLog(entries, onUndo: { id in
///     try await agentActions.undo(id)
/// })
/// ```
///
/// Entries are shown in the order given, usually newest first. An entry
/// with `isUndoable` has Undo when `onUndo` is given; while it runs the
/// button is busy, then the entry reads "Undone". A thrown error shows
/// "Could not undo: …" under the entry. A long log shows `pageSize` entries
/// and a Show more button. Targets with an `http` or `https` URL are links.
///
/// Accessibility: each entry is one element read as "Research agent (AI
/// agent) added a summary to Quarterly report", with the time and
/// "Undone" as its value; a target link is a named action ("Open Quarterly
/// report"). The avatar is decorative. Undo buttons are named per entry
/// ("Undo: Research agent added a summary to Quarterly report"). A
/// successful and a failed undo are announced.
@_spi(Experimental) public struct MonarchAiActionLog: View {
    private let entries: [MonarchAiActionLogEntry]
    private let now: Date?
    private let pageSize: Int
    private let labels: MonarchAiActionLogLabels
    private let onUndo: (@MainActor (String) async throws -> Void)?

    @State private var undoStates: [String: MonarchAiActionLogTokens.UndoState] = [:]
    @State private var shown: Int

    private typealias Tokens = MonarchAiActionLogTokens

    /// An AI action log.
    ///
    /// - Parameters:
    ///   - entries: The actions, in the order to show them, usually newest first.
    ///   - now: The reference time for relative times. Without it they follow the clock, redrawn more often while a time is recent.
    ///   - pageSize: How many entries are shown at first, and how many Show more adds.
    ///   - labels: Replacements for the built-in strings.
    ///   - onUndo: Reverses an action, given its entry id. Shows Undo on entries with `isUndoable`. Throw to
    ///     show the failure.
    public init(
        _ entries: [MonarchAiActionLogEntry],
        now: Date? = nil,
        pageSize: Int = 200,
        labels: MonarchAiActionLogLabels = MonarchAiActionLogLabels(),
        onUndo: (@MainActor (String) async throws -> Void)? = nil
    ) {
        self.entries = entries
        self.now = now
        self.pageSize = pageSize
        self.labels = labels
        self.onUndo = onUndo
        _shown = State(initialValue: Tokens.step(pageSize))
    }

    /// The content of the view.
    public var body: some View {
        if entries.isEmpty {
            MonarchEmptyState(
                labels.empty ?? MonarchAiActionLogStrings.empty,
                systemImage: Tokens.emptySymbol,
                size: .sm
            )
        } else if let now {
            content(now: now)
        } else {
            TimelineView(MonarchRelativeTimeSchedule(dates: entries.map(\.date))) { context in
                content(now: context.date)
            }
        }
    }

    private func content(now: Date) -> some View {
        let step = Tokens.step(pageSize)
        let more = Tokens.moreCount(total: entries.count, shown: shown, step: step)
        return VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
                ForEach(Array(entries.prefix(shown).enumerated()), id: \.element.id) { index, entry in
                    if index > 0 {
                        MonarchDivider()
                    }
                    MonarchAiActionLogRow(
                        entry: entry,
                        state: undoStates[entry.id],
                        canUndo: Tokens.canUndo(entry, state: undoStates[entry.id], hasHandler: onUndo != nil),
                        now: now,
                        labels: labels,
                        onUndo: { undo(entry) }
                    )
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(labels.label ?? MonarchAiActionLogStrings.label)
            if more > 0 {
                MonarchButton(Tokens.showMore(more, labels: labels), variant: .secondary, size: .sm) {
                    shown += step
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func undo(_ entry: MonarchAiActionLogEntry) {
        guard let onUndo, undoStates[entry.id] != .pending else { return }
        undoStates[entry.id] = .pending
        let id = entry.id
        Task { @MainActor in
            do {
                try await onUndo(id)
                undoStates[id] = .done
                AccessibilityNotification.Announcement(Tokens.undoneAnnouncement(entry, labels: labels)).post()
            } catch {
                undoStates[id] = .failed
                AccessibilityNotification.Announcement(Tokens.undoFailed(entry, labels: labels)).post()
            }
        }
    }
}

/// One entry: the agent's avatar, who did what to what, when, and Undo.
private struct MonarchAiActionLogRow: View {
    let entry: MonarchAiActionLogEntry
    let state: MonarchAiActionLogTokens.UndoState?
    let canUndo: Bool
    let now: Date
    let labels: MonarchAiActionLogLabels
    let onUndo: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    private typealias Tokens = MonarchAiActionLogTokens

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .top, spacing: Tokens.columnSpacing) {
                summary
                undoButton
            }
            VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                summary
                undoButton
            }
        }
    }

    private var summary: some View {
        let isUndone = Tokens.isUndone(entry, state: state)
        let meta = Tokens.meta(entry, isUndone: isUndone, now: now, locale: locale, labels: labels)
        let failure = state == .failed ? Tokens.undoFailed(entry, labels: labels) : nil
        let url = Tokens.safeURL(entry.url)
        return HStack(alignment: .top, spacing: Tokens.columnSpacing) {
            MonarchAiAgentAvatar(entry.agent, size: .sm, isDecorative: true)
            VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                MonarchFlowLayout(spacing: Tokens.nameSpacing) {
                    Text(entry.agent.name)
                        .monarchTextStyle(.bodyStrong)
                        .foregroundStyle(theme.colors.textDefault)
                    MonarchAiTag(Tokens.actorKind(labels), style: .caption, isSmall: true)
                }
                sentence(url: url, isUndone: isUndone)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.textRole(isUndone: isUndone)])
                    .tint(theme.colors.accentStrong)
                Text(meta)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                if let failure {
                    HStack(alignment: .firstTextBaseline, spacing: Tokens.iconSpacing) {
                        MonarchIcon(systemName: Tokens.errorSymbol, size: .sm)
                        Text(failure).monarchTextStyle(.caption)
                    }
                    .foregroundStyle(theme.colors.dangerStrong)
                }
            }
            .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.accessibilityLabel(entry, labels: labels))
        .accessibilityValue(Tokens.accessibilityValue(meta: meta, failure: failure))
        .modifier(MonarchAiActionLogOpenAction(url: url, name: openName))
    }

    private var openName: String {
        let target = entry.target ?? ""
        return labels.openTarget?(target) ?? MonarchAiActionLogStrings.openTarget(target)
    }

    private func sentence(url: URL?, isUndone: Bool) -> Text {
        var text = AttributedString(entry.action)
        if isUndone { text.strikethroughStyle = .single }
        if let target = entry.target, !target.isEmpty {
            var part = AttributedString(target)
            if let url {
                part.link = url
                part.foregroundColor = theme.colors.textLink
                part.underlineStyle = .single
            }
            if isUndone { part.strikethroughStyle = .single }
            text += AttributedString(" ") + part
        }
        return Text(text)
    }

    @ViewBuilder private var undoButton: some View {
        if canUndo {
            MonarchButton(
                labels.undo ?? MonarchAiActionLogStrings.undo,
                variant: .ghost,
                size: .sm,
                isLoading: state == .pending
            ) {
                onUndo()
            }
            .accessibilityLabel(Tokens.undoLabel(entry, labels: labels))
        }
    }
}

/// Adds a named accessibility action that opens the target, when it has a link.
private struct MonarchAiActionLogOpenAction: ViewModifier {
    let url: URL?
    let name: String

    @Environment(\.openURL) private var openURL

    func body(content: Content) -> some View {
        if let url {
            content.accessibilityAction(named: Text(name)) { openURL(url) }
        } else {
            content
        }
    }
}

#if DEBUG
    #Preview("MonarchAiActionLog") {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        let agent = MonarchAiAgentRef(id: "research", name: "Research agent", role: "Finds sources")
        MonarchPreviewMatrix {
            VStack {
                MonarchAiActionLog(
                    [
                        MonarchAiActionLogEntry(
                            id: "1",
                            date: now.addingTimeInterval(-240),
                            agent: agent,
                            action: "added a summary to",
                            target: "Quarterly report",
                            url: URL(string: "https://example.com/report"),
                            isUndoable: true
                        ),
                        MonarchAiActionLogEntry(
                            id: "2",
                            date: now.addingTimeInterval(-7_200),
                            agent: agent,
                            action: "renamed",
                            target: "Roadmap",
                            isUndoable: true,
                            isUndone: true
                        ),
                    ],
                    now: now,
                    onUndo: { _ in }
                )
                MonarchAiActionLog([])
            }
        }
    }
#endif
