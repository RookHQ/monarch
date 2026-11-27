import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchPresenceAvatars``.
enum MonarchPresenceAvatarsTokens {
    /// The size of the avatars in the list.
    static let listAvatarSize: MonarchAvatarSize = .lg
    /// The gap between the avatar and the text in the list.
    static let listSpacing: CGFloat = MonarchTokens.Space.space3
    /// The colour of the second line in the list.
    static var detailColor: MonarchColorRole { \.textMuted }
    /// The colour of the name in the list.
    static var nameColor: MonarchColorRole { \.textDefault }

    /// How many avatars the stack shows before the "+N" tile.
    static func visibleCount(total: Int, maxVisible: Int) -> Int {
        MonarchOverflow.visibleCount(total: total, maxVisible: maxVisible)
    }

    /// Whether the stack is a button that opens the list: when people are
    /// hidden behind "+N", or when a person can be tapped.
    static func opensList(total: Int, maxVisible: Int, hasPersonAction: Bool) -> Bool {
        hasPersonAction || visibleCount(total: total, maxVisible: maxVisible) < total
    }

    /// The spoken presence of a person, with the custom status.
    static func presence(_ person: MonarchPresencePerson) -> String {
        MonarchPresenceDot.label(for: person.status, customStatus: person.customStatus)
    }

    /// The activity, or `nil` when it is empty.
    static func activity(_ person: MonarchPresencePerson) -> String? {
        guard let activity = person.activity?.trimmingCharacters(in: .whitespacesAndNewlines), !activity.isEmpty
        else { return nil }
        return activity
    }

    /// The spoken status of a person, read after the name: the presence and the activity.
    static func spokenStatus(_ person: MonarchPresencePerson, labels: MonarchPresenceAvatarsLabels) -> String {
        let presence = presence(person)
        let activity = activity(person)
        if let custom = labels.status { return custom(presence, activity) }
        guard let activity else { return presence }
        return MonarchPresenceAvatarsStrings.status(presence, activity)
    }

    /// The spoken text of a person: name, presence and activity. The same
    /// text ``MonarchAvatar`` reads with this status.
    static func personText(_ person: MonarchPresencePerson, labels: MonarchPresenceAvatarsLabels) -> String {
        MonarchAvatarTokens.accessibilityLabel(
            name: person.name,
            status: person.status,
            statusLabel: spokenStatus(person, labels: labels)
        )
    }

    /// The second line of a person in the list.
    static func detail(_ person: MonarchPresencePerson, labels: MonarchPresenceAvatarsLabels) -> String {
        let presence = presence(person)
        let activity = activity(person)
        if let custom = labels.detail { return custom(presence, activity) }
        guard let activity else { return presence }
        return MonarchPresenceAvatarsStrings.detail(presence, activity)
    }

    /// The spoken name of the stack.
    static func label(count: Int, labels: MonarchPresenceAvatarsLabels) -> String {
        labels.label?(count) ?? MonarchPresenceAvatarsStrings.label(count)
    }

    /// The spoken value of a stack that is one button: every visible person,
    /// then the number of the others.
    static func value(
        _ people: [MonarchPresencePerson],
        maxVisible: Int,
        labels: MonarchPresenceAvatarsLabels
    ) -> String {
        let visible = visibleCount(total: people.count, maxVisible: maxVisible)
        var parts = people.prefix(visible).map { personText($0, labels: labels) }
        if visible < people.count {
            parts.append(MonarchAvatarStrings.more(people.count - visible))
        }
        return parts.joined(separator: "; ")
    }

    /// The names Voice Control accepts for a person: the name and the
    /// initials shown when there is no picture.
    static func inputLabels(_ person: MonarchPresencePerson) -> [String] {
        let initials = MonarchAvatarTokens.initials(person.name)
        guard person.avatarURL == nil, MonarchAvatarTokens.hasReadableInitials(initials), initials != person.name
        else { return [person.name] }
        return [person.name, initials]
    }
}
