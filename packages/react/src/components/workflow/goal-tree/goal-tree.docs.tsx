import { Button } from '@rookcom/monarch-react';
import { GoalTree, type GoalTreeItem } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const GOALS: GoalTreeItem[] = [
  {
    id: 'o1',
    title: 'Grow repeat customers',
    progress: 62,
    status: 'on-track',
    owner: { name: 'Sam Lee' },
    meta: 'weight 40% · due 31 Mar',
    alignedTo: 'Customers come back (Company)',
  },
  {
    id: 'k1',
    parentId: 'o1',
    title: 'Launch loyalty emails',
    progress: 100,
    status: 'done',
    meta: '30%',
  },
  {
    id: 'k2',
    parentId: 'o1',
    title: 'Cut monthly churn to 3%',
    progress: 30,
    status: 'at-risk',
    meta: '40%',
  },
  {
    id: 'k3',
    parentId: 'o1',
    title: 'Interview 20 customers',
    progress: 55,
    status: 'on-track',
    meta: '30%',
  },
  {
    id: 'o2',
    title: 'Ship the mobile app',
    progress: 10,
    status: 'off-track',
    owner: { name: 'Alex Kim' },
  },
  { id: 'k4', parentId: 'o2', title: 'Beta with 50 testers', progress: 0, status: 'not-started' },
];

export default {
  id: 'goal-tree',
  title: 'GoalTree',
  summary:
    'Nested goals (objectives and key results) with progress, status, owner and alignment, expandable and with row actions.',
  sections: {
    purpose: {
      summary:
        'Shows objectives with their key results underneath, each with progress, a status in words, the owner and what it is aligned to, so a person or manager can see how goals are doing at a glance. Needed by: People (GoalTree on performance pages, per person and per team).',
      useWhen: [
        'Goals cascade: objectives with measurable key results, or team goals supporting company goals.',
        'Each goal needs its progress and status visible with actions such as check in or edit.',
      ],
      avoidWhen: [
        'A plain hierarchy without progress (folders, departments): use TreeView.',
        'Reporting lines between people: use OrgChart.',
        'A single progress figure: use ProgressBar or StatCard.',
      ],
    },
    designPrinciples: {
      items: [
        'Status is words first: a badge (or hidden text on quiet children) says "At risk"; the bar colour only repeats it.',
        'Quiet by default: key results that are on track or not started hide their badge so problems stand out (`quietChildren`).',
        'Actions belong to the product: `renderActions` adds buttons per row and `onAddChild` adds "Add key result"; editing, deleting and check-ins stay in the product.',
        'Data in, flat: pass goals with `parentId`; the tree is built for you and cycles are ignored.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref is the root `<div>`; other div attributes pass through.',
        'Expansion is uncontrolled (`defaultExpanded`, default: all) or controlled (`expanded` + `onExpandedChange`).',
        'Rows are list items with a disclosure button rather than an ARIA tree, because each row holds its own buttons.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from People `GoalTree` (`components/performance/GoalTree.tsx`): map each `Goal` to `{ id, parentId, title, progress, status, owner: { name, avatarSrc: photoUrl }, meta: weight and due date, alignedTo: aligned goal title and owner }` (`ON_TRACK` → `on-track`, `AT_RISK` → `at-risk`, `OFF_TRACK` → `off-track`, `DONE` → `done`, `NOT_STARTED` → `not-started`). Key results are goals whose `parentId` is the objective. The History/Check in buttons and RowMenu → `renderActions`; "+ Add key result" → `onAddChild` (with `canAddChild` for `canEdit`). Grouping by person: render one GoalTree per person under your own heading. The dialogs and delete confirmation stay in the product.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'quietChildren',
          values: ['true', 'false'],
          description:
            '`true` (default): children on track or not started show their status as hidden text only. `false`: every row has a badge.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Status',
          attribute: 'data-status on the item',
          description:
            'not-started, on-track, at-risk, off-track or done; sets the badge and bar tone.',
        },
        {
          name: 'Expanded / collapsed',
          attribute: 'aria-expanded on the toggle',
          description: 'Collapsed goals show "3 key results" instead of their children.',
        },
        {
          name: 'Depth',
          attribute: 'data-depth',
          description:
            'Top-level titles are medium weight; children are indented with a guide line.',
        },
        { name: 'Empty', attribute: 'data-empty', description: 'The empty message is shown.' },
      ],
    },
    interaction: {
      items: [
        'The chevron button expands or collapses a goal; it rotates to show the state.',
        '"Add key result" calls `onAddChild` with the parent goal.',
        'Buttons from `renderActions` behave as the product defines.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through the expand buttons, row actions and add buttons in reading order.',
        },
        {
          keys: 'Enter / Space',
          action: 'Expands or collapses the focused goal, or activates the focused action.',
        },
      ],
    },
    accessibility: {
      role: 'list of listitems with disclosure buttons',
      name: 'Give the tree a heading or `aria-label` in your page, such as "Sam Lee’s goals".',
      items: [
        'Each toggle is named "Expand {goal}" or "Collapse {goal}" with `aria-expanded` and `aria-controls`.',
        'Each bar is a progressbar named "Progress of {goal}" with the percentage as its value text.',
        'The owner avatar is decorative; the owner name is in visually hidden text.',
        'Add buttons are named "Add key result to {goal}".',
      ],
    },
    responsive: {
      items: [
        'Below 30rem row actions move under the goal and indentation shrinks, so nothing scrolls sideways at 390px.',
        'Long titles wrap; on coarse pointers toggles and add buttons are 44px.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only: badges and bars use info/accent, warning, danger and success tones; guide lines use the default border.',
        'Light, dark and high-contrast themes apply automatically; forced colours keep guide lines and link colour.',
        'Reduced motion stops the chevron rotation transition.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Objectives and key results with actions',
          stack: true,
          render: () => (
            <GoalTree
              aria-label="Team goals"
              goals={GOALS}
              onAddChild={() => {}}
              renderActions={(goal) => (
                <Button size="xs" variant="ghost">
                  {goal.parentId ? 'Check in' : 'History'}
                </Button>
              )}
            />
          ),
        },
        {
          title: 'Collapsed, every status shown',
          stack: true,
          render: () => <GoalTree goals={GOALS} defaultExpanded={[]} quietChildren={false} />,
        },
      ],
    },
    doDont: {
      do: [
        'Give each goal an owner and a status the server computes, not one derived from colour.',
        'Name action buttons with the goal, such as "Check in on {goal}", when there are many rows.',
      ],
      dont: [
        'Do not nest more than three levels; split into separate trees per team.',
        'Do not put long descriptions in `meta`; open a detail view instead.',
      ],
    },
    related: {
      items: [
        { id: 'tree-view', reason: 'A plain hierarchy with the ARIA tree keyboard model.' },
        { id: 'org-chart', reason: 'Reporting lines between people.' },
        { id: 'progress-bar', reason: 'One progress value on its own.' },
        { id: 'rating-picker', reason: 'Rating people in a review cycle.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'badge', 'progress-bar'],
    },
    testing: {
      unit: [
        'Status, owner, alignment and progress values; quiet children keep hidden status text; axe passes.',
        'Disclosure toggles with names, aria-expanded/aria-controls and the collapsed count, by mouse and keyboard.',
        'Controlled expansion; row actions; add-child button.',
        'Cycles and missing parents; empty message; translations; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
