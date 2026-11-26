'use client';

import { useCallback, useMemo, useRef, type CSSProperties, type ReactNode } from 'react';
import { getErrorMessage, useToast } from '../toast/index.js';

/** How an undoable action ended: the person undid it, or the window closed and it stands. */
export type UndoOutcome = 'undone' | 'committed';

/** Default strings of `useUndoToast`, for translation. */
export interface UndoToastMessages {
  /** The action button. Default "Undo". */
  undo: string;
  /** The confirmation shown after undoing. Default "Restored". */
  restored: ReactNode;
  /** Shown when `onUndo` fails without a readable message. Default "Could not undo. Try again.". */
  undoFailed: string;
}

const DEFAULT_MESSAGES: UndoToastMessages = {
  undo: 'Undo',
  restored: 'Restored',
  undoFailed: 'Could not undo. Try again.',
};

export interface UndoableOptions {
  /** What just happened, in the past tense: "Note deleted". */
  title: ReactNode;
  /** One line of detail, e.g. the item's name. */
  description?: ReactNode;
  /** Label of the action button. Default `messages.undo` ("Undo"). */
  undoLabel?: string;
  /**
   * What a screen reader hears instead of the button, naming another way to
   * undo ("Restore it from Trash"). Defaults to the label.
   */
  undoAltText?: string;
  /**
   * Milliseconds the Undo button stays available. Default 8000, kept between
   * 5000 and 10000; `Infinity` keeps the toast until it is dismissed. The
   * provider's `durationScale` still applies.
   */
  duration?: number;
  /** Reverts the action the app already applied. Called at most once, only when the person presses Undo. */
  onUndo: () => unknown;
  /**
   * Called at most once when the Undo window closes without an undo
   * (timeout, Dismiss, Escape, swipe, a newer toast pushing it out, or the
   * page being hidden). Use it for deferred server deletes.
   */
  onCommit?: () => unknown;
  /**
   * The confirmation toast after undoing. Default `messages.restored`
   * ("Restored"); `false` shows none.
   */
  restoredTitle?: ReactNode | false;
  /**
   * Show a thin bar that shrinks over `duration`. It pauses with the toast
   * while hovered or focused, but does not follow the provider's
   * `durationScale`, so leave it off when that is changed. Default `false`.
   */
  countdown?: boolean;
}

export interface UseUndoToastOptions {
  /** Replaces any of the default strings. */
  messages?: Partial<UndoToastMessages>;
}

export interface UseUndoToastResult {
  /**
   * Shows "<title>. Undo" for an action the app has already applied. Resolves
   * `'undone'` after `onUndo` has run, or `'committed'` after `onCommit` has
   * run. Rejects if `onUndo` fails, after showing an error toast.
   */
  undoable: (options: UndoableOptions) => Promise<UndoOutcome>;
}

const DEFAULT_DURATION = 8000;
const MIN_DURATION = 5000;
const MAX_DURATION = 10000;
const RESTORED_DURATION = 4000;

/** The Undo window in milliseconds: 8s by default, 5–10s, or Infinity. */
function undoDuration(duration: number | undefined): number {
  if (duration === Infinity) return Infinity;
  if (duration === undefined || !Number.isFinite(duration)) return DEFAULT_DURATION;
  return Math.min(MAX_DURATION, Math.max(MIN_DURATION, Math.round(duration)));
}

/** Calls `fn` now, turning its result or a synchronous throw into a promise. */
function run(fn: (() => unknown) | undefined): Promise<unknown> {
  try {
    return Promise.resolve(fn?.());
  } catch (error) {
    return Promise.reject(error);
  }
}

/**
 * The "Note deleted. Undo" pattern for reversible, low-stakes deletes: the
 * app applies the change at once, and this shows a toast with an Undo button
 * for a few seconds. Undo calls `onUndo` and confirms with a short "Restored"
 * toast; letting the toast close calls `onCommit`, where a deferred server
 * delete can run. Each callback fires at most once. Needs `ToastProvider`.
 */
export function useUndoToast(options: UseUndoToastOptions = {}): UseUndoToastResult {
  const { toast } = useToast();
  const messagesRef = useRef<UndoToastMessages>(DEFAULT_MESSAGES);
  messagesRef.current = { ...DEFAULT_MESSAGES, ...options.messages };

  const undoable = useCallback(
    (opts: UndoableOptions) =>
      new Promise<UndoOutcome>((resolve, reject) => {
        const messages = messagesRef.current;
        const duration = undoDuration(opts.duration);
        let settled = false;
        let undoRequested = false;

        const onPageHide = () => commit();
        const finish = () => {
          settled = true;
          if (typeof window !== 'undefined') window.removeEventListener('pagehide', onPageHide);
        };

        function commit() {
          if (settled || undoRequested) return;
          finish();
          handle.dismiss();
          run(opts.onCommit).then(
            () => resolve('committed'),
            (error: unknown) => reject(error),
          );
        }

        const undo = () => {
          if (settled || undoRequested) return;
          undoRequested = true;
          finish();
          run(opts.onUndo).then(
            () => {
              const restored =
                opts.restoredTitle === undefined ? messages.restored : opts.restoredTitle;
              if (restored !== false && restored !== null) {
                toast({
                  title: restored,
                  tone: 'success',
                  politeness: 'polite',
                  duration: RESTORED_DURATION,
                });
              }
              resolve('undone');
            },
            (error: unknown) => {
              toast.error(getErrorMessage(error, { fallback: messages.undoFailed }));
              reject(error);
            },
          );
        };

        const handle = toast({
          title: opts.countdown ? (
            <span
              className="mn-undo-toast"
              data-countdown={Number.isFinite(duration) ? '' : undefined}
              style={
                Number.isFinite(duration)
                  ? ({ '--_duration': `${duration}ms` } as CSSProperties)
                  : undefined
              }
            >
              {opts.title}
              {Number.isFinite(duration) ? (
                <span className="mn-undo-toast__countdown" aria-hidden />
              ) : null}
            </span>
          ) : (
            opts.title
          ),
          description: opts.description,
          tone: 'neutral',
          duration,
          action: {
            label: opts.undoLabel ?? messages.undo,
            ...(opts.undoAltText ? { altText: opts.undoAltText } : {}),
            onClick: undo,
          },
          // Runs once however the toast closed; the action's click runs first.
          onDismiss: () => commit(),
        });

        if (typeof window !== 'undefined') window.addEventListener('pagehide', onPageHide);
      }),
    [toast],
  );

  return useMemo(() => ({ undoable }), [undoable]);
}
