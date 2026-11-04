import { useState } from 'react';
import { MessageComposer, type MessageComposerMessage } from '@rookcom/monarch-react/editor';
import { Button } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const PEOPLE = [
  { id: 'p1', label: 'Alex Morgan', description: 'Design' },
  { id: 'p2', label: 'Sam Rivera', description: 'Engineering' },
  { id: 'p3', label: 'Jordan Lee', description: 'Support' },
];
const searchPeople = (text: string) =>
  PEOPLE.filter((p) => p.label.toLowerCase().includes(text.toLowerCase()));
const wait = (ms: number) => new Promise((r) => setTimeout(r, ms));

function ChatDemo() {
  const [sent, setSent] = useState<string[]>([]);
  const [replyTo, setReplyTo] = useState<{ author: string; excerpt: string } | undefined>({
    author: 'Sam Rivera',
    excerpt: 'Can we move the design review to Thursday?',
  });
  return (
    <div style={{ display: 'grid', gap: 12, width: '100%' }}>
      {sent.length ? (
        <ul style={{ margin: 0, paddingLeft: 18, fontSize: 13 }}>
          {sent.map((t, i) => (
            <li key={i}>{t}</li>
          ))}
        </ul>
      ) : null}
      <MessageComposer
        label="Message #design"
        placeholder="Message #design"
        mentions={{ search: searchPeople }}
        replyTo={replyTo}
        onDismissReply={() => setReplyTo(undefined)}
        emoji={(insert) => (
          <Button
            size="sm"
            variant="ghost"
            aria-label="Insert thumbs up"
            onClick={() => insert('👍')}
          >
            👍
          </Button>
        )}
        onSend={async (m: MessageComposerMessage) => {
          await wait(600);
          setSent((s) => [...s, m.text]);
          setReplyTo(undefined);
        }}
      />
    </div>
  );
}

function TicketDemo() {
  const [mode, setMode] = useState('public');
  const [status, setStatus] = useState<'idle' | 'saving' | 'saved'>('idle');
  return (
    <MessageComposer
      label={mode === 'note' ? 'Internal note' : 'Reply to customer'}
      modes={[
        { value: 'public', label: 'Reply', placeholder: 'Write a reply to the customer' },
        {
          value: 'note',
          label: 'Internal note',
          tone: 'note',
          placeholder: 'Only your team can see notes',
          sendLabel: 'Add note',
        },
      ]}
      mode={mode}
      onModeChange={setMode}
      modeAside={mode === 'public' ? 'To: customer@example.com' : 'Visible to your team'}
      sendOn="mod-enter"
      onDraftChange={() => {
        setStatus('saving');
        void wait(400).then(() => setStatus('saved'));
      }}
      draftStatus={status}
      maxLength={2000}
      onSend={() => wait(500)}
    />
  );
}

export default {
  id: 'message-composer',
  title: 'MessageComposer',
  summary:
    'The chat and ticket reply box: rich text with Enter to send, attachments, emoji and action slots, drafts, edit and reply modes, and a product mode switch.',
  sections: {
    purpose: {
      summary:
        'Writes and sends a message: a chat message, a thread reply, a ticket reply or an internal note. It wraps RichTextEditor with the send behaviour and the surrounding slots every Rook conversation surface needs.',
      useWhen: [
        'People send messages in a conversation, thread or ticket.',
        'The same box edits a message that was already sent.',
        'A reply can be public or an internal note, chosen per message.',
      ],
      avoidWhen: [
        'Long-form documents or descriptions with headings: use RichTextEditor.',
        'A one-line comment with no formatting: a Textarea with a button loads no editor.',
        'Email campaigns with templates and merge fields: that builder is product-specific.',
      ],
    },
    designPrinciples: {
      items: [
        'Sending is the primary action: one obvious button, one key (Enter, or ⌘/Ctrl+Enter where messages are long), and the text is never lost if sending fails.',
        'The product owns the conversation: uploads, emoji, mentions search, drafts storage, typing presence and modes are callbacks and slots.',
        'An internal note must never look like a public reply: the note tone changes the box surface, not just a label.',
        'Compact by default: formatting sits on one row with the actions, so the box is small until someone writes more.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `.mn-message-composer`, containing the mode row (`__modes`), the reply or edit context (`__context`), the attachment row (`__attachments`), a `RichTextEditor` (`__editor`, toolbar below the text with the actions at its end) and a footer (`__footer`) with the key hint or disabled reason and the draft status.',
        '`editorRef` reaches the underlying RichTextEditor handle (focus, insertText, clear, getHTML…).',
        'Exports `MESSAGE_COMPOSER_TOOLBAR`, the default compact toolbar.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onSend(message)` receives `{ html, text, json, mode }`. Return a promise for the pending state: the box clears when it resolves and keeps the text when it rejects. Or control the state with `sending`.',
        '`value` and `defaultValue` are HTML (the composer always uses `format="html"`); restore a saved draft with `defaultValue`.',
        '`onDraftChange(html)` runs after typing pauses for `draftDebounceMs`, on unmount if a save is still waiting, and with `""` after a successful send. Show the result with `draftStatus`.',
        '`modes` is product data: each mode has a value, label, optional `tone: "note"`, placeholder and send label. The chosen mode is passed to `onSend`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'sendOn',
          values: ['enter', 'mod-enter'],
          description:
            'Chat-style (Enter sends, Shift+Enter adds a line) or long-form (Enter adds a line, ⌘/Ctrl+Enter sends).',
        },
        {
          prop: 'modes[].tone',
          values: ['default', 'note'],
          description:
            'A note gives the box the warning surface, so private notes are unmistakable.',
        },
        {
          prop: 'editing',
          values: ['false', 'true'],
          description:
            'Edit an existing message: "Editing message" context, Save and Cancel, Escape cancels.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Empty',
          attribute: 'disabled on the send button',
          description: 'Send is disabled until there is text, or `hasAttachments` is set.',
        },
        {
          name: 'Sending',
          attribute: 'aria-busy on the send button',
          description:
            'The button shows a spinner and "Sending…"; Enter does nothing until it settles.',
        },
        {
          name: 'Replying',
          attribute: '.mn-message-composer__context',
          description: 'The quoted author and excerpt above the text, with a dismiss button.',
        },
        {
          name: 'Editing',
          attribute: 'data-editing',
          description: 'Editing context, Save and Cancel; the text is focused.',
        },
        {
          name: 'Internal note',
          attribute: 'data-tone="note"',
          description: 'Warning border and surface on the text box.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description:
            'The box, toolbar and actions are inert; `disabledReason` replaces the key hint and is linked to the text box.',
        },
        {
          name: 'Draft status',
          attribute: 'data-status on the status line',
          description:
            '"Saving draft…", "Draft saved" or "Draft not saved" (danger colour), in a status region.',
        },
      ],
    },
    interaction: {
      items: [
        'Enter sends (or ⌘/Ctrl+Enter with `sendOn="mod-enter"`); ⌘/Ctrl+Enter always sends. Shift+Enter adds a line break. Inside a code block Enter adds a line, so code can be typed naturally.',
        'While a mention list is open, Enter picks the mention instead of sending.',
        'The send button and the keys do nothing while sending, so a message is never sent twice.',
        'Files pasted or dropped into the text, and files from the attach button, go to `onFiles`; the product renders them in `attachments`.',
        '`emoji(insert)` renders the product’s picker; `insert` puts the emoji at the caret.',
        'Arrow Up in an empty box calls `onEditLast` (edit your last message).',
        '`onTyping` fires at most once per `typingIntervalMs` while the text changes; `onTypingStop` fires when the box is emptied, sent or left.',
        'Changing mode keeps the text; the placeholder and send label follow the mode.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter', action: 'Sends (default), or adds a line with `sendOn="mod-enter"`.' },
        { keys: '⌘/Ctrl+Enter', action: 'Sends.' },
        { keys: 'Shift+Enter', action: 'Adds a line break.' },
        { keys: 'Escape (editing)', action: 'Cancels the edit.' },
        { keys: 'ArrowUp (empty box)', action: 'Calls `onEditLast`.' },
        { keys: 'ArrowLeft / ArrowRight (mode switch)', action: 'Changes the mode.' },
      ],
      notes: [
        'All RichTextEditor keys apply: formatting shortcuts, the toolbar’s arrow keys and mention suggestions.',
      ],
    },
    accessibility: {
      role: 'textbox (multi-line) with a formatting toolbar; radiogroup for modes',
      name: '`label` names the text box. The mode switch is named by `messages.modes` (default "Message type").',
      items: [
        'The key hint ("Enter to send, Shift+Enter for a new line") is linked to the text box with `aria-describedby`, so the send key is announced.',
        'A disabled composer links its reason to the text box, so people know why they cannot write.',
        'Draft status is a polite status region.',
        'The send button keeps focus while sending (`aria-busy`, `aria-disabled`), so keyboard users are not dropped.',
        'Attach, dismiss-reply and the product’s emoji slot are labelled buttons in the Tab order after the formatting toolbar.',
      ],
    },
    responsive: {
      items: [
        'The toolbar and actions share one row and wrap when the composer is narrow; the send button never shrinks.',
        'Below 22rem of composer width (a chat dock, a thread panel) the key hint is hidden; it stays available to screen readers through the text box description.',
        'The text grows from one line to 15rem, then scrolls.',
      ],
    },
    theme: {
      items: [
        'Inherits RichTextEditor’s tokens; the note tone uses `--mn-color-warning-subtle` and `--mn-color-warning-border`, the reply context `--mn-color-bg-subtle` with an `--mn-color-accent-border` edge.',
        'Light, dark, high-contrast and product accents apply through tokens; reduced motion removes transitions.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Chat message with reply and mentions',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <ChatDemo />
            </div>
          ),
        },
        {
          title: 'Ticket reply and internal note',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <TicketDemo />
            </div>
          ),
        },
        {
          title: 'Editing, attachments and disabled',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 16, width: '100%', maxWidth: 560 }}>
              <MessageComposer
                label="Edit message"
                editing
                // Editing focuses the text by default; the docs page must not move focus on load.
                // eslint-disable-next-line jsx-a11y/no-autofocus
                autoFocus={false}
                defaultValue="<p>The review is on <strong>Thursday</strong> at 10:00.</p>"
                onCancelEdit={() => {}}
                onSend={() => {}}
              />
              <MessageComposer
                label="Message"
                placeholder="Add a message"
                onFiles={() => {}}
                hasAttachments
                attachments={
                  <span
                    style={{
                      padding: '4px 8px',
                      border: '1px solid var(--mn-color-border-subtle)',
                      borderRadius: 6,
                      fontSize: 12,
                    }}
                  >
                    quarterly-plan.pdf · 240 KB
                  </span>
                }
                onSend={() => {}}
              />
              <MessageComposer
                label="Message #announcements"
                disabled
                disabledReason="Only admins can post in #announcements."
                onSend={() => {}}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Return the send promise so the composer can show the pending state and keep the text on failure.',
        'Use `sendOn="mod-enter"` where replies are long (tickets, email-like replies), and the default where they are short (chat).',
        'Give internal notes `tone: "note"` and a distinct send label ("Add note").',
        'Persist drafts per conversation with `onDraftChange` and restore them with `defaultValue`.',
      ],
      dont: [
        'Do not clear the box yourself before the send succeeds; return the promise instead.',
        'Do not hide why a composer is disabled; pass `disabledReason`.',
        'Do not put headings or checklists in a chat composer; the default toolbar leaves them out on purpose.',
      ],
    },
    related: {
      items: [
        {
          id: 'rich-text-editor',
          reason: 'The editor underneath, for documents and long descriptions.',
        },
        { id: 'message-row', reason: 'Shows the messages this composer sends.' },
        { id: 'file-upload', reason: 'Upload state for the attachment row.' },
        { id: 'mention-picker', reason: 'Mentions in a plain textarea composer.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'segmented-control', 'rich-text-editor'],
      notes: [
        'Button, IconButton and SegmentedControl for the actions and modes; Tiptap comes through RichTextEditor, so the same optional peers apply.',
        'Adds about 3 KB min+gzip on top of RichTextEditor (127 KB for the entry with both).',
      ],
    },
    testing: {
      unit: [
        'Enter sends, Shift+Enter adds a line, the box clears; `mod-enter` mode; Enter in a code block adds a line.',
        'Send disabled while empty; pending state for a promise with no double send; text kept when sending fails.',
        'Sending only attachments with `hasAttachments`; attach button and file picker reach `onFiles`; emoji slot inserts at the caret.',
        'Draft debounce and the empty draft after sending; typing callbacks throttled and stopped on send.',
        'Editing (Save, Escape and Cancel), reply-to preview and dismiss, `onEditLast` on Arrow Up.',
        'Disabled with a linked reason; modes switch tone, placeholder and send label and reach `onSend`; controlled value.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/message-composer',
        'Cross-browser axe: /components/message-composer',
      ],
    },
  },
} satisfies ComponentDocs;
