import { Container } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const fill = {
  padding: 12,
  borderRadius: 'var(--mn-radius-md)',
  background: 'var(--mn-color-bg-muted)',
  fontSize: 'var(--mn-text-sm)',
} as const;

export default {
  id: 'container',
  title: 'Container',
  summary: 'Centres page content at a maximum width with responsive side gutters, 16px on phones.',
  sections: {
    purpose: {
      summary:
        'Every page needs the same outer frame: centred, a sensible maximum width, and side padding that is 16px on phones. Apps write `container mx-auto px-4` with different widths each time. Container is that frame on the Monarch width tokens. Needed by: every Rook frontend page layout.',
      useWhen: [
        'The outer wrapper of a page or a full-width page section.',
        'A reading-width column for articles, settings and forms (`size="narrow"`).',
      ],
      avoidWhen: [
        'Inside cards, dialogs or panels: they have their own padding.',
        'The app shell itself: AppShell already places the content area.',
      ],
    },
    designPrinciples: {
      items: [
        'Widths come from tokens: narrow 44rem, content 72rem, wide 80rem.',
        '16px gutters on phones, 24px from 768px, so content never touches the screen edge.',
        'The maximum width includes the gutters, so the edge of the content lines up across sections.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<div class="mn-container" data-size data-gutter>` (or `as`, such as `main`).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Tailwind: `container mx-auto px-4` becomes `<Container>`; `max-w-3xl mx-auto` becomes `<Container size="narrow">`; `max-w-7xl` becomes `size="wide"`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['narrow', 'content', 'wide', 'full'],
          description:
            '`--mn-width-narrow`, `--mn-width-content` (default), `--mn-width-wide`, or no limit.',
        },
        {
          prop: 'gutter',
          values: ['true', 'false'],
          description: 'Side padding (`--mn-layout-gutter`, `--mn-layout-gutter-lg` from 768px).',
        },
      ],
    },
    states: { items: [], notes: ['No states; props are reflected as `data-*` attributes.'] },
    interaction: { items: [], notes: ['Not interactive: layout only.'] },
    keyboard: { items: [], notes: ['Not interactive.'] },
    accessibility: {
      role: 'none by default; `as="main"` or `as="section"` for landmarks',
      name: 'A labelled section needs `aria-label` or `aria-labelledby`.',
      items: ['Use one `as="main"` Container per page at most.', 'Checked with axe in jsdom.'],
    },
    responsive: {
      items: [
        'Full width with 16px gutters on phones (no horizontal scroll at 320px); 24px gutters from 768px; centred from its maximum width.',
      ],
    },
    theme: {
      items: ['No colours; widths from `--mn-width-*`, gutters from `--mn-layout-gutter*`.'],
    },
    examples: {
      items: [
        {
          title: 'Sizes',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 8, width: '100%' }}>
              <Container size="narrow">
                <div style={fill}>narrow</div>
              </Container>
              <Container>
                <div style={fill}>content (default)</div>
              </Container>
              <Container size="wide">
                <div style={fill}>wide</div>
              </Container>
            </div>
          ),
        },
        {
          title: 'Without gutters',
          stack: true,
          render: () => (
            <Container size="narrow" gutter={false}>
              <div style={fill}>Aligned with the parent padding</div>
            </Container>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use one Container per page section so edges line up.',
        'Use `narrow` for long text and forms.',
      ],
      dont: [
        'Do not nest Containers.',
        'Do not add extra side padding inside a Container on phones.',
      ],
    },
    related: {
      items: [
        { id: 'app-shell', reason: 'The surrounding app frame with rail and header.' },
        {
          id: 'page-header',
          reason: 'The title row at the top of the page, inside the Container.',
        },
        { id: 'stack', reason: 'Spacing between the sections inside it.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Default content size with gutters; axe.',
        'Size and gutter props; landmark element; axe.',
        'Ref and className; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
