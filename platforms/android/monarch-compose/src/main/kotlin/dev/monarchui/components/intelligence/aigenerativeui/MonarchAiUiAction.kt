package dev.monarchui.components.intelligence.aigenerativeui

import androidx.compose.runtime.Immutable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON

/**
 * An action from a generated component, as the `onAction` of
 * `MonarchAiGenerativeUi` receives it. Send it back to the agent, for
 * example as the result of [toolCallId] or as a new message.
 *
 * @property type what happened, such as `submit`, `select` or `confirm`.
 * @property payload plain data from the component, sanitised like props. `null` for none.
 * @property component the registered name of the component that sent it.
 * @property partId the id of the part, when it has one.
 * @property toolCallId the tool call the part answers, when it has one.
 */
@Immutable
public data class MonarchAiUiAction(
    public val type: String,
    public val payload: MonarchAiJSON? = null,
    public val component: String,
    public val partId: String? = null,
    public val toolCallId: String? = null,
)
