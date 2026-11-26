package com.rookhq.monarch.components.intelligence.aicomputerview

import com.rookhq.monarch.components.actions.button.MonarchButtonVariant

/** Who controls the session and which controls and announcements each status has. */
internal object AiComputerViewRules {
    private val Finished = setOf(
        MonarchAiComputerViewStatus.Done,
        MonarchAiComputerViewStatus.Failed,
        MonarchAiComputerViewStatus.Stopped,
    )
    private val AgentHolds = setOf(
        MonarchAiComputerViewStatus.Starting,
        MonarchAiComputerViewStatus.Working,
        MonarchAiComputerViewStatus.Paused,
        MonarchAiComputerViewStatus.WaitingForYou,
    )
    private val AgentActs = setOf(MonarchAiComputerViewStatus.Starting, MonarchAiComputerViewStatus.Working)

    fun finished(status: MonarchAiComputerViewStatus): Boolean = status in Finished

    fun agentActing(status: MonarchAiComputerViewStatus): Boolean = status in AgentActs

    fun canPause(status: MonarchAiComputerViewStatus, handled: Boolean): Boolean = handled && agentActing(status)

    fun canResume(status: MonarchAiComputerViewStatus, handled: Boolean): Boolean =
        handled && status == MonarchAiComputerViewStatus.Paused

    fun canTakeOver(status: MonarchAiComputerViewStatus, handled: Boolean): Boolean = handled && status in AgentHolds

    fun canHandBack(status: MonarchAiComputerViewStatus, handled: Boolean): Boolean =
        handled && status == MonarchAiComputerViewStatus.TakenOver

    fun canStop(status: MonarchAiComputerViewStatus, handled: Boolean): Boolean = handled && !finished(status)

    /** Take over is the primary action while the agent waits for the person. */
    fun takeOverVariant(status: MonarchAiComputerViewStatus): MonarchButtonVariant = when (status) {
        MonarchAiComputerViewStatus.WaitingForYou -> MonarchButtonVariant.Primary
        else -> MonarchButtonVariant.Secondary
    }

    /** The announcement a change from [before] to [after] makes. */
    fun news(before: MonarchAiComputerViewStatus, after: MonarchAiComputerViewStatus): AiComputerViewNews = when {
        after == MonarchAiComputerViewStatus.TakenOver -> AiComputerViewNews.TakenOver
        before == MonarchAiComputerViewStatus.TakenOver && agentActing(after) -> AiComputerViewNews.Released
        else -> AiComputerViewNews.Status
    }

    /** A failure or a wait for the person is announced at once; other changes politely. */
    fun urgent(status: MonarchAiComputerViewStatus): Boolean =
        status == MonarchAiComputerViewStatus.Failed || status == MonarchAiComputerViewStatus.WaitingForYou
}
