import { Badge, Button, Card, CardContent, Stack } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const box = {
  padding: 12,
  borderRadius: 'var(--mn-radius-md)',
  background: 'var(--mn-color-bg-muted)',
  fontSize: 'var(--mn-text-sm)',
} as const;

export default {
  id: 'stack',
  title: 'Stack',
  summary: 'A vertical column of children with even spacing from the Monarch space scale.',
  sections: {
    purpose: {
      summary:
        'Spacing between stacked blocks is the most repeated layout in every app, written as `flex flex-col gap-*` wrappers with a different gap each time. Stack makes it one component on the space scale. Needed by: every Rook frontend; it replaces ad-hoc column wrappers.',
      useWhen: [
        'Blocks that sit one above the other with the same space between them: form sections, card bodies, settings groups, page sections.',
        'A list whose items need even spacing (`as="ul"`).',
      ],
      avoidWhen: [
        'Items in a row: use Inline.',
        'Items in columns: use Grid.',
        'Spacing that must differ between specific children: use the space tokens directly on those children.',
      ],
    },
    designPrinciples: {
      items: [
        'Space comes from the scale only (`gap="4"` is 16px), so rhythm stays consistent between apps.',
        'The parent owns spacing: children carry no outer margins.',
        'Semantic element through `as`; lists lose their bullets and indent but keep list semantics.',
        'Dividers sit in the middle of the gap, so turning them on does not change spacing.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<div class="mn-stack">` (or `as`) with `--mn-stack-gap` set inline and `data-gap`, `data-align`, `data-justify`, `data-dividers`.',
        '`LayoutGap`, `LayoutElement` and `layoutGap(gap)` are shared with Inline and Grid.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Tailwind: `flex flex-col gap-4` becomes `<Stack gap="4">`; `items-center` becomes `align="center"`; `justify-between` becomes `justify="between"`; `divide-y` becomes `dividers`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'gap',
          values: ['0', '0-5', '1', '1-5', '2', '2-5', '3', '4', '5', '6', '8', '10', '12', '16'],
          description: 'Space between children (`--mn-space-*`). Default 4 (16px).',
        },
        {
          prop: 'align',
          values: ['stretch', 'start', 'center', 'end'],
          description: 'Horizontal alignment of children.',
        },
        {
          prop: 'justify',
          values: ['start', 'center', 'end', 'between'],
          description: 'Vertical distribution when the stack is taller than its content.',
        },
        {
          prop: 'dividers',
          values: ['false', 'true'],
          description: 'A subtle line between children.',
        },
      ],
    },
    states: {
      items: [],
      notes: ['Stack has no states; its props are reflected as `data-*` attributes.'],
    },
    interaction: {
      items: [],
      notes: ['Not interactive: layout only.'],
    },
    keyboard: {
      items: [],
      notes: ['Not interactive: reading and tab order follow the source order of the children.'],
    },
    accessibility: {
      role: 'none by default; the role of the `as` element (list, region, form…)',
      name: 'Only when the element needs one: `aria-label` on a landmark or list.',
      items: [
        'Visual order always equals source order: Stack never reorders children.',
        'Lists rendered with `as="ul"` keep their list semantics.',
        'Checked with axe in jsdom.',
      ],
    },
    responsive: {
      items: [
        'Full width by default with `min-inline-size: 0`, so long children shrink rather than overflow at 320px.',
        'To change the gap by screen size, render a different `gap` from your breakpoint logic or use Grid.',
      ],
    },
    theme: {
      items: [
        'Gap from `--mn-space-*`; dividers use `--mn-color-border-subtle` (CanvasText in forced-colours mode).',
      ],
    },
    examples: {
      items: [
        {
          title: 'Default spacing',
          stack: true,
          render: () => (
            <Stack style={{ width: '100%', maxWidth: 360 }}>
              <div style={box}>Profile</div>
              <div style={box}>Notifications</div>
              <div style={box}>Security</div>
            </Stack>
          ),
        },
        {
          title: 'A card body with dividers',
          stack: true,
          render: () => (
            <Card style={{ width: '100%', maxWidth: 360 }}>
              <CardContent>
                <Stack as="ul" gap="3" dividers aria-label="Recent activity">
                  <li>Report exported</li>
                  <li>Two members invited</li>
                  <li>
                    Plan renewed <Badge tone="success">Done</Badge>
                  </li>
                </Stack>
              </CardContent>
            </Card>
          ),
        },
        {
          title: 'Centred, tight',
          render: () => (
            <Stack gap="2" align="center">
              <strong>No results</strong>
              <span style={{ fontSize: 'var(--mn-text-sm)' }}>Try a different search.</span>
              <Button size="sm" variant="secondary">
                Clear search
              </Button>
            </Stack>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pick one gap per level of hierarchy and reuse it.',
        'Use `as` for the right semantics (`ul`, `section`, `form`).',
      ],
      dont: [
        'Do not add margins to children inside a Stack.',
        'Do not nest Stacks only to add one extra space: change the gap.',
      ],
    },
    related: {
      items: [
        { id: 'inline', reason: 'The horizontal counterpart, with wrapping.' },
        { id: 'grid', reason: 'Columns of items.' },
        { id: 'separator', reason: 'A standalone divider with its own semantics.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Root class, default gap, align and justify; axe.',
        'gap, align, justify and dividers map to attributes and the gap variable.',
        'Renders as a list with semantics kept and style merged; axe.',
        'Ref and className forwarding; gap helper; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
