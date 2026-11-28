import { useState, type ComponentProps } from 'react';
import { Badge, Button, StatusBadge } from '@rookcom/monarch-react';
import { BarChart } from '@rookcom/monarch-react/charts';
import { DashboardOperational, PeriodSelector } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { DemoRows } from '../../../internal/docs-demo.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './dashboard-operational.fixture.json';

interface StatusRow {
  label: string;
  state: string;
}
interface BadgeRow {
  label: string;
  badge: string;
  tone: 'danger' | 'warning' | 'info' | 'neutral';
}

// One support desk, Monday 15 June 2026, 11:00 in London.
const NOW = fixture.data.now;
type Props = ComponentProps<typeof DashboardOperational>;
const sampleKpis = fixture.data.kpis.map((kpi) => {
  const { text, value, ...rest } = kpi as typeof kpi & { text?: string; value?: number };
  return { ...rest, id: kpi.label, value: text ?? value };
}) as NonNullable<Props['kpis']>;
const slaInsight = fixture.data.insight;
const sampleInsights = [
  {
    id: slaInsight.id,
    severity: slaInsight.severity as 'warning',
    origin: slaInsight.origin as 'rule',
    title: slaInsight.title,
    why: slaInsight.why,
    action: { label: slaInsight.action, onClick: () => {} },
  },
];
const sampleRanking = fixture.data.workload.entries;
const sampleEvents = fixture.data.activity.map((e) => ({
  id: e.id,
  actor: { name: e.actor },
  verb: e.verb,
  object: e.object,
  time: e.time,
}));
const TREND = fixture.data.trend.data;
const [API, EMAIL, CHAT] = fixture.data.status as unknown as [StatusRow, StatusRow, StatusRow];
const [ATTENTION_1, ATTENTION_2] = fixture.data.attention as [BadgeRow, BadgeRow];
const [QUEUE_1, QUEUE_2, QUEUE_3] = fixture.data.queue as [BadgeRow, BadgeRow, BadgeRow];
const [MAINTENANCE, ROTA] = fixture.data.upcoming as [
  { label: string; value: string },
  { label: string; value: string },
];

function ArrangeDemo() {
  const [editing, setEditing] = useState(false);
  const [layout, setLayout] = useState<string[] | undefined>(undefined);
  return (
    <DashboardOperational
      title={fixture.data.title}
      description={fixture.data.description}
      period={<PeriodSelector defaultValue={{ preset: '7d' }} today="2026-06-15" size="sm" />}
      kpis={sampleKpis}
      kpiProps={{ minItemWidth: '9rem' }}
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
      status={
        <DemoRows
          rows={[
            { label: API.label, value: <StatusBadge status={API.state.toLowerCase()} /> },
            { label: EMAIL.label, value: <StatusBadge status={EMAIL.state.toLowerCase()} /> },
            { label: CHAT.label, value: <StatusBadge status={CHAT.state.toLowerCase()} /> },
          ]}
        />
      }
      attention={
        <DemoRows
          rows={[
            {
              label: ATTENTION_1.label,
              value: <Badge tone={ATTENTION_1.tone as 'danger'}>{ATTENTION_1.badge}</Badge>,
            },
            {
              label: ATTENTION_2.label,
              value: <Badge tone={ATTENTION_2.tone as 'warning'}>{ATTENTION_2.badge}</Badge>,
            },
          ]}
        />
      }
      queue={
        <DemoRows
          rows={[
            { label: QUEUE_1.label, value: <Badge tone="info">{QUEUE_1.badge}</Badge> },
            { label: QUEUE_2.label, value: <Badge>{QUEUE_2.badge}</Badge> },
            { label: QUEUE_3.label, value: <Badge tone="info">{QUEUE_3.badge}</Badge> },
          ]}
        />
      }
      trend={
        <BarChart
          title={fixture.data.trend.title}
          description={fixture.data.trend.description}
          data={TREND}
          categoryKey="day"
          categoryLabel={fixture.data.trend.categoryLabel}
          series={fixture.data.trend.series}
          height={180}
          locale="en-GB"
        />
      }
      workload={sampleRanking}
      workloadProps={{ valueLabel: fixture.data.workload.valueLabel }}
      upcoming={
        <DemoRows
          rows={[
            { label: MAINTENANCE.label, value: MAINTENANCE.value },
            { label: ROTA.label, value: ROTA.value },
          ]}
        />
      }
      activity={sampleEvents}
      activityProps={{ now: NOW }}
    />
  );
}

export default {
  id: 'dashboard-operational',
  title: 'DashboardOperational',
  summary:
    'Home-page template for operational products: status, what needs attention, my queue, trend, team load, coming up and activity.',
  sections: {
    purpose: {
      summary:
        'Gives operational products one home-page structure instead of a hand-built dashboard each. Needed by: Pulse (`rookpulse-frontend/src/app/(workspace)/w/[slug]/home/page.tsx` → `components/pulse/home/SystemStatusHome.tsx`: open incidents, uptime, slowest endpoints, upcoming maintenance, expiring soon) and Tickets (`rooktickets-frontend/src/app/(workspace)/w/[slug]/(desk)/home/page.tsx` → `components/home/HomeDashboard.tsx`: my queue, about to breach, team load, recent escalations, customer satisfaction).',
      useWhen: [
        'The home page of a product whose work arrives continuously: incidents, tickets, alerts.',
        'A team lead’s overview of live work and load.',
      ],
      avoidWhen: [
        'A sales, hiring or campaign home: use DashboardPipeline.',
        'A home about a set of assets or contracts: use DashboardPortfolio.',
        'A person’s own "My work" page: use DashboardPersonalWork.',
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
        'Sections, in default order: `status` (Status, DashboardSlot, span full); `attention` (Needs attention, DashboardSlot, span 2); `queue` (My queue, DashboardSlot, span 1); `trend` (Trend, DashboardSlot, span 2); `workload` (Team load, Leaderboard entries, span 1); `upcoming` (Coming up, DashboardSlot, span 1); `activity` (Recent activity, ActivityWidget events, span 1).',
        'Shared types `DashboardSlot`, `DashboardSlotConfig` and `DashboardTemplateBaseProps` are exported from the DashboardOperational module.',
        '`defaultDashboardOperationalMessages` holds the English section titles.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app home pages: Pulse `SystemStatusHome`: the status overview → `status`, open incidents → `attention`, uptime and p95 charts → `trend`, upcoming maintenance and expiring certificates → `upcoming`, the Space picker → `scope`. Tickets `HomeDashboard`: My queue → `queue`, About to breach → `attention`, Team load → `workload` (entries per agent), Recent escalations → `activity`, the availability toggle → `actions`, the open / unassigned / SLA counts → `kpis`.',
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
          title: 'A support desk, arrangeable',
          stack: true,
          render: () => <ArrangeDemo />,
        },
        {
          title: 'Slot config: custom title, loading and a view-all link',
          stack: true,
          render: () => (
            <DashboardOperational
              title="Home"
              headingLevel={2}
              kpis={sampleKpis}
              status={{
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
        { id: 'dashboard-pipeline', reason: 'Another home-page archetype.' },
        { id: 'dashboard-portfolio', reason: 'Another home-page archetype.' },
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
