'use client';

import { forwardRef, useEffect, useRef, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { Button } from '../../actions/button/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { AiAgentAvatar } from '../ai-agent-avatar/index.js';
import type { AiAgentRef } from '../ai-conversation/index.js';

/** One thing an AI agent did. */
export interface AiActionLogEntry {
  /** Stable id, used as the React key. */
  id: string;
  /** When it happened: a Date, epoch milliseconds or an ISO 8601 string. */
  at: DateInput;
  /** The agent that acted. */
  agent: AiAgentRef;
  /** What it did, as a verb phrase, e.g. "updated the status of". */
  action: string;
  /** What it acted on, e.g. "Quarterly report". */
  target?: string | undefined;
  /** Link to the target. Unsafe URLs are dropped and the target shows as text. */
  href?: string | undefined;
  /** The action can be reversed; shows an Undo button when `onUndo` is given. */
  undoable?: boolean | undefined;
  /** Reverses the action. Return a promise to show a pending state; a rejection is announced. */
  onUndo?: (() => void | Promise<void>) | undefined;
  /** The action has already been undone. */
  undone?: boolean | undefined;
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiActionLogMessages {
  /** Accessible name of the list. */
  label: string;
  /** Tag after the agent's name that marks the actor as an AI agent. */
  actorKind: string;
  /** Visible text of the undo button. */
  undo: string;
  /** Accessible name of the undo button, so each one is distinct. */
  undoLabel: (entry: AiActionLogEntry) => string;
  /** Shown on an entry once undone. */
  undone: string;
  /** Announced after a successful undo. */
  undoneAnnouncement: (entry: AiActionLogEntry) => string;
  /** Announced when an undo fails. */
  undoFailed: (entry: AiActionLogEntry) => string;
  /** Shown when there are no entries. */
  empty: string;
  /** The button that shows more of a long log, with how many it adds. */
  showMore: (count: number) => string;
}

const describe = (e: AiActionLogEntry) =>
  [e.agent.name, e.action, e.target].filter(Boolean).join(' ');

export const AI_ACTION_LOG_MESSAGES: AiActionLogMessages = {
  label: 'AI activity',
  actorKind: 'AI agent',
  undo: 'Undo',
  undoLabel: (e) => `Undo: ${describe(e)}`,
  undone: 'Undone',
  undoneAnnouncement: (e) => `Undone: ${describe(e)}`,
  undoFailed: (e) => `Could not undo: ${describe(e)}`,
  empty: 'No AI activity yet.',
  showMore: (count) => (count === 1 ? 'Show 1 more action' : `Show ${count} more actions`),
};

export interface AiActionLogProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The actions, in the order shown (usually newest first). */
  entries: readonly AiActionLogEntry[];
  /**
   * Reference time for the relative timestamps. Pass the same value on the
   * server and the client for identical server rendering.
   */
  now?: DateInput | undefined;
  /**
   * How many entries are rendered at first, and how many "Show more" adds each
   * time. Keeps a long log fast. Default 200.
   */
  visibleEntries?: number | undefined;
  /** BCP 47 locale for the timestamps. Default: the runtime locale. */
  locale?: string | undefined;
  /** Replaces any of the English strings. */
  messages?: Partial<AiActionLogMessages> | undefined;
}

type UndoState = 'pending' | 'done' | 'failed';

/**
 * An audit list of what AI agents did: who acted (marked as an AI agent, never
 * mistaken for a person), what they did and to what, when, and an Undo button
 * for reversible actions.
 */
export const AiActionLog = forwardRef<HTMLDivElement, AiActionLogProps>(function AiActionLog(
  { entries, now, locale, visibleEntries = 200, messages, className, ...rest },
  ref,
) {
  const m = { ...AI_ACTION_LOG_MESSAGES, ...messages };
  const [undo, setUndo] = useState<Record<string, UndoState>>({});
  const [live, setLive] = useState('');
  const [alert, setAlert] = useState('');
  const rows = useRef(new Map<string, HTMLLIElement>());

  /* Long logs: a bounded number of rows, more on request. */
  const step = Math.max(1, Math.trunc(visibleEntries) || 1);
  const [shown, setShown] = useState(step);
  const shownEntries = entries.length > shown ? entries.slice(0, shown) : entries;
  const hiddenCount = entries.length - shownEntries.length;
  const focusAfterMore = useRef<string | null>(null);
  useEffect(() => {
    const id = focusAfterMore.current;
    if (!id) return;
    focusAfterMore.current = null;
    // The button may be gone: focus lands on the first row it revealed.
    rows.current.get(id)?.focus();
  }, [shown]);

  const runUndo = async (entry: AiActionLogEntry) => {
    if (!entry.onUndo) return;
    setUndo((u) => ({ ...u, [entry.id]: 'pending' }));
    setAlert('');
    try {
      await entry.onUndo();
      // The Undo button goes away, so keep focus on its row instead of losing it.
      const row = rows.current.get(entry.id);
      const hadFocus =
        row && typeof document !== 'undefined' && row.contains(document.activeElement);
      setUndo((u) => ({ ...u, [entry.id]: 'done' }));
      setLive(m.undoneAnnouncement(entry));
      if (hadFocus) row.focus();
    } catch {
      setUndo((u) => ({ ...u, [entry.id]: 'failed' }));
      setAlert(m.undoFailed(entry));
    }
  };

  return (
    <div ref={ref} {...rest} className={cx('mn-ai-action-log', className)}>
      {entries.length === 0 ? (
        <p className="mn-ai-action-log__empty">{m.empty}</p>
      ) : (
        <ol className="mn-ai-action-log__list" aria-label={m.label}>
          {shownEntries.map((entry) => {
            const state = undo[entry.id];
            const undone = entry.undone || state === 'done';
            const href = entry.href && isSafeUrl(entry.href) ? entry.href : undefined;
            const canUndo = Boolean(entry.undoable && entry.onUndo) && !undone;
            return (
              <li
                key={entry.id}
                ref={(el) => {
                  if (el) rows.current.set(entry.id, el);
                  else rows.current.delete(entry.id);
                }}
                tabIndex={-1}
                className="mn-ai-action-log__entry"
                data-undone={undone || undefined}
              >
                <AiAgentAvatar agent={entry.agent} size="sm" decorative />
                <div className="mn-ai-action-log__body">
                  <p className="mn-ai-action-log__text">
                    <span className="mn-ai-action-log__agent">{entry.agent.name}</span>{' '}
                    <span className="mn-ai-action-log__kind">
                      <span className="mn-visually-hidden">(</span>
                      {m.actorKind}
                      <span className="mn-visually-hidden">)</span>
                    </span>{' '}
                    <span className="mn-ai-action-log__action" dir="auto">
                      {entry.action}
                    </span>
                    {entry.target ? (
                      <>
                        {' '}
                        {href ? (
                          <a className="mn-ai-action-log__target" href={href}>
                            {entry.target}
                          </a>
                        ) : (
                          <span className="mn-ai-action-log__target">{entry.target}</span>
                        )}
                      </>
                    ) : null}
                  </p>
                  <p className="mn-ai-action-log__meta">
                    <RelativeTime
                      date={entry.at}
                      tooltip={false}
                      {...(now !== undefined ? { now } : {})}
                      {...(locale ? { locale } : {})}
                    />
                    {undone ? (
                      <span className="mn-ai-action-log__undone">
                        <span aria-hidden> · </span>
                        {m.undone}
                      </span>
                    ) : null}
                  </p>
                </div>
                {canUndo ? (
                  <Button
                    className="mn-ai-action-log__undo"
                    size="sm"
                    variant="ghost"
                    aria-label={m.undoLabel(entry)}
                    loading={state === 'pending'}
                    onClick={() => {
                      if (state !== 'pending') void runUndo(entry);
                    }}
                  >
                    {m.undo}
                  </Button>
                ) : null}
              </li>
            );
          })}
        </ol>
      )}
      {hiddenCount > 0 ? (
        <Button
          size="sm"
          variant="secondary"
          className="mn-ai-action-log__more"
          onClick={() => {
            focusAfterMore.current = entries[shown]?.id ?? null;
            setShown(shown + step);
          }}
        >
          {m.showMore(Math.min(step, hiddenCount))}
        </Button>
      ) : null}
      <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
        {live}
      </span>
      <span className="mn-visually-hidden" role="alert" aria-atomic="true">
        {alert}
      </span>
    </div>
  );
});
