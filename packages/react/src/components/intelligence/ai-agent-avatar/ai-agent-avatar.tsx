'use client';

import { forwardRef, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';
import { BotGlyph } from '../../../internal/icons.js';
import type { AiAgentRef } from '../ai-conversation/index.js';

/** What an agent is doing now. `waiting` means it needs a person (an approval, an answer). */
export type AiAgentState = 'idle' | 'working' | 'waiting' | 'error';

/** Preset sizes, matching `Avatar`. */
export type AiAgentAvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

/** Every visible and assistive-technology string. English defaults. */
export interface AiAgentAvatarMessages {
  /** Read after the name so assistive technology never mistakes an agent for a person. */
  kind: string;
  /** State text, read in the accessible name and shown with `showState`. */
  states: Record<AiAgentState, string>;
  /** Builds the accessible name, e.g. "Research agent, AI agent, Working". */
  label: (name: string, kind: string, state: string | undefined) => string;
}

export const AI_AGENT_AVATAR_MESSAGES: AiAgentAvatarMessages = {
  kind: 'AI agent',
  states: { idle: 'Idle', working: 'Working', waiting: 'Waiting for you', error: 'Error' },
  label: (name, kind, state) => [name, kind, state].filter(Boolean).join(', '),
};

/** Merges a partial `messages` prop with the English defaults. */
export function resolveAiAgentAvatarMessages(
  messages?: AiAgentAvatarProps['messages'],
): AiAgentAvatarMessages {
  const { states, ...rest } = messages ?? {};
  return {
    ...AI_AGENT_AVATAR_MESSAGES,
    ...rest,
    states: { ...AI_AGENT_AVATAR_MESSAGES.states, ...states },
  };
}

/** Initials for the fallback, from the codename when there is one. */
export function aiAgentInitials(agent: AiAgentRef): string {
  const source = (agent.codename || agent.name).trim();
  const parts = source.split(/\s+/).filter(Boolean);
  const first = parts[0]?.charAt(0) ?? '';
  const last = parts.length > 1 ? (parts[parts.length - 1]?.charAt(0) ?? '') : '';
  return (first + last).toUpperCase();
}

export interface AiAgentAvatarProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  /** The agent: its `name` names the avatar, `avatar` is the image, `codename` feeds the initials. */
  agent: AiAgentRef;
  /**
   * What the agent is doing. `working` adds a moving ring (still under reduced
   * motion), `waiting` a dashed ring, `error` a danger ring. The state is
   * always in the accessible name too, never colour or motion alone. Default `idle`.
   */
  state?: AiAgentState | undefined;
  /** A preset, or a number in CSS pixels. Default `md` (32px). */
  size?: AiAgentAvatarSize | number | undefined;
  /** Show the state as visible text beside the avatar. Default false. */
  showState?: boolean | undefined;
  /**
   * Set when the agent's name is already visible next to the avatar: the
   * avatar is hidden from assistive technology except for a non-idle state,
   * which is still read. Default false.
   */
  decorative?: boolean | undefined;
  /** Replaces any of the English strings; `states` may be partial. */
  messages?:
    | (Partial<Omit<AiAgentAvatarMessages, 'states'>> & {
        states?: Partial<Record<AiAgentState, string>> | undefined;
      })
    | undefined;
}

/**
 * The picture of an AI agent. It is a rounded square with a Bot badge, so it
 * never reads as a person's round avatar, and it shows what the agent is
 * doing: a moving ring while it works, a dashed ring while it waits for
 * someone, a danger ring on error, always with the state in text as well.
 */
export const AiAgentAvatar = forwardRef<HTMLSpanElement, AiAgentAvatarProps>(function AiAgentAvatar(
  {
    agent,
    state = 'idle',
    size = 'md',
    showState = false,
    decorative = false,
    messages,
    className,
    style,
    ...rest
  },
  ref,
) {
  const m = resolveAiAgentAvatarMessages(messages);
  const [failedSrc, setFailedSrc] = useState<string | null>(null);
  const src = agent.avatar;
  const showImage = Boolean(src) && failedSrc !== src;
  const text = aiAgentInitials(agent);
  const stateText = m.states[state];
  const announcedState = state === 'idle' ? undefined : stateText;
  const numeric = typeof size === 'number';
  const vars = numeric ? { ['--mn-ai-agent-avatar-size' as string]: `${size}px` } : undefined;

  const picture = (
    <span
      className="mn-ai-agent-avatar__picture"
      {...(decorative
        ? { 'aria-hidden': true as const }
        : { role: 'img' as const, 'aria-label': m.label(agent.name, m.kind, announcedState) })}
    >
      <span className="mn-ai-agent-avatar__face">
        {showImage ? (
          <img
            className="mn-ai-agent-avatar__image"
            src={src}
            alt=""
            onError={() => setFailedSrc(src ?? null)}
          />
        ) : text ? (
          <span className="mn-ai-agent-avatar__initials">{text}</span>
        ) : (
          <BotGlyph className="mn-ai-agent-avatar__glyph" />
        )}
      </span>
      <span className="mn-ai-agent-avatar__badge">
        <BotGlyph />
      </span>
    </span>
  );

  return (
    <span
      ref={ref}
      {...rest}
      className={cx('mn-ai-agent-avatar', className)}
      data-state={state}
      data-size={numeric ? 'custom' : size}
      style={vars ? { ...style, ...vars } : style}
    >
      {picture}
      {showState ? (
        <span className="mn-ai-agent-avatar__state" aria-hidden={decorative ? undefined : true}>
          {stateText}
        </span>
      ) : decorative && announcedState ? (
        <span className="mn-visually-hidden">{announcedState}</span>
      ) : null}
    </span>
  );
});
