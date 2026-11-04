'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState,
  type InputHTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph } from '../../../internal/icons.js';
import { useFieldControl } from '../../forms/form-field/index.js';
import { Spinner } from '../../feedback/spinner/index.js';
import { Kbd } from '../../typography/kbd/index.js';

export interface SearchInputProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'value' | 'defaultValue' | 'size' | 'type'
> {
  /** The query text (controlled). */
  value?: string;
  /** Initial query text when uncontrolled. */
  defaultValue?: string;
  /** Called on every keystroke and on clear, with the new text. */
  onValueChange?: (value: string) => void;
  /**
   * Called with the query to run: after `debounce` ms of no typing, at once on
   * Enter, and with `''` when the field is cleared. Use this for fetching.
   */
  onSearch?: (query: string) => void;
  /** Milliseconds to wait after the last keystroke before calling `onSearch`. Default 0 (no wait). */
  debounce?: number;
  /** A search is in flight: the leading icon becomes a spinner and the input is `aria-busy`. */
  pending?: boolean;
  /** Show a clear button while there is text. Default true. */
  clearable?: boolean;
  /** Accessible name of the clear button. Default "Clear search". */
  clearLabel?: string;
  /** Called after the field is cleared by the button or Escape. */
  onClear?: () => void;
  /** Hint shown at the trailing edge while the field is empty, e.g. "/" or "⌘K". Decorative. */
  shortcut?: ReactNode;
  /**
   * The key combination that focuses the field from anywhere on the page, in
   * `aria-keyshortcuts` syntax ("/", "Meta+K", "Control+K"). Setting it binds the
   * listener and sets `aria-keyshortcuts`; single keys are ignored while typing
   * in another field.
   */
  shortcutKeys?: string;
  /** Replaces the leading magnifier icon. Decorative. Pass `null` for none. */
  icon?: ReactNode;
  size?: 'sm' | 'md' | 'lg';
  /** Marks the value invalid. Inside a Field, the Field's `invalid` is used. */
  invalid?: boolean;
  /** Class for the outer wrapper (the input itself is `.mn-search-input__input`). */
  className?: string;
}

function SearchGlyph() {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      aria-hidden
      focusable={false}
    >
      <circle cx="7" cy="7" r="4.5" />
      <path d="M10.5 10.5L14 14" />
    </svg>
  );
}

function isEditable(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  return (
    target.isContentEditable ||
    target.tagName === 'INPUT' ||
    target.tagName === 'TEXTAREA' ||
    target.tagName === 'SELECT'
  );
}

/**
 * True when a keyboard event matches one `aria-keyshortcuts` combination such
 * as "Meta+K" or "/". Modifiers that are not named must not be held (Shift is
 * ignored for single printable keys, since it is how some symbols are typed).
 */
function matchesShortcut(event: globalThis.KeyboardEvent, combo: string): boolean {
  const parts = combo.split('+').map((p) => p.trim());
  const key = parts.pop();
  if (!key) return false;
  const mods = new Set(parts.map((p) => p.toLowerCase()));
  const want = {
    ctrl: mods.has('control') || mods.has('ctrl'),
    meta: mods.has('meta'),
    alt: mods.has('alt'),
    shift: mods.has('shift'),
  };
  if (event.ctrlKey !== want.ctrl || event.metaKey !== want.meta || event.altKey !== want.alt)
    return false;
  if (want.shift && !event.shiftKey) return false;
  if (!want.shift && event.shiftKey && key.length > 1) return false;
  return event.key.toLowerCase() === key.toLowerCase();
}

/**
 * A search box: leading icon, clear button, optional debounce for fetching,
 * a pending state, and an optional keyboard shortcut that focuses it.
 * Renders a native `type="search"` input, so give it a visible label (Field)
 * or an `aria-label`.
 */
export const SearchInput = forwardRef<HTMLInputElement, SearchInputProps>(function SearchInput(
  {
    value: valueProp,
    defaultValue = '',
    onValueChange,
    onSearch,
    debounce = 0,
    pending = false,
    clearable = true,
    clearLabel = 'Clear search',
    onClear,
    shortcut,
    shortcutKeys,
    icon,
    size = 'md',
    invalid,
    className,
    onChange,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const [internal, setInternal] = useState(defaultValue);
  const value = valueProp !== undefined ? valueProp : internal;
  const inputRef = useRef<HTMLInputElement | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const searchRef = useRef(onSearch);
  searchRef.current = onSearch;

  const cancel = useCallback(() => {
    if (timer.current !== null) {
      clearTimeout(timer.current);
      timer.current = null;
    }
  }, []);
  useEffect(() => cancel, [cancel]);

  const runSearch = (query: string, immediate: boolean) => {
    cancel();
    if (!searchRef.current) return;
    if (immediate || debounce <= 0) searchRef.current(query);
    else
      timer.current = setTimeout(() => {
        timer.current = null;
        searchRef.current?.(query);
      }, debounce);
  };

  const setValue = (next: string) => {
    if (valueProp === undefined) setInternal(next);
    onValueChange?.(next);
  };

  const clear = () => {
    setValue('');
    runSearch('', true);
    onClear?.();
    inputRef.current?.focus();
  };

  useEffect(() => {
    if (!shortcutKeys) return;
    const combos = shortcutKeys.split(' ').filter(Boolean);
    const onKey = (e: globalThis.KeyboardEvent) => {
      if (e.defaultPrevented) return;
      const combo = combos.find((c) => matchesShortcut(e, c));
      if (!combo) return;
      const plain = !/(control|ctrl|meta|alt)\+/i.test(combo);
      if (plain && isEditable(e.target)) return;
      const input = inputRef.current;
      if (!input || input.disabled) return;
      e.preventDefault();
      input.focus();
      input.select();
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [shortcutKeys]);

  const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
    onKeyDown?.(e);
    if (e.defaultPrevented) return;
    if (e.key === 'Enter') {
      runSearch(value, true);
    } else if (e.key === 'Escape' && value !== '') {
      // Escape clears first; a second Escape (empty field) reaches dialogs and popovers.
      e.preventDefault();
      e.stopPropagation();
      clear();
    }
  };

  const fieldProps = useFieldControl(rest, { invalid });
  const showClear = clearable && value !== '' && !fieldProps.disabled && !rest.readOnly;
  const leading = pending ? <Spinner size="sm" /> : icon === undefined ? <SearchGlyph /> : icon;

  return (
    <div
      className={cx('mn-input', 'mn-search-input', className)}
      data-size={size}
      data-pending={pending || undefined}
      data-disabled={fieldProps.disabled || undefined}
      data-invalid={fieldProps['aria-invalid'] ? true : undefined}
    >
      {leading ? (
        <span className="mn-search-input__icon" aria-hidden>
          {leading}
        </span>
      ) : null}
      <input
        ref={(node) => {
          inputRef.current = node;
          if (typeof ref === 'function') ref(node);
          else if (ref) ref.current = node;
        }}
        autoComplete="off"
        spellCheck={false}
        enterKeyHint="search"
        {...fieldProps}
        type="search"
        aria-keyshortcuts={shortcutKeys || undefined}
        aria-busy={pending || undefined}
        className="mn-search-input__input"
        value={value}
        onChange={(e) => {
          onChange?.(e);
          setValue(e.target.value);
          runSearch(e.target.value, false);
        }}
        onKeyDown={handleKey}
      />
      {showClear ? (
        <button
          type="button"
          className="mn-search-input__clear"
          aria-label={clearLabel}
          onMouseDown={(e) => e.preventDefault()}
          onClick={clear}
        >
          <CloseGlyph />
        </button>
      ) : shortcut && value === '' ? (
        <Kbd className="mn-search-input__shortcut" aria-hidden>
          {shortcut}
        </Kbd>
      ) : null}
    </div>
  );
});
