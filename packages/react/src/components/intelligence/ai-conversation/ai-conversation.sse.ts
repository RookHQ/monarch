/**
 * The shared server-sent events transport: one place for the request, the
 * frame reader, retry before the first event, resume after a dropped
 * connection and the idle timeout. Vendor and service formats plug in as a
 * mapper (see `ai-conversation.providers.ts` and `rookIntelligenceTransport`).
 */
import type { AiMessage } from './ai-conversation.types.js';
import { messageText } from './ai-conversation.types.js';
import type {
  AiResumeRequest,
  AiStreamEvent,
  AiToolResponse,
  AiTransport,
  AiTransportRequest,
} from './ai-conversation.protocol.js';
import {
  AI_CONVERSATION_MESSAGES,
  classifyHttpStatus,
  parseRetryAfter,
  type AiConversationMessages,
} from './ai-conversation.errors.js';

/** One server-sent events frame. */
export interface SseFrame {
  /** The `event:` field; empty when the frame has none. */
  event: string;
  /** The `data:` lines joined with new lines. */
  data: string;
  /** The `id:` field, when the frame has one. */
  id?: string;
}

class IdleTimeoutError extends Error {
  constructor() {
    super('The assistant stopped responding.');
    this.name = 'IdleTimeoutError';
  }
}

function parseFrame(raw: string): SseFrame | null {
  let event = '';
  let id: string | undefined;
  const data: string[] = [];
  for (const line of raw.split('\n')) {
    if (!line || line.startsWith(':')) continue;
    const colon = line.indexOf(':');
    const field = colon < 0 ? line : line.slice(0, colon);
    const value = colon < 0 ? '' : line.slice(colon + 1).replace(/^ /, '');
    if (field === 'data') data.push(value);
    else if (field === 'event') event = value;
    else if (field === 'id' && !value.includes('\0')) id = value;
  }
  if (!data.length && !event && id === undefined) return null;
  return { event, data: data.join('\n'), ...(id !== undefined ? { id } : {}) };
}

/**
 * Normalises line ends of one decoded chunk. A `\r` at the very end may be
 * the first half of a `\r\n` split across chunks, so it is held back and
 * returned as `carry` for the next chunk instead of becoming a line end now.
 */
export function normaliseSseChunk(
  carry: string,
  chunk: string,
  final = false,
): { text: string; carry: string } {
  let text = carry + chunk;
  let next = '';
  if (!final && text.endsWith('\r')) {
    text = text.slice(0, -1);
    next = '\r';
  }
  return { text: text.replace(/\r\n?/g, '\n'), carry: next };
}

/**
 * Reads server-sent events frames from a response body: `event`, `data` and
 * `id` fields, comments skipped, `\n`, `\r\n` and `\r` line ends (also when a
 * `\r\n` is split between two chunks). With `idleTimeoutMs`, a body that
 * sends nothing for that long is abandoned with an error, so a stalled
 * connection does not hang the reply forever.
 */
export async function* readSseFrames(
  body: ReadableStream<Uint8Array>,
  options: { signal?: AbortSignal | undefined; idleTimeoutMs?: number | undefined } = {},
): AsyncIterable<SseFrame> {
  const { signal, idleTimeoutMs } = options;
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let carry = '';
  const read = () => {
    if (!idleTimeoutMs) return reader.read();
    let timer: ReturnType<typeof setTimeout> | undefined;
    const idle = new Promise<never>((_, reject) => {
      timer = setTimeout(() => reject(new IdleTimeoutError()), idleTimeoutMs);
    });
    return Promise.race([reader.read(), idle]).finally(() => clearTimeout(timer));
  };
  function* drain(): Generator<SseFrame> {
    let boundary = buffer.indexOf('\n\n');
    while (boundary >= 0) {
      const frame = parseFrame(buffer.slice(0, boundary));
      buffer = buffer.slice(boundary + 2);
      if (frame) yield frame;
      boundary = buffer.indexOf('\n\n');
    }
  }
  try {
    while (true) {
      if (signal?.aborted) return;
      const { value, done } = await read();
      if (done) break;
      const next = normaliseSseChunk(carry, decoder.decode(value, { stream: true }));
      carry = next.carry;
      buffer += next.text;
      yield* drain();
    }
    buffer += normaliseSseChunk(carry, decoder.decode(), true).text;
    yield* drain();
    const last = parseFrame(buffer);
    if (last?.data) yield last;
  } finally {
    // Cancelling also stops the network read when the caller leaves early.
    await reader.cancel().catch(() => undefined);
  }
}

/**
 * Turns the frames of one reply into stream events. A new mapper is made for
 * every reply, so it can keep state (a tool call arriving in pieces, the
 * model name) without leaking it into the next turn.
 */
export interface AiStreamMapper {
  map(frame: SseFrame): readonly AiStreamEvent[];
  /** Called once when the body ends, for anything still pending. */
  flush?(): readonly AiStreamEvent[];
}

export interface SseRetryOptions {
  /** Extra attempts after the first. Default 2. */
  attempts?: number;
  /** First wait in milliseconds; doubles each attempt. Default 500. */
  baseDelayMs?: number;
  /** Longest wait in milliseconds. A `Retry-After` header is honoured up to this. Default 8000. */
  maxDelayMs?: number;
}

export interface SseTransportOptions {
  /** The endpoint of your own service. Never a model vendor's API with a key in the browser. */
  url: string | ((request: AiTransportRequest) => string);
  /** Extra request headers (authentication). Called per request when a function. */
  headers?: HeadersInit | (() => HeadersInit | Promise<HeadersInit>);
  /** Default `same-origin`. */
  credentials?: RequestCredentials;
  /** The JSON request body. Default `{ messages: toChatMessages(messages), conversationId, ...body }`. */
  buildBody?: (request: AiTransportRequest) => unknown;
  /** Makes the mapper for one reply. */
  createMapper: () => AiStreamMapper;
  /**
   * Try again when the request fails before any event arrived (network
   * failure, HTTP 429, 502, 503 or 504). Nothing has been shown yet, so a
   * retry cannot duplicate text. Off unless set.
   */
  retry?: SseRetryOptions | boolean;
  /**
   * Reconnect when the connection drops mid-reply. Only used when the server
   * sent `id:` fields: the request is repeated with a `Last-Event-ID` header
   * and the server must continue after that event. Off unless set.
   */
  resume?: { attempts?: number } | boolean;
  /** Abandon a reply that sends nothing for this long (milliseconds). Off unless set. */
  idleTimeoutMs?: number;
  /**
   * Enables tool approval over the same endpoint: the JSON body for a tool
   * response. Without it the transport has no `respondToTool`.
   */
  buildToolBody?: (response: AiToolResponse) => unknown;
  /** Endpoint for tool responses. Default: `url`; required when `url` is a function. */
  toolUrl?: string;
  /**
   * Enables `resume`: reconnecting to a run by the id from its `run` event,
   * e.g. after a reload. Returns the URL to `GET`; the server replays the
   * run's events (after `Last-Event-ID`, when sent) and continues it.
   */
  resumeUrl?: (request: AiResumeRequest) => string;
  /** User-facing text of the errors this transport raises itself. */
  messages?: Partial<SseTransportMessages>;
  /** Custom fetch, for tests or proxies. */
  fetch?: typeof fetch;
}

/** User-facing text of the errors an SSE transport raises itself. */
export type SseTransportMessages = Pick<
  AiConversationMessages,
  | 'rateLimit'
  | 'unreachable'
  | 'connectionLost'
  | 'idleTimeout'
  | 'auth'
  | 'badRequest'
  | 'unavailable'
>;

const DEFAULT_MESSAGES: SseTransportMessages = {
  rateLimit: AI_CONVERSATION_MESSAGES.rateLimit,
  unreachable: AI_CONVERSATION_MESSAGES.unreachable,
  connectionLost: AI_CONVERSATION_MESSAGES.connectionLost,
  idleTimeout: AI_CONVERSATION_MESSAGES.idleTimeout,
  auth: AI_CONVERSATION_MESSAGES.auth,
  badRequest: AI_CONVERSATION_MESSAGES.badRequest,
  unavailable: AI_CONVERSATION_MESSAGES.unavailable,
};

/** Frame ids remembered for de-duplication after a resume. */
const MAX_SEEN_IDS = 10_000;

const RETRY_STATUS = new Set([429, 502, 503, 504]);

/** `{ role, content }` for the user and assistant turns, text only: what most chat endpoints accept. */
export function toChatMessages(
  messages: readonly AiMessage[],
): { role: 'user' | 'assistant'; content: string }[] {
  const out: { role: 'user' | 'assistant'; content: string }[] = [];
  for (const m of messages) {
    if (m.role !== 'user' && m.role !== 'assistant') continue;
    const content = messageText(m);
    if (content) out.push({ role: m.role, content });
  }
  return out;
}

const sleep = (ms: number, signal: AbortSignal) =>
  new Promise<void>((resolve) => {
    if (ms <= 0 || signal.aborted) return resolve();
    const done = () => {
      clearTimeout(timer);
      signal.removeEventListener('abort', done);
      resolve();
    };
    const timer = setTimeout(done, ms);
    signal.addEventListener('abort', done);
  });

function retryAfterMs(response: Response): number | null {
  return parseRetryAfter(response.headers?.get?.('retry-after'));
}

/** An event stream that fails when read, for errors raised before a request could start. */
function failing(error: Error): AsyncIterable<AiStreamEvent> {
  return { [Symbol.asyncIterator]: () => ({ next: () => Promise.reject(error) }) };
}

/**
 * A transport for any service that answers with server-sent events. It owns
 * the request and the connection; `createMapper` decides what each frame
 * means. The reply ends at the mapper's `done` or `error` event, or when the
 * body ends.
 */
export function createSseTransport(options: SseTransportOptions): AiTransport {
  const m = { ...DEFAULT_MESSAGES, ...options.messages };
  const retry: Required<SseRetryOptions> | null = options.retry
    ? {
        attempts: 2,
        baseDelayMs: 500,
        maxDelayMs: 8000,
        ...(typeof options.retry === 'object' ? options.retry : {}),
      }
    : null;
  const resumeAttempts = options.resume
    ? ((typeof options.resume === 'object' ? options.resume.attempts : undefined) ?? 3)
    : 0;

  async function* run(
    url: string,
    body: unknown,
    signal: AbortSignal,
    method: 'POST' | 'GET' = 'POST',
    fromEventId?: string,
  ): AsyncIterable<AiStreamEvent> {
    const doFetch = options.fetch ?? fetch;
    const mapper = options.createMapper();
    let lastEventId: string | undefined = fromEventId;
    let started = false;
    let attempt = 0;
    let resumes = 0;
    // Ids already delivered: a server that replays from the start after a
    // resume must not duplicate text.
    const seen = new Set<string>();

    while (true) {
      const headers =
        typeof options.headers === 'function' ? await options.headers() : (options.headers ?? {});
      let response: Response;
      try {
        response = await doFetch(url, {
          method,
          credentials: options.credentials ?? 'same-origin',
          signal,
          headers: {
            ...(method === 'POST' ? { 'Content-Type': 'application/json' } : {}),
            Accept: 'text/event-stream',
            ...headers,
            ...(lastEventId !== undefined ? { 'Last-Event-ID': lastEventId } : {}),
          },
          ...(method === 'POST' ? { body: JSON.stringify(body) } : {}),
        });
      } catch (error) {
        if (signal.aborted) return;
        if (!started && retry && attempt < retry.attempts) {
          await sleep(Math.min(retry.maxDelayMs, retry.baseDelayMs * 2 ** attempt), signal);
          attempt += 1;
          continue;
        }
        if (started && lastEventId !== undefined && resumes < resumeAttempts) {
          resumes += 1;
          await sleep(500 * resumes, signal);
          continue;
        }
        throw error;
      }

      if (!response.ok || !response.body) {
        if (!started && retry && attempt < retry.attempts && RETRY_STATUS.has(response.status)) {
          const wait = retryAfterMs(response) ?? retry.baseDelayMs * 2 ** attempt;
          await sleep(Math.min(retry.maxDelayMs, wait), signal);
          attempt += 1;
          continue;
        }
        const failure = classifyHttpStatus(response.status);
        const wait = retryAfterMs(response);
        yield {
          type: 'error',
          code: failure.code,
          message: m[failure.message],
          retryable: failure.retryable,
          ...(wait !== null ? { retryAfterMs: wait } : {}),
        };
        return;
      }

      let dropped: unknown = null;
      try {
        for await (const frame of readSseFrames(response.body, {
          signal,
          idleTimeoutMs: options.idleTimeoutMs,
        })) {
          if (frame.id !== undefined) {
            if (seen.has(frame.id)) continue;
            if (seen.size >= MAX_SEEN_IDS) seen.clear();
            seen.add(frame.id);
            lastEventId = frame.id;
          }
          for (const event of mapper.map(frame)) {
            started = true;
            yield event;
            if (event.type === 'done' || event.type === 'error') return;
          }
        }
      } catch (error) {
        if (signal.aborted) return;
        dropped = error ?? new Error(m.connectionLost);
      }
      if (signal.aborted) return;

      if (dropped) {
        // Mid-reply: only a server that numbers its events can continue where it stopped.
        if (lastEventId !== undefined && resumes < resumeAttempts) {
          resumes += 1;
          await sleep(500 * resumes, signal);
          continue;
        }
        yield {
          type: 'error',
          code: dropped instanceof IdleTimeoutError ? 'timeout' : 'network',
          message: dropped instanceof IdleTimeoutError ? m.idleTimeout : m.connectionLost,
          retryable: true,
        };
        return;
      }

      // The body ended cleanly without a terminal event.
      for (const event of mapper.flush?.() ?? []) {
        yield event;
        if (event.type === 'done' || event.type === 'error') return;
      }
      return;
    }
  }

  const transport: AiTransport = {
    stream(request) {
      const url = typeof options.url === 'function' ? options.url(request) : options.url;
      const body = options.buildBody
        ? options.buildBody(request)
        : {
            messages: toChatMessages(request.messages),
            ...(request.conversationId ? { conversationId: request.conversationId } : {}),
            ...request.body,
          };
      return run(url, body, request.signal);
    },
  };
  const { buildToolBody, resumeUrl } = options;
  if (buildToolBody) {
    transport.respondToTool = (response) => {
      const url = options.toolUrl ?? (typeof options.url === 'string' ? options.url : undefined);
      // Fails when read, so the conversation shows an error instead of hanging.
      if (!url)
        return failing(new Error('createSseTransport: set `toolUrl` when `url` is a function.'));
      return run(url, buildToolBody(response), response.signal);
    };
  }
  if (resumeUrl) {
    transport.resume = (request) =>
      run(resumeUrl(request), undefined, request.signal, 'GET', request.lastEventId);
  }
  return transport;
}
