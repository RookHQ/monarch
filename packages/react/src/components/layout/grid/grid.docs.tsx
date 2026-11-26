import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Grid,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const PROJECTS = [
  'Website refresh',
  'Quarterly review',
  'Mobile release',
  'Support playbook',
  'Hiring plan',
];

const tile = {
  padding: 12,
  borderRadius: 'var(--mn-radius-md)',
  background: 'var(--mn-color-bg-muted)',
  fontSize: 'var(--mn-text-sm)',
} as const;

export default {
  id: 'grid',
  title: 'Grid',
  summary:
    'A responsive grid: as many columns as fit a minimum item width, or set columns per breakpoint.',
  sections: {
    purpose: {
      summary:
        'Card grids, tile pickers and two-column forms are written as `grid grid-cols-1 md:grid-cols-3 gap-4` in every app. Grid gives the two responsive strategies as props, on the space scale and safe at 390px. Needed by: every Rook frontend with card or tile grids (for example the tile grid in Rook Links `src/views/Grid.tsx`).',
      useWhen: [
        'Cards or tiles of similar size that should flow into as many columns as fit (`minItemWidth`).',
        'A layout with a known number of columns per breakpoint (`columns={{ base: 1, md: 2, lg: 3 }}`).',
      ],
      avoidWhen: [
        'A single row: use Inline.',
        'A dashboard of widgets the user arranges: use DashboardGrid.',
        'Tabular data: use DataTable.',
      ],
    },
    designPrinciples: {
      items: [
        'Content-driven by default: columns come from the item size, not the screen, so the same grid works in a page or a side panel.',
        'Never overflows: the minimum width is capped at 100% of the grid.',
        'Mobile first: column counts inherit from the breakpoint below.',
        'Equal row heights by default (`align="stretch"`).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<div class="mn-grid" data-mode="auto|columns">` (or `as`). Variables set inline: `--mn-grid-min`, `--mn-grid-cols-base|sm|md|lg|xl`, `--mn-grid-gap`, `--mn-grid-row-gap`.',
        'Breakpoints are the `--mn-breakpoint-*` values (640, 768, 1024, 1280px) and follow the viewport.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Tailwind: `grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4` becomes `<Grid columns={{ base: 1, md: 2, lg: 3 }}>`; `grid-cols-[repeat(auto-fill,minmax(240px,1fr))]` becomes `<Grid minItemWidth={240}>`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'minItemWidth',
          values: ['16rem (default)', 'any CSS length', 'number of px'],
          description: 'Auto columns of at least this width.',
        },
        {
          prop: 'fit',
          values: ['fill', 'fit'],
          description: 'Keep empty tracks (items keep their size) or stretch items across the row.',
        },
        {
          prop: 'columns',
          values: ['number', '{ base, sm, md, lg, xl }'],
          description: 'Fixed column counts; overrides minItemWidth.',
        },
        {
          prop: 'gap / rowGap',
          values: ['0 … 16'],
          description: 'Space between items and rows. Default 4 (16px).',
        },
        {
          prop: 'align',
          values: ['stretch', 'start', 'center', 'end'],
          description: 'Vertical alignment within a row.',
        },
      ],
    },
    states: { items: [], notes: ['No states; the mode is reflected in `data-mode`.'] },
    interaction: { items: [], notes: ['Not interactive: layout only.'] },
    keyboard: {
      items: [],
      notes: [
        'Not interactive: tab order follows source order. For arrow-key movement across tiles, wrap it in RovingFocusGroup with `orientation="grid"`.',
      ],
    },
    accessibility: {
      role: 'none by default; the role of the `as` element (use `ul` for a set of cards)',
      name: 'Only when the element needs one.',
      items: [
        'Visual order equals source order; Grid never places items out of order.',
        'Checked with axe in jsdom.',
      ],
    },
    responsive: {
      items: [
        '`minItemWidth` flows from one column at 320px to as many as fit, with no breakpoints.',
        '`columns` per breakpoint follows the viewport from 640, 768, 1024 and 1280px.',
      ],
    },
    theme: { items: ['No colours; spacing from `--mn-space-*`.'] },
    examples: {
      items: [
        {
          title: 'Cards by minimum width',
          stack: true,
          render: () => (
            <Grid as="ul" minItemWidth="12rem" aria-label="Projects" style={{ width: '100%' }}>
              {PROJECTS.map((p) => (
                <li key={p}>
                  <Card>
                    <CardHeader>
                      <CardTitle>{p}</CardTitle>
                      <CardDescription>Updated this week</CardDescription>
                    </CardHeader>
                    <CardContent>3 open tasks</CardContent>
                  </Card>
                </li>
              ))}
            </Grid>
          ),
        },
        {
          title: 'Columns per breakpoint',
          stack: true,
          render: () => (
            <Grid columns={{ base: 1, sm: 2, lg: 4 }} gap="3" style={{ width: '100%' }}>
              {['Open', 'In progress', 'Waiting', 'Closed'].map((s) => (
                <div key={s} style={tile}>
                  {s}
                </div>
              ))}
            </Grid>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Prefer `minItemWidth`: it works in panels as well as pages.',
        'Render a set of cards as `as="ul"` with `li` children.',
      ],
      dont: [
        'Do not set a minimum width larger than a phone screen and expect it to overflow: it is capped.',
        'Do not use Grid to reorder content visually.',
      ],
    },
    related: {
      items: [
        { id: 'dashboard-grid', reason: 'User-arranged widget layouts.' },
        { id: 'sortable-grid', reason: 'A grid of tiles the user reorders.' },
        { id: 'stack', reason: 'A single column.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['stack'] },
    testing: {
      unit: [
        'Default auto mode with 16rem minimum; axe.',
        'Pixel and CSS-length minimums; fit.',
        'Fixed and per-breakpoint columns, invalid counts skipped.',
        'gap, rowGap, align; renders as a list; axe; ref; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
