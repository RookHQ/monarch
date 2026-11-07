'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type CSSProperties,
  type ForwardedRef,
  type MouseEvent as ReactMouseEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, BotGlyph } from '../../../internal/icons.js';

/** Every string AiLauncher shows or announces. English defaults. */
export interface AiLauncherMessages {
  /** The button's accessible name (and visible text with `showLabel`). */
  label: string;
  /** Added to the name for `badge="unread"`. */
  unread: string;
  /** Added to the name for `badge="working"`. */
  working: string;
  /** Added to the name for a numeric badge. */
  count: (count: number) => string;
}

const DEFAULT_MESSAGES: AiLauncherMessages = {
  label: 'Assistant',
  unread: 'New messages',
  working: 'Working',
  count: (n) => (n === 1 ? '1 unread message' : `${n} unread messages`),
};

/** A parsed keyboard shortcut. `mod` is ⌘ on Apple devices and Ctrl elsewhere. */
export interface AiLauncherShortcut {
  key: string;
  mod: boolean;
  ctrl: boolean;
  meta: boolean;
  alt: boolean;
  shift: boolean;
}

/**
 * Parses a shortcut such as `mod+j`, `ctrl+shift+k` or `alt+a` (case and
 * spaces ignored). `mod` means ⌘ on Apple devices and Ctrl elsewhere.
 */
export function parseAiShortcut(shortcut: string): AiLauncherShortcut {
  const parts = shortcut
    .toLowerCase()
    .split('+')
    .map((p) => p.trim())
    .filter(Boolean);
  const key = parts.pop() ?? '';
  const has = (...names: string[]) => parts.some((p) => names.includes(p));
  return {
    key,
    mod: has('mod'),
    ctrl: has('ctrl', 'control'),
    meta: has('meta', 'cmd', 'command'),
    alt: has('alt', 'option'),
    shift: has('shift'),
  };
}

const isApplePlatform = () =>
  typeof navigator !== 'undefined' && /Mac|iP(hone|ad|od)/.test(navigator.platform);

/** Whether a keyboard event matches a parsed shortcut on this platform. */
export function matchesAiShortcut(
  event: Pick<KeyboardEvent, 'key' | 'ctrlKey' | 'metaKey' | 'altKey' | 'shiftKey'>,
  shortcut: AiLauncherShortcut,
  apple = isApplePlatform(),
): boolean {
  const wantMeta = shortcut.meta || (shortcut.mod && apple);
  const wantCtrl = shortcut.ctrl || (shortcut.mod && !apple);
  return (
    event.key.toLowerCase() === shortcut.key &&
    event.metaKey === wantMeta &&
    event.ctrlKey === wantCtrl &&
    event.altKey === shortcut.alt &&
    event.shiftKey === shortcut.shift
  );
}

/** The shortcut as `aria-keyshortcuts` wants it, e.g. `Meta+J` or `Control+J`. */
export function ariaKeyShortcut(shortcut: AiLauncherShortcut, apple: boolean): string {
  const out: string[] = [];
  if (shortcut.ctrl || (shortcut.mod && !apple)) out.push('Control');
  if (shortcut.meta || (shortcut.mod && apple)) out.push('Meta');
  if (shortcut.alt) out.push('Alt');
  if (shortcut.shift) out.push('Shift');
  out.push(shortcut.key.length === 1 ? shortcut.key.toUpperCase() : shortcut.key);
  return out.join('+');
}

/** The shortcut as people read it, e.g. `⌘J` or `Ctrl+J`. */
function displayShortcut(shortcut: AiLauncherShortcut, apple: boolean): string {
  const key = shortcut.key.length === 1 ? shortcut.key.toUpperCase() : shortcut.key;
  if (apple) {
    return (
      (shortcut.ctrl ? '⌃' : '') +
      (shortcut.alt ? '⌥' : '') +
      (shortcut.shift ? '⇧' : '') +
      (shortcut.meta || shortcut.mod ? '⌘' : '') +
      key
    );
  }
  const out: string[] = [];
  if (shortcut.ctrl || shortcut.mod) out.push('Ctrl');
  if (shortcut.meta) out.push('Win');
  if (shortcut.alt) out.push('Alt');
  if (shortcut.shift) out.push('Shift');
  out.push(key);
  return out.join('+');
}

function isEditable(target: EventTarget | null): boolean {
  if (typeof Element === 'undefined' || !(target instanceof Element)) return false;
  if (target instanceof HTMLElement && target.isContentEditable) return true;
  return (
    target.closest('input, textarea, select, [contenteditable=""], [contenteditable="true"]') !==
    null
  );
}

export type AiLauncherBadge = 'unread' | 'working' | number;

/** Where the launcher sits. `inline` renders it in the flow (a rail, a toolbar). */
export type AiLauncherPosition = 'bottom-end' | 'bottom-start' | 'inline';

export interface AiLauncherProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'children' | 'type'
> {
  /** Whether the assistant it controls is open. Sets `aria-expanded`. */
  open: boolean;
  /** Called with the next open state by a click and by the shortcut. */
  onOpenChange: (open: boolean) => void;
  /**
   * Global keyboard shortcut that toggles `open`, e.g. `mod+j` (the default:
   * ⌘J on Apple devices, Ctrl+J elsewhere). `false` turns it off. Mount one
   * launcher with a shortcut per page.
   */
  shortcut?: string | false | undefined;
  /**
   * Let the shortcut work while focus is in a text field or editable region.
   * Default false, so typing is never interrupted.
   */
  shortcutInEditable?: boolean | undefined;
  /** `unread` (a dot), `working` (a moving ring, static under reduced motion) or a count. Included in the accessible name. */
  badge?: AiLauncherBadge | undefined;
  /** `bottom-end` (default) and `bottom-start` float over the page; `inline` sits in the layout. */
  position?: AiLauncherPosition | undefined;
  /**
   * CSS selector of elements the floating launcher must not cover, such as a
   * chat dock. While a matching element is visible at the same corner, the
   * launcher moves up above it. Ignored for `position="inline"`.
   */
  avoidSelector?: string | undefined;
  /** Space, in CSS pixels, kept between the launcher and an avoided element. Default 12. */
  avoidGap?: number | undefined;
  /** Show the label (and the shortcut) as text next to the glyph. Default false, except for `inline`. */
  showLabel?: boolean | undefined;
  /** Replaces the Bot glyph. */
  icon?: ReactNode | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiLauncherMessages> | undefined;
}

function setRef<T>(ref: ForwardedRef<T>, value: T | null) {
  if (typeof ref === 'function') ref(value);
  else if (ref) ref.current = value;
}

/**
 * Moves a floating launcher above any visible element matching `selector`
 * at the same corner. Returns the upward offset in CSS pixels.
 */
function useAvoidOffset(
  button: { current: HTMLButtonElement | null },
  selector: string | undefined,
  gap: number,
  enabled: boolean,
): number {
  const [offset, setOffset] = useState(0);
  const offsetRef = useRef(0);

  useEffect(() => {
    if (!enabled || !selector || typeof window === 'undefined') {
      offsetRef.current = 0;
      setOffset(0);
      return;
    }
    let frame = 0;
    let observed: Element[] = [];
    const resize =
      typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(() => schedule());

    const measure = () => {
      frame = 0;
      const el = button.current;
      if (!el) return;
      const r = el.getBoundingClientRect();
      // Where the launcher would be with no offset.
      const top = r.top + offsetRef.current;
      const bottom = r.bottom + offsetRef.current;
      let next = 0;
      let targets: Element[] = [];
      try {
        targets = Array.from(document.querySelectorAll(selector));
      } catch {
        targets = [];
      }
      for (const t of targets) {
        if (t === el || t.contains(el)) continue;
        const b = t.getBoundingClientRect();
        if (b.width === 0 || b.height === 0) continue;
        const style = getComputedStyle(t);
        if (style.visibility === 'hidden' || style.display === 'none') continue;
        const overlapsX = b.left < r.right && b.right > r.left;
        const overlapsY = b.top < bottom && b.bottom > top;
        if (overlapsX && overlapsY) next = Math.max(next, Math.ceil(bottom - b.top + gap));
      }
      if (next !== offsetRef.current) {
        offsetRef.current = next;
        setOffset(next);
      }
      if (resize) {
        const same =
          targets.length === observed.length && targets.every((t, i) => observed[i] === t);
        if (!same) {
          resize.disconnect();
          targets.forEach((t) => resize.observe(t));
          observed = targets;
        }
      }
    };
    function schedule() {
      if (!frame) frame = requestAnimationFrame(measure);
    }

    measure();
    const mutation =
      typeof MutationObserver === 'undefined' ? null : new MutationObserver(() => schedule());
    mutation?.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['class', 'style', 'hidden', 'open'],
    });
    window.addEventListener('resize', schedule);
    window.addEventListener('scroll', schedule, { passive: true, capture: true });
    return () => {
      if (frame) cancelAnimationFrame(frame);
      resize?.disconnect();
      mutation?.disconnect();
      window.removeEventListener('resize', schedule);
      window.removeEventListener('scroll', schedule, { capture: true });
    };
  }, [button, selector, gap, enabled]);

  return offset;
}

/**
 * The button that opens and closes an AI assistant: a floating button in a
 * page corner (or inline in a rail), a global ⌘/Ctrl+J shortcut that is
 * ignored while typing, an unread or working badge, and collision avoidance
 * so it moves above a chat dock or other floating element in the same corner.
 * Pair it with `AiAssistantPanel`; the launcher owns the shortcut.
 */
export const AiLauncher = forwardRef<HTMLButtonElement, AiLauncherProps>(function AiLauncher(
  {
    open,
    onOpenChange,
    shortcut = 'mod+j',
    shortcutInEditable = false,
    badge,
    position = 'bottom-end',
    avoidSelector,
    avoidGap = 12,
    showLabel,
    icon,
    messages: messagesProp,
    className,
    style,
    onClick,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const buttonRef = useRef<HTMLButtonElement | null>(null);
  const floating = position !== 'inline';
  const labelShown = showLabel ?? !floating;

  // Platform is read after mount so server and client render the same markup.
  const [apple, setApple] = useState(false);
  useEffect(() => setApple(isApplePlatform()), []);

  const parsed = shortcut ? parseAiShortcut(shortcut) : null;
  const openRef = useRef(open);
  openRef.current = open;
  const toggleRef = useRef(onOpenChange);
  toggleRef.current = onOpenChange;

  const shortcutKey = shortcut || '';
  useEffect(() => {
    if (!shortcutKey || typeof window === 'undefined') return;
    const sc = parseAiShortcut(shortcutKey);
    const onKey = (e: KeyboardEvent) => {
      if (e.defaultPrevented || e.repeat || e.isComposing) return;
      if (!matchesAiShortcut(e, sc)) return;
      if (!shortcutInEditable && isEditable(e.target)) return;
      e.preventDefault();
      toggleRef.current(!openRef.current);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [shortcutKey, shortcutInEditable]);

  const offset = useAvoidOffset(buttonRef, avoidSelector, avoidGap, floating);

  const handleClick = useCallback(
    (e: ReactMouseEvent<HTMLButtonElement>) => {
      onClick?.(e);
      if (!e.defaultPrevented) onOpenChange(!open);
    },
    [onClick, onOpenChange, open],
  );

  let badgeText: string | null = null;
  let badgeVisual: ReactNode = null;
  if (badge === 'unread') {
    badgeText = m.unread;
    badgeVisual = <span className="mn-ai-launcher__dot" aria-hidden />;
  } else if (badge === 'working') {
    badgeText = m.working;
    badgeVisual = <span className="mn-ai-launcher__ring" aria-hidden />;
  } else if (typeof badge === 'number' && badge > 0) {
    badgeText = m.count(badge);
    badgeVisual = (
      <span className="mn-ai-launcher__count" aria-hidden>
        {badge > 99 ? '99+' : badge}
      </span>
    );
  }

  return (
    <button
      ref={(node) => {
        buttonRef.current = node;
        setRef(ref, node);
      }}
      type="button"
      aria-expanded={open}
      aria-keyshortcuts={parsed ? ariaKeyShortcut(parsed, apple) : undefined}
      // The name starts with the visible label, then the badge (label in name).
      aria-label={badgeText ? `${m.label}, ${badgeText}` : m.label}
      {...rest}
      className={cx('mn-ai-launcher', className)}
      data-position={position}
      data-open={open || undefined}
      data-badge={badge === undefined ? undefined : typeof badge === 'number' ? 'count' : badge}
      data-avoiding={offset > 0 || undefined}
      data-label={labelShown || undefined}
      style={
        offset > 0
          ? ({ ...style, '--mn-ai-launcher-avoid': `${offset}px` } as CSSProperties)
          : style
      }
      onClick={handleClick}
    >
      <span className="mn-ai-launcher__icon" aria-hidden>
        {open && floating ? <CloseGlyph /> : (icon ?? <BotGlyph />)}
      </span>
      {labelShown ? <span className="mn-ai-launcher__label">{m.label}</span> : null}
      {labelShown && parsed ? (
        <kbd className="mn-ai-launcher__shortcut" aria-hidden>
          {displayShortcut(parsed, apple)}
        </kbd>
      ) : null}
      {badgeVisual}
    </button>
  );
});
