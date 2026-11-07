import { useState } from 'react';
import { AiMessageActions, type AiFeedback } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function Demo() {
  const [feedback, setFeedback] = useState<AiFeedback | undefined>();
  const [last, setLast] = useState('Nothing yet');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)' }}>
      <AiMessageActions
        visibility="always"
        messageText="The draft has three sections and a list of next steps."
        onCopy={(text) => navigator.clipboard?.writeText(text)}
        onRegenerate={() => setLast('Regenerate')}
        feedback={feedback}
        onFeedback={(f) => {
          setFeedback(f);
          setLast(f ? `Rated ${f}` : 'Rating cleared');
        }}
        onShare={() => setLast('Share')}
        onReadAloud={(speaking) => setLast(speaking ? 'Reading aloud' : 'Stopped reading')}
      />
      <span>Last action: {last}</span>
    </div>
  );
}

export default {
  id: 'ai-message-actions',
  title: 'AiMessageActions',
  summary: 'Copy, regenerate, edit, rate, share and read-aloud toolbar for chat messages.',
  sections: {
    purpose: {
      summary:
        'The row of icon buttons under a chat message. Each action appears only when its callback is given; AiMessageView wires it for you.',
      useWhen: [
        'A message needs per-message actions: copy, regenerate, rate, share or read aloud.',
        'You render messages yourself and want the standard toolbar.',
      ],
      avoidWhen: [
        'You render messages with AiMessageView: it already includes this toolbar.',
        'The actions are page-level (export the whole conversation): use Button or DropdownMenu.',
      ],
    },
    designPrinciples: {
      items: [
        'Only offer what works: no callback, no button.',
        'One tab stop for the whole row; arrow keys move inside it.',
        'Quiet until needed on pointer devices, always visible on touch.',
        'Ratings are toggles, exposed with `aria-pressed`, and can be cleared.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the `role="toolbar"` element.',
        '`onCopy` replaces the DOM clipboard event of the same name: it receives the message text.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'visibility',
          values: ['hover', 'always'],
          description:
            '`hover` (default): hidden until the message (`.mn-ai-message`) or the toolbar is hovered or focused, on hover-capable devices only. `always`: always visible.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Pressed rating',
          attribute: 'aria-pressed="true", data-pressed',
          description: 'The current thumbs rating.',
        },
        {
          name: 'Copied',
          attribute: 'data-copied',
          description: 'Copy shows a check and "Copied" for two seconds.',
        },
        {
          name: 'Reading aloud',
          attribute: 'data-speaking',
          description: 'Read aloud becomes "Stop reading".',
        },
      ],
    },
    interaction: {
      items: [
        'Pressing the pressed thumb again calls `onFeedback(undefined)`.',
        'Read aloud uses the browser’s speech synthesis on `messageText`; it is hidden where speech synthesis is missing and only appears after mount (SSR-safe).',
        'Each button has a tooltip with its label.',
        'Controls in `more` join the arrow-key order.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Enters the toolbar on the last used button; Tab again leaves it.' },
        {
          keys: 'Arrow Right / Arrow Left',
          action: 'Next / previous button, wrapping (mirrored in RTL).',
        },
        { keys: 'Home / End', action: 'First / last button.' },
        { keys: 'Enter / Space', action: 'Activates the button.' },
      ],
    },
    accessibility: {
      role: 'toolbar',
      name: '`messages.toolbar` ("Message actions").',
      items: [
        'Every icon button has a text label (`IconButton label`) and a matching tooltip.',
        'Thumbs use `aria-pressed`; Copy and Read aloud change their label to report their state.',
        'Hidden-until-hover is opacity only, so the buttons stay focusable and appear when focused.',
      ],
    },
    responsive: {
      items: [
        'Wraps on narrow widths.',
        'Buttons grow to 44×44px on coarse pointers, and stay visible on touch.',
      ],
    },
    theme: {
      items: [
        'Muted text colour at rest; a pressed rating uses `--mn-color-ai-subtle` and `--mn-color-ai-strong`.',
        'Forced colours: always visible, pressed buttons use Highlight.',
      ],
    },
    examples: { items: [{ title: 'All actions', stack: true, render: () => <Demo /> }] },
    doDont: {
      do: ['Send ratings to your feedback API in `onFeedback`.', 'Pass translated `messages`.'],
      dont: [
        'Do not add a callback for an action that does nothing.',
        'Do not put destructive actions here without a confirmation.',
      ],
    },
    related: {
      items: [
        { id: 'ai-message', reason: 'Renders a message with this toolbar wired up.' },
        { id: 'copy-button', reason: 'A standalone copy control.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'tooltip'] },
    testing: {
      unit: [
        'Labelled toolbar with only the actions that have callbacks.',
        'Roving tabindex: arrows, Home and End, one tab stop.',
        'Thumbs aria-pressed from `feedback`, toggling off.',
        'Copy passes the text and shows "Copied".',
        'Read aloud speaks with speech synthesis and toggles to Stop; hidden without it.',
        'Translated labels.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
