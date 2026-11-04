'use client';

import {
  forwardRef,
  useState,
  type ClipboardEvent,
  type CSSProperties,
  type InputHTMLAttributes,
  type ReactNode,
  type SyntheticEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { useFieldControl } from '../form-field/index.js';

export interface OTPPinInputProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'value' | 'defaultValue' | 'onChange' | 'size' | 'type' | 'maxLength' | 'minLength' | 'pattern'
> {
  /** Number of characters. Default 6. */
  length?: number;
  /** The code (controlled). */
  value?: string;
  /** Initial code when uncontrolled. */
  defaultValue?: string;
  /** Called with the cleaned code on every change. */
  onValueChange?: (value: string) => void;
  /** Called once each time the code reaches `length` characters (typed, pasted or autofilled). */
  onComplete?: (value: string) => void;
  /** `numeric` (default) accepts digits and opens the number keypad; `alphanumeric` accepts A–Z and 0–9, upper-cased. */
  mode?: 'numeric' | 'alphanumeric';
  /** Show dots instead of the characters, for a PIN typed on a shared screen. */
  mask?: boolean;
  /** Draw a gap after every N slots, e.g. 3 for "123 456". Visual only. */
  groupSize?: number;
  size?: 'md' | 'lg';
  /** Marks the code invalid (e.g. after a failed verification). Inside a Field, the Field's `invalid` is used. */
  invalid?: boolean;
  /** Class for the outer wrapper (the real input is `.mn-otp-pin-input__input`). */
  className?: string;
}

function clean(raw: string, mode: 'numeric' | 'alphanumeric', length: number): string {
  const kept =
    mode === 'numeric' ? raw.replace(/\D/g, '') : raw.toUpperCase().replace(/[^A-Z0-9]/g, '');
  return kept.slice(0, length);
}

function caretToEnd(e: SyntheticEvent<HTMLInputElement>) {
  const input = e.currentTarget;
  const end = input.value.length;
  if (input.selectionStart !== end || input.selectionEnd !== end) input.setSelectionRange(end, end);
}

/**
 * A one-time code or PIN entry shown as separate boxes. It is one real
 * `<input>` underneath, so autofill from SMS or email (`autocomplete="one-time-code"`),
 * paste, the numeric keypad and screen readers all treat it as a single field.
 * Give it a label with Field or `aria-label`.
 */
export const OTPPinInput = forwardRef<HTMLInputElement, OTPPinInputProps>(function OTPPinInput(
  {
    length = 6,
    value: valueProp,
    defaultValue = '',
    onValueChange,
    onComplete,
    mode = 'numeric',
    mask = false,
    groupSize,
    size = 'md',
    invalid,
    className,
    onPaste,
    onFocus,
    onBlur,
    onSelect,
    onClick,
    ...rest
  },
  ref,
) {
  const [internal, setInternal] = useState(() => clean(defaultValue, mode, length));
  const value = clean(valueProp !== undefined ? valueProp : internal, mode, length);
  const [focused, setFocused] = useState(false);

  const setValue = (next: string) => {
    if (next === value) return;
    if (valueProp === undefined) setInternal(next);
    onValueChange?.(next);
    if (next.length === length) onComplete?.(next);
  };

  const handlePaste = (e: ClipboardEvent<HTMLInputElement>) => {
    onPaste?.(e);
    if (e.defaultPrevented) return;
    e.preventDefault();
    const pasted = clean(e.clipboardData.getData('text'), mode, length);
    if (!pasted) return;
    // A full code replaces what is there; a fragment is appended.
    setValue(pasted.length === length ? pasted : clean(value + pasted, mode, length));
  };

  const fieldProps = useFieldControl(rest, { invalid });
  const active = focused ? Math.min(value.length, length - 1) : -1;

  const slots: ReactNode[] = [];
  for (let i = 0; i < length; i++) {
    if (groupSize && i > 0 && i % groupSize === 0)
      slots.push(<span key={`sep-${i}`} className="mn-otp-pin-input__separator" />);
    const char = value[i];
    slots.push(
      <span
        key={i}
        className="mn-otp-pin-input__slot"
        data-filled={char !== undefined || undefined}
        data-active={i === active || undefined}
      >
        {char === undefined ? (
          i === active && value.length < length ? (
            <span className="mn-otp-pin-input__caret" />
          ) : null
        ) : mask ? (
          <span className="mn-otp-pin-input__dot" />
        ) : (
          char
        )}
      </span>,
    );
  }

  return (
    <div
      className={cx('mn-otp-pin-input', className)}
      style={
        {
          '--_n': length,
          '--_seps': groupSize ? Math.floor((length - 1) / groupSize) : 0,
        } as CSSProperties
      }
      data-size={size}
      data-focused={focused || undefined}
      data-complete={value.length === length || undefined}
      data-invalid={fieldProps['aria-invalid'] ? true : undefined}
      data-disabled={fieldProps.disabled || undefined}
    >
      <div className="mn-otp-pin-input__slots" aria-hidden>
        {slots}
      </div>
      <input
        ref={ref}
        autoComplete="one-time-code"
        autoCapitalize={mode === 'alphanumeric' ? 'characters' : 'off'}
        autoCorrect="off"
        spellCheck={false}
        {...fieldProps}
        type={mask ? 'password' : 'text'}
        inputMode={mode === 'numeric' ? 'numeric' : 'text'}
        pattern={mode === 'numeric' ? '[0-9]*' : '[A-Za-z0-9]*'}
        className="mn-otp-pin-input__input"
        data-mask={mask || undefined}
        value={value}
        onChange={(e) => setValue(clean(e.target.value, mode, length))}
        onPaste={handlePaste}
        onFocus={(e) => {
          onFocus?.(e);
          setFocused(true);
          caretToEnd(e);
        }}
        onBlur={(e) => {
          onBlur?.(e);
          setFocused(false);
        }}
        onSelect={(e) => {
          onSelect?.(e);
          caretToEnd(e);
        }}
        onClick={(e) => {
          onClick?.(e);
          caretToEnd(e);
        }}
      />
    </div>
  );
});
