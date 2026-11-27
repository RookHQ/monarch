import { InlineCode, Text } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'inline-code',
  title: 'InlineCode',
  summary: 'A code fragment inside running text.',
  sections: {
    purpose: {
      summary:
        'Marks a function name, path, command or value inside a sentence with a native `<code>` in the monospace font. Replaces the hand-styled `<code className="rounded bg-muted px-1 …">` found across the apps. Needed by: Rook Developers (API docs), Rook Tickets, Rook Recruit, Rook GTM, Rook Pulse, Rook People, Rook Partner, Rook Chat.',
      useWhen: [
        'A short piece of code, an identifier or a literal value appears inside a sentence or a table cell.',
        'Telling people exactly what to type: a command, a key name in a config, a template variable.',
      ],
      avoidWhen: [
        'Several lines of code, or code people will copy: use CodeBlock.',
        'A keyboard shortcut: use Kbd.',
        'Code inside Prose: plain `<code>` elements are already styled there.',
      ],
    },
    designPrinciples: {
      items: [
        'Sizes itself at 0.9em of the surrounding text, so it fits headings, body and small text alike.',
        'Long tokens (hashes, URLs, file paths) break anywhere instead of widening the layout.',
        'Ligatures are off so characters such as `!=` or `->` read exactly as typed.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['Renders `<code>` and forwards its ref; all HTML attributes pass through.'],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['subtle', 'plain'],
          description:
            '`subtle` (default) sits on a muted chip with a small radius; `plain` only changes the font. Exposed as `data-variant`.',
        },
        {
          prop: 'nowrap',
          values: ['true', 'false'],
          description: 'Keeps the fragment on one line (`data-nowrap`). Only for short code.',
        },
      ],
    },
    states: { items: [], notes: ['No states: it renders static content.'] },
    interaction: {
      items: [],
      notes: ['Not interactive. Put a CopyButton next to it when people should copy the value.'],
    },
    keyboard: { items: [], notes: ['Not focusable.'] },
    accessibility: {
      role: 'code',
      items: [
        'Native `<code>`: screen readers read the text as written; most do not announce it as code, so the sentence should make clear what it is.',
        'Spell out anything that is ambiguous when read aloud, such as a symbol-only operator.',
      ],
    },
    responsive: {
      items: [
        'Breaks long tokens anywhere (`overflow-wrap: anywhere`), so it never causes horizontal scrolling at 390px.',
        'When it wraps, each line keeps its padding and rounded chip (`box-decoration-break: clone`).',
      ],
    },
    theme: {
      items: [
        'The chip is `--mn-color-bg-muted` with `--mn-color-text-default`, so it follows light, dark and more-contrast themes.',
        'In forced-colours mode the `subtle` chip is drawn with a CanvasText border.',
      ],
    },
    examples: {
      items: [
        {
          title: 'In a sentence',
          stack: true,
          render: () => (
            <>
              <Text>
                Set <InlineCode>timeout</InlineCode> to a number of seconds, then run{' '}
                <InlineCode>npm run sync</InlineCode>.
              </Text>
              <Text size="sm" tone="muted">
                The file is saved to <InlineCode>exports/2026/report.csv</InlineCode>.
              </Text>
            </>
          ),
        },
        {
          title: 'Plain and long tokens',
          stack: true,
          render: () => (
            <div style={{ maxWidth: 280 }}>
              <Text size="sm">
                Commit <InlineCode variant="plain">a1b2c3d</InlineCode> changed this line.
              </Text>
              <Text size="sm">
                Token <InlineCode>9f8e7d6c5b4a39281706f5e4d3c2b1a0</InlineCode> breaks instead of
                overflowing.
              </Text>
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep it short: one identifier, path or command.',
        'Pair it with a CopyButton when people need to copy the value.',
      ],
      dont: [
        'Do not use it for emphasis or for keyboard keys.',
        'Do not use `nowrap` for long values; they would overflow on phones.',
      ],
    },
    related: {
      items: [
        { id: 'code-block', reason: 'Multi-line code with copy and wrap controls.' },
        { id: 'kbd', reason: 'Keyboard keys and shortcuts.' },
        { id: 'prose', reason: 'Styles `<code>` inside rendered Markdown automatically.' },
        { id: 'copy-button', reason: 'Lets people copy the value shown.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Renders a `<code>` inside text, forwarding ref and attributes; no axe violations.',
        'Exposes the `plain` variant and `nowrap`.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
