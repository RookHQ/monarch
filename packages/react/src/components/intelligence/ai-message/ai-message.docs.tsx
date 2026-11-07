import { useState } from 'react';
import { AiMessageView, type AiMessage } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const AT = '2026-03-02T10:15:00.000Z';

const QUESTION: AiMessage = {
  id: 'q1',
  role: 'user',
  createdAt: AT,
  parts: [
    { type: 'text', text: 'Summarise the meeting notes and list the next steps.', state: 'done' },
  ],
};

const ANSWER: AiMessage = {
  id: 'a1',
  role: 'assistant',
  createdAt: AT,
  agent: { name: 'Assistant', role: 'Writing assistant' },
  branch: { index: 1, count: 2 },
  parts: [
    {
      type: 'reasoning',
      text: 'Find the decisions first, then the owners.',
      state: 'done',
      durationMs: 3000,
    },
    {
      type: 'tool-call',
      id: 't1',
      name: 'search_documents',
      input: { query: 'meeting notes' },
      state: 'succeeded',
      startedAt: '2026-03-02T10:15:00.000Z',
      endedAt: '2026-03-02T10:15:01.600Z',
    },
    {
      type: 'text',
      text: 'The team agreed on the launch date.\n\n- Confirm the venue\n- Share the agenda\n- Book a review',
      state: 'done',
    },
    {
      type: 'source',
      id: 's1',
      title: 'Weekly meeting notes',
      url: 'https://example.com/notes',
      kind: 'doc',
    },
    { type: 'citation', id: 'c1', sourceId: 's1' },
    { type: 'file', name: 'agenda.pdf', mediaType: 'application/pdf', size: 48_000 },
  ],
};

function EditDemo() {
  const [message, setMessage] = useState(QUESTION);
  const [editing, setEditing] = useState(false);
  return (
    <AiMessageView
      message={message}
      actionsVisibility="always"
      onEdit={() => setEditing(true)}
      editing={editing}
      onEditCancel={() => setEditing(false)}
      onEditSubmit={(text) => {
        setEditing(false);
        setMessage({ ...message, parts: [{ type: 'text', text, state: 'done' }] });
      }}
    />
  );
}

function AnswerDemo() {
  const [message, setMessage] = useState(ANSWER);
  return (
    <AiMessageView
      message={message}
      actionsVisibility="always"
      onRegenerate={() => {}}
      onFeedback={(feedback) => setMessage({ ...message, feedback })}
      onBranch={(index) => setMessage({ ...message, branch: { index, count: 2 } })}
    />
  );
}

export default {
  id: 'ai-message',
  title: 'AiMessageView',
  summary:
    'One chat message: streaming text, reasoning, tool calls, sources, files and errors in order.',
  sections: {
    purpose: {
      summary:
        'Renders one `AiMessage` with its parts in the order the model produced them, the layout for its role, an action toolbar, a branch switcher and inline editing for the person’s messages. AiThread uses it for every row.',
      useWhen: [
        'You render a conversation yourself, or a single message outside AiThread.',
        'You need the default part rendering and want to override only some parts with `renderPart`.',
      ],
      avoidWhen: [
        'People talk to people: use MessageRow.',
        'You only show answer text: use AiStreamingText.',
      ],
    },
    designPrinciples: {
      items: [
        'Parts render in model order; nothing is regrouped except sources, which form one numbered list at the first source.',
        'The person’s message is an end-aligned bubble; the assistant’s is full width under its avatar, marked with the Bot glyph (never sparkles).',
        'Model output is untrusted: links pass `isSafeUrl` and get `rel="noopener noreferrer nofollow"`; no raw HTML.',
        'Later components (tool-call cards, artifacts, plans) plug in through `renderPart`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the `<article>`.',
        'Callbacks receive the message as their last argument, so one set of props can serve a whole thread.',
        '`onCopy` defaults to writing the message text to the clipboard; `onCopy={false}` hides Copy.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'message.role',
          values: ['user', 'assistant', 'system', 'tool'],
          description:
            'Layout, exposed as `data-role`: bubble, full width with header, or centred muted text.',
        },
        {
          prop: 'actionsVisibility',
          values: ['hover', 'always'],
          description: 'See AiMessageActions.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Streaming',
          attribute: 'data-streaming (aria-busy="true" with announce={false})',
          description: 'A text or reasoning part is streaming; the toolbar is hidden.',
        },
        {
          name: 'Editing',
          attribute: 'data-editing',
          description: 'The person’s message is replaced by a textarea with Save and Cancel.',
        },
        {
          name: 'Reasoning',
          description:
            'A closed `<details>` "Thought for Ns"; "Thinking…" while streaming; "Reasoning hidden" when redacted.',
        },
        {
          name: 'Tool call',
          attribute: 'data-state on .mn-ai-message__tool',
          description: 'Name, state text and icon, and duration once known.',
        },
      ],
    },
    interaction: {
      items: [
        'Branch switcher "‹ 2 / 3 ›" calls `onBranch(index, message)`; ends are disabled.',
        'Error parts render AiErrorMessage from the part’s `code`; the provider text goes in its collapsed technical details. Retryable ones show Try again, calling `onRetry(message)`.',
        'Citations link to their numbered source.',
        '`renderPart` returning `undefined` keeps the default, `null` renders nothing.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter (editing)', action: 'Saves the edit.' },
        { keys: 'Shift+Enter (editing)', action: 'New line.' },
        {
          keys: 'Escape (editing)',
          action: 'Cancels the edit, without stopping a reply in the thread.',
        },
        {
          keys: 'Enter / Space on the reasoning summary',
          action: 'Opens or closes the reasoning.',
        },
      ],
      notes: ['The toolbar keys are listed on AiMessageActions.'],
    },
    accessibility: {
      role: 'article',
      name: 'None; the content starts with a visually hidden "Assistant said:" / "You said:" prefix.',
      items: [
        'The AI-generated identification is the visually hidden prefix (`messages.assistantSaid`), one approach used everywhere.',
        'While streaming, the article gets `aria-busy="true"` when `announce={false}` (inside AiThread, which announces). With `announce` on, AiStreamingText marks only its own content busy, because a busy ancestor would silence its live region.',
        'Reasoning uses native `<details>`/`<summary>`.',
        'The avatar is decorative because the name is shown next to it; icons are hidden from assistive technology.',
        'Streamed text announces itself unless `announce={false}` (AiThread owns announcements).',
      ],
    },
    responsive: {
      items: [
        'Bubbles are at most 85% wide; long words and URLs wrap.',
        'Logical properties throughout, so RTL mirrors the bubble side and branch chevrons.',
        '44px targets on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'AI identity uses `--mn-color-ai-*` (avatar, badge, citations); reasoning uses `--mn-color-ai-thinking`, tool rows `--mn-color-ai-tool`.',
        'Forced colours add borders to bubbles, tool rows, chips and badges.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Assistant answer with reasoning, tool, sources and file',
          stack: true,
          render: () => <AnswerDemo />,
        },
        { title: 'The person’s message with inline edit', stack: true, render: () => <EditDemo /> },
      ],
    },
    doDont: {
      do: [
        'Pass `useAiConversation().agent` as `agent` when messages do not carry one.',
        'Use `renderPart` for artifacts, plans and generative UI.',
      ],
      dont: [
        'Do not render model HTML yourself inside a part.',
        'Do not use sparkle icons for the assistant.',
      ],
    },
    related: {
      items: [
        { id: 'ai-thread', reason: 'The conversation list that renders this per message.' },
        { id: 'ai-message-actions', reason: 'The toolbar it includes.' },
        { id: 'ai-streaming-text', reason: 'Renders its text parts.' },
        { id: 'ai-error-message', reason: 'Renders its error parts.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'copy-button',
        'spinner',
        'avatar',
        'ai-conversation',
        'ai-error-message',
        'ai-message-actions',
        'ai-streaming-text',
      ],
    },
    testing: {
      unit: [
        'Parts render in order; unknown types render nothing.',
        'Tool row name, state and duration.',
        'Sources with safe links only; citations link to them.',
        'Reasoning details label, live label, redacted.',
        'Actions wired by role; hidden while streaming; built-in Copy.',
        'Branch switcher label, callbacks, disabled ends.',
        'Edit mode keyboard: Enter, Shift+Enter, Escape without propagation.',
        'Error part retry; renderPart override.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
