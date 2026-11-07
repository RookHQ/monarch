import { ForceGraph } from '@rookcom/monarch-react/charts';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const docs = [
  { id: 'handbook', label: 'Team handbook', group: 'ops', description: 'Operations' },
  { id: 'onboarding', label: 'Onboarding', group: 'ops' },
  { id: 'travel', label: 'Travel policy', group: 'ops' },
  { id: 'security', label: 'Security policy', group: 'eng' },
  { id: 'incident', label: 'Incident runbook', group: 'eng' },
  { id: 'release', label: 'Release process', group: 'eng' },
  { id: 'style', label: 'Writing style', group: 'brand' },
  { id: 'glossary', label: 'Glossary', group: 'brand' },
  { id: 'old-runbook', label: 'Old runbook', group: 'eng' },
  { id: 'notes', label: 'Meeting notes' },
];
const links = [
  { source: 'handbook', target: 'onboarding', kind: 'parent' },
  { source: 'handbook', target: 'travel', kind: 'parent' },
  { source: 'handbook', target: 'security', kind: 'related' },
  { source: 'security', target: 'incident', kind: 'related' },
  { source: 'incident', target: 'release' },
  { source: 'incident', target: 'old-runbook', kind: 'supersedes' },
  { source: 'onboarding', target: 'glossary' },
  { source: 'style', target: 'glossary', kind: 'related' },
  { source: 'release', target: 'style' },
];
const groups = [
  { key: 'ops', label: 'Operations' },
  { key: 'eng', label: 'Engineering' },
  { key: 'brand', label: 'Brand' },
];
const kinds = [
  { key: 'parent', label: 'Parent / child', color: 1 as const },
  { key: 'related', label: 'Related', color: 3 as const, dashed: true },
  { key: 'supersedes', label: 'Supersedes', color: 4 as const },
];

export default {
  id: 'force-graph',
  title: 'ForceGraph',
  summary:
    'A network of linked nodes laid out by a small built-in force simulation, with groups, link kinds, zoom, drag, keyboard navigation between nodes and a table of every link.',
  sections: {
    purpose: {
      summary:
        'Shows how things connect — documents that link to each other, related entities — so people can spot hubs, clusters and orphans, with every node and link also listed as text. Needed by: Dossier (document graph), related-entity views for AI features.',
      useWhen: [
        'Up to a few hundred nodes whose connections are the point.',
        'Exploring links and opening a node from the graph.',
      ],
      avoidWhen: [
        'A hierarchy with one parent per node: use TreeView or OrgChart.',
        'Thousands of nodes: lay them out on the server and use a dedicated viewer.',
        'A few related items: a list with links reads faster.',
      ],
    },
    designPrinciples: {
      items: [
        'No dependency: `forceLayout` (links, repulsion, collision, centring, a weak pull to the middle) replaces d3-force in about 200 lines.',
        'Deterministic: nodes start on a spiral and the simulation runs a fixed number of steps before the first paint, so the server and browser draw the same layout and nothing animates.',
        'Groups use palette slots in order, link kinds add dashes as a second encoding, and unlinked nodes are hollow and dashed.',
        'One tab stop: arrow keys move to the nearest node in that direction, and the table lists every node with its links.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Takes every ChartFrame prop except `children`, `legend`, `table` and `height`; the ref goes to the `<figure>`.',
        'Also exports `forceLayout(nodes, edges, options?)`, the pure layout function, returning a `Map` of positions by id.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Edges that point to unknown nodes, and self-links, are ignored. Groups default to the distinct `group` keys of the nodes.',
        'Migrating from the app copy: Dossier\'s `DocumentGraph` `data.nodes` map to `nodes` with `{ id, label: title, group: space?.id, description: space?.name }` and `groups` from the spaces; `data.edges` map to `edges` with `{ source: sourceDocumentId, target: targetDocumentId, kind: relationType ?? type }`; `RELATION_COLORS` becomes `edgeKinds` with palette slots and `dashed` for "related"; `onOpen(id)` → `onNodeSelect(node)`. The `activeSpaces` and `linkedOnly` filters stay in the app: filter `nodes` before passing them. The d3 zoom and drag are built in (buttons, +/− keys, drag).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showLabels',
          values: ['true', 'false'],
          description: 'Node names under each node.',
        },
        {
          prop: 'layoutOptions',
          values: ['linkDistance', 'charge', 'collide', 'gravity', 'ticks'],
          description: 'Tune spacing for dense or sparse graphs.',
        },
        {
          prop: 'status',
          values: ['ready', 'loading', 'empty', 'error'],
          description: 'ChartFrame states, exposed as `data-status` on the figure.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Inspected',
          attribute: 'data-active',
          description: 'Outlined node; its links are emphasised and other nodes dimmed.',
        },
        {
          name: 'Dimmed',
          attribute: 'data-dimmed',
          description: 'Nodes and links outside the inspected neighbourhood.',
        },
        {
          name: 'Selected',
          attribute: 'data-selected',
          description: 'Accent ring (`selectedId`).',
        },
        {
          name: 'Unlinked',
          attribute: 'data-orphan',
          description: 'Hollow, dashed, neutral node.',
        },
        {
          name: 'Empty',
          attribute: 'data-status="empty"',
          description: 'Automatically when there are no nodes.',
        },
      ],
    },
    interaction: {
      items: [
        'Hovering or tapping a node shows its name, group, link count and description, and highlights its neighbours.',
        'Click a node (or Enter / Space) to call `onNodeSelect`; dragging a node moves it without selecting.',
        'Drag the background to pan; Zoom in, Zoom out and Reset view (also +, − and 0) change the scale between 0.35× and 3×. Reset also returns dragged nodes.',
        'On touch screens the canvas captures drags so it can pan; scroll the page outside it.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Focuses the zoom buttons, then the canvas (inspecting the first node by name).',
        },
        {
          keys: 'Arrow keys',
          action: 'Move to the nearest node in that direction; the view follows.',
        },
        { keys: 'Home / End', action: 'First or last node by name.' },
        { keys: 'Enter / Space', action: 'Select the inspected node (`onNodeSelect`).' },
        { keys: '+ / − / 0', action: 'Zoom in, zoom out, reset the view.' },
        { keys: 'Escape', action: 'Clear the inspected node.' },
      ],
    },
    accessibility: {
      role: 'figure; the canvas is role="group" with aria-roledescription "graph"',
      name: 'The figure is labelled by `title`; the canvas by the generated summary ("10 nodes and 9 links in 3 groups. Most connected: Team handbook (3).") or `summary`.',
      items: [
        'Keyboard moves are announced in a polite live region with the node\'s group and every link ("Security policy, Engineering. 2 connections: Team handbook (Related), Incident runbook (Related)").',
        'The data table lists every node with its group and connections; it is always in the page and visible through "View as table".',
        'The zoom controls are real buttons with names; the SVG and tooltip are hidden.',
        'Every word is replaceable through `labels`.',
      ],
    },
    responsive: {
      items: [
        'The layout is fitted to the container width and `height`, re-fitting when the width changes; at 390px the graph is smaller but complete.',
        'Zoom buttons are 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        "Nodes use `--mn-color-data-*` by group (neutral `--mn-color-text-subtle` without a group); links use their kind's slot or the neutral token; the canvas is `--mn-color-bg-subtle`.",
        'Forced colours draw links and labels in CanvasText, nodes keep their fill, and the inspected and selected nodes get a Highlight ring.',
        'Nothing animates; the dimming transition is removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Document graph',
          stack: true,
          render: () => (
            <ForceGraph
              title="Document links"
              description="Documents and how they reference each other"
              nodes={docs}
              edges={links}
              groups={groups}
              edgeKinds={kinds}
              height={360}
              style={{ width: '100%', maxWidth: '44rem' }}
            />
          ),
        },
        {
          title: 'Without labels, selected node',
          stack: true,
          render: () => (
            <ForceGraph
              title="Related entities"
              nodes={docs}
              edges={links}
              groups={groups}
              showLabels={false}
              selectedId="security"
              height={260}
              style={{ width: '100%', maxWidth: '28rem' }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Filter nodes in the app (by space, by "linked only") and pass the result.',
        'Name link kinds and dash at least one of them when kinds differ in meaning.',
        'Use `onNodeSelect` to open the node and `selectedId` to show which one is open.',
      ],
      dont: [
        'Do not pass thousands of nodes: the layout is O(n²) per step.',
        'Do not rely on position to carry meaning: layouts change when nodes change.',
        'Do not use status colours for groups.',
      ],
    },
    related: {
      items: [
        { id: 'tree-view', reason: 'Hierarchies with one parent per node.' },
        { id: 'org-chart', reason: 'Reporting lines as a tree.' },
        { id: 'chart-frame', reason: 'The container, states and table every chart shares.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'chart-frame'] },
    testing: {
      unit: [
        'forceLayout is deterministic, separates nodes, keeps links short and honours pinned nodes.',
        'Nodes, valid links, orphans, group colours, dashed kinds, both legends and the summary.',
        'Table of nodes with groups and connections.',
        'Arrow-key navigation, Home/End, Enter selection and Escape; live region.',
        'Zoom buttons and keys, reset and the zoom limit.',
        'Hover neighbourhood highlight, selected ring, click selection; dragging a node and panning.',
        'Derived groups, translated words, empty state, identical server renders.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
