'use client';

import {
  forwardRef,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';

export interface AiFollowUpSuggestionsMessages {
  /** Accessible name of the toolbar. */
  label: string;
}

export const AI_FOLLOW_UP_SUGGESTIONS_MESSAGES: AiFollowUpSuggestionsMessages = {
  label: 'Suggested follow-ups',
};

export interface AiFollowUpSuggestionsProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onSelect'
> {
  /** Follow-up questions the person might ask next, as complete prompts. */
  suggestions: readonly string[];
  /** Called with the suggestion's text when one is chosen. */
  onSelect: (suggestion: string) => void;
  /** Disables every chip, e.g. while a reply is being generated. */
  disabled?: boolean | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiFollowUpSuggestionsMessages> | undefined;
}

function isRtl(el: HTMLElement): boolean {
  return typeof getComputedStyle === 'function' && getComputedStyle(el).direction === 'rtl';
}

/**
 * Chips with follow-up questions shown after a reply. They form one toolbar
 * tab stop: arrow keys move between chips (mirrored in right-to-left
 * layouts), Home and End jump to the ends, and Enter or Space chooses one.
 */
export const AiFollowUpSuggestions = forwardRef<HTMLDivElement, AiFollowUpSuggestionsProps>(
  function AiFollowUpSuggestions(
    {
      suggestions,
      onSelect,
      disabled = false,
      messages: messagesProp,
      className,
      onKeyDown,
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_FOLLOW_UP_SUGGESTIONS_MESSAGES, ...messagesProp };
    const [active, setActive] = useState(0);
    const buttons = useRef<(HTMLButtonElement | null)[]>([]);
    const count = suggestions.length;
    const current = Math.min(active, Math.max(0, count - 1));

    if (count === 0) return null;

    const move = (index: number) => {
      const next = (index + count) % count;
      setActive(next);
      buttons.current[next]?.focus();
    };

    const handleKeyDown = (event: ReactKeyboardEvent<HTMLDivElement>) => {
      onKeyDown?.(event);
      if (event.defaultPrevented || disabled) return;
      const rtl = isRtl(event.currentTarget);
      const forward = rtl ? 'ArrowLeft' : 'ArrowRight';
      const back = rtl ? 'ArrowRight' : 'ArrowLeft';
      switch (event.key) {
        case forward:
        case 'ArrowDown':
          move(current + 1);
          break;
        case back:
        case 'ArrowUp':
          move(current - 1);
          break;
        case 'Home':
          move(0);
          break;
        case 'End':
          move(count - 1);
          break;
        default:
          return;
      }
      event.preventDefault();
    };

    return (
      <div
        ref={ref}
        role="toolbar"
        aria-label={m.label}
        aria-orientation="horizontal"
        {...rest}
        className={cx('mn-ai-follow-up-suggestions', className)}
        onKeyDown={handleKeyDown}
      >
        {suggestions.map((suggestion, index) => (
          <button
            key={`${index}-${suggestion}`}
            ref={(el) => {
              buttons.current[index] = el;
            }}
            type="button"
            className="mn-ai-follow-up-suggestions__chip"
            tabIndex={index === current ? 0 : -1}
            disabled={disabled}
            onFocus={() => setActive(index)}
            onClick={() => onSelect(suggestion)}
          >
            <span className="mn-ai-follow-up-suggestions__text">{suggestion}</span>
          </button>
        ))}
      </div>
    );
  },
);
