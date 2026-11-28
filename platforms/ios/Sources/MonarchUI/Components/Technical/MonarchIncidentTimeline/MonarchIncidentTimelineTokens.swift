import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchIncidentTimeline``.
enum MonarchIncidentTimelineTokens {
    /// The gap between the marker column and the update.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the header and the body of an update.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the parts of the header.
    static let metaSpacing: CGFloat = MonarchTokens.Space.space2
    /// The side of the marker column before Dynamic Type.
    static let markerColumn: CGFloat = MonarchTokens.Space.space6
    /// The side of the dot marker.
    static let dotSide: CGFloat = MonarchTokens.Space.space3
    /// The width of the line that joins the markers.
    static let lineWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The colour of the line that joins the markers.
    static var lineRole: MonarchColorRole { \.borderDefault }
    /// The separator between parts of what VoiceOver reads.
    static let separator = ", "
    /// The separator between the header and the body in what VoiceOver reads.
    static let bodySeparator = ". "

    /// The space after each update.
    static func rowSpacing(_ density: MonarchIncidentTimelineDensity) -> CGFloat {
        switch density {
        case .regular: MonarchTokens.Space.space6
        case .compact: MonarchTokens.Space.space3
        }
    }

    /// The built-in tone of a standard stage.
    static func tone(_ stage: MonarchIncidentStage) -> MonarchTone {
        switch stage {
        case .investigating: .danger
        case .identified: .warning
        case .monitoring: .info
        case .resolved: .success
        }
    }

    /// The label and tone of a stage key: a product stage first, then a
    /// standard stage (ignoring case), else the key made readable
    /// ("partial_outage" is "Partial outage") with the neutral tone.
    static func resolve(
        _ stage: String,
        stages: [String: MonarchStatusDefinition],
        labels: MonarchIncidentTimelineLabels
    ) -> MonarchStatusDefinition {
        if let custom = stages[stage] { return custom }
        if let standard = MonarchIncidentStage(rawValue: stage.lowercased()) {
            return MonarchStatusDefinition(labels.stageLabel(standard), tone: tone(standard))
        }
        return MonarchStatusDefinition(readable(stage), tone: .neutral)
    }

    /// A key made readable: separators become spaces, the first letter is
    /// upper case and the rest lower case. Empty keys stay empty.
    static func readable(_ key: String) -> String {
        let words = key.replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: "-", with: " ")
            .split(separator: " ", omittingEmptySubsequences: true)
            .joined(separator: " ")
            .lowercased()
        guard let first = words.first else { return key }
        return first.uppercased() + words.dropFirst()
    }

    /// The colour of the marker of a tone: the solid tone, the strong
    /// border for neutral.
    static func markerRole(_ tone: MonarchTone) -> MonarchColorRole {
        tone == .neutral ? \.borderStrong : MonarchTonePalette.palette(for: tone).solid
    }

    /// The time of an update in a fixed time zone, with the zone name, so
    /// readers in different zones quote the same time: "Nov 14, 09:30 GMT".
    static func time(_ date: Date, timeZone: TimeZone, locale: Locale) -> String {
        // The web's `Intl` text: the short date, the 24-hour time and the short zone name, joined by a comma
        // ("Mar 2, 09:05 UTC"). Foundation's single template would join them with "at" and name UTC "GMT".
        let day = DateFormatter()
        day.locale = locale
        day.timeZone = timeZone
        day.setLocalizedDateFormatFromTemplate("MMMd")
        let clock = DateFormatter()
        clock.locale = locale
        clock.timeZone = timeZone
        clock.setLocalizedDateFormatFromTemplate("HHmm")
        let isUTC = timeZone.secondsFromGMT(for: date) == 0 && ["UTC", "GMT", "Etc/UTC"].contains(timeZone.identifier)
        let zone = isUTC ? "UTC" : (timeZone.abbreviation(for: date) ?? timeZone.identifier)
        return "\(day.string(from: date)), \(clock.string(from: date)) \(zone)"
    }

    /// What VoiceOver reads for an update: the stage, the time, the author
    /// and the Automated tag, then the body.
    static func accessibilityLabel(
        stage: String,
        time: String,
        author: String?,
        automated: String?,
        body: String
    ) -> String {
        let header = [stage, time, author ?? "", automated ?? ""]
            .filter { !$0.isEmpty }
            .joined(separator: separator)
        return body.isEmpty ? header : header + bodySeparator + body
    }

    /// The update to announce when the list grows: the last one, only when
    /// there are more updates than before.
    static func newUpdate(_ updates: [MonarchIncidentUpdate], previousCount: Int) -> MonarchIncidentUpdate? {
        guard updates.count > previousCount else { return nil }
        return updates.last
    }
}
