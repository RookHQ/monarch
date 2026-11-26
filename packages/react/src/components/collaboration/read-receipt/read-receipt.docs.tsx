import { useState } from 'react';
import { ReadReceipt, type ReadReceiptReader } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const READERS: ReadReceiptReader[] = [
  { id: 'u1', name: 'Ana Lopez' },
  { id: 'u2', name: 'Ben Okafor' },
  { id: 'u3', name: 'Chloé Martin' },
  { id: 'u4', name: 'Dev Patel' },
  { id: 'u5', name: 'Eve Stone' },
];

function ShowReadersDemo() {
  const [shown, setShown] = useState(false);
  return (
    <div style={{ display: 'grid', gap: '0.5rem', width: '100%' }}>
      <ReadReceipt readers={READERS} display="count" onShowReaders={() => setShown((s) => !s)} />
      {shown ? (
        <ul style={{ margin: 0, textAlign: 'end' }}>
          {READERS.map((r) => (
            <li key={r.id}>{r.name}</li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

export default {
  id: 'read-receipt',
  title: 'ReadReceipt',
  summary: '“Seen”, “Seen by 3” or readers’ avatars under the newest message others have read.',
  sections: {
    purpose: {
      summary:
        'Tells the sender that the other people in a conversation have read their message, without making them ask. The product decides which message carries the receipt and who has read it. Needed by: Chat (ReadReceipt in direct and group messages), Developers (support chat widget “Seen”).',
      useWhen: [
        'Under the sender’s newest read message in a direct or small group conversation.',
        'A support conversation where the customer wants to know an agent has seen their message.',
      ],
      avoidWhen: [
        'Large channels, where a read count is noise and can feel like surveillance.',
        'Delivery or failure states of a message: MessageRow shows sending and failed.',
      ],
    },
    designPrinciples: {
      items: [
        'One receipt per conversation, on the newest message read, not one per message.',
        'Short on screen, complete for assistive technology: whatever the display, the full “Seen by …” list of names is read out and shown as a tooltip.',
        'The product owns privacy: if a workspace turns read receipts off, do not render the component.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref is forwarded to the root `<p>`. Nothing renders, and the ref stays null, while `readers` is empty.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy: Chat `ReadReceipt({ readers: string[], isGroup })` took ids and looked names up in context; pass `readers` as `{ id, name }` objects and use `display="count"` where `isGroup` was true (`"text"` otherwise). Chat’s `useReceiptTarget` (choosing the message) stays in the product. The support widget’s `seen` string maps to `messages.seen`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'display',
          values: ['text', 'count', 'avatars'],
          description:
            '“Seen” with a double check, “Seen by 3”, or up to `maxAvatars` small faces followed by “Seen”.',
        },
        {
          prop: 'align',
          values: ['end', 'start'],
          description: 'End (default) for the sender’s own messages on the right; start otherwise.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Hidden', description: 'No readers yet: nothing is rendered.' },
        {
          name: 'Read',
          description: 'At least one reader: the chosen display is shown.',
          attribute: 'data-display',
        },
        {
          name: 'Expandable',
          description:
            'With `onShowReaders` the receipt is a button that opens the product’s reader list.',
        },
      ],
    },
    interaction: {
      items: [
        'Hovering shows the full list of names as a tooltip (`title`).',
        'With `onShowReaders`, clicking or pressing Enter or Space calls it; the product opens a popover or sheet with the readers.',
      ],
    },
    keyboard: {
      items: [{ keys: 'Enter / Space', action: 'Calls `onShowReaders` when it is set.' }],
      notes: ['Without `onShowReaders` the receipt is text and not a tab stop.'],
    },
    accessibility: {
      role: 'paragraph (a button inside it with `onShowReaders`)',
      name: 'The full sentence, e.g. “Seen by Ana Lopez and Ben Okafor”, in visually hidden text.',
      items: [
        'The icon, the avatars and the short visible label are `aria-hidden`, so the sentence is not read twice.',
        'The avatars are decorative; names are in the sentence.',
        'Names are joined with `messages.list`, which you can translate.',
      ],
    },
    responsive: {
      items: [
        'Stays on one line; avatars overlap to keep it short.',
        'On touch screens the button grows to a 44px target.',
      ],
    },
    theme: {
      items: [
        'Text and icon use `text-muted`; hover and focus use `text-default` and the focus ring token.',
        'Avatars get a ring in `bg-surface` so they separate in every theme. Forced colours use `CanvasText` and `LinkText`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Displays',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: '0.75rem', width: '100%' }}>
              <ReadReceipt readers={READERS.slice(0, 1)} />
              <ReadReceipt readers={READERS} display="count" />
              <ReadReceipt readers={READERS} display="avatars" />
            </div>
          ),
        },
        { title: 'Open the list of readers', render: () => <ShowReadersDemo /> },
      ],
    },
    doDont: {
      do: [
        'Show it only on the newest message the others have read.',
        'Respect the workspace setting and each person’s privacy choice before rendering it.',
      ],
      dont: [
        'Do not show read counts in large channels.',
        'Do not include the sender in `readers`.',
      ],
    },
    related: {
      items: [
        { id: 'message-row', reason: 'Put the receipt in the row’s footer.' },
        { id: 'typing-indicator', reason: 'The other conversation status line.' },
        { id: 'avatar', reason: 'The faces in the avatars display.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['avatar'] },
    testing: {
      unit: [
        'Text, count and avatars displays, with the full sentence hidden for screen readers and in `title`.',
        'Empty readers render nothing; avatar overflow and the no-“+1” rule.',
        'Button with `onShowReaders`, reachable with Tab and Enter.',
        'Translated text and alignment.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
