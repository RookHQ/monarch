'use client';

import { forwardRef, useId, type HTMLAttributes } from 'react';
import { Switch } from '../../forms/switch/index.js';
import { cx } from '../../../internal/cx.js';

export interface AvailabilityToggleMessages {
  /** The switch's accessible name. Default "Available for new work". */
  label: string;
  /** Status text when on. Default "Available". */
  available: string;
  /** Status text when off. Default "Away". */
  away: string;
  /** Hint when on (with `showHint`). */
  availableHint: string;
  /** Hint when off (with `showHint`). */
  awayHint: string;
}

const DEFAULT_MESSAGES: AvailabilityToggleMessages = {
  label: 'Available for new work',
  available: 'Available',
  away: 'Away',
  availableHint: 'Automatic assignment can give you new work.',
  awayHint: 'Automatic assignment skips you. You can still work on what you have.',
};

export interface AvailabilityToggleProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'onChange'
> {
  /**
   * Whether the person is available. `null` or `undefined` means not known
   * yet: the toggle renders nothing, as the app copy did.
   */
  available: boolean | null | undefined;
  /** Called with the new value. Update optimistically and roll back on error in the app. */
  onAvailableChange?: ((available: boolean) => void) | undefined;
  /** A change is being saved: the switch is disabled and busy. */
  pending?: boolean | undefined;
  /** Show a one-line hint under the status that explains the effect. Default false. */
  showHint?: boolean | undefined;
  /**
   * Set when the toggle sits inside a DropdownMenu or other widget with its own
   * key handling, so Space and typing reach the switch instead of the menu.
   */
  inMenu?: boolean | undefined;
  /** Disable the switch (no permission to change it). */
  disabled?: boolean | undefined;
  /** Interface text, for translation. */
  messages?: Partial<AvailabilityToggleMessages> | undefined;
}

/**
 * An agent's on/off availability for automatic assignment: a status word
 * ("Available" / "Away"), an optional hint and a switch. The app saves the
 * change and shows any error; the toggle only reports the new value.
 */
export const AvailabilityToggle = forwardRef<HTMLDivElement, AvailabilityToggleProps>(
  function AvailabilityToggle(
    {
      available,
      onAvailableChange,
      pending = false,
      showHint = false,
      inMenu = false,
      disabled = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const id = useId();
    if (available === null || available === undefined) return null;
    const statusId = `${id}-status`;
    const hintId = `${id}-hint`;
    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-availability-toggle', className)}
        data-available={available ? '' : undefined}
        data-pending={pending ? '' : undefined}
      >
        <div className="mn-availability-toggle__text">
          <span id={statusId} className="mn-availability-toggle__status">
            <span className="mn-availability-toggle__mark" aria-hidden />
            {available ? messages.available : messages.away}
          </span>
          {showHint ? (
            <span id={hintId} className="mn-availability-toggle__hint">
              {available ? messages.availableHint : messages.awayHint}
            </span>
          ) : null}
        </div>
        <Switch
          checked={available}
          disabled={disabled || pending}
          aria-busy={pending || undefined}
          aria-label={messages.label}
          aria-describedby={showHint ? hintId : undefined}
          onCheckedChange={(next) => onAvailableChange?.(next)}
          // In a menu, Space and typing must toggle the switch, not reach the menu's typeahead.
          onKeyDown={inMenu ? (event) => event.stopPropagation() : undefined}
          className="mn-availability-toggle__switch"
        />
      </div>
    );
  },
);
