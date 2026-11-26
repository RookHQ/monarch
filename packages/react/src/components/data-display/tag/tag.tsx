'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  type HTMLAttributes,
  type KeyboardEvent,
  type MouseEvent,
  type ReactNode,
} from 'react';
import { useIsomorphicLayoutEffect } from '../../../internal/layout-effect.js';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, CloseGlyph } from '../../../internal/icons.js';
import { useControllable } from '../../../internal/use-controllable.js';

// useIsomorphicLayoutEffect warns during server rendering; TagGroup only uses it to move focus.
const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useIsomorphicLayoutEffect;

/** The colour role of a tag. */
export type TagTone = 'neutral' | 'accent' | 'info' | 'success' | 'warning' | 'danger';

/** The words Tag speaks. Override them to translate. */
export interface TagMessages {
  /** Accessible name of the remove button. Default "Remove {label}" ("Remove tag" when the label is not text). */
  remove: (label: string | null) => string;
}

const DEFAULT_MESSAGES: TagMessages = {
  remove: (label) => (label ? `Remove ${label}` : 'Remove tag'),
};

export interface TagProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /** The tag's text ("Design", "Priority: high"). */
  children: ReactNode;
  /** Colour role. Default `neutral`. Exposed as `data-tone`. */
  tone?: TagTone | undefined;
  /** `sm` for dense rows, `md` (default). */
  size?: 'sm' | 'md' | undefined;
  /** Decorative icon before the label. */
  icon?: ReactNode;
  /**
   * Called when the remove button is pressed, or Backspace or Delete is
   * pressed on the tag. Giving it makes the tag removable. Remove the tag from
   * your data here; inside a TagGroup focus then moves to a neighbouring tag.
   */
  onRemove?: (() => void) | undefined;
  /** Whether a selectable tag is on (controlled). Giving it makes the tag a toggle button. */
  selected?: boolean | undefined;
  /** Whether a selectable tag starts on (uncontrolled). Giving it makes the tag a toggle button. */
  defaultSelected?: boolean | undefined;
  /** Called when a selectable tag is toggled. Giving it makes the tag a toggle button. */
  onSelectedChange?: ((selected: boolean) => void) | undefined;
  /** The tag cannot be toggled or removed. */
  disabled?: boolean | undefined;
  /** Words to speak; merged over the English defaults. */
  messages?: Partial<TagMessages> | undefined;
}

function textOf(node: ReactNode): string | null {
  return typeof node === 'string' || typeof node === 'number' ? String(node) : null;
}

/**
 * A chip for user data that people act on: a label they can remove, or a
 * filter they can switch on and off. For a read-only status, use Badge.
 */
export const Tag = forwardRef<HTMLSpanElement, TagProps>(function Tag(
  {
    children,
    tone = 'neutral',
    size = 'md',
    icon,
    onRemove,
    selected: selectedProp,
    defaultSelected,
    onSelectedChange,
    disabled = false,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const selectable =
    selectedProp !== undefined || defaultSelected !== undefined || onSelectedChange !== undefined;
  const [selected, setSelected] = useControllable(
    selectedProp,
    defaultSelected ?? false,
    onSelectedChange,
  );
  const removable = onRemove !== undefined;

  // On the buttons, not the root, so only a focused tag reacts.
  const handleKeyDown = (event: KeyboardEvent<HTMLButtonElement>) => {
    if (event.defaultPrevented || disabled || !onRemove) return;
    if (event.key === 'Backspace' || event.key === 'Delete') {
      event.preventDefault();
      onRemove();
    }
  };

  const content = (
    <>
      {selectable && selected ? <CheckGlyph className="mn-tag__check" /> : null}
      {icon ? (
        <span className="mn-tag__icon" aria-hidden="true">
          {icon}
        </span>
      ) : null}
      <span className="mn-tag__text">{children}</span>
    </>
  );

  return (
    <span
      ref={ref}
      {...rest}
      className={cx('mn-tag', className)}
      data-tone={tone}
      data-size={size}
      data-selectable={selectable || undefined}
      data-selected={(selectable && selected) || undefined}
      data-removable={removable || undefined}
      data-disabled={disabled || undefined}
    >
      {selectable ? (
        <button
          type="button"
          className="mn-tag__toggle"
          aria-pressed={selected}
          disabled={disabled}
          onClick={() => setSelected(!selected)}
          onKeyDown={handleKeyDown}
        >
          {content}
        </button>
      ) : (
        <span className="mn-tag__label">{content}</span>
      )}
      {removable ? (
        <button
          type="button"
          className="mn-tag__remove"
          aria-label={messages.remove(textOf(children))}
          disabled={disabled}
          onKeyDown={handleKeyDown}
          onClick={(event: MouseEvent<HTMLButtonElement>) => {
            event.stopPropagation();
            onRemove();
          }}
        >
          <CloseGlyph />
        </button>
      ) : null}
    </span>
  );
});

export interface TagGroupProps extends HTMLAttributes<HTMLDivElement> {
  /** Accessible name of the group, e.g. "Labels" or "Filters". Required unless `aria-labelledby` is given. */
  label?: string | undefined;
}

const FOCUSABLE = '.mn-tag__toggle:not(:disabled), .mn-tag__remove:not(:disabled)';

/**
 * A wrapping row of Tags with an accessible name. When a tag inside is
 * removed, focus moves to the next tag (or the previous one, or the group)
 * instead of being lost.
 */
export const TagGroup = forwardRef<HTMLDivElement, TagGroupProps>(function TagGroup(
  { label, className, onClickCapture, onKeyDownCapture, children, ...rest },
  forwardedRef,
) {
  const inner = useRef<HTMLDivElement | null>(null);
  const pending = useRef<{ index: number; count: number } | null>(null);
  const setRef = useCallback(
    (node: HTMLDivElement | null) => {
      inner.current = node;
      if (typeof forwardedRef === 'function') forwardedRef(node);
      else if (forwardedRef) forwardedRef.current = node;
    },
    [forwardedRef],
  );

  const remember = (target: EventTarget) => {
    const root = inner.current;
    if (!root || !(target instanceof Element)) return;
    const tag = target.closest('.mn-tag');
    if (!tag || !tag.hasAttribute('data-removable')) return;
    const tags = Array.from(root.querySelectorAll('.mn-tag'));
    pending.current = { index: tags.indexOf(tag), count: tags.length };
  };

  // After a removal re-renders the children, focus the tag now at the same place.
  useIsoLayoutEffect(() => {
    const root = inner.current;
    const p = pending.current;
    if (!root || !p) return;
    const tags = Array.from(root.querySelectorAll<HTMLElement>('.mn-tag'));
    if (tags.length >= p.count) return;
    pending.current = null;
    const next = tags[Math.min(p.index, tags.length - 1)];
    const target = next?.querySelector<HTMLElement>(FOCUSABLE);
    (target ?? root).focus();
  });

  return (
    <div
      ref={setRef}
      role="group"
      aria-label={label}
      tabIndex={-1}
      {...rest}
      className={cx('mn-tag-group', className)}
      onClickCapture={(event) => {
        onClickCapture?.(event);
        if (event.target instanceof Element && event.target.closest('.mn-tag__remove')) {
          remember(event.target);
        }
      }}
      onKeyDownCapture={(event) => {
        onKeyDownCapture?.(event);
        if (event.key === 'Backspace' || event.key === 'Delete') remember(event.target);
      }}
    >
      {children}
    </div>
  );
});
