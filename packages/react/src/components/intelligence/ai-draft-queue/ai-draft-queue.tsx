'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  CheckGlyph,
  DashGlyph,
  PencilGlyph,
  RetryGlyph,
  SuccessGlyph,
} from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { Textarea } from '../../forms/text-field/index.js';
import { Kbd } from '../../typography/kbd/index.js';
import { AiThinkingIndicator } from '../ai-thinking-indicator/index.js';

/** Where a draft is in review. */
export type AiDraftStatus = 'draft' | 'regenerating' | 'approved' | 'sent' | 'skipped';

/** One AI-written draft waiting for review. */
export interface AiDraft {
  /** Stable id passed back to every callback. */
  id: string;
  /** What the draft is, e.g. the recipient or the subject. */
  title: string;
  /** The drafted text. Shown as plain text (never as HTML). */
  body: string;
  /** What it answers or is based on, e.g. the original message. */
  context?: ReactNode | undefined;
  status: AiDraftStatus;
}

/** Every string AiDraftQueue shows or announces. English defaults. */
export interface AiDraftQueueMessages {
  /** Accessible name of the queue when there is no `title`. */
  label: string;
  /** Progress line, e.g. "2 of 5 reviewed". */
  progress: (reviewed: number, total: number) => string;
  /** Shown when there are no drafts. */
  empty: string;
  /** Status labels, shown with an icon. */
  status: Record<AiDraftStatus, string>;
  /** Shown instead of the body while a draft is regenerating. */
  writing: string;
  /** Visually hidden before each body, so it is identified as AI-written. */
  aiDraft: string;
  /** Heading of the context block. */
  context: string;
  approve: string;
  send: string;
  edit: string;
  regenerate: string;
  skip: string;
  save: string;
  cancel: string;
  /** Accessible name of the edit box. */
  editLabel: (title: string) => string;
  editHint: string;
  /** Visible keyboard hint when `keyboardShortcuts` is on. */
  shortcutsHint: string;
  /** Announced when a draft's status changes. */
  announce: (status: AiDraftStatus, title: string) => string;
}

const STATUS: Record<AiDraftStatus, string> = {
  draft: 'Needs review',
  regenerating: 'Writing',
  approved: 'Approved',
  sent: 'Sent',
  skipped: 'Skipped',
};

const DEFAULT_MESSAGES: AiDraftQueueMessages = {
  label: 'Drafts to review',
  progress: (reviewed, total) => `${reviewed} of ${total} reviewed`,
  empty: 'No drafts to review.',
  status: STATUS,
  writing: 'AI is still writing…',
  aiDraft: 'AI draft:',
  context: 'Context',
  approve: 'Approve',
  send: 'Send',
  edit: 'Edit',
  regenerate: 'Regenerate',
  skip: 'Skip',
  save: 'Save',
  cancel: 'Cancel',
  editLabel: (title) => `Edit draft: ${title}`,
  editHint: 'Ctrl+Enter or ⌘+Enter to save, Escape to cancel.',
  shortcutsHint: 'to move between drafts',
  announce: (status, title) =>
    status === 'draft' ? `New draft ready: ${title}` : `${STATUS[status]}: ${title}`,
};

export interface AiDraftQueueProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The drafts, in review order. */
  drafts: readonly AiDraft[];
  /** Called with the edited text when the person saves an edit. */
  onEdit: (id: string, body: string) => void;
  /** Asks for a new version; set the draft's status to `regenerating` while it is written. */
  onRegenerate: (id: string) => void;
  onApprove: (id: string) => void;
  /** Sends an approved draft. Without it, approved drafts show no Send button. */
  onSend?: ((id: string) => void) | undefined;
  onSkip: (id: string) => void;
  /** Heading above the queue. Also names it. */
  title?: ReactNode | undefined;
  /** Heading level of each draft's title (the queue title is one level up). Default 3. */
  headingLevel?: 3 | 4 | 5 | 6 | undefined;
  /**
   * Opt in to J / K to move to the next / previous draft while focus is in the
   * queue (never while typing). Default false.
   */
  keyboardShortcuts?: boolean | undefined;
  /** After approve, send or skip, move focus to the next draft that needs review. Default true. */
  advanceOnAction?: boolean | undefined;
  /** Replaces the empty message. */
  empty?: ReactNode | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiDraftQueueMessages> | undefined;
}

const REVIEWED: ReadonlySet<AiDraftStatus> = new Set(['approved', 'sent', 'skipped']);

function StatusGlyph({ status }: { status: AiDraftStatus }) {
  switch (status) {
    case 'draft':
      return <PencilGlyph />;
    case 'regenerating':
      return <RetryGlyph />;
    case 'approved':
      return <CheckGlyph />;
    case 'sent':
      return <SuccessGlyph />;
    case 'skipped':
      return <DashGlyph />;
  }
}

function isEditable(target: EventTarget | null): boolean {
  if (typeof Element === 'undefined' || !(target instanceof Element)) return false;
  if (target instanceof HTMLElement && target.isContentEditable) return true;
  return target.closest('input, textarea, select, [contenteditable]') !== null;
}

/**
 * A review queue for AI-written drafts (replies, messages, notes): each draft
 * with its context, an "AI is still writing…" state while it regenerates, and
 * Approve, Edit, Regenerate, Skip and Send. Focus moves to the next draft that
 * needs review after each decision, and J / K move between drafts when
 * `keyboardShortcuts` is on. Nothing is sent without a person approving it.
 */
export const AiDraftQueue = forwardRef<HTMLElement, AiDraftQueueProps>(function AiDraftQueue(
  {
    drafts,
    onEdit,
    onRegenerate,
    onApprove,
    onSend,
    onSkip,
    title,
    headingLevel = 3,
    keyboardShortcuts = false,
    advanceOnAction = true,
    empty,
    messages: messagesProp,
    className,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const m = {
    ...DEFAULT_MESSAGES,
    ...messagesProp,
    status: { ...STATUS, ...messagesProp?.status },
  };
  const base = useId().replace(/:/g, '');
  const titleId = `mn-ai-draft-queue-${base}-title`;
  const cardId = (id: string) => `mn-ai-draft-queue-${base}-${id}`;
  const Heading = `h${headingLevel}` as 'h3';
  const QueueHeading = `h${headingLevel - 1}` as 'h2';

  const [editing, setEditing] = useState<{ id: string; text: string } | null>(null);
  const [activeId, setActiveId] = useState<string | null>(null);
  const focusNext = useRef<string | null>(null);

  // ------------------------------------------------------------ announcer
  const [announcement, setAnnouncement] = useState('');
  const prevStatus = useRef<Map<string, AiDraftStatus> | null>(null);
  useEffect(() => {
    const prev = prevStatus.current;
    const next = new Map(drafts.map((d) => [d.id, d.status]));
    prevStatus.current = next;
    if (!prev) return;
    const said: string[] = [];
    for (const d of drafts) {
      const was = prev.get(d.id);
      // "Regenerating" is announced by its own indicator.
      if (was && was !== d.status && d.status !== 'regenerating') {
        said.push(m.announce(d.status, d.title));
      }
    }
    if (said.length) setAnnouncement(said.join('. '));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [drafts]);

  // ------------------------------------------------------------ focus
  useEffect(() => {
    const id = focusNext.current;
    if (!id) return;
    focusNext.current = null;
    if (typeof document === 'undefined') return;
    document.getElementById(cardId(id))?.focus();
  });

  const focusCard = (id: string) => {
    setActiveId(id);
    document.getElementById(cardId(id))?.focus();
  };

  const afterDecision = (id: string) => {
    if (!advanceOnAction) {
      focusNext.current = id;
      return;
    }
    const i = drafts.findIndex((d) => d.id === id);
    const order = [...drafts.slice(i + 1), ...drafts.slice(0, Math.max(0, i))];
    const next = order.find((d) => d.status === 'draft');
    focusNext.current = next ? next.id : id;
    if (next) setActiveId(next.id);
  };

  const handleKeyDown = (e: ReactKeyboardEvent<HTMLElement>) => {
    onKeyDown?.(e);
    if (e.defaultPrevented || !keyboardShortcuts) return;
    if (e.metaKey || e.ctrlKey || e.altKey || isEditable(e.target)) return;
    const key = e.key.toLowerCase();
    if (key !== 'j' && key !== 'k') return;
    if (drafts.length === 0) return;
    e.preventDefault();
    const current = drafts.findIndex((d) => d.id === activeId);
    const step = key === 'j' ? 1 : -1;
    const index = current === -1 ? (step > 0 ? 0 : drafts.length - 1) : current + step;
    const target = drafts[Math.max(0, Math.min(drafts.length - 1, index))];
    if (target) focusCard(target.id);
  };

  const reviewed = drafts.filter((d) => REVIEWED.has(d.status)).length;

  const saveEdit = () => {
    if (!editing) return;
    const { id, text } = editing;
    setEditing(null);
    focusNext.current = id;
    onEdit(id, text);
  };
  const cancelEdit = () => {
    if (!editing) return;
    focusNext.current = editing.id;
    setEditing(null);
  };

  const renderCard = (draft: AiDraft) => {
    const id = cardId(draft.id);
    const isEditing = editing?.id === draft.id;
    const writing = draft.status === 'regenerating';
    const actions: ReactNode[] = [];
    if (!isEditing && draft.status === 'draft') {
      actions.push(
        <Button
          key="approve"
          size="sm"
          variant="primary"
          leadingIcon={<CheckGlyph />}
          onClick={() => {
            afterDecision(draft.id);
            onApprove(draft.id);
          }}
        >
          {m.approve}
        </Button>,
      );
    }
    if (!isEditing && draft.status === 'approved' && onSend) {
      actions.push(
        <Button
          key="send"
          size="sm"
          variant="primary"
          onClick={() => {
            afterDecision(draft.id);
            onSend(draft.id);
          }}
        >
          {m.send}
        </Button>,
      );
    }
    if (!isEditing && (draft.status === 'draft' || draft.status === 'approved')) {
      actions.push(
        <Button
          key="edit"
          size="sm"
          leadingIcon={<PencilGlyph />}
          onClick={() => setEditing({ id: draft.id, text: draft.body })}
        >
          {m.edit}
        </Button>,
      );
    }
    if (!isEditing && draft.status === 'draft') {
      actions.push(
        <Button
          key="regenerate"
          size="sm"
          variant="ghost"
          leadingIcon={<RetryGlyph />}
          onClick={() => {
            focusNext.current = draft.id;
            onRegenerate(draft.id);
          }}
        >
          {m.regenerate}
        </Button>,
        <Button
          key="skip"
          size="sm"
          variant="ghost"
          onClick={() => {
            afterDecision(draft.id);
            onSkip(draft.id);
          }}
        >
          {m.skip}
        </Button>,
      );
    }

    return (
      <li key={draft.id} className="mn-ai-draft-queue__item">
        {/* onFocus only tracks the current draft for J / K. */}
        {/* eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions */}
        <article
          id={id}
          className="mn-ai-draft-queue__draft"
          aria-labelledby={`${id}-title`}
          aria-busy={writing || undefined}
          // Focus target for J / K and for moving on after a decision.
          tabIndex={-1}
          data-status={draft.status}
          data-active={activeId === draft.id || undefined}
          onFocus={() => setActiveId(draft.id)}
        >
          <header className="mn-ai-draft-queue__head">
            <Heading id={`${id}-title`} className="mn-ai-draft-queue__title">
              {draft.title}
            </Heading>
            <span className="mn-ai-draft-queue__status" data-status={draft.status}>
              <span className="mn-ai-draft-queue__status-icon" aria-hidden>
                <StatusGlyph status={draft.status} />
              </span>
              {m.status[draft.status]}
            </span>
          </header>
          {draft.context ? (
            <div className="mn-ai-draft-queue__context">
              <span className="mn-ai-draft-queue__context-label">{m.context}</span>
              <div className="mn-ai-draft-queue__context-body">{draft.context}</div>
            </div>
          ) : null}
          {isEditing ? (
            <div className="mn-ai-draft-queue__editor">
              <Textarea
                aria-label={m.editLabel(draft.title)}
                aria-describedby={`${id}-hint`}
                rows={5}
                value={editing.text}
                // The edit box opens on request, so it takes focus.
                // eslint-disable-next-line jsx-a11y/no-autofocus
                autoFocus
                onChange={(e) => setEditing({ id: draft.id, text: e.target.value })}
                onKeyDown={(e) => {
                  if (e.key === 'Escape') {
                    e.preventDefault();
                    e.stopPropagation();
                    cancelEdit();
                  } else if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
                    e.preventDefault();
                    saveEdit();
                  }
                }}
              />
              <p id={`${id}-hint`} className="mn-ai-draft-queue__hint">
                {m.editHint}
              </p>
              <div className="mn-ai-draft-queue__actions">
                <Button size="sm" variant="primary" onClick={saveEdit}>
                  {m.save}
                </Button>
                <Button size="sm" onClick={cancelEdit}>
                  {m.cancel}
                </Button>
              </div>
            </div>
          ) : writing ? (
            <AiThinkingIndicator className="mn-ai-draft-queue__writing" label={m.writing} />
          ) : (
            <p className="mn-ai-draft-queue__body">
              <span className="mn-visually-hidden">{m.aiDraft} </span>
              {draft.body}
            </p>
          )}
          {actions.length ? <div className="mn-ai-draft-queue__actions">{actions}</div> : null}
        </article>
      </li>
    );
  };

  return (
    // J / K are handled for the whole queue when `keyboardShortcuts` is on.
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <section
      ref={ref}
      aria-labelledby={title ? titleId : undefined}
      aria-label={title ? undefined : m.label}
      {...rest}
      className={cx('mn-ai-draft-queue', className)}
      data-shortcuts={keyboardShortcuts || undefined}
      onKeyDown={handleKeyDown}
    >
      <div className="mn-ai-draft-queue__top">
        {title ? (
          <QueueHeading id={titleId} className="mn-ai-draft-queue__queue-title">
            {title}
          </QueueHeading>
        ) : null}
        {drafts.length ? (
          <p className="mn-ai-draft-queue__progress">{m.progress(reviewed, drafts.length)}</p>
        ) : null}
        {keyboardShortcuts && drafts.length ? (
          <p className="mn-ai-draft-queue__keys">
            <Kbd>J</Kbd> <Kbd>K</Kbd> {m.shortcutsHint}
          </p>
        ) : null}
      </div>
      {drafts.length ? (
        <ol
          className="mn-ai-draft-queue__list"
          aria-keyshortcuts={keyboardShortcuts ? 'J K' : undefined}
        >
          {drafts.map(renderCard)}
        </ol>
      ) : (
        <div className="mn-ai-draft-queue__empty">{empty ?? m.empty}</div>
      )}
      <div role="status" className="mn-visually-hidden">
        {announcement}
      </div>
    </section>
  );
});
