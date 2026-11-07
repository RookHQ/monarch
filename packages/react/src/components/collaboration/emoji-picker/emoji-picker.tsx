'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactElement,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { SearchInput } from '../../search/search-input/index.js';
import { Popover, PopoverContent, PopoverTrigger } from '../../overlays/popover/index.js';
import {
  defaultEmojiCategories,
  type EmojiCategoryId,
  type EmojiPickerCategory,
  type EmojiPickerItem,
} from './emoji-picker.data.js';

export {
  defaultEmojiCategories,
  type EmojiCategoryId,
  type EmojiPickerCategory,
  type EmojiPickerItem,
} from './emoji-picker.data.js';

export type EmojiSkinTone = 'none' | 'light' | 'medium-light' | 'medium' | 'medium-dark' | 'dark';

const SKIN_TONES: readonly EmojiSkinTone[] = [
  'none',
  'light',
  'medium-light',
  'medium',
  'medium-dark',
  'dark',
];

const TONE_MODIFIERS: Record<EmojiSkinTone, string> = {
  none: '',
  light: '\u{1F3FB}',
  'medium-light': '\u{1F3FC}',
  medium: '\u{1F3FD}',
  'medium-dark': '\u{1F3FE}',
  dark: '\u{1F3FF}',
};

const MODIFIER_PATTERN = /\u{1F3FB}|\u{1F3FC}|\u{1F3FD}|\u{1F3FE}|\u{1F3FF}|\u{FE0F}/gu;

/**
 * The emoji with a skin tone modifier after its first code point (and any
 * variation selector removed). `none` returns the emoji unchanged.
 */
export function applySkinTone(emoji: string, tone: EmojiSkinTone): string {
  if (tone === 'none') return emoji;
  const first = String.fromCodePoint(emoji.codePointAt(0) ?? 0);
  const rest = emoji.slice(first.length).replace(MODIFIER_PATTERN, '');
  return `${first}${TONE_MODIFIERS[tone]}${rest}`;
}

function baseOf(emoji: string): string {
  return emoji.replace(MODIFIER_PATTERN, '');
}

/**
 * Emoji whose name or keywords match every word of `query` (each word as a
 * prefix of a name or keyword word), names that start with the query first.
 * Useful for `:shortcode` completion in a composer too.
 */
export function searchEmoji(
  categories: readonly EmojiPickerCategory[],
  query: string,
  limit = Infinity,
): EmojiPickerItem[] {
  const words = query.toLowerCase().trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return [];
  const seen = new Set<string>();
  const starts: EmojiPickerItem[] = [];
  const others: EmojiPickerItem[] = [];
  for (const category of categories) {
    for (const item of category.emojis) {
      if (seen.has(item.emoji)) continue;
      const name = item.name.toLowerCase();
      const tokens = [
        ...name.split(/[\s:,-]+/),
        ...(item.keywords ?? []).map((k) => k.toLowerCase()),
      ];
      if (!words.every((w) => tokens.some((t) => t.startsWith(w)))) continue;
      seen.add(item.emoji);
      (name.startsWith(words.join(' ')) ? starts : others).push(item);
    }
  }
  return [...starts, ...others].slice(0, limit);
}

/**
 * A new recents list with `emoji` first, without duplicates, at most `max`
 * long. Store the result wherever the product keeps preferences.
 */
export function pushRecentEmoji(recents: readonly string[], emoji: string, max = 24): string[] {
  return [emoji, ...recents.filter((e) => e !== emoji)].slice(0, Math.max(0, max));
}

export interface EmojiPickerMessages {
  /** Name of the picker group. Default "Emoji picker". */
  label: string;
  /** Accessible name of the search field. Default "Search emoji". */
  search: string;
  /** Placeholder of the search field. Default "Search emoji". */
  searchPlaceholder: string;
  /** Accessible name of the clear-search button. Default "Clear search". */
  clearSearch: string;
  /** Name of the category bar. Default "Emoji categories". */
  categoryNav: string;
  /** Names of the built-in categories. */
  categories: Record<EmojiCategoryId, string>;
  /** Heading of the search results. Default "Search results". */
  searchResults: string;
  /** Shown when the search matches nothing. Default "No emoji found". */
  noResults: string;
  /** Announced after each search. Default "1 emoji found" / "{n} emoji found". */
  resultsCount: (count: number) => string;
  /** Name of the skin tone button and choices. Default "Skin tone". */
  skinTone: string;
  /** Names of the six skin tones. */
  skinTones: Record<EmojiSkinTone, string>;
  /** Preview text before anything is focused. Default "Pick an emoji…". */
  preview: string;
}

const DEFAULT_MESSAGES: EmojiPickerMessages = {
  label: 'Emoji picker',
  search: 'Search emoji',
  searchPlaceholder: 'Search emoji',
  clearSearch: 'Clear search',
  categoryNav: 'Emoji categories',
  categories: {
    recent: 'Recently used',
    smileys: 'Smileys & emotion',
    people: 'People & body',
    animals: 'Animals & nature',
    food: 'Food & drink',
    travel: 'Travel & places',
    activities: 'Activities',
    objects: 'Objects',
    symbols: 'Symbols',
    flags: 'Flags',
  },
  searchResults: 'Search results',
  noResults: 'No emoji found',
  resultsCount: (n) => (n === 1 ? '1 emoji found' : `${n} emoji found`),
  skinTone: 'Skin tone',
  skinTones: {
    none: 'Default',
    light: 'Light',
    'medium-light': 'Medium-light',
    medium: 'Medium',
    'medium-dark': 'Medium-dark',
    dark: 'Dark',
  },
  preview: 'Pick an emoji…',
};

export interface EmojiPickerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'onSelect'
> {
  /** Called with the chosen emoji (skin tone applied) and its entry. */
  onSelect: (emoji: string, item: EmojiPickerItem) => void;
  /**
   * The emoji to offer, by category. Default: Monarch's compact built-in set
   * of about 500 common emoji. Pass a full dataset, or add a category of
   * your own, when the product needs it.
   */
  categories?: readonly EmojiPickerCategory[];
  /**
   * Recently used emoji, newest first, shown as the first category. The
   * product stores them (see `pushRecentEmoji`).
   */
  recents?: readonly string[];
  /** Show at most this many recents. Default 24. */
  maxRecents?: number;
  /** Skin tone for emoji that accept one (controlled). */
  skinTone?: EmojiSkinTone;
  /** Initial skin tone when uncontrolled. Default `none`. */
  defaultSkinTone?: EmojiSkinTone;
  /** Called when the person picks a skin tone. Store it to remember the choice. */
  onSkinToneChange?: ((tone: EmojiSkinTone) => void) | undefined;
  /** Show the skin tone control. Default `true`. */
  showSkinTones?: boolean;
  /** Emoji per row; arrow keys move along this grid. Default 8. */
  columns?: number;
  /** Focus the search field when the picker mounts. Default `false` (Popover focuses it already). */
  autoFocus?: boolean;
  /** Show the name of the focused or hovered emoji at the bottom. Default `true`. */
  showPreview?: boolean;
  /** Translated text. Unset keys keep their English defaults. */
  messages?: Partial<EmojiPickerMessages>;
}

interface Section {
  id: string;
  label: string;
  icon: string;
  items: readonly { item: EmojiPickerItem; display: string }[];
}

interface Position {
  s: number;
  i: number;
}

/**
 * An emoji picker: search, categories, recently used, skin tones and full
 * keyboard navigation, with a compact built-in emoji set and no third-party
 * data. Use it inline, or in a popover with `EmojiPickerPopover`.
 */
export const EmojiPicker = forwardRef<HTMLDivElement, EmojiPickerProps>(function EmojiPicker(
  {
    onSelect,
    categories = defaultEmojiCategories,
    recents = [],
    maxRecents = 24,
    skinTone: skinToneProp,
    defaultSkinTone = 'none',
    onSkinToneChange,
    showSkinTones = true,
    columns = 8,
    autoFocus = false,
    showPreview = true,
    messages,
    className,
    style,
    ...rest
  },
  ref,
) {
  const m: EmojiPickerMessages = {
    ...DEFAULT_MESSAGES,
    ...messages,
    categories: { ...DEFAULT_MESSAGES.categories, ...messages?.categories },
    skinTones: { ...DEFAULT_MESSAGES.skinTones, ...messages?.skinTones },
  };
  const cols = Math.max(1, Math.floor(columns));
  const id = useId();
  const [query, setQuery] = useState('');
  const [toneOwn, setToneOwn] = useState<EmojiSkinTone>(defaultSkinTone);
  const tone = skinToneProp ?? toneOwn;
  const [toneOpen, setToneOpen] = useState(false);
  const [active, setActive] = useState<Position>({ s: 0, i: 0 });
  const [current, setCurrent] = useState(0);
  const [preview, setPreview] = useState<{ item: EmojiPickerItem; display: string } | null>(null);
  const searchRef = useRef<HTMLInputElement>(null);
  const viewportRef = useRef<HTMLDivElement>(null);
  const toneButtonRef = useRef<HTMLButtonElement>(null);
  const cellRefs = useRef(new Map<string, HTMLButtonElement>());
  const sectionRefs = useRef(new Map<number, HTMLDivElement>());
  const navRefs = useRef(new Map<number, HTMLButtonElement>());

  const byBase = useMemo(() => {
    const map = new Map<string, EmojiPickerItem>();
    for (const c of categories) for (const item of c.emojis) map.set(baseOf(item.emoji), item);
    return map;
  }, [categories]);

  const trimmed = query.trim();
  const sections = useMemo<Section[]>(() => {
    const show = (item: EmojiPickerItem) => ({
      item,
      display: item.skinTones ? applySkinTone(item.emoji, tone) : item.emoji,
    });
    if (trimmed) {
      return [
        {
          id: 'search',
          label: m.searchResults,
          icon: '',
          items: searchEmoji(categories, trimmed).map(show),
        },
      ];
    }
    const list: Section[] = [];
    const recentItems = recents.slice(0, Math.max(0, maxRecents)).map((emoji) => ({
      item: byBase.get(baseOf(emoji)) ?? { emoji, name: emoji },
      display: emoji,
    }));
    if (recentItems.length > 0) {
      list.push({ id: 'recent', label: m.categories.recent, icon: '🕘', items: recentItems });
    }
    for (const c of categories) {
      if (c.emojis.length === 0) continue;
      list.push({
        id: c.id,
        label: c.label ?? m.categories[c.id as EmojiCategoryId] ?? c.id,
        icon: c.icon ?? c.emojis[0]?.emoji ?? '',
        items: c.emojis.map(show),
      });
    }
    return list;
    // eslint-disable-next-line react-hooks/exhaustive-deps -- labels come from `messages`, which callers often pass inline.
  }, [trimmed, categories, recents, maxRecents, tone, byBase, messages]);

  const total = sections.reduce((n, s) => n + s.items.length, 0);
  const pos: Position =
    active.i < (sections[active.s]?.items.length ?? 0) ? active : { s: 0, i: 0 };

  useEffect(() => {
    if (autoFocus) searchRef.current?.focus();
  }, [autoFocus]);

  useEffect(() => {
    setActive({ s: 0, i: 0 });
    if (viewportRef.current) viewportRef.current.scrollTop = 0;
    setCurrent(0);
  }, [trimmed]);

  const focusCell = (p: Position) => {
    setActive(p);
    cellRefs.current.get(`${p.s}:${p.i}`)?.focus();
  };

  const choose = (entry: { item: EmojiPickerItem; display: string }) => {
    onSelect(entry.display, entry.item);
  };

  const setTone = (next: EmojiSkinTone) => {
    if (skinToneProp === undefined) setToneOwn(next);
    onSkinToneChange?.(next);
  };

  const move = (p: Position, key: string, ctrl: boolean): Position | 'search' | null => {
    const len = (s: number) => sections[s]?.items.length ?? 0;
    const last = sections.length - 1;
    const col = p.i % cols;
    const rowStart = p.i - col;
    switch (key) {
      case 'ArrowRight':
        if (p.i + 1 < len(p.s)) return { s: p.s, i: p.i + 1 };
        return p.s < last ? { s: p.s + 1, i: 0 } : null;
      case 'ArrowLeft':
        if (p.i > 0) return { s: p.s, i: p.i - 1 };
        return p.s > 0 ? { s: p.s - 1, i: len(p.s - 1) - 1 } : null;
      case 'ArrowDown': {
        if (p.i + cols < len(p.s)) return { s: p.s, i: p.i + cols };
        if (Math.floor(p.i / cols) < Math.floor((len(p.s) - 1) / cols))
          return { s: p.s, i: len(p.s) - 1 };
        return p.s < last ? { s: p.s + 1, i: Math.min(col, len(p.s + 1) - 1) } : null;
      }
      case 'ArrowUp': {
        if (p.i - cols >= 0) return { s: p.s, i: p.i - cols };
        if (p.s === 0) return 'search';
        const prev = len(p.s - 1);
        const lastRow = Math.floor((prev - 1) / cols) * cols;
        return { s: p.s - 1, i: Math.min(lastRow + col, prev - 1) };
      }
      case 'Home':
        return ctrl ? { s: 0, i: 0 } : { s: p.s, i: rowStart };
      case 'End':
        return ctrl
          ? { s: last, i: len(last) - 1 }
          : { s: p.s, i: Math.min(rowStart + cols - 1, len(p.s) - 1) };
      case 'PageDown':
        return p.s < last ? { s: p.s + 1, i: 0 } : { s: p.s, i: len(p.s) - 1 };
      case 'PageUp':
        return p.s > 0 ? { s: p.s - 1, i: 0 } : { s: 0, i: 0 };
      default:
        return null;
    }
  };

  const onCellKeyDown = (e: KeyboardEvent<HTMLButtonElement>, p: Position) => {
    const next = move(p, e.key, e.ctrlKey || e.metaKey);
    if (next === null) {
      if (e.key.startsWith('Arrow') || e.key === 'Home' || e.key === 'End') e.preventDefault();
      return;
    }
    e.preventDefault();
    if (next === 'search') searchRef.current?.focus();
    else focusCell(next);
  };

  const onSearchKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Escape' && query !== '') {
      // Clear first; a second Escape reaches the popover. Handled here because
      // a surrounding popover may already have marked the event as handled.
      e.preventDefault();
      e.stopPropagation();
      setQuery('');
    } else if (e.key === 'ArrowDown' && total > 0) {
      e.preventDefault();
      focusCell({ s: 0, i: 0 });
    } else if (e.key === 'Enter' && trimmed && sections[0]?.items[0]) {
      e.preventDefault();
      choose(sections[0].items[0]);
    }
  };

  const jumpTo = (s: number) => {
    const viewport = viewportRef.current;
    const section = sectionRefs.current.get(s);
    if (viewport && section) viewport.scrollTop = section.offsetTop;
    setCurrent(s);
    setActive({ s, i: 0 });
  };

  const onScroll = () => {
    const viewport = viewportRef.current;
    if (!viewport) return;
    let found = 0;
    sectionRefs.current.forEach((el, s) => {
      if (el.offsetTop <= viewport.scrollTop + 1 && s > found) found = s;
    });
    setCurrent(found);
  };

  const onNavKeyDown = (e: KeyboardEvent<HTMLButtonElement>, s: number) => {
    const last = sections.length - 1;
    const next =
      e.key === 'ArrowRight'
        ? Math.min(last, s + 1)
        : e.key === 'ArrowLeft'
          ? Math.max(0, s - 1)
          : e.key === 'Home'
            ? 0
            : e.key === 'End'
              ? last
              : null;
    if (next === null) return;
    e.preventDefault();
    setCurrent(next);
    navRefs.current.get(next)?.focus();
  };

  const onToneKeyDown = (e: KeyboardEvent<HTMLButtonElement>, index: number) => {
    const n = SKIN_TONES.length;
    let next: number | null = null;
    if (e.key === 'ArrowRight' || e.key === 'ArrowDown') next = (index + 1) % n;
    else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') next = (index - 1 + n) % n;
    else if (e.key === 'Escape') {
      e.preventDefault();
      e.stopPropagation();
      setToneOpen(false);
      toneButtonRef.current?.focus();
      return;
    }
    if (next === null) return;
    e.preventDefault();
    setTone(SKIN_TONES[next] ?? 'none');
    (e.currentTarget.parentElement?.children[next] as HTMLElement | undefined)?.focus();
  };

  const searching = trimmed !== '';

  return (
    <div
      ref={ref}
      role="group"
      aria-label={m.label}
      {...rest}
      className={cx('mn-emoji-picker', className)}
      style={{ ...style, ['--mn-emoji-picker-columns' as string]: cols } as CSSProperties}
      data-searching={searching ? '' : undefined}
    >
      <div className="mn-emoji-picker__header">
        <SearchInput
          ref={searchRef}
          size="sm"
          className="mn-emoji-picker__search"
          aria-label={m.search}
          placeholder={m.searchPlaceholder}
          clearLabel={m.clearSearch}
          value={query}
          onValueChange={setQuery}
          onKeyDown={onSearchKeyDown}
          autoComplete="off"
          spellCheck={false}
        />
        {showSkinTones ? (
          <button
            ref={toneButtonRef}
            type="button"
            className="mn-emoji-picker__tone-button"
            aria-label={`${m.skinTone}: ${m.skinTones[tone]}`}
            aria-expanded={toneOpen}
            aria-controls={`${id}-tones`}
            onClick={() => setToneOpen((o) => !o)}
          >
            <span aria-hidden>{applySkinTone('✋', tone)}</span>
          </button>
        ) : null}
      </div>
      {showSkinTones && toneOpen ? (
        <div
          id={`${id}-tones`}
          role="radiogroup"
          aria-label={m.skinTone}
          className="mn-emoji-picker__tones"
        >
          {SKIN_TONES.map((t, index) => (
            <button
              key={t}
              type="button"
              role="radio"
              aria-checked={t === tone}
              aria-label={m.skinTones[t]}
              tabIndex={t === tone ? 0 : -1}
              className="mn-emoji-picker__tone"
              onClick={() => {
                setTone(t);
                setToneOpen(false);
                toneButtonRef.current?.focus();
              }}
              onKeyDown={(e) => onToneKeyDown(e, index)}
            >
              <span aria-hidden>{applySkinTone('✋', t)}</span>
            </button>
          ))}
        </div>
      ) : null}
      {!searching && sections.length > 1 ? (
        <div role="toolbar" aria-label={m.categoryNav} className="mn-emoji-picker__nav">
          {sections.map((section, s) => (
            <button
              key={section.id}
              ref={(el) => {
                if (el) navRefs.current.set(s, el);
                else navRefs.current.delete(s);
              }}
              type="button"
              className="mn-emoji-picker__nav-item"
              aria-label={section.label}
              aria-current={s === current ? 'true' : undefined}
              title={section.label}
              tabIndex={s === current ? 0 : -1}
              onClick={() => jumpTo(s)}
              onKeyDown={(e) => onNavKeyDown(e, s)}
            >
              <span aria-hidden>{section.icon}</span>
            </button>
          ))}
        </div>
      ) : null}
      <div ref={viewportRef} className="mn-emoji-picker__viewport" onScroll={onScroll}>
        {total === 0 ? (
          <p className="mn-emoji-picker__empty">{m.noResults}</p>
        ) : (
          sections.map((section, s) => (
            <div
              key={section.id}
              ref={(el) => {
                if (el) sectionRefs.current.set(s, el);
                else sectionRefs.current.delete(s);
              }}
              role="group"
              aria-labelledby={`${id}-s${s}`}
              className="mn-emoji-picker__section"
              data-category={section.id}
            >
              <div id={`${id}-s${s}`} className="mn-emoji-picker__heading">
                {section.label}
              </div>
              <div className="mn-emoji-picker__grid">
                {section.items.map((entry, i) => {
                  const key = `${s}:${i}`;
                  const isActive = pos.s === s && pos.i === i;
                  return (
                    <button
                      key={`${entry.display}-${i}`}
                      ref={(el) => {
                        if (el) cellRefs.current.set(key, el);
                        else cellRefs.current.delete(key);
                      }}
                      type="button"
                      className="mn-emoji-picker__emoji"
                      aria-label={entry.item.name}
                      tabIndex={isActive ? 0 : -1}
                      data-active={isActive ? '' : undefined}
                      onClick={() => choose(entry)}
                      onKeyDown={(e) => onCellKeyDown(e, { s, i })}
                      onFocus={() => {
                        setActive({ s, i });
                        setPreview(entry);
                      }}
                      onMouseEnter={() => setPreview(entry)}
                    >
                      <span aria-hidden>{entry.display}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          ))
        )}
      </div>
      <div role="status" className="mn-visually-hidden">
        {searching ? m.resultsCount(total) : ''}
      </div>
      {showPreview ? (
        <div className="mn-emoji-picker__preview" aria-hidden>
          {preview ? (
            <>
              <span className="mn-emoji-picker__preview-emoji">{preview.display}</span>
              <span className="mn-emoji-picker__preview-name">{preview.item.name}</span>
            </>
          ) : (
            <span className="mn-emoji-picker__preview-name">{m.preview}</span>
          )}
        </div>
      ) : null}
    </div>
  );
});

export interface EmojiPickerPopoverProps extends Omit<EmojiPickerProps, 'autoFocus'> {
  /** The trigger, usually an `IconButton` labelled "Add reaction" or "Emoji". Must accept a ref. */
  children: ReactElement;
  /** Open state (controlled). */
  open?: boolean;
  /** Initial open state when uncontrolled. Default `false`. */
  defaultOpen?: boolean;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Close after an emoji is picked, returning focus to the trigger. Default `true`. */
  closeOnSelect?: boolean;
  /** Preferred side of the trigger. Flips to stay on screen. Default `top`. */
  side?: 'top' | 'bottom' | 'left' | 'right';
  /** Alignment against the trigger. Default `start`. */
  align?: 'start' | 'center' | 'end';
}

/**
 * The EmojiPicker in a popover opened by a trigger you supply. It flips to
 * stay on screen, focuses the search field on open, closes on pick, Escape
 * or a click outside, and returns focus to the trigger.
 */
export function EmojiPickerPopover({
  children,
  open: openProp,
  defaultOpen = false,
  onOpenChange,
  closeOnSelect = true,
  side = 'top',
  align = 'start',
  onSelect,
  messages,
  ...pickerProps
}: EmojiPickerPopoverProps) {
  const [own, setOwn] = useState(defaultOpen);
  const open = openProp ?? own;
  const setOpen = (next: boolean) => {
    if (openProp === undefined) setOwn(next);
    onOpenChange?.(next);
  };
  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>{children}</PopoverTrigger>
      <PopoverContent
        side={side}
        align={align}
        collisionPadding={8}
        className="mn-emoji-picker-popover"
        aria-label={messages?.label ?? DEFAULT_MESSAGES.label}
        onEscapeKeyDown={(e) => {
          // Escape first clears the search or closes the skin tones; only then the popover.
          const target = e.target;
          if (!(target instanceof HTMLElement)) return;
          const clearing =
            target instanceof HTMLInputElement &&
            target.value !== '' &&
            target.closest('.mn-emoji-picker') !== null;
          if (clearing || target.closest('.mn-emoji-picker__tones')) e.preventDefault();
        }}
      >
        <EmojiPicker
          {...pickerProps}
          {...(messages ? { messages } : {})}
          onSelect={(emoji, item) => {
            onSelect(emoji, item);
            if (closeOnSelect) setOpen(false);
          }}
        />
      </PopoverContent>
    </Popover>
  );
}
