'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type ChangeEvent,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
  type RefCallback,
  type SyntheticEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';

const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useLayoutEffect;

// ------------------------------------------------------------------ data

/** One entry in the slash menu, such as "Heading 1" or "Checklist". */
export interface SlashMenuItem {
  /** Stable id, used for option ids. */
  id: string;
  /** The name shown in the menu, e.g. `Heading 1`. */
  label: string;
  /** What it inserts or does, shown under the label and read with it. */
  description?: string | undefined;
  /** Decorative icon before the label. */
  icon?: ReactNode | undefined;
  /** Group heading the item sits under, e.g. `Text` or `Lists`. Items without one form an unlabelled group. */
  group?: string | undefined;
  /** Extra words the filter matches, e.g. `['h1', 'title']`. */
  keywords?: readonly string[] | undefined;
  /** Shown but cannot be picked, e.g. an item that needs a selection. */
  disabled?: boolean | undefined;
}

/**
 * Filters items by what was typed after the slash, ranked the same way as
 * `filterAiSlashCommands`: labels that start with the query first, then labels
 * that contain it, then matches in the id, description or keywords.
 * Case-insensitive; an empty query keeps every item.
 */
export function filterSlashMenuItems(
  items: readonly SlashMenuItem[],
  query: string,
): SlashMenuItem[] {
  const q = query.trim().toLowerCase();
  if (!q) return [...items];
  const ranked: { item: SlashMenuItem; rank: number; index: number }[] = [];
  items.forEach((item, index) => {
    const label = item.label.toLowerCase();
    const rest = [item.id, item.description ?? '', ...(item.keywords ?? [])]
      .join(' ')
      .toLowerCase();
    const rank = label.startsWith(q) ? 0 : label.includes(q) ? 1 : rest.includes(q) ? 2 : -1;
    if (rank >= 0) ranked.push({ item, rank, index });
  });
  return ranked.sort((a, b) => a.rank - b.rank || a.index - b.index).map((r) => r.item);
}

/** A group of items in display order. */
export interface SlashMenuGroup {
  /** The heading, or `undefined` for items without a group. */
  label: string | undefined;
  items: SlashMenuItem[];
}

/**
 * Splits items into groups in display order: the groups named in
 * `groupOrder` first, in that order, then any others in the order they first
 * appear. Arrow keys follow this order, so it matches what is on screen.
 */
export function groupSlashMenuItems(
  items: readonly SlashMenuItem[],
  groupOrder: readonly string[] = [],
): SlashMenuGroup[] {
  const map = new Map<string | undefined, SlashMenuItem[]>();
  for (const g of groupOrder) map.set(g, []);
  for (const item of items) {
    const list = map.get(item.group);
    if (list) list.push(item);
    else map.set(item.group, [item]);
  }
  return [...map]
    .filter(([, list]) => list.length)
    .map(([label, list]) => ({ label, items: list }));
}

/** The slash command being typed at the caret. */
export interface SlashQuery {
  /** Text typed after the slash. */
  query: string;
  /** Index of the slash. */
  start: number;
  /** Index of the caret (end of the query). */
  end: number;
}

/** Options for `findSlashQuery`. */
export interface FindSlashQueryOptions {
  /** The trigger. Default `/`. */
  char?: string | undefined;
  /**
   * Where the trigger may be typed. `'line-start'` (default) only at the start
   * of a line, so paths and fractions such as `1/2` never open the menu;
   * `'word-start'` also after a space.
   */
  position?: 'line-start' | 'word-start' | undefined;
  /** Longest query that keeps the menu open. Default 24. */
  maxLength?: number | undefined;
}

const escapeRegExp = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

/**
 * Finds a slash command being typed before the caret: the trigger followed by
 * letters, digits or hyphens. Returns null when there is none.
 */
export function findSlashQuery(
  value: string,
  caret: number,
  { char = '/', position = 'line-start', maxLength = 24 }: FindSlashQueryOptions = {},
): SlashQuery | null {
  const lineStart = value.lastIndexOf('\n', caret - 1) + 1;
  const line = value.slice(lineStart, caret);
  const lead = position === 'line-start' ? '^' : '(?:^|\\s)';
  const match = new RegExp(
    `${lead}${escapeRegExp(char)}([\\p{L}\\p{N}-]{0,${maxLength}})$`,
    'u',
  ).exec(line);
  if (!match) return null;
  const query = match[1] ?? '';
  return { query, start: caret - query.length - char.length, end: caret };
}

/** Id of a menu option, for `aria-activedescendant`. */
export function slashMenuOptionId(listId: string, itemId: string): string {
  return `${listId}-opt-${itemId.replace(/[^\w-]/g, '_')}`;
}

/** Index of the next enabled entry, wrapping; -1 when none is enabled. */
function stepEnabled(list: readonly SlashMenuItem[], from: number, step: 1 | -1): number {
  const n = list.length;
  for (let k = 1; k <= n; k++) {
    const i = (((from + step * k) % n) + n) % n;
    if (!list[i]?.disabled) return i;
  }
  return -1;
}

// ------------------------------------------------------------------ listbox

/** Every string the slash menu shows or announces. */
export interface SlashMenuMessages {
  /** Name of the listbox. */
  label: string;
  /** Shown when nothing matches; receives what was typed, including the trigger. */
  empty: (query: string) => string;
  /** Announced politely as the list changes. */
  count: (count: number) => string;
}

export const SLASH_MENU_MESSAGES: SlashMenuMessages = {
  label: 'Insert block',
  empty: (query) => `Nothing matches “${query}”`,
  count: (n) => (n === 1 ? '1 option' : `${n} options`),
};

export interface SlashMenuProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onSelect' | 'id'> {
  /** The items to show, already filtered (see `filterSlashMenuItems`). They are grouped for display. */
  items: readonly SlashMenuItem[];
  /** Picks an item (click, or Enter and Tab from the text field). */
  onSelect: (item: SlashMenuItem) => void;
  /** The highlighted item. Focus stays in the text field, which points here with `aria-activedescendant`. */
  activeId?: string | null | undefined;
  /** Called when the pointer moves over an item. Hover only highlights; it never picks. */
  onActiveChange?: ((id: string) => void) | undefined;
  /** Group headings in display order. Groups not listed follow in the order they first appear. */
  groupOrder?: readonly string[] | undefined;
  /** The text typed after the trigger, for the empty message. */
  query?: string | undefined;
  /** The trigger character, for the empty message. Default `/`. */
  char?: string | undefined;
  /** Id of the listbox, for `aria-controls`. Generated when omitted. */
  listId?: string | undefined;
  /** Override any string. */
  messages?: Partial<SlashMenuMessages> | undefined;
}

/**
 * The `/` menu of a composer or document editor: a grouped listbox of things
 * to insert, with icons, labels and descriptions. Focus stays in the text
 * field and the highlighted option is announced through
 * `aria-activedescendant`, like AiSlashCommands. Use `useSlashMenu` to wire it
 * to a textarea, or `useSlashMenuKeys` for a rich-text editor.
 */
export const SlashMenu = forwardRef<HTMLDivElement, SlashMenuProps>(function SlashMenu(
  {
    items,
    onSelect,
    activeId,
    onActiveChange,
    groupOrder,
    query = '',
    char = '/',
    listId: listIdProp,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...SLASH_MENU_MESSAGES, ...messagesProp };
  const auto = `mn-slash-menu-${useId().replace(/:/g, '')}`;
  const listId = listIdProp ?? `${auto}-listbox`;
  const groups = groupSlashMenuItems(items, groupOrder);

  // Keep the highlighted option in view as the arrow keys move.
  useEffect(() => {
    if (!activeId || typeof document === 'undefined') return;
    document.getElementById(slashMenuOptionId(listId, activeId))?.scrollIntoView?.({
      block: 'nearest',
    });
  }, [activeId, listId]);

  const option = (item: SlashMenuItem) => {
    const id = slashMenuOptionId(listId, item.id);
    const active = item.id === activeId;
    return (
      // Options are never focused: focus stays in the text field (aria-activedescendant), which handles keys.
      // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/interactive-supports-focus
      <div
        key={item.id}
        id={id}
        role="option"
        aria-selected={active}
        aria-disabled={item.disabled || undefined}
        aria-labelledby={`${id}-label`}
        aria-describedby={item.description ? `${id}-desc` : undefined}
        data-highlighted={active || undefined}
        data-disabled={item.disabled || undefined}
        className="mn-slash-menu__option"
        onMouseDown={(e) => e.preventDefault()}
        onMouseMove={() => {
          if (!active && !item.disabled) onActiveChange?.(item.id);
        }}
        onClick={() => {
          if (!item.disabled) onSelect(item);
        }}
      >
        {item.icon ? (
          <span className="mn-slash-menu__icon" aria-hidden>
            {item.icon}
          </span>
        ) : null}
        <span className="mn-slash-menu__text">
          <span id={`${id}-label`} className="mn-slash-menu__label">
            {item.label}
          </span>
          {item.description ? (
            <span id={`${id}-desc`} className="mn-slash-menu__description">
              {item.description}
            </span>
          ) : null}
        </span>
      </div>
    );
  };

  return (
    <div ref={ref} {...rest} className={cx('mn-slash-menu', className)}>
      {items.length ? (
        <div role="listbox" id={listId} aria-label={m.label} className="mn-slash-menu__list">
          {groups.map((group, gi) =>
            group.label ? (
              <div
                key={group.label}
                role="group"
                aria-labelledby={`${listId}-group-${gi}`}
                className="mn-slash-menu__group"
              >
                <div
                  role="presentation"
                  id={`${listId}-group-${gi}`}
                  className="mn-slash-menu__group-label"
                >
                  {group.label}
                </div>
                {group.items.map(option)}
              </div>
            ) : (
              <div key={`ungrouped-${gi}`} role="group" className="mn-slash-menu__group">
                {group.items.map(option)}
              </div>
            ),
          )}
        </div>
      ) : (
        <p className="mn-slash-menu__empty">{m.empty(`${char}${query}`)}</p>
      )}
      <span role="status" className="mn-visually-hidden">
        {items.length ? m.count(items.length) : ''}
      </span>
    </div>
  );
});

// ------------------------------------------------------------------ keys

/** The parts of a key event the menu reads. A React or DOM `KeyboardEvent` both fit. */
export interface SlashMenuKeyEvent {
  key: string;
  shiftKey?: boolean;
  isComposing?: boolean;
  nativeEvent?: { isComposing?: boolean };
  preventDefault: () => void;
}

export interface UseSlashMenuKeysOptions {
  /** The items on screen, already filtered. */
  items: readonly SlashMenuItem[];
  /** Picks an item. */
  onSelect: (item: SlashMenuItem) => void;
  /** Closes the menu (Escape). */
  onClose?: (() => void) | undefined;
  /** Same as SlashMenu's `groupOrder`, so arrow keys follow the visible order. */
  groupOrder?: readonly string[] | undefined;
}

export interface UseSlashMenuKeysResult {
  /** The highlighted item, or undefined when nothing can be picked. */
  active: SlashMenuItem | undefined;
  /** Highlights an item. */
  setActiveId: (id: string | null) => void;
  /**
   * Handles Arrow Up and Down (wrapping, skipping disabled items), Enter and
   * Tab (pick) and Escape (close). Returns true when it used the key, after
   * calling `preventDefault()`; return that from a rich-text editor's
   * suggestion `onKeyDown`.
   */
  handleKeyDown: (event: SlashMenuKeyEvent) => boolean;
  /** The items in display order (grouped), which the arrow keys follow. */
  ordered: SlashMenuItem[];
}

/**
 * Highlight and keyboard state for a SlashMenu whose trigger detection is done
 * elsewhere, such as a rich-text editor's suggestion plugin. Spread
 * `{ activeId: active?.id, onActiveChange: setActiveId }` on SlashMenu and call
 * `handleKeyDown` from the editor.
 */
export function useSlashMenuKeys({
  items,
  onSelect,
  onClose,
  groupOrder,
}: UseSlashMenuKeysOptions): UseSlashMenuKeysResult {
  const [activeId, setActiveId] = useState<string | null>(null);
  const ordered = useMemo(
    () => groupSlashMenuItems(items, groupOrder).flatMap((g) => g.items),
    [items, groupOrder],
  );
  const active =
    ordered.find((i) => i.id === activeId && !i.disabled) ?? ordered.find((i) => !i.disabled);

  const handleKeyDown = (event: SlashMenuKeyEvent): boolean => {
    if (event.isComposing || event.nativeEvent?.isComposing) return false;
    if (event.key === 'Escape' && onClose) {
      event.preventDefault();
      onClose();
      return true;
    }
    if (!active) return false;
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      const next =
        ordered[stepEnabled(ordered, ordered.indexOf(active), event.key === 'ArrowDown' ? 1 : -1)];
      if (next) setActiveId(next.id);
      return true;
    }
    if ((event.key === 'Enter' || event.key === 'Tab') && !event.shiftKey) {
      event.preventDefault();
      onSelect(active);
      return true;
    }
    return false;
  };

  return { active, setActiveId, handleKeyDown, ordered };
}

// ------------------------------------------------------------------ textarea hook

export interface UseSlashMenuOptions {
  /** Everything the menu can offer; the hook filters it by what is typed. */
  items: readonly SlashMenuItem[];
  /** The textarea's value. The product owns it. */
  value: string;
  /** Receives the new value, including after an item is picked. */
  onChange: (value: string) => void;
  /**
   * Runs the picked item. Return text to put in place of the typed `/query`
   * (e.g. `'## '`), or nothing to just remove it.
   */
  onSelect: (item: SlashMenuItem) => unknown;
  /** The trigger. Default `/`. */
  char?: string | undefined;
  /** Where the trigger may be typed; see `findSlashQuery`. Default `'line-start'`. */
  position?: FindSlashQueryOptions['position'];
  /** Group headings in display order. */
  groupOrder?: readonly string[] | undefined;
  /** Your own filter. Default `filterSlashMenuItems`. */
  filter?: ((items: readonly SlashMenuItem[], query: string) => SlashMenuItem[]) | undefined;
  /** Your own key handler. Runs when the menu did not use the key. */
  onKeyDown?: ((event: ReactKeyboardEvent<HTMLTextAreaElement>) => void) | undefined;
}

export interface UseSlashMenuResult {
  /** Whether the menu is open. Render `<SlashMenu {...menuProps} />` only while it is. */
  open: boolean;
  /** The command being typed, or null. */
  query: SlashQuery | null;
  /** Spread on the `<textarea>`: value, change, keys, caret tracking and the ARIA wiring. */
  textareaProps: {
    ref: RefCallback<HTMLTextAreaElement>;
    value: string;
    onChange: (event: ChangeEvent<HTMLTextAreaElement>) => void;
    onKeyDown: (event: ReactKeyboardEvent<HTMLTextAreaElement>) => void;
    onSelect: (event: SyntheticEvent<HTMLTextAreaElement>) => void;
    onBlur: () => void;
    'aria-autocomplete': 'list';
    'aria-controls': string | undefined;
    'aria-activedescendant': string | undefined;
  };
  /** Spread on `<SlashMenu>`. */
  menuProps: Pick<
    SlashMenuProps,
    | 'items'
    | 'onSelect'
    | 'activeId'
    | 'onActiveChange'
    | 'groupOrder'
    | 'query'
    | 'char'
    | 'listId'
  >;
  /** Opens the menu as if the trigger had been typed at the caret (for a toolbar button). */
  openMenu: () => void;
  /** Closes the menu without changing the text. */
  close: () => void;
}

/**
 * Connects a plain `<textarea>` to SlashMenu: typing `/` at the start of a
 * line opens the menu filtered by what follows, Arrow keys move, Enter or Tab
 * pick, Escape closes, and the typed `/query` is replaced by whatever
 * `onSelect` returns.
 */
export function useSlashMenu({
  items,
  value,
  onChange,
  onSelect,
  char = '/',
  position = 'line-start',
  groupOrder,
  filter = filterSlashMenuItems,
  onKeyDown,
}: UseSlashMenuOptions): UseSlashMenuResult {
  const listId = `mn-slash-menu-${useId().replace(/:/g, '')}-listbox`;
  const field = useRef<HTMLTextAreaElement | null>(null);
  const pendingCaret = useRef<number | null>(null);
  const valueRef = useRef(value);
  valueRef.current = value;
  const onSelectRef = useRef(onSelect);
  onSelectRef.current = onSelect;
  const [query, setQuery] = useState<SlashQuery | null>(null);
  // Escape hides the menu until the query changes, so typing on reopens it.
  const [dismissedAt, setDismissedAt] = useState<number | null>(null);

  const matches = useMemo(() => (query ? filter(items, query.query) : []), [filter, items, query]);
  const open = Boolean(query) && dismissedAt !== query?.end;

  const close = useCallback(() => setDismissedAt(query?.end ?? null), [query]);

  const pick = (item: SlashMenuItem) => {
    if (!query || item.disabled) return;
    const result = onSelectRef.current(item);
    const text = typeof result === 'string' ? result : '';
    const current = valueRef.current;
    pendingCaret.current = query.start + text.length;
    setQuery(null);
    onChange(current.slice(0, query.start) + text + current.slice(query.end));
  };

  const keys = useSlashMenuKeys({
    items: open ? matches : [],
    onSelect: pick,
    onClose: open ? close : undefined,
    groupOrder,
  });

  useIsoLayoutEffect(() => {
    const el = field.current;
    const caret = pendingCaret.current;
    if (el && caret !== null && el.value === value) {
      pendingCaret.current = null;
      el.setSelectionRange(caret, caret);
      el.focus();
    }
  }, [value]);

  const detect = (el: HTMLTextAreaElement) => {
    if (el.selectionStart !== el.selectionEnd) {
      setQuery(null);
      return;
    }
    const next = findSlashQuery(el.value, el.selectionStart ?? 0, { char, position });
    setQuery((prev) =>
      prev &&
      next &&
      prev.start === next.start &&
      prev.end === next.end &&
      prev.query === next.query
        ? prev
        : next,
    );
    if (next?.start !== query?.start) {
      keys.setActiveId(null);
      setDismissedAt(null);
    }
  };

  const openMenu = () => {
    const el = field.current;
    const current = valueRef.current;
    const caret = el?.selectionStart ?? current.length;
    const lineStart = current.lastIndexOf('\n', caret - 1) + 1;
    const before = current.slice(0, caret);
    const prefix = caret === lineStart ? char : `\n${char}`;
    pendingCaret.current = caret + prefix.length;
    setDismissedAt(null);
    setQuery({ query: '', start: pendingCaret.current - char.length, end: pendingCaret.current });
    onChange(before + prefix + current.slice(caret));
  };

  const expanded = open && matches.length > 0;

  return {
    open,
    query: open ? query : null,
    textareaProps: {
      ref: (node) => {
        field.current = node;
      },
      value,
      onChange: (event) => {
        onChange(event.target.value);
        detect(event.target);
      },
      onKeyDown: (event) => {
        if (open && keys.handleKeyDown(event)) return;
        onKeyDown?.(event);
      },
      onSelect: (event) => {
        const el = event.currentTarget;
        if (typeof document !== 'undefined' && document.activeElement === el) detect(el);
      },
      onBlur: () => setQuery(null),
      'aria-autocomplete': 'list',
      'aria-controls': expanded ? listId : undefined,
      'aria-activedescendant':
        expanded && keys.active ? slashMenuOptionId(listId, keys.active.id) : undefined,
    },
    menuProps: {
      items: matches,
      onSelect: pick,
      activeId: keys.active?.id ?? null,
      onActiveChange: keys.setActiveId,
      groupOrder,
      query: query?.query ?? '',
      char,
      listId,
    },
    openMenu,
    close,
  };
}
