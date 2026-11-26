'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { useControllable } from '../../../internal/use-controllable.js';
import { Tooltip, TooltipProvider } from '../../overlays/tooltip/index.js';

/** The words TruncatedText shows. Override them to translate. */
export interface TruncatedTextMessages {
  /** Label of the button that expands the text. Default "Show more". */
  showMore: string;
  /** Label of the button that collapses it again. Default "Show less". */
  showLess: string;
}

const DEFAULT_MESSAGES: TruncatedTextMessages = { showMore: 'Show more', showLess: 'Show less' };

export interface TruncatedTextProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The text. Inline elements (links, emphasis) are fine; the whole of it stays in the page. */
  children: ReactNode;
  /** How many lines to show before clamping. `1` ends the line with an ellipsis. Default 3. */
  lines?: number | undefined;
  /**
   * Show a "Show more" button when the text is clamped. Default `true`. Turn it
   * off only where the full text is one click away (a row that opens a record)
   * or `tooltip` is on.
   */
  expandable?: boolean | undefined;
  /**
   * While the text is clamped, show the full text in a tooltip on hover and
   * keyboard focus. The clamped text becomes focusable so keyboard users can
   * reach the tooltip. Use for short text such as a title cut to one line.
   */
  tooltip?: boolean | undefined;
  /** Whether the full text is shown (controlled). Pair with `onExpandedChange`. */
  expanded?: boolean | undefined;
  /** Whether the full text starts shown (uncontrolled). Default `false`. */
  defaultExpanded?: boolean | undefined;
  /** Called when the button expands or collapses the text. */
  onExpandedChange?: ((expanded: boolean) => void) | undefined;
  /** Words to show; merged over the English defaults. */
  messages?: Partial<TruncatedTextMessages> | undefined;
}

/**
 * Text clamped to a number of lines with a "Show more" button that appears
 * only when the text actually overflows. The clamp is visual: the full text
 * is always in the page, so screen readers, find-in-page and copy get all of it.
 */
export const TruncatedText = forwardRef<HTMLDivElement, TruncatedTextProps>(function TruncatedText(
  {
    children,
    lines = 3,
    expandable = true,
    tooltip = false,
    expanded: expandedProp,
    defaultExpanded = false,
    onExpandedChange,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const id = useId();
  const textRef = useRef<HTMLDivElement>(null);
  const [expanded, setExpanded] = useControllable(expandedProp, defaultExpanded, onExpandedChange);
  const [overflows, setOverflows] = useState(false);
  const [tipOpen, setTipOpen] = useState(false);
  const clampLines = Math.max(1, Math.floor(lines));

  const measure = useCallback(() => {
    const el = textRef.current;
    if (!el || expanded) return;
    setOverflows(el.scrollHeight - el.clientHeight > 1 || el.scrollWidth - el.clientWidth > 1);
  }, [expanded]);

  useEffect(() => {
    measure();
    const el = textRef.current;
    if (!el || typeof ResizeObserver === 'undefined') return;
    const observer = new ResizeObserver(measure);
    observer.observe(el);
    return () => observer.disconnect();
  }, [measure, children, clampLines]);

  const clamped = overflows && !expanded;
  const withTooltip = tooltip && clamped;
  const text = (
    <div
      ref={textRef}
      id={id}
      className="mn-truncated-text__text"
      data-expanded={expanded || undefined}
      data-single={clampLines === 1 || undefined}
      style={{ '--_lines': clampLines } as CSSProperties}
      // Focusable only so keyboard users can open the tooltip.
      tabIndex={withTooltip && !expandable ? 0 : undefined}
    >
      {children}
    </div>
  );

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-truncated-text', className)}
      data-clamped={clamped || undefined}
    >
      {tooltip ? (
        // Always wrapped when `tooltip` is on, so the measured element never remounts.
        <TooltipProvider>
          <Tooltip
            open={withTooltip && tipOpen}
            onOpenChange={setTipOpen}
            content={<span className="mn-truncated-text__tooltip">{children}</span>}
          >
            {text}
          </Tooltip>
        </TooltipProvider>
      ) : (
        text
      )}
      {expandable && (overflows || expanded) ? (
        <button
          type="button"
          className="mn-truncated-text__toggle"
          aria-expanded={expanded}
          aria-controls={id}
          onClick={() => setExpanded(!expanded)}
        >
          {expanded ? messages.showLess : messages.showMore}
        </button>
      ) : null}
    </div>
  );
});
