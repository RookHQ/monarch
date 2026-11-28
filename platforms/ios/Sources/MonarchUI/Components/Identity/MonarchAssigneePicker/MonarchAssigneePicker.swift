import SwiftUI

/// A field for assigning one person, or several, with the current user
/// first, an "Unassigned" row and an "Assign to me" shortcut.
///
/// ```swift
/// MonarchAssigneePicker(
///     "Assignee",
///     selection: $assigneeID,
///     people: members,
///     currentUserID: me.id
/// )
/// ```
///
/// A preset of ``MonarchEntityPicker`` for people: tapping the field opens
/// a sheet with a searchable `List` of people with avatars, emails and
/// hints. The signed-in person is listed first as "Ana Silva (you)". With
/// one assignee the first row is "Unassigned", which clears it. Under the
/// field, "Assign to me" assigns the signed-in person in one tap; it is
/// hidden once they are assigned. With `search`, people are looked up on
/// the app's server as the text is typed.
///
/// Accessibility: the field is the entity picker's button, labelled with
/// `label` and valued with every assigned name ("Unassigned" while nobody
/// is). Avatars are decorative. In the sheet each person is a button read
/// with the name and email; assigned people have the selected trait and a
/// check mark. "Assign to me" is a separate button of at least 44 points.
public struct MonarchAssigneePicker: View {
    private enum Selection {
        case single(Binding<String?>)
        case multiple(Binding<[String]>)
    }

    private let label: String
    private let selection: Selection
    private let people: [MonarchAssigneePerson]
    private let selectedPeople: [MonarchAssigneePerson]
    private let currentUserID: String?
    private let helper: String?
    private let error: String?
    private let isRequired: Bool
    private let isLoading: Bool
    private let allowsUnassigned: Bool
    private let showsAssignToMe: Bool
    private let minSearchLength: Int
    private let debounce: Duration
    private let labels: MonarchAssigneePickerLabels
    private let search: (@Sendable (String) async throws -> [MonarchAssigneePerson])?

    @Environment(\.isEnabled) private var isEnabled

    private typealias Tokens = MonarchAssigneePickerTokens

    /// A picker for one assignee.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised, such as "Assignee".
    ///   - selection: The assigned person's id, or `nil` for nobody.
    ///   - people: The people to choose from, such as the workspace's members. With `search`, shown until the first search runs.
    ///   - selectedPeople: People for assigned ids that may not be in `people`, for their names and avatars.
    ///   - currentUserID: The signed-in person's id: listed first as "(you)", with the Assign to me shortcut.
    ///   - helper: Help text under the field.
    ///   - error: An error message, shown with an icon and announced.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - isLoading: Shows "Loading…" while `people` is empty.
    ///   - allowsUnassigned: Adds the Unassigned row that clears the assignee.
    ///   - showsAssignToMe: Shows Assign to me under the field while the signed-in person is not assigned.
    ///   - minSearchLength: The shortest text `search` runs for.
    ///   - debounce: How long to wait after typing before `search` runs.
    ///   - labels: Replacements for the built-in strings.
    ///   - search: Looks up the people who match the typed text on the app's server. Throw to show an error with Try again.
    public init(
        _ label: String,
        selection: Binding<String?>,
        people: [MonarchAssigneePerson],
        selectedPeople: [MonarchAssigneePerson] = [],
        currentUserID: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        isRequired: Bool = false,
        isLoading: Bool = false,
        allowsUnassigned: Bool = true,
        showsAssignToMe: Bool = true,
        minSearchLength: Int = 0,
        debounce: Duration = .milliseconds(250),
        labels: MonarchAssigneePickerLabels = MonarchAssigneePickerLabels(),
        search: (@Sendable (String) async throws -> [MonarchAssigneePerson])? = nil
    ) {
        self.label = label
        self.selection = .single(selection)
        self.people = people
        self.selectedPeople = selectedPeople
        self.currentUserID = currentUserID
        self.helper = helper
        self.error = error
        self.isRequired = isRequired
        self.isLoading = isLoading
        self.allowsUnassigned = allowsUnassigned
        self.showsAssignToMe = showsAssignToMe
        self.minSearchLength = minSearchLength
        self.debounce = debounce
        self.labels = labels
        self.search = search
    }

    /// A picker for several assignees, kept in the order they were assigned.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised, such as "Assignees".
    ///   - selection: The assigned people's ids.
    ///   - people: The people to choose from. With `search`, shown until the first search runs.
    ///   - selectedPeople: People for assigned ids that may not be in `people`, for their names and avatars.
    ///   - currentUserID: The signed-in person's id: listed first as "(you)", with the Assign to me shortcut.
    ///   - helper: Help text under the field.
    ///   - error: An error message, shown with an icon and announced.
    ///   - isRequired: Shows a mark after the label and adds "Required" to the hint.
    ///   - isLoading: Shows "Loading…" while `people` is empty.
    ///   - showsAssignToMe: Shows Assign to me under the field while the signed-in person is not assigned.
    ///   - minSearchLength: The shortest text `search` runs for.
    ///   - debounce: How long to wait after typing before `search` runs.
    ///   - labels: Replacements for the built-in strings.
    ///   - search: Looks up the people who match the typed text on the app's server. Throw to show an error with Try again.
    public init(
        _ label: String,
        selection: Binding<[String]>,
        people: [MonarchAssigneePerson],
        selectedPeople: [MonarchAssigneePerson] = [],
        currentUserID: String? = nil,
        helper: String? = nil,
        error: String? = nil,
        isRequired: Bool = false,
        isLoading: Bool = false,
        showsAssignToMe: Bool = true,
        minSearchLength: Int = 0,
        debounce: Duration = .milliseconds(250),
        labels: MonarchAssigneePickerLabels = MonarchAssigneePickerLabels(),
        search: (@Sendable (String) async throws -> [MonarchAssigneePerson])? = nil
    ) {
        self.label = label
        self.selection = .multiple(selection)
        self.people = people
        self.selectedPeople = selectedPeople
        self.currentUserID = currentUserID
        self.helper = helper
        self.error = error
        self.isRequired = isRequired
        self.isLoading = isLoading
        self.allowsUnassigned = false
        self.showsAssignToMe = showsAssignToMe
        self.minSearchLength = minSearchLength
        self.debounce = debounce
        self.labels = labels
        self.search = search
    }

    private var selectedIDs: [String] {
        switch selection {
        case .single(let binding): binding.wrappedValue.map { [$0] } ?? []
        case .multiple(let binding): binding.wrappedValue
        }
    }

    private var entitySearch: (@Sendable (String) async throws -> [MonarchEntityPickerEntity])? {
        guard let search else { return nil }
        let currentUserID = currentUserID
        let labels = labels
        return { query in
            let people = try await search(query)
            return Tokens.entities(people, currentUserID: currentUserID, labels: labels)
        }
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: Tokens.shortcutSpacing) {
            picker
            if showsAssignToMe, let currentUserID,
                Tokens.showsAssignToMe(currentUserID: currentUserID, selection: selectedIDs, isEnabled: isEnabled)
            {
                MonarchButton(
                    labels.assignToMe ?? MonarchAssigneePickerStrings.assignToMe,
                    systemImage: Tokens.assignToMeSymbol,
                    variant: .ghost,
                    size: .sm
                ) {
                    assign(currentUserID)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder private var picker: some View {
        let entities = Tokens.entities(people, currentUserID: currentUserID, labels: labels)
        let selected = Tokens.entities(selectedPeople, currentUserID: currentUserID, labels: labels)
        let pickerLabels = Tokens.pickerLabels(labels)
        switch selection {
        case .single(let binding):
            MonarchEntityPicker(
                label,
                selection: binding,
                entities: entities,
                selectedEntities: selected,
                prompt: allowsUnassigned ? Tokens.unassigned(labels) : nil,
                helper: helper,
                error: error,
                clearOptionTitle: allowsUnassigned ? Tokens.unassigned(labels) : nil,
                isRequired: isRequired,
                isLoading: isLoading,
                minSearchLength: minSearchLength,
                debounce: debounce,
                labels: pickerLabels,
                search: entitySearch
            )
        case .multiple(let binding):
            MonarchEntityPicker(
                label,
                selection: binding,
                entities: entities,
                selectedEntities: selected,
                prompt: Tokens.unassigned(labels),
                helper: helper,
                error: error,
                isRequired: isRequired,
                isLoading: isLoading,
                minSearchLength: minSearchLength,
                debounce: debounce,
                labels: pickerLabels,
                search: entitySearch
            )
        }
    }

    private func assign(_ id: String) {
        switch selection {
        case .single(let binding): binding.wrappedValue = id
        case .multiple(let binding): binding.wrappedValue = Tokens.assigningMe(binding.wrappedValue, currentUserID: id)
        }
        AccessibilityNotification.Announcement(labels.assigned ?? MonarchAssigneePickerStrings.assigned)
            .post()
    }
}

#if DEBUG
    #Preview("MonarchAssigneePicker") {
        let people = [
            MonarchAssigneePerson(id: "ana", name: "Ana Silva", email: "ana@example.com", hint: "3 open"),
            MonarchAssigneePerson(id: "sam", name: "Sam Lee", email: "sam@example.com"),
            MonarchAssigneePerson(id: "kim", name: "Kim Park", isDisabled: true),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAssigneePicker("Assignee", selection: .constant(nil), people: people, currentUserID: "sam")
                MonarchAssigneePicker("Assignee", selection: .constant("sam"), people: people, currentUserID: "sam")
                MonarchAssigneePicker(
                    "Reviewers",
                    selection: .constant(["ana", "kim"]),
                    people: people,
                    currentUserID: "sam"
                )
                MonarchAssigneePicker(
                    "Assignee",
                    selection: .constant(nil),
                    people: people,
                    error: "Choose who owns this.",
                    isRequired: true,
                    allowsUnassigned: false
                )
            }
        }
    }
#endif
