import type {
  AiStreamEvent,
  AiToolResponse,
  AiTransport,
  AiTransportRequest,
} from './ai-conversation.protocol.js';
import { messageText, type AiFilePart } from './ai-conversation.types.js';
import {
  createSseTransport,
  readSseFrames,
  type SseRetryOptions,
  type SseTransportMessages,
} from './ai-conversation.sse.js';
import { AI_CONVERSATION_MESSAGES, isRetryableErrorCode } from './ai-conversation.errors.js';

export * from './ai-conversation.protocol.js';

// ---------------------------------------------------------------- SSE

/**
 * Reads `data: …` lines from a server-sent events body. Yields each data
 * payload as a string; stops at `[DONE]`. `readSseFrames` also gives the
 * `event` and `id` fields.
 */
export async function* readServerSentEvents(
  body: ReadableStream<Uint8Array>,
  signal?: AbortSignal,
): AsyncIterable<string> {
  for await (const frame of readSseFrames(body, { signal })) {
    if (frame.data === '[DONE]') return;
    if (frame.data) yield frame.data;
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
  /** Try again when the request fails before any event arrived. See `SseTransportOptions.retry`. */
  retry?: SseRetryOptions | boolean;
  /** Abandon a reply that sends nothing for this long (milliseconds). */
  idleTimeoutMs?: number;
  /** User-facing text of the errors the transport raises itself. */
  messages?: Partial<SseTransportMessages>;
  /** Custom fetch, for tests or proxies. */
  fetch?: typeof fetch;
}

/** Options for `mapRookIntelligenceEvent`. */
export interface RookIntelligenceMapOptions {
  /** Shown when the service's error has no text. */
  unavailable?: string;
}

let toolCounter = 0;

/** The attachments of a message, as the Rook body's `attachments` field. */
function rookAttachments(files: readonly AiFilePart[]) {
  return files.map((f) => ({
    name: f.name,
    mediaType: f.mediaType,
    ...(f.url ? { url: f.url } : {}),
    ...(f.fileId ? { fileId: f.fileId } : {}),
    ...(f.size !== undefined ? { size: f.size } : {}),
  }));
}

/** Maps one Rook Intelligence payload (`agent | chunk | tool | done | error`) to stream events. */
export function mapRookIntelligenceEvent(
  payload: unknown,
  options: RookIntelligenceMapOptions = {},
): AiStreamEvent | null {
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
    case 'error': {
      const code = typeof event.code === 'string' && event.code ? event.code : 'service';
      return {
        type: 'error',
        code,
        message:
          typeof event.content === 'string' && event.content
            ? event.content
            : (options.unavailable ?? AI_CONVERSATION_MESSAGES.unavailable),
        retryable:
          typeof event.retryable === 'boolean' ? event.retryable : isRetryableErrorCode(code),
      };
    }
    default:
      return null;
  }
}

/**
 * Transport for Rook Intelligence (`POST /v1/agents/{id}/chat`, SSE). Sends
 * `{ message, conversationId, history, attachments?, ...body }` (attachments
 * only when the last message has files) and maps its
 * `agent | chunk | tool | done | error` events to the message model.
 */
export function rookIntelligenceTransport(options: RookIntelligenceTransportOptions): AiTransport {
  const { url, credentials = 'include', historyLimit = 16 } = options;
  return createSseTransport({
    url,
    credentials,
    ...(options.headers ? { headers: options.headers } : {}),
    ...(options.fetch ? { fetch: options.fetch } : {}),
    ...(options.retry !== undefined ? { retry: options.retry } : {}),
    ...(options.idleTimeoutMs !== undefined ? { idleTimeoutMs: options.idleTimeoutMs } : {}),
    ...(options.messages ? { messages: options.messages } : {}),
    buildBody({ messages, conversationId, body }) {
      const last = messages[messages.length - 1];
      const history = messages
        .slice(0, -1)
        .filter((m) => m.role === 'user' || m.role === 'assistant')
        .map((m) => ({ role: m.role, content: messageText(m).slice(0, 4000) }))
        .filter((m) => m.content)
        .slice(-historyLimit);
      const files = last ? last.parts.filter((p): p is AiFilePart => p.type === 'file') : [];
      return {
        message: last ? messageText(last) : '',
        ...(conversationId ? { conversationId } : {}),
        history,
        ...(files.length ? { attachments: rookAttachments(files) } : {}),
        ...body,
      };
    },
    createMapper: () => ({
      map(frame) {
        // The service sends its own `done` event; `[DONE]` only closes the body.
        if (frame.data === '[DONE]') return [];
        let payload: unknown;
        try {
          payload = JSON.parse(frame.data);
        } catch {
          return [];
        }
        const event = mapRookIntelligenceEvent(payload, {
          unavailable: options.messages?.unavailable ?? AI_CONVERSATION_MESSAGES.unavailable,
        });
        return event ? [event] : [];
      },
    }),
  });
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
    const done = () => {
      clearTimeout(t);
      signal.removeEventListener('abort', done);
      resolve();
    };
    const t = setTimeout(done, ms);
    signal.addEventListener('abort', done);
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
