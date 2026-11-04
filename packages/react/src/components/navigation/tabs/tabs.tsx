'use client';

import {
  forwardRef,
  useCallback,
  useId,
  useRef,
  useState,
  type AnchorHTMLAttributes,
  type ComponentPropsWithoutRef,
  type ElementRef,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import * as TabsPrimitive from '@radix-ui/react-tabs';
import { Slot, Slottable } from '@radix-ui/react-slot';
import { cx } from '../../../internal/cx.js';

/** Arrow keys move between tabs; Home/End jump to the ends. */
export const Tabs = TabsPrimitive.Root;

export type TabsVariant = 'line' | 'pill';
export type TabsCountTone = 'neutral' | 'accent' | 'info' | 'success' | 'warning' | 'danger';

export interface TabsListProps extends ComponentPropsWithoutRef<typeof TabsPrimitive.List> {
  /** `line` for page sections, `pill` for compact view switches. */
  variant?: TabsVariant;
  /**
   * Scroll horizontally instead of overflowing when the tabs do not fit, as
   * on phones. The focused tab is scrolled into view. Exposed as `data-scrollable`.
   */
  scrollable?: boolean;
}

export const TabsList = forwardRef<ElementRef<typeof TabsPrimitive.List>, TabsListProps>(
  function TabsList({ variant = 'line', scrollable = false, className, ...rest }, ref) {
    return (
      <TabsPrimitive.List
        ref={ref}
        {...rest}
        data-variant={variant}
        data-scrollable={scrollable || undefined}
        className={cx('mn-tabs__list', className)}
      />
    );
  },
);

function hasCount(count: ReactNode): boolean {
  return count !== null && count !== undefined && count !== false && count !== '';
}

/** The count bubble shared by every trigger type. Part of the trigger's accessible name. */
function TabsCount({ count, tone }: { count: ReactNode; tone: TabsCountTone }) {
  if (!hasCount(count)) return null;
  return (
    <span className="mn-tabs__count" data-tone={tone}>
      {count}
    </span>
  );
}

interface CountProps {
  /**
   * Count shown in a bubble after the label, for example open items. It is
   * read as part of the tab's name ("Open 12"); pass a string such as
   * `"12 new"` when the number alone is ambiguous. On TabsTrigger, not
   * combined with `asChild` (the child must be the only element).
   */
  count?: ReactNode;
  /** Colour of the count bubble. Default `neutral`. */
  countTone?: TabsCountTone;
}

export interface TabsTriggerProps
  extends ComponentPropsWithoutRef<typeof TabsPrimitive.Trigger>, CountProps {}

export const TabsTrigger = forwardRef<ElementRef<typeof TabsPrimitive.Trigger>, TabsTriggerProps>(
  function TabsTrigger({ className, count, countTone = 'neutral', children, ...rest }, ref) {
    return (
      <TabsPrimitive.Trigger ref={ref} {...rest} className={cx('mn-tabs__trigger', className)}>
        {hasCount(count) ? (
          <>
            {children}
            <TabsCount count={count} tone={countTone} />
          </>
        ) : (
          children
        )}
      </TabsPrimitive.Trigger>
    );
  },
);

export const TabsContent = forwardRef<
  ElementRef<typeof TabsPrimitive.Content>,
  ComponentPropsWithoutRef<typeof TabsPrimitive.Content>
>(function TabsContent({ className, ...rest }, ref) {
  return (
    <TabsPrimitive.Content ref={ref} {...rest} className={cx('mn-tabs__content', className)} />
  );
});

/* ------------------------------------------------------------------ */
/* Link (route) tabs                                                   */
/* ------------------------------------------------------------------ */

export interface TabsNavProps extends HTMLAttributes<HTMLElement> {
  /** Names the navigation landmark, for example "Project sections". Required. */
  'aria-label': string;
  variant?: TabsVariant;
  /** Scroll horizontally when the links do not fit. */
  scrollable?: boolean;
}

/**
 * Tab-styled navigation between pages: a `<nav>` of TabsLink. Each link is a
 * real link with its own URL, so it uses link semantics (`aria-current`), not
 * the tab role, and Tab moves between links.
 */
export const TabsNav = forwardRef<HTMLElement, TabsNavProps>(function TabsNav(
  { variant = 'line', scrollable = false, className, ...rest },
  ref,
) {
  return (
    <nav
      ref={ref}
      {...rest}
      data-variant={variant}
      data-scrollable={scrollable || undefined}
      className={cx('mn-tabs__list', 'mn-tabs__nav', className)}
    />
  );
});

export interface TabsLinkProps extends AnchorHTMLAttributes<HTMLAnchorElement>, CountProps {
  /** The current page. Sets `aria-current="page"` and the active style. */
  active?: boolean;
  /**
   * Render the child element (a router link such as Next.js `<Link>`) with
   * tab styling. Monarch does not import a router.
   */
  asChild?: boolean;
}

export const TabsLink = forwardRef<HTMLAnchorElement, TabsLinkProps>(function TabsLink(
  { active = false, asChild = false, count, countTone = 'neutral', className, children, ...rest },
  ref,
) {
  const Comp = asChild ? Slot : 'a';
  return (
    <Comp
      ref={ref}
      aria-current={active ? 'page' : undefined}
      {...rest}
      data-state={active ? 'active' : 'inactive'}
      className={cx('mn-tabs__trigger', 'mn-tabs__link', className)}
    >
      <Slottable>{children}</Slottable>
      <TabsCount count={count} tone={countTone} />
    </Comp>
  );
});

/* ------------------------------------------------------------------ */
/* Filter tabs (no panels)                                             */
/* ------------------------------------------------------------------ */

export interface TabsFilterOption {
  value: string;
  label: ReactNode;
  count?: ReactNode;
  countTone?: TabsCountTone;
  disabled?: boolean;
}

export interface TabsFilterProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'defaultValue' | 'onChange' | 'children'
> {
  /** Names the filter set, for example "Status". Required. */
  'aria-label': string;
  options: readonly TabsFilterOption[];
  value?: string;
  defaultValue?: string;
  onValueChange?: (value: string) => void;
  variant?: TabsVariant;
  scrollable?: boolean;
  /**
   * `automatic` (default) selects a tab when it receives focus; `manual`
   * waits for Enter or Space, for filters that are slow to apply.
   */
  activationMode?: 'automatic' | 'manual';
  /**
   * The filtered content. When given it is rendered in one shared
   * `tabpanel` labelled by the selected tab, and every tab points at it with
   * `aria-controls`. Leave it out when the results live elsewhere on the page.
   */
  children?: ReactNode;
  /** Class for the shared panel. */
  panelClassName?: string;
}

/**
 * Tabs that filter one list instead of switching panels: "All 24 · Open 6 ·
 * Closed 18". Same keyboard model as Tabs (arrow keys, Home/End, roving
 * focus) and the same look, with one shared, optional panel.
 */
export const TabsFilter = forwardRef<HTMLDivElement, TabsFilterProps>(function TabsFilter(
  {
    options,
    value: valueProp,
    defaultValue,
    onValueChange,
    variant = 'line',
    scrollable = false,
    activationMode = 'automatic',
    children,
    panelClassName,
    className,
    id,
    'aria-label': label,
    ...rest
  },
  ref,
) {
  const auto = useId();
  const base = id ?? `mn-tabs-filter-${auto}`;
  const panelId = `${base}-panel`;
  const tabId = (v: string) => `${base}-tab-${v.replace(/[^\w-]/g, '_')}`;
  const firstEnabled = options.find((o) => !o.disabled)?.value ?? '';
  const [inner, setInner] = useState(defaultValue ?? firstEnabled);
  const value = valueProp ?? inner;
  const hasPanel = children !== undefined && children !== null && children !== false;
  const buttons = useRef<Map<string, HTMLButtonElement>>(new Map());
  // The tab in the Tab order: the selected one, or the first enabled tab
  // when the value matches no option.
  const tabbable = options.some((o) => o.value === value && !o.disabled) ? value : firstEnabled;

  const select = useCallback(
    (next: string) => {
      if (next === value) return;
      if (valueProp === undefined) setInner(next);
      onValueChange?.(next);
    },
    [value, valueProp, onValueChange],
  );

  const onKeyDown = (event: KeyboardEvent<HTMLButtonElement>, current: string) => {
    const enabled = options.filter((o) => !o.disabled);
    const i = enabled.findIndex((o) => o.value === current);
    const rtl = getComputedStyle(event.currentTarget).direction === 'rtl';
    let target: TabsFilterOption | undefined;
    switch (event.key) {
      case 'ArrowRight':
        target = enabled[(i + (rtl ? -1 : 1) + enabled.length) % enabled.length];
        break;
      case 'ArrowLeft':
        target = enabled[(i + (rtl ? 1 : -1) + enabled.length) % enabled.length];
        break;
      case 'Home':
        target = enabled[0];
        break;
      case 'End':
        target = enabled[enabled.length - 1];
        break;
      default:
        return;
    }
    if (!target) return;
    event.preventDefault();
    buttons.current.get(target.value)?.focus();
    if (activationMode === 'automatic') select(target.value);
  };

  return (
    <div ref={ref} {...rest} id={base} className={cx('mn-tabs-filter', className)}>
      <div
        role="tablist"
        aria-label={label}
        aria-orientation="horizontal"
        data-variant={variant}
        data-scrollable={scrollable || undefined}
        className="mn-tabs__list"
      >
        {options.map((o) => {
          const selected = o.value === value;
          return (
            <button
              key={o.value}
              ref={(el) => {
                if (el) buttons.current.set(o.value, el);
                else buttons.current.delete(o.value);
              }}
              type="button"
              role="tab"
              id={tabId(o.value)}
              aria-selected={selected}
              aria-controls={hasPanel ? panelId : undefined}
              tabIndex={o.value === tabbable ? 0 : -1}
              disabled={o.disabled}
              data-state={selected ? 'active' : 'inactive'}
              data-disabled={o.disabled ? '' : undefined}
              className="mn-tabs__trigger"
              onClick={() => select(o.value)}
              onKeyDown={(e) => onKeyDown(e, o.value)}
            >
              {o.label}
              <TabsCount count={o.count} tone={o.countTone ?? 'neutral'} />
            </button>
          );
        })}
      </div>
      {hasPanel ? (
        <div
          role="tabpanel"
          id={panelId}
          aria-labelledby={tabId(value)}
          // APG: a tab panel is focusable so its content can be reached by keyboard.
          // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
          tabIndex={0}
          className={cx('mn-tabs__content', panelClassName)}
        >
          {children}
        </div>
      ) : null}
    </div>
  );
});

/* ------------------------------------------------------------------ */
/* URL binding                                                         */
/* ------------------------------------------------------------------ */

export interface UseTabsUrlBindingOptions<V extends string> {
  /** The raw value from the URL (a search param or route segment); null or undefined when absent. */
  value: string | null | undefined;
  /** Writes the value back to the URL. Receives null to remove the param (see `clearDefault`). */
  onChange: (value: V | null) => void;
  /** Every allowed value. Anything else in the URL is treated as absent. */
  values: readonly V[];
  /** Used when the URL has no value or an unknown one. */
  defaultValue: V;
  /** Write null instead of `defaultValue`, so the default tab has a clean URL. Default true. */
  clearDefault?: boolean;
}

/**
 * Binds Tabs or TabsFilter to the URL without importing a router: the app
 * passes the param it read and a setter, and spreads the result.
 *
 *   const [params, setParams] = useSearchParams();
 *   const tabs = useTabsUrlBinding({
 *     value: params.get('tab'),
 *     onChange: (v) => setParams(v ? { tab: v } : {}),
 *     values: ['overview', 'activity'],
 *     defaultValue: 'overview',
 *   });
 *   <Tabs {...tabs}>…</Tabs>
 */
export function useTabsUrlBinding<V extends string>({
  value,
  onChange,
  values,
  defaultValue,
  clearDefault = true,
}: UseTabsUrlBindingOptions<V>): { value: V; onValueChange: (value: string) => void } {
  const current = (values as readonly string[]).includes(value ?? '') ? (value as V) : defaultValue;
  const onValueChange = useCallback(
    (next: string) => {
      if (!(values as readonly string[]).includes(next)) return;
      onChange(clearDefault && next === defaultValue ? null : (next as V));
    },
    [values, onChange, clearDefault, defaultValue],
  );
  return { value: current, onValueChange };
}
