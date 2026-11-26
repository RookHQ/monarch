'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { AiEditProposalPart } from '../ai-conversation/index.js';
import { diffWords, type AiDiffSegment, type AiEditHunk } from '../ai-edit-proposal/index.js';

/** What `rewrite` is asked to change. */
export interface AiRewriteRequest {
  /** The whole text of the field. */
  text: string;
  /** The part to rewrite: the selection, or the whole text when nothing is selected. */
  selection: string;
  /** Where the selection starts in `text`. */
  start: number;
  /** Where the selection ends in `text`. */
  end: number;
  /** What the person asked for, e.g. "Make it shorter". */
  instruction: string;
}

/**
 * Sends the selection and the instruction to a model and resolves with the
 * replacement for the selection. Stop work when `signal` aborts.
 */
export type AiRewriteFunction = (
  request: AiRewriteRequest,
  signal: AbortSignal,
) => Promise<string> | string;

/** A text field, or a text with a selection range, to rewrite. */
export type AiRewriteTarget =
  | Pick<HTMLTextAreaElement | HTMLInputElement, 'value' | 'selectionStart' | 'selectionEnd'>
  | { text: string; start: number; end: number };

/** A proposed rewrite of a selection, ready for AiInlineDiff or AiEditProposal. */
export interface AiRewriteProposal {
  /** Unique within the hook; also the id of `hunk` and `part`. */
  id: string;
  /** What the person asked for. */
  instruction: string;
  /** The whole text the proposal was made for. */
  text: string;
  /** Where the rewritten range starts in `text`. */
  start: number;
  /** Where the rewritten range ends in `text`. */
  end: number;
  /** The selected text as it is now. */
  before: string;
  /** The text the model proposes in its place. */
  after: string;
  /** The whole text with the proposal applied. */
  result: string;
  /** Word-level diff of `before` and `after`: pass to `AiInlineDiff` as `segments`. */
  segments: AiDiffSegment[];
  /** The change as one hunk: pass `[hunk]` to `AiEditProposal` as `hunks`. */
  hunk: AiEditHunk;
  /** An edit-proposal part in the `proposed` state: pass to `AiEditProposal` as `part`. */
  part: AiEditProposalPart;
}

/**
 * Where a rewrite is: `idle` (nothing asked), `pending` (request in flight),
 * `ready` (a proposal is waiting for a decision) or `error`.
 */
export type AiRewriteStatus = 'idle' | 'pending' | 'ready' | 'error';

export interface UseAiRewriteOptions {
  /** Asks the model to rewrite the selection. */
  rewrite: AiRewriteFunction;
  /** Names what is being edited in the proposal's `part.target`, e.g. a document id. Default `selection`. */
  target?: string | undefined;
  /** Called when `rewrite` throws or rejects (not when it is aborted). */
  onError?: ((error: unknown) => void) | undefined;
}

export interface UseAiRewriteResult {
  /** Where the rewrite is. */
  status: AiRewriteStatus;
  /** The proposal waiting for a decision, or null. */
  proposal: AiRewriteProposal | null;
  /** What the last failed request threw, or null. */
  error: unknown;
  /**
   * Sends the target's current selection (the whole text when nothing is
   * selected) with `instruction`. A newer call aborts the one in flight.
   * Resolves with the proposal, or null when it was aborted, failed, or there
   * was nothing to rewrite.
   */
  rewriteSelection: (
    target: AiRewriteTarget,
    instruction: string,
  ) => Promise<AiRewriteProposal | null>;
  /** Accepts the proposal and clears it. Returns the whole text with it applied, or null. */
  accept: () => string | null;
  /** Drops the proposal, or aborts the request in flight. */
  reject: () => void;
}

/**
 * Applies a proposal to a text: replaces the proposal's range with its `after`
 * text. Returns the text unchanged when the range no longer holds the text the
 * proposal was made for (the person kept typing).
 */
export function applyAiRewrite(
  text: string,
  proposal: Pick<AiRewriteProposal, 'start' | 'end' | 'before' | 'after'>,
): string {
  if (text.slice(proposal.start, proposal.end) !== proposal.before) return text;
  return text.slice(0, proposal.start) + proposal.after + text.slice(proposal.end);
}

function readTarget(target: AiRewriteTarget): { text: string; start: number; end: number } {
  const text = 'value' in target ? target.value : target.text;
  const rawStart = 'value' in target ? target.selectionStart : target.start;
  const rawEnd = 'value' in target ? target.selectionEnd : target.end;
  const clamp = (n: number | null) => Math.min(Math.max(0, Math.trunc(n ?? 0) || 0), text.length);
  const a = clamp(rawStart);
  const b = clamp(rawEnd);
  const start = Math.min(a, b);
  const end = Math.max(a, b);
  // No selection: the instruction is about the whole text.
  return start === end ? { text, start: 0, end: text.length } : { text, start, end };
}

interface State {
  status: AiRewriteStatus;
  proposal: AiRewriteProposal | null;
  error: unknown;
}

const IDLE: State = { status: 'idle', proposal: null, error: null };

/**
 * Rewrites the current selection with an instruction: sends both to a
 * caller-supplied function and returns a proposal that `AiInlineDiff`
 * (`proposal.segments`) or `AiEditProposal` (`proposal.part`, `[proposal.hunk]`)
 * can show. Nothing is changed until the app applies `accept()`'s result.
 */
export function useAiRewrite(options: UseAiRewriteOptions): UseAiRewriteResult {
  const latest = useRef(options);
  latest.current = options;
  const [state, setState] = useState<State>(IDLE);
  const current = useRef<State>(IDLE);
  const commit = useCallback((next: State) => {
    current.current = next;
    setState(next);
  }, []);
  const controller = useRef<AbortController | null>(null);
  const count = useRef(0);

  const abort = useCallback(() => {
    controller.current?.abort();
    controller.current = null;
  }, []);
  useEffect(() => abort, [abort]);

  const rewriteSelection = useCallback(
    async (target: AiRewriteTarget, instruction: string): Promise<AiRewriteProposal | null> => {
      abort();
      const { text, start, end } = readTarget(target);
      const before = text.slice(start, end);
      if (!before.trim() || !instruction.trim()) {
        commit(IDLE);
        return null;
      }
      const mine = new AbortController();
      controller.current = mine;
      commit({ status: 'pending', proposal: null, error: null });
      try {
        const after = await latest.current.rewrite(
          { text, selection: before, start, end, instruction },
          mine.signal,
        );
        if (mine.signal.aborted) return null;
        controller.current = null;
        if (typeof after !== 'string') throw new TypeError('rewrite must resolve with a string');
        count.current += 1;
        const id = `rewrite-${count.current}`;
        const proposal: AiRewriteProposal = {
          id,
          instruction,
          text,
          start,
          end,
          before,
          after,
          result: text.slice(0, start) + after + text.slice(end),
          segments: diffWords(before, after),
          hunk: { id, before, after, label: instruction },
          part: {
            type: 'edit-proposal',
            id,
            target: latest.current.target ?? 'selection',
            diff: { start, end, before, after },
            state: 'proposed',
          },
        };
        commit({ status: 'ready', proposal, error: null });
        return proposal;
      } catch (error) {
        if (mine.signal.aborted) return null;
        controller.current = null;
        commit({ status: 'error', proposal: null, error });
        latest.current.onError?.(error);
        return null;
      }
    },
    [abort, commit],
  );

  const accept = useCallback(() => {
    const { proposal } = current.current;
    if (!proposal) return null;
    commit(IDLE);
    return proposal.result;
  }, [commit]);

  const reject = useCallback(() => {
    abort();
    if (current.current !== IDLE) commit(IDLE);
  }, [abort, commit]);

  return useMemo(
    () => ({ ...state, rewriteSelection, accept, reject }),
    [state, rewriteSelection, accept, reject],
  );
}
