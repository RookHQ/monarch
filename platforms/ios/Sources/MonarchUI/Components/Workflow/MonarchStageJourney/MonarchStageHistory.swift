import SwiftUI

/// Every stage move of an item, as a vertical timeline: from, to, when,
/// who and why.
///
/// ```swift
/// MonarchStageHistory(application.moves.map {
///     MonarchStageHistoryEntry(id: $0.id, from: $0.from, to: $0.to, at: $0.date, actor: $0.author)
/// })
/// ```
///
/// Pass the moves newest first or oldest first, as the product prefers.
/// Times are relative ("2 days ago") and follow the clock once a minute
/// unless `now` is fixed.
///
/// Accessibility: each entry is one element, read as the move in words
/// ("Filed to Examination"), "moved back" when it went back, the time, the
/// actor, the facts and the note. The dots and the line are decorative.
public struct MonarchStageHistory: View {
    private let entries: [MonarchStageHistoryEntry]
    private let now: Date?
    private let emptyMessage: String?
    private let labels: MonarchStageHistoryLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A stage history.
    ///
    /// - Parameters:
    ///   - entries: The moves, in the order to show them.
    ///   - now: A fixed reference time for the relative times.
    ///   - emptyMessage: Shown when there are no moves. Built in: "No stage
    ///     moves yet.".
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ entries: [MonarchStageHistoryEntry],
        now: Date? = nil,
        emptyMessage: String? = nil,
        labels: MonarchStageHistoryLabels = MonarchStageHistoryLabels()
    ) {
        self.entries = entries
        self.now = now
        self.emptyMessage = emptyMessage
        self.labels = labels
    }

    /// The content of the view.
    public var body: some View {
        if entries.isEmpty {
            Text(emptyMessage ?? MonarchStageHistoryStrings.empty(labels))
                .monarchTextStyle(.label, weight: .regular)
                .foregroundStyle(theme.colors.textMuted)
        } else if let now {
            timeline(now: now)
        } else {
            MonarchRelativeTimeClock(dates: entries.map(\.at)) { current in
                timeline(now: current)
            }
        }
    }

    private func timeline(now: Date) -> some View {
        VStack(alignment: .leading, spacing: MonarchStageJourneyTokens.historySpacing) {
            ForEach(entries) { entry in
                row(entry, now: now)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(alignment: .leading) {
            Rectangle()
                .fill(theme.colors.borderDefault)
                .frame(width: MonarchShapes.borderWidth)
                .padding(.leading, MonarchStageJourneyTokens.historyLineInset)
                .accessibilityHidden(true)
        }
        .accessibilityElement(children: .contain)
    }

    private func row(_ entry: MonarchStageHistoryEntry, now: Date) -> some View {
        let time = MonarchStageJourneyTokens.relativeText(entry.at, now: now, locale: locale)
        return HStack(alignment: .firstTextBaseline, spacing: 0) {
            Text(Image(systemName: "circle.fill"))
                .font(.system(size: MonarchStageJourneyTokens.historyDotSize))
                .foregroundStyle(theme.colors[role: MonarchStageJourneyTokens.historyDot(isBackward: entry.isBackward)])
                .frame(width: MonarchStageJourneyTokens.historyDotColumn, alignment: .leading)
            VStack(alignment: .leading, spacing: MonarchStageJourneyTokens.historyLineSpacing) {
                move(entry)
                    .monarchTextStyle(.label, weight: .regular)
                Text(MonarchStageJourneyTokens.historyMetaText(entry, time: time))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                if let note = entry.note, !note.isEmpty {
                    Text(note)
                        .monarchTextStyle(.label, weight: .regular)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchStageJourneyTokens.historyAccessibilityLabel(entry, time: time, labels: labels))
    }

    private func move(_ entry: MonarchStageHistoryEntry) -> Text {
        var text: Text
        if let from = entry.from {
            text =
                Text(verbatim: from + " ") + Text(Image(systemName: "arrow.forward")) + Text(verbatim: " ")
                + Text(verbatim: entry.to).fontWeight(.medium)
        } else {
            text = Text(MonarchStageHistoryStrings.started(entry.to, labels))
        }
        if entry.isBackward {
            text =
                text + Text(verbatim: " ")
                + Text(MonarchStageHistoryStrings.movedBack(labels)).foregroundStyle(theme.colors.textMuted)
        }
        return text
    }
}
