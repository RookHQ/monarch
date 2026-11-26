import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import { Badge, type Tone } from '../../../components/data-display/badge/index.js';

/** A tag on an entry, such as "New", "Improved" or "Fix". */
export interface ChangelogTag {
  label: string;
  /** Badge tone. Default `neutral`. Use status tones only when the tag means that status. */
  tone?: Tone | undefined;
}

/** One release note. */
export interface ChangelogEntry {
  /** Stable key, also used as the entry's element id for deep links. */
  id: string;
  /** Release date: a `Date` or an ISO string (`2026-06-01`). */
  date: Date | string;
  /** Headline of the change. */
  title: ReactNode;
  /** What changed, in plain words. Paragraphs, lists or links. */
  body?: ReactNode;
  /** Version label, such as "2.4.0". */
  version?: ReactNode;
  /** Tags; strings are neutral badges. */
  tags?: readonly (string | ChangelogTag)[] | undefined;
  /** Link to the full note. Unsafe URLs are dropped. */
  href?: string | undefined;
  /** Decorative icon shown on the timeline. */
  icon?: ReactNode;
}

export interface ChangelogFeedMessages {
  /** Accessible name of the list when there is no `title`. Default "Changelog". */
  listLabel: string;
  /** Link text to the full note. Default "Read more". */
  readMore: string;
  /** Link text for the RSS feed. Default "RSS feed". */
  rss: string;
  /** Shown when there are no entries. */
  empty: string;
}

export const defaultChangelogFeedMessages: ChangelogFeedMessages = {
  listLabel: 'Changelog',
  readMore: 'Read more',
  rss: 'RSS feed',
  empty: 'No updates yet.',
};

export interface ChangelogFeedProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** Entries, newest first. */
  entries: readonly ChangelogEntry[];
  /** Heading above the feed. Names the section. */
  title?: ReactNode;
  /** One line under the title. */
  description?: ReactNode;
  /** Heading level of `title`; entry headings use the next level. Default 2. */
  headingLevel?: 2 | 3 | undefined;
  /** URL of an RSS or Atom feed, shown as a link in the header. Unsafe URLs are dropped. */
  rssHref?: string | undefined;
  /** Replaces the built-in RSS link, for example with a subscribe button. */
  rssSlot?: ReactNode;
  /** BCP 47 locale for dates. Default: the runtime locale. */
  locale?: string | undefined;
  /**
   * Time zone used to print dates. Default `UTC`, so a date-only ISO string
   * shows the same day on the server and in every browser.
   */
  timeZone?: string | undefined;
  /** Date format. Default `{ year: 'numeric', month: 'long', day: 'numeric' }`. */
  dateFormat?: Intl.DateTimeFormatOptions | undefined;
  /** Content after the list, such as a "Load older updates" button. */
  footer?: ReactNode;
  messages?: Partial<ChangelogFeedMessages> | undefined;
}

const DEFAULT_DATE_FORMAT: Intl.DateTimeFormatOptions = {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
};

function toDate(value: Date | string): Date | null {
  const d = value instanceof Date ? value : new Date(value);
  return Number.isNaN(d.getTime()) ? null : d;
}

/**
 * A dated list of release notes for a public changelog page: each entry has
 * a date, headline, tags, an optional version and body, and a permalink.
 * The header can carry an RSS link. Server-component safe.
 */
export const ChangelogFeed = forwardRef<HTMLElement, ChangelogFeedProps>(function ChangelogFeed(
  {
    entries,
    title,
    description,
    headingLevel = 2,
    rssHref,
    rssSlot,
    locale,
    timeZone = 'UTC',
    dateFormat = DEFAULT_DATE_FORMAT,
    footer,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultChangelogFeedMessages, ...messages };
  const titleId = useId();
  const Title = `h${headingLevel}` as 'h2';
  const EntryHeading = `h${headingLevel + 1}` as 'h3';
  // An unknown `timeZone` must not take the page down: fall back to UTC.
  let formatter: Intl.DateTimeFormat;
  try {
    formatter = new Intl.DateTimeFormat(locale, { ...dateFormat, timeZone });
  } catch {
    formatter = new Intl.DateTimeFormat(locale, { ...dateFormat, timeZone: 'UTC' });
  }
  const rss =
    rssSlot ??
    (rssHref && isSafeUrl(rssHref) ? (
      <a className="mn-changelog-feed__rss" href={rssHref} type="application/rss+xml">
        <svg viewBox="0 0 16 16" aria-hidden fill="currentColor">
          <circle cx="3.5" cy="12.5" r="1.5" />
          <path d="M2 7a7 7 0 0 1 7 7H7a5 5 0 0 0-5-5V7Zm0-4.5A11.5 11.5 0 0 1 13.5 14h-2A9.5 9.5 0 0 0 2 4.5v-2Z" />
        </svg>
        {m.rss}
      </a>
    ) : null);

  return (
    <section
      ref={ref}
      {...(title ? { 'aria-labelledby': titleId } : { 'aria-label': m.listLabel })}
      {...rest}
      className={cx('mn-changelog-feed', className)}
    >
      {title || description || rss ? (
        <header className="mn-changelog-feed__header">
          <div className="mn-changelog-feed__intro">
            {title ? (
              <Title id={titleId} className="mn-changelog-feed__title">
                {title}
              </Title>
            ) : null}
            {description ? <p className="mn-changelog-feed__description">{description}</p> : null}
          </div>
          {rss ? <div className="mn-changelog-feed__header-actions">{rss}</div> : null}
        </header>
      ) : null}

      {entries.length === 0 ? (
        <p className="mn-changelog-feed__empty">{m.empty}</p>
      ) : (
        <ol className="mn-changelog-feed__list">
          {entries.map((entry, index) => {
            const date = toDate(entry.date);
            // By position: an entry id with a space would break aria-labelledby.
            const headingId = `${titleId}-entry-${index}`;
            const href = entry.href && isSafeUrl(entry.href) ? entry.href : undefined;
            return (
              <li key={entry.id} className="mn-changelog-feed__item">
                <article
                  id={entry.id}
                  className="mn-changelog-feed__entry"
                  aria-labelledby={headingId}
                >
                  <div className="mn-changelog-feed__marker" aria-hidden>
                    {entry.icon ?? <span className="mn-changelog-feed__dot" />}
                  </div>
                  <div className="mn-changelog-feed__content">
                    <p className="mn-changelog-feed__meta">
                      {date ? (
                        <time
                          className="mn-changelog-feed__date"
                          dateTime={date.toISOString().slice(0, 10)}
                        >
                          {formatter.format(date)}
                        </time>
                      ) : (
                        <span className="mn-changelog-feed__date">{String(entry.date)}</span>
                      )}
                      {entry.version ? (
                        <span className="mn-changelog-feed__version">{entry.version}</span>
                      ) : null}
                    </p>
                    <EntryHeading id={headingId} className="mn-changelog-feed__entry-title">
                      {entry.title}
                    </EntryHeading>
                    {entry.tags && entry.tags.length > 0 ? (
                      <ul className="mn-changelog-feed__tags">
                        {entry.tags.map((tag, i) => {
                          const t = typeof tag === 'string' ? { label: tag } : tag;
                          return (
                            <li key={`${i}-${t.label}`}>
                              <Badge tone={t.tone ?? 'neutral'} variant="subtle" size="sm">
                                {t.label}
                              </Badge>
                            </li>
                          );
                        })}
                      </ul>
                    ) : null}
                    {entry.body ? (
                      <div className="mn-changelog-feed__body">{entry.body}</div>
                    ) : null}
                    {href ? (
                      <a
                        className="mn-changelog-feed__more"
                        href={href}
                        aria-describedby={headingId}
                      >
                        {m.readMore}
                      </a>
                    ) : null}
                  </div>
                </article>
              </li>
            );
          })}
        </ol>
      )}
      {footer ? <div className="mn-changelog-feed__footer">{footer}</div> : null}
    </section>
  );
});
