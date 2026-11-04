'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type ClipboardEvent,
  type InputHTMLAttributes,
  type KeyboardEvent,
} from 'react';
import * as PopoverPrimitive from '@radix-ui/react-popover';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph } from '../../../internal/icons.js';
import { useFieldControl } from '../form-field/index.js';

export interface TagInputProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'value' | 'defaultValue' | 'onChange' | 'size' | 'type' | 'list'
> {
  /** The tags (controlled). */
  value?: string[];
  /** Initial tags when uncontrolled. */
  defaultValue?: string[];
  /** Called with the full new list whenever a tag is added or removed. */
  onValueChange?: (tags: string[]) => void;
  /** Text being typed (controlled). Use it to fetch suggestions from a server. */
  inputValue?: string;
  onInputValueChange?: (text: string) => void;
  /**
   * Characters that commit the typed text as a tag, besides Enter. They also
   * split pasted text, together with new lines and tabs. Default `[',']`.
   */
  separators?: string[];
  /** Cleans typed text before it is checked and added. Default: trims whitespace. */
  normalize?: (raw: string) => string;
  /**
   * Checks a tag before it is added. Return a message to reject it (shown under
   * the field and announced), or nothing to accept it.
   */
  validate?: (tag: string, tags: readonly string[]) => string | null | undefined;
  /** Allow the same tag twice. Duplicates are compared case-insensitively. Default false. */
  allowDuplicates?: boolean;
  /** Maximum number of tags. Further additions are rejected with `messages.max`. */
  max?: number;
  /** Values offered in a listbox while typing. Already added tags are left out. */
  suggestions?: string[];
  /** Which suggestions match the typed text. Default: case-insensitive substring. Pass `() => true` for server-filtered lists. */
  filterSuggestions?: (suggestion: string, query: string) => boolean;
  /** Commit the typed text when the field loses focus. Default false. */
  addOnBlur?: boolean;
  /** Accessible name of each tag's remove button. Default `Remove <tag>`. */
  removeLabel?: (tag: string) => string;
  /** Override the built-in messages (rejections and screen reader announcements). */
  messages?: Partial<TagInputMessages>;
  /** Form field name: one hidden input per tag is rendered with this name. */
  name?: string;
  size?: 'sm' | 'md' | 'lg';
  /** Marks the value invalid. Inside a Field, the Field's `invalid` is used. */
  invalid?: boolean;
  /** Class for the outer wrapper (the text input is `.mn-tag-input__input`). */
  className?: string;
}

export interface TagInputMessages {
  duplicate: (tag: string) => string;
  max: (max: number) => string;
  added: (tag: string) => string;
  removed: (tag: string) => string;
  /** Accessible name of the suggestions listbox. */
  suggestions: string;
}

const defaultMessages: TagInputMessages = {
  duplicate: (tag) => `“${tag}” is already added.`,
  max: (max) => `You can add up to ${max}.`,
  added: (tag) => `Added ${tag}.`,
  removed: (tag) => `Removed ${tag}.`,
  suggestions: 'Suggestions',
};

const defaultFilter = (s: string, q: string) => s.toLowerCase().includes(q.toLowerCase());
const defaultNormalize = (raw: string) => raw.trim();
const escapeRe = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

type FocusTarget = number | 'input' | null;

/**
 * Edits a list of short values as removable chips: skills, labels, recipient
 * addresses. Enter or a separator adds the typed text, pasted lists are split,
 * a validator can reject values, and optional suggestions appear in a listbox.
 */
export const TagInput = forwardRef<HTMLInputElement, TagInputProps>(function TagInput(
  {
    value: valueProp,
    defaultValue = [],
    onValueChange,
    inputValue: inputProp,
    onInputValueChange,
    separators = [','],
    normalize = defaultNormalize,
    validate,
    allowDuplicates = false,
    max,
    suggestions,
    filterSuggestions = defaultFilter,
    addOnBlur = false,
    removeLabel = (tag) => `Remove ${tag}`,
    messages: messagesProp,
    name,
    size = 'md',
    invalid,
    className,
    placeholder,
    onKeyDown,
    onBlur,
    onPaste,
    ...rest
  },
  ref,
) {
  const id = useId();
  const listId = `mn-tag-input-${id}-list`;
  const messageId = `mn-tag-input-${id}-message`;
  const messages = { ...defaultMessages, ...messagesProp };

  const [internalTags, setInternalTags] = useState<string[]>(defaultValue);
  const tags = valueProp !== undefined ? valueProp : internalTags;
  const [internalInput, setInternalInput] = useState('');
  const text = inputProp !== undefined ? inputProp : internalInput;
  const [message, setMessage] = useState<string | null>(null);
  const [announcement, setAnnouncement] = useState('');
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);

  const inputRef = useRef<HTMLInputElement | null>(null);
  const controlRef = useRef<HTMLDivElement | null>(null);
  const removeRefs = useRef<Array<HTMLButtonElement | null>>([]);
  const focusAfter = useRef<FocusTarget>(null);

  const setTags = (next: string[]) => {
    if (valueProp === undefined) setInternalTags(next);
    onValueChange?.(next);
  };
  const setText = (next: string) => {
    if (inputProp === undefined) setInternalInput(next);
    onInputValueChange?.(next);
  };

  // Move focus once the list has re-rendered after a removal.
  useEffect(() => {
    const target = focusAfter.current;
    if (target === null) return;
    focusAfter.current = null;
    if (target === 'input') inputRef.current?.focus();
    else (removeRefs.current[target] ?? inputRef.current)?.focus();
  }, [tags]);

  const fieldProps = useFieldControl(
    {
      ...rest,
      'aria-describedby': cx(rest['aria-describedby'], message ? messageId : undefined),
    },
    { invalid: invalid || (message ? true : undefined) },
  );
  const disabled = !!fieldProps.disabled;
  const readOnly = !!rest.readOnly;
  const editable = !disabled && !readOnly;

  const visible = useMemo(() => {
    if (!suggestions) return [];
    const taken = new Set(tags.map((t) => t.toLowerCase()));
    const q = text.trim();
    return suggestions.filter(
      (s) => !taken.has(s.toLowerCase()) && (q === '' || filterSuggestions(s, q)),
    );
  }, [suggestions, tags, text, filterSuggestions]);
  const listOpen = open && visible.length > 0 && editable;

  useEffect(() => {
    setActive((a) => Math.min(a, visible.length - 1));
  }, [visible.length]);

  /** Adds each value in order; returns the ones that were rejected. */
  const addMany = (raws: string[]): string[] => {
    const next = [...tags];
    const added: string[] = [];
    const rejected: string[] = [];
    let error: string | null = null;
    for (const raw of raws) {
      const tag = normalize(raw);
      if (!tag) continue;
      let reason: string | null | undefined = null;
      if (max !== undefined && next.length >= max) reason = messages.max(max);
      else if (!allowDuplicates && next.some((t) => t.toLowerCase() === tag.toLowerCase()))
        reason = messages.duplicate(tag);
      else reason = validate?.(tag, next);
      if (reason) {
        error ??= reason;
        rejected.push(raw.trim());
      } else {
        next.push(tag);
        added.push(tag);
      }
    }
    if (added.length > 0) {
      setTags(next);
      setAnnouncement(added.map((t) => messages.added(t)).join(' '));
    }
    setMessage(error);
    return rejected;
  };

  const commitText = (raw = text) => {
    if (raw.trim() === '') return;
    const rejected = addMany([raw]);
    setText(rejected.join(', '));
    setActive(-1);
  };

  const remove = (index: number, focus: FocusTarget) => {
    const tag = tags[index];
    if (tag === undefined || !editable) return;
    focusAfter.current = focus;
    setTags(tags.filter((_, i) => i !== index));
    setAnnouncement(messages.removed(tag));
    setMessage(null);
  };

  const focusRemove = (index: number) => {
    if (index < 0) return;
    if (index >= tags.length) inputRef.current?.focus();
    else removeRefs.current[index]?.focus();
  };

  const handleInputKey = (e: KeyboardEvent<HTMLInputElement>) => {
    onKeyDown?.(e);
    if (e.defaultPrevented || !editable) return;
    const caretAtStart = e.currentTarget.selectionStart === 0 && e.currentTarget.selectionEnd === 0;
    if (e.key === 'Enter') {
      if (listOpen && active >= 0 && visible[active] !== undefined) {
        e.preventDefault();
        commitText(visible[active]);
        setOpen(false);
      } else if (text.trim() !== '') {
        e.preventDefault();
        commitText();
      }
    } else if (separators.includes(e.key)) {
      e.preventDefault();
      commitText();
    } else if (e.key === 'Backspace' && text === '' && tags.length > 0) {
      e.preventDefault();
      focusRemove(tags.length - 1);
    } else if (e.key === 'ArrowLeft' && caretAtStart && tags.length > 0) {
      e.preventDefault();
      focusRemove(tags.length - 1);
    } else if (e.key === 'ArrowDown' && suggestions) {
      e.preventDefault();
      if (!open) setOpen(true);
      setActive((a) => Math.min(a + 1, visible.length - 1));
    } else if (e.key === 'ArrowUp' && suggestions) {
      e.preventDefault();
      setActive((a) => Math.max(a - 1, 0));
    } else if (e.key === 'Escape' && listOpen) {
      e.preventDefault();
      setOpen(false);
      setActive(-1);
    }
  };

  const handleRemoveKey = (e: KeyboardEvent<HTMLButtonElement>, index: number) => {
    switch (e.key) {
      case 'Backspace':
        e.preventDefault();
        remove(index, index > 0 ? index - 1 : tags.length > 1 ? 0 : 'input');
        break;
      case 'Delete':
        e.preventDefault();
        remove(index, index < tags.length - 1 ? index : 'input');
        break;
      case 'ArrowLeft':
        e.preventDefault();
        focusRemove(Math.max(index - 1, 0));
        break;
      case 'ArrowRight':
        e.preventDefault();
        focusRemove(index + 1);
        break;
    }
  };

  const handlePaste = (e: ClipboardEvent<HTMLInputElement>) => {
    onPaste?.(e);
    if (e.defaultPrevented || !editable) return;
    const pasted = e.clipboardData.getData('text');
    const splitter = new RegExp(`[${separators.map(escapeRe).join('')}\\n\\r\\t]`);
    if (!splitter.test(pasted)) return;
    e.preventDefault();
    const rejected = addMany((text + pasted).split(splitter));
    setText(rejected.join(', '));
  };

  const activeId = listOpen && active >= 0 ? `${listId}-opt-${active}` : undefined;
  const comboboxProps = suggestions
    ? {
        role: 'combobox' as const,
        'aria-autocomplete': 'list' as const,
        'aria-expanded': listOpen,
        'aria-controls': listOpen ? listId : undefined,
        'aria-activedescendant': activeId,
      }
    : {};

  return (
    <div className={cx('mn-tag-input', className)} data-size={size}>
      <PopoverPrimitive.Root open={listOpen} onOpenChange={setOpen}>
        <PopoverPrimitive.Anchor asChild>
          <div
            ref={controlRef}
            className="mn-input mn-tag-input__control"
            data-size={size}
            data-disabled={disabled || undefined}
            data-invalid={fieldProps['aria-invalid'] ? true : undefined}
          >
            {tags.length > 0 ? (
              <ul className="mn-tag-input__list">
                {tags.map((tag, i) => (
                  <li key={`${tag}-${i}`} className="mn-tag-input__tag">
                    <span className="mn-tag-input__label">{tag}</span>
                    {editable ? (
                      <button
                        ref={(node) => {
                          removeRefs.current[i] = node;
                        }}
                        type="button"
                        className="mn-tag-input__remove"
                        aria-label={removeLabel(tag)}
                        onClick={() => remove(i, 'input')}
                        onKeyDown={(e) => handleRemoveKey(e, i)}
                      >
                        <CloseGlyph />
                      </button>
                    ) : null}
                  </li>
                ))}
              </ul>
            ) : null}
            <input
              ref={(node) => {
                inputRef.current = node;
                if (typeof ref === 'function') ref(node);
                else if (ref) ref.current = node;
              }}
              autoComplete="off"
              {...fieldProps}
              {...comboboxProps}
              type="text"
              className="mn-tag-input__input"
              placeholder={tags.length === 0 ? placeholder : undefined}
              value={text}
              onChange={(e) => {
                setText(e.target.value);
                setMessage(null);
                setActive(-1);
                if (suggestions) setOpen(true);
              }}
              onKeyDown={handleInputKey}
              onPaste={handlePaste}
              onBlur={(e) => {
                onBlur?.(e);
                const into = e.relatedTarget as Node | null;
                if (controlRef.current?.contains(into)) return;
                if (document.getElementById(listId)?.contains(into)) return;
                if (addOnBlur && editable) commitText();
                setOpen(false);
              }}
            />
          </div>
        </PopoverPrimitive.Anchor>
        {suggestions ? (
          <PopoverPrimitive.Portal>
            <PopoverPrimitive.Content
              align="start"
              sideOffset={4}
              onOpenAutoFocus={(e) => e.preventDefault()}
              onCloseAutoFocus={(e) => e.preventDefault()}
              onInteractOutside={(e) => {
                if (controlRef.current?.contains(e.target as Node)) e.preventDefault();
              }}
              className="mn-menu mn-tag-input__content"
            >
              <div
                id={listId}
                role="listbox"
                aria-label={messages.suggestions}
                className="mn-tag-input__listbox"
              >
                {visible.map((s, i) => (
                  // aria-activedescendant pattern: the input keeps focus and handles every key.
                  // monarch-ignore-next-line a11y/click-without-key -- aria-activedescendant pattern: the input handles all keys
                  // eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/interactive-supports-focus
                  <div
                    key={s}
                    id={`${listId}-opt-${i}`}
                    role="option"
                    aria-selected={i === active}
                    data-highlighted={i === active || undefined}
                    className="mn-menu__item"
                    onMouseEnter={() => setActive(i)}
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => {
                      commitText(s);
                      inputRef.current?.focus();
                    }}
                  >
                    <span className="mn-menu__text">{s}</span>
                  </div>
                ))}
              </div>
            </PopoverPrimitive.Content>
          </PopoverPrimitive.Portal>
        ) : null}
      </PopoverPrimitive.Root>
      {message ? (
        <p id={messageId} className="mn-tag-input__message" aria-live="polite">
          {message}
        </p>
      ) : null}
      <span className="mn-visually-hidden" role="status">
        {announcement}
      </span>
      {name ? tags.map((tag, i) => <input key={i} type="hidden" name={name} value={tag} />) : null}
    </div>
  );
});
