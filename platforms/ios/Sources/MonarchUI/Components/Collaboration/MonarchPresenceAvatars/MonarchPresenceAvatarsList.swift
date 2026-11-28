import SwiftUI

/// The list of everyone here, shown in a sheet by ``MonarchPresenceAvatars``.
struct MonarchPresenceAvatarsList: View {
    let people: [MonarchPresencePerson]
    let labels: MonarchPresenceAvatarsLabels
    let onPersonTap: (@MainActor (MonarchPresencePerson) -> Void)?
    let onDone: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        NavigationStack {
            List(people) { person in
                if let onPersonTap {
                    Button {
                        onDone()
                        onPersonTap(person)
                    } label: {
                        row(person)
                    }
                    .buttonStyle(.plain)
                    .monarchRowsOnCanvas()
                } else {
                    row(person)
                        .monarchRowsOnCanvas()
                }
            }
            .listStyle(.plain)
            .monarchCanvas()
            .navigationTitle(labels.list ?? MonarchPresenceAvatarsStrings.list)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(labels.done ?? MonarchPresenceAvatarsStrings.done) { onDone() }
                }
            }
        }
    }

    private func row(_ person: MonarchPresencePerson) -> some View {
        HStack(spacing: MonarchPresenceAvatarsTokens.listSpacing) {
            MonarchAvatar(
                url: person.avatarURL,
                name: person.name,
                seed: person.seed,
                size: MonarchPresenceAvatarsTokens.listAvatarSize,
                status: person.status,
                isDecorative: true
            )
            VStack(alignment: .leading) {
                Text(person.name)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors[role: MonarchPresenceAvatarsTokens.nameColor])
                Text(MonarchPresenceAvatarsTokens.detail(person, labels: labels))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchPresenceAvatarsTokens.detailColor])
            }
            Spacer(minLength: 0)
        }
        .monarchMinimumHitTarget()
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchPresenceAvatarsTokens.personText(person, labels: labels))
        .accessibilityInputLabels(MonarchPresenceAvatarsTokens.inputLabels(person).map { Text($0) })
    }
}
