import Foundation
import MonarchTokens

/// Component tokens and text rules for ``MonarchUsageMeter``.
enum MonarchUsageMeterTokens {
    /// The gap between the header and the items, and between two items.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of an item's card.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The padding of an item's card.
    static let cardPadding: CGFloat = MonarchTokens.Space.space4
    /// The corner radius of an item's card.
    static let cardRadius: CGFloat = CGFloat(MonarchTokens.Radius.lg)
    /// The gap between lines of one item.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between an icon and its text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The height of the meter's track.
    static let trackHeight: CGFloat = MonarchTokens.Space.space1_5
    /// The track role.
    static var track: MonarchColorRole { \.bgMuted }
    /// The text drawn, redacted, in a placeholder row.
    static let placeholderLabel = "Placeholder"

    /// Placeholder items for the loading state: one for each real item, or three.
    /// At most eight.
    static func placeholderItems(count: Int) -> [MonarchUsageMeterItem] {
        let rows = count > 0 ? min(count, 8) : 3
        return (0..<rows).map { index in
            MonarchUsageMeterItem(placeholderLabel, id: "placeholder-\(index)", used: 40, limit: 100)
        }
    }

    /// The level for an amount against a limit. `warningAt` is a fraction of
    /// the limit. No limit, or a limit of zero or less, is unlimited.
    static func level(used: Double, limit: Double?, warningAt: Double) -> MonarchUsageMeterLevel {
        guard let limit, limit > 0 else { return .unlimited }
        if used > limit { return .over }
        if used >= limit * warningAt { return .warning }
        return .normal
    }

    /// The fill of the meter and the colour of the status line for a level.
    static func roles(for level: MonarchUsageMeterLevel) -> (fill: MonarchColorRole, status: MonarchColorRole) {
        switch level {
        case .normal, .unlimited: return (fill: \.textMuted, status: \.textMuted)
        case .warning: return (fill: \.warningStrong, status: \.warningStrong)
        case .over: return (fill: \.dangerSolid, status: \.dangerStrong)
        }
    }

    /// The SF Symbol before the status line, from the shared tone palette.
    static func symbol(for level: MonarchUsageMeterLevel) -> String? {
        switch level {
        case .normal, .unlimited: nil
        case .warning: MonarchTonePalette.palette(for: .warning).symbol
        case .over: MonarchTonePalette.palette(for: .danger).symbol
        }
    }

    /// A fraction limited to 0 through 1. Not-a-number counts as 0.
    static func clamped(_ fraction: Double) -> Double {
        guard !fraction.isNaN else { return 0 }
        return min(max(fraction, 0), 1)
    }

    /// A figure in a locale, with at most one fraction digit.
    static func number(_ value: Double, locale: Locale) -> String {
        value.formatted(.number.precision(.fractionLength(0...1)).locale(locale))
    }

    /// The share of the limit used, as a whole percentage in a locale.
    static func percent(used: Double, limit: Double, locale: Locale) -> String {
        (used / limit).formatted(.percent.precision(.fractionLength(0)).locale(locale))
    }

    /// The texts of one item.
    struct ItemText: Equatable {
        /// The figures: "8,200 of 10,000 GB", or "4 seats" without a limit.
        let figures: String
        /// The line under the meter: "82% used" or "Near the limit · 82% used". Empty without a limit.
        let status: String
        /// What VoiceOver reads as the value.
        let accessibilityValue: String
    }

    /// The texts of one item at a level. Pure, so it is tested without a view.
    static func text(
        for item: MonarchUsageMeterItem,
        level: MonarchUsageMeterLevel,
        locale: Locale,
        labels: MonarchUsageMeterLabels
    ) -> ItemText {
        let used = number(item.used, locale: locale)
        let unit = item.unit.map { " \($0)" } ?? ""
        guard let limit = item.limit, level != .unlimited else {
            let figures = used + unit
            return ItemText(figures: figures, status: "", accessibilityValue: figures)
        }
        let figures = labels.amount(used, number(limit, locale: locale)) + unit
        let share = labels.percent(percent(used: item.used, limit: limit, locale: locale))
        let levelText: String? =
            switch level {
            case .warning: labels.nearLimit
            case .over: labels.overLimit
            case .normal, .unlimited: nil
            }
        return ItemText(
            figures: figures,
            status: [levelText, share].compactMap { $0 }.joined(separator: " · "),
            accessibilityValue: [figures, levelText, share].compactMap { $0 }.joined(separator: ", ")
        )
    }

    /// The line under the title: the period and the reset date, or `nil`.
    static func subtitle(
        period: String?,
        resetsAt: Date?,
        locale: Locale,
        timeZone: TimeZone,
        labels: MonarchUsageMeterLabels
    ) -> String? {
        var style = Date.FormatStyle(date: .abbreviated, time: .omitted)
        style.locale = locale
        style.timeZone = timeZone
        let resets = resetsAt.map { labels.resets($0.formatted(style)) }
        let parts = [period, resets].compactMap { $0 }.filter { !$0.isEmpty }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    /// The items whose level rose to near or over the limit since `previous`
    /// (the levels by item id before the change), in display order. An item
    /// seen for the first time is not a change, so nothing is read when the
    /// meter first appears.
    static func crossings(
        previous: [String: MonarchUsageMeterLevel],
        items: [MonarchUsageMeterItem],
        levels: [MonarchUsageMeterLevel]
    ) -> (items: [MonarchUsageMeterItem], levels: [MonarchUsageMeterLevel]) {
        var crossedItems: [MonarchUsageMeterItem] = []
        var crossedLevels: [MonarchUsageMeterLevel] = []
        for (item, level) in zip(items, levels) {
            guard let before = previous[item.id], before != .unlimited else { continue }
            let rose = (level == .warning && before == .normal) || (level == .over && before != .over)
            if rose {
                crossedItems.append(item)
                crossedLevels.append(level)
            }
        }
        return (crossedItems, crossedLevels)
    }

    /// What VoiceOver announces when levels change: one sentence per item
    /// that is near or over its limit. Empty when there is none.
    static func announcement(
        items: [MonarchUsageMeterItem],
        levels: [MonarchUsageMeterLevel],
        labels: MonarchUsageMeterLabels
    ) -> String {
        zip(items, levels).compactMap { item, level in
            switch level {
            case .warning: labels.announcement(item.label, labels.nearLimit)
            case .over: labels.announcement(item.label, labels.overLimit)
            case .normal, .unlimited: nil
            }
        }
        .joined(separator: ". ")
    }
}
