import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import type { AiAgentRef } from '../ai-conversation/index.js';
import { AiAgentAvatar, type AiAgentState } from '../ai-agent-avatar/index.js';

/** A tool the agent can call, as shown to people. */
export interface AiAgentTool {
  /** Tool name, e.g. "Search documents". */
  name: string;
  /** What the tool does, in plain words. */
  description?: ReactNode;
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiAgentCardMessages {
  /** Shown after the role, so the card never reads as a person's profile. */
  kind: string;
  /** Heading of the capabilities list. */
  capabilities: string;
  /** Heading of the tools list. */
  tools: string;
  /** Heading of the data-access list. */
  dataAccess: string;
  /** Label of the owner row. */
  owner: string;
  /** Compact variant: how many tools, e.g. "3 tools". */
  toolCount: (count: number) => string;
}

export const AI_AGENT_CARD_MESSAGES: AiAgentCardMessages = {
  kind: 'AI agent',
  capabilities: 'What it can do',
  tools: 'Tools',
  dataAccess: 'Data it can see',
  owner: 'Owner',
  toolCount: (n) => `${n} ${n === 1 ? 'tool' : 'tools'}`,
};

export interface AiAgentCardProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** The agent: name, role, codename and avatar image. */
  agent: AiAgentRef;
  /** One or two sentences on what the agent is for. */
  description?: ReactNode;
  /** Short capability phrases, shown as tags, e.g. "Drafts replies". */
  capabilities?: readonly string[] | undefined;
  /** Tools the agent can call, with what each does. */
  tools?: readonly AiAgentTool[] | undefined;
  /** What data the agent can read, e.g. "Documents in this space". Shown as a list. */
  dataAccess?: readonly string[] | undefined;
  /** Who owns or maintains the agent: a name, or an Avatar with a name. */
  owner?: ReactNode;
  /** What the agent is doing now, shown on its avatar. Default `idle`. */
  state?: AiAgentState | undefined;
  /**
   * `full` (default) shows every section. `compact` is a one-row summary
   * (avatar, name, role, description, capability tags and a tool count) for
   * lists and pickers.
   */
  variant?: 'full' | 'compact' | undefined;
  /** Heading level of the agent's name, or `none` for a plain line (inside a control). Default 3. */
  headingLevel?: 2 | 3 | 4 | 5 | 6 | 'none' | undefined;
  /** Root element. Use `div` when the card sits inside a control such as AiAgentPicker. Default `article`. */
  as?: 'article' | 'div' | undefined;
  /** Id for the name element, so a surrounding control can use it as its label. */
  nameId?: string | undefined;
  /** Id for the description element, so a surrounding control can use it as its description. */
  descriptionId?: string | undefined;
  /** Buttons or links at the end of the card, e.g. "Start a chat". */
  actions?: ReactNode;
  /** Replaces any of the English strings. */
  messages?: Partial<AiAgentCardMessages> | undefined;
}

/**
 * What an AI agent is and what it may do: its name and role, a description,
 * capabilities, the tools it can call, the data it can see and who owns it.
 * The `compact` variant summarises it for lists and AiAgentPicker.
 */
export const AiAgentCard = forwardRef<HTMLElement, AiAgentCardProps>(function AiAgentCard(
  {
    agent,
    description,
    capabilities,
    tools,
    dataAccess,
    owner,
    state = 'idle',
    variant = 'full',
    headingLevel = 3,
    as = 'article',
    nameId: nameIdProp,
    descriptionId: descriptionIdProp,
    actions,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...AI_AGENT_CARD_MESSAGES, ...messages };
  const auto = useId();
  const nameId = nameIdProp ?? `${auto}-name`;
  const descriptionId = descriptionIdProp ?? `${auto}-description`;
  const compact = variant === 'compact';
  const Root = as;
  const Name = headingLevel === 'none' ? 'span' : (`h${headingLevel}` as const);
  const hasDescription = description !== undefined && description !== null && description !== '';

  const subtitle = [agent.role, m.kind].filter(Boolean).join(' · ');

  const capabilityList =
    capabilities && capabilities.length > 0 ? (
      <ul
        className="mn-ai-agent-card__tags"
        aria-label={compact ? m.capabilities : undefined}
        aria-labelledby={compact ? undefined : `${auto}-cap`}
      >
        {capabilities.map((c) => (
          <li key={c} className="mn-ai-agent-card__tag">
            {c}
          </li>
        ))}
      </ul>
    ) : null;

  return (
    <Root
      ref={ref as never}
      {...rest}
      className={cx('mn-ai-agent-card', className)}
      data-variant={variant}
      data-state={state}
      aria-labelledby={as === 'article' ? nameId : undefined}
    >
      <div className="mn-ai-agent-card__header">
        <AiAgentAvatar agent={agent} state={state} size={compact ? 'md' : 'lg'} decorative />
        <div className="mn-ai-agent-card__identity">
          <Name id={nameId} className="mn-ai-agent-card__name">
            {agent.name}
          </Name>
          <span className="mn-ai-agent-card__role">{subtitle}</span>
        </div>
        {compact && tools && tools.length > 0 ? (
          <span className="mn-ai-agent-card__count">{m.toolCount(tools.length)}</span>
        ) : null}
      </div>
      {hasDescription ? (
        <p id={descriptionId} className="mn-ai-agent-card__description">
          {description}
        </p>
      ) : null}
      {compact ? (
        capabilityList
      ) : (
        <>
          {capabilityList ? (
            <div className="mn-ai-agent-card__section">
              <span id={`${auto}-cap`} className="mn-ai-agent-card__section-title">
                {m.capabilities}
              </span>
              {capabilityList}
            </div>
          ) : null}
          {tools && tools.length > 0 ? (
            <div className="mn-ai-agent-card__section">
              <span id={`${auto}-tools`} className="mn-ai-agent-card__section-title">
                {m.tools}
              </span>
              <ul className="mn-ai-agent-card__tools" aria-labelledby={`${auto}-tools`}>
                {tools.map((t) => (
                  <li key={t.name} className="mn-ai-agent-card__tool">
                    <span className="mn-ai-agent-card__tool-name">{t.name}</span>
                    {t.description !== undefined && t.description !== null ? (
                      <span className="mn-ai-agent-card__tool-description">{t.description}</span>
                    ) : null}
                  </li>
                ))}
              </ul>
            </div>
          ) : null}
          {dataAccess && dataAccess.length > 0 ? (
            <div className="mn-ai-agent-card__section">
              <span id={`${auto}-data`} className="mn-ai-agent-card__section-title">
                {m.dataAccess}
              </span>
              <ul className="mn-ai-agent-card__data" aria-labelledby={`${auto}-data`}>
                {dataAccess.map((d) => (
                  <li key={d}>{d}</li>
                ))}
              </ul>
            </div>
          ) : null}
          {owner !== undefined && owner !== null ? (
            <dl className="mn-ai-agent-card__owner">
              <dt>{m.owner}</dt>
              <dd>{owner}</dd>
            </dl>
          ) : null}
          {actions ? <div className="mn-ai-agent-card__actions">{actions}</div> : null}
        </>
      )}
    </Root>
  );
});
