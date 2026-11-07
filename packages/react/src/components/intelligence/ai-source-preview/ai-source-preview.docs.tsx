import { useState } from 'react';
import { AiSourcePreview, type AiSourcePart } from '@rookcom/monarch-react/ai';
import { Button } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const handbook: AiSourcePart = {
  type: 'source',
  id: 's1',
  title: 'Team handbook',
  url: 'https://example.com/handbook',
  kind: 'doc',
  product: 'Docs',
};

const text = [
  'Welcome to the team. This handbook explains how we work together.',
  'Requests are answered within two working days. Urgent issues go to the on-call queue, which answers within two hours.',
  'Every request gets an owner. The owner keeps the requester up to date until it is closed.',
].join('\n\n');

function TextDemo() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button variant="secondary" onClick={() => setOpen(true)}>
        Preview source
      </Button>
      <AiSourcePreview
        source={handbook}
        quote="Requests are answered within two working days."
        content={text}
        open={open}
        onOpenChange={setOpen}
      />
    </>
  );
}

function RecordDemo() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button variant="secondary" onClick={() => setOpen(true)}>
        Preview record
      </Button>
      <AiSourcePreview
        source={{
          type: 'source',
          id: 'r1',
          title: 'Ticket 1042',
          kind: 'record',
          product: 'Helpdesk',
        }}
        content={
          <dl style={{ margin: 0 }}>
            <dt>Status</dt>
            <dd>Open</dd>
            <dt>Summary</dt>
            <dd>
              The export <mark>stops after the first page</mark> of results.
            </dd>
          </dl>
        }
        open={open}
        onOpenChange={setOpen}
      />
    </>
  );
}

export default {
  id: 'ai-source-preview',
  title: 'AiSourcePreview',
  summary: 'Side sheet that shows a cited source with the quote highlighted.',
  sections: {
    purpose: {
      summary:
        'Lets people check an AI answer against the source it cited without leaving the page: the document, record or page opens in a side sheet with the quoted passage highlighted and scrolled into view, plus a link to the original.',
      useWhen: [
        'From AiCitation or AiSourceList `onOpen`.',
        'The product can fetch the cited content (a document excerpt, record fields, page text).',
      ],
      avoidWhen: [
        'You cannot show the content: link to the original instead.',
        'Editing the source: open it in its own product.',
      ],
    },
    designPrinciples: {
      items: [
        'Show the evidence, not a summary of it: the exact quote is highlighted in place.',
        'If the quote cannot be found in the content, show it separately rather than hiding it.',
        'Plain text is rendered as text, never as HTML.',
        'Focus returns to whatever opened the sheet.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the sheet content (`role="dialog"`). Other SheetContent props (`side`, `size`, `busy`…) pass through.',
        '`findQuoteRange(text, quote)` is exported: it finds the quote ignoring case and whitespace differences.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'side',
          values: ['right', 'left', 'top', 'bottom'],
          description: 'Edge the sheet attaches to. Default `right`.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg', 'xl', 'full'],
          description: 'Sheet width; default `md`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Quote found',
          description:
            'The passage is wrapped in `<mark>` and scrolled to the middle of the sheet.',
        },
        {
          name: 'Quote not found',
          description: 'A "Quoted passage" block shows the quote above the content.',
        },
        {
          name: 'No content',
          description: 'Shows the quote alone, or "No preview is available" without one.',
        },
        {
          name: 'Busy',
          attribute: 'aria-busy, data-busy',
          description: 'Pass `busy` while content loads.',
        },
      ],
    },
    interaction: {
      items: [
        'A string `content` is split into paragraphs on blank lines; the first match of `quote` is highlighted, even across paragraphs.',
        'Node `content` renders as is; the first `<mark>` inside it is scrolled into view.',
        '"Open original" links to `openHref` or `source.url` in a new tab, only when the URL is safe.',
        'Scrolling is smooth unless reduced motion is requested.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves within the sheet (focus is trapped).' },
        {
          keys: 'Escape',
          action: 'Closes the sheet; focus returns to the element that opened it.',
        },
      ],
    },
    accessibility: {
      role: 'dialog',
      name: 'The source title; described by its host, product and kind.',
      items: [
        'The highlight is a `<mark>` element, which assistive technology can report as highlighted.',
        '"Open original" announces that it opens in a new tab.',
        'Focus return works even without a SheetTrigger, so it can be opened from citations and lists.',
      ],
    },
    responsive: {
      items: [
        'Side sheets never exceed the screen width; at 390px the sheet fills the screen.',
        'Long titles and text wrap.',
      ],
    },
    theme: {
      items: [
        'The highlight uses `--mn-color-ai-subtle` with an `--mn-color-ai-citation` underline.',
        'Forced colours: the highlight uses the system Mark colours.',
        'RTL: the quote rule uses logical properties.',
      ],
    },
    examples: {
      items: [
        { title: 'Document text with a quote', render: () => <TextDemo /> },
        { title: 'Record with its own mark', render: () => <RecordDemo /> },
      ],
    },
    doDont: {
      do: [
        'Load the content when the sheet opens and pass `busy` while it loads.',
        'Pass the citation part’s `quote`.',
      ],
      dont: [
        'Do not pass model-generated HTML as content.',
        'Do not use it for content people need to edit.',
      ],
    },
    related: {
      items: [
        { id: 'ai-citation', reason: 'Opens this from an inline marker.' },
        { id: 'ai-source-list', reason: 'Opens this from the source list.' },
        { id: 'sheet', reason: 'The side panel it is built on.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['sheet', 'button', 'ai-citation'],
    },
    testing: {
      unit: [
        'Dialog named by the source, described by its meta line.',
        'Highlights the quote ignoring whitespace and scrolls to it.',
        'Escape closes and focus returns to the opener.',
        'Quote shown separately when not found; unsafe URLs not linked.',
        'Node content and the empty state.',
        '`findQuoteRange`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
