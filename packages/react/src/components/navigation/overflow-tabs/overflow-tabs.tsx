'use client';

import {
  forwardRef,
  useRef,
  useState,
  type ComponentPropsWithoutRef,
  type ElementRef,
  type ReactNode,
} from 'react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';
import {
  Tabs,
  TabsList,
  TabsTrigger,
  type TabsCountTone,
  type TabsVariant,
} from '../tabs/index.js';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import { useOverflowItems } from '../../../internal/overflow.js';

export interface OverflowTabsItem {
  /** Matches the `value` of its TabsContent panel. */
  value: string;
  label: ReactNode;
  /** Count bubble after the label, read as part of the tab's name. */
  count?: ReactNode;
  /** Colour of the count bubble. Default `neutral`. */
  countTone?: TabsCountTone;
  disabled?: boolean;
}

export interface OverflowTabsMessages {
  /** The overflow menu button. Default "More". */
  more: string;
}

export const OVERFLOW_TABS_MESSAGES: OverflowTabsMessages = { more: 'More' };

export interface OverflowTabsProps extends Omit<
  ComponentPropsWithoutRef<typeof Tabs>,
  'children' | 'orientation'
> {
  /** The tabs, in priority order: the last ones move into "More" first. */
  tabs: readonly OverflowTabsItem[];
  /** Names the tab list, for example "Project sections". Required. */
  'aria-label': string;
  /** `line` (default) for page sections, `pill` for compact view switches. */
  variant?: TabsVariant;
  /** The panels: one TabsContent per tab, with matching `value`. */
  children?: ReactNode;
  /** Class for the TabsList. */
  listClassName?: string;
  /** Visible and spoken strings, for translation. */
  messages?: Partial<OverflowTabsMessages> | undefined;
}

const textOf = (label: ReactNode) =>
  typeof label === 'string' || typeof label === 'number' ? String(label) : undefined;

/**
 * Tabs that keep to one row: tabs that do not fit move into a "More" menu
 * as the width shrinks, and choosing one there selects it and brings it
 * into the row. The selected tab is always visible. Built on Tabs, so
 * panels are ordinary TabsContent and the keyboard model is the same.
 */
export const OverflowTabs = forwardRef<ElementRef<typeof Tabs>, OverflowTabsProps>(
  function OverflowTabs(
    {
      tabs,
      value: valueProp,
      defaultValue,
      onValueChange,
      variant = 'line',
      'aria-label': label,
      children,
      listClassName,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...OVERFLOW_TABS_MESSAGES, ...messagesProp };
    const firstEnabled = tabs.find((t) => !t.disabled)?.value ?? '';
    const [inner, setInner] = useState(defaultValue ?? firstEnabled);
    const value = valueProp ?? inner;
    const select = (next: string) => {
      if (valueProp === undefined) setInner(next);
      onValueChange?.(next);
    };

    const selectedIndex = tabs.findIndex((t) => t.value === value);
    const { containerRef, measureRef, visible } = useOverflowItems(
      tabs.length,
      selectedIndex,
      tabs
        .map((t) => `${t.value}:${textOf(t.label) ?? ''}:${textOf(t.count as ReactNode) ?? ''}`)
        .join('|'),
    );
    const shown = visible ?? tabs.map((_, i) => i);
    const overflow = tabs.map((_, i) => i).filter((i) => !shown.includes(i));

    // After choosing a tab from the menu, focus it in the row instead of More.
    const listRef = useRef<HTMLDivElement>(null);
    const focusSelected = useRef(false);

    return (
      <Tabs
        ref={ref}
        {...rest}
        value={value}
        onValueChange={select}
        orientation="horizontal"
        className={cx('mn-overflow-tabs', className)}
      >
        <div
          ref={containerRef}
          className="mn-overflow-tabs__bar"
          data-variant={variant}
          data-overflowing={overflow.length > 0 || undefined}
        >
          <TabsList
            ref={listRef}
            variant={variant}
            aria-label={label}
            className={cx('mn-overflow-tabs__list', listClassName)}
          >
            {shown.map((i) => {
              const t = tabs[i] as OverflowTabsItem;
              return (
                <TabsTrigger
                  key={t.value}
                  value={t.value}
                  disabled={t.disabled}
                  count={t.count}
                  countTone={t.countTone ?? 'neutral'}
                >
                  {t.label}
                </TabsTrigger>
              );
            })}
          </TabsList>
          {overflow.length > 0 ? (
            <DropdownMenu>
              <DropdownMenuTrigger className="mn-tabs__trigger mn-overflow-tabs__more">
                {messages.more}
                <span className="mn-overflow-tabs__chevron" aria-hidden>
                  <ChevronDownGlyph />
                </span>
              </DropdownMenuTrigger>
              <DropdownMenuContent
                align="end"
                onCloseAutoFocus={(event) => {
                  if (!focusSelected.current) return;
                  focusSelected.current = false;
                  const tab = listRef.current?.querySelector<HTMLElement>(
                    '[role="tab"][data-state="active"]',
                  );
                  if (!tab) return;
                  event.preventDefault();
                  tab.focus();
                }}
              >
                {overflow.map((i) => {
                  const t = tabs[i] as OverflowTabsItem;
                  const text = textOf(t.label);
                  return (
                    <DropdownMenuItem
                      key={t.value}
                      disabled={t.disabled}
                      {...(text !== undefined ? { textValue: text } : {})}
                      onSelect={() => {
                        focusSelected.current = true;
                        select(t.value);
                      }}
                    >
                      {t.label}
                      {t.count !== undefined && t.count !== null && t.count !== false ? (
                        <span className="mn-overflow-tabs__menu-count"> {t.count}</span>
                      ) : null}
                    </DropdownMenuItem>
                  );
                })}
              </DropdownMenuContent>
            </DropdownMenu>
          ) : null}
          <div
            ref={measureRef}
            className="mn-tabs__list mn-overflow-tabs__measure"
            data-variant={variant}
            aria-hidden
          >
            {tabs.map((t) => (
              <span key={t.value} className="mn-tabs__trigger">
                {t.label}
                {t.count !== undefined &&
                t.count !== null &&
                t.count !== false &&
                t.count !== '' ? (
                  <span className="mn-tabs__count">{t.count}</span>
                ) : null}
              </span>
            ))}
            <span className="mn-tabs__trigger mn-overflow-tabs__more">
              {messages.more}
              <span className="mn-overflow-tabs__chevron">
                <ChevronDownGlyph />
              </span>
            </span>
          </div>
        </div>
        {children}
      </Tabs>
    );
  },
);
