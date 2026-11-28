import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiContextDivider``.
enum MonarchAiContextDividerTokens {
    /// Space above and below the divider, between messages.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the rule, the description and the controls.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the lines and the label.
    static let ruleSpacing: CGFloat = MonarchTokens.Space.space2
    /// The least length of a line beside the label.
    static let lineMinWidth: CGFloat = MonarchTokens.Space.space4
    /// The thickness of a line.
    static let lineWidth: CGFloat = MonarchTokens.Space.px
    /// The length of a dash and of a gap of the dashed line.
    static let dash: CGFloat = MonarchTokens.Space.space1
    /// Padding before and after the summary text.
    static let summaryHorizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the summary text.
    static let summaryVerticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The corner radius of the summary box.
    static let summaryCornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The weight of the label.
    static let labelWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)

    /// The colour role of the lines.
    static func lineRole(_ kind: MonarchAiContextDividerKind) -> MonarchColorRole {
        switch kind {
        case .day: \.borderSubtle
        case .contextReset: \.borderDefault
        case .summarized: \.aiBorder
        }
    }

    /// The colour role of the label.
    static func labelRole(_ kind: MonarchAiContextDividerKind) -> MonarchColorRole {
        kind == .summarized ? \.aiStrong : \.textMuted
    }

    /// Whether the lines are dashed. A reset is also told apart by its shape, not only its colour.
    static func isDashed(_ kind: MonarchAiContextDividerKind) -> Bool {
        kind == .contextReset
    }

    /// Whether the divider has controls. Only a summarized divider with a
    /// summary or a way to show the originals has.
    static func isInteractive(kind: MonarchAiContextDividerKind, hasSummary: Bool, hasExpand: Bool) -> Bool {
        kind == .summarized && (hasSummary || hasExpand)
    }

    /// The visible label.
    ///
    /// - Parameters:
    ///   - kind: What the divider marks.
    ///   - date: The day of a `day` divider.
    ///   - now: The reference time for "Today" and "Yesterday".
    ///   - calendar: The calendar, which carries the time zone.
    ///   - locale: The locale of the day label.
    ///   - override: Replaces the built-in label.
    /// - Returns: The label, or an empty string for a `day` divider without a date.
    static func label(
        kind: MonarchAiContextDividerKind,
        date: Date?,
        now: Date,
        calendar: Calendar,
        locale: Locale,
        override: String?
    ) -> String {
        if let override { return override }
        switch kind {
        case .day:
            guard let date else { return "" }
            return dayLabel(for: date, now: now, calendar: calendar, locale: locale)
        case .contextReset:
            return MonarchAiContextDividerStrings.contextReset
        case .summarized:
            return MonarchAiContextDividerStrings.summarized
        }
    }

    /// "Today", "Yesterday", a weekday and date for this year, or a full
    /// date for another year. The relative names come from the system, in
    /// the given locale.
    static func dayLabel(for date: Date, now: Date, calendar: Calendar, locale: Locale) -> String {
        let offset = calendar.dateComponents(
            [.day],
            from: calendar.startOfDay(for: now),
            to: calendar.startOfDay(for: date)
        ).day
        if let offset, offset == 0 || offset == -1 {
            let formatter = RelativeDateTimeFormatter()
            formatter.locale = locale
            formatter.calendar = calendar
            formatter.dateTimeStyle = .named
            formatter.formattingContext = .beginningOfSentence
            return formatter.localizedString(from: DateComponents(day: offset))
        }
        var style = Date.FormatStyle(locale: locale, calendar: calendar, timeZone: calendar.timeZone)
            .weekday(.wide)
            .month(.wide)
            .day()
        if !calendar.isDate(date, equalTo: now, toGranularity: .year) {
            style = style.year()
        }
        return date.formatted(style)
    }

    /// What VoiceOver reads for a divider without controls: the label, then the description.
    static func accessibilityLabel(label: String, detail: String?) -> String {
        guard let detail, !detail.isEmpty else { return label }
        return label.isEmpty ? detail : "\(label). \(detail)"
    }

    /// What VoiceOver reads for the summary: the prefix that says AI wrote it, then the summary.
    static func summaryAccessibilityLabel(prefix: String, summary: String) -> String {
        prefix.isEmpty ? summary : "\(prefix) \(summary)"
    }
}
