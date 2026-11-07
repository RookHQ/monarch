'use client';

import { useId, useMemo, useState, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph, SearchGlyph } from '../../../internal/icons.js';
import { ExternalGlyph } from '../../../internal/shell/icons.js';
import { ShellLink } from '../../../internal/shell/link.js';
import { useRookBarPanel } from './rook-bar.context.js';

/** A link in a RookBar panel. */
export interface RookBarLink {
  label: string;
  href: string;
  /** One line under the label. */
  description?: string | undefined;
  /** Another site: opens in a new tab and says so to screen readers. */
  external?: boolean | undefined;
}

interface PanelLinkProps {
  link: Pick<RookBarLink, 'href' | 'external'>;
  className?: string | undefined;
  newTab: string;
  children: ReactNode;
}

/** A panel link: the bar's link component for internal links, a new tab for external ones. */
function PanelLink({ link, className, newTab, children }: PanelLinkProps) {
  const { close, linkComponent } = useRookBarPanel();
  if (link.external) {
    return (
      <a
        href={link.href}
        className={className}
        target="_blank"
        rel="noopener noreferrer"
        onClick={close}
      >
        {children}
        <span className="mn-visually-hidden"> ({newTab})</span>
      </a>
    );
  }
  return (
    <ShellLink as={linkComponent} href={link.href} className={className} onClick={close}>
      {children}
    </ShellLink>
  );
}

/* ---------------------------------------------------------------- Catalog */

/** An app (or product) tile in the catalog. */
export interface RookBarApp extends RookBarLink {
  id: string;
  /** Icon or logo. Decorative: the label names the link. */
  icon?: ReactNode;
}

export interface RookBarAppCategory {
  id: string;
  name: string;
  /** What the category does for the customer, under its name. */
  promise?: string | undefined;
  apps: RookBarApp[];
}

export interface RookBarAppCatalogMessages {
  /** Search field label and placeholder. Default "Find an app". */
  search: string;
  /** The all-apps choice in the category list. Default "All apps". */
  allApps: string;
  /** Heading over every app. Default "Every app". */
  allTitle: string;
  /** Line under `allTitle`. Default "". */
  allPromise: string;
  /** Name of the category list. Default "Categories". */
  categories: string;
  /** Result count while searching. */
  results: (count: number) => string;
  /** No search results. */
  noResults: (query: string) => string;
  /** Appended to links that open in a new tab. */
  newTab: string;
}

const CATALOG_MESSAGES: RookBarAppCatalogMessages = {
  search: 'Find an app',
  allApps: 'All apps',
  allTitle: 'Every app',
  allPromise: '',
  categories: 'Categories',
  results: (n) => `${n} ${n === 1 ? 'app' : 'apps'}`,
  noResults: (q) => `No app matches “${q}”.`,
  newTab: 'opens in a new tab',
};

export interface RookBarAppCatalogProps {
  categories: RookBarAppCategory[];
  /** The third column on desktop (platform links, programs, a sign-up call to action); last on mobile. */
  aside?: ReactNode;
  /** Interface text, for translation. */
  messages?: Partial<RookBarAppCatalogMessages> | undefined;
}

function AppTile({ app, newTab, compact }: { app: RookBarApp; newTab: string; compact?: boolean }) {
  return (
    <PanelLink link={app} newTab={newTab} className="mn-rook-bar-catalog__tile">
      {app.icon ? (
        <span
          className="mn-rook-bar-catalog__icon"
          data-compact={compact ? '' : undefined}
          aria-hidden
        >
          {app.icon}
        </span>
      ) : null}
      <span className="mn-rook-bar-catalog__text">
        <span className="mn-rook-bar-catalog__name">
          {app.label}
          {app.external ? <ExternalGlyph className="mn-rook-bar__external" /> : null}
        </span>
        {app.description ? (
          <span className="mn-rook-bar-catalog__tagline">{app.description}</span>
        ) : null}
      </span>
    </PanelLink>
  );
}

/**
 * A catalog of apps for a RookBar menu: search and a category list beside a
 * grid of app tiles on desktop, and grouped lists on mobile. Put it in a
 * RookBar item's `panel`.
 */
export function RookBarAppCatalog({ categories, aside, messages: m }: RookBarAppCatalogProps) {
  const messages = { ...CATALOG_MESSAGES, ...m };
  const { layout } = useRookBarPanel();
  const [active, setActive] = useState<string | null>(null);
  const [query, setQuery] = useState('');
  const searchId = useId();
  const all = useMemo(() => categories.flatMap((c) => c.apps), [categories]);
  const q = query.trim().toLowerCase();
  const results = q
    ? all.filter((a) => `${a.label} ${a.description ?? ''}`.toLowerCase().includes(q))
    : [];
  const category = categories.find((c) => c.id === active);

  if (layout === 'mobile') {
    return (
      <div className="mn-rook-bar-catalog" data-layout="mobile">
        {categories.map((c) => (
          <div key={c.id} className="mn-rook-bar-catalog__group">
            <p className="mn-rook-bar__eyebrow">{c.name}</p>
            <ul className="mn-rook-bar-catalog__grid">
              {c.apps.map((app) => (
                <li key={app.id}>
                  <AppTile app={app} newTab={messages.newTab} compact />
                </li>
              ))}
            </ul>
          </div>
        ))}
        {aside ? <div className="mn-rook-bar-catalog__aside">{aside}</div> : null}
      </div>
    );
  }

  const filterButton = (id: string | null, label: string, count: number) => {
    const current = !q && active === id;
    return (
      <li key={id ?? 'all'}>
        <button
          type="button"
          className="mn-rook-bar-catalog__category"
          aria-pressed={current}
          onFocus={() => !q && setActive(id)}
          onMouseEnter={() => !q && setActive(id)}
          onClick={() => {
            setQuery('');
            setActive(id);
          }}
        >
          <span>{label}</span>
          <span className="mn-rook-bar-catalog__count">{count}</span>
        </button>
      </li>
    );
  };

  const shown = q ? results : (category?.apps ?? all);
  return (
    <div className="mn-rook-bar-catalog" data-layout="desktop" data-aside={aside ? '' : undefined}>
      <div className="mn-rook-bar-catalog__side">
        <label className="mn-rook-bar-catalog__search" htmlFor={searchId}>
          <SearchGlyph aria-hidden />
          <span className="mn-visually-hidden">{messages.search}</span>
          <input
            id={searchId}
            type="search"
            value={query}
            placeholder={messages.search}
            onChange={(e) => setQuery(e.target.value)}
          />
        </label>
        <ul className="mn-rook-bar-catalog__categories" aria-label={messages.categories}>
          {filterButton(null, messages.allApps, all.length)}
          {categories.map((c) => filterButton(c.id, c.name, c.apps.length))}
        </ul>
      </div>
      <div className="mn-rook-bar-catalog__main">
        <div className="mn-rook-bar-catalog__head">
          {q ? (
            <p className="mn-rook-bar-catalog__status" role="status">
              {results.length ? messages.results(results.length) : messages.noResults(query.trim())}
            </p>
          ) : (
            <>
              <p className="mn-rook-bar-catalog__title">{category?.name ?? messages.allTitle}</p>
              {(category ? category.promise : messages.allPromise) ? (
                <p className="mn-rook-bar-catalog__promise">
                  {category ? category.promise : messages.allPromise}
                </p>
              ) : null}
            </>
          )}
        </div>
        <ul
          className="mn-rook-bar-catalog__grid"

          data-columns={!q && !category ? 3 : 2}
        >
          {shown.map((app) => (
            <li key={app.id}>
              <AppTile app={app} newTab={messages.newTab} compact={!q && !category} />
            </li>
          ))}
        </ul>
      </div>
      {aside ? <div className="mn-rook-bar-catalog__aside">{aside}</div> : null}
    </div>
  );
}

/* ------------------------------------------------------------- Link list */

export interface RookBarLinkListProps {
  /** Small heading above the list ("Platform", "Programs"). */
  heading?: string | undefined;
  links: RookBarLink[];
  /** Number the links 01, 02, … (services, steps). */
  numbered?: boolean | undefined;
  /** `list` (one column) or `grid` (two columns on desktop). Default `list`. */
  layout?: 'list' | 'grid' | undefined;
  className?: string | undefined;
  /** Appended to links that open in a new tab. Default "opens in a new tab". */
  newTabLabel?: string | undefined;
}

/** A list of described links for a RookBar panel or a catalog `aside`. */
export function RookBarLinkList({
  heading,
  links,
  numbered = false,
  layout = 'list',
  className,
  newTabLabel = 'opens in a new tab',
}: RookBarLinkListProps) {
  const headingId = useId();
  return (
    <div className={cx('mn-rook-bar-links', className)} data-layout={layout}>
      {heading ? (
        <p id={headingId} className="mn-rook-bar__eyebrow">
          {heading}
        </p>
      ) : null}
      <ul className="mn-rook-bar-links__list" aria-labelledby={heading ? headingId : undefined}>
        {links.map((link, i) => (
          <li key={link.href}>
            <PanelLink link={link} newTab={newTabLabel} className="mn-rook-bar-links__link">
              {numbered ? (
                <span className="mn-rook-bar-links__number" aria-hidden>
                  {String(i + 1).padStart(2, '0')}
                </span>
              ) : null}
              <span className="mn-rook-bar-links__label">
                {link.label}
                {link.external ? (
                  <ExternalGlyph className="mn-rook-bar__external" />
                ) : (
                  <ChevronRightGlyph className="mn-rook-bar-links__chevron" aria-hidden />
                )}
              </span>
              {link.description ? (
                <span className="mn-rook-bar-links__description">{link.description}</span>
              ) : null}
            </PanelLink>
          </li>
        ))}
      </ul>
    </div>
  );
}

/* --------------------------------------------------------- Feature panel */

export interface RookBarFeature {
  title: string;
  description?: string | undefined;
  /** A logo or picture above the title. Give an `<img>` its own alt text. */
  media?: ReactNode;
  /** The call to action at the bottom of the card. */
  action?: RookBarLink | undefined;
}

export interface RookBarFeaturePanelProps {
  /** The highlighted card on the left (desktop) or after the links (mobile). */
  feature: RookBarFeature;
  links: RookBarLink[];
  /** Number the links. Default true. */
  numbered?: boolean | undefined;
  /** Appended to links that open in a new tab. Default "opens in a new tab". */
  newTabLabel?: string | undefined;
}

/** A featured card beside a grid of links, for a RookBar menu (services, solutions). */
export function RookBarFeaturePanel({
  feature,
  links,
  numbered = true,
  newTabLabel = 'opens in a new tab',
}: RookBarFeaturePanelProps) {
  const { layout } = useRookBarPanel();
  const action = feature.action ? (
    <PanelLink link={feature.action} newTab={newTabLabel} className="mn-rook-bar-feature__action">
      {feature.action.label}
      {feature.action.external ? <ExternalGlyph className="mn-rook-bar__external" /> : null}
    </PanelLink>
  ) : null;
  if (layout === 'mobile') {
    return (
      <div className="mn-rook-bar-feature" data-layout="mobile">
        <RookBarLinkList links={links} layout="grid" newTabLabel={newTabLabel} />
        {action}
      </div>
    );
  }
  return (
    <div className="mn-rook-bar-feature" data-layout="desktop">
      <div className="mn-rook-bar-feature__card">
        {feature.media ? <div className="mn-rook-bar-feature__media">{feature.media}</div> : null}
        <p className="mn-rook-bar-feature__title">{feature.title}</p>
        {feature.description ? (
          <p className="mn-rook-bar-feature__description">{feature.description}</p>
        ) : null}
        {action}
      </div>
      <RookBarLinkList
        links={links}
        numbered={numbered}
        layout="grid"
        newTabLabel={newTabLabel}
        className="mn-rook-bar-feature__links"
      />
    </div>
  );
}

/* --------------------------------------------------------- Stories panel */

/** A recent post in the stories panel. */
export interface RookBarStory {
  id: string;
  title: string;
  href: string;
  /** Section name ("Press release"). */
  label?: string | undefined;
  /** Already-formatted date or other short meta ("Mar 2, 2026"). */
  meta?: string | undefined;
  /** Picture URL. Decorative; without it the label fills the card. */
  image?: string | null | undefined;
  external?: boolean | undefined;
}

export interface RookBarStoriesPanelMessages {
  /** Heading over the recent posts. Default "Latest". */
  latest: string;
  /** Name of the section list. Default "Sections". */
  sections: string;
  /** Spoken while posts load. Default "Loading latest posts". */
  loading: string;
  /** No posts. Default "Pick a section to read the latest." */
  empty: string;
  newTab: string;
}

const STORIES_MESSAGES: RookBarStoriesPanelMessages = {
  latest: 'Latest',
  sections: 'Sections',
  loading: 'Loading latest posts',
  empty: 'Pick a section to read the latest.',
  newTab: 'opens in a new tab',
};

export interface RookBarStoriesPanelProps {
  /** Section pages (blog, press, news). */
  sections: RookBarLink[];
  /** Recent posts, newest first. `null` while loading. Load them when the menu opens. */
  stories?: RookBarStory[] | null | undefined;
  /** How many posts to show. Default 3 on desktop, 2 on mobile. */
  limit?: number | undefined;
  messages?: Partial<RookBarStoriesPanelMessages> | undefined;
}

/** Section links beside the latest posts, for a RookBar menu (news, blog, press). */
export function RookBarStoriesPanel({
  sections,
  stories,
  limit,
  messages: m,
}: RookBarStoriesPanelProps) {
  const messages = { ...STORIES_MESSAGES, ...m };
  const { layout } = useRookBarPanel();
  const headingId = useId();
  const max = limit ?? (layout === 'mobile' ? 2 : 3);
  const shown = stories ? stories.slice(0, max) : stories;

  let latest: ReactNode;
  if (shown === undefined) latest = null;
  else if (shown === null)
    latest = (
      <div className="mn-rook-bar-stories__grid" role="status" aria-label={messages.loading}>
        {Array.from({ length: max }, (_, i) => (
          <div key={i} className="mn-rook-bar-stories__placeholder" aria-hidden>
            <span />
            <span />
            <span />
          </div>
        ))}
      </div>
    );
  else if (shown.length === 0)
    latest = <p className="mn-rook-bar-stories__empty">{messages.empty}</p>;
  else
    latest = (
      <ul className="mn-rook-bar-stories__grid" aria-labelledby={headingId}>
        {shown.map((story) => (
          <li key={story.id}>
            <PanelLink link={story} newTab={messages.newTab} className="mn-rook-bar-stories__story">
              {layout === 'desktop' ? (
                <span className="mn-rook-bar-stories__image" aria-hidden>
                  {story.image ? (
                    <img src={story.image} alt="" loading="lazy" />
                  ) : (
                    <span>{story.label}</span>
                  )}
                </span>
              ) : null}
              <span className="mn-rook-bar-stories__meta">
                {[story.label, story.meta].filter(Boolean).join(' · ')}
              </span>
              <span className="mn-rook-bar-stories__title">{story.title}</span>
            </PanelLink>
          </li>
        ))}
      </ul>
    );

  return (
    <div className="mn-rook-bar-stories" data-layout={layout}>
      <RookBarLinkList
        links={sections}
        newTabLabel={messages.newTab}
        className="mn-rook-bar-stories__sections"
      />
      {shown !== undefined && !(layout === 'mobile' && (shown === null || shown.length === 0)) ? (
        <div className="mn-rook-bar-stories__latest">
          <p id={headingId} className="mn-rook-bar__eyebrow">
            {messages.latest}
          </p>
          {latest}
        </div>
      ) : null}
    </div>
  );
}
