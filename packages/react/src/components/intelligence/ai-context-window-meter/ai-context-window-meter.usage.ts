'use client';

/**
 * Token counting for the context meter: how full the model's context window
 * is, from the conversation's messages. Uses the service's own figure when a
 * reply reported `usage`, your tokenizer when you pass one, and otherwise a
 * rough estimate that is labelled as approximate.
 */
import { useEffect, useMemo, useRef, useState } from 'react';
import type { AiMessage, AiPart, AiUsage } from '../ai-conversation/index.js';
import type { AiContextUsage, AiContextWindowPart } from './ai-context-window-meter.types.js';

/** Counts the tokens of a text, now or later (a tokenizer in a worker, a counting endpoint). */
export type AiTokenCounter = (text: string) => number | Promise<number>;

// Scripts written without spaces, where a character is about one token:
// CJK ideographs, kana, hangul and their extensions.
const DENSE = /[ᄀ-ᇿ⺀-〿぀-ヿ㄰-㆏㐀-䶿一-鿿가-힯豈-﫿＀-￯]/g;

/**
 * A rough token estimate with no tokenizer: one token for every four
 * characters, and one for each Chinese, Japanese or Korean character. It is
 * an approximation, good to about a fifth either way for English prose and
 * worse for code, numbers and other scripts. Never bill or cut off on it;
 * pass a real tokenizer as `countTokens` where the number matters.
 */
export function estimateAiTokens(text: string): number {
  if (!text) return 0;
  const dense = text.match(DENSE)?.length ?? 0;
  return dense + Math.ceil((text.length - dense) / 4);
}

function json(value: unknown): string {
  if (value === undefined || value === null) return '';
  if (typeof value === 'string') return value;
  try {
    return JSON.stringify(value) ?? '';
  } catch {
    return '';
  }
}

function partText(part: AiPart): string {
  switch (part.type) {
    case 'text':
      return part.text;
    case 'reasoning':
      return part.redacted ? '' : part.text;
    case 'tool-call':
      return [part.name, part.inputText ?? json(part.input), json(part.output), part.error ?? '']
        .filter(Boolean)
        .join('\n');
    case 'source':
      return [part.title, part.url ?? ''].filter(Boolean).join(' ');
    case 'file':
      return part.name;
    case 'artifact':
      return part.title;
    case 'edit-proposal':
      return json(part.diff);
    case 'plan':
      return part.steps.map((step) => step.label).join('\n');
    case 'ui':
      return json(part.props);
    case 'memory':
      return part.text;
    case 'citation':
      return part.quote ?? '';
    case 'error':
      return '';
  }
}

/**
 * The text of a message that takes up context: its text, visible reasoning,
 * tool names with their arguments and results, source titles, plan steps and
 * memories. Attached files count by name only: what a file costs depends on
 * the model, so add it yourself through `base` if you know it.
 */
export function aiMessageTokenText(message: Pick<AiMessage, 'parts'>): string {
  return message.parts.map(partText).filter(Boolean).join('\n');
}

/** The labels `useAiContextUsage` gives the parts of its breakdown. English defaults. */
export interface AiContextUsageLabels {
  /** The conversation's messages. */
  messages: string;
  /** Input read from a prompt cache. */
  cached: string;
  /** The model's hidden reasoning. */
  reasoning: string;
}

const DEFAULT_LABELS: AiContextUsageLabels = {
  messages: 'Messages',
  cached: 'Cached input',
  reasoning: 'Reasoning',
};

export interface UseAiContextUsageOptions {
  /** The conversation (`useAiConversation().messages`). */
  messages: readonly AiMessage[];
  /** The model's context window size, in tokens. */
  limit: number;
  /**
   * Counts the tokens of a text with the model's real tokenizer; it may
   * answer later. Each message is counted once and cached while it does not
   * change. Default: `estimateAiTokens`, an approximation.
   */
  countTokens?: AiTokenCounter | undefined;
  /**
   * What is always in the window besides the messages, e.g.
   * `[{ label: 'Instructions', tokens: 1200 }]`. Added to the count and shown
   * in the breakdown. Left out while the service's own figure is used, which
   * already includes it.
   */
  base?: readonly AiContextWindowPart[] | undefined;
  /** Tokens added per message for its role and framing. Default 4. */
  perMessageTokens?: number | undefined;
  /**
   * Use the `usage` a reply reported (from the stream's `usage` event) as
   * the starting point, and count only what came after it. Default true.
   */
  useReported?: boolean | undefined;
  /** Labels of the breakdown, for localisation. */
  labels?: Partial<AiContextUsageLabels> | undefined;
}

interface Cache {
  counter: AiTokenCounter | undefined;
  /** Settled counts, per message object. */
  counts: WeakMap<AiMessage, number>;
  /** Messages whose asynchronous count is on its way. */
  pending: WeakSet<AiMessage>;
}

const isStreamingMessage = (message: AiMessage) =>
  message.parts.some(
    (p) => (p.type === 'text' || p.type === 'reasoning') && p.state === 'streaming',
  );

const whole = (n: number) => (Number.isFinite(n) && n > 0 ? Math.round(n) : 0);

/**
 * Works out how full the context window is. When the latest reply reported
 * `usage`, that figure (input plus output tokens) is the starting point and
 * only newer messages are counted; otherwise every message is counted, with
 * `countTokens` or the approximate default. Counts are cached per message,
 * so a streaming reply only recounts itself, and a message is sent to an
 * asynchronous counter once, after it has finished streaming. Pass the
 * result to AiContextWindowMeter as `usage`.
 */
export function useAiContextUsage(options: UseAiContextUsageOptions): AiContextUsage {
  const {
    messages,
    limit,
    countTokens,
    base,
    perMessageTokens = 4,
    useReported = true,
    labels: labelsProp,
  } = options;
  const cache = useRef<Cache>({
    counter: countTokens,
    counts: new WeakMap(),
    pending: new WeakSet(),
  });
  if (cache.current.counter !== countTokens)
    cache.current = { counter: countTokens, counts: new WeakMap(), pending: new WeakSet() };
  const [version, setVersion] = useState(0);
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  // The latest reply that says what the service counted.
  let reportedAt = -1;
  if (useReported)
    for (let i = messages.length - 1; i >= 0; i--)
      if (messages[i]?.usage) {
        reportedAt = i;
        break;
      }
  const reported: AiUsage | undefined = reportedAt >= 0 ? messages[reportedAt]?.usage : undefined;

  const counted = useMemo(() => {
    const store = cache.current;
    let total = 0;
    let guessed = false;
    let waiting = 0;
    const ask: AiMessage[] = [];
    for (let i = reportedAt + 1; i < messages.length; i++) {
      const message = messages[i];
      // A message still waiting to be sent is not in the window yet.
      if (!message || message.queued) continue;
      const known = store.counts.get(message);
      if (known !== undefined) {
        total += known + perMessageTokens;
        continue;
      }
      const text = aiMessageTokenText(message);
      const streaming = isStreamingMessage(message);
      if (!countTokens || streaming) {
        // The default estimator; also used for a reply that is still being
        // written, so a tokenizer is not called for every token that arrives.
        const estimate = estimateAiTokens(text);
        if (!streaming) store.counts.set(message, estimate);
        total += estimate + perMessageTokens;
        guessed = true;
        continue;
      }
      // Until the counter answers, the estimate stands in.
      if (!store.pending.has(message)) ask.push(message);
      waiting += 1;
      total += estimateAiTokens(text) + perMessageTokens;
      guessed = true;
    }
    return { total, guessed, waiting, ask };
    // `version` re-reads the cache after an asynchronous count arrives.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [messages, reportedAt, countTokens, perMessageTokens, version]);

  // Ask the counter for messages it has not seen. Synchronous answers are
  // stored at once; asynchronous ones when they arrive, if still wanted.
  useEffect(() => {
    if (!countTokens || counted.ask.length === 0) return;
    const store = cache.current;
    let changed = false;
    for (const message of counted.ask) {
      if (store.pending.has(message) || store.counts.has(message)) continue;
      const settle = (value: number) => {
        store.pending.delete(message);
        store.counts.set(message, whole(value));
      };
      let answer: number | Promise<number>;
      try {
        answer = countTokens(aiMessageTokenText(message));
      } catch {
        // A counter that fails leaves the estimate in place.
        settle(estimateAiTokens(aiMessageTokenText(message)));
        changed = true;
        continue;
      }
      if (typeof answer === 'number') {
        settle(answer);
        changed = true;
        continue;
      }
      store.pending.add(message);
      answer.then(
        (value) => {
          if (cache.current !== store) return;
          settle(value);
          if (mounted.current) setVersion((v) => v + 1);
        },
        () => {
          if (cache.current !== store) return;
          settle(estimateAiTokens(aiMessageTokenText(message)));
          if (mounted.current) setVersion((v) => v + 1);
        },
      );
    }
    if (changed) setVersion((v) => v + 1);
  }, [counted, countTokens]);

  return useMemo<AiContextUsage>(() => {
    const labels = { ...DEFAULT_LABELS, ...labelsProp };
    const counting = counted.waiting > 0;
    // With a real counter, only a streaming reply or a pending count is a guess.
    const approximate = counted.guessed;
    if (reported) {
      const start = whole(reported.inputTokens) + whole(reported.outputTokens);
      const parts: AiContextWindowPart[] = [];
      if (reported.cacheReadTokens)
        parts.push({ label: labels.cached, tokens: whole(reported.cacheReadTokens) });
      if (reported.reasoningTokens)
        parts.push({ label: labels.reasoning, tokens: whole(reported.reasoningTokens) });
      return {
        used: start + counted.total,
        limit,
        approximate,
        counting,
        source: 'reported',
        ...(parts.length ? { breakdown: parts } : {}),
        reported,
      };
    }
    const fixed = (base ?? []).reduce((sum, part) => sum + whole(part.tokens), 0);
    return {
      used: fixed + counted.total,
      limit,
      approximate,
      counting,
      source: countTokens ? 'counted' : 'estimated',
      ...(base?.length
        ? { breakdown: [...base, { label: labels.messages, tokens: counted.total }] }
        : {}),
    };
  }, [base, countTokens, counted, labelsProp, limit, reported]);
}
