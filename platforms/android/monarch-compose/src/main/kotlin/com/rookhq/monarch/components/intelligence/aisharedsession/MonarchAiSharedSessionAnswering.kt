package com.rookhq.monarch.components.intelligence.aisharedsession

import androidx.compose.runtime.Immutable

/**
 * The answer in progress in a `MonarchAiSharedSession`.
 *
 * @property participantId the agent that is answering.
 * @property forParticipantId the person whose request is being answered.
 * @property label a short description of the request, for example "Summarize the Q3 notes".
 */
@Immutable
public class MonarchAiSharedSessionAnswering(
    public val participantId: String,
    public val forParticipantId: String,
    public val label: String? = null,
)
