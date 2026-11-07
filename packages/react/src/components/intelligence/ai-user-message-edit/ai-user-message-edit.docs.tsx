import { useState } from 'react';
import { AiUserMessageEdit } from '@rookcom/monarch-react/ai';
import { Button } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function Demo() {
  const [sent, setSent] = useState(['List the open tasks for the launch.']);
  const [editing, setEditing] = useState(false);
  const current = sent[sent.length - 1] ?? '';
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%', maxWidth: '36rem' }}>
      {editing ? (
        <AiUserMessageEdit
          defaultValue={current}
          onSubmit={(text) => {
            setSent((list) => [...list, text]);
            setEditing(false);
          }}
          onCancel={() => setEditing(false)}
        />
      ) : (
        <div style={{ display: 'flex', gap: 'var(--mn-space-2)', alignItems: 'center' }}>
          <p style={{ margin: 0, flex: 1 }}>{current}</p>
          <Button size="sm" onClick={() => setEditing(true)}>
            Edit
          </Button>
        </div>
      )}
      <p style={{ margin: 0, color: 'var(--mn-color-text-muted)' }}>Branches: {sent.length}</p>
    </div>
  );
}

export default {
  id: 'ai-user-message-edit',
  title: 'AiUserMessageEdit',
  summary: 'Inline editor for a sent prompt that resends it as a new branch.',
  sections: {
    purpose: {
      summary:
        'Lets people fix or refine a prompt they already sent. Resending creates a new branch of the conversation, so the original prompt and its reply stay available through the version switcher.',
      useWhen: [
        'A person chooses Edit on one of their own messages in a thread.',
        'Building a custom message layout that needs the same editor AiMessageView shows.',
      ],
      avoidWhen: [
        'Writing a new prompt: use AiPromptInput.',
        'Editing assistant output: use AiEditProposal or an artifact editor.',
      ],
    },
    designPrinciples: {
      items: [
        'Honest about consequences: the hint says a new branch is created and the original is kept.',
        'Same keys as the prompt input: Enter sends, Shift+Enter adds a line, Escape cancels.',
        'Nothing is resent when the text is empty or unchanged.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<form>`; native form attributes pass through and the ref is forwarded to it.',
        '`onSubmit(text)` receives the trimmed text. With `useAiConversation`, call `editAndResend(messageId, text)`.',
        'The field starts with `defaultValue`; remount it (change `key`) to start over.',
        '`AI_USER_MESSAGE_EDIT_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: { items: [], notes: ['There are no visual variants.'] },
    states: {
      items: [
        { name: 'Unchanged', description: 'Send is disabled until the text differs.' },
        { name: 'Edited', description: 'Send is enabled.' },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'Field and Send are disabled, for example while a reply streams.',
        },
      ],
    },
    interaction: {
      items: [
        'The field takes focus with the caret at the end when it opens (`focusOnMount`, default on).',
        'It grows with its content up to `maxRows` lines, then scrolls.',
        'Send or Enter calls `onSubmit`; Cancel or Escape calls `onCancel`.',
        'Escape does not bubble, so it never also stops a reply or closes a surrounding panel.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter', action: 'Sends the edited prompt.' },
        { keys: 'Shift+Enter', action: 'Adds a new line.' },
        { keys: 'Escape', action: 'Cancels the edit.' },
        { keys: 'Tab', action: 'Moves to Cancel and Send.' },
      ],
      notes: ['Enter while an input method is composing text is left to the input method.'],
    },
    accessibility: {
      role: 'form',
      name: '"Edit message" (aria-label)',
      items: [
        'The field is named "Edit message" and described by the branch hint and the keyboard hint.',
        'The keyboard hint is visually hidden; the branch hint is visible text with a decorative icon.',
      ],
    },
    responsive: {
      items: [
        'Fills its container up to 40rem; the hint wraps above the buttons on narrow screens.',
        'On coarse pointers the buttons are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Field: `--mn-color-border-input`, `--mn-color-bg-surface`, `--mn-color-focus-ring`.',
        'Hint: `--mn-color-text-muted`.',
        'In forced colours the field border uses CanvasText and focus uses Highlight.',
      ],
    },
    examples: {
      items: [{ title: 'Edit and resend', stack: true, render: () => <Demo /> }],
    },
    doDont: {
      do: [
        'Put it in place of the person’s message, so they edit where they read.',
        'Keep the original version reachable with AiMessageBranch.',
      ],
      dont: [
        'Do not overwrite the original prompt or its reply.',
        'Do not resend on blur; sending is always explicit.',
      ],
    },
    related: {
      items: [
        { id: 'ai-message', reason: 'Shows this kind of editor for the person’s messages.' },
        { id: 'ai-message-branch', reason: 'Switches between the original and edited versions.' },
        { id: 'ai-prompt-input', reason: 'For new prompts.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Named form, focused field with caret at the end, described by both hints; props and ref forwarded.',
        'Enter sends trimmed text, Shift+Enter adds a line, Send button.',
        'Empty and unchanged text are not sent.',
        'Escape cancels without bubbling; Cancel button.',
        'focusOnMount off, disabled and messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
