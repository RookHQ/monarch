'use client';

import { forwardRef, type ButtonHTMLAttributes, type SVGProps } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';

/** Issue priority levels, from most to least urgent; `none` means not set. */
export type PriorityLevel = 'urgent' | 'high' | 'medium' | 'low' | 'none';

/** Levels in menu order. */
export const PRIORITY_LEVELS: readonly PriorityLevel[] = [
  'urgent',
  'high',
  'medium',
  'low',
  'none',
];

/**
 * Converts the numeric priorities many trackers store (Stride and Linear:
 * 0 none, 1 urgent, 2 high, 3 medium, 4 low) to a level, and back.
 */
export function priorityFromNumber(n: number | null | undefined): PriorityLevel {
  return (['none', 'urgent', 'high', 'medium', 'low'] as const)[n ?? 0] ?? 'none';
}
export function priorityToNumber(level: PriorityLevel): number {
  return { none: 0, urgent: 1, high: 2, medium: 3, low: 4 }[level];
}

/**
 * A distinct shape per level: an exclamation tile for urgent, three, two or
 * one filled bars for high, medium and low, and dashes for none. Decorative.
 */
export const PriorityGlyph = forwardRef<
  SVGSVGElement,
  { priority: PriorityLevel } & Omit<SVGProps<SVGSVGElement>, 'ref'>
>(function PriorityGlyph({ priority, className, ...rest }, ref) {
  const bars = { urgent: 0, high: 3, medium: 2, low: 1, none: 0 }[priority];
  return (
    <svg
      viewBox="0 0 14 14"
      width="14"
      height="14"
      fill="none"
      aria-hidden
      focusable={false}
      {...rest}
      ref={ref}
      className={cx('mn-priority-picker__glyph', className)}
      data-priority={priority}
    >
      {priority === 'urgent' ? (
        <>
          <rect x="1" y="1" width="12" height="12" rx="3" fill="currentColor" />
          <path
            d="M7 3.8v4M7 10v.1"
            stroke="var(--mn-color-bg-surface)"
            strokeWidth="1.6"
            strokeLinecap="round"
          />
        </>
      ) : priority === 'none' ? (
        <path
          d="M2.5 7h2M6 7h2M9.5 7h2"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinecap="round"
        />
      ) : (
        [0, 1, 2].map((i) => (
          <rect
            key={i}
            x={1.5 + i * 4}
            y={9 - i * 3}
            width="3"
            height={3 + i * 3}
            rx="1"
            fill={i < bars ? 'currentColor' : 'none'}
            stroke="currentColor"
            strokeWidth={i < bars ? 0 : 1}
            opacity={i < bars ? 1 : 0.45}
          />
        ))
      )}
    </svg>
  );
});
PriorityGlyph.displayName = 'PriorityGlyph';

/** Every string a priority picker shows or announces. */
export interface PriorityPickerMessages {
  /** Accessible name of the trigger, with the current priority. */
  trigger: (priority: string) => string;
  /** Names of the levels. */
  levels: Record<PriorityLevel, string>;
}

export const PRIORITY_PICKER_MESSAGES: PriorityPickerMessages = {
  trigger: (priority) => `Priority: ${priority}`,
  levels: {
    urgent: 'Urgent',
    high: 'High',
    medium: 'Medium',
    low: 'Low',
    none: 'No priority',
  },
};

export interface PriorityPickerProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'value' | 'defaultValue' | 'onChange'
> {
  /** The selected level. */
  value: PriorityLevel | null | undefined;
  /** Called with the chosen level. */
  onValueChange?: ((level: PriorityLevel) => void) | undefined;
  /** Levels offered, in order. Default all five. */
  levels?: readonly PriorityLevel[] | undefined;
  /** Show the level name next to the glyph. Default true; false gives a glyph-only trigger (still named). */
  showLabel?: boolean | undefined;
  /** Trigger size. Default `md`. */
  size?: 'sm' | 'md' | undefined;
  /** Start with the menu open (documentation previews). */
  defaultOpen?: boolean | undefined;
  /** Translated strings. */
  messages?:
    | (Partial<Omit<PriorityPickerMessages, 'levels'>> & {
        levels?: Partial<Record<PriorityLevel, string>> | undefined;
      })
    | undefined;
}

/**
 * A menu button for an issue’s priority. Every level has its own shape as
 * well as colour (urgent tile, bar counts, dashes) and its name in the menu,
 * so priority is never shown by colour alone.
 */
export const PriorityPicker = forwardRef<HTMLButtonElement, PriorityPickerProps>(
  function PriorityPicker(
    {
      value,
      onValueChange,
      levels = PRIORITY_LEVELS,
      showLabel = true,
      size = 'md',
      defaultOpen,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = {
      ...PRIORITY_PICKER_MESSAGES,
      ...messagesProp,
      levels: { ...PRIORITY_PICKER_MESSAGES.levels, ...messagesProp?.levels },
    };
    const current = value ?? 'none';
    const name = m.levels[current];
    return (
      <DropdownMenu {...(defaultOpen !== undefined ? { defaultOpen } : {})}>
        <DropdownMenuTrigger
          ref={ref}
          type="button"
          {...rest}
          aria-label={m.trigger(name)}
          className={cx('mn-priority-picker', className)}
          data-size={size}
          data-priority={current}
          data-icon-only={!showLabel || undefined}
        >
          <PriorityGlyph priority={current} />
          {showLabel ? <span className="mn-priority-picker__name">{name}</span> : null}
          {showLabel ? <ChevronDownGlyph className="mn-priority-picker__chevron" /> : null}
        </DropdownMenuTrigger>
        <DropdownMenuContent className="mn-priority-picker__menu">
          <DropdownMenuRadioGroup
            value={current}
            onValueChange={(v) => onValueChange?.(v as PriorityLevel)}
          >
            {levels.map((level) => (
              <DropdownMenuRadioItem key={level} value={level}>
                <span className="mn-priority-picker__option">
                  <PriorityGlyph priority={level} />
                  {m.levels[level]}
                </span>
              </DropdownMenuRadioItem>
            ))}
          </DropdownMenuRadioGroup>
        </DropdownMenuContent>
      </DropdownMenu>
    );
  },
);
