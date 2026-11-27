import { useState, type ComponentProps } from 'react';
import { Badge, Button } from '@rookcom/monarch-react';
import { BarList, DonutChart } from '@rookcom/monarch-react/charts';
import { DashboardPortfolio, PeriodSelector } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './dashboard-portfolio.fixture.json';
import { DemoRows } from '../../../internal/docs-demo.js';

// A company’s supplier contracts on Monday 15 June 2026.
const NOW = fixture.data.now;
const sampleKpis = fixture.data.kpis.map((kpi, index) => ({
  id: String(index),
  label: kpi.label,
  value: kpi.value,
  ...(kpi.currency !== undefined && {
    format: 'currency' as const,
    currency: kpi.currency,
    notation: 'compact' as const,
    locale: fixture.data.locale,
  }),
}));
const sampleInsights = [
  {
    id: fixture.data.insight.id,
    severity: fixture.data.insight.severity as 'warning',
    origin: fixture.data.insight.origin as 'rule',
    title: fixture.data.insight.title,
    why: fixture.data.insight.why,
    action: { label: fixture.data.insight.action, onClick: () => {} },
  },
];
const sampleRanking = fixture.data.ranking.entries.map((entry) => ({
  id: entry.id,
  name: entry.name,
  value: entry.value,
  display: entry.display,
}));
const sampleEvents = fixture.data.events.map((event) => ({
  id: event.id,
  actor: { name: event.actor },
  verb: event.verb,
  object: event.object,
  time: event.time,
}));

function ArrangeDemo() {
  const [editing, setEditing] = useState(false);
  const [layout, setLayout] = useState<string[] | undefined>(undefined);
  return (
    <DashboardPortfolio
      title={fixture.data.title}
      description={fixture.data.description}
      period={
        <PeriodSelector defaultValue={{ preset: '7d' }} today={fixture.data.today} size="sm" />
      }
      kpis={sampleKpis}
      insights={sampleInsights}
      actions={
        <Button
          variant="secondary"
          size="sm"
          onClick={() => setEditing((v) => !v)}
          aria-pressed={editing}
        >
          {editing ? 'Done' : fixture.data.arrange}
        </Button>
      }
      editing={editing}
      layout={layout}
      onLayoutChange={(next: string[]) => setLayout(next)}
      composition={
        <DonutChart
          title={fixture.data.composition.title}
          segments={fixture.data.composition.segments}
          centerValue={fixture.data.composition.centerValue}
          centerLabel={fixture.data.composition.centerLabel}
          locale={fixture.data.locale}
        />
      }
      attention={
        <DemoRows
          rows={fixture.data.attention.map((row) => ({
            label: row.label,
            value: (
              <Badge tone={row.tone as NonNullable<ComponentProps<typeof Badge>['tone']>}>
                {row.badge}
              </Badge>
            ),
          }))}
        />
      }
      upcoming={<DemoRows rows={fixture.data.upcoming} />}
      breakdown={
        <BarList
          label={fixture.data.breakdown.label}
          items={fixture.data.breakdown.items}
          locale={fixture.data.locale}
        />
      }
      ranking={sampleRanking}
      rankingProps={{ valueLabel: fixture.data.ranking.valueLabel, showAvatars: false }}
      activity={sampleEvents}
      activityProps={{ now: NOW }}
    />
  );
}

export default {
  id: 'dashboard-portfolio',
  title: 'DashboardPortfolio',
  summary:
    'Home-page template for portfolio products: composition, needs attention, coming up, breakdown, top items and activity.',
  sections: {
    purpose: {
      summary:
        'Gives products that manage a set of owned things (assets, contracts, intellectual property) one home-page structure. Needed by: Atlas (`rookatlas-frontend/src/app/(workspace)/w/[slug]/home/page.tsx` → `features/overview/components/OverviewView.tsx`: metric cards, bar and donut charts, sources) and Intellect (`rookintellect-frontend/src/app/(workspace)/w/[slug]/home/page.tsx`: portfolio by type, needs attention, renewals in 6 months, jurisdictions).',
      useWhen: [
        'The home page of a product that holds a portfolio and tracks renewals or deadlines.',
        'An overview where composition matters more than flow.',
      ],
      avoidWhen: [
        'Work that moves through stages: use DashboardPipeline.',
        'Live operational work: use DashboardOperational.',
        'A person’s own work: use DashboardPersonalWork.',
      ],
    },
    designPrinciples: {
      items: [
        'One question per home: the order of sections follows what this kind of product is asked first.',
        'Composition, not configuration: every section is a slot or kit component (KPIRow, InsightStrip, Widget, Leaderboard, ActivityWidget, TodayPanel), so apps keep their data fetching.',
        'Every section is optional and fails on its own: a widget’s loading, empty and error states never block the page.',
        'People can rearrange sections (DashboardGrid arrange mode); the template never stores the layout itself.',
        'The figures in these examples are demo data; the template never invents or computes metrics.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<div>`.',
        'Sections, in default order: `composition` (Composition, DashboardSlot, span 2); `attention` (Needs attention, DashboardSlot, span 1); `upcoming` (Coming up, DashboardSlot, span 2); `breakdown` (Breakdown, DashboardSlot, span 1); `ranking` (Top items, Leaderboard entries, span 1); `activity` (Recent activity, ActivityWidget events, span 2).',
        'Shared types `DashboardSlot`, `DashboardSlotConfig` and `DashboardTemplateBaseProps` are exported from the DashboardOperational module.',
        '`defaultDashboardPortfolioMessages` holds the English section titles.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app home pages: Intellect home: Portfolio by type → `composition`, Needs attention → `attention`, Renewals in 6 months → `upcoming`, Jurisdictions → `breakdown`; its count tiles → `kpis`. Atlas `OverviewView`: `MetricCard`s → `kpis`, the donut chart → `composition`, bar charts → `breakdown` or `widgets`, `SourcesCard` → `ranking` or a `widgets` item.',
        'The app homes are hand-built from shadcn/ui `Card`, `CardHeader`, `CardTitle` and `CardContent`: `CardTitle` → the section title (or `DashboardSlotConfig.title`), `CardContent` → the slot content, a card’s header button → `DashboardSlotConfig.actions`, a "View all" link → `viewAllHref`, and loading skeletons or error panels → `loading`, `error` and `onRetry`.',
        'A slot takes plain content (wrapped in a Widget with the default title) or a `DashboardSlotConfig` with any Widget prop plus `content`, `title` and `span`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'columns',
          values: ['1', '2', '3', '4'],
          description:
            'Grid columns on wide screens (default 3). Medium widths use two, narrow one.',
        },
        {
          prop: 'headingLevel',
          values: ['1', '2'],
          description: 'Level of the page title; section titles use the next level.',
        },
        {
          prop: 'editing',
          values: ['false', 'true'],
          description: 'Arrange mode: sections get move handles and buttons.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Section left out',
          description: 'A missing, `null` or empty section is not rendered; the grid closes up.',
        },
        {
          name: 'Section loading / error / empty',
          description:
            'Set on a `DashboardSlotConfig` (`loading`, `error` + `onRetry`, `empty`) or through `…Props` on data sections.',
        },
        {
          name: 'KPIs loading',
          description: '`kpiProps.loading` shows skeleton figures before `kpis` arrive.',
        },
        {
          name: 'Arranging',
          attribute: 'data-editing',
          description: 'On the grid while `editing` is true.',
        },
      ],
    },
    interaction: {
      items: [
        'The template has no behaviour of its own; header controls and widgets keep theirs.',
        'In arrange mode, sections move by drag, by keyboard on the handle, or with Move earlier / Move later; `onLayoutChange` receives the new order of section ids.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through scope, period and actions, then each section’s controls in reading order.',
        },
        {
          keys: 'Space / Enter (on a handle)',
          action: 'Picks up or drops a section in arrange mode.',
        },
        { keys: 'Arrow keys (holding a section)', action: 'Moves it earlier or later.' },
        { keys: 'Escape (holding a section)', action: 'Cancels the move.' },
      ],
    },
    accessibility: {
      role: 'none (a layout); the grid is a list named "Dashboard sections"',
      name: 'The page title heading; the KPI row is a group named "Key figures".',
      items: [
        'Heading outline: page title (h1 by default), then one heading per section and the insight strip.',
        'Each section is a Widget region named by its title.',
        'Moves in arrange mode are announced through DashboardGrid’s live region.',
      ],
    },
    responsive: {
      items: [
        'The header wraps: controls move under the title on narrow screens and take the full width below 480px.',
        'The grid goes from the chosen columns to two at medium widths and one at 390px; spans clamp to the columns available.',
        'Header actions are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Layout and type tokens only; colour comes from the kit components, so light, dark and every product accent work.',
        'Logical properties throughout, so the layout mirrors in RTL.',
        'Forced-colours mode keeps the today panel border.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Supplier contracts, arrangeable',
          stack: true,
          render: () => <ArrangeDemo />,
        },
        {
          title: 'Slot config: custom title, loading and a view-all link',
          stack: true,
          render: () => (
            <DashboardPortfolio
              title="Home"
              headingLevel={2}
              kpis={sampleKpis}
              composition={{
                title: 'Custom title',
                content: <p style={{ margin: 0 }}>Content</p>,
                viewAllHref: '#all',
              }}
              widgets={[
                {
                  id: 'extra',
                  label: 'Extra section',
                  content: <p style={{ margin: 0 }}>Any extra content</p>,
                },
              ]}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep each section’s data fetching in the app and pass loading and error states per section.',
        'Store `onLayoutChange` results per person and pass them back as `layout`.',
        'Put the workspace or Space picker in `scope` and the PeriodSelector in `period`.',
      ],
      dont: [
        'Do not fill sections with placeholder numbers in production; leave a section out until it has data.',
        'Do not nest another page header inside a section.',
        'Do not use colour alone to signal a problem in a section; say it in words.',
      ],
    },
    related: {
      items: [
        { id: 'dashboard-operational', reason: 'Another home-page archetype.' },
        { id: 'dashboard-pipeline', reason: 'Another home-page archetype.' },
        { id: 'dashboard-personal-work', reason: 'Another home-page archetype.' },
        { id: 'dashboard-grid', reason: 'The arrangeable grid the sections sit in.' },
        { id: 'widget', reason: 'The frame of each section.' },
        { id: 'period-selector', reason: 'The usual `period` control.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'dashboard-grid',
        'dashboard-operational',
        'widget',
        'kpi-row',
        'insight-strip',
        'leaderboard',
        'activity-widget',
      ],
    },
    testing: {
      unit: [
        'Header, KPI row, insights and every section in a named grid with section headings and data; axe clean.',
        'Slot config objects (title, actions), left-out sections and extra widgets.',
        'Loading state from a slot config.',
        'Header controls reachable by Tab; arrange mode moves sections and reports the new order.',
        'Messages translate titles; headingLevel shifts the outline; ref forwarded.',
        'Server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
