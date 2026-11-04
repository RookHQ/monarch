import {
  Badge,
  BackLink,
  Breadcrumb,
  Button,
  CopyButton,
  PageHeader,
  TabsLink,
  TabsNav,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { PlusIcon, TrashIcon } from '../../../internal/docs-demo.js';

export default {
  id: 'page-header',
  title: 'PageHeader',
  summary:
    'Top of a main view: the single h1, back link or breadcrumbs, status, details, page actions with overflow, and tabs.',
  sections: {
    purpose: {
      summary:
        'Lays out the top of every main view: an optional banner, back link and breadcrumbs, the page’s h1 with status and meta, a details line and description, page-level actions (a primary action plus secondary actions that collapse into a "More actions" menu), and section tabs.',
      useWhen: [
        'At the top of each main view, once per page.',
        'A publishable entity (a page, a domain, a form) needs its status, address and Publish action in one row.',
        'A page embedded in another shell (App Settings) still needs its heading: use `embedded`.',
      ],
      avoidWhen: [
        'Headings for sections inside a page: use SectionHeader, SectionCard or SettingsSection.',
        'The header of one record (a contact, a ticket): use RecordHeader, which adds an avatar, facts and prev/next.',
        'Dialogs and panels: they have their own headers.',
      ],
    },
    designPrinciples: {
      items: [
        'One h1 per page: `title` always renders as the heading (`h1` unless `headingLevel="h2"`).',
        'Actions in the header apply to the whole page; keep one primary action and let the rest overflow.',
        'Slots, not settings: banner, back, breadcrumbs, status, meta, details, actions and tabs take any nodes.',
        'Responsive by CSS alone: the server and the client render the same markup at every width.',
        'Print-friendly: actions and tabs are hidden when printing.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<header>`, forwards its ref and passes native attributes through.',
        'Server-safe: the header has no state. Only the overflow menu (a DropdownMenu) is a client component; `secondaryActions` with `onSelect` must come from a client component, `href` actions work from server components.',
        '`PageHeaderAction` is `{ id, label, icon?, onSelect?, href?, disabled?, tone? }`.',
        'CSS variables: `--mn-page-header-sticky-top` (sticky offset, default 0) and `--mn-page-header-padding-inline` (inline padding, for example the icon-rail offset, default 0).',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'banner / back / breadcrumbs / eyebrow / status / meta / details / actions / tabs',
          values: ['omitted', 'provided'],
          description:
            'Each slot renders its wrapper only when provided, in that order from the top.',
        },
        {
          prop: 'secondaryActions + maxVisibleActions',
          values: ['0', '1', '2 (default)', 'n'],
          description:
            'How many secondary actions show as buttons from 640px; the rest are in the More menu. Below 640px all are in the menu.',
        },
        {
          prop: 'sticky',
          values: ['false', 'true'],
          description:
            'Sticks to the top of the scroll container with a blurred, slightly translucent canvas background and a bottom border. Exposed as `data-sticky`.',
        },
        {
          prop: 'embedded',
          values: ['false', 'true'],
          description:
            'Hides the title visually (it stays the heading for screen readers) for a page inside another shell. Exposed as `data-embedded`.',
        },
        {
          prop: 'headingLevel',
          values: ['h1', 'h2'],
          description: 'The heading element. Default `h1`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Static', description: 'The header has no state; slot content owns its states.' },
        {
          name: 'Overflowing actions',
          attribute: 'data-overflow on the More button',
          description:
            'Set when more secondary actions exist than `maxVisibleActions`; without it the More button only shows below 640px.',
        },
        {
          name: 'Disabled action',
          description:
            'A disabled action is a disabled button, and a disabled menu item (`aria-disabled`); a disabled `href` action renders as a button so it cannot be followed.',
        },
      ],
    },
    interaction: {
      items: [
        'Secondary action buttons call `onSelect` or follow `href`.',
        'The More button opens a menu with every secondary action; on wide screens the items already shown as buttons are hidden from it.',
        'Choosing a menu item calls its `onSelect` (or follows its link), closes the menu and returns focus to the More button.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through back link, breadcrumbs, actions and tabs in reading order.',
        },
        { keys: 'Enter / Space / ArrowDown', action: 'On the More button, opens the menu.' },
        { keys: 'ArrowUp / ArrowDown', action: 'Moves between menu items.' },
        { keys: 'Escape', action: 'Closes the menu and returns focus to the More button.' },
      ],
    },
    accessibility: {
      role: 'banner landmark only when it is not inside main, article or section (native <header> rules)',
      name: 'The heading is the page’s main heading.',
      items: [
        '`title` renders as the h1, so screen-reader heading navigation lands on it; with `embedded` it is visually hidden, not removed.',
        'Pass a Breadcrumb in `breadcrumbs` for the navigation landmark and current-page marking.',
        'The More button is an icon button with a required name; give it the object: `moreActionsLabel="More actions for Website refresh"`.',
        'Actions keep visible text labels; icons in actions are decorative.',
        'The live examples on this page render with `headingLevel="h2"` because the docs page has its own h1; in an app use the default h1.',
      ],
    },
    responsive: {
      items: [
        'The title block and the actions sit in a wrapping row; actions move under the title when space runs out.',
        'Below 640px secondary actions collapse into the More menu, so at 320px the row holds the primary action and one icon button.',
        'Long titles and details wrap anywhere instead of overflowing.',
        'The description is limited to `--mn-layout-reading-max` for readable line length.',
      ],
    },
    theme: {
      items: [
        'Title and muted text use Monarch typography and colour tokens, so every theme applies.',
        'The sticky backdrop mixes the canvas token with transparency; with `prefers-contrast: more` or without backdrop-filter support it is solid.',
      ],
    },
    examples: {
      items: [
        {
          title: 'With breadcrumb, status and actions',
          stack: true,
          render: () => (
            <PageHeader
              headingLevel="h2"
              eyebrow={
                <Breadcrumb
                  items={[
                    { label: 'Projects', href: '#/components/page-header' },
                    { label: 'Website refresh' },
                  ]}
                />
              }
              title="Website refresh"
              meta={<Badge tone="success">Active</Badge>}
              description="Tasks, owners and dates for this project."
              actions={
                <>
                  <Button>Share</Button>
                  <Button variant="primary">New task</Button>
                </>
              }
            />
          ),
          code: `<PageHeader
  eyebrow={<Breadcrumb items={[{ label: 'Projects', href: '/projects' }, { label: 'Website refresh' }]} />}
  title="Website refresh"
  meta={<Badge tone="success">Active</Badge>}
  description="Tasks, owners and dates for this project."
  actions={<><Button>Share</Button><Button variant="primary">New task</Button></>}
/>`,
        },
        {
          title: 'Publishable entity: back link, status, address and overflow actions',
          stack: true,
          render: () => (
            <PageHeader
              headingLevel="h2"
              back={<BackLink href="#/components/page-header">All pages</BackLink>}
              title="Spring launch"
              status={<Badge tone="success">Published</Badge>}
              details={
                <>
                  <span>launch.example.com/spring</span>
                  <CopyButton
                    value="https://launch.example.com/spring"
                    iconOnly
                    size="xs"
                    label="Copy address"
                  />
                  <span>Updated 5 minutes ago</span>
                </>
              }
              secondaryActions={[
                { id: 'preview', label: 'Preview', href: '#/components/page-header' },
                { id: 'duplicate', label: 'Duplicate', onSelect: () => {} },
                { id: 'unpublish', label: 'Unpublish', onSelect: () => {} },
                {
                  id: 'delete',
                  label: 'Delete page',
                  icon: <TrashIcon />,
                  tone: 'danger',
                  onSelect: () => {},
                },
              ]}
              moreActionsLabel="More actions for Spring launch"
              primaryAction={<Button variant="primary">Publish changes</Button>}
            />
          ),
          code: `<PageHeader
  back={<BackLink href="/pages">All pages</BackLink>}
  title="Spring launch"
  status={<Badge tone="success">Published</Badge>}
  details={<>
    <span>launch.example.com/spring</span>
    <CopyButton value="https://launch.example.com/spring" iconOnly size="xs" label="Copy address" />
    <span>Updated 5 minutes ago</span>
  </>}
  secondaryActions={[
    { id: 'preview', label: 'Preview', href: '/pages/spring/preview' },
    { id: 'duplicate', label: 'Duplicate', onSelect: duplicate },
    { id: 'unpublish', label: 'Unpublish', onSelect: unpublish },
    { id: 'delete', label: 'Delete page', icon: <TrashIcon />, tone: 'danger', onSelect: confirmDelete },
  ]}
  moreActionsLabel="More actions for Spring launch"
  primaryAction={<Button variant="primary">Publish changes</Button>}
/>`,
        },
        {
          title: 'With section tabs',
          stack: true,
          render: () => (
            <PageHeader
              headingLevel="h2"
              title="Billing"
              description="Plan, invoices and payment methods."
              primaryAction={
                <Button variant="primary" leadingIcon={<PlusIcon />}>
                  Add payment method
                </Button>
              }
              tabs={
                <TabsNav aria-label="Billing sections">
                  <TabsLink href="#/components/page-header" active>
                    Overview
                  </TabsLink>
                  <TabsLink href="#/components/page-header">Invoices</TabsLink>
                  <TabsLink href="#/components/page-header">Payment methods</TabsLink>
                </TabsNav>
              }
            />
          ),
          code: `<PageHeader
  title="Billing"
  description="Plan, invoices and payment methods."
  primaryAction={<Button variant="primary" leadingIcon={<PlusIcon />}>Add payment method</Button>}
  tabs={
    <TabsNav aria-label="Billing sections">
      <TabsLink href="/billing" active>Overview</TabsLink>
      <TabsLink href="/billing/invoices">Invoices</TabsLink>
      <TabsLink href="/billing/methods">Payment methods</TabsLink>
    </TabsNav>
  }
/>`,
        },
      ],
    },
    doDont: {
      do: [
        'Use exactly one PageHeader per page.',
        'Keep a single primary action; put the rest in `secondaryActions` so they collapse on phones.',
        'Name the More button after the page when several menus share a screen.',
      ],
      dont: [
        'Do not add another h1 elsewhere on the page.',
        'Do not put row- or section-level actions in the page header.',
        'Do not pass `onSelect` callbacks from a server component; use `href` actions there.',
      ],
    },
    related: {
      items: [
        { id: 'breadcrumb', reason: 'The usual `breadcrumbs` content.' },
        { id: 'back-link', reason: 'The `back` slot, or built for you by `backHref`.' },
        { id: 'tabs', reason: 'Section navigation in the `tabs` slot (TabsNav for routes).' },
        { id: 'record-header', reason: 'Header for one record: avatar, facts, prev/next.' },
        { id: 'toolbar', reason: 'List tools that sit under the header.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['back-link', 'button', 'dropdown-menu'],
      notes: ['The sticky backdrop uses `--mn-z-sticky` and `--mn-color-bg-canvas`.'],
    },
    testing: {
      unit: [
        'Renders the title as the single h1 with actions.',
        'Renders banner, back link, breadcrumbs, status, details and primary action in order.',
        'A custom `back` node wins over `backHref`.',
        'Shows the first secondary actions as buttons and lists every action in the More menu, marking the inline ones; keyboard selection calls `onSelect` and returns focus.',
        'Keeps the More button for narrow screens when every action fits; disabled actions are disabled in both places.',
        'Embedded mode hides the heading visually; `headingLevel` changes the level.',
        'Sticky mode, ref forwarding and native attributes.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/page-header',
        'Cross-browser axe: /components/page-header',
      ],
    },
  },
} satisfies ComponentDocs;
