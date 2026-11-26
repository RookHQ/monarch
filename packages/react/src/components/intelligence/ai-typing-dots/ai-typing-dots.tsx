'use client';

import { forwardRef, useEffect, useState, type HTMLAttributes } from 'react';
import { cx } from '../../../internal/cx.js';

export type AiTypingDotsSize = 'sm' | 'md';

export interface AiTypingDotsProps extends HTMLAttributes<HTMLSpanElement> {
  /** Visually hidden text for assistive technology. Default "Assistant is typing". */
  label?: string;
  /** `sm` for docks and dense rows, `md` (default) for a message bubble. */
  size?: AiTypingDotsSize;
  /**
   * Announce `label` politely when the dots appear (default). Pass `false`
   * when something else already announces the state; the label is then
   * plain hidden text.
   */
  announce?: boolean;
}

/**
 * A compact three-dot "assistant is typing" indicator for chat docks, list
 * rows and other small surfaces. The dots are still under reduced motion and
 * in forced colours.
 */
export const AiTypingDots = forwardRef<HTMLSpanElement, AiTypingDotsProps>(function AiTypingDots(
  { label = 'Assistant is typing', size = 'md', announce = true, className, ...rest },
  ref,
) {
  // Filled after mount: a live region that already holds text when it is
  // inserted is not reliably announced.
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  return (
    <span ref={ref} {...rest} className={cx('mn-ai-typing-dots', className)} data-size={size}>
      <span className="mn-ai-typing-dots__dot" aria-hidden />
      <span className="mn-ai-typing-dots__dot" aria-hidden />
      <span className="mn-ai-typing-dots__dot" aria-hidden />
      {announce ? (
        <span role="status" className="mn-visually-hidden">
          {mounted ? label : ''}
        </span>
      ) : (
        <span className="mn-visually-hidden">{label}</span>
      )}
    </span>
  );
});
