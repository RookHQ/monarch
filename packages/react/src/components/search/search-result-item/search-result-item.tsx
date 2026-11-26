import {
  forwardRef,
  useId,
  type HTMLAttributes,
  type MouseEvent,
  type ReactNode,
  type Ref,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { ShellLink, type ShellLinkComponent } from '../../../internal/shell/link.js';

/** A `[start, end)` character range in a string that matched the query. */
export type MatchRange = readonly [start: number, end: number];

/** Where highlights come from. Give one of `terms`, `ranges` or `markTags`. */
export interface HighlightOptions {
  /**
   * Words to highlight, matched case-insensitively wherever they appear. Use
   * `queryTerms(query)` to split what the user typed.
   */
  terms?: readonly string[] | undefined;
  /** Exact match ranges from the search backend. Overlapping ranges are merged. */
  ranges?: readonly MatchRange[] | undefined;
  /**
   * The text carries `<mark>` and `</mark>` markers from the backend (for
   * example PostgreSQL `ts_headline`). Only those two markers are read; any
   * other markup stays literal text and is never parsed as HTML.
   */
  markTags?: boolean | undefined;
}

/** One run of text and whether it matched. */
export interface HighlightSegment {
  text: string;
  match: boolean;
}

/**
 * Splits a query into the words to highlight: letters and digits only,
 * lower-cased, de-duplicated, at most 8.
 */
export function queryTerms(query: string): string[] {
  const words = query.toLowerCase().match(/[\p{L}\p{N}]+/gu) ?? [];
  return [...new Set(words)].slice(0, 8);
}

function mergeRanges(ranges: [number, number][]): [number, number][] {
  const sorted = ranges.filter(([s, e]) => e > s).sort((a, b) => a[0] - b[0]);
  const out: [number, number][] = [];
  for (const [s, e] of sorted) {
    const last = out[out.length - 1];
    if (last && s <= last[1]) last[1] = Math.max(last[1], e);
    else out.push([s, e]);
  }
  return out;
}

function fromRanges(text: string, ranges: [number, number][]): HighlightSegment[] {
  const out: HighlightSegment[] = [];
  let last = 0;
  for (const [s, e] of mergeRanges(ranges)) {
    const start = Math.max(last, Math.min(s, text.length));
    const end = Math.min(e, text.length);
    if (end <= start) continue;
    if (start > last) out.push({ text: text.slice(last, start), match: false });
    out.push({ text: text.slice(start, end), match: true });
    last = end;
  }
  if (last < text.length) out.push({ text: text.slice(last), match: false });
  return out;
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Splits `text` into matched and unmatched runs. Pure and string-only: the
 * result is rendered as React text, so nothing in `text` is ever treated as
 * markup.
 */
export function highlightSegments(
  text: string,
  options: HighlightOptions = {},
): HighlightSegment[] {
  if (!text) return [];
  if (options.markTags) {
    const out: HighlightSegment[] = [];
    let match = false;
    for (const part of text.split(/(<\/?mark>)/gi)) {
      const tag = part.toLowerCase();
      if (tag === '<mark>') match = true;
      else if (tag === '</mark>') match = false;
      else if (part) out.push({ text: part, match });
    }
    return out;
  }
  if (options.ranges?.length) {
    return fromRanges(
      text,
      options.ranges.map(([s, e]) => [s, e]),
    );
  }
  const terms = (options.terms ?? []).map((t) => t.trim()).filter(Boolean);
  if (!terms.length) return [{ text, match: false }];
  const pattern = new RegExp(
    terms
      .sort((a, b) => b.length - a.length)
      .map(escapeRegExp)
      .join('|'),
    'giu',
  );
  const ranges: [number, number][] = [];
  for (const m of text.matchAll(pattern)) {
    const i = m.index ?? 0;
    ranges.push([i, i + m[0].length]);
  }
  return fromRanges(text, ranges);
}

export interface HighlightTextProps extends HighlightOptions {
  /** Plain text to show. Never parsed as HTML. */
  text: string;
  /** Class name for each `<mark>`. */
  markClassName?: string | undefined;
}

/**
 * Plain text with the matched parts wrapped in `<mark>`. Safe for untrusted
 * input: everything is rendered as React text.
 */
export function HighlightText({ text, markClassName, ...options }: HighlightTextProps) {
  const segments = highlightSegments(text, options);
  return (
    <>
      {segments.map((s, i) =>
        s.match ? (
          <mark key={i} className={cx('mn-search-result-item__mark', markClassName)}>
            {s.text}
          </mark>
        ) : (
          s.text
        ),
      )}
    </>
  );
}

export interface SearchResultItemMessages {
  /** Accessible name of the breadcrumb path. Default "Location". */
  path: string;
}

const DEFAULT_MESSAGES: SearchResultItemMessages = { path: 'Location' };

export interface SearchResultItemProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'title' | 'onSelect'
> {
  /** The result's title, as plain text. It becomes the link or button. */
  title: string;
  /**
   * Where the result goes. Only relative, http(s) and mailto URLs are used;
   * anything else (such as `javascript:`) is dropped and the title renders as
   * a button when `onSelect` is set, or as text.
   */
  href?: string | undefined;
  /** Called when the result is activated (click, Enter). Runs before link navigation. */
  onSelect?: ((event: MouseEvent<HTMLElement>) => void) | undefined;
  /** Breadcrumb segments shown above the snippet, such as `['Handbook', 'Travel']`. */
  path?: readonly string[] | undefined;
  /** A short excerpt of the matching content, as plain text. */
  snippet?: string | undefined;
  /** Words to highlight in the title and snippet. Use `queryTerms(query)`. */
  terms?: readonly string[] | undefined;
  /** Exact match ranges in the title (overrides `terms` for the title). */
  titleRanges?: readonly MatchRange[] | undefined;
  /** Exact match ranges in the snippet (overrides `terms` for the snippet). */
  snippetRanges?: readonly MatchRange[] | undefined;
  /** The snippet carries `<mark>`/`</mark>` markers from the backend. Other markup stays text. */
  snippetMarkTags?: boolean | undefined;
  /** Leading icon or avatar, decorative. */
  icon?: ReactNode;
  /** Trailing details such as a type badge, date or owner. */
  meta?: ReactNode;
  /** Router link component for client-side navigation; defaults to `<a>`. */
  linkComponent?: ShellLinkComponent | undefined;
  /** Tab index of the link or button. SearchResults sets it for roving focus. */
  targetTabIndex?: number | undefined;
  /** Ref to the link or button. */
  targetRef?: Ref<HTMLElement> | undefined;
  /** Text overrides. */
  messages?: Partial<SearchResultItemMessages> | undefined;
}

/**
 * One search result: title (a link or button), breadcrumb path, snippet with
 * the matched words in `<mark>`, and optional icon and meta. The whole row is
 * clickable; text is always rendered as text, never as HTML.
 */
export const SearchResultItem = forwardRef<HTMLDivElement, SearchResultItemProps>(
  function SearchResultItem(
    {
      title,
      href,
      onSelect,
      path,
      snippet,
      terms,
      titleRanges,
      snippetRanges,
      snippetMarkTags,
      icon,
      meta,
      linkComponent,
      targetTabIndex,
      targetRef,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const id = useId();
    const pathId = `${id}-path`;
    const snippetId = `${id}-snippet`;
    const safeHref = href && isSafeUrl(href) ? href : undefined;
    const hasPath = Boolean(path?.length);
    const describedBy = cx(hasPath && pathId, snippet && snippetId) || undefined;
    const titleText = <HighlightText text={title} terms={terms} ranges={titleRanges} />;

    let target: ReactNode;
    if (safeHref) {
      target = (
        <ShellLink
          ref={targetRef as Ref<HTMLAnchorElement>}
          as={linkComponent}
          href={safeHref}
          className="mn-search-result-item__target"
          aria-describedby={describedBy}
          tabIndex={targetTabIndex}
          data-mn-result-target=""
          onClick={onSelect}
        >
          {titleText}
        </ShellLink>
      );
    } else if (onSelect) {
      target = (
        <button
          ref={targetRef as Ref<HTMLButtonElement>}
          type="button"
          className="mn-search-result-item__target"
          aria-describedby={describedBy}
          tabIndex={targetTabIndex}
          data-mn-result-target=""
          onClick={onSelect}
        >
          {titleText}
        </button>
      );
    } else {
      target = <span className="mn-search-result-item__text">{titleText}</span>;
    }

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-search-result-item', className)}
        data-interactive={safeHref || onSelect ? '' : undefined}
      >
        {icon ? (
          <span className="mn-search-result-item__icon" aria-hidden="true">
            {icon}
          </span>
        ) : null}
        <div className="mn-search-result-item__body">
          <div className="mn-search-result-item__title">{target}</div>
          {hasPath ? (
            <p id={pathId} className="mn-search-result-item__path">
              <span className="mn-visually-hidden">{messages.path}: </span>
              {(path ?? []).map((segment, i) => (
                <span key={i} className="mn-search-result-item__segment">
                  {i > 0 ? (
                    <>
                      <span className="mn-search-result-item__separator" aria-hidden="true">
                        /
                      </span>
                      <span className="mn-visually-hidden">, </span>
                    </>
                  ) : null}
                  {segment}
                </span>
              ))}
            </p>
          ) : null}
          {snippet ? (
            <p id={snippetId} className="mn-search-result-item__snippet">
              <HighlightText
                text={snippet}
                terms={terms}
                ranges={snippetRanges}
                markTags={snippetMarkTags}
              />
            </p>
          ) : null}
        </div>
        {meta ? <div className="mn-search-result-item__meta">{meta}</div> : null}
      </div>
    );
  },
);
