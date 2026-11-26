'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useRef,
  type ElementType,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph, CloseGlyph } from '../../../internal/icons.js';
import { ownsOpenPopup } from '../../../internal/dismiss.js';
import { isSafeUrl } from '../../../internal/sanitize.js';

/** The words PeekPanel shows and speaks. Override them to translate. */
export interface PeekPanelMessages {
  /** Accessible name of the close button. Default "Close preview". */
  close: string;
  /** Accessible name of the previous-record button. Default "Previous". */
  previous: string;
  /** Accessible name of the next-record button. Default "Next". */
  next: string;
  /** Where the record sits in the list. Default "3 of 20". */
  position: (index: number, total: number) => string;
  /** Label of the link to the full record. Default "Open full record". */
  openFull: string;
}

const DEFAULT_MESSAGES: PeekPanelMessages = {
  close: 'Close preview',
  previous: 'Previous',
  next: 'Next',
  position: (index, total) => `${index} of ${total}`,
  openFull: 'Open full record',
};

export interface PeekPanelProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** Whether the panel is shown. The panel is controlled: the list decides what is previewed. */
  open: boolean;
  /** Called with `false` when the close button or Escape closes the panel. */
  onOpenChange: (open: boolean) => void;
  /** The record's name, shown as the panel heading and used as its accessible name. */
  title: ReactNode;
  /** A line under the title, e.g. the record's id or status. */
  description?: ReactNode;
  /** The preview. */
  children?: ReactNode;
  /** Extra header actions (an IconButton or a menu) before the close button. */
  actions?: ReactNode;
  /** Actions at the bottom of the panel that stay visible while the body scrolls. */
  footer?: ReactNode;
  /** Show the previous record. Giving it adds the previous/next buttons. */
  onPrevious?: (() => void) | undefined;
  /** Show the next record. Giving it adds the previous/next buttons. */
  onNext?: (() => void) | undefined;
  /** Whether there is a previous record. Default `true` when `onPrevious` is given. */
  hasPrevious?: boolean | undefined;
  /** Whether there is a next record. Default `true` when `onNext` is given. */
  hasNext?: boolean | undefined;
  /** Where the record sits in the list (1-based), shown as "3 of 20". */
  position?: { index: number; total: number } | undefined;
  /**
   * Address of the full record page. Adds an "Open full record" link in the
   * header. Must be a relative or http(s) URL; anything else is not linked.
   */
  href?: string | undefined;
  /** Component used for `href`, e.g. a router link. Default `a`. */
  linkComponent?: ElementType | undefined;
  /**
   * `overlay` (default) floats over the page at the inline end, leaving the
   * list usable; `inline` is an ordinary block for a split layout you build.
   */
  placement?: 'overlay' | 'inline' | undefined;
  /** Width of the overlay panel: 24, 32 (default) or 44rem. Phones always get the full screen. */
  size?: 'sm' | 'md' | 'lg' | undefined;
  /**
   * Move focus to the panel when it opens. Default `true`. Turn it off when
   * people move through the list with the keyboard and the panel follows.
   */
  focusOnOpen?: boolean | undefined;
  /**
   * J/K and Alt+Down/Alt+Up move to the next and previous record while focus
   * is in the panel (J and K not while typing). Default `true`.
   */
  shortcuts?: boolean | undefined;
  /** Words to show and speak; merged over the English defaults. */
  messages?: Partial<PeekPanelMessages> | undefined;
}

function isEditable(target: EventTarget | null) {
  return (
    target instanceof HTMLElement &&
    (target.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(target.tagName))
  );
}

/**
 * A side panel that previews a record next to the list it came from: people
 * read it, step to the previous or next record, and close it without losing
 * their place. It is non-modal (no focus trap, no scroll lock); Escape closes
 * it and returns focus to where it was.
 */
/** True for controls that use Alt+ArrowDown/ArrowUp themselves to open or close their popup. */
function opensWithAltArrow(target: EventTarget | null) {
  return (
    target instanceof HTMLElement &&
    (target.tagName === 'SELECT' ||
      target.getAttribute('role') === 'combobox' ||
      target.hasAttribute('aria-haspopup'))
  );
}

export const PeekPanel = forwardRef<HTMLElement, PeekPanelProps>(function PeekPanel(
  {
    open,
    onOpenChange,
    title,
    description,
    children,
    actions,
    footer,
    onPrevious,
    onNext,
    hasPrevious,
    hasNext,
    position,
    href,
    linkComponent: LinkComponent = 'a',
    placement = 'overlay',
    size = 'md',
    focusOnOpen = true,
    shortcuts = true,
    messages: messagesProp,
    className,
    onKeyDown,
    ...rest
  },
  forwardedRef,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const titleId = useId();
  const descriptionId = useId();
  const panel = useRef<HTMLElement | null>(null);
  const returnTo = useRef<HTMLElement | null>(null);
  const restoreOnClose = useRef(false);

  const setRef = useCallback(
    (node: HTMLElement | null) => {
      panel.current = node;
      if (typeof forwardedRef === 'function') forwardedRef(node);
      else if (forwardedRef) forwardedRef.current = node;
    },
    [forwardedRef],
  );

  useEffect(() => {
    if (!open) return;
    const active = document.activeElement;
    returnTo.current = active instanceof HTMLElement && active !== document.body ? active : null;
    if (focusOnOpen) panel.current?.focus({ preventScroll: true });
    return () => {
      // Closed from inside (Escape or the close button): put focus back on the row.
      if (!restoreOnClose.current) return;
      restoreOnClose.current = false;
      const target = returnTo.current;
      if (target?.isConnected) target.focus({ preventScroll: true });
    };
    // Only on opening; record changes keep the original return target.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  if (!open) return null;

  const close = () => {
    restoreOnClose.current = true;
    onOpenChange(false);
  };
  const navigable = onPrevious !== undefined || onNext !== undefined;
  const canPrevious = onPrevious !== undefined && (hasPrevious ?? true);
  const canNext = onNext !== undefined && (hasNext ?? true);
  const safeHref = href !== undefined && isSafeUrl(href) ? href : undefined;

  const handleKeyDown = (event: KeyboardEvent<HTMLElement>) => {
    onKeyDown?.(event);
    if (event.defaultPrevented) return;
    if (event.key === 'Escape') {
      if (ownsOpenPopup(event.target)) return;
      event.preventDefault();
      close();
      return;
    }
    if (!shortcuts || event.ctrlKey || event.metaKey) return;
    const typing = isEditable(event.target);
    // Alt+Arrow opens a native select, a combobox and a menu button: leave it to them.
    const altArrow = event.altKey && !opensWithAltArrow(event.target);
    const next =
      (altArrow && event.key === 'ArrowDown') ||
      (!typing && !event.altKey && event.key.toLowerCase() === 'j');
    const previous =
      (altArrow && event.key === 'ArrowUp') ||
      (!typing && !event.altKey && event.key.toLowerCase() === 'k');
    if (next && canNext) {
      event.preventDefault();
      onNext?.();
    } else if (previous && canPrevious) {
      event.preventDefault();
      onPrevious?.();
    }
  };

  return (
    // The dialog container owns Escape and the record shortcuts (WAI-ARIA APG dialog).
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <section
      ref={setRef}
      role="dialog"
      aria-modal="false"
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      tabIndex={-1}
      {...rest}
      className={cx('mn-peek-panel', className)}
      data-placement={placement}
      data-size={size}
      onKeyDown={handleKeyDown}
    >
      <header className="mn-peek-panel__header">
        {navigable ? (
          <div className="mn-peek-panel__nav">
            <button
              type="button"
              className="mn-peek-panel__button"
              aria-label={messages.previous}
              aria-disabled={!canPrevious || undefined}
              aria-keyshortcuts={shortcuts ? 'K Alt+ArrowUp' : undefined}
              onClick={() => canPrevious && onPrevious?.()}
            >
              <ChevronDownGlyph className="mn-peek-panel__up" />
            </button>
            <button
              type="button"
              className="mn-peek-panel__button"
              aria-label={messages.next}
              aria-disabled={!canNext || undefined}
              aria-keyshortcuts={shortcuts ? 'J Alt+ArrowDown' : undefined}
              onClick={() => canNext && onNext?.()}
            >
              <ChevronDownGlyph />
            </button>
            {position ? (
              <span className="mn-peek-panel__position">
                {messages.position(position.index, position.total)}
              </span>
            ) : null}
          </div>
        ) : null}
        <div className="mn-peek-panel__tools">
          {safeHref ? (
            <LinkComponent className="mn-peek-panel__open" href={safeHref}>
              {messages.openFull}
            </LinkComponent>
          ) : null}
          {actions}
          <button
            type="button"
            className="mn-peek-panel__button"
            aria-label={messages.close}
            onClick={close}
          >
            <CloseGlyph />
          </button>
        </div>
      </header>
      {/* Polite, so stepping to another record announces its title. */}
      <div className="mn-peek-panel__heading" aria-live="polite">
        <h2 id={titleId} className="mn-peek-panel__title">
          {title}
        </h2>
        {description ? (
          <p id={descriptionId} className="mn-peek-panel__description">
            {description}
          </p>
        ) : null}
      </div>
      <div className="mn-peek-panel__body">{children}</div>
      {footer ? <footer className="mn-peek-panel__footer">{footer}</footer> : null}
    </section>
  );
});
