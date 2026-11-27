import SwiftUI

/// The decorative picture of a participant of ``MonarchAiSharedSession``:
/// the square agent avatar, turning while it answers, or a person's round
/// avatar with a presence dot.
struct MonarchAiSharedSessionFace: View {
    let participant: MonarchAiSharedSessionParticipant
    let isAnswering: Bool
    let size: MonarchAvatarSize

    var body: some View {
        switch participant.kind {
        case .agent:
            MonarchAiAgentAvatar(
                MonarchAiAgentRef(
                    id: participant.id,
                    name: participant.name,
                    avatar: participant.avatarURL?.absoluteString
                ),
                state: isAnswering ? .working : .idle,
                size: size,
                isDecorative: true
            )
            .accessibilityHidden(true)
        case .person:
            MonarchAvatar(
                url: participant.avatarURL,
                name: participant.name,
                seed: participant.id,
                size: size,
                status: MonarchAiSharedSessionTokens.presenceStatus(participant.presence),
                isDecorative: true
            )
        }
    }
}
