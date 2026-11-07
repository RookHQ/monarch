'use client';

import { forwardRef, useMemo, useState, type ElementRef, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { SearchGlyph } from '../../../internal/icons.js';
import {
  CommandPalette,
  defaultCommandFilter,
  type CommandPaletteItem,
  type CommandPaletteMessages,
  type CommandPaletteSource,
  type Hotkey,
} from '../command-palette/index.js';

/** A place the person can jump to: a conversation, project, document or page. */
export interface QuickSwitcherDestination {
  /** Stable, unique id. */
  id: string;
  /** Name shown and matched, e.g. "#general" or "Website refresh". */
  label: string;
  /** Second line, e.g. "3 unread" or the parent space. Also matched. */
  description?: string;
  /** Leading icon or avatar. Decorative. */
  icon?: ReactNode;
  /** Group heading, e.g. "Channels" or "People". */
  group?: string;
  /** Extra words that should match, e.g. a person's email. */
  keywords?: readonly string[];
  /** Where it goes. Passed back in `onNavigate`; the product does the routing. */
  href?: string;
  disabled?: boolean;
}

/** Visible and announced strings, for translation. */
export interface QuickSwitcherMessages extends CommandPaletteMessages {
  /** Label of the "search everywhere" row. Default: Search for “{query}”. */
  searchAll: (query: string) => string;
  /** Group heading of the "search everywhere" row. Default "Search". */
  searchAllGroup: string;
}

/** Jump-to defaults; other strings come from CommandPalette's defaults. */
export const QUICK_SWITCHER_MESSAGES: Pick<
  QuickSwitcherMessages,
  'title' | 'placeholder' | 'empty' | 'select' | 'searchAll' | 'searchAllGroup'
> = {
  title: 'Jump to',
  placeholder: 'Jump to…',
  empty: 'No matches',
  select: 'to open',
  searchAll: (query) => `Search for “${query}”`,
  searchAllGroup: 'Search',
};

const SEARCH_ALL_ID = '__mn-quick-switcher-search-all';

/**
 * The default ranking: CommandPalette's word matching, then a looser
 * in-order letter match ("gnl" finds "general") ranked below it.
 */
export function quickSwitcherFilter(item: CommandPaletteItem, query: string): number {
  if (item.id === SEARCH_ALL_ID) return 1;
  const score = defaultCommandFilter(item, query);
  if (score > 0) return score + 1;
  const text = item.label.toLowerCase();
  let i = 0;
  for (const ch of query.toLowerCase().replace(/\s+/g, '')) {
    i = text.indexOf(ch, i);
    if (i < 0) return 0;
    i += 1;
  }
  return 1;
}

export interface QuickSwitcherProps {
  /** Every destination that can be matched locally, e.g. the person's conversations. */
  destinations?: readonly QuickSwitcherDestination[];
  /** Shown under "Recent" while the query is empty. */
  recent?: readonly QuickSwitcherDestination[];
  /**
   * Remote searches (people, messages, files), debounced and cancelled by
   * CommandPalette. Their results reach `onNavigate` as `{ id, label,
   * description }`, so look them up by id, or give each result its
   * own `onSelect`.
   */
  sources?: readonly CommandPaletteSource[];
  /**
   * Called with the chosen destination (local, recent or from a source). The
   * product routes to `href` or opens what it needs. The switcher closes.
   */
  onNavigate?: ((destination: QuickSwitcherDestination) => void) | undefined;
  /**
   * Adds a last row, "Search for “query”", once the query has
   * `searchAllMinLength` characters. Called with the query.
   */
  onSearchAll?: ((query: string) => void) | undefined;
  /** Minimum query length for the "search everywhere" row. Default 2. */
  searchAllMinLength?: number;
  /** Controlled open state. */
  open?: boolean;
  defaultOpen?: boolean;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Opens and closes it from anywhere. Default `mod+k` (⌘K or Ctrl+K). `false` to bind it yourself. */
  hotkey?: Hotkey | false;
  /** An element (usually a button) that opens it, rendered with `asChild`. */
  trigger?: ReactNode;
  /** Ranks a destination for the query (0 hides it). Default `quickSwitcherFilter`. */
  filter?: (item: CommandPaletteItem, query: string) => number;
  /** Content at the bottom. Default: keyboard hints. `null` for none. */
  footer?: ReactNode;
  /** Shown instead of `messages.empty` when nothing matches. */
  emptyState?: ReactNode;
  /** Visible and announced strings, for translation. */
  messages?: Partial<QuickSwitcherMessages>;
  /** Class on the dialog content. */
  className?: string;
}

/** Drops `undefined` values so optional props pass through exactly. */
function definedOnly<T extends object>(props: T): { [K in keyof T]?: Exclude<T[K], undefined> } {
  return Object.fromEntries(Object.entries(props).filter(([, v]) => v !== undefined)) as {
    [K in keyof T]?: Exclude<T[K], undefined>;
  };
}

function toItem(d: QuickSwitcherDestination): CommandPaletteItem {
  return {
    id: d.id,
    label: d.label,
    ...(d.description !== undefined ? { description: d.description } : {}),
    ...(d.icon !== undefined ? { icon: d.icon } : {}),
    ...(d.group !== undefined ? { group: d.group } : {}),
    ...(d.keywords !== undefined ? { keywords: d.keywords } : {}),
    ...(d.disabled !== undefined ? { disabled: d.disabled } : {}),
  };
}

/**
 * A ⌘K "jump to" switcher: type part of a name and press Enter to go
 * there. A thin layer over CommandPalette with jump-to defaults (title,
 * placeholder, forgiving letter matching, recent destinations and an
 * optional "search everywhere" row). The product owns the data and the
 * routing through `onNavigate`.
 */
export const QuickSwitcher = forwardRef<ElementRef<typeof CommandPalette>, QuickSwitcherProps>(
  function QuickSwitcher(
    {
      destinations = [],
      recent = [],
      sources,
      onNavigate,
      onSearchAll,
      searchAllMinLength = 2,
      hotkey = 'mod+k',
      filter = quickSwitcherFilter,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const { searchAll, searchAllGroup, ...paletteMessages } = {
      ...QUICK_SWITCHER_MESSAGES,
      ...messagesProp,
    };
    const [query, setQuery] = useState('');
    const term = query.trim();

    const byId = useMemo(() => {
      const map = new Map<string, QuickSwitcherDestination>();
      for (const d of recent) map.set(d.id, d);
      for (const d of destinations) map.set(d.id, d);
      return map;
    }, [destinations, recent]);

    const items = useMemo(() => {
      const list = destinations.map(toItem);
      if (onSearchAll && term.length >= searchAllMinLength) {
        list.push({
          id: SEARCH_ALL_ID,
          label: searchAll(term),
          group: searchAllGroup,
          icon: <SearchGlyph />,
        });
      }
      return list;
    }, [destinations, onSearchAll, term, searchAllMinLength, searchAll, searchAllGroup]);

    const recentItems = useMemo(() => recent.map(toItem), [recent]);

    return (
      <CommandPalette
        ref={ref}
        {...definedOnly(rest)}
        items={items}
        recent={recentItems}
        {...(sources ? { sources } : {})}
        hotkey={hotkey}
        filter={filter}
        search={query}
        onSearchChange={setQuery}
        messages={paletteMessages}
        className={cx('mn-quick-switcher', className) ?? 'mn-quick-switcher'}
        onItemSelect={(item) => {
          if (item.id === SEARCH_ALL_ID) {
            onSearchAll?.(term);
            return;
          }
          const destination: QuickSwitcherDestination = byId.get(item.id) ?? {
            id: item.id,
            label: item.label,
            ...(item.description !== undefined ? { description: item.description } : {}),
            ...(item.group !== undefined ? { group: item.group } : {}),
          };
          onNavigate?.(destination);
        }}
      />
    );
  },
);
