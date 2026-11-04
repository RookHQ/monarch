'use client';

import { forwardRef, useRef, type FocusEvent, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { CopyButton, type CopyButtonProps } from '../copy-button/index.js';
import { useFieldContext } from '../../forms/form-field/index.js';
import { Input, Textarea, type ControlSize } from '../../forms/text-field/index.js';

export interface CopyFieldProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onCopy'> {
  /** The read-only value shown and copied. */
  value: string;
  /**
   * What the value is ("Webhook URL"). Names the text box outside a Field
   * (inside one, the FieldLabel does) and completes the copy button's name:
   * "Copy webhook URL".
   */
  label: string;
  /** Show the value in a multi-line text area (certificates, JSON, snippets). */
  multiline?: boolean;
  /** Visible rows when `multiline`. Default 3. */
  rows?: number;
  /** Monospace text for codes, keys, hostnames and DNS records. Default true. */
  mono?: boolean;
  size?: ControlSize;
  /** Select the whole value when the text box gets focus. Default true. */
  selectOnFocus?: boolean;
  /** Show only the copy icon. The button is still named "Copy {label}". Default false. */
  iconOnly?: boolean;
  /** Visible text of the copy button. Default "Copy". */
  copyLabel?: string;
  /** Visible text of the copy button after copying. Default "Copied". */
  copiedLabel?: string;
  /** Milliseconds before the button returns to its idle look. Default 2000. */
  resetAfter?: number;
  /** Called with the copied text after a successful copy. */
  onCopy?: (text: string) => void;
  /**
   * Called when copying fails. The value is selected first, so the user can
   * copy it by hand; show a toast here if the product uses them.
   */
  onCopyError?: (error: Error) => void;
  disabled?: boolean;
  /** Extra props for the copy button, for example `variant`. */
  copyButtonProps?: Omit<
    CopyButtonProps,
    | 'value'
    | 'label'
    | 'target'
    | 'iconOnly'
    | 'onCopy'
    | 'onCopyError'
    | 'resetAfter'
    | 'size'
    | 'copiedLabel'
  >;
}

const selectAll = (event: FocusEvent<HTMLInputElement | HTMLTextAreaElement>) =>
  event.currentTarget.select();

/**
 * A read-only value with a copy button: webhook URLs, DNS records, embed
 * snippets, calendar feed links. If copying fails, the value is focused and
 * selected so the user can copy it by hand.
 */
export const CopyField = forwardRef<HTMLDivElement, CopyFieldProps>(function CopyField(
  {
    value,
    label,
    multiline = false,
    rows = 3,
    mono = true,
    size = 'md',
    selectOnFocus = true,
    iconOnly = false,
    copyLabel = 'Copy',
    copiedLabel = 'Copied',
    resetAfter = 2000,
    onCopy,
    onCopyError,
    disabled,
    copyButtonProps,
    className,
    ...rest
  },
  ref,
) {
  const field = useFieldContext();
  const controlRef = useRef<HTMLInputElement & HTMLTextAreaElement>(null);
  const control = {
    // Inside a Field the visible FieldLabel names the control; outside, `label` does.
    ...(field ? {} : { 'aria-label': label }),
    value,
    readOnly: true,
    disabled,
    spellCheck: false,
    autoComplete: 'off',
    onFocus: selectOnFocus ? selectAll : undefined,
    className: 'mn-copy-field__control',
  } as const;

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-copy-field', className)}
      data-multiline={multiline || undefined}
      data-mono={mono || undefined}
      data-size={size}
    >
      {multiline ? (
        <Textarea ref={controlRef} {...control} rows={rows} resizable={false} />
      ) : (
        <Input ref={controlRef} {...control} size={size} />
      )}
      <CopyButton
        {...copyButtonProps}
        value={value}
        label={copyLabel}
        target={label}
        copiedLabel={copiedLabel}
        iconOnly={iconOnly}
        size={size}
        resetAfter={resetAfter}
        disabled={disabled}
        {...(onCopy ? { onCopy } : {})}
        onCopyError={(error) => {
          const el = controlRef.current;
          if (el) {
            el.focus();
            el.select();
          }
          onCopyError?.(error);
        }}
        className={cx('mn-copy-field__button', copyButtonProps?.className)}
      />
    </div>
  );
});
