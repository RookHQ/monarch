import { AiToolResultCard, type AiToolResultKind } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms));

const KINDS: { kind: AiToolResultKind; title: string; description: string }[] = [
  { kind: 'record-created', title: 'Northwind Traders', description: 'Added to Accounts' },
  { kind: 'email-sent', title: 'Weekly update', description: 'To 3 people' },
  { kind: 'event-booked', title: 'Design review', description: 'Wed 4 Mar, 10:00–10:30' },
  { kind: 'ticket-filed', title: 'Export fails for large files', description: 'Support queue' },
  { kind: 'generic', title: 'Settings updated', description: 'Notifications turned on' },
];

export default {
  id: 'ai-tool-result-card',
  title: 'AiToolResultCard',
  summary: 'A rich preview of what an assistant tool did, with a link to the result and Undo.',
  sections: {
    purpose: {
      summary:
        'Turns a finished tool call into something people recognise: the record that was created, the email that was sent, the event that was booked or the ticket that was filed, with its key facts, a link to open it and an optional Undo.',
      useWhen: [
        'A tool call succeeded and produced a thing the person may want to open or check.',
        'The action can be reversed and you want to offer Undo right where it is reported.',
      ],
      avoidWhen: [
        'You only need to show that a tool ran and its raw output: use AiToolCall.',
        'The AI changed fields on an existing record: use AiUndoBanner, which shows before and after.',
      ],
    },
    designPrinciples: {
      items: [
        'Show the object, not the operation: the title is the record’s name or the email’s subject.',
        'Keep it scannable: one line of description and a few label/value facts.',
        'Open and Undo are the only actions; anything else belongs on the record itself.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`href` is checked with `isSafeUrl`: relative, http(s) and mailto only. The link always gets `rel="noopener noreferrer nofollow"`; `external` adds `target="_blank"`.',
        '`onUndo` may return a promise; the button shows a spinner until it settles.',
        'Build it from a tool call’s `output` in `AiMessageView renderPart`; Monarch does not guess the mapping.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'kind',
          values: ['record-created', 'email-sent', 'event-booked', 'ticket-filed', 'generic'],
          description: 'Picks the icon and the label above the title. Exposed as `data-kind`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Done', description: 'The default: kind, title, details, Open and Undo.' },
        {
          name: 'Undoing',
          attribute: 'aria-busy on Undo',
          description: 'While `onUndo`’s promise is pending.',
        },
        {
          name: 'Undone',
          attribute: 'data-undone',
          description: 'Title struck through, Open and Undo removed, “Undone” shown and announced.',
        },
        {
          name: 'Undo failed',
          attribute: 'data-status="failed" on the status',
          description: '“Couldn’t undo. Try again.” shown; Undo stays.',
        },
      ],
    },
    interaction: {
      items: [
        'Open follows the link (same tab unless `external`).',
        'Undo calls `onUndo` once per press; a second press is ignored while it runs.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to Open, then Undo.' },
        { keys: 'Enter', action: 'Follows Open or runs Undo.' },
      ],
      notes: ['After a successful undo, focus moves to the card itself so it is not lost.'],
    },
    accessibility: {
      role: 'group',
      name: 'The title, via aria-labelledby',
      items: [
        'The Open link reads “Open” and is described by the title, so it is distinct in a links list.',
        'Undo results are announced through a polite `role="status"`.',
        'Facts are a `<dl>`; the kind icon is decorative.',
      ],
    },
    responsive: {
      items: [
        'Below 480px the actions move under the content. Text wraps; no horizontal scroll at 390px.',
        'The Open link is at least 44px tall on coarse pointers; Undo follows Button.',
      ],
    },
    theme: {
      items: [
        'Card on `--mn-color-bg-surface` with `--mn-color-border-default`; the icon tile uses the success subtle and strong tokens.',
        'Link colour is `--mn-color-text-link`; in forced colours it is LinkText and borders are CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Every kind',
          stack: true,
          render: () => (
            <>
              {KINDS.map((item) => (
                <AiToolResultCard
                  key={item.kind}
                  kind={item.kind}
                  title={item.title}
                  description={item.description}
                  href="#"
                />
              ))}
            </>
          ),
        },
        {
          title: 'With facts and Undo',
          stack: true,
          render: () => (
            <AiToolResultCard
              kind="event-booked"
              title="Design review"
              description="Wed 4 Mar, 10:00–10:30"
              meta={[
                { label: 'Room', value: 'Studio 2' },
                { label: 'Guests', value: '4' },
              ]}
              href="#"
              onUndo={() => wait(600)}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Link to the real record so people can check what the assistant made.',
        'Offer Undo whenever the action can be reversed.',
      ],
      dont: [
        'Do not render model-written URLs without passing them through `href` (it filters unsafe schemes).',
        'Do not list every field: pick the two or three that identify the result.',
      ],
    },
    related: {
      items: [
        { id: 'ai-tool-call', reason: 'The raw tool call with input and output.' },
        { id: 'ai-undo-banner', reason: 'For AI edits to existing fields, with before and after.' },
        { id: 'ai-tool-approval', reason: 'The gate before the action runs.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Kind label, title, description, facts, safe link with rel and description; prop and ref forwarding.',
        'Labels for every kind; unsafe links dropped; external opens in a new tab.',
        'Undo success announces “Undone” and keeps focus on the card; failure lets the person retry.',
        'Message overrides.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
