'use client';

import {
  forwardRef,
  type HTMLAttributes,
  type ReactElement,
  type ReactNode,
  type Ref,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CalendarGlyph } from '../../../internal/icons.js';
import { SegmentedControl, type SegmentedOption } from '../../forms/segmented-control/index.js';

/** The views ViewSwitcher knows by name, with a built-in label and icon. */
export type ViewSwitcherBuiltIn = 'table' | 'board' | 'calendar' | 'gallery' | 'list' | 'timeline';

/** A view: a built-in name, or your own value with a label and icon. */
export interface ViewSwitcherOption<T extends string = string> {
  value: T;
  /** Visible text and accessible name. Default: the built-in label for built-in values. */
  label?: string | undefined;
  /** Decorative icon. Default: the built-in icon for built-in values. */
  icon?: ReactNode | undefined;
  /** A count after the label, e.g. items in that view. */
  count?: number | string | undefined;
  disabled?: boolean | undefined;
}

export interface ViewSwitcherMessages {
  /** Accessible name of the group. */
  label: string;
  table: string;
  board: string;
  calendar: string;
  gallery: string;
  list: string;
  timeline: string;
}

export const defaultViewSwitcherMessages: ViewSwitcherMessages = {
  label: 'View',
  table: 'Table',
  board: 'Board',
  calendar: 'Calendar',
  gallery: 'Gallery',
  list: 'List',
  timeline: 'Timeline',
};

export interface ViewSwitcherProps<T extends string = ViewSwitcherBuiltIn> extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange' | 'children' | 'dir'
> {
  /**
   * The views, in order: built-in names (`'table'`, `'board'`, `'calendar'`,
   * `'gallery'`, `'list'`, `'timeline'`) or option objects. Default: table,
   * board, calendar and gallery.
   */
  views?: readonly (T | ViewSwitcherOption<T>)[] | undefined;
  /** The current view (controlled). */
  value?: T | undefined;
  /** The initial view (uncontrolled). Default: the first view. */
  defaultValue?: T | undefined;
  /** Called with the chosen view. */
  onValueChange?: ((value: T) => void) | undefined;
  /**
   * `show` keeps text labels, `hide` shows icons only (labels stay as the
   * accessible names and tooltips), `auto` (default) hides them on narrow
   * screens.
   */
  labels?: 'auto' | 'show' | 'hide' | undefined;
  size?: 'sm' | 'md' | 'lg' | undefined;
  /** Disables every view. */
  disabled?: boolean | undefined;
  /** Text direction. Default: inherited. */
  dir?: 'ltr' | 'rtl' | undefined;
  messages?: Partial<ViewSwitcherMessages> | undefined;
}

const glyph = {
  width: 16,
  height: 16,
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.5,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
  focusable: false,
} as const;

function TableGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <rect x="2" y="2.5" width="12" height="11" rx="1.5" />
      <path d="M2 6h12M2 9.5h12M6 6v7.5" />
    </svg>
  );
}
function BoardGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <rect x="2" y="2.5" width="3.5" height="11" rx="1" />
      <rect x="6.25" y="2.5" width="3.5" height="7" rx="1" />
      <rect x="10.5" y="2.5" width="3.5" height="9" rx="1" />
    </svg>
  );
}
function GalleryGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <rect x="2" y="2" width="5" height="5" rx="1" />
      <rect x="9" y="2" width="5" height="5" rx="1" />
      <rect x="2" y="9" width="5" height="5" rx="1" />
      <rect x="9" y="9" width="5" height="5" rx="1" />
    </svg>
  );
}
function ListGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <path d="M5.5 4h8.5M5.5 8h8.5M5.5 12h8.5M2.25 4h.5M2.25 8h.5M2.25 12h.5" />
    </svg>
  );
}
function TimelineGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props}>
      <path d="M2 4h7M5 8h8M3.5 12h6" />
    </svg>
  );
}

const ICONS: Record<ViewSwitcherBuiltIn, () => ReactNode> = {
  table: () => <TableGlyph />,
  board: () => <BoardGlyph />,
  calendar: () => <CalendarGlyph />,
  gallery: () => <GalleryGlyph />,
  list: () => <ListGlyph />,
  timeline: () => <TimelineGlyph />,
};

const DEFAULT_VIEWS: readonly ViewSwitcherBuiltIn[] = ['table', 'board', 'calendar', 'gallery'];

function isBuiltIn(value: string): value is ViewSwitcherBuiltIn {
  return value in ICONS;
}

/** Resolves views to SegmentedControl options with built-in labels and icons. */
export function viewSwitcherOptions<T extends string>(
  views: readonly (T | ViewSwitcherOption<T>)[],
  messages: Partial<ViewSwitcherMessages> = {},
): SegmentedOption<T>[] {
  const m = { ...defaultViewSwitcherMessages, ...messages };
  return views.map((v) => {
    const o: ViewSwitcherOption<T> = typeof v === 'string' ? { value: v } : v;
    const builtIn = isBuiltIn(o.value) ? o.value : null;
    const option: SegmentedOption<T> = {
      value: o.value,
      label: o.label ?? (builtIn ? m[builtIn] : o.value),
    };
    const icon = o.icon ?? (builtIn ? ICONS[builtIn]() : undefined);
    if (icon) option.icon = icon;
    if (o.count !== undefined) option.count = o.count;
    if (o.disabled) option.disabled = true;
    return option;
  });
}

function ViewSwitcherInner<T extends string = ViewSwitcherBuiltIn>(
  {
    views = DEFAULT_VIEWS as unknown as readonly T[],
    value,
    defaultValue,
    onValueChange,
    labels = 'auto',
    size = 'md',
    disabled = false,
    dir,
    messages,
    className,
    'aria-label': ariaLabel,
    'aria-labelledby': labelledBy,
    ...rest
  }: ViewSwitcherProps<T>,
  ref: Ref<HTMLDivElement>,
) {
  const m = { ...defaultViewSwitcherMessages, ...messages };
  const options = viewSwitcherOptions(views, messages).map((o) =>
    labels === 'hide' ? { ...o, hideLabel: true } : o,
  );
  const first = options.find((o) => !o.disabled)?.value;
  const name = labelledBy
    ? { 'aria-labelledby': labelledBy }
    : { 'aria-label': ariaLabel ?? m.label };
  const initial = defaultValue ?? first;
  const state =
    value !== undefined ? { value } : initial !== undefined ? { defaultValue: initial } : {};
  return (
    <SegmentedControl<T>
      ref={ref}
      {...rest}
      {...name}
      {...state}
      {...(onValueChange ? { onValueChange } : {})}
      {...(dir ? { dir } : {})}
      options={options}
      size={size}
      disabled={disabled}
      className={cx('mn-view-switcher', className)}
      data-labels={labels}
    />
  );
}

/**
 * Switches how a collection is shown (table, board, calendar, gallery, list,
 * timeline) as one segmented control with an icon and label per view. A
 * radio group: one Tab stop, arrow keys move and select. Labels collapse to
 * icons on narrow screens and stay as accessible names.
 */
export const ViewSwitcher = forwardRef(ViewSwitcherInner) as <
  T extends string = ViewSwitcherBuiltIn,
>(
  props: ViewSwitcherProps<T> & { ref?: Ref<HTMLDivElement> },
) => ReactElement;
