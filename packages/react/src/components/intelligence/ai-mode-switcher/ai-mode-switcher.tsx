'use client';

import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';

/** One way the assistant can work, e.g. Ask, Edit or Agent. */
export interface AiMode {
  /** Stable id, the value. */
  id: string;
  /** Short visible name: one word is best. */
  label: string;
  /** What changes in this mode. Shown under the switcher for the selected mode. */
  description?: string | undefined;
  /** Decorative icon before the label. */
  icon?: ReactNode | undefined;
  /** Show only the icon; the label stays the accessible name and tooltip. */
  hideLabel?: boolean | undefined;
  disabled?: boolean | undefined;
}

export interface AiModeSwitcherMessages {
  /** Name of the radio group. */
  label: string;
}

export const AI_MODE_SWITCHER_MESSAGES: AiModeSwitcherMessages = { label: 'Mode' };

export interface AiModeSwitcherProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultValue'
> {
  /** The modes, 2 to 5. */
  modes: readonly AiMode[];
  /** The selected mode id. */
  value: string;
  /** Called with the new mode id. */
  onChange: (id: string) => void;
  /** Show the selected mode's description under the switcher. Default true. */
  showDescription?: boolean | undefined;
  /** Control size. Default `sm`, for toolbars. */
  size?: 'sm' | 'md' | undefined;
  /** Disables every mode, e.g. while a response is running. */
  disabled?: boolean | undefined;
  /** Override any string. */
  messages?: Partial<AiModeSwitcherMessages> | undefined;
}

/**
 * Switches how the assistant works (for example Ask, Edit, Agent) as one
 * segmented radio group, with the selected mode's description underneath
 * so the consequence of the choice is always visible. Built on
 * SegmentedControl: one Tab stop, arrow keys move and select.
 */
export const AiModeSwitcher = forwardRef<HTMLDivElement, AiModeSwitcherProps>(
  function AiModeSwitcher(
    {
      modes,
      value,
      onChange,
      showDescription = true,
      size = 'sm',
      disabled = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_MODE_SWITCHER_MESSAGES, ...messagesProp };
    const descId = `${useId()}-description`;
    const selected = modes.find((mode) => mode.id === value);
    const describes = showDescription && modes.some((mode) => mode.description);
    const description = describes ? selected?.description : undefined;
    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-mode-switcher', className)}
        data-mode={selected?.id}
      >
        <SegmentedControl
          aria-label={m.label}
          aria-describedby={description ? descId : undefined}
          className="mn-ai-mode-switcher__control"
          size={size}
          disabled={disabled}
          value={value}
          onValueChange={onChange}
          options={modes.map((mode) => ({
            value: mode.id,
            label: mode.label,
            ...(mode.icon !== undefined ? { icon: mode.icon } : {}),
            ...(mode.hideLabel ? { hideLabel: true } : {}),
            ...(mode.disabled ? { disabled: true } : {}),
          }))}
        />
        {describes ? (
          // Polite, so switching modes with the arrow keys also reads what the new mode does.
          <p id={descId} className="mn-ai-mode-switcher__description" aria-live="polite">
            {description}
          </p>
        ) : null}
      </div>
    );
  },
);
