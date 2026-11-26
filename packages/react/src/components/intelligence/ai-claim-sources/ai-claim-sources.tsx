'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type MouseEvent,
  type ReactElement,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  ChevronDownGlyph,
  ErrorGlyph,
  HelpGlyph,
  PartialGlyph,
  SuccessGlyph,
  WarningGlyph,
} from '../../../internal/icons.js';
import { useControllable } from '../../../internal/use-controllable.js';
import { Button } from '../../actions/button/index.js';
import { CopyButton } from '../../actions/copy-button/index.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import {
  AI_SOURCE_KIND_LABELS,
  AI_SOURCE_LINK_REL,
  AiSourceGlyph,
  aiSafeSourceUrl,
  type AiSourceKindLabels,
} from '../ai-citation/index.js';
import type { AiSourcePart } from '../ai-conversation/index.js';

/* ------------------------------------------------------------------------ */
/* Model                                                                    */
/* ------------------------------------------------------------------------ */

/**
 * How well the sources back a claim.
 * - `supported`: at least one passage backs it and none contradicts it.
 * - `partial`: passages back only part of it.
 * - `unsupported`: no source backs it.
 * - `conflicting`: sources disagree about it.
 * - `unchecked`: it has not been compared with the sources yet.
 */
export type AiClaimSupport = 'supported' | 'partial' | 'unsupported' | 'conflicting' | 'unchecked';

/** Whether a passage backs the claim or speaks against it. */
export type AiClaimEvidenceStance = 'supports' | 'contradicts';

/** One passage from a source, tied to a claim. */
export interface AiClaimEvidence {
  /** Stable id, unique within the claim. */
  id: string;
  /** The `id` of the source the passage comes from (an entry of `sources`). */
  sourceId: string;
  /** The exact passage, word for word. Shown as a quotation and copied by "Copy quote". */
  quote: string;
  /** Where the passage is in the source: a page, section or line label, e.g. "Page 4". */
  location?: string | undefined;
  /** Whether the passage backs the claim or speaks against it. Unset: not stated. */
  stance?: AiClaimEvidenceStance | undefined;
}

/** One statement of an AI answer and the passages that back or contradict it. */
export interface AiClaim {
  /** Stable id. Reported by `onSelectClaim`, `onFlag` and `onVerify`. */
  id: string;
  /** The claim as the answer states it. */
  text: string;
  /** How well the sources back it. */
  support: AiClaimSupport;
  /** The passages found for it, in the order to show them. Empty when there are none. */
  evidence: readonly AiClaimEvidence[];
}

/**
 * A source that evidence points at. An `AiSourcePart` from a message fits
 * this shape, so the sources of an answer can be passed as they are.
 */
export interface AiClaimSource {
  /** Stable id, matched against `AiClaimEvidence.sourceId`. */
  id: string;
  /** The title shown with every passage from this source. */
  title: string;
  /** Where the source lives. Becomes a link only when it is safe (http, https, mailto or relative). */
  url?: string | undefined;
  /** What kind of thing it is; picks the glyph and the kind name. */
  kind?: AiSourcePart['kind'] | undefined;
  /** When it was published, as an ISO 8601 date or date-time. */
  publishedAt?: string | undefined;
}

/** Which claims the list shows. */
export type AiClaimSourcesFilter = 'all' | 'needs-checking';

/** How many claims there are at each support level. */
export interface AiClaimCounts extends Record<AiClaimSupport, number> {
  /** All claims. */
  total: number;
  /** Claims that are not `supported`: the ones "Needs checking" shows. */
  needsChecking: number;
}

/** `true` for every claim that is not fully `supported`. */
export function aiClaimNeedsChecking(claim: Pick<AiClaim, 'support'>): boolean {
  return claim.support !== 'supported';
}

/** Counts claims by support level. */
export function aiClaimCounts(claims: readonly Pick<AiClaim, 'support'>[]): AiClaimCounts {
  const counts: AiClaimCounts = {
    total: claims.length,
    needsChecking: 0,
    supported: 0,
    partial: 0,
    unsupported: 0,
    conflicting: 0,
    unchecked: 0,
  };
  for (const claim of claims) {
    counts[claim.support] += 1;
    if (aiClaimNeedsChecking(claim)) counts.needsChecking += 1;
  }
  return counts;
}

/* ------------------------------------------------------------------------ */
/* Messages                                                                 */
/* ------------------------------------------------------------------------ */

/** Every visible and assistive-technology string. English defaults. */
export interface AiClaimSourcesMessages {
  /** Heading of the view; also names the region. */
  heading: string;
  /** The line under the heading, e.g. "5 claims: 3 supported, 1 partly, 1 without a source". */
  summary: (counts: AiClaimCounts) => string;
  /** Accessible name of the filter. */
  filterLabel: string;
  /** Filter options. */
  filters: Record<AiClaimSourcesFilter, string>;
  /** Support level shown beside every claim, in plain words. */
  support: Record<AiClaimSupport, string>;
  /** A sentence at the top of an open claim that is not fully supported. */
  notes: Record<Exclude<AiClaimSupport, 'supported'>, string>;
  /** Number of passages found for a claim. */
  passages: (count: number) => string;
  /** Accessible name of a claim's list of passages. */
  evidenceLabel: (claim: string) => string;
  /** Stance of a passage towards the claim. */
  stances: Record<AiClaimEvidenceStance, string>;
  /** Title shown when a passage names a source that is not in `sources`. */
  unknownSource: string;
  /** Publication date of a source, from the formatted date. */
  published: (date: string) => string;
  /** Kind names shown with the source title. */
  kinds: AiSourceKindLabels;
  /** Link or button that opens a passage's source. */
  openSource: string;
  /** Appended to external links for screen readers. */
  newTab: string;
  /** Button that copies a passage. */
  copyQuote: string;
  /** The copy button's label after a successful copy. */
  copied: string;
  /** The copy button's label after a failed copy. */
  copyFailed: string;
  /** Announced after a successful copy. */
  copiedAnnouncement: string;
  /** Announced after a failed copy. */
  copyFailedAnnouncement: string;
  /** Hidden text that tells apart the actions of each passage, from the source title. */
  fromSource: (title: string) => string;
  /** Button that calls `onFlag`. */
  flag: string;
  /** Button that calls `onVerify` on an unchecked claim. */
  verify: string;
  /** Shown when there are no claims at all. */
  empty: string;
  /** Shown when "Needs checking" has nothing to show. */
  noneNeedChecking: string;
  /** Shown in an open claim that has no passages and no note. */
  noEvidence: string;
  /** Announced when the filter changes. */
  showing: (shown: number, total: number) => string;
}

/** Messages with every group of strings optional and partial. */
export type AiClaimSourcesMessageOverrides = Partial<
  Omit<AiClaimSourcesMessages, 'filters' | 'support' | 'notes' | 'stances' | 'kinds'>
> & {
  filters?: Partial<AiClaimSourcesMessages['filters']> | undefined;
  support?: Partial<AiClaimSourcesMessages['support']> | undefined;
  notes?: Partial<AiClaimSourcesMessages['notes']> | undefined;
  stances?: Partial<AiClaimSourcesMessages['stances']> | undefined;
  kinds?: Partial<AiSourceKindLabels> | undefined;
};

export const AI_CLAIM_SOURCES_MESSAGES: AiClaimSourcesMessages = {
  heading: 'Where this came from',
  summary: (c) => {
    if (c.total === 0) return 'No claims';
    const parts = [
      c.supported ? `${c.supported} supported` : '',
      c.partial ? `${c.partial} partly` : '',
      c.unsupported ? `${c.unsupported} without a source` : '',
      c.conflicting ? `${c.conflicting} with sources that disagree` : '',
      c.unchecked ? `${c.unchecked} not checked` : '',
    ].filter(Boolean);
    return `${c.total} ${c.total === 1 ? 'claim' : 'claims'}: ${parts.join(', ')}`;
  },
  filterLabel: 'Show claims',
  filters: { all: 'All', 'needs-checking': 'Needs checking' },
  support: {
    supported: 'Supported',
    partial: 'Partly supported',
    unsupported: 'No source supports this',
    conflicting: 'Sources disagree',
    unchecked: 'Not checked yet',
  },
  notes: {
    partial: 'The sources back only part of this claim.',
    unsupported: 'No source supports this claim. Check it yourself before you rely on it.',
    conflicting: 'Sources disagree about this claim. Read both sides before you rely on it.',
    unchecked: 'This claim has not been checked against the sources yet.',
  },
  passages: (n) => (n === 0 ? 'No passages' : n === 1 ? '1 passage' : `${n} passages`),
  evidenceLabel: (claim) => `Passages for: ${claim}`,
  stances: { supports: 'Supports the claim', contradicts: 'Contradicts the claim' },
  unknownSource: 'Unknown source',
  published: (date) => `Published ${date}`,
  kinds: AI_SOURCE_KIND_LABELS,
  openSource: 'Open source',
  newTab: '(opens in a new tab)',
  copyQuote: 'Copy quote',
  copied: 'Copied',
  copyFailed: "Couldn't copy",
  copiedAnnouncement: 'Quote copied to clipboard',
  copyFailedAnnouncement: "Couldn't copy. Select the quote and copy it manually.",
  fromSource: (title) => `from ${title}`,
  flag: 'Report a problem',
  verify: 'Check again',
  empty: 'There are no claims to check.',
  noneNeedChecking: 'Nothing needs checking: every claim is supported by a source.',
  noEvidence: 'No passages were provided for this claim.',
  showing: (shown, total) => `Showing ${shown} of ${total} claims`,
};

/**
 * The summary line for a set of claims, e.g. "5 claims: 3 supported, 1 partly,
 * 1 without a source". Pass a translated `summary` to change the wording.
 */
export function aiClaimSummary(
  claims: readonly Pick<AiClaim, 'support'>[],
  summary: AiClaimSourcesMessages['summary'] = AI_CLAIM_SOURCES_MESSAGES.summary,
): string {
  return summary(aiClaimCounts(claims));
}

/* ------------------------------------------------------------------------ */
/* Component                                                                */
/* ------------------------------------------------------------------------ */

export interface AiClaimSourcesProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** The claims of the answer, in the order the answer makes them. */
  claims: readonly AiClaim[];
  /** The sources the evidence points at. `AiSourcePart` objects from the message fit. */
  sources: readonly AiClaimSource[];
  /** Controlled ids of the claims whose evidence is open. */
  expandedIds?: readonly string[] | undefined;
  /** Ids of the claims open at first, when uncontrolled. Default none. */
  defaultExpandedIds?: readonly string[] | undefined;
  /** Called with the new list of open claim ids when a claim opens or closes. */
  onExpandedChange?: ((ids: string[]) => void) | undefined;
  /** Controlled filter. */
  filter?: AiClaimSourcesFilter | undefined;
  /** Filter at first, when uncontrolled. Default `all`. */
  defaultFilter?: AiClaimSourcesFilter | undefined;
  /** Called when the person changes the filter. */
  onFilterChange?: ((filter: AiClaimSourcesFilter) => void) | undefined;
  /**
   * Controlled selected claim, e.g. the sentence the person clicked in the
   * answer. `null` selects none.
   */
  selectedClaimId?: string | null | undefined;
  /** Selected claim at first, when uncontrolled. Default none. */
  defaultSelectedClaimId?: string | null | undefined;
  /**
   * Called with a claim's id when the person activates it, so the product can
   * highlight that sentence in the answer.
   */
  onSelectClaim?: ((claimId: string) => void) | undefined;
  /**
   * Called when the person opens a passage's source. A source without a safe
   * URL gets an "Open source" button that calls this. A source with a safe URL
   * is a real link that also calls this first: call `event.preventDefault()`
   * to open your own preview (e.g. AiSourcePreview) instead of the new tab.
   */
  onOpenSource?:
    | ((source: AiClaimSource, evidence: AiClaimEvidence, event: MouseEvent<HTMLElement>) => void)
    | undefined;
  /** Shows "Report a problem" in every open claim; called with the claim id. */
  onFlag?: ((claimId: string) => void) | undefined;
  /** Shows "Check again" in open `unchecked` claims; called with the claim id. */
  onVerify?: ((claimId: string) => void) | undefined;
  /** Called with the passage after "Copy quote" succeeds. */
  onCopyQuote?: ((evidence: AiClaimEvidence) => void) | undefined;
  /** BCP 47 locale for the publication dates. Default: the runtime's locale. */
  locale?: string | undefined;
  /** Replaces any of the English strings; grouped strings may be partial. */
  messages?: AiClaimSourcesMessageOverrides | undefined;
}

const SUPPORT_GLYPH: Record<AiClaimSupport, (props: SVGProps<SVGSVGElement>) => ReactElement> = {
  supported: SuccessGlyph,
  partial: PartialGlyph,
  unsupported: ErrorGlyph,
  conflicting: WarningGlyph,
  unchecked: HelpGlyph,
};

const STANCE_GLYPH: Record<
  AiClaimEvidenceStance,
  (props: SVGProps<SVGSVGElement>) => ReactElement
> = {
  supports: SuccessGlyph,
  contradicts: ErrorGlyph,
};

const NO_IDS: readonly string[] = [];

function formatDate(iso: string | undefined, locale: string | undefined): string | undefined {
  if (!iso) return undefined;
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return undefined;
  try {
    // UTC so a date-only value shows the same day on the server and the client.
    return new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeZone: 'UTC' }).format(date);
  } catch {
    return undefined;
  }
}

/**
 * A "where did this come from" view for an AI answer. It lists the answer's
 * claims, says in words how well the sources back each one, and opens a claim
 * to show the exact passages with their source, location and stance. Claims
 * with no source, or with sources that disagree, are stated plainly and never
 * hidden.
 */
export const AiClaimSources = forwardRef<HTMLElement, AiClaimSourcesProps>(function AiClaimSources(
  {
    claims,
    sources,
    expandedIds: expandedProp,
    defaultExpandedIds,
    onExpandedChange,
    filter: filterProp,
    defaultFilter = 'all',
    onFilterChange,
    selectedClaimId,
    defaultSelectedClaimId = null,
    onSelectClaim,
    onOpenSource,
    onFlag,
    onVerify,
    onCopyQuote,
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = useMemo<AiClaimSourcesMessages>(() => {
    const d = AI_CLAIM_SOURCES_MESSAGES;
    const o = messagesProp ?? {};
    return {
      ...d,
      ...(Object.fromEntries(
        Object.entries(o).filter(([, v]) => v !== undefined),
      ) as Partial<AiClaimSourcesMessages>),
      filters: { ...d.filters, ...o.filters },
      support: { ...d.support, ...o.support },
      notes: { ...d.notes, ...o.notes },
      stances: { ...d.stances, ...o.stances },
      kinds: { ...d.kinds, ...o.kinds },
    };
  }, [messagesProp]);

  const id = useId();
  const headingId = `${id}-heading`;

  const [expanded, setExpanded] = useControllable<readonly string[]>(
    expandedProp,
    defaultExpandedIds ?? NO_IDS,
    onExpandedChange as ((ids: readonly string[]) => void) | undefined,
  );
  const [filter, setFilter] = useControllable<AiClaimSourcesFilter>(
    filterProp,
    defaultFilter,
    onFilterChange,
  );
  const [selected, setSelected] = useControllable<string | null>(
    selectedClaimId,
    defaultSelectedClaimId,
    onSelectClaim as ((id: string | null) => void) | undefined,
  );

  const counts = aiClaimCounts(claims);
  const shown = filter === 'needs-checking' ? claims.filter(aiClaimNeedsChecking) : claims;
  const sourceById = useMemo(() => new Map(sources.map((s) => [s.id, s])), [sources]);

  // Announce the result of a filter change, never the initial filter.
  const previousFilter = useRef(filter);
  const [live, setLive] = useState('');
  useEffect(() => {
    if (previousFilter.current === filter) return;
    previousFilter.current = filter;
    setLive(m.showing(shown.length, claims.length));
    // Only a filter change is news.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  const toggle = (claimId: string) => {
    setExpanded(
      expanded.includes(claimId) ? expanded.filter((x) => x !== claimId) : [...expanded, claimId],
    );
    setSelected(claimId);
  };

  const moveFocus = (event: KeyboardEvent<HTMLButtonElement>) => {
    const { key } = event;
    if (key !== 'ArrowDown' && key !== 'ArrowUp' && key !== 'Home' && key !== 'End') return;
    const list = event.currentTarget.closest('.mn-ai-claim-sources__claims');
    if (!list) return;
    const triggers = Array.from(
      list.querySelectorAll<HTMLButtonElement>('.mn-ai-claim-sources__trigger'),
    );
    const at = triggers.indexOf(event.currentTarget);
    const to =
      key === 'Home'
        ? 0
        : key === 'End'
          ? triggers.length - 1
          : key === 'ArrowDown'
            ? Math.min(triggers.length - 1, at + 1)
            : Math.max(0, at - 1);
    event.preventDefault();
    triggers[to]?.focus();
  };

  const renderEvidence = (evidence: AiClaimEvidence) => {
    const source = sourceById.get(evidence.sourceId);
    const title = source?.title ?? m.unknownSource;
    const href = aiSafeSourceUrl(source?.url);
    const published = formatDate(source?.publishedAt, locale);
    const StanceGlyph = evidence.stance ? STANCE_GLYPH[evidence.stance] : null;
    const which = m.fromSource(title);
    return (
      <li key={evidence.id} className="mn-ai-claim-sources__evidence" data-stance={evidence.stance}>
        <figure className="mn-ai-claim-sources__figure">
          <blockquote className="mn-ai-claim-sources__quote" dir="auto">
            {evidence.quote}
          </blockquote>
          <figcaption className="mn-ai-claim-sources__caption">
            <span className="mn-ai-claim-sources__source">
              <AiSourceGlyph source={{ kind: source?.kind ?? 'doc' }} />
              <cite className="mn-ai-claim-sources__source-title">{title}</cite>
            </span>
            {source?.kind ? (
              <span className="mn-ai-claim-sources__detail">{m.kinds[source.kind]}</span>
            ) : null}
            {evidence.location ? (
              <span className="mn-ai-claim-sources__detail">{evidence.location}</span>
            ) : null}
            {published && source?.publishedAt ? (
              <span className="mn-ai-claim-sources__detail">
                <time dateTime={source.publishedAt}>{m.published(published)}</time>
              </span>
            ) : null}
            {evidence.stance && StanceGlyph ? (
              <span className="mn-ai-claim-sources__stance" data-stance={evidence.stance}>
                <StanceGlyph />
                {m.stances[evidence.stance]}
              </span>
            ) : null}
          </figcaption>
        </figure>
        <div className="mn-ai-claim-sources__evidence-actions">
          {source && href ? (
            <Button asChild size="sm" variant="ghost">
              <a
                href={href}
                target="_blank"
                rel={AI_SOURCE_LINK_REL}
                onClick={(event) => onOpenSource?.(source, evidence, event)}
              >
                {m.openSource}
                <span className="mn-visually-hidden">
                  {' '}
                  {which} {m.newTab}
                </span>
              </a>
            </Button>
          ) : source && onOpenSource ? (
            <Button
              size="sm"
              variant="ghost"
              onClick={(event) => onOpenSource(source, evidence, event)}
            >
              {m.openSource}
              <span className="mn-visually-hidden"> {which}</span>
            </Button>
          ) : null}
          <CopyButton
            size="sm"
            variant="ghost"
            value={evidence.quote}
            label={m.copyQuote}
            copiedLabel={m.copied}
            errorLabel={m.copyFailed}
            copiedMessage={m.copiedAnnouncement}
            errorMessage={m.copyFailedAnnouncement}
            target={which}
            {...(onCopyQuote ? { onCopy: () => onCopyQuote(evidence) } : {})}
          />
        </div>
      </li>
    );
  };

  const renderClaim = (claim: AiClaim) => {
    const open = expanded.includes(claim.id);
    const isSelected = selected === claim.id;
    const panelId = `${id}-panel-${claim.id}`;
    const statusId = `${id}-status-${claim.id}`;
    const Glyph = SUPPORT_GLYPH[claim.support];
    const note = claim.support === 'supported' ? undefined : m.notes[claim.support];
    const canVerify = Boolean(onVerify) && claim.support === 'unchecked';
    return (
      <li
        key={claim.id}
        className="mn-ai-claim-sources__claim"
        data-support={claim.support}
        data-selected={isSelected || undefined}
        data-expanded={open || undefined}
      >
        <div className="mn-ai-claim-sources__row">
          <button
            type="button"
            className="mn-ai-claim-sources__trigger"
            aria-expanded={open}
            aria-controls={panelId}
            aria-describedby={statusId}
            aria-current={isSelected ? 'true' : undefined}
            onClick={() => toggle(claim.id)}
            onKeyDown={moveFocus}
          >
            <ChevronDownGlyph className="mn-ai-claim-sources__chevron" />
            <span className="mn-ai-claim-sources__text" dir="auto">
              {claim.text}
            </span>
          </button>
          <p id={statusId} className="mn-ai-claim-sources__status">
            <span className="mn-ai-claim-sources__support">
              <Glyph />
              {m.support[claim.support]}
            </span>
            <span className="mn-ai-claim-sources__count">{m.passages(claim.evidence.length)}</span>
          </p>
        </div>
        {open ? (
          <div id={panelId} className="mn-ai-claim-sources__panel">
            {note ? (
              <p className="mn-ai-claim-sources__note" dir="auto">
                {note}
              </p>
            ) : null}
            {claim.evidence.length > 0 ? (
              <ul
                className="mn-ai-claim-sources__evidence-list"
                aria-label={m.evidenceLabel(claim.text)}
              >
                {claim.evidence.map(renderEvidence)}
              </ul>
            ) : note ? null : (
              <p className="mn-ai-claim-sources__note">{m.noEvidence}</p>
            )}
            {onFlag || canVerify ? (
              <div className="mn-ai-claim-sources__claim-actions">
                {canVerify ? (
                  <Button size="sm" variant="secondary" onClick={() => onVerify?.(claim.id)}>
                    {m.verify}
                  </Button>
                ) : null}
                {onFlag ? (
                  <Button size="sm" variant="ghost" onClick={() => onFlag(claim.id)}>
                    {m.flag}
                  </Button>
                ) : null}
              </div>
            ) : null}
          </div>
        ) : null}
      </li>
    );
  };

  return (
    <section
      ref={ref}
      // A named group, not a region: a page may show several, and landmarks must be unique.
      role="group"
      aria-labelledby={headingId}
      {...rest}
      className={cx('mn-ai-claim-sources', className)}
      data-filter={filter}
    >
      <header className="mn-ai-claim-sources__header">
        <div className="mn-ai-claim-sources__intro">
          <h3 id={headingId} className="mn-ai-claim-sources__heading">
            {m.heading}
          </h3>
          <p className="mn-ai-claim-sources__summary">{m.summary(counts)}</p>
        </div>
        {claims.length > 0 ? (
          <SegmentedControl<AiClaimSourcesFilter>
            className="mn-ai-claim-sources__filter"
            size="sm"
            aria-label={m.filterLabel}
            value={filter}
            onValueChange={setFilter}
            options={[
              { value: 'all', label: m.filters.all, count: counts.total },
              {
                value: 'needs-checking',
                label: m.filters['needs-checking'],
                count: counts.needsChecking,
              },
            ]}
          />
        ) : null}
      </header>

      {shown.length > 0 ? (
        <ol className="mn-ai-claim-sources__claims" aria-labelledby={headingId}>
          {shown.map(renderClaim)}
        </ol>
      ) : (
        <p className="mn-ai-claim-sources__empty">
          {claims.length === 0 ? m.empty : m.noneNeedChecking}
        </p>
      )}

      <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
        {live}
      </span>
    </section>
  );
});
