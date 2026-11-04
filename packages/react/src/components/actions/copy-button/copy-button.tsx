'use client';

import { forwardRef, useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, CopyGlyph, WarningGlyph } from '../../../internal/icons.js';
import { Button, type ButtonProps } from '../button/index.js';

export type CopyStatus = 'idle' | 'copied' | 'error';

/** What to copy: a string, or a function that returns one (sync or async). */
export type CopyValue = string | (() => string | Promise<string>);

export interface UseCopyToClipboardOptions {
  /** Milliseconds before `copied` or `error` returns to `idle`. `0` keeps the state. Default 2000. */
  resetAfter?: number;
  /** Called with the copied text after a successful copy. */
  onCopy?: (text: string) => void;
  /** Called when the clipboard is unavailable or the browser refuses the write. */
  onError?: (error: Error) => void;
}

export interface UseCopyToClipboardResult {
  /** Copies `value`. Resolves `true` on success; never rejects. */
  copy: (value: CopyValue) => Promise<boolean>;
  status: CopyStatus;
  /** The last error, or `null`. */
  error: Error | null;
  /** Returns to `idle` immediately. */
  reset: () => void;
}

/**
 * Fallback for browsers or contexts without the async Clipboard API (for
 * example an insecure origin). Uses a temporary off-screen textarea and
 * restores the previous focus and selection.
 */
function legacyCopy(text: string): boolean {
  if (typeof document === 'undefined' || typeof document.execCommand !== 'function') return false;
  const active = document.activeElement as HTMLElement | null;
  const selection = document.getSelection();
  const range = selection && selection.rangeCount > 0 ? selection.getRangeAt(0) : null;
  const area = document.createElement('textarea');
  area.value = text;
  area.setAttribute('readonly', '');
  area.setAttribute('aria-hidden', 'true');
  area.style.position = 'fixed';
  area.style.top = '0';
  area.style.left = '-9999px';
  area.style.opacity = '0';
  document.body.appendChild(area);
  area.select();
  let ok = false;
  try {
    ok = document.execCommand('copy');
  } catch {
    ok = false;
  }
  area.remove();
  if (range && selection) {
    selection.removeAllRanges();
    selection.addRange(range);
  }
  active?.focus?.({ preventScroll: true });
  return ok;
}

/** Writes text to the clipboard: the async Clipboard API first, then the legacy fallback. */
export async function writeToClipboard(text: string): Promise<void> {
  const clipboard = typeof navigator !== 'undefined' ? navigator.clipboard : undefined;
  if (clipboard?.writeText) {
    try {
      await clipboard.writeText(text);
      return;
    } catch (error) {
      if (legacyCopy(text)) return;
      throw error instanceof Error ? error : new Error(String(error));
    }
  }
  if (legacyCopy(text)) return;
  throw new Error('Clipboard is not available in this browser.');
}

/**
 * Copies text and tracks the result. The state machine is
 * `idle → copied | error → idle` (after `resetAfter`). It never throws:
 * failures go to `onError` and `status: 'error'`, so the caller decides how
 * to tell the user (inline, a toast, a manual-copy fallback).
 */
export function useCopyToClipboard(
  options: UseCopyToClipboardOptions = {},
): UseCopyToClipboardResult {
  const { resetAfter = 2000 } = options;
  const [status, setStatus] = useState<CopyStatus>('idle');
  const [error, setError] = useState<Error | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const mounted = useRef(true);
  const callbacks = useRef(options);
  callbacks.current = options;

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      clearTimeout(timer.current);
    };
  }, []);

  const reset = useCallback(() => {
    clearTimeout(timer.current);
    setStatus('idle');
    setError(null);
  }, []);

  const copy = useCallback(
    async (value: CopyValue) => {
      clearTimeout(timer.current);
      let next: CopyStatus;
      try {
        const text = typeof value === 'function' ? await value() : value;
        await writeToClipboard(text);
        if (!mounted.current) return true;
        setError(null);
        next = 'copied';
        callbacks.current.onCopy?.(text);
      } catch (raw) {
        const err = raw instanceof Error ? raw : new Error(String(raw));
        if (!mounted.current) return false;
        setError(err);
        next = 'error';
        callbacks.current.onError?.(err);
      }
      setStatus(next);
      if (resetAfter > 0) {
        timer.current = setTimeout(() => {
          if (mounted.current) setStatus('idle');
        }, resetAfter);
      }
      return next === 'copied';
    },
    [resetAfter],
  );

  return { copy, status, error, reset };
}

export interface CopyButtonProps extends Omit<
  ButtonProps,
  'children' | 'value' | 'onError' | 'onCopy' | 'leadingIcon' | 'asChild'
> {
  /** The text to copy, or a function that produces it when clicked. */
  value: CopyValue;
  /** Visible label, and the accessible name. Default "Copy". */
  label?: string;
  /** Label while in the copied state. Default "Copied". */
  copiedLabel?: string;
  /** Label after a failed copy. Default "Couldn't copy". */
  errorLabel?: string;
  /**
   * What is copied, for screen readers ("webhook URL"). Appended to the
   * visible label as visually hidden text, so several copy buttons on one page
   * have distinct names: "Copy webhook URL", "Copied webhook URL".
   */
  target?: string;
  /**
   * Hide the visible text and show only the icon. `label` stays the accessible
   * name, so give it the object: "Copy API key".
   */
  iconOnly?: boolean;
  /** Polite announcement after a successful copy. Default "Copied to clipboard". */
  copiedMessage?: string;
  /** Polite announcement after a failure. Default "Couldn't copy. Select the text and copy it manually." */
  errorMessage?: string;
  /** Replaces the copy icon. Decorative. */
  icon?: ReactNode;
  /** Milliseconds before the button returns to its idle look. Default 2000. */
  resetAfter?: number;
  /** Called with the copied text after a successful copy. */
  onCopy?: (text: string) => void;
  /** Called when copying fails. Use it to show a toast or a manual-copy fallback. */
  onCopyError?: (error: Error) => void;
}

/**
 * Copies a value to the clipboard, shows a short "Copied" state and announces
 * the result through a polite live region. Errors are reported to the caller
 * through `onCopyError`; the button never fails silently.
 */
export const CopyButton = forwardRef<HTMLButtonElement, CopyButtonProps>(function CopyButton(
  {
    value,
    label = 'Copy',
    copiedLabel = 'Copied',
    errorLabel = "Couldn't copy",
    iconOnly = false,
    target,
    copiedMessage = 'Copied to clipboard',
    errorMessage = "Couldn't copy. Select the text and copy it manually.",
    icon,
    resetAfter = 2000,
    onCopy,
    onCopyError,
    onClick,
    variant = iconOnly ? 'ghost' : 'secondary',
    className,
    ...rest
  },
  ref,
) {
  const { copy, status } = useCopyToClipboard(
    onCopy || onCopyError
      ? {
          resetAfter,
          ...(onCopy ? { onCopy } : {}),
          ...(onCopyError ? { onError: onCopyError } : {}),
        }
      : { resetAfter },
  );
  // The live region keeps its text until the next attempt, then is cleared and
  // refilled so repeated copies are announced each time.
  const [message, setMessage] = useState('');
  const glyph =
    status === 'copied' ? (
      <CheckGlyph />
    ) : status === 'error' ? (
      <WarningGlyph />
    ) : (
      (icon ?? <CopyGlyph />)
    );

  return (
    <>
      <Button
        ref={ref}
        {...rest}
        variant={variant}
        aria-label={iconOnly ? (target ? `${label} ${target}` : label) : undefined}
        title={rest.title ?? (iconOnly ? (target ? `${label} ${target}` : label) : undefined)}
        data-state={status}
        data-icon-only={iconOnly || undefined}
        className={cx('mn-copy-button', className)}
        onClick={(event) => {
          onClick?.(event);
          if (event.defaultPrevented) return;
          setMessage('');
          void copy(value).then((ok) => setMessage(ok ? copiedMessage : errorMessage));
        }}
      >
        <span className="mn-copy-button__icon" aria-hidden>
          {glyph}
        </span>
        {iconOnly ? null : (
          // The idle and copied labels share one grid cell, so the button keeps
          // the width of the longer one and does not jump on success. The rarer
          // error label replaces them and may widen the button.
          <span className="mn-copy-button__label">
            {status === 'error' ? (
              <span data-active="">{errorLabel}</span>
            ) : (
              (
                [
                  ['idle', label],
                  ['copied', copiedLabel],
                ] as const
              ).map(([key, text]) => (
                <span
                  key={key}
                  data-active={key === status || undefined}
                  aria-hidden={key === status ? undefined : true}
                >
                  {text}
                </span>
              ))
            )}
          </span>
        )}
        {!iconOnly && target ? <span className="mn-visually-hidden"> {target}</span> : null}
      </Button>
      <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
        {message}
      </span>
    </>
  );
});
