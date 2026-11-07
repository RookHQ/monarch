import type {
  AiAgentRef,
  AiMessage,
  AiPart,
  AiToolCallPart,
  AiUsage,
} from './ai-conversation.types.js';
import { messageText } from './ai-conversation.types.js';

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
  | { type: 'usage'; usage: AiUsage; model?: string }
  | { type: 'done'; conversationId?: string }
  | { type: 'error'; code?: string; message: string; retryable?: boolean };

/** Why the conversation is asking the transport for a turn. */
export type AiTrigger = 'send' | 'regenerate' | 'edit';

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
}

// ---------------------------------------------------------------- SSE

/**
 * Reads `data: …` lines from a server-sent events body. Yields each data
 * payload as a string; stops at `[DONE]`.
 */
export async function* readServerSentEvents(
  body: ReadableStream<Uint8Array>,
  signal?: AbortSignal,
): AsyncIterable<string> {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  try {
    while (true) {
      if (signal?.aborted) return;
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      let boundary = buffer.indexOf('\n\n');
      while (boundary >= 0) {
        const raw = buffer.slice(0, boundary);
        buffer = buffer.slice(boundary + 2);
        const data = raw
          .split('\n')
          .filter((line) => line.startsWith('data:'))
          .map((line) => line.slice(5).replace(/^ /, ''))
          .join('\n');
        if (data === '[DONE]') return;
        if (data) yield data;
        boundary = buffer.indexOf('\n\n');
      }
    }
  } finally {
    reader.releaseLock();
  }
}

export interface RookIntelligenceTransportOptions {
  /** Full URL of the chat endpoint, e.g. `https://…/v1/agents/dossier/chat`. */
  url: string;
  /** Extra request headers (authentication). Called per request when a function. */
  headers?: HeadersInit | (() => HeadersInit | Promise<HeadersInit>);
  /** Default `include`, for cookie sessions across Rook subdomains. */
  credentials?: RequestCredentials;
  /** How many earlier turns to send as history. The service accepts up to 16. Default 16. */
  historyLimit?: number;
  /** Custom fetch, for tests or proxies. */
  fetch?: typeof fetch;
}

let toolCounter = 0;

/** Maps one Rook Intelligence payload (`agent | chunk | tool | done | error`) to stream events. */
export function mapRookIntelligenceEvent(payload: unknown): AiStreamEvent | null {
  if (!payload || typeof payload !== 'object') return null;
  const event = payload as Record<string, unknown>;
  switch (event.type) {
    case 'agent': {
      const a = (event.agent ?? {}) as Record<string, unknown>;
      return {
        type: 'agent',
        agent: {
          name: String(a.name ?? ''),
          ...(typeof a.codename === 'string' ? { codename: a.codename } : {}),
          ...(typeof a.role === 'string' ? { role: a.role } : {}),
          ...(typeof a.avatar === 'string' ? { avatar: a.avatar } : {}),
        },
      };
    }
    case 'chunk':
      return typeof event.content === 'string' ? { type: 'text-delta', text: event.content } : null;
    case 'tool': {
      // Rook Intelligence tool events are UI actions the tool already applied
      // (e.g. a document edit). They arrive complete, so they are `succeeded`.
      const tool = (event.tool ?? {}) as Record<string, unknown>;
      const now = new Date().toISOString();
      return {
        type: 'part',
        part: {
          type: 'tool-call',
          id: typeof tool.id === 'string' ? tool.id : `tool-${++toolCounter}`,
          name: typeof tool.type === 'string' ? tool.type : 'action',
          input: tool,
          state: 'succeeded',
          startedAt: now,
          endedAt: now,
        },
      };
    }
    case 'done':
      return typeof event.conversationId === 'string'
        ? { type: 'done', conversationId: event.conversationId }
        : { type: 'done' };
    case 'error':
      return {
        type: 'error',
        code: 'service',
        message:
          typeof event.content === 'string' ? event.content : 'The assistant is unavailable.',
        retryable: true,
      };
    default:
      return null;
  }
}

/**
 * Transport for Rook Intelligence (`POST /v1/agents/{id}/chat`, SSE). Sends
 * `{ message, conversationId, history, ...body }` and maps its
 * `agent | chunk | tool | done | error` events to the message model.
 */
export function rookIntelligenceTransport(options: RookIntelligenceTransportOptions): AiTransport {
  const { url, credentials = 'include', historyLimit = 16 } = options;
  return {
    async *stream({ messages, conversationId, signal, body }) {
      const last = messages[messages.length - 1];
      const history = messages
        .slice(0, -1)
        .filter((m) => m.role === 'user' || m.role === 'assistant')
        .map((m) => ({ role: m.role, content: messageText(m).slice(0, 4000) }))
        .filter((m) => m.content)
        .slice(-historyLimit);
      const headers =
        typeof options.headers === 'function' ? await options.headers() : (options.headers ?? {});
      const doFetch = options.fetch ?? fetch;
      const response = await doFetch(url, {
        method: 'POST',
        credentials,
        signal,
        headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream', ...headers },
        body: JSON.stringify({
          message: last ? messageText(last) : '',
          ...(conversationId ? { conversationId } : {}),
          history,
          ...body,
        }),
      });
      if (!response.ok || !response.body) {
        yield {
          type: 'error',
          code: response.status === 429 ? 'rate-limit' : `http-${response.status}`,
          message:
            response.status === 429
              ? 'Too many requests. Wait a moment and try again.'
              : 'The assistant could not be reached.',
          retryable: true,
        };
        return;
      }
      for await (const data of readServerSentEvents(response.body, signal)) {
        let payload: unknown;
        try {
          payload = JSON.parse(data);
        } catch {
          continue;
        }
        const event = mapRookIntelligenceEvent(payload);
        if (event) yield event;
      }
    },
  };
}

// ---------------------------------------------------------------- mock

export interface MockTransportOptions {
  /** The events of each reply. A function receives the request, e.g. to echo the prompt. */
  reply: readonly AiStreamEvent[] | ((request: AiTransportRequest) => readonly AiStreamEvent[]);
  /** Events after a tool response. Default: the tool call succeeds and the reply says so. */
  toolReply?: (response: AiToolResponse) => readonly AiStreamEvent[];
  /** Milliseconds between events. `0` streams synchronously between awaits. Default 30. */
  delayMs?: number;
}

const wait = (ms: number, signal: AbortSignal) =>
  new Promise<void>((resolve) => {
    if (ms <= 0 || signal.aborted) return resolve();
    const t = setTimeout(resolve, ms);
    signal.addEventListener('abort', () => {
      clearTimeout(t);
      resolve();
    });
  });

/**
 * A deterministic transport for docs, tests and prototypes: it replays the
 * events you give it with a fixed delay, and stops when aborted.
 */
export function mockTransport(options: MockTransportOptions): AiTransport {
  const { delayMs = 30 } = options;
  async function* replay(events: readonly AiStreamEvent[], signal: AbortSignal) {
    for (const event of events) {
      await wait(delayMs, signal);
      if (signal.aborted) return;
      yield event;
    }
  }
  return {
    stream(request) {
      const events = typeof options.reply === 'function' ? options.reply(request) : options.reply;
      return replay(events, request.signal);
    },
    respondToTool(response) {
      const events = options.toolReply
        ? options.toolReply(response)
        : ([
            {
              type: 'tool-update' as const,
              id: response.toolCallId,
              patch: response.approved
                ? { state: 'succeeded' as const, endedAt: new Date().toISOString() }
                : { state: 'denied' as const },
            },
            { type: 'text-delta', text: response.approved ? 'Done.' : 'Okay, I did not do that.' },
            { type: 'done' },
          ] satisfies AiStreamEvent[]);
      return replay(events, response.signal);
    },
  };
}

/** Splits text into word-sized `text-delta` events, for mock replies. */
export function textDeltas(text: string): AiStreamEvent[] {
  return (text.match(/\S+\s*|\s+/g) ?? []).map((t) => ({ type: 'text-delta', text: t }));
}
