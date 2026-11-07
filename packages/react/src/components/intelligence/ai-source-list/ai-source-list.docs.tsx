import { useState } from 'react';
import { AiSourceList, AiSourcePreview, type AiSourceListItem } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const sources: AiSourceListItem[] = [
  {
    type: 'source',
    id: 's1',
    title: 'Team handbook',
    url: 'https://example.com/handbook',
    kind: 'doc',
    product: 'Docs',
    snippet: 'Requests are answered within two working days.',
  },
  {
    type: 'source',
    id: 's2',
    title: 'Support policy',
    url: 'https://example.com/support-policy',
    kind: 'web',
    snippet: 'Urgent issues go to the on-call queue.',
  },
  {
    type: 'source',
    id: 's3',
    title: 'Ticket 1042',
    kind: 'record',
    product: 'Helpdesk',
    used: false,
  },
  {
    type: 'source',
    id: 's4',
    title: 'Onboarding checklist.pdf',
    kind: 'file',
    used: false,
    snippet: 'A list of first-week tasks.',
  },
];

function PreviewDemo() {
  const [open, setOpen] = useState<AiSourceListItem | null>(null);
  return (
    <>
      <AiSourceList sources={sources} onOpen={setOpen} />
      {open ? (
        <AiSourcePreview
          source={open}
          quote={open.snippet}
          content={open.snippet}
          open
          onOpenChange={(next) => {
            if (!next) setOpen(null);
          }}
        />
      ) : null}
    </>
  );
}

export default {
  id: 'ai-source-list',
  title: 'AiSourceList',
  summary: 'Sources under an AI answer, grouped into used and considered.',
  sections: {
    purpose: {
      summary:
        'Shows where an AI answer came from: every source with its favicon or kind glyph, title, host and a short snippet. Sources the agent looked at but did not rely on are listed apart as "Considered", so the answer is honest about what it used.',
      useWhen: [
        'An answer has `source` parts, or a retrieval step reports what it looked at.',
        'Under an AiSummaryCard or any generated text that cites documents or records.',
      ],
      avoidWhen: [
        'A single inline reference: use AiCitation.',
        'Search results people browse for their own sake: use a regular results list.',
      ],
    },
    designPrinciples: {
      items: [
        'Numbers match AiCitation markers: the first source is [1].',
        'Used before considered; the split appears only when the agent reports both.',
        'Short by default: more than three sources collapse behind "Show all".',
        'Untrusted output stays safe: only safe URLs become links, and favicons load only from http(s).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<section>`.',
        '`sources` takes `AiSourcePart` objects from the message with optional `used` and `snippet`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'numbered',
          values: ['true', 'false'],
          description: 'Shows `[n]` before each title to match AiCitation markers. Default true.',
        },
      ],
      notes: ['Grouping is automatic: it appears when any source has `used: false`.'],
    },
    states: {
      items: [
        {
          name: 'Collapsed',
          attribute: 'data-collapsed',
          description:
            'More than `collapseAfter` (3) sources: only the first few show, with "Show all n sources".',
        },
        {
          name: 'Expanded',
          attribute: 'aria-expanded="true" on the toggle',
          description: 'All sources show, with "Show fewer".',
        },
        {
          name: 'Considered',
          attribute: 'data-group="considered"',
          description: 'Sources with `used: false`, drawn with a dashed border.',
        },
      ],
    },
    interaction: {
      items: [
        'With `onOpen`, each title is a button that calls it (for example to open AiSourcePreview).',
        'Without `onOpen`, a title with a safe URL links to it in a new tab; otherwise it is plain text.',
        'The toggle keeps focus when it expands or collapses the list.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through source titles, then the Show all toggle.' },
        { keys: 'Enter', action: 'Opens a source or toggles the list.' },
        { keys: 'Space', action: 'Activates button titles and the toggle.' },
      ],
    },
    accessibility: {
      role: 'region (section)',
      name: '`messages.heading` ("Sources").',
      items: [
        'Each group is a list labelled "Used (n)" or "Considered (n)"; without groups the list is labelled by the heading.',
        'The toggle has `aria-expanded` and `aria-controls`.',
        'External links announce that they open in a new tab; glyphs and `[n]` numbers are hidden from assistive technology.',
      ],
    },
    responsive: {
      items: [
        'Titles, hosts and snippets wrap at any width; snippets clamp to two lines.',
        'On coarse pointers titles and the toggle grow to 44px targets.',
      ],
    },
    theme: {
      items: [
        'Cards use the surface background and subtle border; numbers use `--mn-color-ai-citation`.',
        'Forced colours: borders use CanvasText and links LinkText.',
        'RTL: layout uses flex and logical properties.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Used and considered, opening a preview',
          stack: true,
          render: () => <PreviewDemo />,
        },
        {
          title: 'Links, no grouping',
          stack: true,
          render: () => <AiSourceList sources={sources.slice(0, 2)} />,
        },
      ],
    },
    doDont: {
      do: [
        'Pass sources in the order the answer cites them.',
        'Mark sources the agent did not use with `used: false` instead of hiding them.',
      ],
      dont: [
        'Do not add sources the agent did not return.',
        'Do not show raw URLs as titles when a title is available.',
      ],
    },
    related: {
      items: [
        { id: 'ai-citation', reason: 'Inline markers that point at these sources.' },
        { id: 'ai-source-preview', reason: 'Opens a source with the quote highlighted.' },
        {
          id: 'ai-message',
          reason: 'Renders source parts; replace its list through `renderPart`.',
        },
      ],
    },
    dependencies: { packages: ['react'], components: ['ai-citation'] },
    testing: {
      unit: [
        'Labelled region with links, meta and snippets.',
        'Used and Considered groups with original numbering.',
        'Collapses past three and toggles with aria-expanded.',
        'Titles call `onOpen` from the keyboard.',
        'No links or favicons from unsafe URLs.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
