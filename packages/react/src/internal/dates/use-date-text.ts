import { useCallback, useEffect, useRef, useState } from 'react';
import type { IsoDate } from './iso.js';

/** Why a date cannot be used: unreadable text, outside `min`/`max`, or blocked by `isDateDisabled`. */
export type DateValidationError = 'invalid' | 'before-min' | 'after-max' | 'unavailable';

export interface DateConstraints {
  min?: IsoDate | undefined;
  max?: IsoDate | undefined;
  isDateDisabled?: ((date: IsoDate) => boolean) | undefined;
}

/** Checks a date against bounds and a disabled-date callback. */
export function validateIsoDate(
  iso: IsoDate,
  { min, max, isDateDisabled }: DateConstraints,
): DateValidationError | null {
  if (min && iso < min) return 'before-min';
  if (max && iso > max) return 'after-max';
  if (isDateDisabled?.(iso)) return 'unavailable';
  return null;
}

export interface UseDateTextOptions {
  /** The committed value. */
  value: IsoDate | null;
  format: (iso: IsoDate) => string;
  parse: (text: string) => IsoDate | null;
  validate: (iso: IsoDate) => DateValidationError | null;
  /** Called when typing or picking produces a new valid value (or `null` when cleared). */
  onCommit: (iso: IsoDate | null) => void;
}

/**
 * The typed-text half of a date field: keeps the text the user is editing,
 * re-formats it from the value when the value changes, and on `commit()`
 * parses and validates it. Invalid text is kept (so the user can fix it) and
 * reported as `error`; the value is left unchanged.
 */
export function useDateText({ value, format, parse, validate, onCommit }: UseDateTextOptions) {
  const shown = value ? format(value) : '';
  const [text, setText] = useState(shown);
  const [typedError, setTypedError] = useState<DateValidationError | null>(null);
  const editing = useRef(false);

  useEffect(() => {
    if (editing.current) return;
    setText(shown);
    setTypedError(null);
  }, [shown]);

  const onTextChange = useCallback((next: string) => {
    editing.current = true;
    setText(next);
  }, []);

  /** Parses the text. Returns the date, `null` for empty text, or `undefined` when it is not usable. */
  const commit = (): IsoDate | null | undefined => {
    editing.current = false;
    const t = text.trim();
    if (!t) {
      setTypedError(null);
      setText('');
      if (value !== null) onCommit(null);
      return null;
    }
    const iso = parse(t);
    const error = iso ? validate(iso) : 'invalid';
    if (!iso || error) {
      setTypedError(error);
      return undefined;
    }
    setTypedError(null);
    setText(format(iso));
    if (iso !== value) onCommit(iso);
    return iso;
  };

  /** Sets a value from outside the text (calendar, preset, clear button). */
  const set = (iso: IsoDate | null) => {
    editing.current = false;
    setTypedError(null);
    setText(iso ? format(iso) : '');
    if (iso !== value) onCommit(iso);
  };

  /** Shows a date in the text without committing it (the caller commits the value itself). */
  const show = (iso: IsoDate | null) => {
    editing.current = false;
    setTypedError(null);
    setText(iso ? format(iso) : '');
  };

  const valueError = value ? validate(value) : null;
  return { text, onTextChange, commit, set, show, error: typedError ?? valueError };
}
