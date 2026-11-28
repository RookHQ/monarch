import Foundation
import MonarchTokens
import SwiftUI

/// The resolved strings of ``MonarchInstallVerify``: the app's replacements, else the built-in ones.
struct MonarchInstallVerifyText {
    let labels: MonarchInstallVerifyLabels

    private func pick(_ value: String?, _ key: String) -> String {
        value ?? MonarchInstallVerifyStrings.string(key)
    }

    var installHeading: String { pick(labels.installHeading, "installHeading") }
    var verifyHeading: String { pick(labels.verifyHeading, "verifyHeading") }
    var platforms: String { pick(labels.platforms, "platforms") }
    var shareInstructions: String { pick(labels.shareInstructions, "shareInstructions") }
    var emailDeveloper: String { pick(labels.emailDeveloper, "emailDeveloper") }
    var shareSubject: String { pick(labels.shareSubject, "shareSubject") }
    var targets: String { pick(labels.targets, "targets") }
    var notSeen: String { pick(labels.notSeen, "notSeen") }
    var openTest: String { pick(labels.openTest, "openTest") }
    var openTestHint: String { pick(labels.openTestHint, "openTestHint") }

    /// The status title.
    func title(_ status: MonarchInstallVerifyStatus, hosts: [String]) -> String {
        switch status {
        case .waiting:
            return pick(labels.waitingTitle, "waitingTitle")
        case .connected:
            if let custom = labels.connectedTitle { return custom(hosts) }
            return hosts.isEmpty
                ? MonarchInstallVerifyStrings.string("connected")
                : MonarchInstallVerifyStrings.string("connectedOn", MonarchInstallVerifyTokens.list(hosts))
        case .unavailable:
            return pick(labels.unavailableTitle, "unavailableTitle")
        }
    }

    /// The status text under the title.
    func body(_ status: MonarchInstallVerifyStatus, targets: [String]) -> String {
        switch status {
        case .waiting:
            if let custom = labels.waitingBody { return custom(targets) }
            return targets.isEmpty
                ? MonarchInstallVerifyStrings.string("waitingBodyNoTargets")
                : MonarchInstallVerifyStrings.string("waitingBody", MonarchInstallVerifyTokens.list(targets))
        case .connected:
            return pick(labels.connectedBody, "connectedBody")
        case .unavailable:
            return pick(labels.unavailableBody, "unavailableBody")
        }
    }

    /// "seen 5 minutes ago", or "not seen yet".
    func seen(_ lastSeen: Date?, now: Date, locale: Locale, calendar: Calendar) -> String {
        guard let lastSeen else { return notSeen }
        let relative = MonarchInstallVerifyTokens.relative(lastSeen, now: now, locale: locale, calendar: calendar)
        return labels.seen?(relative) ?? MonarchInstallVerifyStrings.string("seen", relative)
    }

    /// What VoiceOver reads for a site: its name, then whether and when it was seen.
    func targetValue(_ lastSeen: Date?, now: Date, locale: Locale, calendar: Calendar) -> String {
        guard lastSeen != nil else { return MonarchInstallVerifyStrings.string("notSeenState") }
        return seen(lastSeen, now: now, locale: locale, calendar: calendar)
    }
}

/// Component tokens and rules for ``MonarchInstallVerify``.
enum MonarchInstallVerifyTokens {
    /// The gap between the two halves.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space6
    /// The gap between the parts of one half.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the indicator and the status text, and inside a site row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the status title and its text.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The padding inside the status box.
    static let statusPadding: CGFloat = MonarchTokens.Space.space4
    /// The share button's symbol.
    static let shareSymbol = "square.and.arrow.up"
    /// The test button's symbol.
    static let testSymbol = "globe"
    /// The role of help text and times.
    static var muted: MonarchColorRole { \.textMuted }

    /// The tone of the status box: info while waiting, success once connected, neutral when the
    /// product cannot check.
    static func tone(_ status: MonarchInstallVerifyStatus) -> MonarchTone {
        switch status {
        case .waiting: .neutral
        case .connected: .success
        case .unavailable: .neutral
        }
    }

    /// The symbol beside the status, or `nil` while waiting, when a progress indicator shows.
    static func symbol(_ status: MonarchInstallVerifyStatus) -> String? {
        switch status {
        case .waiting: nil
        case .connected: "checkmark.circle.fill"
        case .unavailable: "info.circle.fill"
        }
    }

    /// The colours of the status box.
    static func surface(_ status: MonarchInstallVerifyStatus) -> MonarchSurfaceAppearance {
        MonarchTonePalette.surface(tone: tone(status), emphasis: .subtle)
    }

    /// The tone of a site's dot: success once seen, neutral before.
    static func targetTone(_ lastSeen: Date?) -> MonarchTone {
        lastSeen == nil ? .neutral : .success
    }

    /// The sites named in the connected title: `connectedOn` when given, else the sites seen.
    static func hosts(connectedOn: [String]?, targets: [MonarchInstallTarget]) -> [String] {
        connectedOn ?? targets.filter { $0.lastSeen != nil }.map(\.label)
    }

    /// Names joined for a sentence. Plain commas keep the result the same in every locale; an app
    /// that wants "a, b and c" passes its own `waitingBody` and `connectedTitle`.
    static func list(_ names: [String]) -> String {
        names.joined(separator: ", ")
    }

    /// A `mailto:` link that carries the handoff text as the body of a new message.
    static func mailURL(subject: String, body: String) -> URL? {
        var components = URLComponents()
        components.scheme = "mailto"
        components.path = ""
        components.queryItems = [
            URLQueryItem(name: "subject", value: subject), URLQueryItem(name: "body", value: body),
        ]
        return components.url
    }

    /// The test page, only when it is a web address: anything else (often built from what a person
    /// typed) is not offered.
    static func safeTestURL(_ url: URL?) -> URL? {
        guard let url, let scheme = url.scheme?.lowercased(), scheme == "https" || scheme == "http",
            url.host?.isEmpty == false
        else { return nil }
        return url
    }

    /// Whether `onConnected` runs now: on the first connected status this view shows.
    static func shouldReportConnected(_ status: MonarchInstallVerifyStatus, alreadyReported: Bool) -> Bool {
        status == .connected && !alreadyReported
    }

    /// "5 minutes ago" in the locale and calendar given.
    static func relative(_ date: Date, now: Date, locale: Locale, calendar: Calendar) -> String {
        return MonarchRelativeTime.string(date, now: now, locale: locale, calendar: calendar, allowsFuture: true)
    }
}
