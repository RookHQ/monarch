import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';

/** One suggested way to start a conversation. */
export interface AiStarterPrompt {
  /** Stable id. */
  id: string;
  /** Short visible label, e.g. "Summarise a document". */
  title: string;
  /** The prompt that is sent (or placed in the input) when chosen. Shown under the title. */
  prompt: string;
  /** Decorative icon shown before the title. Never a sparkle. */
  icon?: ReactNode | undefined;
}

export interface AiStarterPromptsMessages {
  /** Accessible name of the list. */
  label: string;
}

export const AI_STARTER_PROMPTS_MESSAGES: AiStarterPromptsMessages = {
  label: 'Suggested prompts',
};

export interface AiStarterPromptsProps extends Omit<HTMLAttributes<HTMLUListElement>, 'onSelect'> {
  /** The starters, most useful first. Four to six work best. */
  prompts: readonly AiStarterPrompt[];
  /** Called with the prompt text (and the starter) when one is chosen. */
  onSelect: (prompt: string, starter: AiStarterPrompt) => void;
  /** `grid` (default) shows cards in columns; `list` shows full-width rows. */
  layout?: 'grid' | 'list' | undefined;
  /** Hide the prompt text under each title. Default false. */
  hidePrompt?: boolean | undefined;
  /** Disables every starter, e.g. while a reply is being generated. */
  disabled?: boolean | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiStarterPromptsMessages> | undefined;
}

/**
 * Suggested ways to start a conversation, as cards in a grid or rows in a
 * list. Each is a button named by its title and described by its prompt;
 * choosing one calls `onSelect` with the prompt text.
 */
export const AiStarterPrompts = forwardRef<HTMLUListElement, AiStarterPromptsProps>(
  function AiStarterPrompts(
    {
      prompts,
      onSelect,
      layout = 'grid',
      hidePrompt = false,
      disabled = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_STARTER_PROMPTS_MESSAGES, ...messagesProp };
    const baseId = useId();
    if (prompts.length === 0) return null;
    return (
      <ul
        ref={ref}
        aria-label={m.label}
        {...rest}
        className={cx('mn-ai-starter-prompts', className)}
        data-layout={layout}
      >
        {prompts.map((starter, index) => {
          const titleId = `${baseId}-${index}-title`;
          const promptId = `${baseId}-${index}-prompt`;
          return (
            <li key={starter.id} className="mn-ai-starter-prompts__item">
              <button
                type="button"
                className="mn-ai-starter-prompts__button"
                aria-labelledby={titleId}
                aria-describedby={hidePrompt ? undefined : promptId}
                disabled={disabled}
                onClick={() => onSelect(starter.prompt, starter)}
              >
                <span className="mn-ai-starter-prompts__head">
                  {starter.icon ? (
                    <span className="mn-ai-starter-prompts__icon" aria-hidden>
                      {starter.icon}
                    </span>
                  ) : null}
                  <span id={titleId} className="mn-ai-starter-prompts__title">
                    {starter.title}
                  </span>
                </span>
                {hidePrompt ? null : (
                  <span id={promptId} className="mn-ai-starter-prompts__prompt">
                    {starter.prompt}
                  </span>
                )}
              </button>
            </li>
          );
        })}
      </ul>
    );
  },
);
