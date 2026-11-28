import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchCallStatsPanel``.
enum MonarchCallStatsPanelTokens {
    /// The gap between the usage, the people and the footnote.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between lines of a section.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between two people.
    static let peerSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between a name and its grade, and between the numbers.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space2
    /// Shown for a number that has not been measured.
    static let noValueSymbol = "–"
    /// The separator between parts of what VoiceOver reads.
    static let separator = ", "

    /// The tone of a grade.
    static func tone(_ quality: MonarchCallStatsPanelQuality) -> MonarchTone {
        switch quality {
        case .good: .success
        case .fair: .warning
        case .poor: .danger
        case .unknown: .neutral
        }
    }

    /// The SF Symbol of a grade, so the grade is not colour alone.
    static func symbol(_ quality: MonarchCallStatsPanelQuality) -> String {
        switch quality {
        case .good: "wifi"
        case .fair: "wifi.exclamationmark"
        case .poor: "wifi.slash"
        case .unknown: "questionmark.circle"
        }
    }

    /// The name of a grade, from the labels or built in.
    static func name(_ quality: MonarchCallStatsPanelQuality, labels: MonarchCallStatsPanelLabels) -> String {
        labels.qualityNames[quality] ?? MonarchCallStatsPanelStrings.quality(quality)
    }

    /// Bytes in the units people know, powers of 1024: "512 bytes", "1.4 MB".
    static func bytes(_ count: Int64, locale: Locale) -> String {
        let bytes = max(0, count)
        // From 10 MB to 1 GB the figure is whole megabytes ("15 MB"), as the web; elsewhere the system's 3 digits.
        let mebibyte: Int64 = 1_048_576
        if bytes >= 10 * mebibyte, bytes < 1024 * mebibyte {
            let megabytes = (Double(bytes) / Double(mebibyte)).rounded()
            return Int(megabytes).formatted(.number.locale(locale)) + " MB"
        }
        return bytes.formatted(.byteCount(style: .memory).locale(locale))
    }

    /// Bits per second as bps, kbps or Mbps (powers of 1000).
    static func rate(_ bitsPerSecond: Double?, locale: Locale) -> String {
        let value = max(0, bitsPerSecond.flatMap { $0.isFinite ? $0 : nil } ?? 0)
        if value >= 1_000_000 {
            let megabits = value / 1_000_000
            let digits = megabits >= 10 ? 0 : 1
            return MonarchCallStatsPanelStrings.megabitsPerSecond(
                megabits.formatted(
                    .number.precision(.fractionLength(digits)).rounded(rule: .toNearestOrAwayFromZero).locale(locale)
                )
            )
        }
        if value >= 1_000 {
            return MonarchCallStatsPanelStrings.kilobitsPerSecond(
                (value / 1_000).formatted(.number.precision(.fractionLength(0)).locale(locale))
            )
        }
        return MonarchCallStatsPanelStrings.bitsPerSecond(
            value.formatted(.number.precision(.fractionLength(0)).locale(locale))
        )
    }

    /// The usage line: the total, then the current rates.
    static func usageText(
        _ usage: MonarchCallStatsPanelUsage,
        labels: MonarchCallStatsPanelLabels,
        locale: Locale
    ) -> String {
        let total = bytes(usage.sentBytes.addingReportingOverflow(usage.receivedBytes).partialValue, locale: locale)
        let up = rate(usage.upBitsPerSecond, locale: locale)
        let down = rate(usage.downBitsPerSecond, locale: locale)
        return labels.usageSummary?(total, up, down)
            ?? MonarchCallStatsPanelStrings.usageSummary(total: total, up: up, down: down)
    }

    /// The round-trip time in whole milliseconds, or `nil` when not measured.
    static func roundTrip(_ milliseconds: Double?, locale: Locale) -> String? {
        guard let milliseconds, milliseconds.isFinite else { return nil }
        return Measurement(value: milliseconds.rounded(), unit: UnitDuration.milliseconds)
            .formatted(
                .measurement(
                    width: .abbreviated,
                    usage: .asProvided,
                    numberFormatStyle: .number.precision(.fractionLength(0))
                )
                .locale(locale)
            )
    }

    /// The worse of the two loss directions, because either one hurts the
    /// call, or `nil` when neither was measured.
    static func loss(_ peer: MonarchCallStatsPanelPeer) -> Double? {
        [peer.inboundLossPercent, peer.outboundLossPercent].compactMap { $0 }.filter(\.isFinite).max()
    }

    /// A loss percentage with one decimal, or `nil` when not measured.
    static func lossText(_ percent: Double?, locale: Locale) -> String? {
        guard let percent else { return nil }
        return (max(0, percent) / 100).formatted(.percent.precision(.fractionLength(1)).locale(locale))
    }

    /// What VoiceOver reads for one person: the name, the round trip, the
    /// loss and the grade. A number not measured is read as such.
    static func accessibilityLabel(
        _ peer: MonarchCallStatsPanelPeer,
        labels: MonarchCallStatsPanelLabels,
        locale: Locale
    ) -> String {
        let missing = name(.unknown, labels: labels)
        return [
            peer.name,
            "\(labels.roundTrip ?? MonarchCallStatsPanelStrings.roundTrip) \(roundTrip(peer.roundTripMilliseconds, locale: locale) ?? missing)",
            "\(labels.loss ?? MonarchCallStatsPanelStrings.loss) \(lossText(loss(peer), locale: locale) ?? missing)",
            name(peer.quality, labels: labels),
        ]
        .joined(separator: separator)
    }
}
