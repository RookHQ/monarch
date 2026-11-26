import SwiftUI

/// Where VoiceOver focus goes after an item is removed from a list: the
/// item now at the removed position, else the one before it, else nowhere
/// (the component then focuses its container or empty state).
enum MonarchFocusAfterRemoval {
    /// The id to focus.
    ///
    /// - Parameters:
    ///   - removedIndex: The position the removed item had.
    ///   - remaining: The ids of the items left, in order.
    /// - Returns: The id to focus, or `nil` when nothing is left.
    static func target<ID>(removedIndex: Int, remaining: [ID]) -> ID? {
        guard !remaining.isEmpty else { return nil }
        return remaining[min(max(0, removedIndex), remaining.count - 1)]
    }

    /// The id to focus after the item with `removed` is taken out of `ids`,
    /// or `nil` when it is not in `ids` or nothing is left.
    static func target<ID: Equatable>(removing removed: ID, from ids: [ID]) -> ID? {
        guard let index = ids.firstIndex(of: removed) else { return nil }
        var remaining = ids
        remaining.remove(at: index)
        return target(removedIndex: index, remaining: remaining)
    }
}

/// Posts VoiceOver announcements, as a polite or an assertive live region
/// does on the web.
enum MonarchAnnouncer {
    /// How urgently an announcement is spoken.
    enum Politeness: Equatable, Sendable {
        /// Waits for current speech: a status change, a count, a saved state.
        case polite
        /// Interrupts: an error that blocks the person.
        case assertive
    }

    /// The attributed text of an announcement, with its speech priority.
    static func announcement(_ text: String, politeness: Politeness = .polite) -> AttributedString {
        var attributed = AttributedString(text)
        attributed.accessibilitySpeechAnnouncementPriority = politeness == .assertive ? .high : .default
        return attributed
    }

    /// Posts `text`. Empty text is not posted.
    @MainActor
    static func post(_ text: String, politeness: Politeness = .polite) {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        AccessibilityNotification.Announcement(announcement(text, politeness: politeness)).post()
    }
}

/// Joins the parts of a spoken label: empty and `nil` parts are left out,
/// and the rest are separated by ", " so VoiceOver pauses between them.
enum MonarchSpokenLabel {
    /// The parts joined, such as "Invoice 1042, Paid, Due in 3 days".
    static func join(_ parts: [String?], separator: String = ", ") -> String {
        parts
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .joined(separator: separator)
    }

    /// The parts joined, written as separate arguments.
    static func join(_ parts: String?...) -> String {
        join(parts)
    }
}
