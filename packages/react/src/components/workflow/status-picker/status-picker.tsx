'use client';

import { forwardRef, type ButtonHTMLAttributes, type SVGProps } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';

/** The workflow stage a status belongs to. Decides its glyph and colour. */
export type StatusCategory = 'backlog' | 'unstarted' | 'started' | 'completed' | 'cancelled';

/** Categories in workflow order. */
export const STATUS_CATEGORIES: readonly StatusCategory[] = [
  'backlog',
  'unstarted',
  'started',
  'completed',
  'cancelled',
];

/** One status a team uses, such as “In review” in the `started` category. */
export interface StatusOption {
  /** Stable id. */
  id: string;
  /** Visible name. */
  name: string;
  /** The category, which sets the glyph and colour. */
  category: StatusCategory;
}

/** A status glyph per category: dashed ring, ring, half-filled ring, check, cross. Decorative. */
export const StatusGlyph = forwardRef<
  SVGSVGElement,
  { category: StatusCategory } & Omit<SVGProps<SVGSVGElement>, 'ref'>
>(function StatusGlyph({ category, className, ...rest }, ref) {
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
      className={cx('mn-status-picker__glyph', className)}
      data-category={category}
    >
      {category === 'completed' ? (
        <>
          <circle cx="7" cy="7" r="6" fill="currentColor" />
          <path
            d="M4.2 7.2l1.9 1.9 3.7-3.9"
            stroke="var(--mn-color-bg-surface)"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </>
      ) : category === 'cancelled' ? (
        <>
          <circle cx="7" cy="7" r="6" fill="currentColor" />
          <path
            d="M5 5l4 4M9 5l-4 4"
            stroke="var(--mn-color-bg-surface)"
            strokeWidth="1.5"
            strokeLinecap="round"
          />
        </>
      ) : category === 'started' ? (
        <>
          <circle cx="7" cy="7" r="5.5" stroke="currentColor" strokeWidth="1.5" />
          <path d="M7 3.5a3.5 3.5 0 0 1 0 7z" fill="currentColor" />
        </>
      ) : (
        <circle
          cx="7"
          cy="7"
          r="5.5"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeDasharray={category === 'backlog' ? '2 1.6' : undefined}
        />
      )}
    </svg>
  );
});
StatusGlyph.displayName = 'StatusGlyph';

/** Every string a status picker shows or announces. */
export interface StatusPickerMessages {
  /** Accessible name of the trigger, with the current status. */
  trigger: (status: string) => string;
  /** Shown when no status is selected. */
  none: string;
  /** Group headings in the menu. */
  categories: Record<StatusCategory, string>;
}

export const STATUS_PICKER_MESSAGES: StatusPickerMessages = {
  trigger: (status) => `Status: ${status}`,
  none: 'No status',
  categories: {
    backlog: 'Backlog',
    unstarted: 'Not started',
    started: 'In progress',
    completed: 'Completed',
    cancelled: 'Cancelled',
  },
};

export interface StatusPickerProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'value' | 'defaultValue' | 'onChange'
> {
  /** The statuses to choose from, in display order. Grouped by category in the menu. */
  statuses: readonly StatusOption[];
  /** The selected status id. */
  value: string | null | undefined;
  /** Called with the chosen status id. */
  onValueChange?: ((id: string) => void) | undefined;
  /** Show the status name next to the glyph. Default true; false gives a compact glyph-only trigger (still named). */
  showLabel?: boolean | undefined;
  /** Trigger size. Default `md`. */
  size?: 'sm' | 'md' | undefined;
  /** Translated strings. */
  messages?:
    | (Partial<Omit<StatusPickerMessages, 'categories'>> & {
        categories?: Partial<Record<StatusCategory, string>> | undefined;
      })
    | undefined;
}

/**
 * A menu button for an issue’s status. Statuses are grouped by category
 * (backlog, not started, in progress, completed, cancelled), and each shows
 * a category glyph and colour next to its name, so status never relies on
 * colour alone.
 */
export const StatusPicker = forwardRef<HTMLButtonElement, StatusPickerProps>(function StatusPicker(
  {
    statuses,
    value,
    onValueChange,
    showLabel = true,
    size = 'md',
    messages: messagesProp,
    className,
    disabled,
    ...rest
  },
  ref,
) {
  const m = {
    ...STATUS_PICKER_MESSAGES,
    ...messagesProp,
    categories: { ...STATUS_PICKER_MESSAGES.categories, ...messagesProp?.categories },
  };
  const current = statuses.find((s) => s.id === value);
  const name = current?.name ?? m.none;
  const groups = STATUS_CATEGORIES.map((category) => ({
    category,
    items: statuses.filter((s) => s.category === category),
  })).filter((g) => g.items.length > 0);

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        ref={ref}
        type="button"
        {...rest}
        disabled={disabled}
        aria-label={m.trigger(name)}
        className={cx('mn-status-picker', className)}
        data-size={size}
        data-category={current?.category}
        data-icon-only={!showLabel || undefined}
      >
        <StatusGlyph category={current?.category ?? 'backlog'} />
        {showLabel ? <span className="mn-status-picker__name">{name}</span> : null}
        {showLabel ? <ChevronDownGlyph className="mn-status-picker__chevron" /> : null}
      </DropdownMenuTrigger>
      <DropdownMenuContent className="mn-status-picker__menu">
        <DropdownMenuRadioGroup
          value={value ?? ''}
          onValueChange={(id) => {
            if (id) onValueChange?.(id);
          }}
        >
          {groups.map((g) => (
            <DropdownMenuGroup key={g.category}>
              <DropdownMenuLabel>{m.categories[g.category]}</DropdownMenuLabel>
              {g.items.map((s) => (
                <DropdownMenuRadioItem key={s.id} value={s.id}>
                  <span className="mn-status-picker__option">
                    <StatusGlyph category={s.category} />
                    {s.name}
                  </span>
                </DropdownMenuRadioItem>
              ))}
            </DropdownMenuGroup>
          ))}
        </DropdownMenuRadioGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  );
});
