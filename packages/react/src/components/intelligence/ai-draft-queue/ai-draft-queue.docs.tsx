import { useState } from 'react';
import { AiDraftQueue, type AiDraft } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const DRAFTS: AiDraft[] = [
  {
    id: '1',
    title: 'Reply to Alex Morgan',
    context: 'Could we move Thursday’s review to the afternoon?',
    body: 'Hi Alex,\n\nThursday afternoon works. Shall we say 2 pm?\n\nThanks',
    status: 'draft',
  },
  {
    id: '2',
    title: 'Reply to Sam Lee',
    context: 'Is the updated report ready to share?',
    body: 'Hi Sam,\n\nYes, the updated report is attached. Let me know if anything needs changing.',
    status: 'draft',
  },
  {
    id: '3',
    title: 'Reply to Kim Park',
    context: 'Thanks for the notes from yesterday.',
    body: 'You are welcome. Glad they helped.',
    status: 'regenerating',
  },
];

function QueueDemo({ shortcuts = false }: { shortcuts?: boolean }) {
  const [drafts, setDrafts] = useState(DRAFTS);
  const set = (id: string, patch: Partial<AiDraft>) =>
    setDrafts((ds) => ds.map((d) => (d.id === id ? { ...d, ...patch } : d)));
  return (
    <div style={{ width: '100%', maxWidth: '40rem' }}>
      <AiDraftQueue
        title="Replies to review"
        drafts={drafts}
        keyboardShortcuts={shortcuts}
        onEdit={(id, body) => set(id, { body })}
        onRegenerate={(id) => {
          set(id, { status: 'regenerating' });
          setTimeout(
            () => set(id, { status: 'draft', body: 'A new version of the reply, written again.' }),
            1500,
          );
        }}
        onApprove={(id) => set(id, { status: 'approved' })}
        onSend={(id) => set(id, { status: 'sent' })}
        onSkip={(id) => set(id, { status: 'skipped' })}
      />
    </div>
  );
}

export default {
  id: 'ai-draft-queue',
  title: 'AiDraftQueue',
  summary: 'Review queue for AI-written drafts: approve, edit, regenerate, skip, send.',
  sections: {
    purpose: {
      summary:
        'Lets a person review a batch of AI-written drafts (replies, follow-ups, notes) one by one, with the context each answers, and decide on each before anything is sent. Focus moves on after every decision so a queue can be cleared quickly from the keyboard.',
      useWhen: [
        'The AI prepares several drafts that a person must approve before they go out.',
        'Bulk follow-ups, suggested replies, generated summaries waiting for sign-off.',
      ],
      avoidWhen: [
        'A single draft in an editor (show it in the editor with AiGeneratedBadge).',
        'Changes to an existing document (use AiEditProposal).',
      ],
    },
    designPrinciples: {
      items: [
        'Human in the loop: nothing is sent without an explicit Approve (and Send, when you pass `onSend`).',
        'Context next to the draft: show what it answers so it can be judged.',
        'Plain text: the body is untrusted model output and is never rendered as HTML.',
        'Keyboard shortcuts are opt-in and never fire while typing.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The queue is controlled: each callback asks you to change a draft’s `status` (or `body`); the queue renders what you pass.',
        'The ref goes to the root `<section>`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'keyboardShortcuts',
          values: ['false', 'true'],
          description: 'J / K move to the next / previous draft, with a visible hint.',
        },
        {
          prop: 'advanceOnAction',
          values: ['true', 'false'],
          description: 'After approve, send or skip, focus the next draft that needs review.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Needs review',
          attribute: 'data-status="draft"',
          description: 'Approve, Edit, Regenerate and Skip.',
        },
        {
          name: 'Writing',
          attribute: 'data-status="regenerating", aria-busy',
          description: '"AI is still writing…" replaces the body; no actions.',
        },
        {
          name: 'Approved',
          attribute: 'data-status="approved"',
          description: 'Send (with `onSend`) and Edit.',
        },
        { name: 'Sent', attribute: 'data-status="sent"', description: 'Read only.' },
        { name: 'Skipped', attribute: 'data-status="skipped"', description: 'Read only, muted.' },
        { name: 'Editing', description: 'A text box with Save and Cancel replaces the body.' },
        { name: 'Empty', description: '"No drafts to review." or your `empty` content.' },
      ],
    },
    interaction: {
      items: [
        'Approve, Send and Skip move focus to the next draft that needs review (wrapping to the top), or keep it on the same draft when none is left.',
        'Edit opens a text box with the draft; Save calls `onEdit(id, body)` and returns focus to the draft.',
        'Regenerate calls `onRegenerate(id)`; set the status to `regenerating` until the new body arrives.',
        'Status changes are announced politely, e.g. "Approved: Reply to Alex Morgan".',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'J / K',
          action: 'Next / previous draft (with `keyboardShortcuts`; not while typing).',
        },
        { keys: '⌘Enter / Ctrl+Enter', action: 'Saves an edit.' },
        { keys: 'Escape', action: 'Cancels an edit.' },
        { keys: 'Tab', action: 'Moves through each draft’s actions.' },
      ],
    },
    accessibility: {
      role: 'region with an ordered list of articles',
      name: 'The `title` heading, or `messages.label` ("Drafts to review").',
      items: [
        'Each draft is an article named by its title heading (level `headingLevel`, default 3).',
        'Each body starts with a visually hidden "AI draft:" so it is identified as AI-written.',
        'Statuses are shown with an icon and text, never colour alone.',
        'A regenerating draft is `aria-busy` and its indicator announces "AI is still writing…" once.',
        'One polite status region announces decisions; focus moves only in response to the person’s own action.',
      ],
    },
    responsive: {
      items: [
        'Cards stack full width; actions wrap; long titles and bodies wrap without horizontal scroll at 390px.',
        'Action buttons are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'AI intent tokens for drafts needing review (`--mn-color-ai-border`, `--mn-color-ai-subtle`, `--mn-color-ai-strong`), `--mn-color-ai-thinking` while writing, success tokens once approved or sent.',
        'Forced colours give cards CanvasText borders and a Highlight focus outline.',
      ],
    },
    examples: {
      items: [
        { title: 'Review queue', stack: true, render: () => <QueueDemo /> },
        { title: 'With J / K shortcuts', stack: true, render: () => <QueueDemo shortcuts /> },
        {
          title: 'Empty',
          render: () => (
            <AiDraftQueue
              drafts={[]}
              onEdit={() => {}}
              onRegenerate={() => {}}
              onApprove={() => {}}
              onSkip={() => {}}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Show the context the draft answers.',
        'Set `regenerating` while a new version is written, so the old one cannot be approved by mistake.',
        'Turn on `keyboardShortcuts` for queues people work through daily, and say so in your help.',
      ],
      dont: [
        'Do not send on Approve when the product promises a separate Send step.',
        'Do not render model output as HTML in `context`.',
      ],
    },
    related: {
      items: [
        { id: 'ai-generated-badge', reason: 'Marks a single AI-written value or text.' },
        { id: 'ai-edit-proposal', reason: 'For AI changes to an existing document.' },
        { id: 'ai-thinking-indicator', reason: 'The "AI is still writing…" state.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'text-field', 'kbd', 'ai-thinking-indicator'],
    },
    testing: {
      unit: [
        'Named region, heading levels, status, context, progress and the hidden AI prefix.',
        'Approve moves focus to the next draft, updates progress and announces; Send on approved drafts.',
        'Skip wraps focus to the first draft needing review.',
        'Edit with ⌘/Ctrl+Enter to save and Escape to cancel.',
        'Regenerate callback; a regenerating draft is busy with no actions.',
        'J / K only when opted in and never while typing.',
        'Empty state and strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
