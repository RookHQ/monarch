import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { DashboardPersonalWork, PeriodSelector } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = '2026-06-15T10:00:00Z';
const sampleKpis = [
  { id: 'k1', label: 'Sample figure A', value: 12 },
  { id: 'k2', label: 'Sample figure B', value: 4 },
  { id: 'k3', label: 'Sample figure C', value: 27 },
];
const sampleInsights = [
  {
    id: 'i1',
    severity: 'info' as const,
    origin: 'rule' as const,
    title: 'Sample insight',
    why: 'Insights explain why they are shown.',
  },
];
const sampleEvents = [
  {
    id: 'e1',
    actor: { name: 'Person A' },
    verb: 'updated',
    object: 'Item 12',
    time: '2026-06-15T09:40:00Z',
  },
  {
    id: 'e2',
    actor: { name: 'Person B' },
    verb: 'created',
    object: 'Item 13',
    time: '2026-06-15T08:05:00Z',
  },
];
const sampleTasks = [
  { id: 't1', title: 'Review item 12' },
  { id: 't2', title: 'Reply to a request', overdue: true },
];

function ArrangeDemo() {
  const [editing, setEditing] = useState(false);
  const [layout, setLayout] = useState<string[] | undefined>(undefined);
  return (
    <DashboardPersonalWork
      title="Home"
      description="Sample workspace"
      period={<PeriodSelector defaultValue={{ preset: '7d' }} today="2026-06-15" size="sm" />}
      kpis={sampleKpis}
      insights={sampleInsights}
      actions={
        <Button
          variant="secondary"
          size="sm"
          onClick={() => setEditing((v) => !v)}
          aria-pressed={editing}
        >
          {editing ? 'Done' : 'Arrange'}
        </Button>
      }
      editing={editing}
      layout={layout}
      onLayoutChange={(next: string[]) => setLayout(next)}
      today={{ today: '2026-06-15', tasks: sampleTasks }}
      assigned={<p style={{ margin: 0 }}>Your open items by status</p>}
      dueSoon={<p style={{ margin: 0 }}>Items due this week</p>}
      progress={<p style={{ margin: 0 }}>Goals you track</p>}
      announcements={<p style={{ margin: 0 }}>Latest notices</p>}
      team={<p style={{ margin: 0 }}>Requests waiting for your approval</p>}
      activity={sampleEvents}
      activityProps={{ now: NOW }}
    />
  );
}

export default {
  id: 'dashboard-personal-work',
  title: 'DashboardPersonalWork',
  summary:
    'Home-page template for a person’s own work: today, assigned to you, due soon, progress, announcements, team and activity.',
  sections: {
    purpose: {
      summary:
        'Gives "My work" and employee home pages one structure centred on the signed-in person. Needed by: Stride (`rookstride-frontend/src/app/(workspace)/w/[slug]/my-work/page.tsx` → `components/home/MyWorkHome.tsx`: overdue, due this week, in progress, milestones due soon, sprints, projects, recent) and People (`rookpeople-frontend/src/app/(workspace)/w/[slug]/home/page.tsx`: today, timesheet status, leave balance, payslip, goals, expenses, announcements, onboarding tasks, manager section).',
      useWhen: [
        'A "My work" page listing what the signed-in person owns.',
        'An employee home with their day, requests and balances.',
      ],
      avoidWhen: [
        'A team or workspace overview: use DashboardOperational, DashboardPipeline or DashboardPortfolio.',
      ],
    },
    designPrinciples: {
      items: [
        'One question per home: the order of sections follows what this kind of product is asked first.',
        'Composition, not configuration: every section is a slot or kit component (KPIRow, InsightStrip, Widget, Leaderboard, ActivityWidget, TodayPanel), so apps keep their data fetching.',
        'Every section is optional and fails on its own: a widget’s loading, empty and error states never block the page.',
        'People can rearrange sections (DashboardGrid arrange mode); the template never stores the layout itself.',
        'Sample figures in docs are placeholders; the template never invents or computes metrics.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<div>`.',
        'Sections, in default order: `today` (Today, TodayPanel props, span 1); `assigned` (Assigned to you, DashboardSlot, span 2); `dueSoon` (Due soon, DashboardSlot, span 1); `progress` (Progress, DashboardSlot, span 1); `announcements` (Announcements, DashboardSlot, span 1); `team` (Your team, DashboardSlot, span full); `activity` (Recent activity, ActivityWidget events, span 1).',
        'Shared types `DashboardSlot`, `DashboardSlotConfig` and `DashboardTemplateBaseProps` are exported from the DashboardOperational module.',
        '`defaultDashboardPersonalWorkMessages` holds the English section titles.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app home pages: Stride `MyWorkHome`: overdue / due this week / done counts → `kpis`, In progress issues → `assigned`, Milestones due soon → `dueSoon`, sprints and projects → `progress` or `widgets`, `RecentSection` → `activity`. People home: `TodayCard` → `today`, `HomeSummaryStrip` → `kpis`, `LeaveBalanceCard` / `GoalsCard` → `progress`, `TimesheetStatusCard` / `ExpensesCard` / `PayslipCard` / `OnboardingTasksCard` → `widgets` items, `AnnouncementsCard` → `announcements`, `HomeManagerSection` → `team`, the unlinked-account alert → `notice`.',
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
          title: 'All sections with sample data, arrangeable',
          stack: true,
          render: () => <ArrangeDemo />,
        },
        {
          title: 'Slot config: custom title, loading and a view-all link',
          stack: true,
          render: () => (
            <DashboardPersonalWork
              title="Home"
              headingLevel={2}
              kpis={sampleKpis}
              assigned={{
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
        { id: 'dashboard-portfolio', reason: 'Another home-page archetype.' },
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
        'activity-widget',
        'today-panel',
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
