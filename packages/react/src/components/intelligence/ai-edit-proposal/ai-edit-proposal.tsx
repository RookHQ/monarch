'use client';

import {
  forwardRef,
  useId,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { setRefs } from '../../../internal/dismissible.js';
import { CheckGlyph, CloseGlyph, RetryGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { Badge, type Tone } from '../../data-display/badge/index.js';
import type { AiEditProposalPart } from '../ai-conversation/index.js';
import { AiGeneratedBadge } from '../ai-generated-badge/index.js';
import { diffWords, type AiDiffSegment } from './ai-edit-proposal.diff.js';

export * from './ai-edit-proposal.diff.js';

/** A person's decision on one hunk. */
export type AiEditHunkStatus = 'proposed' | 'accepted' | 'rejected';

/** One self-contained change: a field, a paragraph or a sentence. */
export interface AiEditHunk {
  id: string;
  /** The current text. Empty for an addition. */
  before: string;
  /** The text the AI proposes. Empty for a removal. */
  after: string;
  /** What the hunk changes, e.g. a field name or "Paragraph 2". */
  label?: string | undefined;
  /**
   * The decision, when the app keeps it. Leave it out and the component
   * remembers decisions itself.
   */
  status?: AiEditHunkStatus | undefined;
}

export interface AiEditProposalMessages {
  /** Title of the card. */
  title: string;
  /** Provenance badge label. */
  badge: string;
  /** Proposal state labels. */
  states: Record<AiEditProposalPart['state'], string>;
  /** Hunk decision labels. */
  hunkStates: Record<Exclude<AiEditHunkStatus, 'proposed'>, string>;
  /** "3 changes", "1 of 3 changes left". */
  count: (total: number, pending: number) => string;
  /** Name of a hunk without a label. */
  hunkName: (index: number, total: number) => string;
  acceptAll: string;
  rejectAll: string;
  accept: string;
  reject: string;
  revert: string;
  /** Read before removed and added text. */
  deleted: string;
  inserted: string;
  /** Shown when a hunk changes nothing. */
  unchanged: string;
  /** Announced politely after a decision. */
  announce: (
    event: 'accepted' | 'rejected' | 'accepted-all' | 'rejected-all',
    left: number,
  ) => string;
  /** Read-only note after the proposal leaves the proposed state. */
  readOnly: Record<Exclude<AiEditProposalPart['state'], 'proposed'>, string>;
}

const DEFAULT_MESSAGES: AiEditProposalMessages = {
  title: 'Suggested edits',
  badge: 'AI generated',
  states: { proposed: 'Proposed', applied: 'Applied', rejected: 'Rejected', reverted: 'Reverted' },
  hunkStates: { accepted: 'Accepted', rejected: 'Rejected' },
  count: (total, pending) =>
    pending === total
      ? `${total} ${total === 1 ? 'change' : 'changes'}`
      : `${pending} of ${total} ${total === 1 ? 'change' : 'changes'} left`,
  hunkName: (index, total) => `Change ${index} of ${total}`,
  acceptAll: 'Accept all',
  rejectAll: 'Reject all',
  accept: 'Accept',
  reject: 'Reject',
  revert: 'Revert',
  deleted: 'Deleted: ',
  inserted: 'Inserted: ',
  unchanged: 'No change',
  announce: (event, left) => {
    if (event === 'accepted-all') return 'All changes accepted.';
    if (event === 'rejected-all') return 'All changes rejected.';
    const done = event === 'accepted' ? 'Change accepted.' : 'Change rejected.';
    return left > 0 ? `${done} ${left} left.` : `${done} No changes left.`;
  },
  readOnly: {
    applied: 'These edits were applied.',
    rejected: 'These edits were rejected.',
    reverted: 'These edits were reverted.',
  },
};

const STATE_TONE: Record<AiEditProposalPart['state'], Tone> = {
  proposed: 'info',
  applied: 'success',
  rejected: 'neutral',
  reverted: 'warning',
};

export interface AiEditProposalProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'title' | 'part'
> {
  /** The edit-proposal part from the message; its `state` decides whether it can still be changed. */
  part: AiEditProposalPart;
  /** The changes, in document order. */
  hunks: AiEditHunk[];
  /** Replaces the default title "Suggested edits". */
  title?: ReactNode | undefined;
  /** Accept every hunk still undecided. Shows Accept all while the proposal is `proposed`. */
  onAcceptAll?: (() => void) | undefined;
  /** Reject every hunk still undecided. Shows Reject all while the proposal is `proposed`. */
  onRejectAll?: (() => void) | undefined;
  /** Accept one hunk. Shows Accept on each hunk while the proposal is `proposed`. */
  onAcceptHunk?: ((id: string) => void) | undefined;
  /** Reject one hunk. Shows Reject on each hunk while the proposal is `proposed`. */
  onRejectHunk?: ((id: string) => void) | undefined;
  /** Undo applied edits. Shows Revert once the proposal is `applied`. */
  onRevert?: (() => void) | undefined;
  /** Strings, for localisation. Nested records merge per key. */
  messages?: Partial<Omit<AiEditProposalMessages, 'states' | 'hunkStates' | 'readOnly'>> & {
    states?: Partial<AiEditProposalMessages['states']>;
    hunkStates?: Partial<AiEditProposalMessages['hunkStates']>;
    readOnly?: Partial<AiEditProposalMessages['readOnly']>;
  };
}

/** Removed and added text of one diff, as `<del>` and `<ins>` with spoken prefixes. */
function AiDiffText({
  segments,
  deleted = DEFAULT_MESSAGES.deleted,
  inserted = DEFAULT_MESSAGES.inserted,
}: {
  segments: AiDiffSegment[];
  /** Visually hidden text read before removed text. */
  deleted?: string;
  /** Visually hidden text read before added text. */
  inserted?: string;
}) {
  return (
    <>
      {segments.map((seg, i) =>
        seg.type === 'equal' ? (
          <span key={i}>{seg.text}</span>
        ) : seg.type === 'delete' ? (
          <del key={i} className="mn-ai-edit-proposal__delete">
            <span className="mn-visually-hidden">{deleted}</span>
            {seg.text}
          </del>
        ) : (
          <ins key={i} className="mn-ai-edit-proposal__insert">
            <span className="mn-visually-hidden">{inserted}</span>
            {seg.text}
          </ins>
        ),
      )}
    </>
  );
}

/**
 * A reviewable diff of edits the AI proposes to a document or record. Each
 * hunk shows a word-level diff and can be accepted or rejected on its own, or
 * all at once; once applied the edits can be reverted. Nothing changes until a
 * person decides, so AI edits are never applied silently.
 */
export const AiEditProposal = forwardRef<HTMLDivElement, AiEditProposalProps>(
  function AiEditProposal(
    {
      part,
      hunks,
      title,
      onAcceptAll,
      onRejectAll,
      onAcceptHunk,
      onRejectHunk,
      onRevert,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      states: { ...DEFAULT_MESSAGES.states, ...messagesProp?.states },
      hunkStates: { ...DEFAULT_MESSAGES.hunkStates, ...messagesProp?.hunkStates },
      readOnly: { ...DEFAULT_MESSAGES.readOnly, ...messagesProp?.readOnly },
    };
    const editable = part.state === 'proposed';
    const [decided, setDecided] = useState<Record<string, AiEditHunkStatus>>({});
    const [announcement, setAnnouncement] = useState('');
    const titleId = useId();
    const rootRef = useRef<HTMLDivElement | null>(null);
    const titleRef = useRef<HTMLParagraphElement | null>(null);
    /** Index of the hunk just decided, so focus can move on after re-render. */
    const focusFrom = useRef<number | null>(null);

    const statusOf = (hunk: AiEditHunk): AiEditHunkStatus => {
      const own = hunk.status ?? decided[hunk.id] ?? 'proposed';
      if (part.state === 'applied') return own === 'rejected' ? 'rejected' : 'accepted';
      if (part.state === 'rejected') return 'rejected';
      return own;
    };
    const statuses = hunks.map(statusOf);
    const pending = statuses.filter((s) => s === 'proposed').length;

    const diffs = useMemo(
      () => hunks.map((h) => diffWords(h.before, h.after)),
      // Re-diff only when the text changes, not on every decision.
      // eslint-disable-next-line react-hooks/exhaustive-deps
      [hunks.map((h) => `${h.before}\u0000${h.after}`).join('\u0001')],
    );

    // After a decision removes the buttons that had focus, move on to the
    // next undecided hunk, or to the title when none are left.
    useLayoutEffect(() => {
      const from = focusFrom.current;
      if (from === null) return;
      focusFrom.current = null;
      const root = rootRef.current;
      if (!root) return;
      const buttons = Array.from(
        root.querySelectorAll<HTMLButtonElement>('[data-hunk-action="accept"]'),
      );
      const next =
        buttons.find((b) => Number(b.dataset.index) > from) ??
        buttons.find((b) => Number(b.dataset.index) < from);
      (next ?? titleRef.current)?.focus();
    });

    const decide = (index: number, status: 'accepted' | 'rejected') => {
      const hunk = hunks[index];
      if (!hunk) return;
      focusFrom.current = index;
      setDecided((d) => ({ ...d, [hunk.id]: status }));
      setAnnouncement(messages.announce(status, Math.max(0, pending - 1)));
      if (status === 'accepted') onAcceptHunk?.(hunk.id);
      else onRejectHunk?.(hunk.id);
    };
    const decideAll = (status: 'accepted' | 'rejected') => {
      focusFrom.current = hunks.length;
      setDecided((d) => {
        const next = { ...d };
        hunks.forEach((h, i) => {
          if (statuses[i] === 'proposed') next[h.id] = status;
        });
        return next;
      });
      setAnnouncement(
        messages.announce(status === 'accepted' ? 'accepted-all' : 'rejected-all', 0),
      );
      if (status === 'accepted') onAcceptAll?.();
      else onRejectAll?.();
    };

    const showAll = editable && pending > 0 && (onAcceptAll || onRejectAll);

    return (
      <div
        ref={(node) => {
          rootRef.current = node;
          setRefs(node, ref);
        }}
        role="group"
        aria-labelledby={titleId}
        {...rest}
        className={cx('mn-ai-edit-proposal', className)}
        data-state={part.state}
      >
        <div className="mn-ai-edit-proposal__header">
          <div className="mn-ai-edit-proposal__heading">
            <p id={titleId} ref={titleRef} tabIndex={-1} className="mn-ai-edit-proposal__title">
              {title ?? messages.title}
            </p>
            <AiGeneratedBadge size="sm" label={messages.badge} />
            <Badge size="sm" tone={STATE_TONE[part.state]} className="mn-ai-edit-proposal__state">
              {messages.states[part.state]}
            </Badge>
          </div>
          <p className="mn-ai-edit-proposal__count">
            {editable
              ? messages.count(hunks.length, pending)
              : messages.readOnly[part.state as Exclude<typeof part.state, 'proposed'>]}
          </p>
        </div>

        <ol className="mn-ai-edit-proposal__hunks">
          {hunks.map((hunk, i) => {
            const status = statuses[i] ?? 'proposed';
            const labelId = `${titleId}-hunk-${i}`;
            const segments = diffs[i] ?? [];
            const changed = segments.some((s) => s.type !== 'equal');
            return (
              <li
                key={hunk.id}
                className="mn-ai-edit-proposal__hunk"
                data-status={status}
                aria-labelledby={labelId}
              >
                <div className="mn-ai-edit-proposal__hunk-header">
                  <span id={labelId} className="mn-ai-edit-proposal__hunk-label">
                    {hunk.label ?? messages.hunkName(i + 1, hunks.length)}
                  </span>
                  {status !== 'proposed' ? (
                    <span className="mn-ai-edit-proposal__hunk-status">
                      {status === 'accepted' ? (
                        <CheckGlyph aria-hidden />
                      ) : (
                        <CloseGlyph aria-hidden />
                      )}
                      {messages.hunkStates[status]}
                    </span>
                  ) : null}
                </div>
                <p className="mn-ai-edit-proposal__diff">
                  {changed ? (
                    <AiDiffText
                      segments={segments}
                      deleted={messages.deleted}
                      inserted={messages.inserted}
                    />
                  ) : (
                    <>
                      {hunk.after}
                      <span className="mn-ai-edit-proposal__unchanged">
                        {' '}
                        ({messages.unchanged})
                      </span>
                    </>
                  )}
                </p>
                {editable && status === 'proposed' && (onAcceptHunk || onRejectHunk) ? (
                  <div className="mn-ai-edit-proposal__hunk-actions">
                    {onAcceptHunk ? (
                      <Button
                        size="sm"
                        variant="secondary"
                        leadingIcon={<CheckGlyph />}
                        data-hunk-action="accept"
                        data-index={i}
                        aria-describedby={labelId}
                        onClick={() => decide(i, 'accepted')}
                      >
                        {messages.accept}
                      </Button>
                    ) : null}
                    {onRejectHunk ? (
                      <Button
                        size="sm"
                        variant="ghost"
                        leadingIcon={<CloseGlyph />}
                        data-hunk-action="reject"
                        data-index={i}
                        aria-describedby={labelId}
                        onClick={() => decide(i, 'rejected')}
                      >
                        {messages.reject}
                      </Button>
                    ) : null}
                  </div>
                ) : null}
              </li>
            );
          })}
        </ol>

        {showAll || (part.state === 'applied' && onRevert) ? (
          <div className="mn-ai-edit-proposal__actions">
            {showAll && onAcceptAll ? (
              <Button
                size="sm"
                variant="primary"
                leadingIcon={<CheckGlyph />}
                onClick={() => decideAll('accepted')}
              >
                {messages.acceptAll}
              </Button>
            ) : null}
            {showAll && onRejectAll ? (
              <Button size="sm" variant="secondary" onClick={() => decideAll('rejected')}>
                {messages.rejectAll}
              </Button>
            ) : null}
            {part.state === 'applied' && onRevert ? (
              <Button size="sm" variant="secondary" leadingIcon={<RetryGlyph />} onClick={onRevert}>
                {messages.revert}
              </Button>
            ) : null}
          </div>
        ) : null}

        <span role="status" className="mn-visually-hidden">
          {announcement}
        </span>
      </div>
    );
  },
);
