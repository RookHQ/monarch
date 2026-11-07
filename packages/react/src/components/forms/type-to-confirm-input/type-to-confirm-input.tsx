'use client';

import { forwardRef, useEffect, useRef, useState, type CSSProperties, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Field, FieldDescription, FieldError, FieldLabel } from '../form-field/index.js';
import { Input, type InputProps } from '../text-field/index.js';

/**
 * Whether `value` matches the confirmation `phrase`: case-sensitive, with
 * surrounding whitespace ignored on both sides. The same rule ConfirmDialog
 * uses for `confirmPhrase`.
 */
export function phraseMatches(value: string, phrase: string): boolean {
  return value.trim() === phrase.trim();
}

export interface TypeToConfirmInputProps extends Omit<
  InputProps,
  'value' | 'defaultValue' | 'type' | 'invalid' | 'style'
> {
  /** The exact text the person must type, usually the record's own name ("acme-site"). */
  phrase: string;
  /** The typed text (controlled). */
  value?: string;
  /** Initial text when uncontrolled. Default empty. */
  defaultValue?: string;
  /** Called with the raw typed text on every change. */
  onValueChange?: (value: string) => void;
  /** Called each time the match state flips (not on the first render). */
  onMatchChange?: (matches: boolean) => void;
  /** Visible label. Default: Type <phrase> to confirm, with the phrase highlighted in monospace. */
  label?: ReactNode;
  /** Help text under the label, e.g. what is about to be deleted. */
  description?: ReactNode;
  /** Error shown while `showMismatch` is set and the text does not match. Default "The text does not match." */
  mismatchMessage?: ReactNode;
  /**
   * Show the mismatch error and mark the field invalid when the text does not
   * match. The parent decides when, typically after a submit attempt; it is
   * never shown while the person is still typing unless you set it.
   */
  showMismatch?: boolean;
  /** Announced politely when the text starts matching. Default "Text matches". Pass "" for silence. */
  matchAnnouncement?: string;
  /** Announced politely when the text stops matching. Default "" (nothing). */
  mismatchAnnouncement?: string;
  /** Class for the root (the Field wrapper, `.mn-type-to-confirm-input`); the ref goes to the input. */
  className?: string;
  /** Inline style for the root. */
  style?: CSSProperties;
  /** Class for the `<input>`. */
  inputClassName?: string;
}

/**
 * The "type the name to confirm" field for irreversible actions. Wraps Input
 * in a Field so the label, description and mismatch error are wired up, turns
 * autocomplete, autocorrect and spellcheck off, and announces politely when
 * the typed text matches. Matching is case-sensitive and ignores surrounding
 * spaces (see `phraseMatches`). Paste is allowed.
 */
export const TypeToConfirmInput = forwardRef<HTMLInputElement, TypeToConfirmInputProps>(
  function TypeToConfirmInput(
    {
      phrase,
      value: valueProp,
      defaultValue = '',
      onValueChange,
      onMatchChange,
      label,
      description,
      mismatchMessage = 'The text does not match.',
      showMismatch = false,
      matchAnnouncement = 'Text matches',
      mismatchAnnouncement = '',
      className,
      style,
      inputClassName,
      id,
      disabled,
      required,
      onChange,
      ...rest
    },
    ref,
  ) {
    const [internal, setInternal] = useState(defaultValue);
    const value = valueProp ?? internal;
    const matches = phraseMatches(value, phrase);
    const invalid = showMismatch && !matches;
    const [announcement, setAnnouncement] = useState('');

    // Announce and report only real flips, never the initial state.
    const previous = useRef(matches);
    const onMatchChangeRef = useRef(onMatchChange);
    onMatchChangeRef.current = onMatchChange;
    useEffect(() => {
      if (previous.current === matches) return;
      previous.current = matches;
      setAnnouncement(matches ? matchAnnouncement : mismatchAnnouncement);
      onMatchChangeRef.current?.(matches);
    }, [matches, matchAnnouncement, mismatchAnnouncement]);

    return (
      <Field
        {...(id !== undefined ? { id } : {})}
        invalid={invalid}
        disabled={disabled ?? false}
        required={required ?? false}
        className={cx('mn-type-to-confirm-input', className)}
        style={style}
        data-matches={matches || undefined}
      >
        <FieldLabel>
          {label ?? (
            <>
              Type <span className="mn-type-to-confirm-input__phrase">{phrase}</span> to confirm
            </>
          )}
        </FieldLabel>
        {description ? <FieldDescription>{description}</FieldDescription> : null}
        <Input
          ref={ref}
          {...rest}
          value={value}
          onChange={(event) => {
            if (valueProp === undefined) setInternal(event.target.value);
            onValueChange?.(event.target.value);
            onChange?.(event);
          }}
          autoComplete="off"
          autoCapitalize="off"
          autoCorrect="off"
          spellCheck={false}
          className={cx('mn-type-to-confirm-input__control', inputClassName)}
        />
        <FieldError>{invalid ? mismatchMessage : null}</FieldError>
        <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
          {announcement}
        </span>
      </Field>
    );
  },
);
