import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  AiPromptInput,
  messageText,
  mockTransport,
  textDeltas,
  useAiConversation,
  usePromptDraft,
  type AiPromptTriggerApi,
  type AiPromptTriggerState,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const transport = mockTransport({
  delayMs: 50,
  reply: ({ messages }) => [
    { type: 'agent', agent: { name: 'Assistant' } },
    ...textDeltas(
      `You asked: “${messageText(messages[messages.length - 1] ?? { parts: [] })}”. This reply streams word by word from a mock transport, so you can try Stop or press Escape.`,
    ),
    { type: 'done' },
  ],
});

function ChatDemo() {
  const chat = useAiConversation({ transport });
  const [draft, setDraft, clear] = usePromptDraft('docs-ai-prompt-input');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', width: '100%', maxWidth: '40rem' }}>
      <ol style={{ margin: 0, paddingInlineStart: 'var(--mn-space-5)' }}>
        {chat.messages.map((m) => (
          <li key={m.id}>
            <strong>{m.role === 'user' ? 'You' : (m.agent?.name ?? 'Assistant')}:</strong>{' '}
            {messageText(m)}
          </li>
        ))}
      </ol>
      <AiPromptInput
        label="Message the assistant"
        value={draft}
        onValueChange={setDraft}
        status={chat.status}
        onStop={chat.stop}
        onSubmit={(text, { files }) => {
          chat.send(text, {
            files: files.map((f) => ({
              type: 'file',
              name: f.name,
              mediaType: f.type,
              size: f.size,
            })),
          });
          clear();
        }}
        accept="image/*,.pdf,.txt"
        maxLength={4000}
        countTokens={(t) => Math.ceil(t.length / 4)}
        footer="Answers can be wrong. Check important facts."
      />
    </div>
  );
}

const COMMANDS = [
  { id: 'summarize', label: '/summarize', hint: 'Summarize the open document' },
  { id: 'translate', label: '/translate', hint: 'Translate the selection' },
  { id: 'outline', label: '/outline', hint: 'Draft an outline' },
];

function SlashMenu({ state, active }: { state: AiPromptTriggerState; active: number }) {
  const matches = COMMANDS.filter((c) => c.id.startsWith(state.query.toLowerCase()));
  if (!matches.length) return null;
  return (
    <div
      id="docs-slash-menu"
      role="listbox"
      aria-label="Commands"
      style={{
        padding: 'var(--mn-space-1)',
        border: '1px solid var(--mn-color-border-default)',
        borderRadius: 'var(--mn-radius-md)',
        background: 'var(--mn-color-bg-raised)',
        boxShadow: 'var(--mn-shadow-md)',
      }}
    >
      {matches.map((c, i) => (
        <div
          key={c.id}
          id={`docs-slash-${c.id}`}
          role="option"
          tabIndex={-1}
          aria-selected={i === active % matches.length}
          onMouseDown={(e) => {
            e.preventDefault();
            state.insert(`${c.label} `);
          }}
          style={{
            padding: 'var(--mn-space-1-5) var(--mn-space-2)',
            borderRadius: 'var(--mn-radius-sm)',
            background: i === active % matches.length ? 'var(--mn-color-bg-muted)' : undefined,
            cursor: 'pointer',
          }}
        >
          <strong>{c.label}</strong>{' '}
          <span style={{ color: 'var(--mn-color-text-muted)' }}>{c.hint}</span>
        </div>
      ))}
    </div>
  );
}

function SlashCommandsDemo() {
  const [open, setOpen] = useState<AiPromptTriggerApi | null>(null);
  const [query, setQuery] = useState('');
  const [active, setActive] = useState(0);
  const matches = COMMANDS.filter((c) => c.id.startsWith(query.toLowerCase()));
  const current = matches.length ? matches[active % matches.length] : undefined;
  const [sent, setSent] = useState('');
  return (
    <div
      style={{
        display: 'grid',
        gap: 'var(--mn-space-2)',
        width: '100%',
        maxWidth: '40rem',
        paddingBlockStart: '8rem',
      }}
    >
      <AiPromptInput
        label="Message the assistant"
        placeholder="Type / for commands"
        onSubmit={(text) => setSent(text)}
        triggers={[
          {
            char: '/',
            onTrigger: (q, api) => {
              setQuery(q);
              setActive(0);
              setOpen(api);
            },
            onDismiss: () => setOpen(null),
          },
        ]}
        renderTriggerMenu={(state) => <SlashMenu state={state} active={active} />}
        textareaProps={{
          role: 'combobox',
          'aria-autocomplete': 'list',
          'aria-expanded': Boolean(open && matches.length),
          'aria-controls': open && matches.length ? 'docs-slash-menu' : undefined,
          'aria-activedescendant': open && current ? `docs-slash-${current.id}` : undefined,
          onKeyDown: (e) => {
            if (!open || !matches.length) return;
            if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
              e.preventDefault();
              setActive(
                (a) => (a + (e.key === 'ArrowDown' ? 1 : matches.length - 1)) % matches.length,
              );
            } else if ((e.key === 'Enter' || e.key === 'Tab') && current) {
              e.preventDefault();
              open.insert(`${current.label} `);
            }
          },
        }}
      />
      {sent ? <p style={{ margin: 0 }}>Sent: {sent}</p> : null}
    </div>
  );
}

function AttachmentsDemo() {
  const [sent, setSent] = useState<string[]>([]);
  const [web, setWeb] = useState(false);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%', maxWidth: '40rem' }}>
      <AiPromptInput
        label="Ask about a file"
        placeholder="Paste, drop or attach a file"
        accept="image/*,.pdf,.csv,.txt"
        onAttach={(files) => files.filter((f) => f.size <= 10 * 1024 * 1024)}
        onSubmit={(text, { files }) =>
          setSent([text, ...files.map((f) => f.name), web ? '(web search on)' : ''].filter(Boolean))
        }
        toolbar={
          <Button size="sm" variant="ghost" aria-pressed={web} onClick={() => setWeb((w) => !w)}>
            Search the web
          </Button>
        }
      />
      {sent.length ? <p style={{ margin: 0 }}>Sent: {sent.join(', ')}</p> : null}
    </div>
  );
}

export default {
  id: 'ai-prompt-input',
  title: 'AiPromptInput',
  summary:
    'The AI prompt box: auto-growing, Enter to send, Send becomes Stop, attachments, triggers and a live counter.',
  sections: {
    purpose: {
      summary:
        'The one place people write to an AI assistant. It handles everything a prompt box gets wrong when hand-built: sending on Enter without breaking IME input, turning Send into Stop while a response streams, explaining why it is disabled, taking pasted and dropped files, opening slash-command and mention menus, warning before a length limit, and keeping the draft across reloads.',
      useWhen: [
        'Any AI conversation surface: an assistant panel, a full-page chat, an inline “Ask AI” box.',
        'Driven by `useAiConversation` (pass `status`, `onStop` and call `send` in `onSubmit`) or your own state (pass `busy`).',
      ],
      avoidWhen: [
        'Messages between people (chat, ticket replies, comments): use MessageComposer, which has rich text and mentions.',
        'A single-line search or filter: use Input or a search field.',
      ],
    },
    designPrinciples: {
      items: [
        'The textarea is home: after Send or Stop, focus returns to it so the next prompt can be typed at once.',
        'Stop is never hidden: the Send button becomes Stop while busy, and Escape stops from the textarea.',
        'Never lose words: the draft can persist, a failed submit restores the text, and going over the limit blocks sending instead of cutting text.',
        'Plain text in, files alongside: the input never parses or renders model output.',
        'Extensible, not opinionated: triggers report what was typed; the product renders the menu.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`usePromptDraft(key)` returns `[draft, setDraft, clear]`, backed by sessionStorage (read after mount, errors ignored).',
        '`findTrigger(text, caret, triggers)` and `fileMatchesAccept(file, accept)` are the pure helpers the input uses, exported for menus and tests.',
        'Props for the `<textarea>` (id, name, autoFocus, role and ARIA for a menu, extra handlers) go in `textareaProps`; the element is `textareaRef`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'submitOn',
          values: ['enter', 'mod-enter'],
          description:
            'Enter sends (Shift+Enter adds a line), or ⌘/Ctrl+Enter sends and Enter adds a line.',
        },
        {
          prop: 'showLabel',
          values: ['false', 'true'],
          description: 'The required `label` is visually hidden by default.',
        },
      ],
      notes: [
        'Attachments, counter, token estimate, triggers, toolbar and footer appear only when their props are set.',
      ],
    },
    states: {
      items: [
        {
          name: 'busy',
          description:
            '`status` is `submitted` or `streaming`, or `busy`: Send is Stop and Escape stops.',
          attribute: 'data-busy',
        },
        {
          name: 'disabled',
          description: 'Typing and sending are off; `disabledReason` is shown and linked.',
          attribute: 'data-disabled',
        },
        {
          name: 'dragging',
          description: 'Files are dragged over the input; a drop hint covers it.',
          attribute: 'data-dragging',
        },
        {
          name: 'limit',
          description:
            '`ok`, `warning` (from `warnAt` of `maxLength`) or `over` (sending blocked, `aria-invalid`).',
          attribute: 'data-limit',
        },
        {
          name: 'trigger',
          description: 'A trigger is active; the value is its character.',
          attribute: 'data-trigger',
        },
      ],
    },
    interaction: {
      items: [
        'The textarea grows from `minRows` to `maxRows`, then scrolls.',
        'Submitting calls `onSubmit(text, { files })` and clears the text and files at once; a rejected promise brings them back if nothing new was typed.',
        'Empty or whitespace-only prompts with no files are not sent; neither is anything while busy or over the limit.',
        'Typing continues while a response streams; Enter just does not send until it ends.',
        'Paste files (screenshots), drop them on the input, or use the attach button. `accept` filters all three; `onAttach` can filter further.',
        'Typing a trigger at the start of a word calls `onTrigger(query, { close, insert })` as the query changes and renders `renderTriggerMenu`; a space, moving the caret away, Escape or `close()` ends it.',
        'The counter turns to warning near `maxLength` and to danger over it.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Enter',
          action:
            'Sends (with `submitOn="enter"`); adds a line with `mod-enter`. Ignored while an IME is composing.',
        },
        { keys: 'Shift+Enter', action: 'Adds a line.' },
        { keys: '⌘/Ctrl+Enter', action: 'Sends in both modes.' },
        {
          keys: 'Escape',
          action: 'Closes an open trigger menu; otherwise stops the response while busy.',
        },
        {
          keys: 'Tab',
          action: 'Moves through the textarea, attachment chips, toolbar and Send/Stop.',
        },
      ],
      notes: [
        'Your `textareaProps.onKeyDown` runs first; `preventDefault()` there (for example on Enter or arrows in a menu) stops the input acting on the key.',
        'IME safety: keys are ignored while `isComposing` is set, between compositionstart and compositionend, and for keyCode 229 (Safari reports the committing Enter that way).',
      ],
    },
    accessibility: {
      role: 'textbox (or combobox when you pass `role` for a trigger menu)',
      name: '`label`, a real `<label>` that is visually hidden unless `showLabel`.',
      items: [
        'A visually hidden hint (“Enter to send, Shift+Enter for a new line.”, plus “Escape stops the response.” while busy) is the textarea’s description, with the counter and the disabled reason.',
        'The counter is read as “120 of 4000 characters”; reaching the warning threshold or going over is announced once, politely, never per keystroke.',
        'Attaching and removing files is announced politely; removing a chip moves focus to the next chip or back to the textarea.',
        'Send and Stop are labelled icon buttons; Stop has `aria-keyshortcuts="Escape"`.',
        'For a trigger menu, pass `role="combobox"`, `aria-expanded`, `aria-controls` and `aria-activedescendant` through `textareaProps`; focus stays in the textarea.',
        'The drop hint is decorative; the attach button is the accessible route to the same action.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; the bar wraps the toolbar above the counter and Send at narrow widths, with no horizontal scroll at 390px.',
        'On coarse pointers every bar control and chip remove button gets a 44px target, and the text is at least 16px so mobile browsers do not zoom on focus.',
        'File names truncate with an ellipsis; the full name is in the tooltip and the remove button’s label.',
      ],
    },
    theme: {
      items: [
        'Surface, border and text use neutral tokens; the busy border and the drop state use the AI intent (`--mn-color-ai-border`, `--mn-color-ai-subtle`, `--mn-color-ai-strong`).',
        'Warning and over-limit states use `--mn-color-warning-strong` and `--mn-color-danger-strong`.',
        'Forced colours: system colours for the border, focus and drop hint. Reduced motion removes the border transition.',
      ],
    },
    examples: {
      items: [
        {
          title: 'A chat on the mock transport (Stop, Escape, counter, saved draft)',
          stack: true,
          render: () => <ChatDemo />,
        },
        {
          title: 'Slash commands through a trigger',
          stack: true,
          render: () => <SlashCommandsDemo />,
        },
        { title: 'Attachments and a toolbar', stack: true, render: () => <AttachmentsDemo /> },
        {
          title: 'Disabled, with a reason',
          stack: true,
          render: () => (
            <AiPromptInput
              label="Message the assistant"
              onSubmit={() => {}}
              disabled
              disabledReason="Connect a provider to start."
            />
          ),
        },
        {
          title: '⌘/Ctrl+Enter to send, taller',
          stack: true,
          render: () => (
            <AiPromptInput
              label="Describe the change"
              placeholder="Describe the change in detail"
              submitOn="mod-enter"
              minRows={3}
              maxRows={12}
              onSubmit={() => {}}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `status` and `onStop` from `useAiConversation` so Stop and Escape just work.',
        'Use `usePromptDraft` with a key per conversation so a reload keeps the unsent prompt.',
        'Say why the input is disabled with `disabledReason`, and what to do about it.',
        'Set `maxLength` to the backend’s real limit so people learn it before sending.',
      ],
      dont: [
        'Disable the textarea while a response streams; people want to write the next prompt.',
        'Cut text at the limit; the counter warns and sending is blocked instead.',
        'Use it for person-to-person messages; MessageComposer fits those.',
        'Handle Enter yourself on the textarea without `preventDefault()`: the input would send as well.',
      ],
    },
    related: {
      items: [
        {
          id: 'ai-stop-button',
          reason: 'The Stop control the Send button turns into; use it alone elsewhere.',
        },
        {
          id: 'ai-conversation',
          reason: 'Provides `status`, `send` and `stop` to drive the input.',
        },
        { id: 'ai-thread', reason: 'The conversation list that sits above the input.' },
        { id: 'message-composer', reason: 'Rich-text composer for messages between people.' },
        { id: 'mention-picker', reason: 'A ready mention menu to render from an `@` trigger.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['ai-stop-button', 'button', 'file-upload'],
      notes: ['`ai-conversation` is a type-only import (`AiConversationStatus`).'],
    },
    testing: {
      unit: [
        'Enter sends, Shift+Enter adds a line, and the box clears; whitespace-only prompts are not sent.',
        '`submitOn="mod-enter"`: Enter adds a line, Ctrl/⌘+Enter sends.',
        'IME: no submit while composing, including Safari’s keyCode 229.',
        'Send becomes Stop for `submitted`/`streaming`/`busy`; Stop and Escape call `onStop`; Enter does not send while busy; focus stays in the textarea.',
        'The disabled reason is visible and is the textarea’s description.',
        'Controlled value; a rejected submit restores the text.',
        'Pasted and dropped files (DataTransfer and ClipboardEvent) become chips and reach `onSubmit`; `accept` and `onAttach` filter; removing keeps focus; the picker opens from the attach button.',
        'Trigger detection at word start, query reporting, `insert`, Escape closing the menu before stopping, and a caller `onKeyDown` taking Enter.',
        'Counter warning and over-limit states, announced only when crossing; the token estimate.',
        '`usePromptDraft` reads, writes and clears sessionStorage, survives storage errors and is SSR-safe.',
        'axe passes idle, busy, disabled and with attachments; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
