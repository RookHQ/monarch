import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiArtifactVersions``.
enum MonarchAiArtifactVersionsTokens {
    /// The gap between the controls, and between the controls and the diff.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol of the version menu.
    static let menuSymbol = "chevron.down"
    /// The separator between the parts of a menu item.
    static let separator = " · "

    /// The versions oldest first, one per number.
    static func sorted(_ versions: [MonarchAiArtifactVersion]) -> [MonarchAiArtifactVersion] {
        var seen = Set<Int>()
        return versions.sorted { $0.version < $1.version }.filter { seen.insert($0.version).inserted }
    }

    /// The index of the version shown: the selected number, or the newest when it is not in the list.
    static func index(of selection: Int, in sorted: [MonarchAiArtifactVersion]) -> Int? {
        guard !sorted.isEmpty else { return nil }
        return sorted.firstIndex { $0.version == selection } ?? sorted.count - 1
    }

    /// The version before the one at `index`, if any.
    static func previous(at index: Int, in sorted: [MonarchAiArtifactVersion]) -> MonarchAiArtifactVersion? {
        index > 0 && index < sorted.count ? sorted[index - 1] : nil
    }

    /// The texts to compare: the previous and the current version's content, when both exist.
    static func comparison(at index: Int, in sorted: [MonarchAiArtifactVersion]) -> (
        before: MonarchAiArtifactVersion, after: MonarchAiArtifactVersion
    )? {
        guard let before = previous(at: index, in: sorted), before.content != nil, sorted[index].content != nil
        else { return nil }
        return (before, sorted[index])
    }

    /// The position text, "Version 3 of 5".
    static func position(_ version: Int, total: Int, labels: MonarchAiArtifactVersionsLabels) -> String {
        labels.position?(version, total) ?? MonarchAiArtifactVersionsStrings.position(version, of: total)
    }

    /// The announcement after a change, "Showing version 3 of 5".
    static func announcement(_ version: Int, total: Int, labels: MonarchAiArtifactVersionsLabels) -> String {
        labels.announce?(version, total) ?? MonarchAiArtifactVersionsStrings.announce(version, of: total)
    }

    /// The name of a version above the diff, "Version 3".
    static func versionName(_ version: Int, labels: MonarchAiArtifactVersionsLabels) -> String {
        labels.versionName?(version) ?? MonarchAiArtifactVersionsStrings.versionName(version)
    }

    /// A menu item: "v3 · Shorter intro · Mar 12, 2026 at 2:05 PM", leaving out a blank name.
    static func option(
        _ version: MonarchAiArtifactVersion,
        locale: Locale,
        calendar: Calendar,
        timeZone: TimeZone,
        labels: MonarchAiArtifactVersionsLabels
    ) -> String {
        let tag = labels.versionTag?(version.version) ?? MonarchAiArtifactVersionsStrings.versionTag(version.version)
        // The date and the time are formatted apart and joined with a comma ("12 Mar 2026, 10:05"), as the web
        // writes them; the system's own joiner would be "at".
        let day = Date.FormatStyle(
            date: .abbreviated,
            time: .omitted,
            locale: locale,
            calendar: calendar,
            timeZone: timeZone
        )
        let clock = Date.FormatStyle(locale: locale, calendar: calendar, timeZone: timeZone).monarchTime()
        let name = version.label?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return [tag, name, version.createdAt.formatted(day) + ", " + version.createdAt.formatted(clock)]
            .filter { !$0.isEmpty }
            .joined(separator: separator)
    }
}
