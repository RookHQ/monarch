'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  CheckGlyph,
  CloseGlyph,
  LockGlyph,
  WarningGlyph,
  BotGlyph,
} from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { ConfirmDialog } from '../../overlays/confirm-dialog/index.js';

/** How much harm running an action could do. `destructive` always asks for approval. */
export type AiSuggestedActionRisk = 'low' | 'medium' | 'high' | 'destructive';

/** Where an action is in its run. */
export type AiSuggestedActionStatus = 'ready' | 'running' | 'done' | 'failed';

/** One suggested next step. */
export interface AiSuggestedAction {
  /** Stable id, passed to `onRun` and `onDismiss`. */
  id: string;
  /** What the action does, as a verb phrase: "Send the renewal reminder". */
  label: string;
  /** Why it is suggested or what it will change. */
  description?: ReactNode | undefined;
  /** Ask for confirmation in a dialog before running. Implied by `risk: 'destructive'`. */
  requiresApproval?: boolean | undefined;
  /** Shown as a text label for `medium` and above; sets the approval dialog tone. */
  risk?: AiSuggestedActionRisk | undefined;
  /** Concrete consequences listed in the approval dialog ("3 people will get an email"). */
  consequences?: ReactNode[] | undefined;
  /** Label of the approval dialog's confirm button. Default "Run action". */
  confirmLabel?: string | undefined;
  /**
   * Controlled status. Without it the component tracks the status itself
   * from the promise `onRun` returns.
   */
  status?: AiSuggestedActionStatus | undefined;
}

export interface AiSuggestedActionsMessages {
  /** Default heading. */
  title: string;
  /** Provenance chip, per origin. */
  origin: Record<'ai' | 'rule', string>;
  run: string;
  running: string;
  done: string;
  /** Shown (assertively) under an action whose run failed. */
  failed: string;
  /** Accessible name of the dismiss button. */
  dismiss: (label: string) => string;
  needsApproval: string;
  /** Risk label, per risk; `low` is not shown. */
  risk: Record<AiSuggestedActionRisk, string>;
  /** Approval dialog title. */
  approvalTitle: (label: string) => string;
  /** Approval dialog description when the action has none. */
  approvalDescription: string;
  approve: string;
  cancel: string;
  /** Announced politely when an action finishes. */
  doneAnnouncement: (label: string) => string;
  /** Shown when there are no actions. */
  empty: string;
}

export const AI_SUGGESTED_ACTIONS_MESSAGES: AiSuggestedActionsMessages = {
  title: 'Suggested actions',
  origin: { ai: 'Suggested by AI', rule: 'Rule-based' },
  run: 'Run',
  running: 'Running…',
  done: 'Done',
  failed: 'That didn’t work. Try again.',
  dismiss: (label) => `Dismiss “${label}”`,
  needsApproval: 'Needs approval',
  risk: { low: 'Low risk', medium: 'Medium risk', high: 'High risk', destructive: 'Destructive' },
  approvalTitle: (label) => `Run “${label}”?`,
  approvalDescription: 'Check what this action will do before it runs.',
  approve: 'Run action',
  cancel: 'Cancel',
  doneAnnouncement: (label) => `“${label}” done.`,
  empty: 'No suggestions right now.',
};

export interface AiSuggestedActionsProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** The suggestions, most useful first. */
  actions: readonly AiSuggestedAction[];
  /**
   * Runs an action (after approval when it needs it). Return a promise to show
   * Running… until it settles; a rejection marks the action failed (or shows
   * the error inside the approval dialog).
   */
  onRun: (id: string) => unknown;
  /** Shows a dismiss button per action. Remove the action from `actions`; focus moves to the next one. */
  onDismiss?: ((id: string) => void) | undefined;
  /** Heading. Default "Suggested actions". */
  title?: ReactNode | undefined;
  /** Heading level. Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | 6 | undefined;
  /**
   * Where the suggestions came from, always shown. `ai` (default) shows the
   * Bot glyph in the AI colour; `rule` is labelled "Rule-based" in neutral colours.
   */
  origin?: 'ai' | 'rule' | undefined;
  /** Strings, for localisation. `origin` and `risk` merge per key. */
  messages?:
    | (Partial<Omit<AiSuggestedActionsMessages, 'origin' | 'risk'>> & {
        origin?: Partial<Record<'ai' | 'rule', string>>;
        risk?: Partial<Record<AiSuggestedActionRisk, string>>;
      })
    | undefined;
}

function isThenable(value: unknown): value is PromiseLike<unknown> {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as { then?: unknown }).then === 'function'
  );
}

/** True when the action must be confirmed in a dialog before it runs. */
export function aiActionNeedsApproval(action: AiSuggestedAction): boolean {
  return Boolean(action.requiresApproval) || action.risk === 'destructive';
}

/**
 * A short list of next best actions suggested for the current record, each
 * with a Run button. Actions that need approval (and every destructive one)
 * open a ConfirmDialog first, so nothing consequential runs on one click.
 * Shows running, done and failed states, and keeps focus in the list when an
 * action is dismissed.
 */
export const AiSuggestedActions = forwardRef<HTMLDivElement, AiSuggestedActionsProps>(
  function AiSuggestedActions(
    {
      actions,
      onRun,
      onDismiss,
      title,
      headingLevel = 3,
      origin = 'ai',
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = {
      ...AI_SUGGESTED_ACTIONS_MESSAGES,
      ...messagesProp,
      origin: { ...AI_SUGGESTED_ACTIONS_MESSAGES.origin, ...messagesProp?.origin },
      risk: { ...AI_SUGGESTED_ACTIONS_MESSAGES.risk, ...messagesProp?.risk },
    };
    const baseId = useId();
    const headingId = `${baseId}-heading`;
    const Heading = `h${headingLevel}` as const;
    const headingRef = useRef<HTMLHeadingElement>(null);
    const controls = useRef(new Map<string, HTMLElement>());
    const [statuses, setStatuses] = useState<Record<string, AiSuggestedActionStatus>>({});
    const [approving, setApproving] = useState<AiSuggestedAction | null>(null);
    const [dialogOpen, setDialogOpen] = useState(false);
    const [announcement, setAnnouncement] = useState('');
    const dismissed = useRef<{ id: string; index: number } | null>(null);
    const mounted = useRef(true);

    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    const setControl = (id: string, el: HTMLElement | null) => {
      if (el) {
        controls.current.set(id, el);
        return;
      }
      const current = controls.current.get(id);
      if (current && !current.isConnected) controls.current.delete(id);
    };

    const setStatus = useCallback((id: string, status: AiSuggestedActionStatus) => {
      if (mounted.current) setStatuses((s) => ({ ...s, [id]: status }));
    }, []);

    /** Focus to restore to an action's control after its status changes the control. */
    const refocus = useRef<string | null>(null);

    /** Runs an action and tracks its status; rethrows so ConfirmDialog can show the error. */
    const execute = async (action: AiSuggestedAction) => {
      const finish = () => {
        const control = controls.current.get(action.id);
        if (control && control === control.ownerDocument.activeElement) refocus.current = action.id;
        setStatus(action.id, 'done');
        if (mounted.current) setAnnouncement(messages.doneAnnouncement(action.label));
      };
      const result = onRun(action.id);
      if (!isThenable(result)) {
        finish();
        return;
      }
      setStatus(action.id, 'running');
      try {
        await result;
      } catch (error) {
        setStatus(action.id, 'failed');
        throw error;
      }
      finish();
    };

    // The Run button becomes a "Done" label; keep focus on it if it had focus.
    useEffect(() => {
      const id = refocus.current;
      if (!id) return;
      refocus.current = null;
      controls.current.get(id)?.focus();
    }, [statuses]);

    const run = (action: AiSuggestedAction) => {
      if (aiActionNeedsApproval(action)) {
        setApproving(action);
        setDialogOpen(true);
        return;
      }
      execute(action).catch(() => {
        /* Shown as the failed state. */
      });
    };

    const dismiss = (id: string, index: number) => {
      dismissed.current = { id, index };
      onDismiss?.(id);
    };

    // When a dismissed action leaves the list, focus the next one (or the
    // previous, or the heading) so keyboard focus is never lost.
    useEffect(() => {
      const d = dismissed.current;
      if (!d || actions.some((a) => a.id === d.id)) return;
      dismissed.current = null;
      const target = actions[d.index] ?? actions[d.index - 1];
      const button = target ? controls.current.get(target.id) : undefined;
      (button ?? headingRef.current)?.focus();
    }, [actions]);

    const tone =
      approving?.risk === 'destructive'
        ? 'danger'
        : approving?.risk === 'high'
          ? 'warning'
          : 'default';

    return (
      <div
        ref={ref}
        role="group"
        aria-labelledby={headingId}
        {...rest}
        className={cx('mn-ai-suggested-actions', className)}
        data-origin={origin}
      >
        <div className="mn-ai-suggested-actions__header">
          <Heading
            ref={headingRef}
            id={headingId}
            tabIndex={-1}
            className="mn-ai-suggested-actions__title"
          >
            {title ?? messages.title}
          </Heading>
          <span className="mn-ai-suggested-actions__origin" data-origin={origin}>
            {origin === 'ai' ? <BotGlyph /> : null}
            {messages.origin[origin]}
          </span>
        </div>

        {actions.length === 0 ? (
          <p className="mn-ai-suggested-actions__empty">{messages.empty}</p>
        ) : (
          <ul className="mn-ai-suggested-actions__list">
            {actions.map((action, index) => {
              const status = action.status ?? statuses[action.id] ?? 'ready';
              const labelId = `${baseId}-${index}-label`;
              const runTextId = `${baseId}-${index}-run`;
              const descId = `${baseId}-${index}-desc`;
              const approval = aiActionNeedsApproval(action);
              const risk = action.risk && action.risk !== 'low' ? action.risk : undefined;
              const done = status === 'done';
              const busy = status === 'running';
              return (
                <li
                  key={action.id}
                  className="mn-ai-suggested-actions__item"
                  data-status={status}
                  data-risk={action.risk}
                >
                  <div className="mn-ai-suggested-actions__text">
                    <p id={labelId} className="mn-ai-suggested-actions__label">
                      {action.label}
                    </p>
                    {action.description ? (
                      <div id={descId} className="mn-ai-suggested-actions__description">
                        {action.description}
                      </div>
                    ) : null}
                    {risk || approval ? (
                      <div className="mn-ai-suggested-actions__tags">
                        {risk ? (
                          <span className="mn-ai-suggested-actions__tag" data-risk={risk}>
                            <WarningGlyph aria-hidden />
                            {messages.risk[risk]}
                          </span>
                        ) : null}
                        {approval ? (
                          <span className="mn-ai-suggested-actions__tag" data-kind="approval">
                            <LockGlyph aria-hidden />
                            {messages.needsApproval}
                          </span>
                        ) : null}
                      </div>
                    ) : null}
                    {status === 'failed' ? (
                      <p role="alert" className="mn-ai-suggested-actions__failed">
                        {messages.failed}
                      </p>
                    ) : null}
                  </div>
                  <div className="mn-ai-suggested-actions__controls">
                    {done ? (
                      <span
                        ref={(el) => setControl(action.id, el)}
                        tabIndex={-1}
                        className="mn-ai-suggested-actions__done"
                      >
                        <CheckGlyph aria-hidden />
                        {messages.done}
                      </span>
                    ) : (
                      <Button
                        ref={(el) => setControl(action.id, el)}
                        size="sm"
                        loading={busy}
                        aria-labelledby={`${runTextId} ${labelId}`}
                        aria-describedby={action.description ? descId : undefined}
                        className="mn-ai-suggested-actions__run"
                        onClick={() => run(action)}
                      >
                        <span id={runTextId}>{busy ? messages.running : messages.run}</span>
                      </Button>
                    )}
                    {onDismiss ? (
                      <IconButton
                        size="sm"
                        label={messages.dismiss(action.label)}
                        icon={<CloseGlyph />}
                        className="mn-ai-suggested-actions__dismiss"
                        onClick={() => dismiss(action.id, index)}
                      />
                    ) : null}
                  </div>
                </li>
              );
            })}
          </ul>
        )}

        <span role="status" className="mn-visually-hidden">
          {announcement}
        </span>

        <ConfirmDialog
          open={dialogOpen}
          onOpenChange={setDialogOpen}
          title={approving ? messages.approvalTitle(approving.label) : ''}
          description={approving?.description ?? messages.approvalDescription}
          {...(approving?.consequences ? { consequences: approving.consequences } : {})}
          tone={tone}
          confirmLabel={approving?.confirmLabel ?? messages.approve}
          cancelLabel={messages.cancel}
          onConfirm={() => (approving ? execute(approving) : undefined)}
          onCloseAutoFocus={(event) => {
            const target = approving ? controls.current.get(approving.id) : undefined;
            if (!target?.isConnected) return;
            event.preventDefault();
            target.focus();
          }}
        />
      </div>
    );
  },
);
