import { forwardRef, type HTMLAttributes, type ReactNode, type Ref } from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, CloseGlyph, DashGlyph, DotGlyph } from '../../../internal/icons.js';
import { Avatar } from '../../identity/avatar/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';

/**
 * Where a step stands.
 * - `submitted`: the request was sent (usually the first entry).
 * - `approved` / `rejected`: someone decided.
 * - `pending`: waiting for a decision now.
 * - `waiting`: a later step that has not started.
 * - `skipped`: passed over, for example because the approver is the requester.
 * - `cancelled`: withdrawn before a decision.
 */
export type ApprovalStepStatus =
  'submitted' | 'approved' | 'rejected' | 'pending' | 'waiting' | 'skipped' | 'cancelled';

/** A person shown on a step. */
export interface ApprovalPerson {
  name: string;
  avatarUrl?: string | null | undefined;
}

/** One entry of the timeline. */
export interface ApprovalTimelineStep {
  /** Stable, unique id. */
  id: string;
  /** What the step is, e.g. "Reporting manager" or "Finance check". */
  label: ReactNode;
  status: ApprovalStepStatus;
  /** Who decided (or submitted). Shown with the time. */
  actor?: ApprovalPerson | undefined;
  /** Who can decide a step that is not decided yet. */
  approvers?: readonly ApprovalPerson[] | undefined;
  /** The person an approver is standing in for. */
  delegatedFrom?: ApprovalPerson | undefined;
  /** When it was decided or submitted. */
  at?: DateInput | undefined;
  /** The decider's comment, shown as a quote. */
  comment?: ReactNode;
  /** Why the step was skipped or cancelled, or any other short note. */
  note?: ReactNode;
}

/** Labels. Override them to translate. */
export interface ApprovalTimelineMessages {
  /** Accessible name of the list. */
  label: string;
  /** Visible status text after each step label. */
  status: Record<ApprovalStepStatus, string>;
  /** Text before the people who can decide a pending or waiting step. */
  approvers: (names: string) => string;
  /** Text after an approver who stands in for someone else. */
  delegatedFrom: (name: string) => string;
  /** Shown when there are no steps. */
  empty: string;
}

const DEFAULT_MESSAGES: ApprovalTimelineMessages = {
  label: 'Approval timeline',
  status: {
    submitted: 'Submitted',
    approved: 'Approved',
    rejected: 'Rejected',
    pending: 'Waiting for a decision',
    waiting: 'Up next',
    skipped: 'Skipped',
    cancelled: 'Cancelled',
  },
  approvers: (names) => `Decided by ${names}`,
  delegatedFrom: (name) => `standing in for ${name}`,
  empty: 'Not submitted yet.',
};

export interface ApprovalTimelineProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** The steps in order, from submission to the outcome. */
  steps: readonly ApprovalTimelineStep[];
  /**
   * The reference time for relative times. Pass the same value on the server
   * and the client for identical server rendering.
   */
  now?: DateInput | undefined;
  /** BCP 47 locale for times. Default: the runtime locale. */
  locale?: string | undefined;
  /** IANA time zone for absolute times. Default: the viewer's zone. */
  timeZone?: string | undefined;
  /** Show absolute dates once a time is more than this many milliseconds away. Default 7 days. */
  absoluteAfter?: number | undefined;
  /** Show avatars next to deciders. Default true. */
  showAvatars?: boolean | undefined;
  /** Override labels. `status` may name only the statuses to change. */
  messages?:
    | (Partial<Omit<ApprovalTimelineMessages, 'status'>> & {
        status?: Partial<Record<ApprovalStepStatus, string>> | undefined;
      })
    | undefined;
}

const WEEK = 7 * 86_400_000;

function Marker({ status }: { status: ApprovalStepStatus }) {
  let glyph: ReactNode = null;
  if (status === 'approved' || status === 'submitted') glyph = <CheckGlyph />;
  else if (status === 'rejected' || status === 'cancelled') glyph = <CloseGlyph />;
  else if (status === 'skipped') glyph = <DashGlyph />;
  else if (status === 'pending') glyph = <DotGlyph />;
  return (
    <span className="mn-approval-timeline__marker" aria-hidden>
      {glyph}
    </span>
  );
}

/**
 * The history and outlook of one approval: who submitted it, each step with
 * its decision, who is deciding now and what comes next. Read-only; the
 * product supplies the steps from its approval engine.
 */
export const ApprovalTimeline = forwardRef<HTMLElement, ApprovalTimelineProps>(
  function ApprovalTimeline(
    {
      steps,
      now,
      locale,
      timeZone,
      absoluteAfter = WEEK,
      showAvatars = true,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      status: { ...DEFAULT_MESSAGES.status, ...messagesProp?.status },
    };

    if (steps.length === 0) {
      return (
        <div
          ref={ref as Ref<HTMLDivElement>}
          {...rest}
          className={cx('mn-approval-timeline', className)}
          data-empty=""
        >
          <p className="mn-approval-timeline__empty">{m.empty}</p>
        </div>
      );
    }

    return (
      <ol
        ref={ref as Ref<HTMLOListElement>}
        aria-label={m.label}
        {...rest}
        className={cx('mn-approval-timeline', className)}
      >
        {steps.map((step) => {
          const decided =
            step.status === 'approved' ||
            step.status === 'rejected' ||
            step.status === 'submitted' ||
            step.status === 'cancelled';
          const undecided = step.status === 'pending' || step.status === 'waiting';
          const approvers = step.approvers ?? [];
          return (
            <li
              key={step.id}
              className="mn-approval-timeline__step"
              data-status={step.status}
              aria-current={step.status === 'pending' ? 'step' : undefined}
            >
              <Marker status={step.status} />
              <div className="mn-approval-timeline__body">
                <p className="mn-approval-timeline__title">
                  <span className="mn-approval-timeline__label">{step.label}</span>
                  <span className="mn-approval-timeline__status">{m.status[step.status]}</span>
                </p>
                {decided && (step.actor || step.at !== undefined) ? (
                  <p className="mn-approval-timeline__meta">
                    {step.actor && showAvatars ? (
                      <Avatar
                        name={step.actor.name}
                        src={step.actor.avatarUrl ?? null}
                        size="xs"
                        decorative
                      />
                    ) : null}
                    {step.actor ? (
                      <span className="mn-approval-timeline__actor">{step.actor.name}</span>
                    ) : null}
                    {step.actor && step.at !== undefined ? (
                      <span aria-hidden className="mn-approval-timeline__sep">
                        ·
                      </span>
                    ) : null}
                    {step.at !== undefined ? (
                      <RelativeTime
                        date={step.at}
                        {...(now !== undefined ? { now } : {})}
                        {...(locale ? { locale } : {})}
                        {...(timeZone ? { timeZone } : {})}
                        absoluteAfter={absoluteAfter}
                        tooltip={false}
                      />
                    ) : null}
                  </p>
                ) : null}
                {undecided && approvers.length > 0 ? (
                  <p className="mn-approval-timeline__meta">
                    {m.approvers(approvers.map((a) => a.name).join(', '))}
                    {step.delegatedFrom ? ` (${m.delegatedFrom(step.delegatedFrom.name)})` : null}
                  </p>
                ) : null}
                {decided && step.delegatedFrom ? (
                  <p className="mn-approval-timeline__meta">
                    {m.delegatedFrom(step.delegatedFrom.name)}
                  </p>
                ) : null}
                {step.comment ? (
                  <blockquote className="mn-approval-timeline__comment">{step.comment}</blockquote>
                ) : null}
                {step.note ? <p className="mn-approval-timeline__note">{step.note}</p> : null}
              </div>
            </li>
          );
        })}
      </ol>
    );
  },
);
