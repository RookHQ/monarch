import { Button } from '@rookcom/monarch-react';
import { ChangelogFeed } from '@rookcom/monarch-react/marketing';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './changelog-feed.fixture.json';

type Tag = { label: string; tone?: 'accent' };
const tag = (t: Tag) => (t.tone ? { label: t.label, tone: t.tone } : t.label);
const [exported, search, fix] = fixture.data.entries as [
  {
    id: string;
    date: string;
    version: string;
    title: string;
    tags: [{ label: string; tone: 'accent' }, Tag];
    body: string;
    href: string;
  },
  { id: string; date: string; title: string; tags: [Tag]; body: string },
  { id: string; date: string; title: string; tags: [Tag] },
];

const sampleEntries = [
  {
    id: exported.id,
    date: exported.date.slice(0, 10),
    version: exported.version,
    title: exported.title,
    tags: exported.tags.map(tag),
    body: <p>{exported.body}</p>,
    href: exported.href,
  },
  {
    id: search.id,
    date: search.date.slice(0, 10),
    title: search.title,
    tags: search.tags.map(tag),
    body: <p>{search.body}</p>,
  },
  {
    id: fix.id,
    date: fix.date.slice(0, 10),
    title: fix.title,
    tags: fix.tags.map(tag),
  },
];

export default {
  id: 'changelog-feed',
  title: 'ChangelogFeed',
  summary: 'A dated timeline of release notes with tags, versions, permalinks and an RSS link.',
  sections: {
    purpose: {
      summary:
        'Gives every product the same public "what changed" page instead of a hand-built timeline per site. Needed by: Dots, Pulse, Ranker and Signal (`src/app/[locale]/(marketing)/changelog/page.tsx` in each frontend), Recruit (`rookrecruit-frontend/src/app/[locale]/(marketing)/ai/changelog/page.tsx`).',
      useWhen: [
        'A public changelog or release-notes page.',
        'A "What’s new" panel listing recent changes inside an app.',
      ],
      avoidWhen: [
        'An activity log of what people did: use ActivityFeed or the audit log.',
        'A single launch announcement: use AnnouncementBar or a MarketingHero.',
      ],
    },
    designPrinciples: {
      items: [
        'Plain, dated facts: date first, then a headline that says what changed.',
        'Tags are words, not colour codes; status tones only when the tag means that status.',
        'Each entry has a stable id, so it can be linked to directly.',
        'Dates print the same on the server and in the browser (UTC by default).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<section>`.',
        '`defaultChangelogFeedMessages` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app pages’ `ENTRIES` arrays: `date` → `date` (prefer ISO strings over "April 22, 2026"), `tag` → `tags: [tag]`, `icon` → `icon`, `title` → `title`, `body` → `body`; add a stable `id` per entry.',
        'The app pages are hand-built from Section and Tailwind classes, not shadcn/ui; there is no shadcn prop mapping.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'headingLevel',
          values: ['2', '3'],
          description: 'Level of the feed title; entry headlines use the next level.',
        },
      ],
      notes: ['There is one visual style: a vertical timeline.'],
    },
    states: {
      items: [
        { name: 'Empty', description: 'No entries: `messages.empty` is shown.' },
        { name: 'With RSS', description: '`rssHref` (safe URLs only) or `rssSlot` in the header.' },
        { name: 'Unparseable date', description: 'Shown as given, without a `<time>` element.' },
      ],
    },
    interaction: {
      items: [
        'Entries are read-only; the RSS link, Read more links and footer keep their own behaviour.',
        'Entry ids allow deep links such as `/changelog#export`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the RSS link, each Read more link and the footer.' },
        { keys: 'Enter', action: 'Follows the focused link.' },
      ],
    },
    accessibility: {
      role: 'region (section) with an ordered list of articles',
      name: 'The `title`, or `messages.listLabel` ("Changelog") without one.',
      items: [
        'Each entry is an article named by its headline.',
        'Dates are `<time datetime>` elements.',
        'Read more links are described by their entry headline, so a list of them is distinguishable.',
        'Timeline dots and icons are decorative.',
      ],
    },
    responsive: {
      items: [
        'Single column; the timeline rail stays at the start edge at 390px.',
        'Header wraps the RSS link under the title on narrow screens.',
        'Links are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Accent, text, link and border tokens; works in light, dark and every product accent.',
        'Uses logical properties, so the rail moves to the right in RTL.',
        'Forced-colours mode draws the rail, dots and version chips in CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Changelog with RSS',
          render: () => (
            <ChangelogFeed
              title={fixture.data.title}
              description={fixture.data.description}
              rssHref="#rss"
              entries={sampleEntries}
              locale={fixture.data.locale}
            />
          ),
        },
        {
          title: 'Compact in-app list with a footer',
          render: () => (
            <ChangelogFeed
              title="What’s new"
              headingLevel={3}
              entries={sampleEntries.slice(1)}
              dateFormat={{ day: 'numeric', month: 'short' }}
              locale="en-GB"
              footer={<Button variant="secondary">Show older updates</Button>}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write headlines that say what changed for the reader.',
        'Pass ISO dates so every reader sees the same day.',
        'Keep tag vocabulary small and consistent (New, Improved, Fix).',
      ],
      dont: [
        'Do not put marketing claims or unsourced numbers in entries.',
        'Do not reuse ids; they are deep-link anchors.',
        'Do not use colour as the only difference between tags.',
      ],
    },
    related: {
      items: [
        { id: 'announcement-bar', reason: 'Point readers at the newest entry.' },
        { id: 'badge', reason: 'The tags.' },
        { id: 'marketing-section-blocks', reason: 'Wrap the feed in a MarketingSection.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['badge'] },
    testing: {
      unit: [
        'Section named by the title; ordered list of articles with dates, versions and tags; axe clean.',
        'Safe RSS and Read more links reachable by Tab; Read more described by the headline; axe clean.',
        'Unsafe URLs dropped; RSS slot rendered.',
        'Empty message, footer, fallback name, translated text, ref forwarding.',
        'Locale, time zone and date format; unparseable dates kept as text; heading level.',
        'Server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
