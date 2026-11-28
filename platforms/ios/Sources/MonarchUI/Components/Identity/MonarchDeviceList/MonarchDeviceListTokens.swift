import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchDeviceList``.
enum MonarchDeviceListTokens {
    /// The gap between the header, the notices and the list.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title and the line under it.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between rows, around the divider.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the icon, the text and the actions of a row.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the name and the badges, and between actions.
    static let metaSpacing: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol of the empty state.
    static let emptySymbol = "laptopcomputer"
    /// The separator between the parts of the detail line.
    static let separator = " · "

    /// The SF Symbol of a kind of device. Generic shapes only: symbols that
    /// depict Apple products (`iphone`, `ipad`, …) are not allowed in the library.
    static func symbol(_ kind: MonarchDeviceKind) -> String {
        switch kind {
        case .desktop: "desktopcomputer"
        case .mobile: "smartphone"
        case .tablet: "rectangle.portrait"
        case .unknown: "questionmark.square.dashed"
        }
    }

    /// The colour role of a device's icon: danger when it is blocked. The
    /// "Blocked" badge says the same in words.
    static func iconRole(isBlocked: Bool) -> MonarchColorRole {
        isBlocked ? \.dangerStrong : \.textMuted
    }

    /// What the card shows.
    enum Phase: Equatable {
        /// The devices.
        case list
        /// The loading indicator.
        case loading
        /// The empty state.
        case empty
        /// The error state.
        case failed
    }

    /// The phase for a state and a list: a loaded empty list is the empty state.
    static func phase(_ state: MonarchDeviceListState, devices: [MonarchDevice]) -> Phase {
        switch state {
        case .loading: .loading
        case .failed: .failed
        case .ready: devices.isEmpty ? .empty : .list
        }
    }

    /// The name shown for a device: its own, or the unnamed text when it is blank.
    static func displayName(_ device: MonarchDevice, labels: MonarchDeviceListLabels) -> String {
        let trimmed = device.name.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? labels.unnamed ?? MonarchDeviceListStrings.unnamed : trimmed
    }

    /// The system, browser and location in one line, leaving out what is missing.
    static func detail(_ device: MonarchDevice) -> String? {
        let parts = [device.os, device.browser, device.location]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        return parts.isEmpty ? nil : parts.joined(separator: separator)
    }

    /// A time relative to `now`, such as "5 minutes ago" or "yesterday".
    static func relativeTime(_ date: Date, now: Date, locale: Locale) -> String {
        MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    /// The last-used and first-seen lines, such as "Last used 5 minutes ago".
    static func times(
        _ device: MonarchDevice,
        now: Date,
        locale: Locale,
        labels: MonarchDeviceListLabels
    ) -> [String] {
        var lines: [String] = []
        if let date = device.lastUsed {
            let prefix = labels.lastUsed ?? MonarchDeviceListStrings.lastUsed
            lines.append("\(prefix) \(relativeTime(date, now: now, locale: locale))")
        }
        if let date = device.firstSeen {
            let prefix = labels.firstSeen ?? MonarchDeviceListStrings.firstSeen
            lines.append("\(prefix) \(relativeTime(date, now: now, locale: locale))")
        }
        return lines
    }

    /// A badge on a device.
    struct Badge: Equatable, Identifiable {
        /// The text.
        let text: String
        /// The tone.
        let tone: MonarchTone
        var id: String { text }
    }

    /// The badges of a device, current first, with their tones.
    static func badges(_ device: MonarchDevice, labels: MonarchDeviceListLabels) -> [Badge] {
        var badges: [Badge] = []
        if device.isCurrent {
            badges.append(Badge(text: labels.current ?? MonarchDeviceListStrings.current, tone: .success))
        }
        if device.isBlocked {
            badges.append(Badge(text: labels.blocked ?? MonarchDeviceListStrings.blocked, tone: .danger))
        }
        return badges
    }

    /// What VoiceOver reads after the name of a device: the badges, the
    /// detail line and the times, leaving out what is missing.
    static func accessibilityValue(badges: [String], detail: String?, times: [String]) -> String {
        (badges + [detail].compactMap { $0 } + times)
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
    }

    /// Whether a device has a Remove button: there is a handler and it is not the device in use.
    static func canRemove(_ device: MonarchDevice, hasHandler: Bool) -> Bool {
        hasHandler && !device.isCurrent
    }
}
