'use client';

import { forwardRef, type InputHTMLAttributes, type TextareaHTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { useFieldControl } from '../form-field/index.js';

export type ControlSize = 'sm' | 'md' | 'lg';

export interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  size?: ControlSize;
  /** Marks the value invalid. Inside a Field, the Field's `invalid` is used. */
  invalid?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { size = 'md', invalid, className, ...rest },
  ref,
) {
  const props = useFieldControl(rest, { invalid });
  return <input ref={ref} {...props} data-size={size} className={cx('mn-input', className)} />;
});

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  invalid?: boolean;
  /** Allow vertical resizing. Default true. */
  resizable?: boolean;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { invalid, resizable = true, rows = 3, className, ...rest },
  ref,
) {
  const props = useFieldControl(rest, { invalid });
  return (
    <textarea
      ref={ref}
      rows={rows}
      {...props}
      data-resizable={resizable || undefined}
      className={cx('mn-input', 'mn-textarea', className)}
    />
  );
});
