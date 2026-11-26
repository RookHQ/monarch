'use client';

import {
  createContext,
  forwardRef,
  useCallback,
  useContext,
  useEffect,
  useId,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type CSSProperties,
  type HTMLAttributes,
  type MouseEvent,
} from 'react';
import { useIsomorphicLayoutEffect } from '../../../internal/layout-effect.js';
import { Slot } from '@radix-ui/react-slot';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import { useControllable } from '../../../internal/use-controllable.js';

// useIsomorphicLayoutEffect warns during server rendering; the effect only measures the DOM.
const useIsoLayoutEffect = typeof window === 'undefined' ? useEffect : useIsomorphicLayoutEffect;

interface CollapsibleContextValue {
  open: boolean;
  disabled: boolean;
  /** False on the first render, so content that starts open never animates in. */
  animate: boolean;
  contentId: string;
  toggle: () => void;
}

const CollapsibleContext = createContext<CollapsibleContextValue | null>(null);

function useCollapsible(part: string): CollapsibleContextValue {
  const ctx = useContext(CollapsibleContext);
  if (!ctx) throw new Error(`${part} must be used inside <Collapsible>.`);
  return ctx;
}

export interface CollapsibleProps extends HTMLAttributes<HTMLDivElement> {
  /** Whether the content is shown (controlled). Pair with `onOpenChange`. */
  open?: boolean | undefined;
  /** Whether the content starts shown (uncontrolled). Default `false`. */
  defaultOpen?: boolean | undefined;
  /** Called with the new state when the trigger is activated. */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** The trigger cannot be activated. The content keeps its current state. */
  disabled?: boolean | undefined;
}

/**
 * One section that shows and hides its content: a "Show advanced settings"
 * toggle, a "Details" block, an expandable row. Follows the WAI-ARIA
 * disclosure pattern; the content animates its height and stays still when
 * the user prefers reduced motion. For a set of sections, use Accordion.
 */
export const Collapsible = forwardRef<HTMLDivElement, CollapsibleProps>(function Collapsible(
  { open: openProp, defaultOpen = false, onOpenChange, disabled = false, className, ...rest },
  ref,
) {
  const [open, setOpen] = useControllable(openProp, defaultOpen, onOpenChange);
  // The first render never animates; every change after mount does.
  const [animate, setAnimate] = useState(false);
  useEffect(() => setAnimate(true), []);
  const contentId = `${useId()}-content`;
  const toggle = useCallback(() => {
    if (!disabled) setOpen(!open);
  }, [disabled, open, setOpen]);
  return (
    <CollapsibleContext.Provider value={{ open, disabled, animate, contentId, toggle }}>
      <div
        ref={ref}
        {...rest}
        className={cx('mn-collapsible', className)}
        data-state={open ? 'open' : 'closed'}
        data-disabled={disabled || undefined}
      />
    </CollapsibleContext.Provider>
  );
});

export interface CollapsibleTriggerProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** Render the child element (for example a Monarch Button) as the trigger. */
  asChild?: boolean | undefined;
  /** Show a chevron after the label that turns when open. Decorative. Default `false`. */
  chevron?: boolean | undefined;
}

/** The button that shows and hides the content. Its label should name the content. */
export const CollapsibleTrigger = forwardRef<HTMLButtonElement, CollapsibleTriggerProps>(
  function CollapsibleTrigger(
    { asChild = false, chevron = false, onClick, className, children, disabled, ...rest },
    ref,
  ) {
    const ctx = useCollapsible('CollapsibleTrigger');
    const isDisabled = ctx.disabled || disabled;
    const props = {
      ref,
      ...rest,
      'aria-expanded': ctx.open,
      'aria-controls': ctx.contentId,
      'data-state': ctx.open ? 'open' : 'closed',
      'data-disabled': isDisabled || undefined,
      disabled: isDisabled,
      onClick(event: MouseEvent<HTMLButtonElement>) {
        onClick?.(event);
        if (!event.defaultPrevented) ctx.toggle();
      },
    };
    if (asChild) {
      return (
        <Slot {...props} className={cx('mn-collapsible__trigger', className)}>
          {children}
        </Slot>
      );
    }
    return (
      <button
        type="button"
        {...props}
        className={cx('mn-collapsible__trigger', className)}
        data-plain=""
      >
        {children}
        {chevron ? <ChevronDownGlyph className="mn-collapsible__chevron" /> : null}
      </button>
    );
  },
);

export type CollapsibleContentProps = HTMLAttributes<HTMLDivElement>;

/**
 * The content that is shown and hidden. Closed content has the `hidden`
 * attribute, so it is out of the tab order and the accessibility tree; its
 * children stay mounted, so form state inside survives closing.
 */
export const CollapsibleContent = forwardRef<HTMLDivElement, CollapsibleContentProps>(
  function CollapsibleContent({ className, style, children, ...rest }, forwardedRef) {
    const ctx = useCollapsible('CollapsibleContent');
    const inner = useRef<HTMLDivElement | null>(null);
    // `present` lags behind `open` while the closing animation runs.
    const [present, setPresent] = useState(ctx.open);
    const [height, setHeight] = useState<number | null>(null);

    const setRef = useCallback(
      (node: HTMLDivElement | null) => {
        inner.current = node;
        if (typeof forwardedRef === 'function') forwardedRef(node);
        else if (forwardedRef) forwardedRef.current = node;
      },
      [forwardedRef],
    );

    useIsoLayoutEffect(() => {
      if (ctx.open) setPresent(true);
    }, [ctx.open]);

    useIsoLayoutEffect(() => {
      const el = inner.current;
      if (!el || !present) return;
      setHeight(el.scrollHeight);
      if (ctx.open) return;
      // Closing: wait for the animation, or hide at once when there is none
      // (reduced motion, no stylesheet, first render).
      const name = ctx.animate ? getComputedStyle(el).animationName : 'none';
      if (!name || name === 'none') {
        setPresent(false);
        return;
      }
      const done = (event: AnimationEvent) => {
        if (event.target === el) setPresent(false);
      };
      el.addEventListener('animationend', done);
      el.addEventListener('animationcancel', done);
      return () => {
        el.removeEventListener('animationend', done);
        el.removeEventListener('animationcancel', done);
      };
    }, [ctx.open, present, ctx.animate]);

    return (
      <div
        ref={setRef}
        {...rest}
        id={ctx.contentId}
        hidden={!present}
        className={cx('mn-collapsible__content', className)}
        data-state={ctx.open ? 'open' : 'closed'}
        data-animate={ctx.animate || undefined}
        style={
          height === null ? style : ({ ...style, '--_height': `${height}px` } as CSSProperties)
        }
      >
        {children}
      </div>
    );
  },
);
