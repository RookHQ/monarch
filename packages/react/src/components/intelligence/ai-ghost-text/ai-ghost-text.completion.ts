'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type {
  AiCompletionAcceptance,
  AiCompletionFunction,
  AiCompletionStatus,
} from './ai-ghost-text.types.js';

/**
 * The next word of a suggestion: any leading white space, then the run of
 * characters up to the next white space. Text with no spaces (for example
 * Chinese or Japanese) is split with `Intl.Segmenter` where the runtime has
 * it, and returned whole otherwise. Returns an empty string for an empty
 * suggestion.
 */
export function aiGhostTextNextWord(suggestion: string, locale?: string): string {
  if (!suggestion) return '';
  const lead = /^\s*/.exec(suggestion)?.[0] ?? '';
  const run = /^\S+/.exec(suggestion.slice(lead.length))?.[0] ?? '';
  if (!run) return suggestion;
  if (typeof Intl !== 'undefined' && 'Segmenter' in Intl) {
    try {
      const segments = new Intl.Segmenter(locale, { granularity: 'word' }).segment(run);
      let word = '';
      for (const segment of segments) {
        word += segment.segment;
        // Punctuation before a word travels with it ("(hello" is one step).
        if (segment.isWordLike) break;
      }
      // Punctuation after it too, so "word," never leaves a lone comma behind.
      const rest = run.slice(word.length);
      const tail = /^[^\p{L}\p{N}]+/u.exec(rest)?.[0] ?? '';
      return lead + word + tail;
    } catch {
      // An unknown locale: fall through to the white-space rule.
    }
  }
  return lead + run;
}

export interface UseAiCompletionOptions {
  /** Asks the model for a continuation. Called after the debounce; stale calls are aborted. */
  complete: AiCompletionFunction;
  /** How long typing must pause before a request is sent, in milliseconds. Default 300. */
  debounceMs?: number | undefined;
  /** Fewest non-space characters before the cursor for a request. Default 3. */
  minChars?: number | undefined;
  /** Turns suggestions off (and clears the current one) when false. Default true. */
  enabled?: boolean | undefined;
  /** Called when `complete` throws or rejects (not when it is aborted). */
  onError?: ((error: unknown) => void) | undefined;
}

export interface UseAiCompletionResult {
  /** Where the completion is. */
  status: AiCompletionStatus;
  /** The suggested continuation; empty when there is none. */
  suggestion: string;
  /** The text the suggestion (or the pending request) belongs to. */
  text: string;
  /** The cursor the suggestion goes in at. */
  cursor: number;
  /**
   * Tell the hook what the field holds now. It debounces a request, keeps the
   * rest of a suggestion the person is typing through, and does nothing when
   * neither value changed. Ignored while composing or disabled.
   */
  update: (text: string, cursor: number) => void;
  /** Drops the suggestion and aborts any request. It stays away until the text changes. */
  dismiss: () => void;
  /** Accepts the whole suggestion. Returns what to insert, or null when there is none. */
  accept: () => AiCompletionAcceptance | null;
  /** Accepts the next word and keeps the rest showing. Returns null when there is none. */
  acceptWord: () => AiCompletionAcceptance | null;
  /**
   * Call with true on `compositionstart` and false on `compositionend`. While
   * an input method is composing, nothing is requested or shown.
   */
  setComposing: (composing: boolean) => void;
}

interface State {
  status: AiCompletionStatus;
  suggestion: string;
  text: string;
  cursor: number;
}

const IDLE: State = { status: 'idle', suggestion: '', text: '', cursor: 0 };

/**
 * Inline completion state for one text field: debounces requests, aborts the
 * stale ones, never suggests during IME composition, and accepts a suggestion
 * whole or word by word. It holds no DOM: `AiGhostText` wires it to a field,
 * or call `update` from your own editor.
 */
export function useAiCompletion(options: UseAiCompletionOptions): UseAiCompletionResult {
  const { debounceMs = 300, minChars = 3, enabled = true } = options;
  const latest = useRef(options);
  latest.current = options;

  const [state, setState] = useState<State>(IDLE);
  // Read synchronously by event handlers, which run between renders.
  const current = useRef<State>(IDLE);
  const commit = useCallback((next: State) => {
    current.current = next;
    setState(next);
  }, []);

  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const controller = useRef<AbortController | null>(null);
  const sequence = useRef(0);
  const composing = useRef(false);

  const cancel = useCallback(() => {
    sequence.current += 1;
    if (timer.current !== null) {
      clearTimeout(timer.current);
      timer.current = null;
    }
    controller.current?.abort();
    controller.current = null;
  }, []);

  const dismiss = useCallback(() => {
    cancel();
    const { text, cursor, status } = current.current;
    if (status !== 'idle') commit({ status: 'idle', suggestion: '', text, cursor });
  }, [cancel, commit]);

  const update = useCallback(
    (text: string, cursorAt: number) => {
      const cursor = Math.min(Math.max(0, Math.trunc(cursorAt) || 0), text.length);
      const now = current.current;
      if (!enabled || composing.current) {
        cancel();
        if (now.status !== 'idle' || now.text !== text || now.cursor !== cursor)
          commit({ status: 'idle', suggestion: '', text, cursor });
        return;
      }
      // Nothing changed (a caret event, or the echo of our own insertion).
      if (now.text === text && now.cursor === cursor) return;

      // Typing through the suggestion keeps the rest of it: no new request.
      if (now.status === 'ready' && cursor > now.cursor) {
        const typed = text.slice(now.cursor, cursor);
        const sameAround =
          text.slice(0, now.cursor) === now.text.slice(0, now.cursor) &&
          text.slice(cursor) === now.text.slice(now.cursor);
        if (
          sameAround &&
          typed.length < now.suggestion.length &&
          now.suggestion.startsWith(typed)
        ) {
          commit({
            status: 'ready',
            suggestion: now.suggestion.slice(typed.length),
            text,
            cursor,
          });
          return;
        }
      }

      cancel();
      if (text.slice(0, cursor).trim().length < Math.max(0, minChars)) {
        commit({ status: 'idle', suggestion: '', text, cursor });
        return;
      }
      commit({ status: 'waiting', suggestion: '', text, cursor });
      const id = sequence.current;
      timer.current = setTimeout(
        () => {
          timer.current = null;
          const abort = new AbortController();
          controller.current = abort;
          commit({ status: 'loading', suggestion: '', text, cursor });
          const stale = () => id !== sequence.current || abort.signal.aborted;
          Promise.resolve()
            .then(() => latest.current.complete(text, cursor, abort.signal))
            .then(
              (result) => {
                if (stale()) return;
                controller.current = null;
                const suggestion = typeof result === 'string' ? result : '';
                commit({ status: suggestion ? 'ready' : 'idle', suggestion, text, cursor });
              },
              (error: unknown) => {
                if (stale()) return;
                controller.current = null;
                commit({ status: 'error', suggestion: '', text, cursor });
                latest.current.onError?.(error);
              },
            );
        },
        Math.max(0, debounceMs),
      );
    },
    [cancel, commit, debounceMs, enabled, minChars],
  );

  const take = useCallback(
    (wordOnly: boolean): AiCompletionAcceptance | null => {
      const now = current.current;
      if (now.status !== 'ready' || !now.suggestion) return null;
      const inserted = wordOnly ? aiGhostTextNextWord(now.suggestion) : now.suggestion;
      const rest = now.suggestion.slice(inserted.length);
      const text = now.text.slice(0, now.cursor) + inserted + now.text.slice(now.cursor);
      const cursor = now.cursor + inserted.length;
      commit({ status: rest ? 'ready' : 'idle', suggestion: rest, text, cursor });
      return { inserted, text, cursor };
    },
    [commit],
  );
  const accept = useCallback(() => take(false), [take]);
  const acceptWord = useCallback(() => take(true), [take]);

  const setComposing = useCallback(
    (value: boolean) => {
      composing.current = value;
      if (value) dismiss();
    },
    [dismiss],
  );

  useEffect(() => {
    if (!enabled) dismiss();
  }, [enabled, dismiss]);
  // Nothing outlives the component: no timer, no request.
  useEffect(() => cancel, [cancel]);

  return useMemo(
    () => ({ ...state, update, dismiss, accept, acceptWord, setComposing }),
    [state, update, dismiss, accept, acceptWord, setComposing],
  );
}
