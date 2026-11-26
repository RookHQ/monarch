'use client';

import { forwardRef, useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  SaveStatusIndicator,
  type SaveStatus,
  type SaveStatusIndicatorProps,
} from '../../feedback/save-status-indicator/index.js';

export interface UseAutosaveOptions<T> {
  /** The current value (form values, document content). Compared with the last saved value. */
  value: T;
  /**
   * Saves a value. Return a promise: while it is pending the status is
   * `saving`; a rejection is retried `retries` times, then the status becomes
   * `error`. Only one save runs at a time; edits made during a save are saved
   * right after it.
   */
  onSave: (value: T) => unknown;
  /** Milliseconds to wait after the last change before saving. Default 1000. */
  delay?: number;
  /** Pause autosaving (for example while the form is invalid). Changes still mark the value unsaved. Default `true`. */
  enabled?: boolean;
  /**
   * Decides whether two values are the same. Default: `Object.is`, then
   * `JSON.stringify` equality, so a re-created but identical object is not
   * saved again.
   */
  isEqual?: ((a: T, b: T) => boolean) | undefined;
  /** Automatic retries after a failed save, before the status becomes `error`. Default 2. */
  retries?: number;
  /** Milliseconds before the first automatic retry; doubles each time. Default 2000. */
  retryDelay?: number;
  /** When the initial value was last saved, if known. Shown as "Saved · 3 minutes ago". */
  initialSavedAt?: Date | null;
}

export interface UseAutosaveResult {
  /** Where saving stands; pass it to SaveStatusIndicator or use AutosaveIndicator. */
  status: SaveStatus;
  /** When the last successful save finished, or `initialSavedAt`, or `null`. */
  savedAt: Date | null;
  /** The last save error while `status` is `error`, else `null`. */
  error: unknown;
  /** True while there are changes that are not saved yet (unsaved, saving or failed). Use it for UnsavedChangesGuard `when`. */
  isDirty: boolean;
  /** Saves now after a failure, resetting the automatic retries. */
  retry: () => void;
  /**
   * Saves pending changes now, skipping the delay (on blur, before leaving).
   * Resolves `true` when everything is saved and `false` when the save failed.
   */
  flush: () => Promise<boolean>;
}

function defaultIsEqual(a: unknown, b: unknown): boolean {
  if (Object.is(a, b)) return true;
  if (typeof a !== 'object' || typeof b !== 'object' || a === null || b === null) return false;
  try {
    return JSON.stringify(a) === JSON.stringify(b);
  } catch {
    return false;
  }
}

/**
 * Saves a value automatically after the person stops changing it: debounced,
 * one save at a time, retried with back-off on failure, and flushable on
 * demand. Pair it with AutosaveIndicator to show the status.
 *
 * `const autosave = useAutosave({ value: draft, onSave: (v) => api.save(v) })`
 */
export function useAutosave<T>(options: UseAutosaveOptions<T>): UseAutosaveResult {
  const {
    value,
    delay = 1000,
    enabled = true,
    retries = 2,
    retryDelay = 2000,
    initialSavedAt = null,
  } = options;
  const [status, setStatus] = useState<SaveStatus>(initialSavedAt ? 'saved' : 'idle');
  const [savedAt, setSavedAt] = useState<Date | null>(initialSavedAt);
  const [error, setError] = useState<unknown>(null);

  const opts = useRef(options);
  opts.current = options;
  const equal = useCallback((a: T, b: T) => (opts.current.isEqual ?? defaultIsEqual)(a, b), []);
  const latest = useRef(value);
  latest.current = value;
  const lastSaved = useRef(value);
  // The value the debounce timer was last armed for, so re-renders with the
  // same content do not keep pushing the save back.
  const armedFor = useRef<{ value: T } | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const inFlight = useRef<Promise<boolean> | null>(null);
  const attempt = useRef(0);
  const mounted = useRef(true);
  const config = useRef({ delay, retries, retryDelay, enabled });
  config.current = { delay, retries, retryDelay, enabled };

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      clearTimeout(timer.current);
    };
  }, []);

  const run = useCallback(async (): Promise<boolean> => {
    clearTimeout(timer.current);
    if (inFlight.current) {
      await inFlight.current;
      return run();
    }
    const snapshot = latest.current;
    // Marks this value as handled, so re-renders do not schedule it again.
    armedFor.current = { value: snapshot };
    if (equal(snapshot, lastSaved.current)) return true;
    if (mounted.current) setStatus('saving');
    const attemptSave = (async () => {
      try {
        await opts.current.onSave(snapshot);
      } catch (raw) {
        return { ok: false as const, raw };
      }
      return { ok: true as const };
    })();
    const done = attemptSave.then((result) => {
      inFlight.current = null;
      if (!mounted.current) return result.ok;
      if (result.ok) {
        lastSaved.current = snapshot;
        attempt.current = 0;
        setSavedAt(new Date());
        setError(null);
        if (equal(latest.current, snapshot)) {
          setStatus('saved');
        } else {
          // Edited during the save: save the newer value after the usual delay.
          setStatus('unsaved');
          armedFor.current = { value: latest.current };
          timer.current = setTimeout(() => void run(), config.current.delay);
        }
        return true;
      }
      attempt.current += 1;
      if (attempt.current <= config.current.retries) {
        const wait = config.current.retryDelay * 2 ** (attempt.current - 1);
        timer.current = setTimeout(() => void run(), wait);
        return false;
      }
      setError(result.raw);
      setStatus('error');
      return false;
    });
    inFlight.current = done;
    return done;
  }, [equal]);

  // Runs after every render on purpose: values are compared by content, not
  // identity, and `armedFor` stops the same content from re-arming the timer.
  // Every setStatus call below is a no-op once the status matches.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    if (equal(value, lastSaved.current)) {
      // Changed back to what is saved: nothing to do.
      if (armedFor.current) {
        clearTimeout(timer.current);
        armedFor.current = null;
      }
      if (!inFlight.current) setStatus((s) => (s === 'unsaved' ? (savedAt ? 'saved' : 'idle') : s));
      return;
    }
    if (!enabled) {
      clearTimeout(timer.current);
      armedFor.current = null;
      if (!inFlight.current) setStatus((s) => (s === 'error' ? s : 'unsaved'));
      return;
    }
    if (armedFor.current && equal(value, armedFor.current.value)) return;
    armedFor.current = { value };
    attempt.current = 0;
    if (!inFlight.current) setStatus((s) => (s === 'error' ? s : 'unsaved'));
    clearTimeout(timer.current);
    timer.current = setTimeout(() => void run(), delay);
  });

  const retry = useCallback(() => {
    attempt.current = 0;
    setError(null);
    void run();
  }, [run]);

  const flush = useCallback(() => {
    attempt.current = 0;
    return run();
  }, [run]);

  return {
    status,
    savedAt,
    error: status === 'error' ? error : null,
    isDirty: status === 'unsaved' || status === 'saving' || status === 'error',
    retry,
    flush,
  };
}

export interface AutosaveIndicatorProps extends Omit<
  SaveStatusIndicatorProps,
  'status' | 'savedAt' | 'onRetry' | 'error'
> {
  /** The result of `useAutosave`. */
  state: UseAutosaveResult;
  /**
   * Turns the save error into the visible message. Default: the
   * indicator's "Couldn't save".
   */
  errorMessage?: ((error: unknown) => ReactNode) | undefined;
}

/**
 * Shows the state of a `useAutosave` hook: "Unsaved changes", "Saving…",
 * "Saved · 2 minutes ago", or "Couldn't save" with a Retry button that
 * saves again. Announces the outcome politely.
 */
export const AutosaveIndicator = forwardRef<HTMLDivElement, AutosaveIndicatorProps>(
  function AutosaveIndicator({ state, errorMessage, className, ...rest }, ref) {
    const error = state.status === 'error' && errorMessage ? errorMessage(state.error) : undefined;
    return (
      <SaveStatusIndicator
        ref={ref}
        {...rest}
        status={state.status}
        savedAt={state.savedAt}
        onRetry={state.retry}
        {...(error !== undefined ? { error } : {})}
        className={cx('mn-autosave-indicator', className)}
      />
    );
  },
);
