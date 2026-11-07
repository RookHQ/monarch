import {
  TimesheetDiff,
  diffTimesheetEntries,
  type TimesheetDiffEntry,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const SUBMITTED: TimesheetDiffEntry[] = [
  { id: 'e1', date: '2026-01-12', minutes: 120, project: 'Website refresh', activity: 'Design' },
  { id: 'e2', date: '2026-01-13', minutes: 60, project: 'Internal', billable: false },
  { id: 'e3', date: '2026-01-14', minutes: 90, project: 'Mobile app' },
];
const RESUBMITTED: TimesheetDiffEntry[] = [
  { id: 'e1', date: '2026-01-12', minutes: 180, project: 'Website refresh', activity: 'Design' },
  { id: 'e3', date: '2026-01-14', minutes: 90, project: 'Mobile app', activity: 'Testing' },
  { id: 'e4', date: '2026-01-15', minutes: 45, project: 'Mobile app', task: 'APP-7' },
];

export default {
  id: 'timesheet-diff',
  title: 'TimesheetDiff',
  summary:
    'What changed between two versions of a timesheet: added, removed and changed entries, in words, with their effect on the totals.',
  sections: {
    purpose: {
      summary:
        'Shows reviewers and employees exactly what changed between a submitted timesheet and its resubmission or correction, so an approver does not have to re-read the whole week. Needed by: People (SheetDiffView in the approval queue and the sent-back status panel).',
      useWhen: [
        'A timesheet was sent back and resubmitted, and the approver needs to see what was fixed.',
        'An HR correction changed an approved timesheet and the change must be shown on the record.',
        'Someone edits a sent-back sheet and wants to check their changes before resubmitting.',
      ],
      avoidWhen: [
        'Comparing documents or long text: use AiInlineDiff or VersionHistoryPanel.',
        'Showing a whole week of entries: use TimesheetGrid.',
      ],
    },
    designPrinciples: {
      items: [
        'Never colour alone: every row starts with "Added", "Removed" or "Changed time, project"; removed and old values are `<del>`, new values `<ins>`, with hidden "Before:" and "After:" labels.',
        'Effect first: each row shows how it moves the total ("+45m", "−1h"), and the footer sums the total and billable change.',
        'Data in, no fetching: pass the three lists from the server, or compute them with `diffTimesheetEntries(before, after)`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref is the root `<section>`, labelled by its heading.',
        '`diffTimesheetEntries(before, after)` compares two entry lists by id; `timesheetDiffTotals(diff)` returns `{ total, billable }` in minutes. Entries with `billable: false` do not count towards billable.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from People `SheetDiffView` (`components/timesheets/SheetDiffView.tsx`): keep the query in the product and spread its `diff` into the component: `<TimesheetDiff added={diff.added} removed={diff.removed} changed={diff.changed} />`. Map each `SnapshotEntry` to `{ id, date: workDate, minutes, project: projectName, activity: activityName, task, billable }`. `title` is unchanged; "since version N" → `subtitle`. `FIELD_LABEL` → `messages.fields` (`workDate` → `date`, `projectId` → `project`, and so on). `totalDelta`/`billableDelta` are computed; the loading skeleton stays in the product.',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No visual variants. `showTotals`, `showBillable`, `headingLevel` and `renderEntry` adjust the content.',
      ],
    },
    states: {
      items: [
        {
          name: 'Added',
          attribute: 'data-kind="added"',
          description: 'Success colours, "Added" and a positive delta.',
        },
        {
          name: 'Removed',
          attribute: 'data-kind="removed"',
          description: 'Danger colours, "Removed", a struck-through line and a negative delta.',
        },
        {
          name: 'Changed',
          attribute: 'data-kind="changed"',
          description:
            'Warning colours, "Changed" with the field names, the old line struck through and the new line below.',
        },
        {
          name: 'No changes',
          attribute: 'data-empty',
          description: '"No changes." and zero totals.',
        },
      ],
    },
    interaction: {
      items: ['Not interactive: a read-only summary.'],
    },
    keyboard: {
      items: [],
      notes: ['Not interactive: there is nothing to focus.'],
    },
    accessibility: {
      role: 'region (section labelled by its heading), list',
      name: 'The heading: "What changed since version 2".',
      items: [
        'Change type is text, not colour; `<del>` and `<ins>` carry the old and new values semantically.',
        'Hidden "Before:" and "After:" prefixes make the two lines of a changed entry clear when read aloud.',
        'Deltas use a true minus sign (−) so they read as negative.',
      ],
    },
    responsive: {
      items: [
        'Rows stack the label, delta and entry lines, and long entries wrap at any character, so nothing scrolls sideways at 390px.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only: success, danger and warning subtle/border/strong pairs for the three kinds.',
        'Light, dark and high-contrast themes apply automatically; forced colours outline each row.',
        'No motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Resubmission review',
          stack: true,
          render: () => (
            <TimesheetDiff
              {...diffTimesheetEntries(SUBMITTED, RESUBMITTED)}
              title="Changes in this resubmission"
              subtitle="since version 2"
              locale="en-GB"
            />
          ),
        },
        {
          title: 'No changes, billable hidden',
          stack: true,
          render: () => <TimesheetDiff added={[]} removed={[]} changed={[]} showBillable={false} />,
        },
      ],
    },
    doDont: {
      do: [
        'Show the diff wherever someone approves a resubmission or a correction.',
        "Label fields in your product's words with `messages.fields`.",
      ],
      dont: [
        'Do not show only the new totals: reviewers need to see what moved them.',
        'Do not reuse it for document text; it is built for time entries.',
      ],
    },
    related: {
      items: [
        { id: 'timesheet-grid', reason: 'Entering and reviewing the week itself.' },
        { id: 'approval-timeline', reason: 'Who approved or sent back each version.' },
        { id: 'version-history-panel', reason: 'Browsing every saved version of a document.' },
        { id: 'ai-inline-diff', reason: 'Text changes proposed by an assistant.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['duration-input'],
      notes: ['Uses `formatDuration` from DurationInput so durations read the same everywhere.'],
    },
    testing: {
      unit: [
        'diffTimesheetEntries finds added, removed and changed entries and the changed fields; totals exclude non-billable time.',
        'Rows say what changed in words with deltas, `<del>`/`<ins>` and hidden before/after labels; axe passes.',
        'Empty state, custom heading and level, custom entry renderer, hidden totals and billable, translated strings, server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
