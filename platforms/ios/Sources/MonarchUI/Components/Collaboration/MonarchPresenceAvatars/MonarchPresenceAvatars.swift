import SwiftUI

/// Who is here now: an overlapping stack of avatars with presence dots and
/// a "+N" tile, that opens the list of everyone in a sheet.
///
/// ```swift
/// MonarchPresenceAvatars(people, maxVisible: 3)
/// MonarchPresenceAvatars(people) { person in jump(to: person) }
/// ```
///
/// The stack shows at most `maxVisible` people, then "+N". When people are
/// hidden, or when `onPersonTap` is given, the whole stack is one button
/// that opens a sheet listing everyone with their presence and activity; a
/// person in that list is tapped to run `onPersonTap`. Single overlapping
/// avatars are too small to be touch targets on their own.
///
/// Accessibility: the stack is named "5 people here". A stack that opens
/// the list is one button whose value names each visible person with their
/// presence and activity, then "2 more". Otherwise each avatar is an image
/// named "Ana Silva, Online, Editing". Presence is always spoken, so it
/// never depends on the dot colour. Voice Control accepts a person's name
/// or the initials shown on their avatar.
public struct MonarchPresenceAvatars: View {
    private let people: [MonarchPresencePerson]
    private let maxVisible: Int
    private let size: MonarchAvatarSize
    private let labels: MonarchPresenceAvatarsLabels
    private let onPersonTap: (@MainActor (MonarchPresencePerson) -> Void)?

    @State private var showsList = false

    /// Presence avatars.
    ///
    /// - Parameters:
    ///   - people: Who is here, the most relevant first.
    ///   - maxVisible: Show at most this many avatars, then a "+N" tile.
    ///   - size: The size of the avatars in the stack.
    ///   - labels: Replacements for the built-in strings.
    ///   - onPersonTap: Runs when a person is tapped in the list, for example to jump to their cursor.
    public init(
        _ people: [MonarchPresencePerson],
        maxVisible: Int = 4,
        size: MonarchAvatarSize = .sm,
        labels: MonarchPresenceAvatarsLabels = MonarchPresenceAvatarsLabels(),
        onPersonTap: (@MainActor (MonarchPresencePerson) -> Void)? = nil
    ) {
        self.people = people
        self.maxVisible = maxVisible
        self.size = size
        self.labels = labels
        self.onPersonTap = onPersonTap
    }

    private typealias Tokens = MonarchPresenceAvatarsTokens

    /// The content of the view.
    public var body: some View {
        let label = Tokens.label(count: people.count, labels: labels)
        if Tokens.opensList(total: people.count, maxVisible: maxVisible, hasPersonAction: onPersonTap != nil) {
            Button {
                showsList = true
            } label: {
                stack.monarchMinimumHitTarget()
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
            .accessibilityValue(Tokens.value(people, maxVisible: maxVisible, labels: labels))
            .accessibilityHint(labels.showAll ?? MonarchPresenceAvatarsStrings.showAll)
            .accessibilityAddTraits(.isButton)
            .sheet(isPresented: $showsList) {
                MonarchPresenceAvatarsList(people: people, labels: labels, onPersonTap: onPersonTap) {
                    showsList = false
                }
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
            }
        } else {
            stack
        }
    }

    private var stack: some View {
        MonarchAvatarGroup(
            MonarchPresenceAvatarsTokens.label(count: people.count, labels: labels),
            people,
            maxVisible: maxVisible,
            size: size
        ) { person in
            MonarchAvatar(
                url: person.avatarURL,
                name: person.name,
                seed: person.seed,
                size: size,
                status: person.status,
                statusLabel: MonarchPresenceAvatarsTokens.spokenStatus(person, labels: labels)
            )
            .accessibilityInputLabels(MonarchPresenceAvatarsTokens.inputLabels(person).map { Text($0) })
        }
    }
}

#if DEBUG
    #Preview("MonarchPresenceAvatars") {
        let people = [
            MonarchPresencePerson(id: "1", name: "Ana Silva", activity: "Editing"),
            MonarchPresencePerson(id: "2", name: "Ben Okafor", status: .away),
            MonarchPresencePerson(id: "3", name: "Chen Wei", status: .busy, customStatus: "In a meeting"),
            MonarchPresencePerson(id: "4", name: "Dara Kim", status: .doNotDisturb),
            MonarchPresencePerson(id: "5", name: "Eli Roth", status: .offline),
            MonarchPresencePerson(id: "6", name: "Fay Ng"),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchPresenceAvatars(Array(people.prefix(3)))
                MonarchPresenceAvatars(people, maxVisible: 3)
                MonarchPresenceAvatars(people, size: .md) { _ in }
            }
        }
    }
#endif
