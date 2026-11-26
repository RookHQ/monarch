import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { AiSummaryCard, type AiSourcePart } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const SUMMARIES = [
  'The customer renewed for another year in **January** and added two seats.\n\n- Usage is steady at about 40 active people a week.\n- One open support ticket about exports.',
  'Renewal is confirmed and the two extra seats are active.\n\n- Weekly active people rose to **46**.\n- The export ticket was closed yesterday.',
];

const sources: AiSourcePart[] = [
  {
    type: 'source',
    id: 'notes',
    title: 'Renewal call notes',
    url: 'https://example.com/notes/renewal',
    kind: 'doc',
  },
  {
    type: 'source',
    id: 'ticket',
    title: 'Ticket 2041: CSV export',
    kind: 'record',
    product: 'Support',
  },
];

function RefreshDemo() {
  const [index, setIndex] = useState(0);
  const [refreshing, setRefreshing] = useState(false);
  const [generatedAt, setGeneratedAt] = useState(() => Date.now() - 3 * 3_600_000);
  return (
    <AiSummaryCard
      title="Account summary"
      summary={SUMMARIES[index] ?? ''}
      generatedAt={generatedAt}
      sources={sources}
      refreshing={refreshing}
      onRefresh={() => {
        setRefreshing(true);
        window.setTimeout(() => {
          setIndex((i) => (i + 1) % SUMMARIES.length);
          setGeneratedAt(Date.now());
          setRefreshing(false);
        }, 1200);
      }}
      actions={
        <Button size="sm" variant="ghost">
          Open in assistant
        </Button>
      }
    />
  );
}

export default {
  id: 'ai-summary-card',
  title: 'AiSummaryCard',
  summary: 'A model-written summary of a record in a card, with sources, age and Refresh.',
  sections: {
    purpose: {
      summary:
        'Shows an AI-written summary of a record or page where people already look (the top of a record, a dashboard tile), marked as AI, with when it was written, what it was written from and a way to refresh it.',
      useWhen: [
        'A record has enough history that a short summary saves reading it all.',
        'The summary is generated ahead of time or on demand, not in a conversation.',
        'People need to know how fresh the summary is and what it was based on.',
      ],
      avoidWhen: [
        'The summary is part of a chat: render it as an assistant message.',
        'A single notable fact: use AiInsightCallout.',
        'Text a person wrote: use Card.',
      ],
    },
    designPrinciples: {
      items: [
        'Always labelled "AI summary" with the Bot glyph, visibly and in the accessibility tree.',
        'Show provenance: when it was generated and the sources it used.',
        'Say when it may be wrong: a stale state when the data changed, an error state when it could not be written.',
        'Keep the previous summary visible while a new one is generated, so the card does not jump or empty.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<article>` attributes pass through and the ref is forwarded to the root.',
        '`summary` is Markdown rendered by AiMarkdown: raw HTML is shown as text, links are checked and images are off.',
        'Summary headings start one level below the card title (`headingLevel`): with the default 3, `#` renders as `<h4>`. Ask the model for `#` headings or none, so levels are not skipped.',
        'Pass `now` from the server with `generatedAt` for byte-identical server rendering of the relative time.',
        'Source links must pass the URL safety check (`http`, `https`, `mailto` or relative) and get `rel="noopener noreferrer nofollow"`; other sources show their title as text.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'status',
          values: ['ready', 'generating', 'stale', 'error'],
          description:
            'Lifecycle of the summary. Exposed as `data-status`; `refreshing` shows as `generating`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Ready',
          attribute: 'data-status="ready"',
          description: 'The summary and its metadata.',
        },
        {
          name: 'Generating',
          attribute: 'data-status="generating", aria-busy="true"',
          description:
            'With `status="generating"` or `refreshing`: Refresh shows a spinner, a previous summary is dimmed, an empty card shows "Generating summary…" with skeleton lines. "Generating summary…" and then "Summary updated." are announced politely.',
        },
        {
          name: 'Stale',
          attribute: 'data-status="stale"',
          description: 'A warning line says the data changed since the summary was written.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description: 'An assertive error line with optional `error` detail.',
        },
        { name: 'Empty', description: 'No summary and nothing generating: "No summary yet."' },
      ],
    },
    interaction: {
      items: [
        'Refresh calls `onRefresh`; the app sets `refreshing` or `status="generating"` until the new summary arrives.',
        'Refresh stays focusable while busy and ignores clicks, so keyboard users keep their place.',
        'The generated time shows the absolute time on hover and focus.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through Refresh, links in the summary, sources, the time and actions.',
        },
        { keys: 'Enter / Space', action: 'Activates Refresh or a link.' },
      ],
      notes: ['The card never moves focus.'],
    },
    accessibility: {
      role: 'article',
      name: 'The title (aria-labelledby)',
      items: [
        'AI provenance is the visible "AI summary" label inside the article, read before the title.',
        '`aria-busy="true"` while generating; a polite status region announces the start and end of generation, never on first render.',
        'The error line is `role="alert"`; the stale line is plain text.',
        'Refresh is described by the card title, so several cards’ Refresh buttons are distinguishable.',
        'Sources are an ordered list labelled "Sources", matching numbered citations.',
        'Skeleton lines are hidden from assistive technology.',
      ],
    },
    responsive: {
      items: [
        'The card fills its container; text, links and sources wrap, so there is no horizontal scroll at 390px.',
        'Padding tightens below 30rem; on coarse pointers Refresh and source links are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Card: `--mn-color-bg-surface`, `--mn-color-border-default`, `--mn-radius-lg`.',
        'AI label: `--mn-color-ai-subtle`, `--mn-color-ai-strong`, `--mn-color-ai-border`.',
        'Stale and error lines use the warning and danger subtle, border and strong tokens.',
        'In forced colours borders use CanvasText and links LinkText; skeletons stop shimmering under reduced motion.',
      ],
    },
    examples: {
      items: [
        { title: 'Refresh with sources', stack: true, render: () => <RefreshDemo /> },
        {
          title: 'Stale',
          stack: true,
          render: () => (
            <AiSummaryCard
              title="Project summary"
              summary="Design is complete and build started last week. Two tasks are blocked on a vendor reply."
              generatedAt={Date.now() - 5 * 86_400_000}
              status="stale"
              onRefresh={() => {}}
            />
          ),
        },
        {
          title: 'Generating for the first time',
          stack: true,
          render: () => <AiSummaryCard title="Meeting summary" summary="" status="generating" />,
        },
        {
          title: 'Error',
          stack: true,
          render: () => (
            <AiSummaryCard
              title="Account summary"
              summary=""
              status="error"
              error="The request timed out."
              onRefresh={() => {}}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `generatedAt` and `sources` so people can judge the summary.',
        'Set `status="stale"` when the record changes after the summary was written.',
        'Keep summaries short: a few sentences or bullets.',
      ],
      dont: [
        'Do not regenerate on every page view; cache and offer Refresh.',
        'Do not remove the AI label or present the summary as written by a person.',
        'Do not pass HTML; pass Markdown.',
      ],
    },
    related: {
      items: [
        { id: 'ai-markdown', reason: 'Renders the summary text.' },
        { id: 'ai-insight-callout', reason: 'A single notable fact rather than a summary.' },
        { id: 'card', reason: 'Content written by people.' },
        { id: 'relative-time', reason: 'Shows when the summary was generated.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'relative-time', 'skeleton', 'ai-markdown'],
    },
    testing: {
      unit: [
        'Article named by the title, labelled "AI summary", with Markdown rendered and headings offset; props and ref forwarded.',
        'Raw HTML in the summary is shown as text.',
        'Sources as an ordered list; unsafe URLs are not linked; safe links get rel noopener noreferrer nofollow.',
        'Refresh runs from the keyboard and is described by the title.',
        'Busy state, polite announcements on start and finish only, and the previous summary kept while generating.',
        'Placeholder, empty, stale and error states; messages and extra actions.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
