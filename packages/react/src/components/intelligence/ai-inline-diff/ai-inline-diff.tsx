'use client';

import {
  forwardRef,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, ChevronDownGlyph, CloseGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../actions/button/index.js';
import type { AiDiffSegment } from '../ai-edit-proposal/index.js';

/** One suggested change: a run of adjacent removed and added segments. */
export interface AiInlineDiffChange {
  /** The segments' `id`, or the index of the change's first segment as a string. */
  id: string;
  /** Indexes into `segments`. */
  segmentIndexes: number[];
  deleted: string;
  inserted: string;
}

/**
 * Groups diff segments into changes. A change is a run of adjacent `delete`
 * and `insert` segments that share an `id` (or all have none); its id is that
 * `id`, or the index of its first segment as a string.
 */
export function aiInlineDiffChanges(segments: AiDiffSegment[]): AiInlineDiffChange[] {
  const changes: AiInlineDiffChange[] = [];
  let current: AiInlineDiffChange | null = null;
  let currentId: string | undefined;
  segments.forEach((seg, i) => {
    if (seg.type === 'equal') {
      current = null;
      return;
    }
    if (!current || seg.id !== currentId) {
      currentId = seg.id;
      current = { id: seg.id ?? String(i), segmentIndexes: [], deleted: '', inserted: '' };
      changes.push(current);
    }
    current.segmentIndexes.push(i);
    if (seg.type === 'delete') current.deleted += seg.text;
    else current.inserted += seg.text;
  });
  return changes;
}

/**
 * Applies a decision to a change and returns new segments: accepting keeps
 * the inserted text and drops the removed text, rejecting does the opposite.
 * Adjacent unchanged text is merged. Use it to update `segments` from
 * `onAccept` and `onReject`.
 */
export function resolveAiInlineDiff(
  segments: AiDiffSegment[],
  id: string,
  decision: 'accept' | 'reject',
): AiDiffSegment[] {
  const change = aiInlineDiffChanges(segments).find((c) => c.id === id);
  if (!change) return segments;
  const indexes = new Set(change.segmentIndexes);
  const keep = decision === 'accept' ? 'insert' : 'delete';
  const out: AiDiffSegment[] = [];
  segments.forEach((seg, i) => {
    let next: AiDiffSegment | null = seg;
    if (indexes.has(i)) next = seg.type === keep ? { type: 'equal', text: seg.text } : null;
    if (!next || !next.text) return;
    const last = out[out.length - 1];
    if (next.type === 'equal' && last?.type === 'equal' && !last.id && !next.id) {
      out[out.length - 1] = { ...last, text: last.text + next.text };
    } else out.push(next);
  });
  return out;
}

export interface AiInlineDiffMessages {
  /** Accessible name of the whole diff; says the suggestions come from AI. */
  label: string;
  /** Read before removed and added text. */
  deleted: string;
  inserted: string;
  /** "Change 2 of 5". */
  position: (index: number, total: number) => string;
  /** Shown when there are no changes. */
  none: string;
  previous: string;
  next: string;
  accept: (index: number, total: number) => string;
  reject: (index: number, total: number) => string;
  /** Announced politely after a decision. */
  announce: (decision: 'accepted' | 'rejected', left: number) => string;
}

const DEFAULT_MESSAGES: AiInlineDiffMessages = {
  label: 'AI suggested changes',
  deleted: 'Deleted: ',
  inserted: 'Inserted: ',
  position: (index, total) => `Change ${index} of ${total}`,
  none: 'No suggested changes',
  previous: 'Previous change',
  next: 'Next change',
  accept: (index, total) => `Accept change ${index} of ${total}`,
  reject: (index, total) => `Reject change ${index} of ${total}`,
  announce: (decision, left) =>
    `Change ${decision}. ${left > 0 ? `${left} left.` : 'No changes left.'}`,
};

export interface AiInlineDiffProps extends HTMLAttributes<HTMLDivElement> {
  /**
   * The text as unchanged, inserted and deleted runs, e.g. from `diffWords`.
   * Controlled: update it after `onAccept` and `onReject` (see
   * `resolveAiInlineDiff`).
   */
  segments: AiDiffSegment[];
  /** Accept a change. Shows Accept on the active change. */
  onAccept?: ((id: string) => void) | undefined;
  /** Reject a change. Shows Reject on the active change. */
  onReject?: ((id: string) => void) | undefined;
  /** Shows the bar with the position (or "No suggested changes") and previous / next. Default `true`. */
  showNavigation?: boolean;
  /** Strings, for localisation. */
  messages?: Partial<AiInlineDiffMessages>;
}

type Item = { kind: 'equal'; text: string; index: number } | { kind: 'change'; change: number };

/**
 * Suggested edits shown inline in the text, as marked insertions and
 * deletions, with Accept and Reject on the active change and previous / next
 * navigation between changes. Editor-agnostic: it renders segments and
 * reports decisions, and the app applies them.
 */
export const AiInlineDiff = forwardRef<HTMLDivElement, AiInlineDiffProps>(function AiInlineDiff(
  {
    segments,
    onAccept,
    onReject,
    showNavigation,
    messages: messagesProp,
    className,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const changes = useMemo(() => aiInlineDiffChanges(segments), [segments]);
  const total = changes.length;
  const [activeRaw, setActive] = useState(0);
  const active = Math.min(activeRaw, Math.max(0, total - 1));
  const [announcement, setAnnouncement] = useState('');
  const changeRefs = useRef<(HTMLSpanElement | null)[]>([]);
  const contentRef = useRef<HTMLDivElement | null>(null);
  /** Set when focus should follow the active change after the next render. */
  const pendingFocus = useRef(false);
  const decidable = Boolean(onAccept || onReject);

  const items = useMemo(() => {
    const list: Item[] = [];
    const first = new Map(changes.map((c, k) => [c.segmentIndexes[0], k]));
    segments.forEach((seg, i) => {
      const change = first.get(i);
      if (seg.type === 'equal') list.push({ kind: 'equal', text: seg.text, index: i });
      else if (change !== undefined) list.push({ kind: 'change', change });
    });
    return list;
  }, [segments, changes]);

  useLayoutEffect(() => {
    if (!pendingFocus.current) return;
    pendingFocus.current = false;
    const target = changeRefs.current[active];
    if (total > 0 && target) {
      target.focus();
      target.scrollIntoView?.({ block: 'nearest', inline: 'nearest' });
    } else contentRef.current?.focus();
  });

  const go = (index: number) => {
    if (total === 0) return;
    const next = (index + total) % total;
    setActive(next);
    pendingFocus.current = true;
  };

  const decide = (decision: 'accepted' | 'rejected') => {
    const change = changes[active];
    if (!change) return;
    pendingFocus.current = true;
    setAnnouncement(messages.announce(decision, total - 1));
    if (decision === 'accepted') onAccept?.(change.id);
    else onReject?.(change.id);
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    onKeyDown?.(event);
    if (event.defaultPrevented || event.altKey || event.ctrlKey || event.metaKey) return;
    const target = event.target as HTMLElement;
    if (!target.closest('.mn-ai-inline-diff__change, .mn-ai-inline-diff__controls')) return;
    const rtl =
      event.currentTarget.closest('[dir]')?.getAttribute('dir') === 'rtl' ||
      getComputedStyle(event.currentTarget).direction === 'rtl';
    const forward = rtl ? 'ArrowLeft' : 'ArrowRight';
    const back = rtl ? 'ArrowRight' : 'ArrowLeft';
    const moves: Record<string, () => number> = {
      ArrowDown: () => active + 1,
      [forward]: () => active + 1,
      ArrowUp: () => active - 1,
      [back]: () => active - 1,
      Home: () => 0,
      End: () => total - 1,
    };
    const move = moves[event.key];
    if (!move) return;
    event.preventDefault();
    go(move());
  };

  const nav = showNavigation ?? true;

  return (
    // Arrow keys come from the focusable changes and buttons inside the group.
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <div
      ref={ref}
      role="group"
      aria-label={messages.label}
      {...rest}
      className={cx('mn-ai-inline-diff', className)}
      data-empty={total === 0 || undefined}
      onKeyDown={handleKeyDown}
    >
      {nav ? (
        <div className="mn-ai-inline-diff__nav">
          <span className="mn-ai-inline-diff__position">
            {total > 0 ? messages.position(active + 1, total) : messages.none}
          </span>
          {total > 0 ? (
            <>
              <IconButton
                size="sm"
                label={messages.previous}
                icon={<ChevronDownGlyph className="mn-ai-inline-diff__prev-icon" />}
                disabled={total < 2}
                onClick={() => go(active - 1)}
              />
              <IconButton
                size="sm"
                label={messages.next}
                icon={<ChevronDownGlyph />}
                disabled={total < 2}
                onClick={() => go(active + 1)}
              />
            </>
          ) : null}
        </div>
      ) : null}
      <div ref={contentRef} tabIndex={-1} className="mn-ai-inline-diff__content">
        {items.map((item) => {
          if (item.kind === 'equal') return <span key={`e${item.index}`}>{item.text}</span>;
          const change = changes[item.change];
          if (!change) return null;
          const isActive = item.change === active;
          return (
            <span key={`c${change.id}`} className="mn-ai-inline-diff__group">
              {/* A roving tab stop: arrow keys move it, and a click makes the change active. */}
              {/* eslint-disable jsx-a11y/no-noninteractive-tabindex, jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions */}
              <span
                ref={(node) => {
                  changeRefs.current[item.change] = node;
                }}
                tabIndex={isActive ? 0 : -1}
                className="mn-ai-inline-diff__change"
                data-active={isActive || undefined}
                data-change-id={change.id}
                onClick={() => setActive(item.change)}
              >
                {change.segmentIndexes.map((i) => {
                  const seg = segments[i];
                  if (!seg) return null;
                  return seg.type === 'delete' ? (
                    <del key={i} className="mn-ai-inline-diff__delete">
                      <span className="mn-visually-hidden">{messages.deleted}</span>
                      {seg.text}
                    </del>
                  ) : (
                    <ins key={i} className="mn-ai-inline-diff__insert">
                      <span className="mn-visually-hidden">{messages.inserted}</span>
                      {seg.text}
                    </ins>
                  );
                })}
              </span>
              {/* eslint-enable jsx-a11y/no-noninteractive-tabindex, jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions */}
              {isActive && decidable ? (
                <span className="mn-ai-inline-diff__controls">
                  {onAccept ? (
                    <IconButton
                      size="xs"
                      variant="ghost"
                      label={messages.accept(item.change + 1, total)}
                      icon={<CheckGlyph />}
                      className="mn-ai-inline-diff__accept"
                      onClick={() => decide('accepted')}
                    />
                  ) : null}
                  {onReject ? (
                    <IconButton
                      size="xs"
                      variant="ghost"
                      label={messages.reject(item.change + 1, total)}
                      icon={<CloseGlyph />}
                      className="mn-ai-inline-diff__reject"
                      onClick={() => decide('rejected')}
                    />
                  ) : null}
                </span>
              ) : null}
            </span>
          );
        })}
      </div>
      <span role="status" className="mn-visually-hidden">
        {announcement}
      </span>
    </div>
  );
});
