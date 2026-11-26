'use client';

import { forwardRef, useCallback, useState } from 'react';
import { cx } from '../../../internal/cx.js';
import { Button, type ButtonProps } from '../button/index.js';

/** `default` is borderless until pressed; `outline` always shows a border. */
export type ToggleButtonVariant = 'default' | 'outline';

export interface ToggleButtonProps extends Omit<
  ButtonProps,
  'variant' | 'asChild' | 'brandColor' | 'onChange' | 'aria-pressed' | 'type'
> {
  /** Controlled pressed state. */
  pressed?: boolean;
  /** Initial pressed state when uncontrolled. Default `false`. */
  defaultPressed?: boolean;
  /** Called with the new state when the person presses the button. */
  onPressedChange?: ((pressed: boolean) => void) | undefined;
  /** Visual style. Default `default` (borderless until pressed). */
  variant?: ToggleButtonVariant;
}

/** Keeps a value controlled when `value` is defined, uncontrolled otherwise. */
function useControllableBoolean(
  value: boolean | undefined,
  initial: boolean,
  onChange: ((next: boolean) => void) | undefined,
): [boolean, (next: boolean) => void] {
  const [inner, setInner] = useState(initial);
  const controlled = value !== undefined;
  const current = controlled ? value : inner;
  const set = useCallback(
    (next: boolean) => {
      if (!controlled) setInner(next);
      onChange?.(next);
    },
    [controlled, onChange],
  );
  return [current, set];
}

/**
 * A button that stays on or off, such as Bold in a text toolbar or "Show
 * archived" in a list header. It exposes its state with `aria-pressed`, so
 * the label must name the thing being switched and must not change with the
 * state.
 */
export const ToggleButton = forwardRef<HTMLButtonElement, ToggleButtonProps>(function ToggleButton(
  {
    pressed: pressedProp,
    defaultPressed = false,
    onPressedChange,
    variant = 'default',
    className,
    onClick,
    ...rest
  },
  ref,
) {
  const [pressed, setPressed] = useControllableBoolean(
    pressedProp,
    defaultPressed,
    onPressedChange,
  );
  return (
    <Button
      ref={ref}
      {...rest}
      type="button"
      variant={variant === 'outline' ? 'secondary' : 'ghost'}
      aria-pressed={pressed}
      data-state={pressed ? 'on' : 'off'}
      data-toggle-variant={variant}
      className={cx('mn-toggle-button', className)}
      onClick={(event) => {
        onClick?.(event);
        if (event.defaultPrevented) return;
        setPressed(!pressed);
      }}
    />
  );
});
