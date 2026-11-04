'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  useState,
  type ChangeEvent,
  type FocusEvent,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
  type RefCallback,
  type SyntheticEvent,
} from 'react';
import { createPortal } from 'react-dom';
import { cx } from '../../../internal/cx.js';
import { Avatar } from '../../identity/avatar/index.js';
import { Spinner } from '../../feedback/spinner/index.js';

const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useLayoutEffect;

/* ------------------------------------------------------------------ */
/* Data contract                                                       */
/* ------------------------------------------------------------------ */

export type MentionKind = 'person' | 'channel';

/** One suggestion. The product's search returns these. */
export interface MentionItem {
  /** Stable id written into the token, e.g. a user or channel id. */
  id: string;
  /** Display name, written into the token and shown in the list. */
  label: string;
  /** `person` (default) or `channel`. Suggestions are grouped by kind. */
  kind?: MentionKind;
  /** Secondary line: an email address, a role, a member count. */
  description?: ReactNode;
  /** Avatar image for people. Initials are shown without it. */
  avatarSrc?: string | null;
  /** Shown but cannot be chosen, e.g. someone without access to the conversation. */
  disabled?: boolean;
}

/** The text being completed: from the trigger character to the caret. */
export interface MentionQuery {
  /** The trigger that started it, e.g. `@`. */
  trigger: string;
  /** What has been typed after the trigger. */
  text: string;
  /** Index of the trigger character in the value. */
  start: number;
  /** Index of the caret (end of the query). */
  end: number;
}

/**
 * Finds suggestions for a query. Return an array or a promise of one; abort
 * the request when `signal` fires (the reader kept typing or closed the list).
 */
export type MentionSearch = (
  text: string,
  context: { trigger: string; signal: AbortSignal },
) => readonly MentionItem[] | Promise<readonly MentionItem[]>;

/** Where the caret is, in viewport coordinates. */
export interface MentionAnchorRect {
  top: number;
  left: number;
  height: number;
}

export type MentionPickerStatus = 'idle' | 'loading' | 'ready' | 'error';

/* ------------------------------------------------------------------ */
/* Token format                                                        */
/* ------------------------------------------------------------------ */

/**
 * The default token written for a mention: `@[Ana Lopez](u_123)`. The
 * brackets and parentheses in the label are dropped so the token always
 * parses back. Products may write any format through `serialize`.
 */
export function formatMentionToken(item: Pick<MentionItem, 'id' | 'label'>, trigger = '@'): string {
  const label = item.label.replace(/[[\]()]/g, '');
  return `${trigger}[${label}](${item.id})`;
}

export type MentionSegment =
  { type: 'text'; text: string } | { type: 'mention'; trigger: string; label: string; id: string };

/**
 * Splits text written with `formatMentionToken` into plain text and mention
 * segments, for rendering mentions as chips or links.
 */
export function parseMentionTokens(
  text: string,
  triggers: readonly string[] = ['@', '#'],
): MentionSegment[] {
  const t = triggers.map(escapeRegExp).join('|');
  const re = new RegExp(`(${t})\\[([^\\[\\]()\\n]{1,200})\\]\\(([^()\\s]{1,200})\\)`, 'g');
  const out: MentionSegment[] = [];
  let last = 0;
  for (const match of text.matchAll(re)) {
    const index = match.index ?? 0;
    if (index > last) out.push({ type: 'text', text: text.slice(last, index) });
    const [, trigger = '', label = '', id = ''] = match;
    out.push({ type: 'mention', trigger, label, id });
    last = index + match[0].length;
  }
  if (last < text.length) out.push({ type: 'text', text: text.slice(last) });
  return out;
}

function escapeRegExp(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Finds a mention being typed at `caret`: a trigger at the start or after
 * whitespace, followed by up to two words. Returns null when there is none.
 */
export function findMentionQuery(
  value: string,
  caret: number,
  triggers: readonly string[] = ['@'],
): MentionQuery | null {
  const before = value.slice(0, caret);
  const t = triggers.map(escapeRegExp).join('|');
  const stop = triggers.map(escapeRegExp).join('');
  const re = new RegExp(`(?:^|[\\s(])(${t})([^\\s${stop}]{0,40}(?: [^\\s${stop}]{0,40})?)$`);
  const match = re.exec(before);
  if (!match) return null;
  const [, trigger = '', text = ''] = match;
  return { trigger, text, start: caret - text.length - trigger.length, end: caret };
}

/* ------------------------------------------------------------------ */
/* Caret position                                                      */
/* ------------------------------------------------------------------ */

const MIRRORED = [
  'boxSizing',
  'width',
  'borderTopWidth',
  'borderRightWidth',
  'borderBottomWidth',
  'borderLeftWidth',
  'paddingTop',
  'paddingRight',
  'paddingBottom',
  'paddingLeft',
  'fontStyle',
  'fontVariant',
  'fontWeight',
  'fontStretch',
  'fontSize',
  'fontFamily',
  'lineHeight',
  'letterSpacing',
  'textTransform',
  'textIndent',
  'wordSpacing',
  'tabSize',
  'direction',
] as const;

/**
 * The caret's position in a textarea or input, in viewport coordinates, by
 * laying out a hidden copy of the field's text.
 */
export function getCaretRect(
  field: HTMLTextAreaElement | HTMLInputElement,
  position: number = field.selectionStart ?? 0,
): MentionAnchorRect {
  const rect = field.getBoundingClientRect();
  const style = window.getComputedStyle(field);
  const mirror = document.createElement('div');
  const s = mirror.style;
  for (const prop of MIRRORED) s[prop] = style[prop];
  s.position = 'absolute';
  s.visibility = 'hidden';
  s.top = '0';
  s.left = '-9999px';
  s.overflow = 'hidden';
  s.whiteSpace = field instanceof HTMLTextAreaElement ? 'pre-wrap' : 'pre';
  s.overflowWrap = 'break-word';
  mirror.textContent = field.value.slice(0, position);
  const marker = document.createElement('span');
  marker.textContent = field.value.slice(position) || '.';
  mirror.appendChild(marker);
  document.body.appendChild(mirror);
  const lineHeight = parseFloat(style.lineHeight) || parseFloat(style.fontSize) * 1.4 || 16;
  const top = rect.top + marker.offsetTop - field.scrollTop;
  const left = rect.left + marker.offsetLeft - field.scrollLeft;
  document.body.removeChild(mirror);
  return { top, left, height: lineHeight };
}

/* ------------------------------------------------------------------ */
/* useMentionPicker: the editor-agnostic core                           */
/* ------------------------------------------------------------------ */

export interface UseMentionPickerOptions {
  search: MentionSearch;
  /** Called when a suggestion is chosen, with the query it completes. */
  onSelect: (item: MentionItem, query: MentionQuery) => void;
  /** Wait this long after typing before searching. Default 150 ms. */
  debounceMs?: number;
}

export interface UseMentionPickerResult {
  /** The open query, or null when the picker is closed. */
  query: MentionQuery | null;
  /**
   * Opens, updates or (with null) closes the picker. Editors call this as
   * the caret moves, with the caret rectangle in viewport coordinates.
   */
  update: (query: MentionQuery | null, anchor?: MentionAnchorRect | null) => void;
  /** Closes the picker. */
  close: () => void;
  /** Chooses the highlighted suggestion, or the given one. */
  select: (item?: MentionItem) => void;
  /**
   * Handles Arrow Up/Down, Enter, Tab and Escape while the picker is open.
   * Returns true (after preventing the default) when it used the key.
   */
  handleKeyDown: (
    event: Pick<KeyboardEvent, 'key' | 'preventDefault' | 'stopPropagation'> & {
      nativeEvent?: { isComposing?: boolean };
    },
  ) => boolean;
  /** ARIA to spread on the text field. */
  fieldProps: {
    'aria-autocomplete': 'list';
    'aria-controls': string | undefined;
    'aria-activedescendant': string | undefined;
  };
  /** Spread on `<MentionPicker>`. */
  pickerProps: Pick<
    MentionPickerProps,
    | 'id'
    | 'open'
    | 'anchor'
    | 'items'
    | 'status'
    | 'activeId'
    | 'onSelect'
    | 'onActiveChange'
    | 'query'
    | 'onRetry'
  >;
}

/**
 * Suggestion state for a mention picker: async search with debounce and
 * abort, the highlighted option and APG listbox keys. Works with any text
 * editor; for a plain textarea use `useMentionTextarea`.
 */
export function useMentionPicker({
  search,
  onSelect,
  debounceMs = 150,
}: UseMentionPickerOptions): UseMentionPickerResult {
  const id = `mn-mention-${useId().replace(/:/g, '')}`;
  const [query, setQuery] = useState<MentionQuery | null>(null);
  const [anchor, setAnchor] = useState<MentionAnchorRect | null>(null);
  const [items, setItems] = useState<readonly MentionItem[]>([]);
  const [status, setStatus] = useState<MentionPickerStatus>('idle');
  const [activeId, setActiveId] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  const searchRef = useRef(search);
  const onSelectRef = useRef(onSelect);
  searchRef.current = search;
  onSelectRef.current = onSelect;

  const key = query ? `${query.trigger}\u0000${query.text}` : null;

  useEffect(() => {
    if (!query) {
      setItems([]);
      setStatus('idle');
      return;
    }
    const controller = new AbortController();
    setStatus('loading');
    const timer = setTimeout(() => {
      let result: readonly MentionItem[] | Promise<readonly MentionItem[]>;
      try {
        result = searchRef.current(query.text, {
          trigger: query.trigger,
          signal: controller.signal,
        });
      } catch {
        setStatus('error');
        return;
      }
      Promise.resolve(result).then(
        (found) => {
          if (controller.signal.aborted) return;
          setItems(found);
          setStatus('ready');
          setActiveId(found.find((i) => !i.disabled)?.id ?? null);
        },
        () => {
          if (!controller.signal.aborted) setStatus('error');
        },
      );
    }, debounceMs);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
    // Re-search when the trigger or text changes, not when the caret index does.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, debounceMs, attempt]);

  const update = useCallback((next: MentionQuery | null, rect?: MentionAnchorRect | null) => {
    setQuery((prev) =>
      prev &&
      next &&
      prev.trigger === next.trigger &&
      prev.text === next.text &&
      prev.start === next.start &&
      prev.end === next.end
        ? prev
        : next,
    );
    if (rect !== undefined) setAnchor(rect);
    if (!next) setActiveId(null);
  }, []);

  const close = useCallback(() => update(null), [update]);

  // A two-word query with no matches is ordinary prose, not a mention: close.
  const dismissed =
    query !== null && status === 'ready' && items.length === 0 && query.text.includes(' ');
  const open = query !== null && !dismissed;
  const enabled = items.filter((i) => !i.disabled);

  const select = useCallback(
    (item?: MentionItem) => {
      const chosen = item ?? items.find((i) => i.id === activeId);
      if (!chosen || chosen.disabled || !query) return;
      onSelectRef.current(chosen, query);
      update(null);
    },
    [items, activeId, query, update],
  );

  const handleKeyDown: UseMentionPickerResult['handleKeyDown'] = (event) => {
    if (!open || event.nativeEvent?.isComposing) return false;
    const move = (delta: number) => {
      if (enabled.length === 0) return;
      const i = enabled.findIndex((o) => o.id === activeId);
      const next = enabled[(i + delta + enabled.length) % enabled.length];
      if (next) setActiveId(next.id);
    };
    switch (event.key) {
      case 'ArrowDown':
        move(1);
        break;
      case 'ArrowUp':
        move(-1);
        break;
      case 'Enter':
      case 'Tab':
        if (!activeId || status !== 'ready') return false;
        select();
        break;
      case 'Escape':
        event.stopPropagation();
        close();
        break;
      default:
        return false;
    }
    event.preventDefault();
    return true;
  };

  const hasList = open && status === 'ready' && items.length > 0;
  return {
    query: open ? query : null,
    update,
    close,
    select,
    handleKeyDown,
    fieldProps: {
      'aria-autocomplete': 'list',
      'aria-controls': hasList ? `${id}-listbox` : undefined,
      'aria-activedescendant': hasList && activeId ? optionId(id, activeId) : undefined,
    },
    pickerProps: {
      id,
      open,
      anchor,
      items,
      status,
      activeId,
      query: open ? query : null,
      onSelect: (item) => select(item),
      onActiveChange: setActiveId,
      onRetry: () => setAttempt((n) => n + 1),
    },
  };
}

function optionId(base: string, itemId: string): string {
  return `${base}-opt-${itemId.replace(/[^\w-]/g, '_')}`;
}

/* ------------------------------------------------------------------ */
/* useMentionTextarea                                                  */
/* ------------------------------------------------------------------ */

export interface UseMentionTextareaOptions {
  /** The textarea's value. The product owns it. */
  value: string;
  /** Receives the new value, including after a mention is inserted. */
  onChange: (value: string) => void;
  search: MentionSearch;
  /** Characters that open the picker. Default `['@']`; add `#` for channels. */
  triggers?: readonly string[];
  /** Text written for a chosen suggestion. Default `formatMentionToken`. A space is added after it. */
  serialize?: (item: MentionItem, trigger: string) => string;
  /** Called after a mention is inserted, e.g. to track who will be notified. */
  onMention?: (item: MentionItem) => void;
  debounceMs?: number;
  /** Your own key handler. Runs when the picker did not use the key. */
  onKeyDown?: (event: KeyboardEvent<HTMLTextAreaElement>) => void;
}

export interface UseMentionTextareaResult {
  /** Spread on the `<textarea>`. Includes `value`, `onChange`, `onKeyDown` and the ARIA wiring. */
  textareaProps: {
    ref: RefCallback<HTMLTextAreaElement>;
    value: string;
    onChange: (event: ChangeEvent<HTMLTextAreaElement>) => void;
    onKeyDown: (event: KeyboardEvent<HTMLTextAreaElement>) => void;
    onSelect: (event: SyntheticEvent<HTMLTextAreaElement>) => void;
    onBlur: (event: FocusEvent<HTMLTextAreaElement>) => void;
    'aria-autocomplete': 'list';
    'aria-controls': string | undefined;
    'aria-activedescendant': string | undefined;
  };
  /** Spread on `<MentionPicker>`. */
  pickerProps: UseMentionPickerResult['pickerProps'];
  /** The open query, or null. */
  query: MentionQuery | null;
  close: () => void;
}

/**
 * Connects a plain `<textarea>` to MentionPicker: detects `@` at the caret,
 * anchors the list to the caret, and replaces the query with a mention
 * token when a suggestion is chosen.
 */
export function useMentionTextarea({
  value,
  onChange,
  search,
  triggers = ['@'],
  serialize = formatMentionToken,
  onMention,
  debounceMs,
  onKeyDown,
}: UseMentionTextareaOptions): UseMentionTextareaResult {
  const field = useRef<HTMLTextAreaElement | null>(null);
  const pendingCaret = useRef<number | null>(null);
  const valueRef = useRef(value);
  valueRef.current = value;

  const picker = useMentionPicker({
    search,
    ...(debounceMs !== undefined ? { debounceMs } : {}),
    onSelect: (item, query) => {
      const current = valueRef.current;
      const token = `${serialize(item, query.trigger)} `;
      const after = current.slice(query.end).replace(/^ /, '');
      pendingCaret.current = query.start + token.length;
      onChange(current.slice(0, query.start) + token + after);
      onMention?.(item);
    },
  });
  const { update } = picker;

  // Put the caret after the inserted token once the new value is rendered.
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
      update(null);
      return;
    }
    const caret = el.selectionStart ?? 0;
    const query = findMentionQuery(el.value, caret, triggers);
    update(query, query ? getCaretRect(el, query.start) : null);
  };

  return {
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
        if (picker.handleKeyDown(event)) return;
        onKeyDown?.(event);
      },
      onSelect: (event) => {
        // Caret moved by click or arrow keys: re-check, but only when focused.
        const el = event.currentTarget;
        if (typeof document !== 'undefined' && document.activeElement === el) detect(el);
      },
      onBlur: () => picker.close(),
      ...picker.fieldProps,
    },
    pickerProps: picker.pickerProps,
    query: picker.query,
    close: picker.close,
  };
}

/* ------------------------------------------------------------------ */
/* MentionPicker: the suggestion popover                                */
/* ------------------------------------------------------------------ */

export interface MentionPickerMessages {
  /** Name of the list. Default "Suggestions". */
  label: string;
  people: string;
  channels: string;
  /** Default "Searching…". */
  loading: string;
  /** Default "No matches". */
  empty: string;
  /** Default "Couldn’t load suggestions." */
  error: string;
  retry: string;
  /** Announced when results arrive. Default "1 suggestion" / "{n} suggestions". */
  count: (n: number) => string;
}

const DEFAULT_MESSAGES: MentionPickerMessages = {
  label: 'Suggestions',
  people: 'People',
  channels: 'Channels',
  loading: 'Searching…',
  empty: 'No matches',
  error: 'Couldn’t load suggestions.',
  retry: 'Retry',
  count: (n) => (n === 1 ? '1 suggestion' : `${n} suggestions`),
};

export interface MentionPickerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onSelect' | 'id'
> {
  /** Base id; the listbox is `${id}-listbox`. From the hook. */
  id: string;
  open: boolean;
  /** The caret rectangle in viewport coordinates. The list opens below it, or above when there is no room. */
  anchor: MentionAnchorRect | null;
  items: readonly MentionItem[];
  status: MentionPickerStatus;
  activeId: string | null;
  query?: MentionQuery | null;
  onSelect: (item: MentionItem) => void;
  onActiveChange: (id: string) => void;
  /** Repeats a failed search. */
  onRetry?: () => void;
  /** Overrides the default avatar or `#` glyph before each option. */
  renderIcon?: (item: MentionItem) => ReactNode;
  /** Where to portal the list. Default `document.body`. */
  container?: HTMLElement | null;
  messages?: Partial<MentionPickerMessages>;
}

/**
 * The @mention suggestion list: a listbox anchored to the caret, with people
 * and channels in labelled groups and loading, empty and error states. Focus
 * stays in the text field; the field points at the highlighted option with
 * `aria-activedescendant`. Drive it with `useMentionTextarea` or
 * `useMentionPicker`.
 */
export const MentionPicker = forwardRef<HTMLDivElement, MentionPickerProps>(function MentionPicker(
  {
    id,
    open,
    anchor,
    items,
    status,
    activeId,
    query: _query,
    onSelect,
    onActiveChange,
    onRetry,
    renderIcon,
    container,
    messages: messagesProp,
    className,
    style,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const panel = useRef<HTMLDivElement | null>(null);
  const [mounted, setMounted] = useState(false);
  const [position, setPosition] = useState<{ top: number; left: number } | null>(null);
  useEffect(() => setMounted(true), []);

  const setRefs = (node: HTMLDivElement | null) => {
    panel.current = node;
    if (typeof ref === 'function') ref(node);
    else if (ref) ref.current = node;
  };

  // Below the caret, flipped above when it would leave the viewport, and kept inside it horizontally.
  useIsoLayoutEffect(() => {
    if (!open || !anchor || !panel.current) return;
    const { offsetWidth: w, offsetHeight: h } = panel.current;
    const gap = 4;
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    const below = anchor.top + anchor.height + gap;
    const top = below + h > vh - 8 && anchor.top - gap - h >= 8 ? anchor.top - gap - h : below;
    const left = Math.max(8, Math.min(anchor.left, vw - w - 8));
    setPosition({ top, left });
  }, [open, anchor?.top, anchor?.left, anchor?.height, items.length, status]);

  // Keep the highlighted option visible.
  useEffect(() => {
    if (!open || !activeId) return;
    const el = document.getElementById(optionId(id, activeId));
    el?.scrollIntoView?.({ block: 'nearest' });
  }, [open, activeId, id]);

  const announcement = !open
    ? ''
    : status === 'loading'
      ? m.loading
      : status === 'error'
        ? m.error
        : status === 'ready'
          ? items.length
            ? m.count(items.length)
            : m.empty
          : '';

  const people = items.filter((i) => (i.kind ?? 'person') === 'person');
  const channels = items.filter((i) => i.kind === 'channel');
  const groups: Array<{ key: MentionKind; label: string; items: MentionItem[] }> = [];
  if (people.length) groups.push({ key: 'person', label: m.people, items: people });
  if (channels.length) groups.push({ key: 'channel', label: m.channels, items: channels });
  const grouped = groups.length > 1;

  const option = (item: MentionItem) => {
    const active = item.id === activeId;
    return (
      // Options are never focused: focus stays in the field (aria-activedescendant), keys are handled there.
      // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/interactive-supports-focus
      <div
        key={item.id}
        id={optionId(id, item.id)}
        role="option"
        aria-selected={active}
        aria-disabled={item.disabled || undefined}
        data-highlighted={active || undefined}
        data-disabled={item.disabled || undefined}
        className="mn-mention-picker__option"
        // Keep focus (and the caret) in the text field.
        onMouseDown={(e) => e.preventDefault()}
        onMouseMove={() => {
          if (!active && !item.disabled) onActiveChange(item.id);
        }}
        onClick={() => {
          if (!item.disabled) onSelect(item);
        }}
      >
        <span className="mn-mention-picker__icon" aria-hidden>
          {renderIcon ? (
            renderIcon(item)
          ) : item.kind === 'channel' ? (
            <span className="mn-mention-picker__channel">#</span>
          ) : (
            <Avatar name={item.label} src={item.avatarSrc ?? null} size="xs" decorative />
          )}
        </span>
        <span className="mn-mention-picker__text">
          <span className="mn-mention-picker__label">{item.label}</span>
          {item.description ? (
            <span className="mn-mention-picker__description">{item.description}</span>
          ) : null}
        </span>
      </div>
    );
  };

  const body =
    status === 'ready' && items.length > 0 ? (
      <div
        role="listbox"
        id={`${id}-listbox`}
        aria-label={m.label}
        className="mn-mention-picker__list"
      >
        {grouped
          ? groups.map((g) => (
              <div
                key={g.key}
                role="group"
                aria-labelledby={`${id}-group-${g.key}`}
                className="mn-mention-picker__group"
              >
                <div
                  role="presentation"
                  id={`${id}-group-${g.key}`}
                  className="mn-mention-picker__group-label"
                >
                  {g.label}
                </div>
                {g.items.map(option)}
              </div>
            ))
          : items.map(option)}
      </div>
    ) : (
      <div className="mn-mention-picker__message" data-status={status}>
        {status === 'error' ? (
          <>
            <span>{m.error}</span>
            {onRetry ? (
              <button
                type="button"
                className="mn-mention-picker__retry"
                onMouseDown={(e) => e.preventDefault()}
                onClick={onRetry}
              >
                {m.retry}
              </button>
            ) : null}
          </>
        ) : status === 'ready' ? (
          m.empty
        ) : (
          <>
            <Spinner size="sm" />
            <span>{m.loading}</span>
          </>
        )}
      </div>
    );

  return (
    <>
      <span role="status" aria-live="polite" className="mn-visually-hidden">
        {announcement}
      </span>
      {open && mounted && anchor
        ? createPortal(
            <div
              ref={setRefs}
              {...rest}
              className={cx('mn-mention-picker', className)}
              data-status={status}
              style={{
                ...style,
                top: position?.top ?? anchor.top + anchor.height + 4,
                left: position?.left ?? anchor.left,
                visibility: position ? undefined : 'hidden',
              }}
            >
              {body}
            </div>,
            container ?? document.body,
          )
        : null}
    </>
  );
});
