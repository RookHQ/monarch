import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { Widget } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function StatesDemo() {
  const [state, setState] = useState<'ready' | 'loading' | 'error' | 'empty'>('ready');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', maxWidth: '28rem' }}>
      <div style={{ display: 'flex', gap: 'var(--mn-space-2)', flexWrap: 'wrap' }}>
        {(['ready', 'loading', 'error', 'empty'] as const).map((s) => (
          <Button
            key={s}
            size="sm"
            variant={s === state ? 'primary' : 'secondary'}
            onClick={() => setState(s)}
          >
            {s}
          </Button>
        ))}
      </div>
      <Widget
        title="Open requests"
        period="Last 30 days"
        loading={state === 'loading'}
        error={state === 'error' ? 'Requests did not load.' : undefined}
        onRetry={() => setState('ready')}
        empty={state === 'empty'}
      >
        <p style={{ margin: 0 }}>42 open, 7 waiting on a reply.</p>
      </Widget>
    </div>
  );
}

export default {
  id: 'widget',
  title: 'Widget',
  summary:
    'The frame of a dashboard widget: title, period, actions, menu and its own loading, empty and error states.',
  sections: {
    purpose: {
      summary:
        'Gives every dashboard widget the same frame: a titled region with the period it covers, header actions, an overflow menu, a view-all link and an updated-at line, plus loading, empty and error states that stay inside the widget so one slow query never blocks the page. Needed by: Pulse, Tickets, Dots, Recruit, GTM, Atlas, Intellect, Stride, People.',
      useWhen: [
        'A home or dashboard page is made of independent sections that load on their own.',
        'A section needs a "View all" link, a period label or an overflow menu (refresh, export, remove).',
      ],
      avoidWhen: [
        'A single figure: use StatCard.',
        'A settings group: use SettingsSection.',
        'A generic content container with no data states: use Card or SectionCard.',
      ],
    },
    designPrinciples: {
      items: [
        'One failure stays local: the widget shows its own error with Try again, the rest of the dashboard keeps working.',
        'Say what the figures cover: the period sits beside the title, the updated time in the footer.',
        'The title names the region, so screen reader users can jump between widgets by landmark.',
        'Quiet chrome: a thin border and small heading so the data, not the frame, carries the page.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<section>`; native attributes pass through and the ref is forwarded to it.',
        '`WIDGET_MESSAGES` holds the English defaults for `messages`.',
        '`span` only matters inside a DashboardGrid, which reads `data-span`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (People `components/dashboards/Widget.tsx`, Dots/Tickets/Stride `Section`/`SectionCard`, Intellect `dashboard/widgets.tsx` `Card`): `title` → `title`; `description` → `description`; `meta` (Dots, Stride) → `period`; `action={<ViewAll href=… />}` → `viewAllHref`; `actions` → `actions`; `bare` → `variant="plain"`; `skeleton="h-40"` → `skeleton` (a node); the query states (`q.isLoading`, `q.error` + `refetch`) → `loading`, `error` + `onRetry`; "Updated N min ago" → `updatedAt`; `className="lg:col-span-3"` → `span` inside DashboardGrid. The data fetching (`useWidget`) stays in the app.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['card', 'plain'],
          description:
            '`card` (default) has a border and surface; `plain` drops both for a widget inside another surface.',
        },
        {
          prop: 'span',
          values: ['1', '2', '3', '4', 'full'],
          description:
            'Columns spanned inside a DashboardGrid on wide screens. Exposed as `data-span`.',
        },
        {
          prop: 'headingLevel',
          values: ['2', '3', '4', '5', '6'],
          description: 'Level of the title. Default 2, under the page `h1`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Ready', description: 'The body shows the children.' },
        {
          name: 'Loading',
          attribute: 'data-state="loading"',
          description:
            'Three text placeholders (or `skeleton`) named "Loading {title}"; the region has `aria-busy`.',
        },
        {
          name: 'Error',
          attribute: 'data-state="error"',
          description: 'The body is replaced by the error, with Try again when `onRetry` is set.',
        },
        {
          name: 'Empty',
          attribute: 'data-state="empty"',
          description: 'A one-line empty state, or the node passed as `empty`.',
        },
      ],
    },
    interaction: {
      items: [
        'The menu button opens a DropdownMenu; choosing an item calls its `onSelect` and focus returns to the button.',
        'Try again calls `onRetry`; return the refetch promise and the button stays busy until it settles.',
        'The view-all link uses `linkComponent` (a router link) when given.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through header actions, the view-all link, the menu button and the body.',
        },
        { keys: 'Enter / Space', action: 'Opens the menu or activates the focused control.' },
        {
          keys: 'Arrow keys, Escape',
          action: 'Move through and close the open menu (DropdownMenu).',
        },
      ],
    },
    accessibility: {
      role: 'region',
      name: 'The title, through aria-labelledby; the description is the region’s accessible description.',
      items: [
        'The title is a real heading (`h2` by default) so the page outline lists every widget.',
        'The menu button is named "More actions for {title}"; the view-all link "View all: {title}", which starts with its visible text.',
        'The loading placeholder is an image named "Loading {title}" and the region sets `aria-busy`.',
        'An error without a retry is an alert; with a retry it uses InlineErrorRetry, which announces retries politely.',
      ],
    },
    responsive: {
      items: [
        'The header wraps: actions drop under the title on narrow widths, so there is no horizontal scroll at 390px.',
        'On coarse pointers the view-all link and menu button are at least 44px.',
      ],
    },
    theme: {
      items: [
        'Surface `--mn-color-bg-surface`, border `--mn-color-border-default`, footer rule `--mn-color-border-subtle`.',
        'Title `--mn-color-text-default`; period, description and footer `--mn-color-text-muted`; the link `--mn-color-text-link`.',
        'In forced colours the border is CanvasText and the link LinkText. Header actions are hidden when printing.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Widget with period, view-all link, menu and updated time',
          stack: true,
          render: () => (
            <Widget
              style={{ maxWidth: '28rem' }}
              title="Team load"
              period="Open now"
              viewAllHref="#reports"
              menu={[
                { id: 'refresh', label: 'Refresh', onSelect: () => {} },
                { id: 'export', label: 'Export CSV', onSelect: () => {} },
                {
                  id: 'remove',
                  label: 'Remove from dashboard',
                  onSelect: () => {},
                  tone: 'danger',
                  separatorBefore: true,
                },
              ]}
              updatedAt="2026-11-07T09:58:00Z"
              now="2026-11-07T10:00:00Z"
            >
              <p style={{ margin: 0 }}>12 open tickets across 4 people.</p>
            </Widget>
          ),
        },
        { title: 'Loading, error and empty states', stack: true, render: () => <StatesDemo /> },
        {
          title: 'Description and footer note',
          stack: true,
          render: () => (
            <Widget
              style={{ maxWidth: '28rem' }}
              title="Waiting for you"
              description="Approve or send back without leaving this page."
              footer="Requests older than 30 days are archived."
            >
              <p style={{ margin: 0 }}>3 requests waiting.</p>
            </Widget>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give each widget its own loading and error state so one slow query never blocks the page.',
        'Say the period ("Last 30 days") whenever figures cover one.',
        'Use `danger` menu items for removals and confirm them.',
      ],
      dont: [
        'Do not put a whole page of controls in `actions`; one or two at most, the rest in the menu.',
        'Do not show stale data without saying when it was updated.',
        'Do not nest widgets inside widgets; use `variant="plain"` sections instead.',
      ],
    },
    related: {
      items: [
        {
          id: 'dashboard-grid',
          reason: 'Lays out widgets responsively and lets people rearrange them.',
        },
        { id: 'stat-card', reason: 'A single key figure.' },
        { id: 'section-card', reason: 'A titled content container without data states.' },
        { id: 'activity-widget', reason: 'A ready-made Widget with recent activity.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'dropdown-menu',
        'empty-state',
        'inline-error-retry',
        'relative-time',
        'skeleton',
      ],
    },
    testing: {
      unit: [
        'Region named by its title and described by its description; props and ref forwarded.',
        'Heading level and span.',
        'Loading placeholder with aria-busy; error with Try again; default alert error; default and custom empty.',
        'Overflow menu opens from the keyboard, runs an item and returns focus.',
        'View-all link named with the title; updated time and footer; messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
