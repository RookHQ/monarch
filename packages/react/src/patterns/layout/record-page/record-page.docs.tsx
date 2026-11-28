import { Button, StatusBadge } from '@rookcom/monarch-react';
import { RecordPage } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './record-page.fixture.json';

const record = fixture.data;
const sampleTabs = record.tabs.map((tab) => ({
  value: tab.value,
  label: tab.label,
  ...('count' in tab && { count: tab.count }),
  content: <p>{tab.content}</p>,
}));

export default {
  id: 'record-page',
  title: 'RecordPage',
  summary:
    'A record detail template: RecordHeader with tabs, a main column, a side column of details and an activity section.',
  sections: {
    purpose: {
      summary:
        'Gives every record detail page (client, lead, project, ticket, candidate) the same frame, so header, tabs and the details column sit in the same place across products. Needed by: Dots (`rookdots-frontend/src/app/[locale]/(workspace)/w/[slug]/clients/[id]/layout.tsx`, `projects/[id]/page.tsx`, `leads/[id]/page.tsx`), GTM (`rookgtm-frontend/src/app/(workspace)/w/[slug]/leads/[id]/page.tsx`), Atlas (`rookatlas-frontend/src/app/(workspace)/w/[slug]/portfolio/clients/[id]/page.tsx`), Stride (`rookstride-frontend/src/app/(workspace)/w/[slug]/projects/[id]/page.tsx`).',
      useWhen: [
        'A full page for one record with sections (tabs) and properties.',
        'Record pages that should look the same across products.',
      ],
      avoidWhen: [
        'A record shown beside a list: use SplitLayout or SplitInboxLayout with RecordHeader `headingLevel="h2"`.',
        'A quick look without leaving the list: use a Sheet.',
      ],
    },
    designPrinciples: {
      items: [
        'Identity first: the header names the record and holds its actions; tabs come straight under it.',
        'The side column is stable: it does not change when tabs change, so key properties stay in view.',
        'The template owns layout only; the product supplies data, tab content and actions.',
        'Tabs follow the WAI-ARIA tabs pattern through Monarch Tabs.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<div>`.',
        '`headerProps` is passed to RecordHeader (avatar, status, facts, actions, navigator, banner, `onTitleSave`).',
        '`defaultRecordPageMessages` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the Dots client layout: the `TABS` array of `{ segment, label }` route links can stay as routes (pass `header` and render TabsNav yourself) or become `tabs` with `content`; `EntityAvatar` → `headerProps.avatar`; `ClientStatusBadge` / `TierBadge` → `headerProps.status` / `badges`; `LifecycleControl` and the merge action → `headerProps.actions`.',
        'Migrating from shadcn/ui `Tabs` + `TabsList` + `TabsTrigger` + `TabsContent`: `value` / `onValueChange` → `tab` / `onTabChange`; each trigger and content pair → one `tabs` entry `{ value, label, content }`.',
        'For URL-bound tabs, combine `tab` / `onTabChange` with `useTabsUrlBinding`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'activityPlacement',
          values: ['aside', 'main'],
          description: 'Activity under the side column (default) or under the main column.',
        },
        {
          prop: 'asideWidth',
          values: ['sm', 'md', 'lg'],
          description: 'Side column width on wide screens: 16, 20 or 24rem.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Aside width', attribute: 'data-aside-width', description: '`sm`, `md` or `lg`.' },
        {
          name: 'With side column',
          attribute: 'data-has-aside',
          description: 'On the body when an aside or aside activity exists.',
        },
        {
          name: 'Selected tab',
          attribute: 'aria-selected',
          description: 'On the chosen tab; its panel is shown.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing a tab shows its content in the main column; the side column stays.',
        'Tabs can be controlled (`tab`, `onTabChange`) or uncontrolled (`defaultTab`).',
        'Header actions and the previous/next navigator come from RecordHeader.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves from header actions into the selected tab, then into the panel.',
        },
        { keys: 'Arrow Left / Arrow Right', action: 'Moves between tabs and selects them.' },
        { keys: 'Home / End', action: 'First or last tab.' },
      ],
    },
    accessibility: {
      role: 'tablist + tabpanel, complementary, region',
      name: 'Tab list: "Record sections"; side column: "Record details"; activity: its heading.',
      items: [
        'The record title is the page’s h1 (from RecordHeader).',
        'Tab counts are part of the tab name ("Notes 2").',
        'The side column is an `<aside>` landmark; activity is a region named by its heading.',
      ],
    },
    responsive: {
      items: [
        'Below 1024px the side column moves under the main column.',
        'Tabs scroll sideways inside their strip at 390px; the page does not.',
        'Spacing tightens below 480px.',
      ],
    },
    theme: {
      items: [
        'Spacing, text and focus tokens only; colours come from RecordHeader and Tabs.',
        'Logical properties, so the side column sits on the left in RTL.',
        'Forced-colours mode keeps the panel focus outline in Highlight.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Record with tabs, details and activity',
          render: () => (
            <RecordPage
              title={record.title}
              headerProps={{
                headingLevel: 'h2',
                status: (
                  <StatusBadge
                    status={record.status.tone as 'success'}
                    label={record.status.label}
                    size="sm"
                  />
                ),
                subtitle: record.subtitle,
                facts: record.facts,
                actions: <Button size="sm">{record.action}</Button>,
              }}
              tabs={sampleTabs}
              aside={<p>{record.aside}</p>}
              activity={<p>{record.activity}</p>}
            />
          ),
        },
        {
          title: 'No tabs, activity under the main column',
          render: () => (
            <RecordPage
              title="Sample record"
              headerProps={{ headingLevel: 'h2' }}
              activity={<p>Sample activity entries.</p>}
              activityTitle="Recent activity"
              activityPlacement="main"
            >
              <p>Main content.</p>
            </RecordPage>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep the side column to properties that matter on every tab.',
        'Name the record type in navigator labels ("Previous lead").',
        'Bind tabs to the URL so a tab can be shared.',
      ],
      dont: [
        'Do not put primary actions in the side column; they belong in the header.',
        'Do not nest another tab list inside a tab panel.',
        'Do not use colour alone for the record status; use StatusBadge.',
      ],
    },
    related: {
      items: [
        { id: 'record-header', reason: 'The header this template renders.' },
        { id: 'tabs', reason: 'The section tabs.' },
        { id: 'page-header', reason: 'Header for pages that are not records.' },
        { id: 'split-layout', reason: 'A record beside its list.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['record-header', 'tabs'],
    },
    testing: {
      unit: [
        'Header, named tab list, selected panel, named aside and activity region; axe clean.',
        'Arrow keys move and select tabs; `onTabChange` called.',
        'Controlled and initial tab.',
        'Children without tabs, activity under main, custom header; axe clean.',
        'Translated labels, hidden activity heading, aside width, ref forwarding.',
        'Server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
