'use client';

import { forwardRef, useId, useState, type InputHTMLAttributes, type KeyboardEvent } from 'react';
import { cx } from '../../../internal/cx.js';
import { EyeGlyph, EyeOffGlyph } from '../../../internal/icons.js';
import { useFieldControl } from '../form-field/index.js';

/** Interface text, for translation. */
export interface PasswordInputMessages {
  /**
   * Accessible name of the reveal button. It stays the same in both states;
   * `aria-pressed` says whether the password is shown. Default "Show password".
   */
  show: string;
  /** Shown while Caps Lock is on and the field has focus. Default "Caps Lock is on." */
  capsLock: string;
}

export const PASSWORD_INPUT_MESSAGES: PasswordInputMessages = {
  show: 'Show password',
  capsLock: 'Caps Lock is on.',
};

export interface PasswordInputProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'type' | 'size'
> {
  /**
   * What the password is for, which sets `autoComplete`: `current` (sign-in,
   * confirming an action; `current-password`) or `new` (sign-up, reset;
   * `new-password`, so password managers offer to generate and save one).
   * An explicit `autoComplete` wins. Default `current`.
   */
  purpose?: 'current' | 'new' | undefined;
  /** Controlled: whether the password is shown as text. */
  visible?: boolean | undefined;
  /** Uncontrolled initial visibility. Default `false`. */
  defaultVisible?: boolean | undefined;
  /** Called when the reveal button is toggled. */
  onVisibleChange?: ((visible: boolean) => void) | undefined;
  /** Warn under the field while Caps Lock is on. Default `true`. */
  capsLockWarning?: boolean | undefined;
  /** Marks the value invalid. Inside a Field, the Field's `invalid` is used. */
  invalid?: boolean | undefined;
  /** Control height from `--mn-control-*`. Default `md`. */
  size?: 'sm' | 'md' | 'lg' | undefined;
  /** Interface text, for translation. */
  messages?: Partial<PasswordInputMessages> | undefined;
}

/**
 * A password field with a reveal button. The button is a toggle
 * (`aria-pressed`) with a constant name, `purpose` sets the right
 * `autoComplete` for password managers, and an optional hint warns while
 * Caps Lock is on. One focus ring surrounds the input; the button keeps its
 * own. The ref and native input props go to the `<input>`.
 */
export const PasswordInput = forwardRef<HTMLInputElement, PasswordInputProps>(
  function PasswordInput(
    {
      purpose = 'current',
      visible: visibleProp,
      defaultVisible = false,
      onVisibleChange,
      capsLockWarning = true,
      invalid,
      size = 'md',
      messages: messagesProp,
      autoComplete,
      className,
      style,
      onKeyDown,
      onKeyUp,
      onBlur,
      ...rest
    },
    ref,
  ) {
    const messages = { ...PASSWORD_INPUT_MESSAGES, ...messagesProp };
    const autoId = useId();
    const hintId = `mn-password-input-${autoId}-caps`;
    const [internal, setInternal] = useState(defaultVisible);
    const visible = visibleProp ?? internal;
    const [capsLock, setCapsLock] = useState(false);

    const describedBy = [rest['aria-describedby'], capsLock ? hintId : undefined]
      .filter(Boolean)
      .join(' ');
    const props = useFieldControl(
      { ...rest, 'aria-describedby': describedBy || undefined },
      { invalid },
    );
    const inputId = props.id ?? `mn-password-input-${autoId}`;
    const disabled = !!props.disabled;

    const toggle = () => {
      const next = !visible;
      if (visibleProp === undefined) setInternal(next);
      onVisibleChange?.(next);
    };

    const readCapsLock = (e: KeyboardEvent<HTMLInputElement>) => {
      if (!capsLockWarning || typeof e.getModifierState !== 'function') return;
      setCapsLock(e.getModifierState('CapsLock'));
    };

    return (
      <div
        className={cx('mn-password-input', className)}
        style={style}
        data-size={size}
        data-visible={visible || undefined}
        data-disabled={disabled || undefined}
        data-invalid={props['aria-invalid'] ? '' : undefined}
      >
        <div className="mn-password-input__control">
          <input
            ref={ref}
            {...props}
            id={inputId}
            type={visible ? 'text' : 'password'}
            autoComplete={autoComplete ?? (purpose === 'new' ? 'new-password' : 'current-password')}
            autoCapitalize="off"
            autoCorrect="off"
            spellCheck={false}
            className="mn-password-input__input"
            onKeyDown={(e) => {
              readCapsLock(e);
              onKeyDown?.(e);
            }}
            onKeyUp={(e) => {
              readCapsLock(e);
              onKeyUp?.(e);
            }}
            onBlur={(e) => {
              setCapsLock(false);
              onBlur?.(e);
            }}
          />
          <button
            type="button"
            className="mn-password-input__toggle"
            aria-label={messages.show}
            aria-pressed={visible}
            aria-controls={inputId}
            disabled={disabled}
            onClick={toggle}
          >
            {visible ? <EyeOffGlyph /> : <EyeGlyph />}
          </button>
        </div>
        <p id={hintId} className="mn-password-input__hint" aria-live="polite">
          {capsLock ? messages.capsLock : ''}
        </p>
      </div>
    );
  },
);
