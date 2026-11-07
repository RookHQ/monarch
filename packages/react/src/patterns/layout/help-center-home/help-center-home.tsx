'use client';

import { forwardRef, useId, useMemo, useState, type HTMLAttributes, type ReactNode } from 'react';
import { SearchInput } from '../../../components/search/search-input/index.js';
import { LoadingState } from '../../../components/feedback/loading-state/index.js';
import { EmptyState } from '../../../components/feedback/empty-state/index.js';
import { Alert } from '../../../components/feedback/alert/index.js';
import type { LinkComponent } from '../marketing-site-shell/index.js';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph, SearchGlyph } from '../../../internal/icons.js';

/** A published help article, as a list entry. */
export interface HelpArticle {
  id: string;
  title: string;
  /** Category heading it is grouped under. Empty goes under `messages.general`. */
  category?: string | null | undefined;
  /** Where the article opens. */
  href: string;
  /** Already-formatted last update ("Mar 2, 2026"), shown in the `rows` list. */
  updated?: string | undefined;
}

export interface HelpCenterHomeMessages {
  title: string;
  description: string;
  /** Search field name and placeholder. */
  search: string;
  loading: string;
  /** No articles at all. */
  empty: string;
  /** Search found nothing. */
  noResults: (query: string) => string;
  /** Announced while searching. */
  resultCount: (count: number) => string;
  /** Heading for articles without a category. */
  general: string;
  /** Card link hint in the `cards` layout. */
  readArticle: string;
  /** Shown with `unavailable`. */
  unavailable: string;
  /** Prefix of the update date in `rows`. */
  updated: (date: string) => string;
}

const DEFAULT_MESSAGES: HelpCenterHomeMessages = {
  title: 'Help center',
  description: 'Answers to common questions.',
  search: 'Search articles',
  loading: 'Loading articles',
  empty: 'No articles have been published yet.',
  noResults: (q) => `No articles match “${q}”.`,
  resultCount: (n) => `${n} matching ${n === 1 ? 'article' : 'articles'}`,
  general: 'General',
  readArticle: 'Read article',
  unavailable: 'This site doesn’t have a help center.',
  updated: (d) => `Updated ${d}`,
};

const norm = (s: string) => s.toLocaleLowerCase().normalize('NFKD').replace(/[̀-ͯ]/g, '');

/** Articles whose title or category contain every word of the query (case and accents ignored). */
export function filterHelpArticles<T extends Pick<HelpArticle, 'title' | 'category'>>(
  articles: readonly T[],
  query: string,
): T[] {
  const words = norm(query).split(/\s+/).filter(Boolean);
  if (!words.length) return [...articles];
  return articles.filter((a) => {
    const hay = norm(`${a.title} ${a.category ?? ''}`);
    return words.every((w) => hay.includes(w));
  });
}

/** Groups articles by category, in first-seen order. */
export function groupHelpArticles<T extends Pick<HelpArticle, 'category'>>(
  articles: readonly T[],
  general = 'General',
): Array<[string, T[]]> {
  const groups = new Map<string, T[]>();
  for (const a of articles) {
    const key = a.category?.trim() || general;
    const list = groups.get(key);
    if (list) list.push(a);
    else groups.set(key, [a]);
  }
  return [...groups.entries()];
}

export interface HelpCenterHomeProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** Published articles. `null` while loading. */
  articles: HelpArticle[] | null;
  /** Loading failed: shown as an alert instead of the list. */
  error?: ReactNode;
  /** The site has no help center: a short message and `contact` instead of search. */
  unavailable?: boolean | undefined;
  /** How the list looks. Default `columns`. */
  layout?: 'columns' | 'rows' | 'cards' | 'dense' | undefined;
  /** The search text (controlled). */
  query?: string | undefined;
  /** Initial search text, e.g. from `?q=`. */
  defaultQuery?: string | undefined;
  onQueryChange?: ((query: string) => void) | undefined;
  /** Heading element for the page title. Default `h1`. */
  headingAs?: 'h1' | 'h2' | undefined;
  /**
   * "Still need help?" content: a link or button to contact support. Shown after
   * an empty search, with `unavailable`, and at the end of the page.
   */
  contact?: ReactNode;
  /** Small print at the bottom, e.g. where the articles come from. */
  footer?: ReactNode;
  /** Router link for article links (Next.js `Link`). Default `a`. */
  linkAs?: LinkComponent | undefined;
  /** Interface text, for translation. */
  messages?: Partial<HelpCenterHomeMessages> | undefined;
}

/**
 * The home page of a public help center: title, a search box that filters as
 * you type, and the published articles grouped by category in one of four
 * layouts, with loading, error, empty and no-results states. The app loads
 * the articles; search runs on the list it is given.
 */
export const HelpCenterHome = forwardRef<HTMLDivElement, HelpCenterHomeProps>(
  function HelpCenterHome(
    {
      articles,
      error,
      unavailable = false,
      layout = 'columns',
      query: queryProp,
      defaultQuery = '',
      onQueryChange,
      headingAs: Heading = 'h1',
      contact,
      footer,
      linkAs: Link = 'a',
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
    const id = useId();
    const countId = `${id}-count`;
    const [inner, setInner] = useState(defaultQuery);
    const query = queryProp ?? inner;
    const setQuery = (next: string) => {
      if (queryProp === undefined) setInner(next);
      onQueryChange?.(next);
    };
    const filtered = useMemo(
      () => (articles ? filterHelpArticles(articles, query) : []),
      [articles, query],
    );
    const groups = groupHelpArticles(filtered, messages.general);
    const SectionHeading = Heading === 'h1' ? 'h2' : 'h3';
    const anchor = (category: string) =>
      `${id}-${category.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`.replace(/:/g, '');

    const articleLink = (a: HelpArticle) => {
      switch (layout) {
        case 'rows':
          return (
            <Link href={a.href} className="mn-help-center__article">
              <span className="mn-help-center__article-title">{a.title}</span>
              {a.updated ? (
                <span className="mn-help-center__article-meta">{messages.updated(a.updated)}</span>
              ) : null}
              <ChevronRightGlyph className="mn-help-center__chevron" aria-hidden />
            </Link>
          );
        case 'cards':
          return (
            <Link href={a.href} className="mn-help-center__article">
              <span className="mn-help-center__article-title">{a.title}</span>
              <span className="mn-help-center__article-more" aria-hidden>
                {messages.readArticle}
                <ChevronRightGlyph />
              </span>
            </Link>
          );
        default:
          return (
            <Link href={a.href} className="mn-help-center__article">
              <DocGlyph />
              <span className="mn-help-center__article-title">{a.title}</span>
            </Link>
          );
      }
    };

    let body: ReactNode;
    if (unavailable) {
      body = null;
    } else if (articles === null) {
      body = <LoadingState label={messages.loading} variant="list" rows={4} />;
    } else if (error) {
      body = (
        <Alert tone="danger" live="polite">
          {error}
        </Alert>
      );
    } else if (articles.length === 0) {
      body = <p className="mn-help-center__note">{messages.empty}</p>;
    } else if (filtered.length === 0) {
      body = (
        <EmptyState
          size="compact"
          kind="filtered"
          headingLevel="p"
          icon={<SearchGlyph />}
          title={messages.noResults(query.trim())}
          actions={contact}
        />
      );
    } else {
      body = (
        <div className="mn-help-center__groups">
          {groups.map(([category, list]) => (
            <section
              key={category}
              className="mn-help-center__group"
              aria-labelledby={anchor(category)}
            >
              <SectionHeading id={anchor(category)} className="mn-help-center__category">
                {category}
              </SectionHeading>
              <ul className="mn-help-center__list">
                {list.map((a) => (
                  <li key={a.id}>{articleLink(a)}</li>
                ))}
              </ul>
            </section>
          ))}
        </div>
      );
    }

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-help-center', className)}
        data-layout={layout}
        aria-busy={articles === null && !unavailable ? true : undefined}
      >
        <header className="mn-help-center__header">
          <Heading className="mn-help-center__title">{messages.title}</Heading>
          {unavailable ? (
            <p className="mn-help-center__description">{messages.unavailable}</p>
          ) : messages.description ? (
            <p className="mn-help-center__description">{messages.description}</p>
          ) : null}
        </header>
        {unavailable ? (
          contact ? (
            <div className="mn-help-center__contact">{contact}</div>
          ) : null
        ) : (
          <>
            <form
              role="search"
              className="mn-help-center__search"
              onSubmit={(e) => e.preventDefault()}
            >
              <SearchInput
                aria-label={messages.search}
                placeholder={messages.search}
                value={query}
                onValueChange={setQuery}
                size="lg"
                aria-describedby={countId}
              />
            </form>
            <p id={countId} className="mn-visually-hidden" role="status">
              {query.trim() && articles ? messages.resultCount(filtered.length) : ''}
            </p>
            {body}
            {contact && filtered.length > 0 ? (
              <div className="mn-help-center__contact">{contact}</div>
            ) : null}
          </>
        )}
        {footer ? <div className="mn-help-center__footer">{footer}</div> : null}
      </div>
    );
  },
);

function DocGlyph() {
  return (
    <svg
      className="mn-help-center__icon"
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable="false"
    >
      <path d="M9 1.75H4.5a1.25 1.25 0 0 0-1.25 1.25v10a1.25 1.25 0 0 0 1.25 1.25h7a1.25 1.25 0 0 0 1.25-1.25V5.5L9 1.75Z" />
      <path d="M9 1.75V5.5h3.75M5.75 8.5h4.5M5.75 11h3" />
    </svg>
  );
}
