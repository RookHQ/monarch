import { Badge, Button, Inline, type ButtonProps } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './inline.fixture.json';

const ACTIONS = fixture.data.actions as readonly {
  label: string;
  variant: NonNullable<ButtonProps['variant']>;
}[];

export default {
  id: 'inline',
  title: 'Inline',
  summary:
    'A horizontal row of children that wraps, with even spacing from the Monarch space scale.',
  sections: {
    purpose: {
      summary:
        'Rows of buttons, tags and metadata are written as `flex flex-wrap items-center gap-*` in every app, often without wrapping, which breaks on phones. Inline is that row, wrapping by default. Needed by: every Rook frontend; it replaces ad-hoc row wrappers.',
      useWhen: [
        'A row of actions, tags, badges, avatars or metadata items.',
        'A label and value side by side that may wrap on narrow screens.',
      ],
      avoidWhen: [
        'Stacked blocks: use Stack.',
        'Equal-width columns: use Grid.',
        'A toolbar with keyboard navigation: use Toolbar or RovingFocusGroup.',
      ],
    },
    designPrinciples: {
      items: [
        'Wraps by default: a row of buttons never causes horizontal scrolling at 390px.',
        'Separate row gap, so wrapped rows can sit closer than items.',
        'Follows the text direction: `justify="start"` is the right edge in RTL.',
        'Space from the scale only.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<div class="mn-inline">` (or `as`) with `--mn-inline-gap` and `--mn-inline-row-gap` set inline and `data-gap`, `data-align`, `data-justify`, `data-wrap`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Tailwind: `flex flex-wrap items-center gap-2` becomes `<Inline gap="2">`; `justify-between` becomes `justify="between"`; `flex-nowrap` becomes `wrap={false}`; `gap-y-1` becomes `rowGap="1"`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'gap / rowGap',
          values: ['0', '0-5', '1', '1-5', '2', '2-5', '3', '4', '5', '6', '8', '10', '12', '16'],
          description: 'Space between items and between wrapped rows. Default 2 (8px).',
        },
        {
          prop: 'align',
          values: ['center', 'start', 'end', 'baseline', 'stretch'],
          description: 'Vertical alignment in a row.',
        },
        {
          prop: 'justify',
          values: ['start', 'center', 'end', 'between'],
          description: 'Horizontal distribution.',
        },
        {
          prop: 'wrap',
          values: ['true', 'false'],
          description: 'Wrap onto new rows (default) or stay on one line.',
        },
      ],
    },
    states: { items: [], notes: ['No states; props are reflected as `data-*` attributes.'] },
    interaction: { items: [], notes: ['Not interactive: layout only.'] },
    keyboard: { items: [], notes: ['Not interactive: tab order follows source order.'] },
    accessibility: {
      role: 'none by default; the role of the `as` element',
      name: 'Only when the element needs one (a list or a nav landmark).',
      items: [
        'Visual order equals source order; `justify` never reverses items.',
        'Checked with axe in jsdom.',
      ],
    },
    responsive: {
      items: [
        'Wraps at any width, so it works at 320px; `wrap={false}` is for rows that truncate their own content.',
        'Mirrors in right-to-left pages.',
      ],
    },
    theme: { items: ['No colours; spacing from `--mn-space-*`.'] },
    examples: {
      items: [
        {
          title: 'Actions',
          render: () => (
            <Inline>
              {ACTIONS.map((a) => (
                <Button key={a.label} variant={a.variant}>
                  {a.label}
                </Button>
              ))}
            </Inline>
          ),
        },
        {
          title: 'Tags that wrap',
          stack: true,
          render: () => (
            <Inline as="ul" gap="1-5" aria-label="Labels" style={{ maxWidth: 260 }}>
              {['Design', 'Research', 'Billing', 'Onboarding', 'Mobile', 'Accessibility'].map(
                (t) => (
                  <li key={t}>
                    <Badge>{t}</Badge>
                  </li>
                ),
              )}
            </Inline>
          ),
        },
        {
          title: 'Title and action, spread apart',
          stack: true,
          render: () => (
            <Inline justify="between" style={{ width: '100%', maxWidth: 420 }}>
              <strong>Members</strong>
              <Button size="sm">Invite</Button>
            </Inline>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep the default wrapping for anything that can grow.',
        'Use `as="ul"` for a list of tags.',
      ],
      dont: [
        'Do not use `wrap={false}` for rows of buttons.',
        'Do not use Inline for keyboard-navigable toolbars.',
      ],
    },
    related: {
      items: [
        { id: 'stack', reason: 'The vertical counterpart.' },
        {
          id: 'toolbar',
          reason: 'A row of controls with toolbar semantics and arrow-key navigation.',
        },
        { id: 'grid', reason: 'Equal columns rather than a single wrapping row.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['stack'] },
    testing: {
      unit: [
        'Wrapping, centred defaults and gap variables; axe.',
        'gap, rowGap, align, justify and wrap mapping.',
        'Renders as a list; axe; ref; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
