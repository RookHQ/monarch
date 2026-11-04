import { useState } from 'react';
import { Badge, Button, SectionHeaderLink } from '@rookcom/monarch-react';
import { SectionCard, type SectionCardState } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { ClockIcon, PlusIcon } from '../../../internal/docs-demo.js';

const tasks = ['Draft the launch brief', 'Review pricing copy', 'Book the kickoff call'];

function StatesDemo() {
  const [state, setState] = useState<SectionCardState>('loading');
  return (
    <div style={{ display: 'grid', gap: '0.75rem', width: '100%' }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
        {(['ready', 'loading', 'empty', 'error'] as const).map((s) => (
          <Button key={s} size="xs" aria-pressed={state === s} onClick={() => setState(s)}>
            {s}
          </Button>
        ))}
      </div>
      <SectionCard
        title="Upcoming invoices"
        state={state}
        emptyDescription="Invoices you schedule appear here."
        errorDescription="Check your connection and try again."
        onRetry={() => setState('ready')}
      >
        <p style={{ margin: 0 }}>3 invoices due this month.</p>
      </SectionCard>
    </div>
  );
}

export default {
  id: 'section-card',
  title: 'SectionCard',
  summary:
    'A titled card for one section: header with actions and "View all", body, footer, and built-in loading, empty and error states.',
  sections: {
    purpose: {
      summary:
        'Puts one section of a dashboard, detail or settings page in a card: a SectionHeader (title, description, meta, actions, "View all" link), the body, an optional footer, and the loading, empty and error states every data-backed section needs. It can be collapsible.',
      useWhen: [
        'Dashboard widgets and detail-page panels that load their own data.',
        'A titled card with header actions or a "View all" link.',
        'A table or list that runs edge to edge inside a titled card (`flush`).',
      ],
      avoidWhen: [
        'A group of settings rows with Save/Discard: use SettingsSection.',
        'An unboxed section title: use SectionHeader.',
        'A plain surface without a title: use Card.',
      ],
    },
    designPrinciples: {
      items: [
        'Always a labelled region: the heading names the `<section>`, so landmark navigation lists it.',
        'One state at a time: the body shows the content, a skeleton, an empty state or an error, never two.',
        'Refreshing is not loading: `busy` keeps the content and shows a spinner by the title.',
        'The heading level follows the page outline (`headingLevel`), never the visual size.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<section aria-labelledby>` and forwards its ref; native attributes pass through.',
        'The header is a SectionHeader without children; `title`, `description`, `meta`, `actions`, `link` and `headingLevel` are passed to it.',
        'States reuse LoadingState, EmptyState and ErrorState (compact). Pass `empty` for a custom empty state with actions.',
        'Exported from `@rookcom/monarch-react/patterns`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'state',
          values: ['ready', 'loading', 'empty', 'error'],
          description: 'What the body shows. Exposed as `data-state`.',
        },
        {
          prop: 'tone',
          values: ['default', 'danger'],
          description: '`danger` uses the danger border and title colour. Exposed as `data-tone`.',
        },
        {
          prop: 'flush',
          values: ['false', 'true'],
          description:
            'Removes the body padding and adds a divider, for tables and lists. `data-flush`.',
        },
        {
          prop: 'collapsible',
          values: ['false', 'true'],
          description: 'The title becomes a disclosure button. `data-collapsible`, `data-open`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Ready', attribute: 'data-state="ready"', description: 'Shows the children.' },
        {
          name: 'Loading',
          attribute: 'data-state="loading", aria-busy',
          description: 'A skeleton (`loadingVariant`) with a polite `loadingLabel` announcement.',
        },
        {
          name: 'Empty',
          attribute: 'data-state="empty"',
          description: '`empty`, or a compact EmptyState from `emptyTitle` and `emptyDescription`.',
        },
        {
          name: 'Error',
          attribute: 'data-state="error"',
          description:
            'A compact ErrorState with `errorTitle`, `errorDescription` and a retry button when `onRetry` is given.',
        },
        {
          name: 'Busy',
          attribute: 'aria-busy',
          description: 'Refreshing in place: a spinner after the title; content stays.',
        },
        {
          name: 'Collapsed',
          attribute: 'data-open absent',
          description: 'The body and footer are hidden; the header stays.',
        },
      ],
    },
    interaction: {
      items: [
        'Header actions and the "View all" link behave as usual.',
        'With `collapsible`, activating the title toggles the body (uncontrolled with `defaultOpen`, controlled with `open` and `onOpenChange`).',
        'The error state’s retry button calls `onRetry`; show `retrying` while it runs.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves to the title toggle (when collapsible), header actions, link, then the body.',
        },
        { keys: 'Enter / Space', action: 'On the title toggle, expands or collapses the section.' },
      ],
    },
    accessibility: {
      role: 'region (section with aria-labelledby)',
      name: 'The section title.',
      items: [
        'The heading labels the section; set `headingId` to point other elements at it.',
        'The collapsible toggle follows the APG disclosure pattern: a button inside the heading with `aria-expanded` and `aria-controls`.',
        'The icon is decorative and hidden from assistive technology.',
        'Loading and busy set `aria-busy`; the loading label is announced politely.',
      ],
    },
    responsive: {
      items: [
        'Header actions wrap under the title when space runs out (SectionHeader).',
        'Below 480px the header, body and footer padding tighten.',
        'The footer wraps its content and keeps it end-aligned.',
      ],
    },
    theme: {
      items: [
        'Surface, borders, footer and danger colours use Monarch semantic tokens, so every theme applies.',
        'The chevron rotation is instant with reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Header actions, "View all" and footer',
          stack: true,
          render: () => (
            <SectionCard
              title="Open tasks"
              description="Assigned to you this week."
              meta={<Badge>3</Badge>}
              icon={<ClockIcon />}
              actions={
                <Button size="xs" leadingIcon={<PlusIcon />}>
                  Add
                </Button>
              }
              link={
                <SectionHeaderLink href="#/components/section-card">
                  View all tasks
                </SectionHeaderLink>
              }
              footer={<span>Updated just now</span>}
            >
              <ul style={{ margin: 0, paddingInlineStart: '1.25rem' }}>
                {tasks.map((t) => (
                  <li key={t}>{t}</li>
                ))}
              </ul>
            </SectionCard>
          ),
        },
        {
          title: 'Loading, empty and error states',
          stack: true,
          render: () => <StatesDemo />,
        },
        {
          title: 'Collapsible and danger tone',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: '1rem', width: '100%' }}>
              <SectionCard title="Activity" collapsible defaultOpen={false}>
                <p style={{ margin: 0 }}>Ada changed the status to Active.</p>
              </SectionCard>
              <SectionCard
                title="Delete this project"
                tone="danger"
                description="Deleting the project removes its tasks and files."
                actions={
                  <Button variant="danger" size="sm">
                    Delete project
                  </Button>
                }
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Drive `state` from the query, so every section has a loading, empty and error story.',
        'Use `flush` for tables and edge-to-edge lists.',
        'Give "View all" links text that makes sense alone ("View all tasks").',
      ],
      dont: [
        'Do not render your own spinner inside the body; use `state="loading"` or `busy`.',
        'Do not nest SectionCards.',
        'Do not use it for settings with a save bar; SettingsSection has that built in.',
      ],
    },
    related: {
      items: [
        {
          id: 'section-header',
          reason: 'The header it renders; use it alone for unboxed sections.',
        },
        { id: 'card', reason: 'A plain surface without a title or states.' },
        { id: 'settings-section', reason: 'Settings rows with Save/Discard.' },
        { id: 'empty-state', reason: 'The default empty body; pass your own in `empty`.' },
        { id: 'error-state', reason: 'The error body.' },
        { id: 'loading-state', reason: 'The loading body.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['empty-state', 'error-state', 'loading-state', 'section-header'],
    },
    testing: {
      unit: [
        'Is a region labelled by its heading, with icon, actions, link, body and footer; ref forwarding; Tab order.',
        'Shows loading (busy, announced), empty (default and custom) and error (retry) states instead of the children.',
        '`busy` keeps the content and marks the section busy.',
        'Collapsible uncontrolled: the title is a disclosure button; Enter and Space toggle the body and footer.',
        'Collapsible controlled with `open`, and `defaultOpen={false}`.',
        'Exposes tone and flush; honours `headingLevel` and `headingId`.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/section-card',
        'Cross-browser axe: /components/section-card',
      ],
    },
  },
} satisfies ComponentDocs;
