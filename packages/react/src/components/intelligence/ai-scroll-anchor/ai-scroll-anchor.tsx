'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type ForwardedRef,
  type HTMLAttributes,
  type ReactNode,
  type RefObject,
} from 'react';
import { cx } from '../../../internal/cx.js';

const useIsomorphicLayoutEffect = typeof window === 'undefined' ? useEffect : useLayoutEffect;

/** Distance from the bottom, in CSS pixels, that still counts as "at the bottom". */
export const AI_SCROLL_ANCHOR_THRESHOLD = 48;

export interface UseAiScrollAnchorOptions {
  /**
   * Any value that changes when the content changes, such as the message
   * count or the streaming text length. Each change re-pins the scroll
   * position in the same frame; the ResizeObserver catches everything else
   * (images loading, disclosures opening).
   */
  watch?: unknown | undefined;
  /** How close to the bottom, in CSS pixels, still counts as at the bottom. Default 48. */
  threshold?: number | undefined;
  /** Called when `atBottom` changes. */
  onAtBottomChange?: ((atBottom: boolean) => void) | undefined;
}

export interface UseAiScrollAnchorResult<
  S extends HTMLElement = HTMLDivElement,
  C extends HTMLElement = HTMLDivElement,
> {
  /** Attach to the scrolling element (the one with `overflow: auto`). */
  scrollRef: RefObject<S | null>;
  /** Attach to the element inside it that grows as content arrives. */
  contentRef: RefObject<C | null>;
  /** True while the scroll position is within `threshold` of the bottom. */
  atBottom: boolean;
  /**
   * Scrolls to the end and pins there again. `behavior` defaults to `smooth`
   * and becomes `auto` (instant) when the person prefers reduced motion.
   */
  scrollToBottom: (behavior?: ScrollBehavior) => void;
  /** Whether new content will keep the view at the bottom right now (read at call time). */
  isPinned: () => boolean;
}

function prefersReducedMotion(): boolean {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return false;
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function distanceFromBottom(el: HTMLElement): number {
  return el.scrollHeight - el.scrollTop - el.clientHeight;
}

function jumpTo(el: HTMLElement, top: number, behavior: ScrollBehavior) {
  if (typeof el.scrollTo === 'function') el.scrollTo({ top, behavior });
  else el.scrollTop = top;
}

/**
 * Keeps a scroll container at the bottom while its content grows, but only
 * while the person is already there: scrolling up releases the pin, scrolling
 * back down (or `scrollToBottom`) restores it. It never moves focus. Safe to
 * call during server rendering; it does nothing until mounted.
 */
export function useAiScrollAnchor<
  S extends HTMLElement = HTMLDivElement,
  C extends HTMLElement = HTMLDivElement,
>(options: UseAiScrollAnchorOptions = {}): UseAiScrollAnchorResult<S, C> {
  const { watch, threshold = AI_SCROLL_ANCHOR_THRESHOLD, onAtBottomChange } = options;
  const scrollRef = useRef<S | null>(null);
  const contentRef = useRef<C | null>(null);
  const [atBottom, setAtBottom] = useState(true);
  const pinned = useRef(true);
  const autoScrolling = useRef(false);
  const thresholdRef = useRef(threshold);
  thresholdRef.current = threshold;
  const onChangeRef = useRef(onAtBottomChange);
  onChangeRef.current = onAtBottomChange;
  const atBottomRef = useRef(true);

  const report = useCallback((next: boolean) => {
    if (atBottomRef.current === next) return;
    atBottomRef.current = next;
    setAtBottom(next);
    onChangeRef.current?.(next);
  }, []);

  /** Pins to the bottom if pinned, otherwise just re-measures. */
  const settle = useCallback(() => {
    const el = scrollRef.current;
    if (!el) return;
    if (pinned.current) {
      const top = el.scrollHeight - el.clientHeight;
      if (Math.abs(el.scrollTop - top) > 0.5) el.scrollTop = top;
      report(true);
    } else {
      report(distanceFromBottom(el) <= thresholdRef.current);
    }
  }, [report]);

  const scrollToBottom = useCallback(
    (behavior: ScrollBehavior = 'smooth') => {
      const el = scrollRef.current;
      pinned.current = true;
      report(true);
      if (!el) return;
      const resolved = behavior === 'smooth' && prefersReducedMotion() ? 'auto' : behavior;
      autoScrolling.current = resolved === 'smooth';
      jumpTo(el, el.scrollHeight, resolved);
    },
    [report],
  );

  // Scrolling: leaving the bottom (by more than the threshold) releases the
  // pin; reaching it restores the pin. While our own smooth scroll runs, the
  // in-between positions do not count, until the person scrolls themselves.
  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    const onScroll = () => {
      const near = distanceFromBottom(el) <= thresholdRef.current;
      if (autoScrolling.current) {
        if (near) autoScrolling.current = false;
      } else {
        pinned.current = near;
      }
      report(near || autoScrolling.current);
    };
    const takeOver = () => {
      autoScrolling.current = false;
    };
    el.addEventListener('scroll', onScroll, { passive: true });
    el.addEventListener('wheel', takeOver, { passive: true });
    el.addEventListener('touchstart', takeOver, { passive: true });
    el.addEventListener('keydown', takeOver);
    return () => {
      el.removeEventListener('scroll', onScroll);
      el.removeEventListener('wheel', takeOver);
      el.removeEventListener('touchstart', takeOver);
      el.removeEventListener('keydown', takeOver);
    };
  }, [report]);

  // Content or viewport size changes (streaming text, images, a mobile
  // keyboard opening).
  useEffect(() => {
    if (typeof ResizeObserver === 'undefined') return;
    const observer = new ResizeObserver(() => settle());
    if (contentRef.current) observer.observe(contentRef.current);
    if (scrollRef.current) observer.observe(scrollRef.current);
    return () => observer.disconnect();
  }, [settle]);

  // Same-frame pinning when the watched value changes, so the newest line
  // never flashes below the fold.
  useIsomorphicLayoutEffect(() => {
    settle();
  }, [watch, settle]);

  const isPinned = useCallback(() => pinned.current, []);

  return { scrollRef, contentRef, atBottom, scrollToBottom, isPinned };
}

export interface AiScrollAnchorRenderState {
  atBottom: boolean;
  scrollToBottom: (behavior?: ScrollBehavior) => void;
}

export interface AiScrollAnchorProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Re-pins in the same frame whenever this value changes. See `useAiScrollAnchor`. */
  watch?: unknown | undefined;
  /** How close to the bottom, in CSS pixels, still counts as at the bottom. Default 48. */
  threshold?: number | undefined;
  /** Called when the person reaches or leaves the bottom. */
  onAtBottomChange?: ((atBottom: boolean) => void) | undefined;
  /**
   * The scrolling content. A function receives `{ atBottom, scrollToBottom }`,
   * for example to render a "Jump to latest" button.
   */
  children?: (ReactNode | ((state: AiScrollAnchorRenderState) => ReactNode)) | undefined;
  /** Rendered after the content element, inside the scroller (for example a sticky button). */
  footer?: (ReactNode | ((state: AiScrollAnchorRenderState) => ReactNode)) | undefined;
}

function assignRef<T>(ref: ForwardedRef<T>, value: T | null) {
  if (typeof ref === 'function') ref(value);
  else if (ref) ref.current = value;
}

/**
 * A headless scroll container that stays at the bottom while its content
 * grows, only while the person is at the bottom. It ships no styles: give it
 * a height and `overflow: auto` (or use `useAiScrollAnchor` on your own
 * elements). `AiThread` uses the same hook.
 */
export const AiScrollAnchor = forwardRef<HTMLDivElement, AiScrollAnchorProps>(
  function AiScrollAnchor(
    { watch, threshold, onAtBottomChange, children, footer, className, ...rest },
    ref,
  ) {
    const anchor = useAiScrollAnchor({ watch, threshold, onAtBottomChange });
    const state = { atBottom: anchor.atBottom, scrollToBottom: anchor.scrollToBottom };
    const scrollRef = anchor.scrollRef;
    return (
      <div
        {...rest}
        ref={(node) => {
          scrollRef.current = node;
          assignRef(ref, node);
        }}
        className={cx('mn-ai-scroll-anchor', className)}
        data-at-bottom={anchor.atBottom || undefined}
      >
        <div ref={anchor.contentRef} className="mn-ai-scroll-anchor__content">
          {typeof children === 'function' ? children(state) : children}
        </div>
        {typeof footer === 'function' ? footer(state) : footer}
      </div>
    );
  },
);
