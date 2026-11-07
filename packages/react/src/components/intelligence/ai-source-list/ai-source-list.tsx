'use client';

import { forwardRef, useId, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronDownGlyph } from '../../../internal/icons.js';
import type { AiSourcePart } from '../ai-conversation/index.js';
import {
  AI_SOURCE_KIND_LABELS,
  AI_SOURCE_LINK_REL,
  AiSourceGlyph,
  aiSafeSourceUrl,
  aiSourceMeta,
  type AiSourceKindLabels,
} from '../ai-citation/index.js';

/** A source as AiSourceList shows it: the source part plus how the answer used it. */
export type AiSourceListItem = AiSourcePart & {
  /**
   * `true` (or unset): the answer relies on it. `false`: the agent looked at it
   * but did not use it ("Considered").
   */
  used?: boolean | undefined;
  /** A short excerpt shown under the title. */
  snippet?: string | undefined;
};

/** Every visible and assistive-technology string. English defaults. */
export interface AiSourceListMessages {
  /** Heading of the list. */
  heading: string;
  /** Group of sources the answer relies on, with its count. */
  used: (count: number) => string;
  /** Group of sources looked at but not used, with its count. */
  considered: (count: number) => string;
  /** Button that expands a collapsed list. */
  showAll: (count: number) => string;
  /** Button that collapses the list again. */
  showFewer: string;
  /** Appended to external links for screen readers. */
  newTab: string;
  /** Kind names in each item's meta line. */
  kinds: AiSourceKindLabels;
}

export const AI_SOURCE_LIST_MESSAGES: AiSourceListMessages = {
  heading: 'Sources',
  used: (n) => `Used (${n})`,
  considered: (n) => `Considered (${n})`,
  showAll: (n) => `Show all ${n} sources`,
  showFewer: 'Show fewer',
  newTab: '(opens in a new tab)',
  kinds: AI_SOURCE_KIND_LABELS,
};

export interface AiSourceListProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** The sources, in citation order: the first one is `[1]`. */
  sources: readonly AiSourceListItem[];
  /**
   * Makes each title a button that calls this, e.g. to open AiSourcePreview.
   * Without it, titles with a safe URL link to it in a new tab.
   */
  onOpen?: ((source: AiSourceListItem) => void) | undefined;
  /** Collapse to this many items when there are more. Default 3; `Infinity` never collapses. */
  collapseAfter?: number | undefined;
  /** Initial expanded state when the list is collapsible. Default false. */
  defaultExpanded?: boolean | undefined;
  /** Show the `[n]` number that matches each source's AiCitation marker. Default true. */
  numbered?: boolean | undefined;
  /** Labels. */
  messages?: Partial<AiSourceListMessages> | undefined;
}

/**
 * The block of sources under an AI answer: each with its favicon or kind
 * glyph, title, host and snippet, grouped into "Used" and "Considered" when
 * the agent reports both, and collapsed to the first few when there are
 * more than three.
 */
export const AiSourceList = forwardRef<HTMLElement, AiSourceListProps>(function AiSourceList(
  {
    sources,
    onOpen,
    collapseAfter = 3,
    defaultExpanded = false,
    numbered = true,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...AI_SOURCE_LIST_MESSAGES, ...messagesProp };
  const id = useId();
  const [expanded, setExpanded] = useState(defaultExpanded);

  const numberedSources = sources.map((source, i) => ({ source, n: i + 1 }));
  const used = numberedSources.filter((s) => s.source.used !== false);
  const considered = numberedSources.filter((s) => s.source.used === false);
  const grouped = considered.length > 0;
  const collapsible = sources.length > collapseAfter;
  const collapsed = collapsible && !expanded;

  // Used first, then considered; a collapsed list shows the first few of that order.
  let budget = collapsed ? collapseAfter : Infinity;
  const take = <T,>(list: T[]) => {
    const shown = list.slice(0, Math.max(0, budget));
    budget -= shown.length;
    return shown;
  };
  const usedShown = take(used);
  const consideredShown = take(considered);

  const headingId = `${id}-heading`;
  const listId = `${id}-list`;

  const item = ({ source, n }: { source: AiSourceListItem; n: number }) => {
    const href = aiSafeSourceUrl(source.url);
    const meta = aiSourceMeta(source, m.kinds);
    let title: ReactNode;
    if (onOpen) {
      title = (
        <button type="button" className="mn-ai-source-list__title" onClick={() => onOpen(source)}>
          {source.title}
        </button>
      );
    } else if (href) {
      title = (
        <a
          className="mn-ai-source-list__title"
          href={href}
          target="_blank"
          rel={AI_SOURCE_LINK_REL}
        >
          {source.title}
          <span className="mn-visually-hidden"> {m.newTab}</span>
        </a>
      );
    } else {
      title = <span className="mn-ai-source-list__title">{source.title}</span>;
    }
    return (
      <li
        key={source.id}
        className="mn-ai-source-list__item"
        data-used={source.used !== false || undefined}
      >
        <AiSourceGlyph source={source} className="mn-ai-source-list__glyph" />
        <div className="mn-ai-source-list__body">
          <div className="mn-ai-source-list__line">
            {numbered ? (
              <span className="mn-ai-source-list__number" aria-hidden>
                [{n}]
              </span>
            ) : null}
            {title}
          </div>
          {meta ? <p className="mn-ai-source-list__meta">{meta}</p> : null}
          {source.snippet ? <p className="mn-ai-source-list__snippet">{source.snippet}</p> : null}
        </div>
      </li>
    );
  };

  const group = (key: string, label: string, shown: typeof numberedSources): ReactNode => {
    if (shown.length === 0) return null;
    const groupId = `${id}-${key}`;
    return (
      <div key={key} className="mn-ai-source-list__group" data-group={key}>
        <p id={groupId} className="mn-ai-source-list__group-label">
          {label}
        </p>
        <ul className="mn-ai-source-list__list" aria-labelledby={groupId}>
          {shown.map(item)}
        </ul>
      </div>
    );
  };

  return (
    <section
      ref={ref}
      aria-labelledby={headingId}
      {...rest}
      className={cx('mn-ai-source-list', className)}
      data-collapsed={collapsed || undefined}
    >
      <p id={headingId} className="mn-ai-source-list__heading">
        {m.heading}
      </p>
      <div id={listId} className="mn-ai-source-list__groups">
        {grouped ? (
          <>
            {group('used', m.used(used.length), usedShown)}
            {group('considered', m.considered(considered.length), consideredShown)}
          </>
        ) : (
          <ul className="mn-ai-source-list__list" aria-labelledby={headingId}>
            {usedShown.map(item)}
          </ul>
        )}
      </div>
      {collapsible ? (
        <button
          type="button"
          className="mn-ai-source-list__toggle"
          aria-expanded={expanded}
          aria-controls={listId}
          onClick={() => setExpanded((v) => !v)}
        >
          <ChevronDownGlyph className="mn-ai-source-list__toggle-icon" />
          {expanded ? m.showFewer : m.showAll(sources.length)}
        </button>
      ) : null}
    </section>
  );
});
