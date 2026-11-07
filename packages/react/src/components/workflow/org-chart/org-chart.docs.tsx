import { useState } from 'react';
import { Badge, Button } from '@rookcom/monarch-react';
import { OrgChart, type OrgChartNode } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const PEOPLE: OrgChartNode[] = [
  { id: 'p1', name: 'Alex Kim', title: 'Chief executive' },
  {
    id: 'p2',
    parentId: 'p1',
    name: 'Sam Lee',
    title: 'Engineering lead',
    meta: <Badge size="sm">Engineering</Badge>,
  },
  {
    id: 'p3',
    parentId: 'p1',
    name: 'Riya Shah',
    title: 'Finance lead',
    meta: <Badge size="sm">Finance</Badge>,
  },
  {
    id: 'p4',
    parentId: 'p1',
    name: 'Noor Ali',
    title: 'People lead',
    dashed: true,
    meta: (
      <Badge size="sm" tone="warning">
        Acting
      </Badge>
    ),
  },
  { id: 'p5', parentId: 'p2', name: 'Jo Park', title: 'Engineer' },
  { id: 'p6', parentId: 'p2', name: 'Ola Berg', title: 'Engineer' },
  { id: 'p7', parentId: 'p2', name: 'Product designer', kind: 'vacancy', meta: 'Engineering' },
  { id: 'p8', parentId: 'p3', name: 'Mia Chen', title: 'Accountant' },
  { id: 'p9', parentId: 'p4', name: 'Leo Grant', title: 'Recruiter', childCount: 2 },
];

function ChartDemo() {
  const [nodes, setNodes] = useState(PEOPLE);
  const [selected, setSelected] = useState<string | null>(null);
  return (
    <div style={{ display: 'grid', gap: 8 }}>
      <OrgChart
        nodes={nodes}
        focusId={selected}
        dottedLines={[{ from: 'p8', to: 'p2' }]}
        onSelect={(n) => setSelected(n.id)}
        onLoadChildren={(n) =>
          new Promise<void>((resolve) =>
            setTimeout(() => {
              setNodes((all) => [
                ...all,
                { id: `${n.id}-a`, parentId: n.id, name: 'Ivy Moss', title: 'Coordinator' },
                { id: `${n.id}-b`, parentId: n.id, name: 'Tom Reed', title: 'Coordinator' },
              ]);
              resolve();
            }, 500),
          )
        }
        onMoveRequest={(n, to) => {
          if (to === undefined) return;
          setNodes((all) => all.map((x) => (x.id === n.id ? { ...x, parentId: to } : x)));
        }}
        toolbar={<Button size="sm">Export</Button>}
        style={{ ['--mn-org-chart-height' as string]: '28rem' }}
      />
      <p style={{ margin: 0 }}>
        Selected: {nodes.find((n) => n.id === selected)?.name ?? 'nobody'}
      </p>
    </div>
  );
}

export default {
  id: 'org-chart',
  title: 'OrgChart',
  summary:
    'People as cards under their managers, with expandable and lazy-loaded teams, zoom and pan, dotted lines and drag-to-move; an indented list on narrow screens.',
  sections: {
    purpose: {
      summary:
        'Shows who reports to whom across an organisation, lets people explore teams from the keyboard or by pointer, and lets admins move people to a new manager. Needed by: People (org chart page, focused person, move dialog), and any app showing reporting lines.',
      useWhen: [
        'Reporting lines between people (and open positions) need to be seen and explored.',
        'Large organisations load teams on demand as people open them.',
        'Admins reassign managers by dragging or from the keyboard.',
      ],
      avoidWhen: [
        'A generic hierarchy of things (folders, departments without people): use TreeView.',
        'Goals with progress: use GoalTree.',
        'A flat directory with filters: use DataTable.',
      ],
    },
    designPrinciples: {
      items: [
        'One tree, two layouts: the same ARIA tree is drawn top-down where there is room and as an indented list below 40rem, so it works at 390px without horizontal page scrolling.',
        'The keyboard is first-class: the APG tree model moves focus; the visual expand chips are a mouse shortcut.',
        "The product owns data and decisions: `onLoadChildren` fetches branches, `onMoveRequest` opens the product's move dialog, `onSelect` opens its panel.",
        'No extra libraries: lines are CSS borders, dotted lines are an SVG measured from the real cards, and zoom is a CSS transform.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref is the root `<div>`; other div attributes pass through. Set `--mn-org-chart-height` (default 70vh) and `--mn-org-chart-card-width` (14rem) to size it.',
        'Pass every loaded node flat with `parentId`; top-level nodes have none. Cycles and unknown managers are ignored.',
        'Cards are focusable tree items, so `renderNode` and `meta` must not contain buttons or links; put actions in the panel `onSelect` opens.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from People `OrgChart` (`components/org/OrgChart.tsx`): `data.nodes` → `nodes` (`managerId` → `parentId`, `designation` → `title`, `photoUrl` → `avatarSrc`, department/team/temporary pills → `meta`, `temporary` → `dashed`, `directReports` → `childCount`); `data.vacancies` → nodes with `kind: "vacancy"` and `parentId: reportsToEmployeeId`; `data.dotted` → `dottedLines` (`employeeId` → `from`, `managerId` → `to`); the branch fetch in `toggle` → `onLoadChildren` (merge the subtree into `nodes`); `focusId` unchanged; `setPanel` → `onSelect`; `startMove` (drag, background drop and M) → `onMoveRequest(node, newParentId)`; `data.canManage && !historical` → pass `onMoveRequest` only then. The SVG/CSV export buttons → `toolbar`. The side panels and MoveDialog stay in the product.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['auto', 'chart', 'list'],
          description:
            '`auto` (default): top-down chart, indented list below 40rem of its own width. `chart` and `list` force one.',
        },
        {
          prop: 'zoomable',
          values: ['true', 'false'],
          description: 'Zoom buttons (40% to 160%) and Ctrl/⌘ + wheel zoom in the chart layout.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Expanded / collapsed',
          attribute: 'aria-expanded',
          description: 'Teams open and close; the chip under a card shows the number of reports.',
        },
        {
          name: 'Loading',
          attribute: 'aria-busy',
          description: 'A branch is being fetched by `onLoadChildren`; its chip is disabled.',
        },
        {
          name: 'Focused person',
          attribute: 'data-focus, aria-current',
          description: '`focusId`: accent border, the path to it opens and it scrolls into view.',
        },
        {
          name: 'Vacancy',
          attribute: 'data-kind="vacancy"',
          description: 'Dashed card with a briefcase and hidden "Open position".',
        },
        {
          name: 'Temporary line',
          attribute: 'data-dashed',
          description: 'Dashed border with hidden "temporary reporting line".',
        },
        {
          name: 'Dragging / drop target',
          attribute: 'data-dragging, data-drop',
          description: 'The dragged card is faded; an allowed manager is highlighted.',
        },
      ],
    },
    interaction: {
      items: [
        'Click or Enter on a card calls `onSelect`.',
        'The count chip under a card expands or collapses its team; a team not loaded yet is fetched first.',
        'Drag the background to pan; Ctrl/⌘ + wheel or the buttons zoom.',
        "With `onMoveRequest`, drag a card onto a new manager, or onto the background to make it top-level; dropping into one's own team is refused. M on a focused card asks to move it.",
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Enters the tree on the focused (or first) person; one tab stop for the whole tree.',
        },
        { keys: 'ArrowDown / ArrowUp', action: 'Next or previous visible person.' },
        {
          keys: 'ArrowRight',
          action: 'Opens a closed team (loading it if needed), or moves to the first report.',
        },
        { keys: 'ArrowLeft', action: 'Closes an open team, or moves to the manager.' },
        { keys: 'Home / End', action: 'First or last visible person.' },
        { keys: 'Enter / Space', action: 'Calls `onSelect`.' },
        { keys: 'M', action: 'With `onMoveRequest`: asks to move the focused person.' },
      ],
    },
    accessibility: {
      role: 'tree, treeitem, group',
      name: '"Organisation chart" (`messages.label`), described by the keyboard instructions.',
      items: [
        'Each card is a treeitem with `aria-level`, `aria-expanded` and hidden facts: number of reports, open position, temporary line.',
        'Roving tabindex keeps one tab stop; focus follows the arrow keys.',
        'Avatars are decorative; names are text. Lines, the dotted-line SVG and the count chips are hidden from assistive technology.',
        'A failed branch load is announced in a polite status region; the zoom level is announced as it changes.',
        'Dragging has keyboard (M) and panel alternatives (WCAG 2.5.7).',
      ],
    },
    responsive: {
      items: [
        'Below 40rem (with `layout="auto"`) the chart becomes an indented list that fits 390px; zoom and dotted lines are hidden there.',
        'The chart layout scrolls inside its own viewport (`--mn-org-chart-height`); touch scrolls natively.',
        'On coarse pointers the count chips grow; cards themselves are large targets.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only: bg subtle viewport, bg surface cards with shadow xs/md, default border lines, accent for focus and drop targets, border strong for dashed cards.',
        'Light, dark and high-contrast themes apply automatically; forced colours draw lines in CanvasText and highlights in Highlight.',
        'Reduced motion removes transitions and smooth scrolling to the focused person.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Chart with lazy loading, dotted line and moving',
          stack: true,
          render: () => <ChartDemo />,
        },
        {
          title: 'List layout',
          stack: true,
          render: () => <OrgChart nodes={PEOPLE.slice(0, 8)} layout="list" focusId="p6" />,
        },
      ],
    },
    doDont: {
      do: [
        'Pass `childCount` and `onLoadChildren` for organisations over a few hundred people.',
        "Open a side panel from `onSelect` with the person's details and actions.",
        'Confirm moves in your own dialog before saving; `onMoveRequest` is a request, not a save.',
      ],
      dont: [
        'Do not put buttons or links inside cards; they cannot be reached in a tree.',
        'Do not force `layout="chart"` on pages used on phones.',
      ],
    },
    related: {
      items: [
        { id: 'tree-view', reason: 'Generic hierarchies without people cards.' },
        { id: 'goal-tree', reason: 'Nested goals with progress.' },
        { id: 'avatar', reason: 'The person image used on each card.' },
        { id: 'sheet', reason: 'A side panel for the selected person.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['avatar', 'button'],
      notes: [
        '`--mn-org-chart-height` and `--mn-org-chart-card-width` size the chart; `--mn-org-chart-zoom` is set by the component.',
        'No layout or graph library: connectors are CSS, dotted lines are measured SVG.',
      ],
    },
    testing: {
      unit: [
        'ARIA tree with levels, expansion, roving tabindex and hidden facts; axe passes.',
        'APG keyboard model: arrows, Home/End, Enter to select; click to select.',
        'Lazy branch loading, and a failed load announced; mouse toggle and controlled expansion.',
        'focusId opens its path and marks it current; zoom buttons and level.',
        'Drag to a manager, refused drops, drop on the background, and M; renderNode and translations; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
