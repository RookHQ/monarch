import SwiftUI

/// What changed between two versions of a timesheet: added, removed and
/// changed entries, each named in words and with its effect on the total.
/// For resubmissions, corrections and approval review.
///
/// ```swift
/// MonarchTimesheetDiff(before: submitted.entries, after: draft.entries, subtitle: "since version 3")
/// ```
///
/// Entries are matched by identifier. An added entry shows its line, a
/// removed one its line struck through, a changed one the fields that
/// changed and the old line struck through above the new one. The totals
/// line says how much total and billable time the changes add or remove.
///
/// Accessibility: the title is a heading. Each change is one element read
/// as its kind, the change of time in words and the entry ("Removed, minus
/// 45 minutes, Tue, Jan 13, 45 minutes, Atlas"); a changed entry is read
/// with "Before" and "After". The kind is a word as well as a
/// colour, and a removed line is struck through.
public struct MonarchTimesheetDiff: View {
    private let diff: MonarchTimesheetDiffResult
    private let title: String?
    private let subtitle: String?
    private let showsTotals: Bool
    private let showsBillable: Bool
    private let labels: MonarchTimesheetDiffLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone

    /// A timesheet diff the product computed.
    ///
    /// - Parameters:
    ///   - diff: The entries added, removed and changed.
    ///   - title: The heading. Built in: "What changed".
    ///   - subtitle: Quiet text after the heading, such as "since version 3".
    ///   - showsTotals: Whether the totals line is shown.
    ///   - showsBillable: Whether the totals line includes the billable
    ///     change. Turn it off when billing does not apply.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ diff: MonarchTimesheetDiffResult,
        title: String? = nil,
        subtitle: String? = nil,
        showsTotals: Bool = true,
        showsBillable: Bool = true,
        labels: MonarchTimesheetDiffLabels = MonarchTimesheetDiffLabels()
    ) {
        self.diff = diff
        self.title = title
        self.subtitle = subtitle
        self.showsTotals = showsTotals
        self.showsBillable = showsBillable
        self.labels = labels
    }

    /// The diff between two versions of a timesheet.
    ///
    /// - Parameters:
    ///   - before: The entries of the old version.
    ///   - after: The entries of the new version.
    ///   - title: The heading. Built in: "What changed".
    ///   - subtitle: Quiet text after the heading, such as "since version 3".
    ///   - showsTotals: Whether the totals line is shown.
    ///   - showsBillable: Whether the totals line includes the billable
    ///     change.
    ///   - labels: Replacements for the built-in strings.
    public init(
        before: [MonarchTimesheetDiffEntry],
        after: [MonarchTimesheetDiffEntry],
        title: String? = nil,
        subtitle: String? = nil,
        showsTotals: Bool = true,
        showsBillable: Bool = true,
        labels: MonarchTimesheetDiffLabels = MonarchTimesheetDiffLabels()
    ) {
        self.init(
            MonarchTimesheetDiffResult(before: before, after: after),
            title: title,
            subtitle: subtitle,
            showsTotals: showsTotals,
            showsBillable: showsBillable,
            labels: labels
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchTimesheetDiffTokens.spacing) {
            heading
            if diff.isEmpty {
                Text(MonarchTimesheetDiffStrings.noChanges(labels))
                    .monarchTextStyle(.label, weight: .regular)
                    .foregroundStyle(theme.colors.textMuted)
            } else {
                VStack(alignment: .leading, spacing: MonarchTimesheetDiffTokens.lineSpacing) {
                    ForEach(diff.added) { entry in
                        entryLine(entry, kind: .added)
                    }
                    ForEach(diff.removed) { entry in
                        entryLine(entry, kind: .removed)
                    }
                    ForEach(diff.changed) { change in
                        changeLine(change)
                    }
                }
            }
            if showsTotals {
                Text(MonarchTimesheetDiffTokens.totalsText(diff.totals, showsBillable: showsBillable, labels: labels))
                    .monarchTextStyle(.caption)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var heading: some View {
        var text = Text(title ?? MonarchTimesheetDiffStrings.title(labels)).fontWeight(.semibold)
        if let subtitle, !subtitle.isEmpty {
            text = text + Text(verbatim: " ") + Text(subtitle).foregroundStyle(theme.colors.textMuted)
        }
        return
            text
            .monarchTextStyle(.label)
            .accessibilityAddTraits(.isHeader)
    }

    private func entryLine(_ entry: MonarchTimesheetDiffEntry, kind: MonarchTimesheetDiffKind) -> some View {
        let minutes = kind == .removed ? -entry.minutes : entry.minutes
        return line(kind: kind, kindText: MonarchTimesheetDiffTokens.kindText(kind, labels: labels), delta: minutes) {
            Text(MonarchTimesheetDiffTokens.entryText(entry, labels: labels, locale: locale, timeZone: timeZone))
                .monarchTextStyle(.label, weight: .regular)
                .strikethrough(kind == .removed)
                .foregroundStyle(kind == .removed ? theme.colors.textMuted : theme.colors.textDefault)
        }
        .accessibilityLabel(
            MonarchTimesheetDiffTokens.entryAccessibilityLabel(
                entry,
                kind: kind,
                labels: labels,
                locale: locale,
                timeZone: timeZone
            )
        )
    }

    private func changeLine(_ change: MonarchTimesheetDiffChange) -> some View {
        let delta = change.after.minutes - change.before.minutes
        return line(
            kind: .changed,
            kindText: MonarchTimesheetDiffTokens.kindText(.changed, fields: change.fields, labels: labels),
            delta: delta == 0 ? nil : delta
        ) {
            Text(
                MonarchTimesheetDiffTokens.entryText(change.before, labels: labels, locale: locale, timeZone: timeZone)
            )
            .monarchTextStyle(.caption)
            .strikethrough()
            .foregroundStyle(theme.colors.textMuted)
            Text(MonarchTimesheetDiffTokens.entryText(change.after, labels: labels, locale: locale, timeZone: timeZone))
                .monarchTextStyle(.caption)
        }
        .accessibilityLabel(
            MonarchTimesheetDiffTokens.changeAccessibilityLabel(
                change,
                labels: labels,
                locale: locale,
                timeZone: timeZone
            )
        )
    }

    private func line(
        kind: MonarchTimesheetDiffKind,
        kindText: String,
        delta: Int?,
        @ViewBuilder content: () -> some View
    ) -> some View {
        let appearance = MonarchTimesheetDiffTokens.appearance(kind)
        return VStack(alignment: .leading, spacing: MonarchTimesheetDiffTokens.partSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchTimesheetDiffTokens.headSpacing) {
                Text(kindText)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors[role: appearance.kind])
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let delta {
                    Text(MonarchTimesheetDiffTokens.deltaText(delta, labels: labels))
                        .monarchTextStyle(.caption)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors.textMuted)
                        .fixedSize()
                }
            }
            content()
        }
        .padding(.vertical, MonarchTimesheetDiffTokens.verticalPadding)
        .padding(.horizontal, MonarchTimesheetDiffTokens.horizontalPadding)
        .padding(.leading, MonarchTimesheetDiffTokens.barWidth)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.colors[role: appearance.background])
        .overlay(alignment: .leading) {
            Rectangle()
                .fill(theme.colors[role: appearance.bar])
                .frame(width: MonarchTimesheetDiffTokens.barWidth)
        }
        .clipShape(MonarchTimesheetDiffTokens.shape)
        .accessibilityElement(children: .ignore)
    }
}

#if DEBUG
    #Preview("MonarchTimesheetDiff") {
        MonarchPreviewMatrix {
            let day = Date(timeIntervalSinceReferenceDate: 800_000_000)
            let review = MonarchTimesheetDiffEntry(id: "1", date: day, minutes: 120, project: "Atlas", task: "Review")
            VStack(alignment: .leading) {
                MonarchTimesheetDiff(
                    before: [
                        review,
                        MonarchTimesheetDiffEntry(id: "2", date: day, minutes: 45, project: "Pulse", isBillable: false),
                    ],
                    after: [
                        MonarchTimesheetDiffEntry(id: "1", date: day, minutes: 150, project: "Atlas", task: "Review"),
                        MonarchTimesheetDiffEntry(id: "3", date: day, minutes: 60),
                    ],
                    subtitle: "since version 3"
                )
                MonarchTimesheetDiff(before: [review], after: [review])
            }
        }
    }
#endif
