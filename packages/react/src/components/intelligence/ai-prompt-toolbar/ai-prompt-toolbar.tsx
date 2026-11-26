'use client';

import { forwardRef, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Button, type ButtonProps } from '../../actions/button/index.js';

export interface AiPromptToolbarProps extends HTMLAttributes<HTMLDivElement> {
  /** Name of the group. Default "Prompt options". */
  label?: string | undefined;
  /** Buttons, toggles and pickers, in order. */
  children?: ReactNode;
}

/**
 * The row of controls under the prompt: attach, web search, mode, model,
 * tone. Pass it to AiPromptInput's `toolbar` prop, or place it anywhere
 * near a prompt. A labelled group in normal Tab order, so pickers inside it
 * keep their own arrow keys. Use AiPromptToolbarButton and
 * AiPromptToolbarToggle for consistent sizing.
 */
export const AiPromptToolbar = forwardRef<HTMLDivElement, AiPromptToolbarProps>(
  function AiPromptToolbar({ label = 'Prompt options', className, children, ...rest }, ref) {
    return (
      <div
        ref={ref}
        role="group"
        aria-label={label}
        {...rest}
        className={cx('mn-ai-prompt-toolbar', className)}
      >
        {children}
      </div>
    );
  },
);

export interface AiPromptToolbarButtonProps extends Omit<
  ButtonProps,
  'variant' | 'size' | 'leadingIcon' | 'trailingIcon' | 'block' | 'asChild' | 'children'
> {
  /** The visible label, and always the accessible name. */
  children: string;
  /** Decorative icon before the label. */
  icon?: ReactNode | undefined;
  /** Show only the icon; the label becomes the accessible name and the tooltip. */
  hideLabel?: boolean | undefined;
}

/**
 * A compact ghost button for AiPromptToolbar, with an icon and a label that
 * can be hidden (it then becomes the tooltip and accessible name).
 */
export const AiPromptToolbarButton = forwardRef<HTMLButtonElement, AiPromptToolbarButtonProps>(
  function AiPromptToolbarButton(
    { children, icon, hideLabel = false, className, title, ...rest },
    ref,
  ) {
    return (
      <Button
        ref={ref}
        size="sm"
        variant="ghost"
        {...rest}
        title={title ?? (hideLabel ? children : undefined)}
        aria-label={hideLabel ? children : rest['aria-label']}
        className={cx('mn-ai-prompt-toolbar__button', className)}
        data-icon-only={hideLabel || undefined}
        leadingIcon={icon}
      >
        {hideLabel ? null : children}
      </Button>
    );
  },
);

export interface AiPromptToolbarToggleProps extends Omit<
  AiPromptToolbarButtonProps,
  'onClick' | 'aria-pressed'
> {
  /** On or off (controlled). */
  pressed?: boolean | undefined;
  /** Initial state when uncontrolled. Default false. */
  defaultPressed?: boolean | undefined;
  /** Called with the new state. */
  onPressedChange?: ((pressed: boolean) => void) | undefined;
}

/**
 * An on/off option in AiPromptToolbar, such as "Search the web" or "Deep
 * research". A toggle button (`aria-pressed`) whose label stays the same;
 * when on it gets the AI fill and a visible border, so the state never
 * depends on colour alone.
 */
export const AiPromptToolbarToggle = forwardRef<HTMLButtonElement, AiPromptToolbarToggleProps>(
  function AiPromptToolbarToggle(
    { pressed: pressedProp, defaultPressed = false, onPressedChange, className, ...rest },
    ref,
  ) {
    const [inner, setInner] = useState(defaultPressed);
    const pressed = pressedProp ?? inner;
    return (
      <AiPromptToolbarButton
        ref={ref}
        {...rest}
        aria-pressed={pressed}
        data-state={pressed ? 'on' : 'off'}
        className={cx('mn-ai-prompt-toolbar__toggle', className)}
        onClick={() => {
          const next = !pressed;
          if (pressedProp === undefined) setInner(next);
          onPressedChange?.(next);
        }}
      />
    );
  },
);
