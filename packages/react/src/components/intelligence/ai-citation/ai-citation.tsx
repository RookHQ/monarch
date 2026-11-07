'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type SVGProps,
} from 'react';
import * as PopoverPrimitive from '@radix-ui/react-popover';
import { cx } from '../../../internal/cx.js';
import { isSafeImageSrc, isSafeUrl } from '../../../internal/sanitize.js';
import type { AiPart, AiSourcePart } from '../ai-conversation/index.js';

/* ------------------------------------------------------------------------ */
/* Shared source helpers (also used by AiSourceList and AiSourcePreview).   */
/* ------------------------------------------------------------------------ */

/** `rel` for every link that leaves the app from model or tool output. */
export const AI_SOURCE_LINK_REL = 'noopener noreferrer nofollow';

/** Readable names for each source kind. English defaults. */
export interface AiSourceKindLabels {
  web: string;
  doc: string;
  record: string;
  file: string;
}

export const AI_SOURCE_KIND_LABELS: AiSourceKindLabels = {
  web: 'Web page',
  doc: 'Document',
  record: 'Record',
  file: 'File',
};

/**
 * The host name of a source's URL without `www.` (`example.com`), or
 * `undefined` when it has no URL, the URL is relative or it is not safe.
 */
export function aiSourceHost(source: Pick<AiSourcePart, 'url'>): string | undefined {
  if (!source.url || !isSafeUrl(source.url)) return undefined;
  try {
    const host = new URL(source.url).hostname;
    return host ? host.replace(/^www\./, '') : undefined;
  } catch {
    return undefined;
  }
}

/** The source's URL when it is safe to link to (http, https, mailto or relative), else `undefined`. */
export function aiSafeSourceUrl(url: string | undefined): string | undefined {
  return url && isSafeUrl(url) ? url : undefined;
}

/**
 * Numbers the sources of a message in the order they appear (1, 2, 3…), the
 * same numbering AiMessageView and AiSourceList use. Look up a citation
 * part's marker with `index.get(part.sourceId)`.
 */
export function aiSourceIndex(
  parts: readonly AiPart[],
): Map<string, { index: number; source: AiSourcePart }> {
  const map = new Map<string, { index: number; source: AiSourcePart }>();
  for (const part of parts) {
    if (part.type === 'source' && !map.has(part.id)) {
      map.set(part.id, { index: map.size + 1, source: part });
    }
  }
  return map;
}

const glyph = {
  width: 16,
  height: 16,
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.5,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
  focusable: false,
} as const;

const KIND_PATHS: Record<AiSourcePart['kind'], string> = {
  web: 'M8 2a6 6 0 1 0 0 12A6 6 0 0 0 8 2zM2 8h12M8 2c1.6 1.7 2.4 3.7 2.4 6S9.6 12.3 8 14M8 2C6.4 3.7 5.6 5.7 5.6 8s.8 4.3 2.4 6',
  doc: 'M4 2h5l3 3v9H4zM9 2v3h3M6 8h4M6 10.5h4',
  record: 'M2.5 3.5h11v9h-11zM2.5 6.5h11M2.5 9.5h11M6 6.5v6',
  file: 'M10.5 5.5 6 10a1.4 1.4 0 0 0 2 2l5-5a2.8 2.8 0 0 0-4-4L4 8a4.2 4.2 0 0 0 6 6l3.5-3.5',
};

export interface AiSourceGlyphProps extends Omit<SVGProps<SVGSVGElement>, 'children'> {
  /** The source whose favicon or kind glyph to show. */
  source: Pick<AiSourcePart, 'kind' | 'favicon'>;
}

/**
 * A source's favicon when it has a safe one, otherwise a small glyph for its
 * kind (web page, document, record, file). Decorative: hidden from assistive
 * technology, so put the source's title next to it.
 */
export function AiSourceGlyph({ source, className, ...rest }: AiSourceGlyphProps) {
  const [broken, setBroken] = useState(false);
  if (source.favicon && !broken && isSafeImageSrc(source.favicon)) {
    return (
      <img
        className={cx('mn-ai-source-glyph', className)}
        src={source.favicon}
        alt=""
        width={16}
        height={16}
        loading="lazy"
        referrerPolicy="no-referrer"
        onError={() => setBroken(true)}
      />
    );
  }
  return (
    <svg {...glyph} {...rest} className={cx('mn-ai-source-glyph', className)}>
      <path d={KIND_PATHS[source.kind] ?? KIND_PATHS.doc} />
    </svg>
  );
}

/** One line under a source's title: host, product and kind, joined with " · ". */
export function aiSourceMeta(
  source: Pick<AiSourcePart, 'url' | 'product' | 'kind'>,
  kinds: AiSourceKindLabels = AI_SOURCE_KIND_LABELS,
): string {
  return [aiSourceHost(source), source.product, kinds[source.kind]]
    .filter((v): v is string => Boolean(v))
    .join(' · ');
}

/* ------------------------------------------------------------------------ */
/* AiCitation                                                               */
/* ------------------------------------------------------------------------ */

/** Every visible and assistive-technology string. English defaults. */
export interface AiCitationMessages {
  /** Accessible name of the marker, e.g. "Source 1: Quarterly report". */
  marker: (index: number, title: string) => string;
  /** Accessible name of the card. */
  card: (index: number) => string;
  /** Button that calls `onOpen` (an in-app preview). */
  open: string;
  /** Link to the source's URL in a new tab. */
  visit: string;
  /** Appended to the visit link for screen readers. */
  newTab: string;
  /** Kind names shown in the card's meta line. */
  kinds: AiSourceKindLabels;
}

export const AI_CITATION_MESSAGES: AiCitationMessages = {
  marker: (n, title) => `Source ${n}: ${title}`,
  card: (n) => `Source ${n}`,
  open: 'Open source',
  visit: 'Visit page',
  newTab: '(opens in a new tab)',
  kinds: AI_SOURCE_KIND_LABELS,
};

export interface AiCitationProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'children' | 'type'
> {
  /** The number shown in the marker: `1` renders `[1]`. Use `aiSourceIndex` to number a message's sources. */
  index: number;
  /** The cited source (an `AiSourcePart` from the message). */
  source: AiSourcePart;
  /** The passage the answer relies on, from the citation part. Shown in the card. */
  quote?: string | undefined;
  /**
   * Shows an "Open source" button in the card, e.g. to open AiSourcePreview.
   * Without it the card links to `source.url` (when safe) in a new tab.
   */
  onOpen?: ((source: AiSourcePart) => void) | undefined;
  /** Controlled open state of the card. */
  open?: boolean | undefined;
  /** Called when the card opens or closes. */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Milliseconds the pointer rests on the marker before the card opens. Default 200. */
  openDelay?: number | undefined;
  /** Milliseconds after the pointer leaves the marker and card before it closes. Default 250. */
  closeDelay?: number | undefined;
  /** Labels. */
  messages?: Partial<AiCitationMessages> | undefined;
}

/**
 * An inline `[1]` marker that points a sentence of an AI answer at its
 * source. Hovering or focusing it shows a card with the source's title, the
 * quoted passage, where it lives and a way to open it; clicking or pressing
 * Enter pins the card and moves focus into it. Escape closes it.
 */
export const AiCitation = forwardRef<HTMLButtonElement, AiCitationProps>(function AiCitation(
  {
    index,
    source,
    quote,
    onOpen,
    open: openProp,
    onOpenChange,
    openDelay = 200,
    closeDelay = 250,
    messages: messagesProp,
    className,
    onPointerEnter,
    onPointerLeave,
    onFocus,
    onBlur,
    onClick,
    ...rest
  },
  ref,
) {
  const m = { ...AI_CITATION_MESSAGES, ...messagesProp };
  const [openState, setOpenState] = useState(false);
  const open = openProp ?? openState;
  // Pinned: opened by click or Enter, so focus moved into the card and goes
  // back to the marker when it closes. Unpinned (hover/focus) never moves focus.
  const pinned = useRef(false);
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined);
  const contentRef = useRef<HTMLDivElement | null>(null);
  // Set while focus returns from the card to the marker, so that focus does
  // not reopen the card it just closed.
  const returning = useRef(false);
  useEffect(() => () => clearTimeout(timer.current), []);

  const setOpen = useCallback(
    (next: boolean) => {
      clearTimeout(timer.current);
      if (!next) {
        pinned.current = false;
        if (
          typeof document !== 'undefined' &&
          contentRef.current?.contains(document.activeElement)
        ) {
          returning.current = true;
          setTimeout(() => {
            returning.current = false;
          }, 50);
        }
      }
      if (openProp === undefined) setOpenState(next);
      onOpenChange?.(next);
    },
    [openProp, onOpenChange],
  );

  const schedule = (next: boolean, delay: number) => {
    clearTimeout(timer.current);
    if (delay <= 0) setOpen(next);
    else timer.current = setTimeout(() => setOpen(next), delay);
  };

  const hoverIn = () => {
    if (!open) schedule(true, openDelay);
    else clearTimeout(timer.current);
  };
  const hoverOut = () => {
    if (open && !pinned.current) schedule(false, closeDelay);
    else if (!open) clearTimeout(timer.current);
  };

  const href = aiSafeSourceUrl(source.url);
  const meta = aiSourceMeta(source, m.kinds);
  const cardLabel = m.card(index);

  return (
    <PopoverPrimitive.Root
      open={open}
      onOpenChange={(next) => {
        if (!next) setOpen(false);
      }}
    >
      <PopoverPrimitive.Trigger asChild>
        <button
          ref={ref}
          type="button"
          aria-label={m.marker(index, source.title)}
          {...rest}
          className={cx('mn-ai-citation', className)}
          data-state={open ? 'open' : 'closed'}
          onPointerEnter={(e) => {
            onPointerEnter?.(e);
            if (e.pointerType !== 'touch') hoverIn();
          }}
          onPointerLeave={(e) => {
            onPointerLeave?.(e);
            if (e.pointerType !== 'touch') hoverOut();
          }}
          onFocus={(e) => {
            onFocus?.(e);
            if (returning.current) {
              returning.current = false;
              return;
            }
            if (!open) setOpen(true);
          }}
          onBlur={(e) => {
            onBlur?.(e);
            const to = e.relatedTarget as Node | null;
            if (open && !pinned.current && !(to && contentRef.current?.contains(to))) {
              setOpen(false);
            }
          }}
          onClick={(e) => {
            onClick?.(e);
            if (e.defaultPrevented) return;
            // Radix toggles through onOpenChange; we drive it ourselves.
            e.preventDefault();
            if (open && pinned.current) {
              setOpen(false);
              return;
            }
            pinned.current = true;
            if (open) {
              // Already open from hover or focus: pin it and move focus in.
              contentRef.current
                ?.querySelector<HTMLElement>('a[href],button:not([disabled])')
                ?.focus();
            } else setOpen(true);
          }}
        >
          <span aria-hidden>[{index}]</span>
        </button>
      </PopoverPrimitive.Trigger>
      <PopoverPrimitive.Portal>
        <PopoverPrimitive.Content
          ref={contentRef}
          className="mn-ai-citation__card"
          side="top"
          align="center"
          sideOffset={6}
          collisionPadding={8}
          aria-label={cardLabel}
          onOpenAutoFocus={(e) => {
            if (!pinned.current) e.preventDefault();
          }}
          onCloseAutoFocus={(e) => {
            // Only return focus when it was in the card as it closed.
            if (!returning.current) e.preventDefault();
          }}
          onPointerEnter={hoverIn}
          onPointerLeave={hoverOut}
        >
          {meta ? (
            <p className="mn-ai-citation__meta">
              <AiSourceGlyph source={source} />
              <span>{meta}</span>
            </p>
          ) : null}
          <p className="mn-ai-citation__title">{source.title}</p>
          {quote ? <blockquote className="mn-ai-citation__quote">{quote}</blockquote> : null}
          {onOpen || href ? (
            <div className="mn-ai-citation__actions">
              {onOpen ? (
                <button
                  type="button"
                  className="mn-ai-citation__action"
                  onClick={() => {
                    setOpen(false);
                    onOpen(source);
                  }}
                >
                  {m.open}
                </button>
              ) : null}
              {href ? (
                <a
                  className="mn-ai-citation__action"
                  href={href}
                  target="_blank"
                  rel={AI_SOURCE_LINK_REL}
                >
                  {m.visit}
                  <span className="mn-visually-hidden"> {m.newTab}</span>
                </a>
              ) : null}
            </div>
          ) : null}
        </PopoverPrimitive.Content>
      </PopoverPrimitive.Portal>
    </PopoverPrimitive.Root>
  );
});
