import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { BotGlyph, CheckGlyph } from '../../../internal/icons.js';
import { isSafeImageSrc } from '../../../internal/sanitize.js';
import type { AiAgentRef } from '../ai-conversation/index.js';

export interface AiWelcomeMessages {
  /** Heading of the capabilities list (visually hidden). */
  capabilities: string;
  /** Visually hidden prefix before the agent's name. */
  agentPrefix: string;
}

export const AI_WELCOME_MESSAGES: AiWelcomeMessages = {
  capabilities: 'What I can help with',
  agentPrefix: 'AI assistant:',
};

export interface AiWelcomeProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The assistant answering in this conversation: shown with the Bot mark, its name and role. */
  agent?: AiAgentRef | undefined;
  /** The greeting, e.g. "How can I help?". Becomes the region's name. */
  title: ReactNode;
  /** One or two sentences on what the assistant can see or do. */
  description?: ReactNode | undefined;
  /** Short capability statements, shown as a checklist. */
  capabilities?: readonly string[] | undefined;
  /** Starting points, usually AiStarterPrompts. */
  starters?: ReactNode | undefined;
  /** Content under the starters, e.g. AiDisclaimer. */
  footer?: ReactNode | undefined;
  /** Heading level of the title. Default 2. */
  headingLevel?: 1 | 2 | 3 | 4 | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiWelcomeMessages> | undefined;
}

/**
 * The empty state of a new conversation: who the assistant is, a greeting,
 * what it can help with and where to start. Pass AiStarterPrompts as
 * `starters`, and use it as AiThread's `empty` content.
 */
export const AiWelcome = forwardRef<HTMLElement, AiWelcomeProps>(function AiWelcome(
  {
    agent,
    title,
    description,
    capabilities,
    starters,
    footer,
    headingLevel = 2,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...AI_WELCOME_MESSAGES, ...messagesProp };
  const baseId = useId();
  const headingId = `${baseId}-title`;
  const capabilitiesId = `${baseId}-capabilities`;
  const Heading = `h${headingLevel}` as const;
  const avatar = agent?.avatar && isSafeImageSrc(agent.avatar) ? agent.avatar : undefined;

  return (
    <section
      ref={ref}
      aria-labelledby={headingId}
      {...rest}
      className={cx('mn-ai-welcome', className)}
    >
      <div className="mn-ai-welcome__mark" aria-hidden>
        {avatar ? (
          <img className="mn-ai-welcome__avatar" src={avatar} alt="" />
        ) : (
          <BotGlyph className="mn-ai-welcome__glyph" />
        )}
      </div>
      {agent ? (
        <p className="mn-ai-welcome__agent">
          <span className="mn-visually-hidden">{m.agentPrefix} </span>
          <span className="mn-ai-welcome__agent-name">{agent.name}</span>
          {agent.role ? <span className="mn-ai-welcome__agent-role">{agent.role}</span> : null}
        </p>
      ) : null}
      <Heading id={headingId} className="mn-ai-welcome__title">
        {title}
      </Heading>
      {description ? <div className="mn-ai-welcome__description">{description}</div> : null}
      {capabilities && capabilities.length > 0 ? (
        <>
          <p id={capabilitiesId} className="mn-visually-hidden">
            {m.capabilities}
          </p>
          <ul className="mn-ai-welcome__capabilities" aria-labelledby={capabilitiesId}>
            {capabilities.map((capability, index) => (
              <li key={index} className="mn-ai-welcome__capability">
                <CheckGlyph aria-hidden className="mn-ai-welcome__capability-icon" />
                {capability}
              </li>
            ))}
          </ul>
        </>
      ) : null}
      {starters ? <div className="mn-ai-welcome__starters">{starters}</div> : null}
      {footer ? <div className="mn-ai-welcome__footer">{footer}</div> : null}
    </section>
  );
});
