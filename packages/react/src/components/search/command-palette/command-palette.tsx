'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type ElementRef,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph, SearchGlyph } from '../../../internal/icons.js';
import { Kbd } from '../../typography/kbd/index.js';
import { Spinner } from '../../feedback/spinner/index.js';
import { Dialog, DialogContent, DialogTitle, DialogTrigger } from '../../overlays/dialog/index.js';
import { formatHotkey, useHotkey, type Hotkey } from './command-palette.hotkey.js';

export {
  formatHotkey,
  matchesHotkey,
  isTypingTarget,
  useHotkey,
  type Hotkey,
  type UseHotkeyOptions,
} from './command-palette.hotkey.js';

/** One command or result in the palette. */
export interface CommandPaletteItem {
  /** Unique within the palette. */
  id: string;
  /** The name shown and searched. */
  label: string;
  /** Secondary text after the label, such as a path or record number. Also searched. */
  description?: string;
  /** Leading icon. Decorative. */
  icon?: ReactNode;
  /** Group heading, such as "Go to" or "Actions". Items without one come first, ungrouped. */
  group?: string;
  /** Extra words that match this item, such as synonyms ("delete" for "Archive"). */
  keywords?: readonly string[];
  /**
   * The shortcut hint shown at the end of the row: a hotkey string such as
   * `mod+shift+n`, or the keys to show (`['G', 'I']` for a sequence).
   * Display only: the app binds the shortcut itself (see `useHotkey`).
   */
  shortcut?: string | readonly string[];
  /** Opens this nested page (a key of `pages`) instead of running a command. */
  page?: string;
  disabled?: boolean;
  /** Draws the row in the danger colour. */
  tone?: 'danger';
  /** Keep the palette open after this item runs. */
  keepOpen?: boolean;
  /** Runs when the item is chosen with Enter or a click. */
  onSelect?: (item: CommandPaletteItem) => void;
}

/** Results fetched as the user types, such as records from the app's search API. */
export interface CommandPaletteSource {
  /** Unique within the palette. */
  id: string;
  /** Heading for this source's results. */
  group: string;
  /**
   * Fetches results for the query. Abort your request when `signal` fires:
   * it does when the query changes or the palette closes.
   */
  search: (query: string, signal: AbortSignal) => Promise<readonly CommandPaletteItem[]>;
  /** Shortest query (trimmed) that triggers a search. Default `1`. */
  minQueryLength?: number;
  /** Wait this long after the last keystroke before searching. Default `200` ms. */
  debounceMs?: number;
}

/** A nested page, opened by an item with `page`, such as "Move to…". */
export interface CommandPalettePage {
  /** Shown as a chip before the input, and announced. */
  title: string;
  /** Input placeholder on this page. */
  placeholder?: string;
  items?: readonly CommandPaletteItem[];
  sources?: readonly CommandPaletteSource[];
}

/** Visible and announced strings. Override them to translate. */
export interface CommandPaletteMessages {
  /** Accessible name of the dialog. */
  title: string;
  placeholder: string;
  /** Accessible name of the result list. */
  results: string;
  /** Heading of the `recent` group. */
  recent: string;
  /** Shown when the query matches nothing. */
  empty: string;
  /** Announced while a source is searching. */
  loading: string;
  resultCount: (count: number) => string;
  sourceError: (group: string) => string;
  retry: string;
  /** Accessible name of the page chip's back button. */
  back: (page: string) => string;
  navigate: string;
  select: string;
  close: string;
}

const DEFAULT_MESSAGES: CommandPaletteMessages = {
  title: 'Command palette',
  placeholder: 'Type a command or search…',
  results: 'Results',
  recent: 'Recent',
  empty: 'No results',
  loading: 'Searching…',
  resultCount: (n) => (n === 1 ? '1 result' : `${n} results`),
  sourceError: (group) => `Could not load ${group.toLowerCase()}.`,
  retry: 'Retry',
  back: (page) => `Back from ${page}`,
  navigate: 'to navigate',
  select: 'to select',
  close: 'to close',
};

export interface CommandPaletteProps {
  /** Commands shown on the first page. Filtered as the user types. */
  items?: readonly CommandPaletteItem[];
  /** Asynchronous result sources for the first page. */
  sources?: readonly CommandPaletteSource[];
  /** Nested pages by key, opened by items with `page`. */
  pages?: Readonly<Record<string, CommandPalettePage>>;
  /**
   * Shown in a "Recent" group while the query is empty, on the first page.
   * The app keeps this list (for example in localStorage) from `onItemSelect`.
   */
  recent?: readonly CommandPaletteItem[];
  /** Controlled open state. */
  open?: boolean;
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  /** Controlled query. */
  search?: string;
  onSearchChange?: (search: string) => void;
  /**
   * Opens and closes the palette from anywhere. Default `mod+k` (⌘K or Ctrl+K).
   * Pass `false` to bind it yourself.
   */
  hotkey?: Hotkey | false;
  /**
   * Whether a modified hotkey (⌘, Ctrl or Alt) also works while typing in another
   * field. Default `true`. Single-key hotkeys such as `/` never fire while typing.
   */
  hotkeyInFields?: boolean;
  /**
   * Ranks an item for the query: 0 hides it, higher sorts first within its group.
   * Default: every word of the query must appear in the label, description or keywords;
   * label prefix matches rank highest.
   */
  filter?: (item: CommandPaletteItem, query: string) => number;
  /** Called for every chosen item, after its own `onSelect`, with the page it was on. */
  onItemSelect?: (item: CommandPaletteItem, context: { page: string | null }) => void;
  /**
   * An element (usually a button) that opens the palette. Rendered with Radix `asChild`, so it must
   * forward its ref and props to a DOM element, as Monarch Button does.
   */
  trigger?: ReactNode;
  /**
   * Content at the bottom of the palette. Default: keyboard hints
   * (↑↓ to navigate, ↵ to select, Esc to close). Pass `null` for none.
   */
  footer?: ReactNode;
  /** Shown instead of `messages.empty` when nothing matches. */
  emptyState?: ReactNode;
  /** Override visible and announced strings. */
  messages?: Partial<CommandPaletteMessages>;
  /** Class on the dialog content. */
  className?: string;
}

interface Row {
  item: CommandPaletteItem;
  domId: string;
}
interface Section {
  key: string;
  heading?: string | undefined;
  rows: Row[];
}
interface SourceState {
  status: 'idle' | 'loading' | 'done' | 'error';
  query: string;
  items: readonly CommandPaletteItem[];
}

const NO_ITEMS: readonly CommandPaletteItem[] = [];
const NO_SOURCES: readonly CommandPaletteSource[] = [];
const NO_PAGES: Readonly<Record<string, CommandPalettePage>> = {};

function normalise(text: string) {
  return text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}

/** The default ranking: all words must match; label prefix > word start > anywhere > keywords. */
export function defaultCommandFilter(item: CommandPaletteItem, query: string): number {
  const words = normalise(query).split(/\s+/).filter(Boolean);
  if (words.length === 0) return 1;
  const label = normalise(item.label);
  const rest = normalise([item.description ?? '', ...(item.keywords ?? [])].join(' '));
  let score = 0;
  for (const w of words) {
    if (label.startsWith(w)) score += 4;
    else if (new RegExp(`(^|[\\s\\-_/.])${w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}`).test(label))
      score += 3;
    else if (label.includes(w)) score += 2;
    else if (rest.includes(w)) score += 1;
    else return 0;
  }
  return score;
}

function shortcutKeys(shortcut: string | readonly string[]): readonly string[] {
  return typeof shortcut === 'string' ? formatHotkey(shortcut) : shortcut;
}

/**
 * A ⌘K / Ctrl+K command palette: a modal dialog with a search combobox and a
 * grouped listbox of commands, recent items and results from asynchronous
 * sources, nested pages such as "Move to…", and shortcut hints. The app
 * supplies every command and search; the palette owns keyboard, focus,
 * filtering, debouncing, cancellation and announcements.
 */
export const CommandPalette = forwardRef<ElementRef<typeof DialogContent>, CommandPaletteProps>(
  function CommandPalette(
    {
      items = NO_ITEMS,
      sources = NO_SOURCES,
      pages = NO_PAGES,
      recent = NO_ITEMS,
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      search: searchProp,
      onSearchChange,
      hotkey = 'mod+k',
      hotkeyInFields = true,
      filter = defaultCommandFilter,
      onItemSelect,
      trigger,
      footer,
      emptyState,
      messages: messagesProp,
      className,
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const id = useId();
    const listId = `mn-command-palette-${id}-list`;
    const inputRef = useRef<HTMLInputElement | null>(null);
    const listRef = useRef<HTMLDivElement | null>(null);

    const [internalOpen, setInternalOpen] = useState(defaultOpen);
    const open = openProp ?? internalOpen;
    const setOpen = useCallback(
      (next: boolean) => {
        if (openProp === undefined) setInternalOpen(next);
        onOpenChange?.(next);
      },
      [openProp, onOpenChange],
    );

    const [internalSearch, setInternalSearch] = useState('');
    const search = searchProp ?? internalSearch;
    const setSearch = (next: string) => {
      if (searchProp === undefined) setInternalSearch(next);
      onSearchChange?.(next);
    };

    /** The stack of open nested pages, by key. */
    const [stack, setStack] = useState<string[]>([]);
    const pageKey = stack[stack.length - 1] ?? null;
    const page = pageKey ? pages[pageKey] : undefined;

    const [activeId, setActiveId] = useState<string | null>(null);
    const [sourceStates, setSourceStates] = useState<Record<string, SourceState>>({});
    const [retryCount, setRetryCount] = useState(0);

    useHotkey(hotkey, () => setOpen(!open), { inFields: hotkeyInFields });

    // Every open starts on the first page with an empty query.
    useEffect(() => {
      if (!open) return;
      setStack([]);
      setSourceStates({});
      if (searchProp === undefined) setInternalSearch('');
      else if (searchProp !== '') onSearchChange?.('');
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open]);

    const query = search.trim();
    const pageItems = useMemo(() => (page ? (page.items ?? NO_ITEMS) : items), [page, items]);
    const pageSources = useMemo(
      () => (page ? (page.sources ?? NO_SOURCES) : sources),
      [page, sources],
    );

    // Asynchronous sources: debounce, then search with an AbortSignal; a newer
    // query, a page change or closing aborts the request in flight.
    const sourceKey = pageSources.map((s) => s.id).join('\n');
    useEffect(() => {
      if (!open) return;
      const controllers: AbortController[] = [];
      const timers: ReturnType<typeof setTimeout>[] = [];
      for (const source of pageSources) {
        const min = source.minQueryLength ?? 1;
        if (query.length < min) {
          setSourceStates((s) =>
            s[source.id]?.status === 'idle'
              ? s
              : { ...s, [source.id]: { status: 'idle', query, items: [] } },
          );
          continue;
        }
        setSourceStates((s) => ({
          ...s,
          [source.id]: { status: 'loading', query, items: s[source.id]?.items ?? [] },
        }));
        const controller = new AbortController();
        controllers.push(controller);
        timers.push(
          setTimeout(() => {
            source.search(query, controller.signal).then(
              (results) => {
                if (controller.signal.aborted) return;
                setSourceStates((s) => ({
                  ...s,
                  [source.id]: { status: 'done', query, items: results },
                }));
              },
              () => {
                if (controller.signal.aborted) return;
                setSourceStates((s) => ({
                  ...s,
                  [source.id]: { status: 'error', query, items: [] },
                }));
              },
            );
          }, source.debounceMs ?? 200),
        );
      }
      return () => {
        timers.forEach(clearTimeout);
        controllers.forEach((c) => c.abort());
      };
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open, query, pageKey, sourceKey, retryCount]);

    const sections = useMemo<Section[]>(() => {
      const out: Section[] = [];
      let n = 0;
      const row = (item: CommandPaletteItem): Row => ({
        item,
        domId: `${listId}-${n++}`,
      });
      if (!page && query === '' && recent.length > 0) {
        out.push({ key: 'recent', heading: messages.recent, rows: recent.map(row) });
      }
      const ranked = pageItems
        .map((item, index) => ({ item, index, score: query === '' ? 1 : filter(item, query) }))
        .filter((r) => r.score > 0);
      const byGroup = new Map<string, typeof ranked>();
      for (const r of ranked) {
        const g = r.item.group ?? '';
        const list = byGroup.get(g);
        if (list) list.push(r);
        else byGroup.set(g, [r]);
      }
      // Ungrouped items first, then groups in the order they first appear.
      const groups = [...byGroup.keys()].sort((a, b) => (a === '' ? -1 : b === '' ? 1 : 0));
      for (const g of groups) {
        const list = (byGroup.get(g) ?? [])
          .slice()
          .sort((a, b) => b.score - a.score || a.index - b.index);
        out.push({
          key: `group:${g}`,
          heading: g || undefined,
          rows: list.map((r) => row(r.item)),
        });
      }
      for (const source of pageSources) {
        const state = sourceStates[source.id];
        if (!state || state.items.length === 0) continue;
        out.push({ key: `source:${source.id}`, heading: source.group, rows: state.items.map(row) });
      }
      return out;
    }, [
      page,
      query,
      recent,
      pageItems,
      pageSources,
      sourceStates,
      filter,
      listId,
      messages.recent,
    ]);

    const rows = useMemo(() => sections.flatMap((s) => s.rows), [sections]);
    const enabled = useMemo(() => rows.filter((r) => !r.item.disabled), [rows]);
    const loading = pageSources.some((s) => sourceStates[s.id]?.status === 'loading');
    const failed = pageSources.filter((s) => sourceStates[s.id]?.status === 'error');

    // Keep the active row when it survives a change of results, else the first enabled row.
    const activeRow = enabled.find((r) => r.item.id === activeId) ?? enabled[0] ?? null;
    const activeDomId = activeRow?.domId;
    useEffect(() => {
      if (activeRow && activeRow.item.id !== activeId) setActiveId(activeRow.item.id);
    }, [activeRow, activeId]);

    // Typing starts again from the top.
    useEffect(() => {
      setActiveId(null);
    }, [query, pageKey]);

    useEffect(() => {
      if (!activeDomId) return;
      const el = listRef.current?.querySelector<HTMLElement>(`[id="${activeDomId}"]`);
      el?.scrollIntoView?.({ block: 'nearest' });
    }, [activeDomId]);

    const moveActive = (delta: number) => {
      if (enabled.length === 0) return;
      const at = activeRow ? enabled.indexOf(activeRow) : -1;
      const next = enabled[(at + delta + enabled.length) % enabled.length];
      if (next) setActiveId(next.item.id);
    };

    const pushPage = (key: string) => {
      setStack((s) => [...s, key]);
      setSearch('');
      inputRef.current?.focus();
    };
    const popPage = () => {
      setStack((s) => s.slice(0, -1));
      setSearch('');
      inputRef.current?.focus();
    };

    const choose = (item: CommandPaletteItem) => {
      if (item.disabled) return;
      item.onSelect?.(item);
      onItemSelect?.(item, { page: pageKey });
      if (item.page && pages[item.page]) pushPage(item.page);
      else if (!item.keepOpen) setOpen(false);
    };

    const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
      if (e.nativeEvent.isComposing) return;
      switch (e.key) {
        case 'ArrowDown':
          e.preventDefault();
          moveActive(1);
          break;
        case 'ArrowUp':
          e.preventDefault();
          moveActive(-1);
          break;
        case 'PageDown':
        case 'PageUp': {
          e.preventDefault();
          if (enabled.length === 0) break;
          const at = activeRow ? enabled.indexOf(activeRow) : 0;
          const to = Math.max(
            0,
            Math.min(enabled.length - 1, at + (e.key === 'PageDown' ? 5 : -5)),
          );
          setActiveId(enabled[to]?.item.id ?? null);
          break;
        }
        case 'Enter':
          if (activeRow) {
            e.preventDefault();
            choose(activeRow.item);
          }
          break;
        case 'Backspace':
          if (search === '' && stack.length > 0) {
            e.preventDefault();
            popPage();
          }
          break;
      }
    };

    // Announce the number of results once results settle.
    const [announcement, setAnnouncement] = useState('');
    const showEmpty = query !== '' && rows.length === 0 && !loading && failed.length === 0;
    useEffect(() => {
      if (!open) return;
      const t = setTimeout(() => {
        if (loading) setAnnouncement(messages.loading);
        else if (query === '' && !page) setAnnouncement('');
        else setAnnouncement(rows.length ? messages.resultCount(rows.length) : messages.empty);
      }, 150);
      return () => clearTimeout(t);
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open, loading, rows.length, query, pageKey]);

    const defaultFooter = (
      <div className="mn-command-palette__hints" aria-hidden>
        <span>
          <Kbd>↑</Kbd>
          <Kbd>↓</Kbd> {messages.navigate}
        </span>
        <span>
          <Kbd>↵</Kbd> {messages.select}
        </span>
        <span>
          <Kbd>Esc</Kbd> {messages.close}
        </span>
      </div>
    );
    const footerContent = footer === undefined ? defaultFooter : footer;

    return (
      <Dialog open={open} onOpenChange={setOpen}>
        {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
        <DialogContent
          ref={ref}
          aria-describedby={undefined}
          closeLabel={null}
          escapeGuard={false}
          fullScreenOnPhone
          className={cx('mn-command-palette', className)}
          data-page={pageKey ?? undefined}
          onOpenAutoFocus={(e) => {
            e.preventDefault();
            inputRef.current?.focus();
          }}
          onEscapeKeyDown={(e) => {
            // Escape leaves a nested page first, then closes.
            if (stack.length > 0) {
              e.preventDefault();
              popPage();
            }
          }}
        >
          <DialogTitle className="mn-visually-hidden">{messages.title}</DialogTitle>
          <div className="mn-command-palette__search">
            <span className="mn-command-palette__search-icon" aria-hidden>
              {loading ? <Spinner size="sm" /> : <SearchGlyph />}
            </span>
            {page ? (
              <button
                type="button"
                className="mn-command-palette__page"
                aria-label={messages.back(page.title)}
                onClick={popPage}
              >
                {page.title}
                <ChevronRightGlyph />
              </button>
            ) : null}
            <input
              ref={inputRef}
              type="text"
              role="combobox"
              aria-expanded
              aria-controls={listId}
              aria-activedescendant={activeDomId}
              aria-autocomplete="list"
              aria-label={page ? `${messages.title}: ${page.title}` : messages.title}
              autoComplete="off"
              autoCorrect="off"
              spellCheck={false}
              enterKeyHint="go"
              className="mn-command-palette__input"
              placeholder={page?.placeholder ?? messages.placeholder}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              onKeyDown={onKeyDown}
            />
          </div>
          <div
            ref={listRef}
            id={listId}
            role="listbox"
            aria-label={messages.results}
            className="mn-command-palette__list"
          >
            {sections.map((section, sectionIndex) => {
              const headingId = `${listId}-heading-${sectionIndex}`;
              const options = section.rows.map(({ item, domId }) => (
                // The input keeps focus and handles every key (aria-activedescendant),
                // so options are deliberately not focusable.
                // monarch-ignore-next-line a11y/click-without-key -- aria-activedescendant pattern: the input handles all keys
                // eslint-disable-next-line jsx-a11y/click-events-have-key-events
                <div
                  key={`${section.key}:${item.id}`}
                  id={domId}
                  role="option"
                  tabIndex={-1}
                  aria-selected={domId === activeDomId}
                  aria-disabled={item.disabled || undefined}
                  data-highlighted={domId === activeDomId || undefined}
                  data-tone={item.tone}
                  className="mn-command-palette__item"
                  onPointerMove={() => {
                    if (!item.disabled && activeId !== item.id) setActiveId(item.id);
                  }}
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => choose(item)}
                >
                  {item.icon ? (
                    <span className="mn-command-palette__icon" aria-hidden>
                      {item.icon}
                    </span>
                  ) : null}
                  <span className="mn-command-palette__text">
                    <span className="mn-command-palette__label">{item.label}</span>
                    {item.description ? (
                      <span className="mn-command-palette__description">{item.description}</span>
                    ) : null}
                  </span>
                  {item.shortcut ? (
                    <span className="mn-command-palette__shortcut" aria-hidden>
                      {shortcutKeys(item.shortcut).map((k, i) => (
                        <Kbd key={i}>{k}</Kbd>
                      ))}
                    </span>
                  ) : null}
                  {item.page ? (
                    <span className="mn-command-palette__chevron" aria-hidden>
                      <ChevronRightGlyph />
                    </span>
                  ) : null}
                </div>
              ));
              return section.heading ? (
                <div
                  key={section.key}
                  role="group"
                  aria-labelledby={headingId}
                  className="mn-command-palette__group"
                >
                  <div id={headingId} role="presentation" className="mn-command-palette__heading">
                    {section.heading}
                  </div>
                  {options}
                </div>
              ) : (
                <div key={section.key} role="presentation" className="mn-command-palette__group">
                  {options}
                </div>
              );
            })}
          </div>
          {showEmpty ? (
            <div className="mn-command-palette__empty">{emptyState ?? messages.empty}</div>
          ) : null}
          {failed.map((source) => (
            <div key={source.id} className="mn-command-palette__error">
              <span>{messages.sourceError(source.group)}</span>
              <button
                type="button"
                className="mn-command-palette__retry"
                onClick={() => {
                  setRetryCount((n) => n + 1);
                  inputRef.current?.focus();
                }}
              >
                {messages.retry}
              </button>
            </div>
          ))}
          <div className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
            {announcement}
          </div>
          {footerContent !== null ? (
            <div className="mn-command-palette__footer">{footerContent}</div>
          ) : null}
        </DialogContent>
      </Dialog>
    );
  },
);
