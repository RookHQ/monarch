import { forwardRef, type HTMLAttributes, type ReactNode, type SVGProps } from 'react';
import { cx } from '../../../internal/cx.js';
import { Avatar } from '../../identity/avatar/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import type { AiAgentRef } from '../ai-conversation/index.js';
import { AiAgentAvatar } from '../ai-agent-avatar/index.js';

/** A person who takes over from an agent. */
export interface AiHandoffHuman {
  kind: 'human';
  /** The person's name. */
  name: string;
  /** Image URL for the person's avatar. */
  avatar?: string | undefined;
}

/** Who receives the work: another agent, or a person. */
export type AiHandoffTarget = AiAgentRef | AiHandoffHuman;

/** True when the handoff goes to a person rather than an agent. */
export function isAiHandoffHuman(target: AiHandoffTarget): target is AiHandoffHuman {
  return (target as Partial<AiHandoffHuman>).kind === 'human';
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiHandoffMessages {
  /** Accessible name of the marker. */
  label: string;
  /**
   * The sentence. `from` and `to` are the rendered names; `toHuman` says
   * whether the work went to a person.
   */
  handedOff: (from: ReactNode, to: ReactNode, toHuman: boolean) => ReactNode;
  /** Visually hidden tag read after an agent's name. */
  agentKind: string;
  /** Prefix for the reason, read before it. */
  reason: string;
}

export const AI_HANDOFF_MESSAGES: AiHandoffMessages = {
  label: 'Handoff',
  handedOff: (from, to) => (
    <>
      {from} handed off to {to}
    </>
  ),
  agentKind: 'AI agent',
  reason: 'Reason:',
};

export interface AiHandoffProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The agent that hands the work over. */
  from: AiAgentRef;
  /** The agent, or the person (`{ kind: 'human', name }`), that takes it. */
  to: AiHandoffTarget;
  /** Why the work moved, e.g. "Needs a person to approve the refund". */
  reason?: ReactNode | undefined;
  /** When it happened. Shown as relative time. */
  at?: DateInput | undefined;
  /** Reference time for `at`, for identical server and client rendering. */
  now?: DateInput | undefined;
  /** BCP 47 locale for the time. Default: the runtime locale. */
  locale?: string | undefined;
  /** Replaces any of the English strings. */
  messages?: Partial<AiHandoffMessages> | undefined;
}

function ArrowGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width={16}
      height={16}
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
      {...props}
    >
      <path d="M3 8h10M9 4l4 4-4 4" />
    </svg>
  );
}

/**
 * An inline marker in a thread where one agent hands the work to another
 * agent or to a person: both pictures with an arrow, a sentence saying who
 * handed off to whom, the reason and the time.
 */
export const AiHandoff = forwardRef<HTMLDivElement, AiHandoffProps>(function AiHandoff(
  { from, to, reason, at, now, locale, messages, className, ...rest },
  ref,
) {
  const m = { ...AI_HANDOFF_MESSAGES, ...messages };
  const toHuman = isAiHandoffHuman(to);
  const agentName = (name: string) => (
    <span className="mn-ai-handoff__name">
      {name}
      <span className="mn-visually-hidden"> ({m.agentKind})</span>
    </span>
  );
  const hasReason = reason !== undefined && reason !== null && reason !== '';

  return (
    <div
      ref={ref}
      role="note"
      aria-label={m.label}
      {...rest}
      className={cx('mn-ai-handoff', className)}
      data-to={toHuman ? 'human' : 'agent'}
    >
      <span className="mn-ai-handoff__line" aria-hidden />
      <div className="mn-ai-handoff__body">
        <span className="mn-ai-handoff__pictures" aria-hidden>
          <AiAgentAvatar agent={from} size="sm" decorative />
          <ArrowGlyph className="mn-ai-handoff__arrow" />
          {toHuman ? (
            <Avatar name={to.name} src={to.avatar ?? null} size="sm" decorative />
          ) : (
            <AiAgentAvatar agent={to} size="sm" decorative />
          )}
        </span>
        <span className="mn-ai-handoff__text">
          <span className="mn-ai-handoff__sentence">
            {m.handedOff(
              agentName(from.name),
              toHuman ? <span className="mn-ai-handoff__name">{to.name}</span> : agentName(to.name),
              toHuman,
            )}
          </span>
          {hasReason ? (
            <span className="mn-ai-handoff__reason">
              <span className="mn-visually-hidden">{m.reason} </span>
              {reason}
            </span>
          ) : null}
          {at !== undefined ? (
            <RelativeTime
              className="mn-ai-handoff__time"
              date={at}
              tooltip={false}
              {...(now !== undefined ? { now } : {})}
              {...(locale !== undefined ? { locale } : {})}
            />
          ) : null}
        </span>
      </div>
      <span className="mn-ai-handoff__line" aria-hidden />
    </div>
  );
});
