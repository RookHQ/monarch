/// The events of one reply, as an asynchronous sequence that may throw.
public typealias MonarchAiEventStream = AsyncThrowingStream<MonarchAiStreamEvent, any Error>

/// Connects ``MonarchAiConversation`` to a backend. Products implement it;
/// Monarch does no networking of its own and never calls a model vendor.
///
/// ```swift
/// struct MyTransport: MonarchAiTransport {
///     func stream(_ request: MonarchAiTransportRequest) -> MonarchAiEventStream {
///         MonarchAiEventStream { continuation in
///             let task = Task {
///                 do {
///                     for try await event in try await myService.reply(to: request) {
///                         continuation.yield(event)
///                     }
///                     continuation.finish()
///                 } catch {
///                     continuation.finish(throwing: error)
///                 }
///             }
///             continuation.onTermination = { _ in task.cancel() }
///         }
///     }
/// }
/// ```
///
/// The conversation calls these methods on the main actor and reads the
/// stream in a task. When the person stops the reply the task is cancelled
/// and the stream is terminated, so stop work in `onTermination`. A stream
/// that throws ends the reply with a `network` error.
public protocol MonarchAiTransport: Sendable {
    /// Streams the assistant's reply to the latest user message.
    func stream(_ request: MonarchAiTransportRequest) -> MonarchAiEventStream

    /// Whether `respondToTool(_:)` is implemented. Without it, approving a
    /// tool call only marks the call as running and the conversation's
    /// `onToolResponse` is left to the product.
    var respondsToTools: Bool { get }

    /// Continues the same reply after a person approves or denies a tool call.
    func respondToTool(_ response: MonarchAiToolResponse) -> MonarchAiEventStream

    /// Whether `resume(_:)` is implemented. It enables `MonarchAiConversation.resumeRun(_:)`.
    var resumesRuns: Bool { get }

    /// Reconnects to a run that is still going on the service, or replays a
    /// finished one, by the id from its `run` event.
    func resume(_ request: MonarchAiResumeRequest) -> MonarchAiEventStream
}

extension MonarchAiTransport {
    /// `false`: the transport does not continue a reply after a tool response.
    public var respondsToTools: Bool { false }

    /// An empty stream.
    public func respondToTool(_ response: MonarchAiToolResponse) -> MonarchAiEventStream {
        MonarchAiEventStream { $0.finish() }
    }

    /// `false`: the transport cannot resume a run.
    public var resumesRuns: Bool { false }

    /// An empty stream.
    public func resume(_ request: MonarchAiResumeRequest) -> MonarchAiEventStream {
        MonarchAiEventStream { $0.finish() }
    }
}
