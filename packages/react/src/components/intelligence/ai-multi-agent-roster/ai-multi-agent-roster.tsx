'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import type { AiAgentRef } from '../ai-conversation/index.js';
import {
  AiAgentAvatar,
  AI_AGENT_AVATAR_MESSAGES,
  type AiAgentState,
} from '../ai-agent-avatar/index.js';

/** One agent in the roster and what it is doing. */
export interface AiRosterMember {
  agent: AiAgentRef;
  state: AiAgentState;
  /** What it is working on, e.g. "Comparing three sources". */
  task?: ReactNode | undefined;
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiMultiAgentRosterMessages {
  /** Accessible name of the list. */
  label: string;
  /** State text for each agent. */
  states: Record<AiAgentState, string>;
  /** Visually hidden tag read after each agent's name. */
  agentKind: string;
  /** Summary line, e.g. "3 agents · 2 working". */
  summary: (total: number, counts: Record<AiAgentState, number>) => string;
  /** Announced when an agent's state changes. */
  changed: (name: string, state: string) => string;
  /** Shown when there are no agents. */
  empty: string;
}

export const AI_MULTI_AGENT_ROSTER_MESSAGES: AiMultiAgentRosterMessages = {
  label: 'Agents',
  states: AI_AGENT_AVATAR_MESSAGES.states,
  agentKind: 'AI agent',
  summary: (total, c) =>
    [
      `${total} ${total === 1 ? 'agent' : 'agents'}`,
      c.working ? `${c.working} working` : '',
      c.waiting ? `${c.waiting} waiting for you` : '',
      c.error ? `${c.error} with errors` : '',
    ]
      .filter(Boolean)
      .join(' · '),
  changed: (name, state) => `${name}: ${state}`,
  empty: 'No agents are working on this.',
};

export interface AiMultiAgentRosterProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The agents and their states. Keep each `agent.id` (or name) stable between updates. */
  agents: readonly AiRosterMember[];
  /**
   * `list` (default) shows a row per agent with its state and task; `stack`
   * shows overlapping pictures with the summary, for headers and toolbars.
   */
  variant?: 'list' | 'stack' | undefined;
  /** Show the "3 agents · 2 working" summary. Default true. */
  showSummary?: boolean | undefined;
  /** In `stack`, how many pictures to show before "+N". Default 4. */
  max?: number | undefined;
  /**
   * Announce state changes ("Writing agent: Waiting for you") through a polite
   * live region. Never the initial state. Default true.
   */
  announce?: boolean | undefined;
  /** Replaces any of the English strings; `states` may be partial. */
  messages?:
    | (Partial<Omit<AiMultiAgentRosterMessages, 'states'>> & {
        states?: Partial<Record<AiAgentState, string>> | undefined;
      })
    | undefined;
}

const keyOf = (a: AiAgentRef) => a.id ?? a.name;
const ORDER: AiAgentState[] = ['error', 'waiting', 'working', 'idle'];

/**
 * Every agent taking part in a task and what each is doing now: a list with
 * state and current task, or a compact stack of pictures for a header. State
 * changes are announced politely.
 */
export const AiMultiAgentRoster = forwardRef<HTMLDivElement, AiMultiAgentRosterProps>(
  function AiMultiAgentRoster(
    {
      agents,
      variant = 'list',
      showSummary = true,
      max = 4,
      announce = true,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const { states: _states, ...overrides } = messagesProp ?? {};
    const m = { ...AI_MULTI_AGENT_ROSTER_MESSAGES, ...overrides };
    const states = { ...AI_MULTI_AGENT_ROSTER_MESSAGES.states, ...messagesProp?.states };
    const avatarMessages = { kind: m.agentKind, states };

    const counts: Record<AiAgentState, number> = { idle: 0, working: 0, waiting: 0, error: 0 };
    for (const a of agents) counts[a.state] += 1;
    const summary = m.summary(agents.length, counts);

    // Announce changes, never the initial state.
    const previous = useRef<Map<string, AiAgentState> | null>(null);
    const [live, setLive] = useState('');
    const signature = agents.map((a) => `${keyOf(a.agent)}:${a.state}`).join('|');
    useEffect(() => {
      const before = previous.current;
      previous.current = new Map(agents.map((a) => [keyOf(a.agent), a.state]));
      if (!before || !announce) return;
      const lines = agents
        .filter((a) => before.get(keyOf(a.agent)) !== a.state)
        .map((a) => m.changed(a.agent.name, states[a.state]));
      if (lines.length) setLive(lines.join('. '));
      // `signature` captures every change that matters.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [signature, announce]);

    let body: ReactNode;
    if (agents.length === 0) {
      body = <p className="mn-ai-multi-agent-roster__empty">{m.empty}</p>;
    } else if (variant === 'stack') {
      // Agents that need attention come first, so they are never hidden in "+N".
      const sorted = [...agents].sort((a, b) => ORDER.indexOf(a.state) - ORDER.indexOf(b.state));
      const visible = sorted.slice(0, Math.max(1, max));
      const hidden = sorted.length - visible.length;
      body = (
        <ul className="mn-ai-multi-agent-roster__stack" aria-label={m.label}>
          {visible.map((a) => (
            <li key={keyOf(a.agent)} className="mn-ai-multi-agent-roster__stack-item">
              <AiAgentAvatar agent={a.agent} state={a.state} size="sm" messages={avatarMessages} />
            </li>
          ))}
          {hidden > 0 ? (
            <li className="mn-ai-multi-agent-roster__more">
              <span aria-hidden>+{hidden}</span>
              <span className="mn-visually-hidden">
                {sorted
                  .slice(visible.length)
                  .map((a) => `${a.agent.name}, ${states[a.state]}`)
                  .join('; ')}
              </span>
            </li>
          ) : null}
        </ul>
      );
    } else {
      body = (
        <ul className="mn-ai-multi-agent-roster__list" aria-label={m.label}>
          {agents.map((a) => (
            <li
              key={keyOf(a.agent)}
              className="mn-ai-multi-agent-roster__item"
              data-state={a.state}
            >
              {/* The row's text says the state, so the picture stays silent. */}
              <span className="mn-ai-multi-agent-roster__picture" aria-hidden>
                <AiAgentAvatar agent={a.agent} state={a.state} decorative />
              </span>
              <span className="mn-ai-multi-agent-roster__text">
                <span className="mn-ai-multi-agent-roster__heading">
                  <span className="mn-ai-multi-agent-roster__name">
                    {a.agent.name}
                    <span className="mn-visually-hidden"> ({m.agentKind})</span>
                  </span>
                  {a.agent.role ? (
                    <span className="mn-ai-multi-agent-roster__role">{a.agent.role}</span>
                  ) : null}
                </span>
                {a.task !== undefined && a.task !== null && a.task !== '' ? (
                  <span className="mn-ai-multi-agent-roster__task">{a.task}</span>
                ) : null}
              </span>
              <span className="mn-ai-multi-agent-roster__state">
                <span className="mn-visually-hidden">, </span>
                {states[a.state]}
              </span>
            </li>
          ))}
        </ul>
      );
    }

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-multi-agent-roster', className)}
        data-variant={variant}
      >
        {body}
        {showSummary && agents.length > 0 ? (
          <p className="mn-ai-multi-agent-roster__summary">{summary}</p>
        ) : null}
        {announce ? (
          <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
            {live}
          </span>
        ) : null}
      </div>
    );
  },
);
