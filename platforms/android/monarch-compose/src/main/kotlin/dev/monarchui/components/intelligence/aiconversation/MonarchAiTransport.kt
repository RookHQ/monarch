package dev.monarchui.components.intelligence.aiconversation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Connects `MonarchAiConversation` to a backend. Apps implement it; Monarch
 * does no networking of its own and never calls a model vendor.
 *
 * ```kotlin
 * class MyTransport(private val service: MyService) : MonarchAiTransport {
 *     override fun stream(request: MonarchAiTransportRequest): Flow<MonarchAiStreamEvent> =
 *         service.reply(request).flowOn(Dispatchers.IO)
 * }
 * ```
 *
 * The conversation collects the flow in its coroutine scope. When the person
 * stops the reply, sends again or the scope ends, the collection is cancelled,
 * which cancels the flow: release the connection in `onCompletion` or
 * `awaitClose`. A flow that throws ends the reply with a `network` error.
 */
public interface MonarchAiTransport {
    /** Streams the assistant's reply to the latest user message. */
    public fun stream(request: MonarchAiTransportRequest): Flow<MonarchAiStreamEvent>

    /**
     * Whether [respondToTool] is implemented. Without it, approving a tool
     * call only marks the call as running and the conversation's
     * `onToolResponse` is left to the app.
     */
    public val respondsToTools: Boolean get() = false

    /** Continues the same reply after a person approves or denies a tool call. Empty by default. */
    public fun respondToTool(response: MonarchAiToolResponse): Flow<MonarchAiStreamEvent> = emptyFlow()

    /** Whether [resume] is implemented. It enables `MonarchAiConversation.resumeRun`. */
    public val resumesRuns: Boolean get() = false

    /**
     * Reconnects to a run that is still going on the service, or replays a
     * finished one, by the id from its `Run` event. Empty by default.
     */
    public fun resume(request: MonarchAiResumeRequest): Flow<MonarchAiStreamEvent> = emptyFlow()
}
