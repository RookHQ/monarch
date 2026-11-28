import { Heading, Text } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './text.fixture.json';

const lines = fixture.data.lines as unknown as readonly [string, string, string, string];

export default {
  id: 'text',
  title: 'Text',
  summary: 'Body text with the type scale, tones, weights and truncation.',
  sections: {
    purpose: {
      summary:
        'Renders paragraphs, captions, helper and meta text with Monarch type tokens, so apps stop hand-writing `text-sm text-muted-foreground truncate`. Needed by: every Rook app (list rows, cards, empty states, settings descriptions).',
      useWhen: [
        'Any running text outside rendered Markdown: descriptions, meta lines, captions, helper text.',
        'Text that must stay on one line (`truncate`) or a fixed number of lines (`truncate={2}`).',
        'Coloured status text that also has a word or icon saying the status.',
      ],
      avoidWhen: [
        'Rendered Markdown or HTML: wrap it in Prose.',
        'A section title: use Heading so it appears in the outline.',
        'Form field help and errors: use FieldDescription / FieldError, which are wired to the input.',
      ],
    },
    designPrinciples: {
      items: [
        'Sizes are the `--mn-text-*` scale by name; body (`base`) and larger sizes get relaxed line height for reading.',
        'Tones are semantic tokens: `muted` and `subtle` for secondary text, status tones use each intent’s `strong` colour, which meets text contrast.',
        'No margins: spacing comes from the layout.',
        'Inline elements (`span`, `strong`, `em`, `small`) inherit the surrounding size, so Text can be nested inside other text.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `as` (default `p`) and forwards the ref to it; all HTML attributes pass through.',
        '`truncate={n}` sets the `--mn-text-lines` custom property on the element’s style; other inline styles are kept.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Tailwind classes: `text-sm text-muted-foreground` becomes `<Text size="sm" tone="muted">`; `truncate` stays `truncate`; `line-clamp-2` becomes `truncate={2}`; `tabular-nums` becomes `tabular`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['2xs', 'xs', 'sm', 'base', 'lg'],
          description:
            'From `--mn-text-*`. Default `base` for block elements; inline elements inherit. Exposed as `data-size`.',
        },
        {
          prop: 'tone',
          values: ['default', 'muted', 'subtle', 'accent', 'info', 'success', 'warning', 'danger'],
          description: 'Text colour (`data-tone`, absent for `default`).',
        },
        {
          prop: 'weight',
          values: ['regular', 'medium', 'semibold', 'bold'],
          description: '`data-weight`, absent for `regular`.',
        },
        {
          prop: 'truncate',
          values: ['false', 'true', 'number'],
          description:
            '`true` or `1`: one line with an ellipsis (`data-truncate="line"`); `2` or more: clamp to that many lines (`data-truncate="lines"`).',
        },
        {
          prop: 'align',
          values: ['start', 'center', 'end'],
          description: 'Logical alignment that mirrors in right-to-left (`data-align`).',
        },
        {
          prop: 'as',
          values: [
            'p',
            'span',
            'div',
            'label',
            'strong',
            'em',
            'small',
            'li',
            'dt',
            'dd',
            'figcaption',
            'legend',
          ],
          description: 'The element rendered. Default `p`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Truncated (one line)',
          attribute: 'data-truncate="line"',
          description: 'No wrapping; overflow is hidden behind an ellipsis.',
        },
        {
          name: 'Clamped (several lines)',
          attribute: 'data-truncate="lines"',
          description: 'Shows up to `--mn-text-lines` lines, then an ellipsis.',
        },
      ],
    },
    interaction: { items: [], notes: ['Not interactive.'] },
    keyboard: { items: [], notes: ['Not focusable; it renders static text.'] },
    accessibility: {
      items: [
        'Truncation is visual only: screen readers still read the full text. Add `title` (or show the full text elsewhere) when sighted users need it.',
        'A tone is colour only. Always say the status in words or with an icon too.',
        'Use `as="label"` with `htmlFor` only for a real form label; prefer FieldLabel inside a Field.',
      ],
    },
    responsive: {
      items: [
        'Long words wrap (`overflow-wrap: break-word`), so text never widens the page at 390px.',
        'Truncated text takes at most its container’s width (`max-width: 100%`); give flex children `min-width: 0` so they can shrink.',
      ],
    },
    theme: {
      items: [
        'All colours are semantic tokens and follow light, dark and more-contrast themes.',
        'In forced-colours mode every tone becomes CanvasText, so meaning must not rely on colour.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Sizes',
          stack: true,
          render: () => (
            <>
              <Text size="lg">{lines[0]}</Text>
              <Text>{lines[1]}</Text>
              <Text size="sm">{lines[2]}</Text>
              <Text size="xs" tone="muted">
                {lines[3]}
              </Text>
            </>
          ),
        },
        {
          title: 'Tones and weights',
          stack: true,
          render: () => (
            <>
              <Text tone="muted">Muted secondary text.</Text>
              <Text tone="success" weight="medium">
                Saved: all changes are stored.
              </Text>
              <Text tone="danger" weight="semibold">
                Failed: the file could not be uploaded.
              </Text>
              <Text>
                Inline{' '}
                <Text as="strong" weight="semibold">
                  emphasis
                </Text>{' '}
                keeps the surrounding size.
              </Text>
            </>
          ),
        },
        {
          title: 'Truncation',
          stack: true,
          render: () => (
            <div style={{ maxWidth: 280 }}>
              <Heading level={3} size="base">
                Quarterly planning notes
              </Heading>
              <Text size="sm" tone="muted" truncate title="shared-drive/planning/notes-final.md">
                shared-drive/planning/notes-final.md
              </Text>
              <Text size="sm" truncate={2}>
                A longer description that is clamped to two lines, so cards in a grid keep the same
                height whatever the length of the text inside them.
              </Text>
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use `tone="muted"` for secondary text instead of a lighter weight.',
        'Pair status tones with a word or icon.',
        'Give truncated text a `title` or another way to read it in full.',
      ],
      dont: [
        'Do not use Text for headings: screen-reader users cannot jump to it.',
        'Do not use `accent` to mean a status; it is the product colour.',
        'Do not set raw colours or font sizes with `style`; pick a tone and size.',
      ],
    },
    related: {
      items: [
        { id: 'heading', reason: 'Section titles with an outline level.' },
        { id: 'prose', reason: 'Long-form rendered content with element styles.' },
        { id: 'tabular-number', reason: 'Formatted numbers with tabular digits.' },
        { id: 'inline-code', reason: 'Code fragments inside a sentence.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Renders a `p` at `base` size by default, forwarding ref and props; no axe violations.',
        'Exposes size, tone, weight, alignment and tabular digits as data attributes.',
        'Inline elements inherit the size unless it is set.',
        'Truncates to one line, or clamps to n lines through `--mn-text-lines` while keeping other inline styles.',
        'Renders as a label tied to its control.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
