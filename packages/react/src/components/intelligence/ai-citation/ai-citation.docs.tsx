import { useState } from 'react';
import {
  AiCitation,
  AiMessageView,
  AiSourceList,
  AiSourcePreview,
  aiSourceIndex,
  type AiMessage,
  type AiSourcePart,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const handbook: AiSourcePart = {
  type: 'source',
  id: 's1',
  title: 'Team handbook',
  url: 'https://example.com/handbook',
  kind: 'doc',
  product: 'Docs',
};
const policy: AiSourcePart = {
  type: 'source',
  id: 's2',
  title: 'Support policy',
  url: 'https://example.com/support-policy',
  kind: 'web',
};

function InlineDemo() {
  const [preview, setPreview] = useState<AiSourcePart | null>(null);
  return (
    <>
      <p style={{ margin: 0, maxWidth: '36rem' }}>
        Requests are answered within two working days
        <AiCitation
          index={1}
          source={handbook}
          quote="Requests are answered within two working days."
          onOpen={setPreview}
        />
        , and urgent issues have a separate queue
        <AiCitation index={2} source={policy} />.
      </p>
      {preview ? (
        <AiSourcePreview
          source={preview}
          quote="Requests are answered within two working days."
          content={
            'Welcome to the team.\n\nRequests are answered within two working days. Urgent issues go to the on-call queue.\n\nSee the support policy for details.'
          }
          open
          onOpenChange={(open) => {
            if (!open) setPreview(null);
          }}
        />
      ) : null}
    </>
  );
}

const message: AiMessage = {
  id: 'a1',
  role: 'assistant',
  createdAt: '2026-01-15T10:00:00Z',
  parts: [
    { type: 'text', text: 'Requests are answered within two working days.', state: 'done' },
    { type: 'citation', id: 'c1', sourceId: 's1', quote: 'within two working days' },
    handbook,
    policy,
  ],
};

function MessageDemo() {
  const index = aiSourceIndex(message.parts);
  let listShown = false;
  return (
    <AiMessageView
      message={message}
      showActions={false}
      renderPart={(part) => {
        if (part.type === 'citation') {
          const entry = index.get(part.sourceId);
          return entry ? (
            <AiCitation index={entry.index} source={entry.source} quote={part.quote} />
          ) : null;
        }
        if (part.type === 'source') {
          if (listShown) return null;
          listShown = true;
          return <AiSourceList sources={[...index.values()].map((e) => e.source)} />;
        }
        return undefined;
      }}
    />
  );
}

export default {
  id: 'ai-citation',
  title: 'AiCitation',
  summary: 'Inline [1] source marker with a hover and focus card.',
  sections: {
    purpose: {
      summary:
        'Ties a claim in an AI answer to the source it came from. The marker is small enough for running text; the card shows the title, the quoted passage, where the source lives and a way to open it, so people can check the answer without leaving it.',
      useWhen: [
        'An AI answer cites documents, records or pages (`citation` and `source` parts).',
        'You render a citation part through `AiMessageView renderPart`.',
      ],
      avoidWhen: [
        'The answer has no sources: do not invent markers.',
        'You need the full list of sources: use AiSourceList under the answer.',
        'Ordinary footnotes in Markdown: AiMarkdown renders `[^1]` footnotes itself.',
      ],
    },
    designPrinciples: {
      items: [
        'Every claim can be checked: the card always shows the source title and, when given, the exact quote.',
        'Hover and focus both reveal the card; neither moves focus.',
        'Clicking or pressing Enter pins the card and moves focus into it, so its links are reachable by keyboard.',
        'Model and tool output is untrusted: links pass `isSafeUrl`, open in a new tab with `rel="noopener noreferrer nofollow"`, and favicons load only from http(s).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the marker `<button>`.',
        'Also exported for AiSourceList and AiSourcePreview: `aiSourceIndex(parts)` (numbers a message’s sources 1, 2, 3… in order), `aiSourceHost(source)`, `aiSourceMeta(source)`, `aiSafeSourceUrl(url)`, `AiSourceGlyph`, `AI_SOURCE_KIND_LABELS` and `AI_SOURCE_LINK_REL`.',
        'Citations in streamed Markdown: AiMarkdown renders `[1]` in the text literally. Render the `citation` parts it streams alongside the text through `AiMessageView renderPart` (they follow the sentence they support), and the `source` parts as one AiSourceList; see the "In a message" example.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'There are no visual variants. The card opens above the marker and flips when there is no room.',
      ],
    },
    states: {
      items: [
        {
          name: 'Closed',
          attribute: 'data-state="closed", aria-expanded="false"',
          description: 'Only the `[n]` marker shows.',
        },
        {
          name: 'Open',
          attribute: 'data-state="open", aria-expanded="true"',
          description:
            'The card shows; the marker is highlighted with the AI subtle colour. Controlled with `open` and `onOpenChange`, or uncontrolled.',
        },
      ],
    },
    interaction: {
      items: [
        'Pointer: the card opens after `openDelay` (200ms) on hover and closes `closeDelay` (250ms) after the pointer leaves both marker and card, so the pointer can travel into the card.',
        'Focus: tabbing to the marker opens the card without moving focus; tabbing away closes it.',
        'Click or Enter: pins the card and moves focus to its first action. Clicking a pinned marker closes it.',
        '`onOpen` adds an "Open source" button (for example to open AiSourcePreview); a safe `source.url` adds a "Visit page" link that opens in a new tab.',
        'Touch: tapping pins the card; tapping outside closes it.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Focuses the marker and shows the card.' },
        { keys: 'Enter / Space', action: 'Pins the card and moves focus into it.' },
        { keys: 'Tab (in the card)', action: 'Moves between the card’s actions.' },
        {
          keys: 'Escape',
          action: 'Closes the card; focus returns to the marker if it was in the card.',
        },
      ],
    },
    accessibility: {
      role: 'button (marker), dialog (card)',
      name: '`messages.marker(index, title)`, e.g. "Source 1: Team handbook"; the card is "Source 1".',
      items: [
        'The visible `[1]` is hidden from assistive technology; the accessible name says which source it is.',
        'The marker has `aria-haspopup="dialog"` and `aria-expanded`.',
        'The card is non-modal: it never traps focus.',
        'The "Visit page" link announces that it opens in a new tab.',
      ],
    },
    responsive: {
      items: [
        'The card is at most 20rem wide and never wider than the screen minus 2rem; it repositions to stay on screen.',
        'On coarse pointers the marker gets a 44×44px hit area without changing the line height, and card actions are 44px tall.',
      ],
    },
    theme: {
      items: [
        'Marker uses `--mn-color-ai-citation`; open or hovered, `--mn-color-ai-subtle` and `--mn-color-ai-strong`.',
        'Card uses the raised surface, subtle border and large shadow like Popover; the quote has an `--mn-color-ai-border` rule.',
        'Forced colours: the marker uses LinkText and Highlight when open.',
        'RTL: the quote rule uses logical properties.',
      ],
    },
    examples: {
      items: [
        { title: 'Inline citations with a preview', stack: true, render: () => <InlineDemo /> },
        { title: 'In a message', stack: true, render: () => <MessageDemo /> },
      ],
    },
    doDont: {
      do: [
        'Number markers with `aiSourceIndex` so they match AiSourceList.',
        'Pass the citation part’s `quote` so people can see the exact passage.',
        'Use `onOpen` to show the source in AiSourcePreview inside the app.',
      ],
      dont: [
        'Do not show a marker for a source the message does not contain.',
        'Do not parse `[1]` out of model text yourself; render the structured `citation` parts.',
      ],
    },
    related: {
      items: [
        {
          id: 'ai-source-list',
          reason: 'The list of sources under the answer, with the same numbers.',
        },
        { id: 'ai-source-preview', reason: 'Shows a cited source with the quote highlighted.' },
        { id: 'ai-message', reason: 'Renders citation parts through `renderPart`.' },
        { id: 'popover', reason: 'The same non-modal floating panel, opened by click.' },
      ],
    },
    dependencies: {
      packages: ['react', '@radix-ui/react-popover'],
      components: [],
    },
    testing: {
      unit: [
        'Marker is a button named "Source n: title" showing [n].',
        'Focus opens the card without moving focus; Escape closes it.',
        'Hover opens and leaving closes.',
        'Enter pins the card, focus moves in, Escape returns focus to the marker.',
        '`onOpen` receives the source and closes the card.',
        'Unsafe URLs are never linked.',
        'Source helpers: host, meta line and numbering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
