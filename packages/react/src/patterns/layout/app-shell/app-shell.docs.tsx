import { AppShell, IdentityBar, RookRail } from '@rookcom/monarch-react/shell';
import { PageHeader } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { AppMark, CalendarIcon, HomeIcon, InboxIcon } from '../../../internal/docs-demo.js';

function Demo() {
  return (
    <div
      style={{
        width: '100%',
        height: '26rem',
        overflow: 'hidden',
        border: '1px solid var(--mn-color-border-default)',
        borderRadius: 'var(--mn-radius-lg)',
        position: 'relative',
      }}
    >
      <AppShell
        style={{ height: '100%', minHeight: 0 }}
        mainId="app-shell-demo-main"
        mainRole="region"
        mainProps={{ 'aria-label': 'Example app content' }}
        skipLinkLabel="Skip to example content"
        rail={
          <RookRail
            style={{ height: '100%' }}
            persist={false}
            messages={{ nav: 'Main (app shell example)' }}
            brand={{ label: 'Acme Desk home', href: '#home', icon: <AppMark /> }}
            currentPath="/inbox"
            sections={[
              {
                key: 'main',
                items: [
                  { key: 'home', label: 'Home', icon: <HomeIcon />, href: '/home' },
                  { key: 'inbox', label: 'Inbox', icon: <InboxIcon />, href: '/inbox', badge: 12 },
                  { key: 'calendar', label: 'Calendar', icon: <CalendarIcon />, href: '/calendar' },
                ],
              },
            ]}
            account={{ name: 'Ada Lovelace', onSignOut: () => {} }}
          />
        }
        header={<IdentityBar product={{ name: 'Acme Desk' }} />}
      >
        <div style={{ padding: 'var(--mn-space-6)' }}>
          <PageHeader title="Inbox" description="12 open conversations" />
          {Array.from({ length: 12 }, (_, i) => (
            <p key={i}>
              <a href={`#conversation-${i + 1}`}>Conversation {i + 1}</a>
            </p>
          ))}
        </div>
      </AppShell>
    </div>
  );
}

export default {
  id: 'app-shell',
  title: 'AppShell',
  summary:
    'The signed-in Rook layout: rail, optional identity bar and main, with a skip link and contained scrolling.',
  sections: {
    purpose: {
      summary:
        'Lays out every signed-in Rook app the same way and gets the page-level accessibility right once: the skip link, the landmark order and a main region that scrolls on its own. Needed by: app, calendar, friends, stride, whose layouts each hand-offset content by the rail width and had no skip link.',
      useWhen: ['The root layout of a signed-in app (`app/(workspace)/layout.tsx`).'],
      avoidWhen: ['Public and marketing pages.', 'A two-pane area inside a page: use SplitLayout.'],
    },
    designPrinciples: {
      items: [
        'No magic offsets: the rail is a grid column, so content never needs `padding-left: var(--rail-width)`.',
        'The first Tab stop skips the navigation.',
        'One scrolling region by default, so the rail and header never scroll away and the page never double-scrolls.',
        'Server-safe: no state, no effects, no event handlers.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['Forwards its ref to the root `<div>`. `children` render inside `<main>`.'],
    },
    typedProperties: {
      source: 'typescript',
      notes: ['`mainProps` passes attributes (an `aria-label`, a class) to `<main>`.'],
    },
    variants: {
      items: [
        {
          prop: 'mainRole',
          values: ['main', 'region'],
          description:
            '`region` renders the content as a named `<div role="region">`, for a shell embedded in a page that already has `<main>`.',
        },
        {
          prop: 'scroll',
          values: ['main', 'page'],
          description:
            '`main` (default): the shell fills the viewport and `<main>` scrolls. `page`: the document scrolls; the rail and identity bar stay sticky.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Scroll mode',
          attribute: 'data-scroll="main|page"',
          description: 'On the root.',
        },
        {
          name: 'With rail / header',
          attribute: 'data-rail, data-header',
          description: 'Set when the slot is used.',
        },
      ],
    },
    interaction: {
      items: [
        'The skip link appears when focused and moves focus to `<main>` (it has `tabindex="-1"`, so it is not a Tab stop itself).',
        'With `scroll="main"`, scrolling stops at the main region’s edges (`overscroll-behavior: contain`).',
        'Printing hides the rail, header and skip link and lets the content flow.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab (first)', action: 'Shows the "Skip to content" link.' },
        { keys: 'Enter', action: 'On the skip link: focus moves to the main region.' },
      ],
    },
    accessibility: {
      role: 'Landmarks in order: the rail (complementary + navigation), banner (identity bar), main',
      name: '`<main>` needs no name; add one with `mainProps["aria-label"]` if the page has several regions.',
      items: [
        'Adds the skip link every audited app was missing (WCAG 2.4.1 Bypass Blocks).',
        'The main region shows a focus outline only when reached from the keyboard.',
        'With `scroll="main"`, keyboard users scroll main by moving through its focusable content (or after the skip link focuses it); a page with no focusable content at all should use `scroll="page"`.',
      ],
    },
    responsive: {
      items: [
        '768px and up: rail column on the left, identity bar and main to its right.',
        'Below 768px: one column; the rail becomes its top bar (with the drawer), then the identity bar, then main.',
        'Uses `100dvh`, so mobile browser toolbars do not hide content.',
      ],
    },
    theme: {
      items: ['Canvas background and text tokens; the rail and bar bring their own surfaces.'],
    },
    examples: {
      items: [
        {
          title: 'Rail, identity bar and a scrolling main region',
          stack: true,
          render: () => <Demo />,
          code: `// app/(workspace)/layout.tsx (this preview uses mainRole="region": the docs page has its own <main>)
<AppShell
  rail={<RookRail {...rail} currentPath={pathname} linkComponent={Link} />}
  header={<IdentityBar product={{ name: 'Acme Desk' }} />}
>
  {children}
</AppShell>`,
        },
      ],
    },
    doDont: {
      do: [
        'Use it once, at the root of the signed-in layout.',
        'Start each page with a heading inside main (PageHeader).',
        'Use `scroll="page"` for long reading or printable pages.',
      ],
      dont: [
        'Do not add your own rail offset or `overflow` on `body`.',
        'Do not render a second `<main>` inside it.',
      ],
    },
    related: {
      items: [
        { id: 'rook-rail', reason: 'The rail slot.' },
        { id: 'identity-bar', reason: 'The header slot.' },
        { id: 'page-header', reason: 'The first thing inside main.' },
        { id: 'split-layout', reason: 'Two panes inside a page.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
    },
    testing: {
      unit: [
        'Landmarks in order: navigation, banner, main; default scroll mode.',
        'Skip link is the first Tab stop and targets main, which is not a Tab stop.',
        'Custom main id, skip text and main props; page scrolling; no rail or header; embedded as a named region.',
        'Server render.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/app-shell',
        'Cross-browser axe: /components/app-shell',
      ],
    },
  },
} satisfies ComponentDocs;
