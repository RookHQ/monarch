/**
 * The contract between `useAiConversation` and a backend: the events a reply
 * streams, the request a turn sends, and the transport that connects them.
 * Types only, so every transport and mapper can depend on it.
 */
import type {
  AiAgentRef,
  AiFinishReason,
  AiMemoryPart,
  AiMessage,
  AiPart,
  AiToolCallPart,
  AiUsage,
} from './ai-conversation.types.js';

/** A memory change in a stream: what a `memory` part holds. */
export type AiMemoryEvent = Omit<AiMemoryPart, 'type'>;

/** What a transport streams back, one event at a time. */
export type AiStreamEvent =
  | { type: 'agent'; agent: AiAgentRef }
  | { type: 'text-delta'; text: string }
  | { type: 'reasoning-delta'; text: string }
  | { type: 'reasoning-end'; durationMs?: number; redacted?: boolean }
  /** Appends a whole part (tool call, source, citation, artifact, plan, …). */
  | { type: 'part'; part: AiPart }
  /** Updates the tool call with this id, e.g. `{ state: 'succeeded', output }`. */
  | { type: 'tool-update'; id: string; patch: Partial<Omit<AiToolCallPart, 'type' | 'id'>> }
  /**
   * A piece of a tool call's arguments as raw JSON text, appended to the
   * call's `inputText` so the arguments can be shown while they stream.
   */
  | { type: 'tool-input-delta'; id: string; delta: string }
  /** Why the model stopped. Sets `AiMessage.finishReason`. */
  | { type: 'finish'; reason: AiFinishReason }
  /** The service's id for this run, so it can be resumed after a reload. Sets `AiMessage.runId`. */
  | { type: 'run'; runId: string }
  | { type: 'usage'; usage: AiUsage; model?: string }
  /**
   * The assistant used, saved or forgot a memory. Adds a `memory` part to the
   * reply (the same id and action again updates it) and calls `onMemoryEvent`.
   */
  | { type: 'memory'; memory: AiMemoryEvent }
  | { type: 'done'; conversationId?: string }
  | {
      type: 'error';
      code?: string;
      message: string;
      /** Default: decided by `code` (see `isRetryableErrorCode`); unknown codes are not retryable. */
      retryable?: boolean;
      /** Milliseconds until a retry is allowed, e.g. from a `Retry-After` header. */
      retryAfterMs?: number;
      /** The run is still going on the service and can be resumed. Set by `useAiConversation` when the connection drops. */
      resumable?: boolean;
    };

/**
 * Why the conversation is asking the transport for a turn. `tool-result`
 * continues a reply after the client sent tool results back; `resume`
 * reconnects to a run that was already going.
 */
export type AiTrigger = 'send' | 'regenerate' | 'edit' | 'tool-result' | 'resume';

export interface AiTransportRequest {
  /** The visible conversation up to and including the new user message. */
  messages: readonly AiMessage[];
  trigger: AiTrigger;
  conversationId?: string;
  /** Aborted when the person presses Stop or the component unmounts. */
  signal: AbortSignal;
  /** Extra fields from `send(text, { body })`, e.g. the open document id. */
  body?: Readonly<Record<string, unknown>>;
}

export interface AiToolResponse {
  toolCallId: string;
  approved: boolean;
  /** Edited arguments, when the person changed them before approving. */
  input?: unknown;
  reason?: string;
  messages: readonly AiMessage[];
  conversationId?: string;
  signal: AbortSignal;
}

/** Reconnects to a run by its id, e.g. after the page reloaded mid-reply. */
export interface AiResumeRequest {
  /** From the reply's `run` event (`AiMessage.runId`). */
  runId: string;
  /** The last event id the client saw, when the service numbers its events. */
  lastEventId?: string;
  conversationId?: string;
  messages: readonly AiMessage[];
  signal: AbortSignal;
}

/**
 * Connects `useAiConversation` to a backend. Products implement it or use one
 * of the adapters below; Monarch never calls a model vendor directly.
 */
export interface AiTransport {
  /** Streams the assistant's reply to the latest user message. */
  stream(request: AiTransportRequest): AsyncIterable<AiStreamEvent>;
  /**
   * Continues the same reply after a person approves or denies a tool call.
   * Without it, approving only marks the call `running` and `onToolResponse`
   * is left to the product.
   */
  respondToTool?(response: AiToolResponse): AsyncIterable<AiStreamEvent>;
  /**
   * Optional: reconnects to a run that is still going on the service (or
   * replays a finished one), by the id from its `run` event. Enables
   * `useAiConversation().resumeRun`.
   */
  resume?(request: AiResumeRequest): AsyncIterable<AiStreamEvent>;
}
