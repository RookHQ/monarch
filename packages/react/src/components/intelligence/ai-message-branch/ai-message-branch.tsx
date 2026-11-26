'use client';

import { forwardRef, useEffect, useRef, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../actions/button/index.js';

export interface AiMessageBranchMessages {
  /** Accessible name of the switcher group. */
  label: string;
  /** Accessible name of the previous button. */
  previous: string;
  /** Accessible name of the next button. */
  next: string;
  /** Visible position, 1-based, e.g. "2 / 3". */
  position: (index: number, count: number) => string;
  /** Announced politely after switching, 1-based, e.g. "Version 2 of 3". */
  announcement: (index: number, count: number) => string;
}

export const AI_MESSAGE_BRANCH_MESSAGES: AiMessageBranchMessages = {
  label: 'Versions',
  previous: 'Previous version',
  next: 'Next version',
  position: (index, count) => `${index} / ${count}`,
  announcement: (index, count) => `Version ${index} of ${count}`,
};

export interface AiMessageBranchProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange'> {
  /** The version shown, 0-based. */
  index: number;
  /** How many versions exist. With fewer than two nothing is rendered (see `showSingle`). */
  count: number;
  /** Switches to another version; receives the 0-based index. */
  onChange: (index: number) => void;
  /** Render the switcher even when there is only one version. Default false. */
  showSingle?: boolean | undefined;
  /** Both buttons disabled, e.g. while a reply streams. */
  disabled?: boolean | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiMessageBranchMessages> | undefined;
}

/**
 * The "‹ 2 / 3 ›" switcher between alternative versions of a message, made
 * when a reply is regenerated or a prompt is edited and resent. Each button is
 * labelled, the new position is announced politely, and focus stays on the
 * switcher when a button becomes disabled at either end.
 */
export const AiMessageBranch = forwardRef<HTMLDivElement, AiMessageBranchProps>(
  function AiMessageBranch(
    {
      index,
      count,
      onChange,
      showSingle = false,
      disabled = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_MESSAGE_BRANCH_MESSAGES, ...messagesProp };
    const total = Math.max(1, Math.floor(count));
    const current = Math.min(Math.max(0, Math.floor(index)), total - 1);
    const prevRef = useRef<HTMLButtonElement>(null);
    const nextRef = useRef<HTMLButtonElement>(null);
    const [announcement, setAnnouncement] = useState('');
    // Set by a click; the announcement and focus rescue follow the index prop.
    const pending = useRef<{ target: number; from: 'prev' | 'next' } | null>(null);

    useEffect(() => {
      const p = pending.current;
      if (!p || p.target !== current) return;
      pending.current = null;
      setAnnouncement(m.announcement(current + 1, total));
      // A button that just became disabled loses focus: move it to the other one.
      const atEdge = p.from === 'prev' ? current <= 0 : current >= total - 1;
      if (atEdge) (p.from === 'prev' ? nextRef : prevRef).current?.focus();
      // eslint-disable-next-line react-hooks/exhaustive-deps -- announce only on index changes
    }, [current, total]);

    if (total < 2 && !showSingle) return null;

    const go = (from: 'prev' | 'next') => {
      const target = from === 'prev' ? current - 1 : current + 1;
      if (target < 0 || target >= total) return;
      pending.current = { target, from };
      onChange(target);
    };

    return (
      <div
        ref={ref}
        role="group"
        aria-label={m.label}
        {...rest}
        className={cx('mn-ai-message-branch', className)}
      >
        <IconButton
          ref={prevRef}
          size="sm"
          label={m.previous}
          icon={<ChevronRightGlyph className="mn-ai-message-branch__prev-icon" />}
          className="mn-ai-message-branch__button"
          disabled={disabled || current <= 0}
          onClick={() => go('prev')}
        />
        <span className="mn-ai-message-branch__position">{m.position(current + 1, total)}</span>
        <IconButton
          ref={nextRef}
          size="sm"
          label={m.next}
          icon={<ChevronRightGlyph className="mn-ai-message-branch__next-icon" />}
          className="mn-ai-message-branch__button"
          disabled={disabled || current >= total - 1}
          onClick={() => go('next')}
        />
        <span role="status" className="mn-visually-hidden">
          {announcement}
        </span>
      </div>
    );
  },
);
