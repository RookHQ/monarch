'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type InputHTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import * as PopoverPrimitive from '@radix-ui/react-popover';
import { cx } from '../../../internal/cx.js';
import {
  CheckGlyph,
  ChevronDownGlyph,
  CloseGlyph,
  RetryGlyph,
  SearchGlyph,
} from '../../../internal/icons.js';
import { useFieldContext, useFieldControl } from '../form-field/index.js';
import { Spinner } from '../../feedback/spinner/index.js';

export interface ComboboxOption {
  value: string;
  label: string;
  /** Secondary text under the label. */
  description?: string;
  disabled?: boolean;
  /**
   * Group heading. Options with the same `group` are listed together under it,
   * in the order each group first appears; ungrouped options come first.
   */
  group?: string;
  /** Short trailing text, such as a count, a code or an email. Matched by the default filter. */
  hint?: string;
  /** Leading visual (an Avatar, icon or logo). Decorative: the label names the option. */
  media?: ReactNode;
  /** Extra words the default filter matches but never shows, such as old names or aliases. */
  keywords?: string;
}

/** Passed to `onSearch`. The signal aborts when a newer query starts or the list unmounts. */
export interface ComboboxSearchContext {
  signal: AbortSignal;
}

/** An option the list starts with that clears the selection, such as "Unassigned". */
export interface ComboboxClearOption {
  label: string;
  description?: string;
  media?: ReactNode;
}

/** Interface text, for translation. Every message has an English default. */
export interface ComboboxMessages {
  /** Shown while options load and there are none yet. Default "Loading…". */
  loading: string;
  /** Shown when `onSearch` rejects. Default "Couldn’t load options.". */
  error: string;
  /** The retry button after an error. Default "Try again". */
  retry: string;
  /** The create option. Default `Create “${query}”`. */
  create: (query: string) => string;
  /** A chip's remove button in `multiple` mode. Default `Remove ${label}`. */
  remove: (label: string) => string;
  /** Announced when a value is added in `multiple` mode. Default `${label} added`. */
  added: (label: string) => string;
  /** Announced when a value is removed in `multiple` mode. Default `${label} removed`. */
  removed: (label: string) => string;
  /** Shown under a list cut short by `limit`. */
  more: (shown: number, total: number) => string;
  /** The toggle button while the list is closed. Default "Show options". */
  showOptions: string;
  /** The toggle button while the list is open. Default "Close options". */
  closeOptions: string;
}

const DEFAULT_MESSAGES: ComboboxMessages = {
  loading: 'Loading…',
  error: 'Couldn’t load options.',
  retry: 'Try again',
  create: (q) => `Create “${q}”`,
  remove: (label) => `Remove ${label}`,
  added: (label) => `${label} added`,
  removed: (label) => `${label} removed`,
  more: (shown, total) => `Showing ${shown} of ${total}. Type to narrow the list.`,
  showOptions: 'Show options',
  closeOptions: 'Close options',
};

export interface ComboboxOptionState {
  selected: boolean;
  active: boolean;
}

export interface ComboboxProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'value' | 'defaultValue' | 'onChange' | 'size' | 'type'
> {
  /** The options. With `onSearch`, the options shown before the first search finishes (usually none). */
  options?: ComboboxOption[] | undefined;
  /** Selected option value (controlled). Single-select only. */
  value?: string | null | undefined;
  defaultValue?: string | null | undefined;
  onValueChange?: ((value: string | null) => void) | undefined;
  /** Select several values. Use `values`, `defaultValues` and `onValuesChange`; the list stays open after each pick. */
  multiple?: boolean | undefined;
  /** Selected values (controlled), with `multiple`. */
  values?: string[] | undefined;
  defaultValues?: string[] | undefined;
  onValuesChange?: ((values: string[]) => void) | undefined;
  /**
   * Options for the current value(s) when they may not be in `options`, for
   * example after a server search. Used only for labels. Options seen in earlier
   * results are remembered too; an unknown value shows as its raw string.
   */
  valueOptions?: ComboboxOption[] | undefined;
  /** Text typed in the input (controlled). Use for server-side filtering. */
  inputValue?: string | undefined;
  onInputValueChange?: ((text: string) => void) | undefined;
  /** Custom filter. Default: case-insensitive match on label, hint and keywords. Pass `() => true` for server-filtered lists. */
  filter?: ((option: ComboboxOption, query: string) => boolean) | undefined;
  /** Order matches by quality: labels that start with the query, then a word that does, then the rest. Default false (keep the list order). */
  sortByMatch?: boolean | undefined;
  /**
   * Load options from a server. Called with the query (`''` when the list opens
   * with nothing typed) after `searchDebounce` ms. A newer query aborts the
   * previous call through `signal`, and late answers are ignored. Replaces `filter`.
   */
  onSearch?:
    ((query: string, context: ComboboxSearchContext) => Promise<ComboboxOption[]>) | undefined;
  /** Milliseconds to wait after typing before `onSearch`. Default 250. */
  searchDebounce?: number | undefined;
  /** Shortest query `onSearch` runs for; shorter queries show `emptyMessage`. Default 0. */
  minSearchLength?: number | undefined;
  /**
   * Adds a "Create “query”" option when the typed text matches no option's
   * label. Return the new option to select it; return nothing to only close
   * (single) or clear the text (multiple).
   */
  onCreate?:
    | ((
        query: string,
      ) => ComboboxOption | null | undefined | Promise<ComboboxOption | null | undefined>)
    | undefined;
  /** The list starts with an option that clears the selection, such as "Unassigned" or "No country". */
  clearOption?: ComboboxClearOption | undefined;
  /** Custom option content. Default: media, label, description, hint and a check mark. */
  renderOption?: ((option: ComboboxOption, state: ComboboxOptionState) => ReactNode) | undefined;
  /** Render at most this many matching options, with a message to keep typing. Default: all. */
  limit?: number | undefined;
  /** Shown while options load. */
  loading?: boolean | undefined;
  /** Shown when no option matches. */
  emptyMessage?: ReactNode | undefined;
  invalid?: boolean | undefined;
  size?: 'sm' | 'md' | 'lg' | undefined;
  /** Allow clearing the selection with the clear button / Backspace on empty input. */
  clearable?: boolean | undefined;
  /**
   * Show the list inline under a search input, always open, instead of in a
   * popover: for pickers inside a Popover, Dialog or Sheet. The input is a
   * search box: it never shows the selected label.
   */
  inline?: boolean | undefined;
  /** Close the list after a pick. Default: true, or false with `multiple` or `inline`. */
  closeOnSelect?: boolean | undefined;
  /** Whether the list is open (controlled). Ignored with `inline`. */
  open?: boolean | undefined;
  defaultOpen?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Accessible name of the listbox. Default: the Field label, `aria-label`, or "Options". */
  listLabel?: string | undefined;
  /** Interface text, for translation. */
  messages?: Partial<ComboboxMessages> | undefined;
}

/** True when `query` appears in the option's label, hint or keywords (case-insensitive). */
export function matchComboboxOption(option: ComboboxOption, query: string): boolean {
  const q = query.toLowerCase();
  return (
    option.label.toLowerCase().includes(q) ||
    (option.hint?.toLowerCase().includes(q) ?? false) ||
    (option.keywords?.toLowerCase().includes(q) ?? false)
  );
}

/** 0: label starts with the query; 1: a word in the label does; 2: anything else. */
function matchRank(option: ComboboxOption, query: string): number {
  const label = option.label.toLowerCase();
  const q = query.toLowerCase();
  if (label.startsWith(q)) return 0;
  if (label.includes(` ${q}`) || label.includes(`(${q}`) || label.includes(`-${q}`)) return 1;
  return 2;
}

type Item =
  | { kind: 'option'; option: ComboboxOption; group: string }
  | { kind: 'clear' }
  | { kind: 'create'; query: string };

function itemDisabled(item: Item | undefined): boolean {
  return !item || (item.kind === 'option' && !!item.option.disabled);
}

/**
 * Searchable select. The input is the combobox and the options are a listbox
 * (WAI-ARIA APG combobox pattern with `aria-activedescendant`): single or
 * multiple selection, grouped options with media and hints, async search with
 * debounce and abort, a create option and an inline mode for pickers.
 * Keyboard: Down/Up move, Home/End jump, PageDown/PageUp skip ten, Enter
 * selects, Escape closes, Alt+Down/Alt+Up open and close, typing filters.
 */
export const Combobox = forwardRef<HTMLInputElement, ComboboxProps>(function Combobox(
  {
    options = [],
    value: valueProp,
    defaultValue = null,
    onValueChange,
    multiple = false,
    values: valuesProp,
    defaultValues,
    onValuesChange,
    valueOptions,
    inputValue: inputProp,
    onInputValueChange,
    filter = matchComboboxOption,
    sortByMatch = false,
    onSearch,
    searchDebounce = 250,
    minSearchLength = 0,
    onCreate,
    clearOption,
    renderOption,
    limit,
    loading: loadingProp = false,
    emptyMessage = 'No matches',
    invalid,
    size = 'md',
    clearable = true,
    inline = false,
    closeOnSelect,
    open: openProp,
    defaultOpen = false,
    onOpenChange,
    listLabel,
    messages: messagesProp,
    placeholder,
    className,
    onKeyDown,
    onBlur,
    ...rest
  },
  ref,
) {
  const id = useId();
  const listId = `mn-combobox-${id}-list`;
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const field = useFieldContext();
  // In multiple and inline modes the input is a search box; in single mode it shows the selected label.
  const textIsQuery = multiple || inline;
  const shouldClose = closeOnSelect ?? !(multiple || inline);

  const [openState, setOpenState] = useState(defaultOpen);
  const open = inline ? true : (openProp ?? openState);
  const setOpen = useCallback(
    (next: boolean) => {
      if (inline) return;
      setOpenState(next);
      onOpenChange?.(next);
    },
    [inline, onOpenChange],
  );

  // ----- Selection -----
  const [internalValue, setInternalValue] = useState<string | null>(defaultValue);
  const [internalValues, setInternalValues] = useState<string[]>(defaultValues ?? []);
  const value = valueProp !== undefined ? valueProp : internalValue;
  const values = valuesProp ?? internalValues;
  const selectedValues = useMemo(
    () => (multiple ? values : value != null ? [value] : []),
    [multiple, values, value],
  );
  const selectedSet = useMemo(() => new Set(selectedValues), [selectedValues]);

  // ----- Async search -----
  const [results, setResults] = useState<ComboboxOption[] | null>(null);
  const [searchStatus, setSearchStatus] = useState<'idle' | 'loading' | 'error'>('idle');
  const [attempt, setAttempt] = useState(0);
  const onSearchRef = useRef(onSearch);
  onSearchRef.current = onSearch;
  const source = onSearch ? (results ?? options) : options;

  // Remember every option seen, so a value keeps its label after the list changes.
  const known = useRef(new Map<string, ComboboxOption>());
  for (const o of valueOptions ?? []) known.current.set(o.value, o);
  for (const o of source) known.current.set(o.value, o);
  const optionFor = (v: string): ComboboxOption =>
    source.find((o) => o.value === v) ?? known.current.get(v) ?? { value: v, label: v };

  const selected = !multiple && value != null ? optionFor(value) : null;
  const selectedLabel = selected?.label ?? '';

  // ----- Text -----
  const [internalInput, setInternalInput] = useState(textIsQuery ? '' : selectedLabel);
  const inputValue = inputProp !== undefined ? inputProp : internalInput;
  const setInput = (text: string) => {
    if (inputProp === undefined) setInternalInput(text);
    onInputValueChange?.(text);
  };

  // Keep the text in sync when the selection changes from outside.
  useEffect(() => {
    if (!textIsQuery && !open && inputProp === undefined) setInternalInput(selectedLabel);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedLabel]);

  const trimmed = inputValue.trim();
  const query = textIsQuery ? trimmed : trimmed !== '' && trimmed !== selectedLabel ? trimmed : '';

  const searchKey = onSearch && open ? query : null;
  useEffect(() => {
    if (searchKey === null) return;
    if (searchKey.length < minSearchLength) {
      setResults([]);
      setSearchStatus('idle');
      return;
    }
    const controller = new AbortController();
    setSearchStatus('loading');
    const timer = setTimeout(
      () => {
        const run = onSearchRef.current;
        if (!run) return;
        Promise.resolve()
          .then(() => run(searchKey, { signal: controller.signal }))
          .then(
            (next) => {
              if (controller.signal.aborted) return;
              setResults(next);
              setSearchStatus('idle');
            },
            () => {
              if (controller.signal.aborted) return;
              setSearchStatus('error');
            },
          );
      },
      searchKey === '' ? 0 : searchDebounce,
    );
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [searchKey, attempt, minSearchLength, searchDebounce]);

  const loading = loadingProp || searchStatus === 'loading';

  // ----- Visible list -----
  const { items, total } = useMemo(() => {
    let matches = onSearch || query === '' ? source : source.filter((o) => filter(o, query));
    if (sortByMatch && query !== '') {
      matches = matches
        .map((option, n) => ({ option, n, r: matchRank(option, query) }))
        .sort((a, b) => a.r - b.r || a.n - b.n)
        .map((x) => x.option);
    }
    const count = matches.length;
    if (limit !== undefined && matches.length > limit) matches = matches.slice(0, limit);
    // Group in order of first appearance; ungrouped options first.
    const groups = new Map<string, ComboboxOption[]>([['', []]]);
    for (const o of matches) {
      const key = o.group ?? '';
      const list = groups.get(key);
      if (list) list.push(o);
      else groups.set(key, [o]);
    }
    const out: Item[] = [];
    if (clearOption && query === '') out.push({ kind: 'clear' });
    for (const [group, list] of groups)
      for (const option of list) out.push({ kind: 'option', option, group });
    const lower = query.toLowerCase();
    if (
      onCreate &&
      query !== '' &&
      searchStatus !== 'loading' &&
      !source.some((o) => o.label.toLowerCase() === lower)
    )
      out.push({ kind: 'create', query });
    return { items: out, total: count };
  }, [source, query, filter, sortByMatch, limit, onSearch, clearOption, onCreate, searchStatus]);

  // ----- Active option -----
  const [active, setActive] = useState(0);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const listRef = useRef<HTMLDivElement | null>(null);

  // Opening (or mounting inline) puts the active option on the first selected one.
  useEffect(() => {
    if (!open) return;
    const at = items.findIndex(
      (it) =>
        (it.kind === 'option' && selectedSet.has(it.option.value)) ||
        (it.kind === 'clear' && selectedSet.size === 0),
    );
    setActive(Math.max(0, at));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  useEffect(() => {
    setActive((a) => Math.min(a, Math.max(0, items.length - 1)));
  }, [items.length]);

  useEffect(() => {
    if (!open) return;
    const el = listRef.current?.querySelector<HTMLElement>(`[data-index="${active}"]`);
    el?.scrollIntoView?.({ block: 'nearest' });
  }, [active, open]);

  const activeId = open && items[active] ? `${listId}-opt-${active}` : undefined;

  // ----- Announcements (multiple) -----
  const [announcement, setAnnouncement] = useState('');

  // ----- Commit -----
  const commitSingle = (next: ComboboxOption | null) => {
    if (next) known.current.set(next.value, next);
    if (valueProp === undefined) setInternalValue(next?.value ?? null);
    onValueChange?.(next?.value ?? null);
    setInput(textIsQuery ? '' : (next?.label ?? ''));
    if (shouldClose) setOpen(false);
  };

  const commitValues = (next: string[], changed: ComboboxOption, added: boolean) => {
    if (valuesProp === undefined) setInternalValues(next);
    onValuesChange?.(next);
    setAnnouncement(added ? messages.added(changed.label) : messages.removed(changed.label));
  };

  const toggle = (option: ComboboxOption) => {
    if (!multiple) {
      commitSingle(option);
      return;
    }
    known.current.set(option.value, option);
    const has = selectedSet.has(option.value);
    commitValues(
      has ? values.filter((v) => v !== option.value) : [...values, option.value],
      option,
      !has,
    );
    if (trimmed !== '') setInput('');
    if (shouldClose) setOpen(false);
  };

  const removeValue = (v: string) => {
    commitValues(
      values.filter((x) => x !== v),
      optionFor(v),
      false,
    );
  };

  const choose = (item: Item | undefined) => {
    if (!item || itemDisabled(item)) return;
    if (item.kind === 'option') toggle(item.option);
    else if (item.kind === 'clear') {
      if (multiple) {
        if (valuesProp === undefined) setInternalValues([]);
        onValuesChange?.([]);
        if (shouldClose) setOpen(false);
      } else commitSingle(null);
    } else if (onCreate) {
      void Promise.resolve(onCreate(item.query)).then((created) => {
        if (created) {
          if (multiple && selectedSet.has(created.value)) setInput('');
          else toggle(created);
        } else {
          setInput(textIsQuery ? '' : selectedLabel);
          if (!multiple) setOpen(false);
        }
      });
    }
  };

  const retry = () => setAttempt((n) => n + 1);

  const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
    // Radix's dismissable layer prevents default on Escape before this runs,
    // so only a consumer's own preventDefault stops the built-in handling.
    const preventedBefore = e.defaultPrevented;
    onKeyDown?.(e);
    if (e.defaultPrevented && !preventedBefore) return;
    const last = items.length - 1;
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        if (!open) setOpen(true);
        else if (!e.altKey) setActive((a) => Math.min(a + 1, last));
        break;
      case 'ArrowUp':
        e.preventDefault();
        if (e.altKey) {
          if (open) setOpen(false);
        } else if (!open) setOpen(true);
        else setActive((a) => Math.max(a - 1, 0));
        break;
      case 'PageDown':
        if (open) {
          e.preventDefault();
          setActive((a) => Math.min(a + 10, last));
        }
        break;
      case 'PageUp':
        if (open) {
          e.preventDefault();
          setActive((a) => Math.max(a - 10, 0));
        }
        break;
      case 'Home':
        if (open && items.length > 0) {
          e.preventDefault();
          setActive(0);
        }
        break;
      case 'End':
        if (open && items.length > 0) {
          e.preventDefault();
          setActive(last);
        }
        break;
      case 'Enter':
        if (open) {
          e.preventDefault();
          if (searchStatus === 'error' && items.length === 0) retry();
          else choose(items[active]);
        }
        break;
      case 'Escape':
        if (open && !inline) {
          e.preventDefault();
          setOpen(false);
          setInput(textIsQuery ? '' : selectedLabel);
        }
        break;
      case 'Backspace':
        if (inputValue !== '') break;
        if (multiple && values.length > 0) {
          const lastValue = values[values.length - 1];
          if (lastValue !== undefined) removeValue(lastValue);
        } else if (!multiple && !inline && clearable && selected) commitSingle(null);
        break;
    }
  };

  const fieldProps = useFieldControl(rest, { invalid });
  // The listbox is named by `listLabel`, else the Field label, else the input's aria-label.
  const listLabelledBy = !listLabel && field ? field.labelId : undefined;
  const listName = listLabel ?? rest['aria-label'];
  const isInvalid = fieldProps['aria-invalid'] === true;

  const renderDefault = (o: ComboboxOption, isSelected: boolean) => (
    <>
      {o.media ? (
        <span className="mn-combobox__media" aria-hidden>
          {o.media}
        </span>
      ) : null}
      <span className="mn-menu__text">
        {o.label}
        {o.description ? <span className="mn-combobox__description">{o.description}</span> : null}
      </span>
      {o.hint ? <span className="mn-combobox__hint">{o.hint}</span> : null}
      {isSelected ? (
        <span className="mn-menu__indicator">
          <CheckGlyph />
        </span>
      ) : null}
    </>
  );

  const renderItem = (item: Item, i: number) => {
    const isActive = i === active;
    let isSelected = false;
    let content: ReactNode;
    let key: string;
    let disabled = false;
    if (item.kind === 'option') {
      isSelected = selectedSet.has(item.option.value);
      disabled = !!item.option.disabled;
      key = `o:${item.group}:${item.option.value}`;
      content = renderOption
        ? renderOption(item.option, { selected: isSelected, active: isActive })
        : renderDefault(item.option, isSelected);
    } else if (item.kind === 'clear' && clearOption) {
      isSelected = selectedSet.size === 0;
      key = 'clear';
      content = renderDefault({ value: '', ...clearOption }, isSelected);
    } else {
      key = 'create';
      content = (
        <>
          <span className="mn-combobox__media mn-combobox__create-icon" aria-hidden>
            +
          </span>
          <span className="mn-menu__text">
            {messages.create(item.kind === 'create' ? item.query : '')}
          </span>
        </>
      );
    }
    return (
      // Options follow the aria-activedescendant pattern: the input keeps
      // focus and handles every key, so options are deliberately not focusable.
      // monarch-ignore-next-line a11y/click-without-key -- aria-activedescendant pattern: the input handles all keys
      // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/interactive-supports-focus
      <div
        key={key}
        id={`${listId}-opt-${i}`}
        role="option"
        aria-selected={isSelected}
        aria-disabled={disabled || undefined}
        data-index={i}
        data-highlighted={isActive || undefined}
        data-kind={item.kind === 'option' ? undefined : item.kind}
        className="mn-menu__item"
        onMouseEnter={() => setActive(i)}
        onMouseDown={(e) => e.preventDefault()}
        onClick={() => choose(item)}
      >
        {content}
      </div>
    );
  };

  // Split rendered items into groups for role="group" wrappers.
  const blocks: { group: string; start: number; end: number }[] = [];
  items.forEach((item, i) => {
    const group = item.kind === 'option' ? item.group : '';
    const prev = blocks[blocks.length - 1];
    if (prev && prev.group === group) prev.end = i;
    else blocks.push({ group, start: i, end: i });
  });

  const optionCount = items.filter((it) => it.kind === 'option').length;
  let status: ReactNode = null;
  if (searchStatus === 'error') {
    status = (
      <>
        {messages.error}{' '}
        <button
          type="button"
          className="mn-combobox__retry"
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => {
            retry();
            inputRef.current?.focus();
          }}
        >
          <RetryGlyph />
          {messages.retry}
        </button>
      </>
    );
  } else if (loading && optionCount === 0) status = messages.loading;
  else if (optionCount === 0 && !items.some((it) => it.kind === 'create')) status = emptyMessage;
  else if (limit !== undefined && total > limit) status = messages.more(limit, total);

  const list = (
    <>
      <div
        ref={listRef}
        id={listId}
        role="listbox"
        aria-label={listLabelledBy ? undefined : (listName ?? 'Options')}
        aria-labelledby={listLabelledBy}
        aria-multiselectable={multiple || undefined}
        aria-busy={loading || undefined}
        className="mn-combobox__list"
        data-inline={inline || undefined}
      >
        {blocks.map((b) => {
          const children = items
            .slice(b.start, b.end + 1)
            .map((it, k) => renderItem(it, b.start + k));
          if (!b.group) return children;
          const headingId = `${listId}-group-${b.start}`;
          return (
            <div key={`g:${b.group}`} role="group" aria-labelledby={headingId}>
              <div id={headingId} role="presentation" className="mn-menu__label">
                {b.group}
              </div>
              {children}
            </div>
          );
        })}
      </div>
      {status !== null ? (
        <div
          className="mn-combobox__status"
          role="status"
          data-tone={searchStatus === 'error' ? 'danger' : undefined}
        >
          {status}
        </div>
      ) : null}
    </>
  );

  const input = (
    <input
      ref={(node) => {
        inputRef.current = node;
        if (typeof ref === 'function') ref(node);
        else if (ref) ref.current = node;
      }}
      {...fieldProps}
      type="text"
      role="combobox"
      autoComplete="off"
      aria-autocomplete="list"
      aria-expanded={open}
      aria-controls={open ? listId : undefined}
      aria-activedescendant={activeId}
      className="mn-combobox__input"
      placeholder={multiple && values.length > 0 && !inline ? undefined : placeholder}
      value={inputValue}
      onChange={(e) => {
        setInput(e.target.value);
        setActive(0);
        if (!open) setOpen(true);
      }}
      onKeyDown={handleKey}
      onBlur={(e) => {
        onBlur?.(e);
        if (inline) return;
        // Revert unmatched text so the input always reflects the selection.
        if (!listRef.current?.parentElement?.contains(e.relatedTarget as Node | null))
          setInput(textIsQuery ? '' : selectedLabel);
      }}
    />
  );

  const live = multiple ? (
    <span className="mn-visually-hidden" role="status">
      {announcement}
    </span>
  ) : null;

  if (inline) {
    return (
      <div
        className={cx('mn-combobox__panel', className)}
        data-size={size}
        data-disabled={fieldProps.disabled || undefined}
      >
        <div className="mn-combobox__search" data-invalid={isInvalid || undefined}>
          <SearchGlyph className="mn-combobox__search-icon" />
          {input}
          {loading ? <Spinner size="sm" className="mn-combobox__spinner" /> : null}
        </div>
        {list}
        {live}
      </div>
    );
  }

  const chips =
    multiple && values.length > 0 ? (
      <ul className="mn-combobox__chips">
        {values.map((v) => {
          const o = optionFor(v);
          return (
            <li key={v} className="mn-combobox__chip">
              {o.media ? (
                <span className="mn-combobox__chip-media" aria-hidden>
                  {o.media}
                </span>
              ) : null}
              <span className="mn-combobox__chip-label">{o.label}</span>
              {fieldProps.disabled || rest.readOnly ? null : (
                <button
                  type="button"
                  className="mn-combobox__chip-remove"
                  aria-label={messages.remove(o.label)}
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => {
                    removeValue(v);
                    inputRef.current?.focus();
                  }}
                >
                  <CloseGlyph />
                </button>
              )}
            </li>
          );
        })}
      </ul>
    ) : null;

  return (
    <PopoverPrimitive.Root
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        if (!next) setInput(textIsQuery ? '' : selectedLabel);
      }}
    >
      <PopoverPrimitive.Anchor asChild>
        <div
          className={cx('mn-input', 'mn-combobox', className)}
          data-size={size}
          data-disabled={fieldProps.disabled || undefined}
          data-invalid={isInvalid || undefined}
          data-multiple={multiple || undefined}
        >
          {chips}
          {input}
          {loading ? <Spinner size="sm" className="mn-combobox__spinner" /> : null}
          <button
            type="button"
            tabIndex={-1}
            aria-label={open ? messages.closeOptions : messages.showOptions}
            className="mn-combobox__toggle"
            disabled={fieldProps.disabled}
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => {
              setOpen(!open);
              inputRef.current?.focus();
            }}
          >
            <ChevronDownGlyph />
          </button>
          {live}
        </div>
      </PopoverPrimitive.Anchor>
      <PopoverPrimitive.Portal>
        <PopoverPrimitive.Content
          align="start"
          sideOffset={4}
          onOpenAutoFocus={(e) => e.preventDefault()}
          onCloseAutoFocus={(e) => e.preventDefault()}
          onInteractOutside={(e) => {
            if (inputRef.current?.parentElement?.contains(e.target as Node)) e.preventDefault();
          }}
          // The popup is the listbox inside, not a dialog: the input keeps focus throughout.
          role="presentation"
          className="mn-menu mn-combobox__content"
        >
          {list}
        </PopoverPrimitive.Content>
      </PopoverPrimitive.Portal>
    </PopoverPrimitive.Root>
  );
});
