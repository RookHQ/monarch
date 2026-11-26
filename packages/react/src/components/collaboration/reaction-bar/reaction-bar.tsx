'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { IconButton } from '../../actions/button/index.js';
import {
  EmojiPickerPopover,
  type EmojiPickerCategory,
  type EmojiPickerMessages,
} from '../emoji-picker/index.js';
import { useDirection } from '../../../internal/direction.js';

/** One emoji reaction with its count. */
export interface ReactionBarItem {
  /** The emoji shown on the chip, such as `👍`. Also the value passed to `onReactionToggle`. */
  emoji: string;
  /** How many people reacted. Chips with a count of 0 are not shown. */
  count: number;
  /** The current person reacted. The chip shows pressed. */
  reacted?: boolean | undefined;
  /** Spoken name of the emoji, such as "thumbs up". Default: the emoji itself. */
  label?: string | undefined;
  /** Who reacted, such as "Ana, Ben and 3 others". The chip's hover title and accessible description. */
  title?: string | undefined;
}

export interface ReactionBarMessages {
  /** Accessible name of the bar. Default "Reactions". */
  label: string;
  /** Accessible name of a chip. Default "{label}, {count} reaction(s)". */
  reaction: (label: string, count: number) => string;
  /** Accessible name of the add button. Default "Add reaction". */
  add: string;
}

const DEFAULT_MESSAGES: ReactionBarMessages = {
  label: 'Reactions',
  reaction: (label, count) => `${label}, ${count} ${count === 1 ? 'reaction' : 'reactions'}`,
  add: 'Add reaction',
};

export interface ReactionBarProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The reactions, in display order (usually first reacted first). */
  reactions: readonly ReactionBarItem[];
  /**
   * Called with the emoji and the new state when a chip is pressed, or with
   * `true` when an emoji is picked from the add button. The product updates
   * `reactions`. Without it the bar is read-only and has no add button.
   */
  onReactionToggle?: ((emoji: string, reacted: boolean) => void) | undefined;
  /** Show the "+" add button that opens the emoji picker. Default `true` when `onReactionToggle` is set. */
  addable?: boolean | undefined;
  /** Emoji shown first in the picker (its Recent row), such as the product's quick reactions. */
  quickReactions?: readonly string[] | undefined;
  /** Emoji set for the picker. Default: the EmojiPicker's built-in set. */
  categories?: readonly EmojiPickerCategory[] | undefined;
  /** At most this many distinct reactions; the add button hides when reached. Default: no limit. */
  max?: number | undefined;
  /** `sm` (default) for messages and comments, `md` for posts. Exposed as `data-size`. */
  size?: 'sm' | 'md' | undefined;
  /** Interface text, for translation. */
  messages?: Partial<ReactionBarMessages> | undefined;
  /** Interface text of the emoji picker, for translation. */
  pickerMessages?: Partial<EmojiPickerMessages> | undefined;
}

function AddReactionGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      {...props}
    >
      <path d="M13.9 8.6A6 6 0 1 1 7.4 2.1" />
      <path d="M5.25 9.5c.6.9 1.5 1.4 2.5 1.4s1.9-.5 2.5-1.4M5.75 6.25h.01M9.75 6.25h.01M12.5 1.5v4M10.5 3.5h4" />
    </svg>
  );
}

/**
 * The emoji reactions under a message, comment or post: one toggle chip per
 * emoji with its count, pressed when you reacted, and a "+" button that
 * opens the emoji picker. Interactive bars are a toolbar (one tab stop,
 * arrow keys between chips); every chip is named with its emoji and count.
 */
export const ReactionBar = forwardRef<HTMLDivElement, ReactionBarProps>(function ReactionBar(
  {
    reactions,
    onReactionToggle,
    addable,
    quickReactions,
    categories,
    max,
    size = 'sm',
    messages,
    pickerMessages,
    className,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const readDir = useDirection();
  const m = { ...DEFAULT_MESSAGES, ...messages };
  const shown = reactions.filter((r) => r.count > 0);
  const interactive = !!onReactionToggle;
  const canAdd = interactive && (addable ?? true) && (max === undefined || shown.length < max);
  const count = shown.length + (canAdd ? 1 : 0);
  const [focusIndex, setFocusIndex] = useState(0);
  const current = Math.min(focusIndex, Math.max(0, count - 1));
  const items = useRef<(HTMLButtonElement | null)[]>([]);

  // Taking back the last reaction of an emoji removes its chip, and with it
  // keyboard focus. Put focus on the chip that took its place (or the add
  // button), so the next Tab does not start from the top of the page.
  const refocus = useRef(false);
  const chipCount = shown.length;
  useEffect(() => {
    if (!refocus.current) return;
    refocus.current = false;
    const active = typeof document === 'undefined' ? null : document.activeElement;
    if (active && active !== document.body) return;
    items.current[Math.min(focusIndex, Math.max(0, count - 1))]?.focus();
    // Only when a chip appears or disappears.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chipCount]);

  if (!interactive) {
    if (shown.length === 0) return null;
    return (
      <div
        ref={ref}
        role="group"
        aria-label={m.label}
        {...rest}
        className={cx('mn-reaction-bar', className)}
        data-size={size}
        data-readonly=""
      >
        {shown.map((r) => (
          <span
            key={r.emoji}
            role="img"
            className="mn-reaction-bar__chip"
            aria-label={m.reaction(r.label ?? r.emoji, r.count)}
            title={r.title}
            data-reacted={r.reacted ? '' : undefined}
          >
            <span className="mn-reaction-bar__emoji" aria-hidden="true">
              {r.emoji}
            </span>
            <span className="mn-reaction-bar__count" aria-hidden="true">
              {r.count}
            </span>
          </span>
        ))}
      </div>
    );
  }

  const focusAt = (i: number) => {
    if (count === 0) return;
    const next = (i + count) % count;
    setFocusIndex(next);
    items.current[next]?.focus();
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    onKeyDown?.(event);
    if (event.defaultPrevented) return;
    // Only the toolbar's own buttons move focus; the picker lives in a portal.
    if (!(event.target instanceof HTMLButtonElement) || !items.current.includes(event.target)) {
      return;
    }
    const rtl = readDir(event.currentTarget) === 'rtl';
    const forward = rtl ? 'ArrowLeft' : 'ArrowRight';
    const back = rtl ? 'ArrowRight' : 'ArrowLeft';
    if (event.key === forward) focusAt(current + 1);
    else if (event.key === back) focusAt(current - 1);
    else if (event.key === 'Home') focusAt(0);
    else if (event.key === 'End') focusAt(count - 1);
    else return;
    event.preventDefault();
  };

  const pick = (emoji: string) => {
    const existing = shown.find((r) => r.emoji === emoji);
    if (!existing?.reacted) onReactionToggle(emoji, true);
  };

  return (
    <div
      ref={ref}
      role="toolbar"
      aria-label={m.label}
      {...rest}
      className={cx('mn-reaction-bar', className)}
      data-size={size}
      onKeyDown={handleKeyDown}
    >
      {shown.map((r, i) => (
        <button
          key={r.emoji}
          ref={(el) => {
            items.current[i] = el;
          }}
          type="button"
          className="mn-reaction-bar__chip"
          aria-pressed={r.reacted ?? false}
          aria-label={m.reaction(r.label ?? r.emoji, r.count)}
          title={r.title}
          data-reacted={r.reacted ? '' : undefined}
          tabIndex={i === current ? 0 : -1}
          onFocus={() => {
            refocus.current = false;
            setFocusIndex(i);
          }}
          onClick={() => {
            refocus.current = Boolean(r.reacted) && r.count <= 1;
            onReactionToggle(r.emoji, !r.reacted);
          }}
        >
          <span className="mn-reaction-bar__emoji" aria-hidden="true">
            {r.emoji}
          </span>
          <span className="mn-reaction-bar__count" aria-hidden="true">
            {r.count}
          </span>
        </button>
      ))}
      {canAdd ? (
        <EmojiPickerPopover
          onSelect={(emoji) => pick(emoji)}
          side="top"
          align="start"
          {...(quickReactions ? { recents: quickReactions } : {})}
          {...(categories ? { categories } : {})}
          {...(pickerMessages ? { messages: pickerMessages } : {})}
        >
          <IconButton
            ref={(el: HTMLButtonElement | null) => {
              items.current[shown.length] = el;
            }}
            size="xs"
            variant="ghost"
            className="mn-reaction-bar__add"
            label={m.add}
            icon={<AddReactionGlyph />}
            tabIndex={current === shown.length ? 0 : -1}
            onFocus={() => setFocusIndex(shown.length)}
          />
        </EmojiPickerPopover>
      ) : null}
    </div>
  );
});
