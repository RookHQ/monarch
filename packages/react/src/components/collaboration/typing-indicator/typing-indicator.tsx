import { forwardRef, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

export interface TypingPerson {
  /** Stable id, used as the React key. Default: the name. */
  id?: string | undefined;
  /** Display name as it should be read, e.g. a first name. */
  name: string;
}

export interface TypingIndicatorMessages {
  /** One person. Default "{name} is typing…". */
  one: (name: string) => string;
  /** Two people. Default "{first} and {second} are typing…". */
  two: (first: string, second: string) => string;
  /** Three or more people. Default "Several people are typing…". */
  several: (count: number, names: readonly string[]) => string;
}

const DEFAULT_MESSAGES: TypingIndicatorMessages = {
  one: (name) => `${name} is typing…`,
  two: (first, second) => `${first} and ${second} are typing…`,
  several: () => 'Several people are typing…',
};

/** The sentence a TypingIndicator shows for these people, or an empty string for nobody. */
export function formatTyping(
  people: readonly TypingPerson[],
  messages: Partial<TypingIndicatorMessages> = {},
): string {
  const m = { ...DEFAULT_MESSAGES, ...messages };
  const names = people.map((p) => p.name);
  const [first = '', second = ''] = names;
  if (names.length === 0) return '';
  if (names.length === 1) return m.one(first);
  if (names.length === 2) return m.two(first, second);
  return m.several(names.length, names);
}

export interface TypingIndicatorProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /**
   * Who is typing now, newest last. An empty list shows nothing but keeps the
   * live region mounted, so the next change is announced. Leave the current
   * user out.
   */
  people: readonly TypingPerson[];
  /**
   * Keep the line's height when nobody is typing, so the conversation above
   * does not jump each time someone starts or stops. Default `true`.
   */
  reserveSpace?: boolean;
  /**
   * Announce changes politely to screen readers (default). Pass `false` when
   * the product announces typing itself or the surface is very busy.
   */
  announce?: boolean;
  /** Translated text. Unset keys keep their English defaults. */
  messages?: Partial<TypingIndicatorMessages>;
}

/**
 * “Ana is typing…” under a conversation, with three bouncing dots. Takes the
 * list of people from the product's realtime layer and words it for one, two
 * or several people. The dots are decorative and stand still under reduced
 * motion.
 */
export const TypingIndicator = forwardRef<HTMLDivElement, TypingIndicatorProps>(
  function TypingIndicator(
    { people, reserveSpace = true, announce = true, messages, className, ...rest },
    ref,
  ) {
    const text = formatTyping(people, messages);
    const active = text !== '';
    return (
      <div
        ref={ref}
        {...(announce ? { role: 'status', 'aria-live': 'polite' as const } : {})}
        {...rest}
        className={cx('mn-typing-indicator', className)}
        data-active={active ? '' : undefined}
        data-reserve-space={reserveSpace ? '' : undefined}
      >
        {active ? (
          <>
            <span className="mn-typing-indicator__dots" aria-hidden>
              <span className="mn-typing-indicator__dot" />
              <span className="mn-typing-indicator__dot" />
              <span className="mn-typing-indicator__dot" />
            </span>
            <span className="mn-typing-indicator__text">{text}</span>
          </>
        ) : null}
      </div>
    );
  },
);
