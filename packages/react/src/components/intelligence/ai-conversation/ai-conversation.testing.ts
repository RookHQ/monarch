/**
 * A testing kit for products that build on `useAiConversation`: fixtures of
 * stream events, server-sent event bodies cut into chunks (to prove a mapper
 * survives any chunk boundary), a transport that replays recorded turns, and
 * a conversation runner without React. No test-runner dependency: plain
 * functions that work with any runner.
 */
import type {
  AiStreamEvent,
  AiToolResponse,
  AiTransport,
  AiTransportRequest,
} from './ai-conversation.protocol.js';
import type { AiStreamMapper, SseFrame } from './ai-conversation.sse.js';
import { messageText, type AiMessage } from './ai-conversation.types.js';
import { applyStreamEvent } from './ai-conversation.reduce.js';
import { parseJson } from './ai-conversation.json.js';

/** Collects an async iterable into an array. */
export async function collectStream<T>(iterable: AsyncIterable<T>): Promise<T[]> {
  const out: T[] = [];
  for await (const item of iterable) out.push(item);
  return out;
}

/** A recorded reply: its events, and the message and text they produce. */
export interface AiStreamFixture {
  events: readonly AiStreamEvent[];
  /** The assistant message after every event is applied. */
  message: AiMessage;
  /** `messageText(message)`. */
  text: string;
  /** A transport that streams these events for every turn. */
  transport: AiTransport;
}

/** Builds a fixture from events: the final message, its text and a transport that replays it. */
export function createStreamFixture(
  events: readonly AiStreamEvent[],
  options: { id?: string; createdAt?: string } = {},
): AiStreamFixture {
  let message: AiMessage = {
    id: options.id ?? 'fixture',
    role: 'assistant',
    parts: [],
    createdAt: options.createdAt ?? '',
  };
  for (const e of events) message = applyStreamEvent(message, e);
  return { events, message, text: messageText(message), transport: replayTransport([events]) };
}

/**
 * How `sseFromEvents` cuts the body: one chunk per `frame`, one per `byte`
 * (splitting multi-byte characters and `\r\n` pairs), fixed `size`s, cuts
 * `at` byte offsets, or pseudo-random sizes from a `seed` (repeatable).
 */
export type AiSseChunking =
  | 'frame'
  | 'byte'
  | { size: number }
  | { at: readonly number[] }
  | { seed: number; maxSize?: number };

export interface SseFromEventsOptions {
  chunking?: AiSseChunking;
  /** Line end between fields. Default `\n`. */
  lineEnding?: '\n' | '\r\n' | '\r';
  /** Adds `id: 1`, `id: 2`… to each frame. Default false. */
  ids?: boolean;
  /** The `data:` payload of an event. Default `JSON.stringify(event)`. */
  encode?: (event: AiStreamEvent) => string;
}

/** The body text `sseFromEvents` sends, before chunking. */
export function sseText(
  events: readonly (AiStreamEvent | string)[],
  options: Omit<SseFromEventsOptions, 'chunking'> = {},
): string {
  const nl = options.lineEnding ?? '\n';
  const encode = options.encode ?? ((e: AiStreamEvent) => JSON.stringify(e));
  return events
    .map((e, i) => {
      const data = typeof e === 'string' ? e : encode(e);
      const lines = data.split('\n').map((l) => `data: ${l}`);
      return [...(options.ids ? [`id: ${i + 1}`] : []), ...lines].join(nl) + nl + nl;
    })
    .join('');
}

/** A small, repeatable pseudo-random generator (mulberry32). */
function seeded(seed: number) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** Cuts bytes into chunks as `chunking` says. */
export function chunkBytes(
  bytes: Uint8Array,
  chunking: Exclude<AiSseChunking, 'frame'>,
): Uint8Array[] {
  const cuts: number[] = [];
  if (chunking === 'byte') for (let i = 1; i < bytes.length; i++) cuts.push(i);
  else if ('size' in chunking)
    for (let i = Math.max(1, chunking.size); i < bytes.length; i += Math.max(1, chunking.size))
      cuts.push(i);
  else if ('at' in chunking)
    cuts.push(...[...chunking.at].filter((n) => n > 0 && n < bytes.length).sort((x, y) => x - y));
  else {
    const next = seeded(chunking.seed);
    const max = Math.max(1, chunking.maxSize ?? 16);
    for (let i = 1 + Math.floor(next() * max); i < bytes.length; i += 1 + Math.floor(next() * max))
      cuts.push(i);
  }
  const out: Uint8Array[] = [];
  let from = 0;
  for (const cut of [...new Set(cuts)]) {
    out.push(bytes.slice(from, cut));
    from = cut;
  }
  out.push(bytes.slice(from));
  return out;
}

/** A response body that delivers the given chunks, one per read. */
export function bodyFromChunks(
  chunks: readonly (string | Uint8Array)[],
): ReadableStream<Uint8Array> {
  const encoder = new TextEncoder();
  let i = 0;
  return new ReadableStream<Uint8Array>({
    pull(controller) {
      const chunk = chunks[i++];
      if (chunk === undefined) controller.close();
      else controller.enqueue(typeof chunk === 'string' ? encoder.encode(chunk) : chunk);
    },
  });
}

/**
 * A server-sent events body carrying the events (or raw `data` strings such
 * as `[DONE]`), cut into chunks. Feed it to `readSseFrames` or return it from
 * a fake `fetch` to test a mapper against every chunk boundary.
 */
export function sseFromEvents(
  events: readonly (AiStreamEvent | string)[],
  options: SseFromEventsOptions = {},
): ReadableStream<Uint8Array> {
  const { chunking = 'frame', ...rest } = options;
  if (chunking === 'frame') return bodyFromChunks(events.map((e) => sseText([e], rest)));
  return bodyFromChunks(chunkBytes(new TextEncoder().encode(sseText(events, rest)), chunking));
}

/**
 * Every way to cut `text` in two: `[head, tail]` for each byte offset. For
 * fuzz tests that prove a reader gives the same frames for any split.
 */
export function* everySplit(text: string): Generator<[Uint8Array, Uint8Array]> {
  const bytes = new TextEncoder().encode(text);
  for (let i = 0; i <= bytes.length; i++) yield [bytes.slice(0, i), bytes.slice(i)];
}

/** A mapper whose frames are Monarch's own stream events as JSON; `[DONE]` ends the reply. */
export function aiStreamEventMapper(): AiStreamMapper {
  return {
    map(frame: SseFrame) {
      if (frame.data === '[DONE]') return [{ type: 'done' }];
      const parsed = parseJson(frame.data);
      return parsed &&
        typeof parsed === 'object' &&
        typeof (parsed as { type?: unknown }).type === 'string'
        ? [parsed as AiStreamEvent]
        : [];
    },
  };
}

/** One recorded turn: its events, and optionally what follows a tool response. */
export type AiRecordedTurn =
  | readonly AiStreamEvent[]
  | {
      events: readonly AiStreamEvent[];
      /** Events after a tool approval or denial. */
      toolEvents?: readonly AiStreamEvent[];
    };

export interface ReplayTransportOptions {
  /** Milliseconds between events. Default 0. */
  delayMs?: number;
  /** After the last turn: `repeat` the last one (default) or `error`. */
  whenExhausted?: 'repeat' | 'error';
}

/** A transport that replays recorded turns in order, and remembers what it was asked. */
export interface AiReplayTransport extends AiTransport {
  /** Every request `stream` received, oldest first. */
  readonly requests: AiTransportRequest[];
  /** Every tool response `respondToTool` received. */
  readonly toolResponses: AiToolResponse[];
}

const pause = (ms: number, signal: AbortSignal) =>
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
 * Replays recorded turns: the first request gets the first turn, and so on.
 * Stops when aborted. Use it to pin a product's behaviour to a real
 * recording of its service.
 */
export function replayTransport(
  recorded: readonly AiRecordedTurn[],
  options: ReplayTransportOptions = {},
): AiReplayTransport {
  const { delayMs = 0, whenExhausted = 'repeat' } = options;
  const requests: AiTransportRequest[] = [];
  const toolResponses: AiToolResponse[] = [];
  let turn = 0;
  const pick = (): { events: readonly AiStreamEvent[]; toolEvents?: readonly AiStreamEvent[] } => {
    const at =
      turn < recorded.length ? turn : whenExhausted === 'repeat' ? recorded.length - 1 : -1;
    turn += 1;
    const t = at >= 0 ? recorded[at] : undefined;
    if (!t) return { events: [{ type: 'error', code: 'unknown', message: 'No recorded turn.' }] };
    return Array.isArray(t)
      ? { events: t as readonly AiStreamEvent[] }
      : (t as Exclude<AiRecordedTurn, readonly AiStreamEvent[]>);
  };
  let lastTool: readonly AiStreamEvent[] | undefined;
  async function* play(events: readonly AiStreamEvent[], signal: AbortSignal) {
    for (const event of events) {
      await pause(delayMs, signal);
      if (signal.aborted) return;
      yield event;
    }
  }
  return {
    requests,
    toolResponses,
    stream(request) {
      requests.push(request);
      const t = pick();
      lastTool = t.toolEvents;
      return play(t.events, request.signal);
    },
    respondToTool(response) {
      toolResponses.push(response);
      return play(lastTool ?? [{ type: 'done' }], response.signal);
    },
  };
}

/**
 * Runs one turn without React: sends `messages` through the transport and
 * applies every event to a new assistant message. For contract tests of a
 * transport or mapper.
 */
export async function runTurn(
  transport: AiTransport,
  messages: readonly AiMessage[],
  options: { signal?: AbortSignal; body?: Record<string, unknown>; id?: string } = {},
): Promise<{ message: AiMessage; events: AiStreamEvent[] }> {
  const events = await collectStream(
    transport.stream({
      messages,
      trigger: 'send',
      signal: options.signal ?? new AbortController().signal,
      ...(options.body ? { body: options.body } : {}),
    }),
  );
  let message: AiMessage = {
    id: options.id ?? 'reply',
    role: 'assistant',
    parts: [],
    createdAt: '',
  };
  for (const e of events) message = applyStreamEvent(message, e);
  return { message, events };
}

/**
 * Plays a whole conversation without React: each prompt becomes a user
 * message and the transport's reply is applied to the next assistant
 * message. Returns the transcript, oldest first.
 */
export async function simulateConversation(
  transport: AiTransport,
  prompts: readonly string[],
): Promise<AiMessage[]> {
  const transcript: AiMessage[] = [];
  let n = 0;
  for (const prompt of prompts) {
    transcript.push({
      id: `user-${++n}`,
      role: 'user',
      parts: [{ type: 'text', text: prompt, state: 'done' }],
      createdAt: '',
    });
    const { message } = await runTurn(transport, transcript, { id: `assistant-${n}` });
    transcript.push(message);
  }
  return transcript;
}
