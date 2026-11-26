import MonarchTokens
import SwiftUI

/// Layout tokens and the pure logic of ``MonarchAssigneePicker``.
enum MonarchAssigneePickerTokens {
    /// The gap between the field and the Assign to me shortcut.
    static let shortcutSpacing: CGFloat = MonarchTokens.Space.space1
    /// The SF Symbol of the Assign to me shortcut.
    static let assignToMeSymbol = "person.crop.circle.badge.checkmark"

    /// The people with the current user first within their group; everyone else keeps the app's order.
    static func ordered(_ people: [MonarchAssigneePerson], currentUserID: String?) -> [MonarchAssigneePerson] {
        guard let currentUserID, let index = people.firstIndex(where: { $0.id == currentUserID }) else {
            return people
        }
        var rest = people
        let me = rest.remove(at: index)
        let at = rest.firstIndex { ($0.group ?? "") == (me.group ?? "") } ?? rest.endIndex
        rest.insert(me, at: at)
        return rest
    }

    /// The name shown for a person: "(you)" is added for the current user.
    static func displayName(
        _ person: MonarchAssigneePerson,
        currentUserID: String?,
        labels: MonarchAssigneePickerLabels
    ) -> String {
        guard person.id == currentUserID else { return person.name }
        return labels.you?(person.name) ?? MonarchAssigneePickerStrings.you(person.name)
    }

    /// The entity the picker shows for a person: the email under the name,
    /// searched with the name, and the person's avatar.
    static func entity(
        _ person: MonarchAssigneePerson,
        currentUserID: String?,
        labels: MonarchAssigneePickerLabels
    ) -> MonarchEntityPickerEntity {
        let name = displayName(person, currentUserID: currentUserID, labels: labels)
        let keywords = [person.name, person.email].compactMap { $0 }.joined(separator: " ")
        if let image = person.image {
            return MonarchEntityPickerEntity(
                id: person.id,
                name: name,
                description: person.email,
                hint: person.hint,
                image: image,
                keywords: keywords,
                isDisabled: person.isDisabled,
                group: person.group,
                status: person.status
            )
        }
        return MonarchEntityPickerEntity(
            id: person.id,
            name: name,
            description: person.email,
            hint: person.hint,
            url: person.url,
            keywords: keywords,
            isDisabled: person.isDisabled,
            group: person.group,
            status: person.status
        )
    }

    /// The entities for a list of people, current user first.
    static func entities(
        _ people: [MonarchAssigneePerson],
        currentUserID: String?,
        labels: MonarchAssigneePickerLabels
    ) -> [MonarchEntityPickerEntity] {
        ordered(people, currentUserID: currentUserID).map {
            entity($0, currentUserID: currentUserID, labels: labels)
        }
    }

    /// The picker's strings with the assignee search placeholder built in.
    static func pickerLabels(_ labels: MonarchAssigneePickerLabels) -> MonarchEntityPickerLabels {
        var picker = labels.picker
        picker.search = picker.search ?? labels.search ?? MonarchAssigneePickerStrings.search
        return picker
    }

    /// The text of the field with nobody chosen, and of the row that clears it.
    static func unassigned(_ labels: MonarchAssigneePickerLabels) -> String {
        labels.unassigned ?? MonarchAssigneePickerStrings.unassigned
    }

    /// Whether Assign to me is offered: there is a current user, the
    /// shortcut is on, and they are not assigned yet.
    static func showsAssignToMe(currentUserID: String?, selection: [String], isEnabled: Bool) -> Bool {
        guard isEnabled, let currentUserID else { return false }
        return !selection.contains(currentUserID)
    }

    /// The several-person selection after Assign to me: the current user is added at the end.
    static func assigningMe(_ selection: [String], currentUserID: String) -> [String] {
        selection.contains(currentUserID) ? selection : selection + [currentUserID]
    }
}
