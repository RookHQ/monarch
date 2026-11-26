import { Heading, Text } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'heading',
  title: 'Heading',
  summary: 'Section headings whose outline level and visual size are chosen separately.',
  sections: {
    purpose: {
      summary:
        'Renders `<h1>`–`<h6>` with Monarch type styles, so the outline level follows the page structure while the size follows the design. Replaces hand-written `<h2 className="text-lg font-semibold">` headings. Needed by: every Rook app (page, card and dialog titles).',
      useWhen: [
        'Titling a page, section, card or panel.',
        'A heading must look smaller or larger than its level suggests, such as an `h3` card title at `base` size.',
      ],
      avoidWhen: [
        'Text that only looks bold but does not start a section: use Text with `weight="semibold"`, so screen-reader heading navigation is not cluttered.',
        'The title of a Dialog, Sheet or Drawer: use their own `DialogTitle` / `SheetTitle`, which wire up the accessible name.',
        'Marketing display type: use the `--mn-display-*` styles of the marketing components.',
      ],
    },
    designPrinciples: {
      items: [
        'Semantics first: `level` comes from the outline (one `h1` per page, no skipped levels); `size` is purely visual.',
        'Sizes are the `--mn-text-*` scale by name (`sm` to `5xl`), so a heading and body text never drift apart.',
        'Large sizes tighten line height and letter spacing; `text-wrap: balance` avoids a single orphaned word.',
        'No margins: spacing belongs to the layout (Stack, SectionHeader), not the heading.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `h{level}` and forwards its ref (`HTMLHeadingElement`); every heading attribute passes through, including `id` for in-page links and TableOfContents.',
        '`HEADING_DEFAULT_SIZES` is the level-to-size map used when `size` is not set.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Tailwind classes: `text-3xl font-bold tracking-tight` on an `h1` becomes `<Heading level={1} weight="bold">`; `text-lg font-semibold` on an `h3` becomes `<Heading level={3} size="lg">`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'level',
          values: ['1', '2', '3', '4', '5', '6'],
          description: 'The element rendered (`h1`–`h6`). Default 2. Exposed as `data-level`.',
        },
        {
          prop: 'size',
          values: ['sm', 'base', 'lg', 'xl', '2xl', '3xl', '4xl', '5xl'],
          description:
            'Font size from `--mn-text-*`. Defaults by level: h1 3xl, h2 2xl, h3 xl, h4 lg, h5 base, h6 sm. Exposed as `data-size`.',
        },
        {
          prop: 'weight',
          values: ['medium', 'semibold', 'bold'],
          description: 'Default `semibold`. Exposed as `data-weight`.',
        },
        {
          prop: 'tone',
          values: ['default', 'muted'],
          description: '`muted` uses `--mn-color-text-muted`. Exposed as `data-tone`.',
        },
        {
          prop: 'truncate',
          values: ['true', 'false'],
          description: 'One line with an ellipsis (`data-truncate`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Truncated',
          attribute: 'data-truncate',
          description: 'Single line, overflow hidden, ending in an ellipsis.',
        },
        {
          name: 'Muted',
          attribute: 'data-tone="muted"',
          description: 'Secondary text colour.',
        },
      ],
    },
    interaction: { items: [], notes: ['Not interactive.'] },
    keyboard: {
      items: [],
      notes: [
        'Not focusable. Screen-reader users jump between headings with their heading shortcuts (H, 1–6), which is why the level must be right.',
      ],
    },
    accessibility: {
      role: 'heading (aria-level from the element)',
      name: 'The heading text.',
      items: [
        'The level is the real element, so assistive technology reads the correct outline whatever the size.',
        'A truncated heading still exposes its full text to screen readers; add `title` when sighted users need the hidden part.',
        'Give headings an `id` when other parts of the page link to them (TableOfContents, SkipLink targets).',
      ],
    },
    responsive: {
      items: [
        'Below 40rem, `4xl` steps down to the `3xl` size and `5xl` to `4xl`, so long titles fit at 390px.',
        'Long words wrap (`overflow-wrap: break-word`) instead of widening the page.',
      ],
    },
    theme: {
      items: [
        'Colour is `--mn-color-text-default` (or `-muted`); font, sizes, line heights and tracking are tokens, so every product and theme applies.',
        'In forced-colours mode the heading uses CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Levels and their default sizes',
          stack: true,
          render: () => (
            <>
              <Heading level={1}>Workspace overview</Heading>
              <Heading level={2}>Recent activity</Heading>
              <Heading level={3}>This week</Heading>
              <Heading level={4}>Pinned items</Heading>
            </>
          ),
        },
        {
          title: 'Level and size set separately',
          stack: true,
          render: () => (
            <>
              <Heading level={2} size="base">
                A section heading that looks small
              </Heading>
              <Text tone="muted">An h2 in the outline, at body size inside a dense card.</Text>
            </>
          ),
        },
        {
          title: 'Weight, tone and truncation',
          stack: true,
          render: () => (
            <div style={{ maxWidth: 260 }}>
              <Heading level={3} weight="bold">
                Bold heading
              </Heading>
              <Heading level={3} tone="muted" weight="medium">
                Nothing here yet
              </Heading>
              <Heading level={3} truncate title="A long project name that does not fit the card">
                A long project name that does not fit the card
              </Heading>
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pick `level` from the page outline and `size` from the design.',
        'Use one `h1` per page, usually from the page header.',
        'Give linkable headings an `id`.',
      ],
      dont: [
        'Do not skip levels (h2 straight to h4) to get a smaller look: set `size` instead.',
        'Do not use a heading for emphasis inside a paragraph: use Text with a weight.',
        'Do not add margins on the heading itself: space it with the layout.',
      ],
    },
    related: {
      items: [
        { id: 'text', reason: 'Body text with the same size scale, tones and truncation.' },
        { id: 'prose', reason: 'Styles headings inside rendered Markdown or HTML for you.' },
        {
          id: 'section-header',
          reason: 'A heading with a description and actions for a page section.',
        },
        { id: 'table-of-contents', reason: 'Builds in-page navigation from heading ids.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Renders an `h2` by default with the level’s size, forwarding ref and attributes; no axe violations.',
        'Keeps level and size independent; weight, tone and truncate are exposed as data attributes.',
        'Every level maps to its default size.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
