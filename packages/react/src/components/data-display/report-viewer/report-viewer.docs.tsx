import { useState } from 'react';
import { Badge } from '@rookcom/monarch-react';
import {
  KPIRow,
  PeriodSelector,
  ReportViewer,
  type ReportColumn,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

type Row = { team: string; headcount: number; joiners: number; leavers: number; remote: boolean };
const columns: ReportColumn<Row>[] = [
  { key: 'team', label: 'Team' },
  { key: 'headcount', label: 'Headcount' },
  { key: 'joiners', label: 'Joined' },
  { key: 'leavers', label: 'Left' },
  { key: 'remote', label: 'Remote' },
];
const rows: Row[] = [
  { team: 'Design', headcount: 14, joiners: 2, leavers: 0, remote: true },
  { team: 'Engineering', headcount: 41, joiners: 5, leavers: 1, remote: true },
  { team: 'Support', headcount: 18, joiners: 1, leavers: 2, remote: false },
];

function ReportDemo() {
  const [refreshing, setRefreshing] = useState(false);
  return (
    <ReportViewer
      title="Headcount by team"
      description="People on the payroll at the end of the period."
      back={<a href="#reports">All reports</a>}
      filters={
        <PeriodSelector
          today="2026-11-07"
          presets={['30d', 'this-quarter', 'this-year', 'custom']}
          onValueChange={() => {
            setRefreshing(true);
            setTimeout(() => setRefreshing(false), 600);
          }}
        />
      }
      scope="All locations"
      generatedAt="2026-11-07T10:00:00Z"
      timeZone="UTC"
      badges={
        <Badge tone="warning" size="sm">
          Contains pay figures
        </Badge>
      }
      summary={
        <KPIRow
          size="sm"
          items={[
            { id: 'h', label: 'Headcount', value: 73, format: 'number' },
            { id: 'j', label: 'Joined', value: 8, format: 'number' },
            { id: 'l', label: 'Left', value: 3, format: 'number' },
          ]}
        />
      }
      columns={columns}
      rows={rows}
      refreshing={refreshing}
      notes={['Contractors are excluded.']}
      download={{
        formats: [
          { id: 'csv', label: 'CSV' },
          { id: 'xlsx', label: 'Excel' },
        ],
        onDownload: () => new Promise((resolve) => setTimeout(resolve, 600)),
      }}
    />
  );
}

export default {
  id: 'report-viewer',
  title: 'ReportViewer',
  summary:
    'One report on its own page: header, filters, figures, table or chart, notes and download.',
  sections: {
    purpose: {
      summary:
        'The shared layout of a `reports/[key]` page: title and description, a back link, filters, the scope and generated time, headline figures, a chart, a table (built in, or your own body), footnotes and a download button, with loading, refreshing, empty, forbidden and not-found states. Data comes from the app. Needed by: People, Atlas, Tickets, Stride, Intellect.',
      useWhen: [
        'A page shows one report with filters and an export.',
        'Reports across apps should look and behave the same.',
      ],
      avoidWhen: [
        'A dashboard of many widgets: use DashboardGrid with Widgets.',
        'An editable list of records: use DataTable on a list page.',
      ],
    },
    designPrinciples: {
      items: [
        'Say what the numbers cover: scope and generated time sit right under the header.',
        'Keep the previous result visible while filters re-run it (`refreshing`), so the page does not jump.',
        'Errors say why: "You cannot open this report" never offers a pointless retry.',
        'Prints cleanly: the back link, filters and actions are hidden on paper.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Generic over the row type: `ReportViewer<Row>` with `ReportColumn<Row>`.',
        'With `columns` and `rows` the body is a simple table named by the title; numbers, booleans and empty cells are written for you. Pass `children` for a DataTable, pivot or chart instead.',
        '`download` takes DownloadReportButton props; it is disabled while loading or failed unless you set `disabled`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy: People `reports/[key]/page.tsx` → `title`/`description` from the catalogue, `back` (All reports link), `filters={<ReportFilterBar …/>}`, `badges` ("Contains pay figures"), `summary={<KPIRow items={d.summary…}/>}`, `chart`, `children={<ReportTable …/>}` (it paginates), `notes`, and `download` in place of the Export button (the ExportDialog stays in People); its 403 and 404 branches → `error` with `errorKind="forbidden" | "not-found"`. Atlas `features/reports/components/ReportViewer.tsx` → `columns`/`rows` (its `cellText` is built in), `generatedAt`, `scope`, `notes`, `truncated`, `download={{ onDownload: () => reportsApi.csv(…) }}`, Print and Save in `actions`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'headingLevel',
          values: ['1', '2', '3'],
          description: 'Level of the title. Default 1, as the page heading.',
        },
        {
          prop: 'errorKind',
          values: ['error', 'forbidden', 'not-found', 'offline', 'server', 'maintenance'],
          description: 'Sets the error title, icon and whether retry is offered.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Ready', description: 'Summary, chart, body and notes.' },
        {
          name: 'Loading',
          attribute: 'data-state="loading"',
          description: 'Placeholders named "Loading report"; `aria-busy`; download disabled.',
        },
        {
          name: 'Refreshing',
          attribute: 'data-state="refreshing"',
          description: 'The previous result stays, dimmed, with `aria-busy`.',
        },
        { name: 'Empty', description: '"No rows match these filters." when `rows` is empty.' },
        {
          name: 'Error',
          attribute: 'data-state="error"',
          description: 'ErrorState by `errorKind`, with Try again where it can help.',
        },
      ],
    },
    interaction: {
      items: [
        'Filters are the app’s controls; the app re-runs the report and sets `refreshing` meanwhile.',
        'The download button calls `download.onDownload` with the format.',
        'Try again calls `onRetry` (not offered for forbidden or not-found).',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through back link, actions, download, filters and links in the body.',
        },
      ],
      notes: [
        'The built-in table is static; use DataTable in `children` for sorting and row actions.',
      ],
    },
    accessibility: {
      role: 'none on the wrapper; a heading, a filters group and a table',
      name: 'The title names the built-in table (aria-labelledby).',
      items: [
        'Filters are a group named "Report filters".',
        'The generated time is a `<time>` with an ISO date.',
        'Empty cells are a dash read as "None"; booleans are "Yes" and "No".',
        '`aria-busy` is set while loading and refreshing.',
      ],
    },
    responsive: {
      items: [
        'Header actions wrap under the title; filters wrap; the table scrolls inside its own container, so the page never scrolls sideways at 390px.',
      ],
    },
    theme: {
      items: [
        'Title `--mn-color-text-default`, description and meta `--mn-color-text-muted`; the chart panel uses `--mn-color-bg-surface` and `--mn-color-border-default`.',
        'In forced colours the chart border is CanvasText and refreshing content is not dimmed.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Report with filters, figures, table and download',
          stack: true,
          render: () => <ReportDemo />,
        },
        {
          title: 'Not allowed',
          stack: true,
          render: () => (
            <ReportViewer
              title="Salary distribution"
              headingLevel={2}
              error
              errorKind="forbidden"
            />
          ),
        },
        {
          title: 'Loading',
          stack: true,
          render: () => (
            <ReportViewer
              title="Leave balances"
              headingLevel={2}
              loading
              download={{ onDownload: () => {} }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep filters and the page in the URL so a report view can be shared.',
        'State the scope and generated time.',
        'Use `truncated` whenever the table is cut short, and offer the download.',
      ],
      dont: [
        'Do not put several unrelated reports on one viewer.',
        'Do not offer Try again for permission errors.',
      ],
    },
    related: {
      items: [
        { id: 'download-report-button', reason: 'The download control on its own.' },
        { id: 'data-table', reason: 'Sorting, selection and pagination in the body.' },
        { id: 'period-selector', reason: 'The usual report filter.' },
        { id: 'kpi-row', reason: 'Headline figures in `summary`.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'data-table',
        'download-report-button',
        'empty-state',
        'error-state',
        'skeleton',
      ],
    },
    testing: {
      unit: [
        'Header, back link, filters group, scope, generated time and a table named by the title; props and ref forwarded.',
        'Cell formatting for numbers, booleans and empty values; notes.',
        'Download from the header; disabled with placeholders while loading.',
        'Previous result kept while refreshing.',
        'Errors by kind with retry only where it helps; empty, truncated, custom body and custom cells.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
