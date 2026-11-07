import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { TypingIndicator, type TypingPerson } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const PEOPLE: TypingPerson[] = [
  { id: 'u1', name: 'Ana' },
  { id: 'u2', name: 'Ben' },
  { id: 'u3', name: 'Chloé' },
];

function TypingDemo() {
  const [count, setCount] = useState(1);
  return (
    <div style={{ display: 'grid', gap: '0.75rem', width: '100%' }}>
      <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
        {[0, 1, 2, 3].map((n) => (
          <Button
            key={n}
            size="sm"
            variant={n === count ? 'primary' : 'secondary'}
            aria-pressed={n === count}
            onClick={() => setCount(n)}
          >
            {n === 0 ? 'Nobody' : `${n} typing`}
          </Button>
        ))}
      </div>
      <TypingIndicator people={PEOPLE.slice(0, count)} />
    </div>
  );
}

export default {
  id: 'typing-indicator',
  title: 'TypingIndicator',
  summary: '“Ana is typing…” with decorative dots, worded for one, two or several people.',
  sections: {
    purpose: {
      summary:
        'Tells the people in a conversation that someone is writing a reply, so they wait instead of leaving. The product supplies who is typing from its realtime layer; the component words it and announces it. Needed by: Chat (TypingIndicator), Tickets (live chat VisitorTyping), Developers (support chat widget).',
      useWhen: [
        'Under a chat, direct message or thread while other people are typing.',
        'A support conversation where the visitor or agent is writing.',
      ],
      avoidWhen: [
        'An AI assistant generating a reply: use AiTypingDots, which carries the assistant colour.',
        'Long-running work that is not a person typing: use Spinner or ProgressBar.',
      ],
    },
    designPrinciples: {
      items: [
        'Names before numbers: one or two names are read out; three or more becomes “Several people”, which stays short and never lists a crowd.',
        'The sentence carries the meaning; the dots are decoration and stand still under reduced motion.',
        'The line keeps its height when empty, so messages above do not jump every time someone starts or stops.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref is forwarded to the root `<div>`.',
        '`formatTyping(people, messages?)` returns the same sentence as plain text, for a page title or a conversation list row.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy: Chat `TypingIndicator({ k })` read the store itself; pass `people` (names already resolved, first names if the product prefers them, current user excluded) instead. Tickets `VisitorTyping({ name })` becomes `people={[{ name }]}`. The support widget strings `typingAgent` and `typingSomeone` map to `messages.one` (pass `"Someone"` as the name when it is unknown).',
      ],
    },
    variants: {
      items: [],
      notes: ['There are no visual variants; the wording changes with the number of people.'],
    },
    states: {
      items: [
        {
          name: 'Idle',
          description: 'Nobody typing: empty, keeping its height unless `reserveSpace={false}`.',
          attribute: 'data-reserve-space',
        },
        {
          name: 'Active',
          description: 'Someone typing: dots and the sentence are shown and announced.',
          attribute: 'data-active',
        },
      ],
    },
    interaction: {
      items: [],
      notes: ['Not interactive: it only reflects what the product passes in `people`.'],
    },
    keyboard: {
      items: [],
      notes: ['Not focusable and has no controls.'],
    },
    accessibility: {
      role: 'status',
      name: 'None; its text is the announcement.',
      items: [
        'A polite live region that stays mounted while empty, so the first “is typing” is announced.',
        'The dots are `aria-hidden`; the ellipsis is part of the sentence.',
        'Pass `announce={false}` where typing would be announced too often, such as a busy channel; the text is still visible.',
      ],
    },
    responsive: {
      items: ['The sentence truncates with an ellipsis rather than wrapping on narrow screens.'],
    },
    theme: {
      items: [
        'Text and dots use `text-muted`.',
        'Forced colours draw the dots in `CanvasText` and stop the animation.',
      ],
    },
    examples: {
      items: [
        { title: 'One, two and several people', render: () => <TypingDemo /> },
        {
          title: 'Translated text',
          render: () => (
            <TypingIndicator
              people={[{ name: 'Ana' }]}
              messages={{ one: (name) => `${name} está escribiendo…` }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Leave the current user out of `people`.',
        'Clear a person after a few seconds without a typing event, in the product, so a closed tab does not type forever.',
      ],
      dont: [
        'Do not show it for an assistant; use AiTypingDots.',
        'Do not put it inside the scrolling message list where it would scroll away; place it under the list, above the composer.',
      ],
    },
    related: {
      items: [
        { id: 'ai-typing-dots', reason: 'The assistant equivalent.' },
        { id: 'message-timeline', reason: 'The conversation it sits under.' },
        { id: 'read-receipt', reason: 'The other conversation status line.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Wording for one, two and several people, and translated text.',
        'Polite status region that stays mounted while empty and keeps its height.',
        '`announce={false}` and `reserveSpace={false}`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
