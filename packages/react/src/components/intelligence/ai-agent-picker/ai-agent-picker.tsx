'use client';

import {
  forwardRef,
  useId,
  useRef,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph } from '../../../internal/icons.js';
import type { AiAgentRef } from '../ai-conversation/index.js';
import type { AiAgentState } from '../ai-agent-avatar/index.js';
import { AiAgentCard, type AiAgentTool } from '../ai-agent-card/index.js';

/** One agent a person can choose. */
export interface AiAgentPickerOption {
  /** The agent. Its `id` (or, without one, its `name`) is the option's value. */
  agent: AiAgentRef;
  /** One line on what the agent is for. */
  description?: ReactNode;
  /** Short capability phrases, shown as tags. */
  capabilities?: readonly string[] | undefined;
  /** The agent's tools; the option shows how many. */
  tools?: readonly AiAgentTool[] | undefined;
  /** What the agent is doing now, shown on its avatar. */
  state?: AiAgentState | undefined;
  /** The agent cannot be chosen now (for example, not enabled in this space). */
  disabled?: boolean | undefined;
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiAgentPickerMessages {
  /** Accessible name of the group when no `aria-label` or `aria-labelledby` is given. */
  label: string;
  /** Shown when there are no agents. */
  empty: string;
}

export const AI_AGENT_PICKER_MESSAGES: AiAgentPickerMessages = {
  label: 'Choose an agent',
  empty: 'No agents are available.',
};

/** The value of an option: the agent's `id`, or its `name` when it has no id. */
export const aiAgentKey = (agent: AiAgentRef): string => agent.id ?? agent.name;

export interface AiAgentPickerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'onChange' | 'defaultValue'
> {
  /** The agents to choose from, in display order. */
  agents: readonly AiAgentPickerOption[];
  /** The chosen agent's key (`agent.id`, or `agent.name` without an id), or `null` for none. */
  value: string | null;
  /** Called with the chosen agent's key and option when the person picks one. */
  onChange?: ((value: string, option: AiAgentPickerOption) => void) | undefined;
  /** `list` (default) stacks the agents; `grid` lays them out in columns that wrap. */
  layout?: 'list' | 'grid' | undefined;
  /** Disables every option. */
  disabled?: boolean | undefined;
  /** Replaces any of the English strings. */
  messages?: Partial<AiAgentPickerMessages> | undefined;
}

/**
 * A single-choice list of AI agents, each shown as a compact AiAgentCard
 * (avatar, name, role, description, capabilities, tool count). It is a radio
 * group: arrow keys move between agents and choose, Tab leaves the group.
 */
export const AiAgentPicker = forwardRef<HTMLDivElement, AiAgentPickerProps>(function AiAgentPicker(
  {
    agents,
    value,
    onChange,
    layout = 'list',
    disabled = false,
    messages,
    className,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const m = { ...AI_AGENT_PICKER_MESSAGES, ...messages };
  const baseId = useId();
  const refs = useRef<(HTMLDivElement | null)[]>([]);
  const isDisabled = (o: AiAgentPickerOption | undefined) => !o || disabled || Boolean(o.disabled);
  const selectedIndex = agents.findIndex((o) => aiAgentKey(o.agent) === value);
  const focusIndex =
    selectedIndex >= 0 && !isDisabled(agents[selectedIndex])
      ? selectedIndex
      : agents.findIndex((o) => !isDisabled(o));

  const choose = (index: number) => {
    const option = agents[index];
    if (!option || isDisabled(option)) return;
    refs.current[index]?.focus();
    if (aiAgentKey(option.agent) !== value) onChange?.(aiAgentKey(option.agent), option);
  };

  const step = (from: number, delta: 1 | -1) => {
    const n = agents.length;
    for (let i = 1; i <= n; i++) {
      const next = (((from + delta * i) % n) + n) % n;
      if (!isDisabled(agents[next])) return next;
    }
    return from;
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    onKeyDown?.(event);
    if (event.defaultPrevented) return;
    const current = refs.current.findIndex((el) => el === event.target);
    if (current < 0) return;
    let next: number | null = null;
    switch (event.key) {
      case 'ArrowDown':
      case 'ArrowRight':
        next = step(current, 1);
        break;
      case 'ArrowUp':
      case 'ArrowLeft':
        next = step(current, -1);
        break;
      case 'Home':
        next = agents.findIndex((o) => !isDisabled(o));
        break;
      case 'End': {
        const last = [...agents].reverse().findIndex((o) => !isDisabled(o));
        next = last < 0 ? -1 : agents.length - 1 - last;
        break;
      }
      case ' ':
      case 'Enter':
        next = current;
        break;
      default:
        return;
    }
    event.preventDefault();
    if (next !== null && next >= 0) choose(next);
  };

  const named = rest['aria-label'] !== undefined || rest['aria-labelledby'] !== undefined;

  return (
    // Keys are handled here for every radio (roving tabindex); the group itself is not a tab stop.
    // eslint-disable-next-line jsx-a11y/interactive-supports-focus
    <div
      ref={ref}
      role="radiogroup"
      aria-label={named ? undefined : m.label}
      aria-disabled={disabled || undefined}
      {...rest}
      className={cx('mn-ai-agent-picker', className)}
      data-layout={layout}
      data-disabled={disabled || undefined}
      onKeyDown={handleKeyDown}
    >
      {agents.length === 0 ? <p className="mn-ai-agent-picker__empty">{m.empty}</p> : null}
      {agents.map((option, index) => {
        const key = aiAgentKey(option.agent);
        const checked = index === selectedIndex;
        const off = isDisabled(option);
        const nameId = `${baseId}-${index}-name`;
        const descriptionId = `${baseId}-${index}-description`;
        const hasDescription =
          option.description !== undefined &&
          option.description !== null &&
          option.description !== '';
        return (
          // Keyboard choice is handled by the group's onKeyDown.
          // eslint-disable-next-line jsx-a11y/click-events-have-key-events
          <div
            key={key}
            ref={(el) => {
              refs.current[index] = el;
            }}
            role="radio"
            aria-checked={checked}
            aria-disabled={off || undefined}
            aria-labelledby={nameId}
            aria-describedby={hasDescription ? descriptionId : undefined}
            tabIndex={index === focusIndex ? 0 : -1}
            className="mn-ai-agent-picker__option"
            data-checked={checked || undefined}
            data-disabled={off || undefined}
            onClick={() => choose(index)}
          >
            <AiAgentCard
              as="div"
              variant="compact"
              headingLevel="none"
              agent={option.agent}
              description={option.description}
              capabilities={option.capabilities}
              tools={option.tools}
              state={option.state}
              nameId={nameId}
              descriptionId={descriptionId}
              className="mn-ai-agent-picker__card"
            />
            <span className="mn-ai-agent-picker__indicator" aria-hidden>
              {checked ? <CheckGlyph /> : null}
            </span>
          </div>
        );
      })}
    </div>
  );
});
