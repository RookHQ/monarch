'use client';

import { forwardRef, useEffect, useRef, type ReactNode, type RefObject } from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import {
  Sheet,
  SheetBody,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  type SheetContentProps,
} from '../../overlays/sheet/index.js';
import type { AiSourcePart } from '../ai-conversation/index.js';
import {
  AI_SOURCE_KIND_LABELS,
  AI_SOURCE_LINK_REL,
  AiSourceGlyph,
  aiSafeSourceUrl,
  aiSourceMeta,
  type AiSourceKindLabels,
} from '../ai-citation/index.js';

/**
 * Where `quote` first appears in `text` as `[start, end)` character offsets,
 * ignoring case and differences in whitespace; `undefined` when it does not.
 */
export function findQuoteRange(text: string, quote: string): [number, number] | undefined {
  const words = quote.trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return undefined;
  const pattern = words.map((w) => w.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('\\s+');
  const match = new RegExp(pattern, 'i').exec(text);
  return match ? [match.index, match.index + match[0].length] : undefined;
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiSourcePreviewMessages {
  /** Label of the quoted passage when it is not found in the content. */
  quoted: string;
  /** Link to the original in a new tab. */
  openOriginal: string;
  /** Appended to the link for screen readers. */
  newTab: string;
  /** Shown when there is neither content nor a quote. */
  noPreview: string;
  /** Accessible name of the close button. */
  close: string;
  /** Kind names in the meta line. */
  kinds: AiSourceKindLabels;
}

export const AI_SOURCE_PREVIEW_MESSAGES: AiSourcePreviewMessages = {
  quoted: 'Quoted passage',
  openOriginal: 'Open original',
  newTab: '(opens in a new tab)',
  noPreview: 'No preview is available for this source.',
  close: 'Close',
  kinds: AI_SOURCE_KIND_LABELS,
};

export interface AiSourcePreviewProps extends Omit<
  SheetContentProps,
  'children' | 'content' | 'footer' | 'closeLabel'
> {
  /** The source to show. */
  source: AiSourcePart;
  /** The cited passage. Highlighted in `content` with `<mark>` and scrolled into view. */
  quote?: string | undefined;
  /**
   * The cited document, record or page. A string is shown as plain text
   * (paragraphs split on blank lines) with `quote` highlighted. A node is
   * rendered as is: put the passage in a `<mark>` and it is scrolled into view.
   */
  content?: ReactNode | undefined;
  /** Whether the sheet is open. */
  open: boolean;
  /** Called when the sheet asks to open or close (Escape, close button, outside click). */
  onOpenChange: (open: boolean) => void;
  /** Where "Open original" goes. Defaults to `source.url`. Shown only when safe. */
  openHref?: string | undefined;
  /** Labels. */
  messages?: Partial<AiSourcePreviewMessages> | undefined;
}

function highlightedText(text: string, quote: string | undefined): ReactNode {
  const range = quote ? findQuoteRange(text, quote) : undefined;
  const paragraphs: { start: number; end: number }[] = [];
  const re = /\n\s*\n/g;
  let last = 0;
  for (let m = re.exec(text); m; m = re.exec(text)) {
    paragraphs.push({ start: last, end: m.index });
    last = m.index + m[0].length;
  }
  paragraphs.push({ start: last, end: text.length });
  return paragraphs
    .filter((p) => text.slice(p.start, p.end).trim())
    .map((p) => {
      const slice = text.slice(p.start, p.end);
      if (!range || range[1] <= p.start || range[0] >= p.end) return <p key={p.start}>{slice}</p>;
      const from = Math.max(range[0], p.start) - p.start;
      const to = Math.min(range[1], p.end) - p.start;
      return (
        <p key={p.start}>
          {slice.slice(0, from)}
          <mark className="mn-ai-source-preview__mark">{slice.slice(from, to)}</mark>
          {slice.slice(to)}
        </p>
      );
    });
}

/** Scrolls the first `<mark>` in `container` into view once mounted and when `deps` change. */
function ScrollToMark({
  container,
  deps,
}: {
  container: RefObject<HTMLDivElement | null>;
  deps: readonly unknown[];
}) {
  useEffect(() => {
    // Wait a frame so the sheet has laid out before scrolling.
    const frame = requestAnimationFrame(() => {
      const mark = container.current?.querySelector('mark');
      if (!mark) return;
      const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
      mark.scrollIntoView?.({ block: 'center', behavior: reduce ? 'auto' : 'smooth' });
    });
    return () => cancelAnimationFrame(frame);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- deps are passed in.
  }, deps);
  return null;
}

/**
 * A side sheet that shows the document, record or page an AI answer cited,
 * with the quoted passage highlighted and scrolled into view, and a link to
 * the original. Open it from AiCitation or AiSourceList `onOpen`.
 */
export const AiSourcePreview = forwardRef<HTMLDivElement, AiSourcePreviewProps>(
  function AiSourcePreview(
    {
      source,
      quote,
      content,
      open,
      onOpenChange,
      openHref,
      messages: messagesProp,
      side = 'right',
      size = 'md',
      className,
      onOpenAutoFocus,
      onCloseAutoFocus,
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_SOURCE_PREVIEW_MESSAGES, ...messagesProp };
    const bodyRef = useRef<HTMLDivElement | null>(null);
    const href = aiSafeSourceUrl(openHref ?? source.url);
    const meta = aiSourceMeta(source, m.kinds);

    const isText = typeof content === 'string';
    const foundInText = isText && quote ? findQuoteRange(content, quote) !== undefined : false;
    const showQuoteBlock = Boolean(quote) && (content == null || (isText && !foundInText));

    // Radix only returns focus to a SheetTrigger; this sheet is usually
    // opened from a citation or list, so remember what had focus instead.
    const returnTo = useRef<HTMLElement | null>(null);

    return (
      <Sheet open={open} onOpenChange={onOpenChange}>
        <SheetContent
          ref={ref}
          side={side}
          size={size}
          closeLabel={m.close}
          {...rest}
          className={cx('mn-ai-source-preview', className)}
          onOpenAutoFocus={(e) => {
            const active = document.activeElement;
            returnTo.current =
              active instanceof HTMLElement && active !== document.body ? active : null;
            onOpenAutoFocus?.(e);
          }}
          onCloseAutoFocus={(e) => {
            onCloseAutoFocus?.(e);
            const target = returnTo.current;
            returnTo.current = null;
            if (!e.defaultPrevented && target?.isConnected) {
              e.preventDefault();
              target.focus();
            }
          }}
          footer={
            href ? (
              <Button asChild variant="secondary" className="mn-ai-source-preview__open">
                <a href={href} target="_blank" rel={AI_SOURCE_LINK_REL}>
                  {m.openOriginal}
                  <span className="mn-visually-hidden"> {m.newTab}</span>
                </a>
              </Button>
            ) : undefined
          }
        >
          <SheetHeader>
            <SheetTitle className="mn-ai-source-preview__title">{source.title}</SheetTitle>
            <SheetDescription className="mn-ai-source-preview__meta">
              <AiSourceGlyph source={source} />
              <span>{meta || m.kinds[source.kind]}</span>
            </SheetDescription>
          </SheetHeader>
          <SheetBody>
            <div ref={bodyRef} className="mn-ai-source-preview__body">
              <ScrollToMark container={bodyRef} deps={[quote, content]} />
              {showQuoteBlock ? (
                <figure className="mn-ai-source-preview__quote">
                  <figcaption className="mn-ai-source-preview__quote-label">{m.quoted}</figcaption>
                  <blockquote>
                    <mark className="mn-ai-source-preview__mark">{quote}</mark>
                  </blockquote>
                </figure>
              ) : null}
              {isText ? (
                <div className="mn-ai-source-preview__content">
                  {highlightedText(content, foundInText ? quote : undefined)}
                </div>
              ) : content != null ? (
                <div className="mn-ai-source-preview__content">{content}</div>
              ) : null}
              {content == null && !quote ? (
                <p className="mn-ai-source-preview__empty">{m.noPreview}</p>
              ) : null}
            </div>
          </SheetBody>
        </SheetContent>
      </Sheet>
    );
  },
);
