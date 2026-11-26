import { useState } from 'react';
import { AiFeedback, type AiFeedbackSubmission } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function Demo({ askDetails }: { askDetails?: 'down' | 'both' }) {
  const [value, setValue] = useState<AiFeedback | undefined>();
  const [sent, setSent] = useState<AiFeedbackSubmission | null>(null);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)' }}>
      <AiFeedback
        value={value}
        onChange={setValue}
        askDetails={askDetails}
        onSubmit={async (submission) => {
          // A product would POST { messageId, ...submission } to its feedback API here.
          await new Promise((resolve) => setTimeout(resolve, 400));
          setSent(submission);
        }}
      />
      <code style={{ fontSize: 'var(--mn-text-xs)' }}>
        {sent ? JSON.stringify(sent) : 'Nothing sent yet'}
      </code>
    </div>
  );
}

export default {
  id: 'ai-feedback',
  title: 'AiFeedback',
  summary: 'Thumbs up and down with reasons, a comment and a thank-you.',
  sections: {
    purpose: {
      summary:
        'Collects a rating for an AI answer and, after a thumbs down, why: reason chips and an optional comment. The rating is recorded at once; the details are sent when the person submits.',
      useWhen: [
        'You want structured feedback on AI answers to improve prompts, retrieval or models.',
        'Under an answer, summary or generated draft.',
      ],
      avoidWhen: [
        'A plain rating is enough: AiMessageActions already has thumbs.',
        'Rating a person’s work or a product: use a rating or survey component.',
      ],
    },
    designPrinciples: {
      items: [
        'One click to rate; details are optional and never block the rating.',
        'Thumbs are toggles (`aria-pressed`) that can be cleared.',
        'Reasons are chips, not a menu: everything is visible and one tap away.',
        'Sending is the product’s job: the component hands over a typed payload.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`.',
        "`AiFeedback` is also the rating type (`'up' | 'down'`), the same one AiMessageActions and `AiMessage.feedback` use.",
        'Payload to store, for example `POST /v1/feedback`: `{ messageId, value: "up" | "down", reasons: string[], comment: string }`. `onChange` can update the conversation with `useAiConversation().setFeedback(messageId, value)`; `onSubmit` sends `{ value, reasons, comment }` with the message id added by the product.',
        '`AI_FEEDBACK_DOWN_REASONS` holds the default chips: inaccurate, not-helpful, incomplete, hard-to-follow, unsafe.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'askDetails',
          values: ['down', 'both', 'none'],
          description:
            'Which ratings open the follow-up form. `down` (default): thumbs up sends at once and thanks. `none`: every rating sends at once.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Unrated',
          attribute: 'data-phase="idle"',
          description: 'Two thumbs, neither pressed.',
        },
        {
          name: 'Rated',
          attribute: 'data-value="up|down", aria-pressed="true"',
          description: 'The chosen thumb is pressed.',
        },
        {
          name: 'Details',
          attribute: 'data-phase="details"',
          description: 'Reason chips, a comment box, Send and Cancel.',
        },
        {
          name: 'Sending',
          attribute: 'data-phase="pending", aria-busy',
          description: '`onSubmit` returned a promise; Send shows a spinner.',
        },
        {
          name: 'Thanks',
          attribute: 'data-phase="thanks"',
          description: '"Thanks for your feedback." in a status region.',
        },
        {
          name: 'Error',
          description: 'The promise rejected: an alert, and the form stays filled in.',
        },
      ],
    },
    interaction: {
      items: [
        'Pressing a thumb calls `onChange` at once; pressing the pressed thumb clears it (`onChange(undefined)`) and closes the form.',
        'Chips toggle; `reasons` in the payload follow the listed order.',
        'Cancel closes the form and keeps the rating.',
        'After sending from the form, focus moves to the thank-you message; a direct send (thumbs up) leaves focus on the thumb.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Thumbs, then chips, comment, Send and Cancel.' },
        { keys: 'Enter / Space', action: 'Presses a thumb or chip.' },
        { keys: 'Enter (Send focused)', action: 'Sends the form.' },
      ],
    },
    accessibility: {
      role: 'group (thumbs), form (details)',
      name: '`messages.group` ("Rate this response"); thumbs are "Good response" and "Bad response".',
      items: [
        'Thumbs and chips use `aria-pressed`; pressed chips also show a check, so state is never colour alone.',
        'The chip group is labelled by its question; the comment has a visible label.',
        'The thank-you is a polite status region; errors are alerts.',
      ],
    },
    responsive: {
      items: [
        'Chips wrap; the form fills the available width up to 32rem.',
        'Thumbs and chips are 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Pressed thumbs and chips use `--mn-color-ai-subtle`, `--mn-color-ai-strong` and `--mn-color-ai-border`.',
        'Forced colours: pressed controls use Highlight.',
      ],
    },
    examples: {
      items: [
        { title: 'Default (details on thumbs down)', stack: true, render: () => <Demo /> },
        { title: 'Details on both', stack: true, render: () => <Demo askDetails="both" /> },
      ],
    },
    doDont: {
      do: [
        'Store the rating in `onChange` so it survives if the form is not sent.',
        'Send the message id, model and conversation id with the payload.',
        'Pass translated `messages` and reasons.',
      ],
      dont: [
        'Do not require a reason or comment.',
        'Do not show both this and the AiMessageActions thumbs for the same message.',
      ],
    },
    related: {
      items: [
        { id: 'ai-message-actions', reason: 'Has plain thumbs for the message toolbar.' },
        { id: 'ai-message', reason: 'The message being rated.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'text-field'] },
    testing: {
      unit: [
        'Labelled thumbs with aria-pressed.',
        'Thumbs down: reasons, comment, payload, thank-you focus.',
        'Thumbs up sends at once by default.',
        'askDetails="both" with custom reasons.',
        'Clearing the rating; Cancel returns focus.',
        'Pending and error states.',
        'Uncontrolled use and translated labels.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
