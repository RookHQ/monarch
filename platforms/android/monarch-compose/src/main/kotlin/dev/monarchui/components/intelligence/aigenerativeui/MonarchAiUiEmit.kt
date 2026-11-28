package dev.monarchui.components.intelligence.aigenerativeui

import androidx.compose.runtime.Stable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON

/**
 * The action channel a generated component receives: call it to send an
 * action, such as a form submission, back to the host.
 *
 * ```kotlin
 * MonarchButton(text = "Book", onClick = { emit("submit", MonarchAiJSON.Object(mapOf("slot" to slot))) })
 * ```
 *
 * The payload is sanitised before it reaches the host. When the host does
 * not listen for actions, calling it does nothing; [connected] says which.
 * Monarch creates it; build one yourself only in tests and previews.
 *
 * @param handler receives the type and the payload of each action. `null` for a channel nobody listens to.
 */
@Stable
public class MonarchAiUiEmit(private val handler: ((type: String, payload: MonarchAiJSON?) -> Unit)? = null) {
    /** Whether the host listens for actions. Hide controls that only send actions when it does not. */
    public val connected: Boolean get() = handler != null

    /**
     * Sends an action. An empty type is ignored.
     *
     * @param type what happened, such as `submit`.
     * @param payload plain data to send with it.
     */
    public operator fun invoke(type: String, payload: MonarchAiJSON? = null) {
        if (type.isEmpty()) return
        handler?.invoke(type, payload)
    }
}
