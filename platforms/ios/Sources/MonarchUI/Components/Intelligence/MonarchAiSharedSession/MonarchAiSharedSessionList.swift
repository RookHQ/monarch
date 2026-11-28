import SwiftUI

/// The list of everyone in a ``MonarchAiSharedSession``, shown in a sheet:
/// people (you first) and agents, each with role, presence and activity,
/// and a Follow toggle beside other people when following is offered.
struct MonarchAiSharedSessionList: View {
    let participants: [MonarchAiSharedSessionParticipant]
    let answering: MonarchAiSharedSessionAnswering?
    let answeringText: String?
    let following: Binding<Set<String>>?
    let labels: MonarchAiSharedSessionLabels
    let onDone: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchAiSharedSessionTokens

    var body: some View {
        let groups = Tokens.groups(participants)
        NavigationStack {
            List {
                Group {
                    section(labels.people ?? MonarchAiSharedSessionStrings.people, groups.people)
                    section(labels.agents ?? MonarchAiSharedSessionStrings.agents, groups.agents)

                }
                .monarchRowsOnCanvas()
            }
            .listStyle(.plain)
            .monarchCanvas()
            .navigationTitle(labels.listTitle ?? MonarchAiSharedSessionStrings.listTitle)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(labels.done ?? MonarchAiSharedSessionStrings.done, action: onDone)
                }
            }
        }
    }

    @ViewBuilder private func section(_ title: String, _ members: [MonarchAiSharedSessionParticipant]) -> some View {
        if !members.isEmpty {
            Section {
                ForEach(members) { participant in
                    row(participant)
                }
            } header: {
                Text(title)
                    .monarchTextStyle(.caption, weight: Tokens.nameWeight)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
    }

    private func doing(_ participant: MonarchAiSharedSessionParticipant) -> String? {
        if answering?.participantID == participant.id, let answeringText { return answeringText }
        return participant.activity.map { Tokens.activityText($0, labels: labels) }
    }

    private func row(_ participant: MonarchAiSharedSessionParticipant) -> some View {
        let details = [
            Tokens.kindText(participant, labels: labels),
            Tokens.presenceText(participant.presence, labels: labels),
            doing(participant),
        ]
        .compactMap { $0 }
        return HStack(spacing: Tokens.rowSpacing) {
            HStack(spacing: Tokens.rowSpacing) {
                MonarchAiSharedSessionFace(
                    participant: participant,
                    isAnswering: answering?.participantID == participant.id,
                    size: Tokens.listAvatarSize
                )
                VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                    Text(Tokens.displayName(participant, labels: labels))
                        .monarchTextStyle(.body, weight: Tokens.nameWeight)
                        .foregroundStyle(theme.colors.textDefault)
                    Text(details.joined(separator: MonarchAiSharedSessionStrings.separator))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(([Tokens.displayName(participant, labels: labels)] + details).joined(separator: ", "))
            if let following, participant.kind == .person, !participant.isYou {
                Toggle(isOn: followBinding(following, participant.id)) {
                    Label(labels.follow ?? MonarchAiSharedSessionStrings.follow, systemImage: Tokens.eyeSymbol)
                }
                .toggleStyle(.monarchButton(.ghost, size: .sm))
                .accessibilityLabel(
                    labels.followLabel?(participant.name) ?? MonarchAiSharedSessionStrings.followLabel(participant.name)
                )
            }
        }
        .monarchMinimumHitTarget()
    }

    private func followBinding(_ following: Binding<Set<String>>, _ id: String) -> Binding<Bool> {
        Binding(
            get: { following.wrappedValue.contains(id) },
            set: { isOn in
                if isOn { following.wrappedValue.insert(id) } else { following.wrappedValue.remove(id) }
            }
        )
    }
}
