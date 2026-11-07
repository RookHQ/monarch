import {
  forwardRef,
  type CSSProperties,
  type ElementType,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { formatNumber } from '../../../internal/format-number.js';
import { DeltaIndicator, type DeltaIndicatorProps } from '../../charts/delta-indicator/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { InlineErrorRetry } from '../../feedback/inline-error-retry/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import { Avatar } from '../../identity/avatar/index.js';

/** One ranked row. */
export interface LeaderboardEntry {
  /** Stable key. */
  id: string;
  /** Who or what is ranked: a person, team, campaign or product. */
  name: string;
  /** The figure the board ranks by. */
  value: number;
  /** The value as shown, e.g. "4.2 h" or "38%". Default: `value` formatted with `format`. */
  display?: string | undefined;
  /** A second line, e.g. a team or "Step 2 of Spring launch". */
  secondary?: ReactNode;
  /** Avatar image. Without it, initials from `name`. */
  avatarSrc?: string | null | undefined;
  /** Stable seed for the avatar's fallback colour, such as the person's id. */
  seed?: string | undefined;
  /** `circle` for people (default), `square` for teams, products and organisations. */
  avatarShape?: 'circle' | 'square' | undefined;
  /** Opens the entry. The name becomes a link. */
  href?: string | undefined;
  /** Change against the previous period, e.g. `{ value: 0.12 }` or `{ current, previous }`. */
  change?: Omit<DeltaIndicatorProps, 'ref'> | undefined;
  /** A short tag after the name, e.g. a "3 breached" Badge. */
  badge?: ReactNode;
  /** This entry is the person viewing. It is highlighted and tagged "(you)". */
  current?: boolean | undefined;
}

/** The words Leaderboard shows and speaks. Override them to translate. */
export interface LeaderboardMessages {
  /** Tag after the viewer's own name. */
  you: string;
  /** Shown when there are no entries. */
  empty: string;
  /** Spoken name of the loading placeholder. */
  loading: string;
  /** Text of the link (or note) for entries not shown. Receives how many. */
  more: (hidden: number) => string;
  /** Hidden text before a rank. Receives the rank. */
  rank: (rank: number) => string;
}

export const LEADERBOARD_MESSAGES: LeaderboardMessages = {
  you: '(you)',
  empty: 'Nothing to rank yet',
  loading: 'Loading ranking',
  more: (hidden) => `${hidden} more`,
  rank: (rank) => `Rank ${rank}:`,
};

export interface LeaderboardProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The entries, best first (or set `sort`). */
  entries: readonly LeaderboardEntry[];
  /** What the value counts, spoken after it, e.g. "open tickets" or "replies". */
  valueLabel?: string | undefined;
  /** Accessible name of the list, e.g. "Top agents by tickets solved". */
  label?: string | undefined;
  /**
   * `desc` ranks the largest value first, `asc` the smallest (response times);
   * `none` (default) keeps the given order. Equal values share a rank.
   */
  sort?: 'desc' | 'asc' | 'none' | undefined;
  /** How a value without `display` is written: `number` (default) or `percent` (a ratio). */
  format?: 'number' | 'percent' | undefined;
  /** `compact` writes 1.2K. Default `standard`. */
  notation?: 'standard' | 'compact' | undefined;
  /** BCP 47 locale for values. */
  locale?: string | undefined;
  /** Show avatars. Default `true`. */
  showAvatars?: boolean | undefined;
  /** Show a bar under each row, its length relative to the largest value. Default `true`. */
  showBars?: boolean | undefined;
  /** Emphasise the top three ranks with a filled rank marker. Default `false`. */
  highlightTop?: boolean | undefined;
  /** Show at most this many rows. Default: all. */
  limit?: number | undefined;
  /** How many entries exist in total, when `entries` is already a top N. Adds "N more". */
  total?: number | undefined;
  /** Where "N more" links to: the full report. */
  moreHref?: string | undefined;
  /** Component used for links, e.g. a router link. Default `a`. */
  linkComponent?: ElementType | undefined;
  /** Placeholder rows while loading. */
  loading?: boolean | undefined;
  /** Placeholder rows while loading. Default 5. */
  loadingRows?: number | undefined;
  /** Replaces the list with this failure. */
  error?: ReactNode;
  /** With `error`, shows Try again calling this. */
  onRetry?: (() => unknown) | undefined;
  /** Replaces the default empty text. */
  empty?: ReactNode;
  /** Strings, for localisation. */
  messages?: Partial<LeaderboardMessages> | undefined;
}

/** Ranks entries: equal values share a rank and the next rank skips (1, 1, 3). */
export function rankEntries<T extends Pick<LeaderboardEntry, 'value'>>(
  entries: readonly T[],
  sort: 'desc' | 'asc' | 'none' = 'none',
): { entry: T; rank: number }[] {
  const ordered =
    sort === 'none'
      ? entries.slice()
      : entries.slice().sort((a, b) => (sort === 'desc' ? b.value - a.value : a.value - b.value));
  const out: { entry: T; rank: number }[] = [];
  ordered.forEach((entry, index) => {
    const previous = out[index - 1];
    const rank = previous && previous.entry.value === entry.value ? previous.rank : index + 1;
    out.push({ entry, rank });
  });
  return out;
}

/**
 * A ranked list for dashboards (top agents, best campaign steps, busiest
 * teams): rank, avatar, name, value, an optional change and a bar relative to
 * the leader. Equal values share a rank; the viewer's own row is tagged.
 */
export const Leaderboard = forwardRef<HTMLDivElement, LeaderboardProps>(function Leaderboard(
  {
    entries,
    valueLabel,
    label,
    sort = 'none',
    format = 'number',
    notation = 'standard',
    locale,
    showAvatars = true,
    showBars = true,
    highlightTop = false,
    limit,
    total,
    moreHref,
    linkComponent: LinkComponent = 'a',
    loading = false,
    loadingRows = 5,
    error,
    onRetry,
    empty,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...LEADERBOARD_MESSAGES, ...messagesProp };
  const hasError = !loading && error !== undefined && error !== null && error !== false;
  const ranked = rankEntries(entries, sort);
  const shown = limit !== undefined && limit > 0 ? ranked.slice(0, limit) : ranked;
  const hidden = Math.max(0, (total ?? entries.length) - shown.length);
  const max = Math.max(0, ...shown.map(({ entry }) => Math.abs(entry.value)));

  let content: ReactNode;
  if (loading) {
    content = (
      <div className="mn-leaderboard__skeleton" role="img" aria-label={messages.loading}>
        {Array.from({ length: Math.max(1, loadingRows) }, (_, i) => (
          <div key={i} className="mn-leaderboard__skeleton-row">
            {showAvatars ? <Skeleton shape="circle" width="1.75rem" height="1.75rem" /> : null}
            <Skeleton shape="text" width={`${70 - i * 8}%`} />
          </div>
        ))}
      </div>
    );
  } else if (hasError) {
    content = onRetry ? (
      <InlineErrorRetry onRetry={onRetry}>{error}</InlineErrorRetry>
    ) : (
      <p className="mn-leaderboard__error" role="alert">
        {error}
      </p>
    );
  } else if (shown.length === 0) {
    content = empty ?? (
      <EmptyState size="compact" layout="line" headingLevel="p" title={messages.empty} />
    );
  } else {
    content = (
      <>
        <ol className="mn-leaderboard__list" aria-label={label}>
          {shown.map(({ entry, rank }) => {
            const valueText =
              entry.display ??
              formatNumber(entry.value, {
                kind: format,
                notation,
                ...(locale !== undefined && { locale }),
              });
            const share = max > 0 ? Math.abs(entry.value) / max : 0;
            return (
              <li
                key={entry.id}
                className="mn-leaderboard__row"
                data-rank={rank}
                data-top={highlightTop && rank <= 3 ? '' : undefined}
                data-current={entry.current ? '' : undefined}
                aria-current={entry.current ? 'true' : undefined}
              >
                <span className="mn-leaderboard__rank">
                  <span className="mn-visually-hidden">{messages.rank(rank)} </span>
                  <span aria-hidden>{rank}</span>
                </span>
                {showAvatars ? (
                  <Avatar
                    className="mn-leaderboard__avatar"
                    name={entry.name}
                    size="sm"
                    decorative
                    {...(entry.avatarSrc !== undefined && { src: entry.avatarSrc })}
                    {...(entry.seed !== undefined && { seed: entry.seed })}
                    {...(entry.avatarShape !== undefined && { shape: entry.avatarShape })}
                  />
                ) : null}
                <span className="mn-leaderboard__main">
                  <span className="mn-leaderboard__name-line">
                    {entry.href ? (
                      <LinkComponent className="mn-leaderboard__name" href={entry.href}>
                        {entry.name}
                      </LinkComponent>
                    ) : (
                      <span className="mn-leaderboard__name">{entry.name}</span>
                    )}
                    {entry.current ? (
                      <span className="mn-leaderboard__you">{messages.you}</span>
                    ) : null}
                    {entry.badge ? (
                      <span className="mn-leaderboard__badge">{entry.badge}</span>
                    ) : null}
                  </span>
                  {entry.secondary ? (
                    <span className="mn-leaderboard__secondary">{entry.secondary}</span>
                  ) : null}
                  {showBars ? (
                    <span className="mn-leaderboard__bar" aria-hidden>
                      <span
                        className="mn-leaderboard__bar-fill"
                        style={{ '--mn-leaderboard-share': share } as CSSProperties}
                      />
                    </span>
                  ) : null}
                </span>
                <span className="mn-leaderboard__figures">
                  <span className="mn-leaderboard__value">
                    {valueText}
                    {valueLabel ? <span className="mn-visually-hidden"> {valueLabel}</span> : null}
                  </span>
                  {entry.change ? (
                    <DeltaIndicator
                      size="sm"
                      {...(locale !== undefined && { locale })}
                      {...entry.change}
                      className={cx('mn-leaderboard__change', entry.change.className)}
                    />
                  ) : null}
                </span>
              </li>
            );
          })}
        </ol>
        {hidden > 0 ? (
          moreHref ? (
            <LinkComponent className="mn-leaderboard__more" href={moreHref}>
              {messages.more(hidden)}
            </LinkComponent>
          ) : (
            <p className="mn-leaderboard__more">{messages.more(hidden)}</p>
          )
        ) : null}
      </>
    );
  }

  return (
    <div
      ref={ref}
      aria-busy={loading || undefined}
      {...rest}
      className={cx('mn-leaderboard', className)}
      data-state={
        loading ? 'loading' : hasError ? 'error' : shown.length === 0 ? 'empty' : undefined
      }
    >
      {content}
    </div>
  );
});
