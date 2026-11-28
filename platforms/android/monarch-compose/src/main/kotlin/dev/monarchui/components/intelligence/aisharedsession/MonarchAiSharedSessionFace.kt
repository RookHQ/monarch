package dev.monarchui.components.intelligence.aisharedsession

import androidx.compose.runtime.Composable
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentAvatar
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef

/** The decorative picture of a participant: a round avatar with a presence dot, or a square agent avatar. */
@Composable
internal fun MonarchAiSharedSessionFace(
    participant: MonarchAiSharedSessionParticipant,
    answering: Boolean,
    size: MonarchAvatarSize,
) {
    if (participant.kind == MonarchAiSharedSessionKind.Agent) {
        MonarchAiAgentAvatar(
            agent = MonarchAiAgentRef(name = participant.name, id = participant.id),
            image = participant.image,
            state = if (answering) MonarchAiAgentState.Working else MonarchAiAgentState.Idle,
            size = size,
            decorative = true,
        )
    } else {
        MonarchAvatar(
            image = participant.image,
            name = participant.name,
            size = size,
            status = MonarchAiSharedSessionLogic.presenceStatus(participant.presence),
            seed = participant.id,
            decorative = true,
        )
    }
}
