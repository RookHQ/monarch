/**
 * Asks for a continuation of `text` at `cursor` (an index into `text`).
 * Resolve with the text to insert at the cursor, or with an empty string,
 * `null` or `undefined` for no suggestion. Stop work when `signal` aborts: the
 * request is stale and its answer is ignored.
 */
export type AiCompletionFunction = (
  text: string,
  cursor: number,
  signal: AbortSignal,
) => Promise<string | null | undefined> | string | null | undefined;

/**
 * Where a completion is:
 * - `idle`: nothing asked and nothing shown.
 * - `waiting`: the person is still typing; the request has not been sent.
 * - `loading`: the request is in flight.
 * - `ready`: a suggestion is showing.
 * - `error`: the last request failed.
 */
export type AiCompletionStatus = 'idle' | 'waiting' | 'loading' | 'ready' | 'error';

/** What accepting a suggestion (or one word of it) changes. */
export interface AiCompletionAcceptance {
  /** The text that was accepted and goes in at the cursor. */
  inserted: string;
  /** The whole text after the insertion. */
  text: string;
  /** The cursor after the insertion. */
  cursor: number;
}

/** How a suggestion was accepted: all of it, or its next word. */
export type AiGhostTextAcceptKind = 'all' | 'word';
