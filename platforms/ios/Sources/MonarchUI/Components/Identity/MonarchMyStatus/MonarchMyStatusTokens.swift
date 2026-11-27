import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchMyStatus``.
enum MonarchMyStatusTokens {
    /// The gap between the avatar and the text.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the name and the status line.
    static let textSpacing: CGFloat = MonarchTokens.Space.px
    /// The horizontal padding inside the row.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// The vertical padding inside the row.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// The corner radius of the pressed background.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md

    /// The background of the row while pressed.
    static func background(isPressed: Bool) -> MonarchColorRole? {
        isPressed ? \.bgMuted : nil
    }

    /// The colour of the line under the name: muted for a status, subtle for the invitation.
    static func lineRole(isPlaceholder: Bool) -> MonarchColorRole {
        isPlaceholder ? \.textSubtle : \.textMuted
    }

    /// Whether a status has ended at `now`.
    static func isExpired(_ status: MonarchMyStatusCustomStatus, now: Date) -> Bool {
        guard let until = status.until else { return false }
        return until <= now
    }

    /// The status to show at `now`: one with a message or an emoji that has not ended.
    static func activeStatus(_ status: MonarchMyStatusCustomStatus?, now: Date) -> MonarchMyStatusCustomStatus? {
        guard let status, !isExpired(status, now: now) else { return nil }
        let text = status.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let emoji = status.emoji?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return text.isEmpty && emoji.isEmpty ? nil : status
    }

    /// Whether the presence takes the line under the name: with no status, when it is not online or offline.
    static func showsPresence(_ presence: MonarchPresenceStatus, active: MonarchMyStatusCustomStatus?) -> Bool {
        active == nil && presence != .online && presence != .offline
    }

    /// The name of a presence.
    static func presenceName(_ presence: MonarchPresenceStatus, labels: MonarchMyStatusLabels) -> String {
        labels.presence?(presence) ?? MonarchPresenceDot.label(for: presence)
    }

    /// The line under the name and whether it is the invitation to set a status.
    struct Line: Equatable {
        /// The text.
        let text: String
        /// Whether it is the invitation rather than a status.
        let isPlaceholder: Bool
    }

    /// The line under the name: the status (emoji and message), else the presence when it says
    /// something, else the invitation.
    static func line(
        presence: MonarchPresenceStatus,
        status: MonarchMyStatusCustomStatus?,
        now: Date,
        labels: MonarchMyStatusLabels
    ) -> Line {
        let active = activeStatus(status, now: now)
        if let active {
            let parts = [active.emoji, active.text]
                .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
                .filter { !$0.isEmpty }
            return Line(text: parts.joined(separator: " "), isPlaceholder: false)
        }
        if showsPresence(presence, active: nil) {
            return Line(text: presenceName(presence, labels: labels), isPlaceholder: false)
        }
        return Line(text: labels.setStatus ?? MonarchMyStatusStrings.setStatus, isPlaceholder: true)
    }

    /// What VoiceOver reads after the name: the presence, and the line when it is not the presence itself.
    static func accessibilityValue(
        presence: MonarchPresenceStatus,
        status: MonarchMyStatusCustomStatus?,
        now: Date,
        labels: MonarchMyStatusLabels
    ) -> String {
        let presenceName = presenceName(presence, labels: labels)
        let active = activeStatus(status, now: now)
        if showsPresence(presence, active: active) { return presenceName }
        let line = line(presence: presence, status: status, now: now, labels: labels)
        return MonarchMyStatusStrings.value(presence: presenceName, line: line.text)
    }

    /// The times the row must be drawn again from `start`: now, and the end of a status still to come,
    /// so an expired status disappears without anything else changing.
    static func redrawDates(from start: Date, until: Date?) -> [Date] {
        guard let until, until > start else { return [start] }
        return [start, until]
    }
}

/// Draws the row now and again when the status ends.
struct MonarchMyStatusSchedule: TimelineSchedule {
    /// The end of the status, if any.
    let until: Date?

    func entries(from startDate: Date, mode: TimelineScheduleMode) -> [Date] {
        MonarchMyStatusTokens.redrawDates(from: startDate, until: until)
    }
}
