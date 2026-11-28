package com.rookhq.monarch.components.intelligence.aivoicemode

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiVoiceMode`. Every
 * `null`, and every state missing from [states], uses the built-in string
 * named after it in `strings_aivoicemode.xml`.
 *
 * @property label the TalkBack name of the surface, from the assistant's name: "Voice conversation with Ada".
 * @property assistant the assistant's name when no agent is given: "Assistant".
 * @property you the speaker of the person's own captions: "You".
 * @property states the always-visible text of each state, also announced when the state changes.
 * @property microphoneMuted shown beside the state when the microphone is muted in another state.
 * @property mute the Mute toggle.
 * @property interrupt the Interrupt button.
 * @property captions the Captions toggle.
 * @property captionsLabel the TalkBack name of the captions log.
 * @property end the End button.
 * @property restart the Start again button of the ended state.
 * @property retry the Retry button of the error state.
 * @property captionsEmpty shown in the captions log before anyone has spoken.
 * @property elapsed read before the running time: "Call time".
 * @property lasted the duration line of the ended state, from the formatted time: "Lasted 4:05".
 * @property errorFallback the error text when none is given.
 */
@Immutable
public class MonarchAiVoiceModeLabels(
    public val label: ((agentName: String) -> String)? = null,
    public val assistant: String? = null,
    public val you: String? = null,
    public val states: Map<MonarchAiVoiceModeState, String> = emptyMap(),
    public val microphoneMuted: String? = null,
    public val mute: String? = null,
    public val interrupt: String? = null,
    public val captions: String? = null,
    public val captionsLabel: String? = null,
    public val end: String? = null,
    public val restart: String? = null,
    public val retry: String? = null,
    public val captionsEmpty: String? = null,
    public val elapsed: String? = null,
    public val lasted: ((duration: String) -> String)? = null,
    public val errorFallback: String? = null,
)
