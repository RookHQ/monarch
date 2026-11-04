'use client';

import { forwardRef, useCallback, useId, useState } from 'react';
import { cx } from '../../../internal/cx.js';
import { EyeGlyph, EyeOffGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../actions/button/index.js';
import { CopyButton } from '../../actions/copy-button/index.js';
import { useFieldContext } from '../../forms/form-field/index.js';
import { Input, type InputProps } from '../../forms/text-field/index.js';

export interface SecretFieldProps extends Omit<InputProps, 'type' | 'onCopy'> {
  /**
   * What the secret is ("API key", "Bot token"). Names the reveal and copy
   * buttons ("Show API key") and, outside a Field, the text box.
   */
  label: string;
  /** Controlled: whether the secret is shown in plain text. */
  revealed?: boolean;
  /** Uncontrolled initial value of `revealed`. Default false. */
  defaultRevealed?: boolean;
  onRevealedChange?: (revealed: boolean) => void;
  /** Show the reveal/hide toggle. Default true. */
  revealable?: boolean;
  /** Show a copy button. It copies the current value without revealing it. Default false. */
  copyable?: boolean;
  /**
   * A value is saved on the server but is never sent back, so there is
   * nothing to reveal or copy. The field stays empty, says the secret is set,
   * and typing a new value replaces it; leaving it blank keeps the saved one.
   */
  isSet?: boolean;
  /** Placeholder while `isSet` and empty. Default "Saved. Enter a new value to replace it". */
  setPlaceholder?: string;
  /** Screen reader description while `isSet`. Default "A value is saved and hidden. Leave blank to keep it." */
  setDescription?: string;
  /** Called with the copied text after a successful copy. */
  onCopy?: (text: string) => void;
  /** Called when copying fails. */
  onCopyError?: (error: Error) => void;
}

/**
 * A masked secret (API key, token, signing secret) with reveal/hide and an
 * optional copy button. Never prefilled by the browser or password managers.
 * Supports the "set, not shown" state for secrets the server never returns.
 */
export const SecretField = forwardRef<HTMLInputElement, SecretFieldProps>(function SecretField(
  {
    label,
    revealed: revealedProp,
    defaultRevealed = false,
    onRevealedChange,
    revealable = true,
    copyable = false,
    isSet = false,
    setPlaceholder = 'Saved. Enter a new value to replace it',
    setDescription = 'A value is saved and hidden. Leave blank to keep it.',
    onCopy,
    onCopyError,
    size = 'md',
    className,
    style,
    placeholder,
    disabled,
    value,
    defaultValue,
    onChange,
    'aria-describedby': describedBy,
    ...rest
  },
  ref,
) {
  const field = useFieldContext();
  const setId = `mn-secret-${useId()}`;
  const [revealedState, setRevealedState] = useState(defaultRevealed);
  const revealed = revealedProp ?? revealedState;
  const setRevealed = useCallback(
    (next: boolean) => {
      if (revealedProp === undefined) setRevealedState(next);
      onRevealedChange?.(next);
    },
    [revealedProp, onRevealedChange],
  );

  // Track the current text so copy works in uncontrolled mode too.
  const [innerValue, setInnerValue] = useState(() => String(defaultValue ?? ''));
  const current = value !== undefined ? String(value) : innerValue;
  const empty = current === '';
  // A saved-but-hidden secret has nothing to reveal until the user types a new one.
  const hasPlainText = !empty;
  const name = label;
  const actions = (revealable ? 1 : 0) + (copyable ? 1 : 0);
  const buttonSize = size === 'lg' ? 'sm' : 'xs';

  return (
    <div
      className={cx('mn-secret-field', className)}
      style={style}
      data-revealed={revealed || undefined}
      data-set={isSet || undefined}
      data-size={size}
      data-actions={actions}
    >
      <Input
        ref={ref}
        {...rest}
        {...(field ? {} : { 'aria-label': label })}
        {...(value !== undefined ? { value } : {})}
        {...(defaultValue !== undefined ? { defaultValue } : {})}
        onChange={(event) => {
          if (value === undefined) setInnerValue(event.currentTarget.value);
          onChange?.(event);
        }}
        size={size}
        disabled={disabled}
        type={revealed ? 'text' : 'password'}
        placeholder={isSet && empty ? setPlaceholder : placeholder}
        aria-describedby={cx(describedBy, isSet ? setId : undefined)}
        autoComplete={rest.autoComplete ?? 'off'}
        autoCorrect="off"
        autoCapitalize="off"
        spellCheck={false}
        // Ask password managers not to offer, save or fill this field.
        data-1p-ignore=""
        data-lpignore="true"
        data-bwignore=""
        data-form-type="other"
        className="mn-secret-field__control"
      />
      {isSet ? (
        <span id={setId} className="mn-visually-hidden">
          {setDescription}
        </span>
      ) : null}
      <span className="mn-secret-field__actions">
        {revealable ? (
          <IconButton
            label={`Show ${name}`}
            icon={revealed ? <EyeOffGlyph /> : <EyeGlyph />}
            size={buttonSize}
            aria-pressed={revealed}
            // Nothing to show while the field is empty (including a saved-but-hidden secret).
            disabled={disabled || !hasPlainText}
            onClick={() => setRevealed(!revealed)}
            className="mn-secret-field__toggle"
          />
        ) : null}
        {copyable ? (
          <CopyButton
            value={() => current}
            label="Copy"
            target={name}
            iconOnly
            size={buttonSize}
            disabled={disabled || !hasPlainText}
            {...(onCopy ? { onCopy } : {})}
            {...(onCopyError ? { onCopyError } : {})}
            className="mn-secret-field__copy"
          />
        ) : null}
      </span>
    </div>
  );
});
