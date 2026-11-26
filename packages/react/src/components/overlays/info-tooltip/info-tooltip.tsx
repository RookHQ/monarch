'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { HelpGlyph, InfoGlyph } from '../../../internal/icons.js';
import { Popover, PopoverContent, PopoverTrigger } from '../popover/index.js';

export interface InfoTooltipProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'children' | 'content'
> {
  /** The explanation shown in the bubble. Keep it to a sentence or two. */
  children: ReactNode;
  /**
   * Accessible name of the "i" button. Name what it explains, e.g.
   * "About visibility score". Default "More information".
   */
  label?: string;
  /** `info` (default) shows an "i"; `help` shows a "?". Exposed as `data-icon`. */
  icon?: 'info' | 'help';
  /** Replaces the built-in glyph, e.g. a lucide icon. It is hidden from assistive technology. */
  iconNode?: ReactNode;
  side?: 'top' | 'right' | 'bottom' | 'left';
  align?: 'start' | 'center' | 'end';
  /** Controlled open state. */
  open?: boolean;
  /** Uncontrolled initial open state. */
  defaultOpen?: boolean;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Class name for the bubble. */
  contentClassName?: string;
}

/**
 * The small "i" next to a label that explains it. It is a toggletip, not a
 * hover tooltip: a real button that opens on click, tap, Enter or Space,
 * announces its text through a live region, and closes on Escape, an
 * outside click or a second press. Focus stays on the button, so it works
 * the same with a mouse, a keyboard, a screen reader and a touch screen.
 */
export const InfoTooltip = forwardRef<HTMLButtonElement, InfoTooltipProps>(function InfoTooltip(
  {
    children,
    label = 'More information',
    icon = 'info',
    iconNode,
    side = 'top',
    align = 'center',
    open: openProp,
    defaultOpen = false,
    onOpenChange,
    contentClassName,
    className,
    ...rest
  },
  ref,
) {
  const [openState, setOpenState] = useState(defaultOpen);
  const open = openProp ?? openState;
  const bubble = useRef<HTMLDivElement>(null);
  const [announcement, setAnnouncement] = useState('');

  const setOpen = (next: boolean) => {
    if (openProp === undefined) setOpenState(next);
    onOpenChange?.(next);
  };

  // Copy the bubble's text into a live region that is always mounted, so
  // screen readers announce it reliably when it opens (a region inserted
  // together with its content is often missed).
  useEffect(() => {
    if (!open) {
      setAnnouncement('');
      return;
    }
    const frame = requestAnimationFrame(() => {
      setAnnouncement(bubble.current?.textContent?.trim() ?? '');
    });
    return () => cancelAnimationFrame(frame);
  }, [open, children]);

  return (
    <span className={cx('mn-info-tooltip', className)} data-state={open ? 'open' : 'closed'}>
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <button
            ref={ref}
            type="button"
            aria-label={label}
            {...rest}
            className="mn-info-tooltip__trigger"
            data-icon={icon}
          >
            <span className="mn-info-tooltip__icon" aria-hidden>
              {iconNode ?? (icon === 'help' ? <HelpGlyph /> : <InfoGlyph />)}
            </span>
          </button>
        </PopoverTrigger>
        <PopoverContent
          ref={bubble}
          side={side}
          align={align}
          role="note"
          aria-label={label}
          className={cx('mn-info-tooltip__bubble', contentClassName)}
          onOpenAutoFocus={(event) => event.preventDefault()}
        >
          {children}
        </PopoverContent>
      </Popover>
      <span className="mn-visually-hidden" role="status">
        {announcement}
      </span>
    </span>
  );
});
