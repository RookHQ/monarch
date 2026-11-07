import { useState } from 'react';
import { Button, Input } from '@rookcom/monarch-react';
import {
  messageText,
  mockTransport,
  textDeltas,
  useAiConversation,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const transport = mockTransport({
  delayMs: 40,
  reply: ({ messages }) => [
    { type: 'agent', agent: { name: 'Assistant' } },
    ...textDeltas(
      `You asked: “${messageText(messages[messages.length - 1] ?? { parts: [] })}”. This reply is streamed word by word from a mock transport.`,
    ),
    { type: 'done' },
  ],
});

function MinimalChat() {
  const chat = useAiConversation({ transport });
  const [draft, setDraft] = useState('');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', width: '100%' }}>
      <ol style={{ margin: 0, paddingInlineStart: 'var(--mn-space-5)' }}>
        {chat.messages.map((m) => (
          <li key={m.id}>
            <strong>{m.role === 'user' ? 'You' : (m.agent?.name ?? 'Assistant')}:</strong>{' '}
            {messageText(m)}
            {m.branch ? ` (version ${m.branch.index + 1} of ${m.branch.count})` : null}
          </li>
        ))}
      </ol>
      <p style={{ margin: 0 }}>Status: {chat.status}</p>
      <form
        style={{ display: 'flex', gap: 'var(--mn-space-2)', flexWrap: 'wrap' }}
        onSubmit={(e) => {
          e.preventDefault();
          chat.send(draft);
          setDraft('');
        }}
      >
        <Input
          aria-label="Message"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder="Ask something"
        />
        {chat.busy ? (
          <Button onClick={chat.stop}>Stop</Button>
        ) : (
          <Button type="submit" variant="primary">
            Send
          </Button>
        )}
        <Button
          variant="ghost"
          disabled={chat.busy || chat.messages.length === 0}
          onClick={() => chat.regenerate()}
        >
          Regenerate
        </Button>
      </form>
    </div>
  );
}

export default {
  id: 'ai-conversation',
  title: 'useAiConversation',
  summary:
    'The vendor-neutral message model and the headless hook that streams AI replies through a transport.',
  sections: {
    purpose: {
      summary:
        'Holds an AI conversation: sends prompts through a transport, streams the reply into an ordered list of parts (text, reasoning, tool calls, sources, artifacts…), and handles stop, regenerate, edit-and-resend as branches, tool approval and feedback. Every `./ai` component renders this model, and no model vendor is involved: the transport decides where requests go.',
      useWhen: [
        'Any AI surface: an assistant panel, a full-page chat, an inline “Ask AI” prompt.',
        'You need the Rook Intelligence stream (`rookIntelligenceTransport`) or your own backend (implement `AiTransport`).',
      ],
      avoidWhen: [
        'A one-shot generation with no conversation, such as a single summary: call your API and render the result with AiMarkdown.',
      ],
    },
    designPrinciples: {
      items: [
        'Parts, not strings: the order the model produced things in is the order they are shown.',
        'Vendor-neutral: the hook knows only `AiStreamEvent`; adapters translate a backend’s wire format.',
        'Nothing is lost: edits and regenerations become branches, never overwrites.',
        'People approve consequential tool calls before they run.',
        'Streaming is cheap: token updates render at most once per animation frame.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`useAiConversation({ transport, initialMessages?, conversationId?, body?, onFinish?, onError?, onToolResponse?, generateId? })`.',
        'Returns `{ messages, status, error, conversationId, agent, busy, send, stop, regenerate, editAndResend, approveTool, denyTool, branchTo, setFeedback, reset }`.',
        'Transports: `rookIntelligenceTransport({ url, headers?, credentials?, historyLimit?, fetch? })` for `POST /v1/agents/{id}/chat`; `mockTransport({ reply, toolReply?, delayMs? })` for docs and tests; `readServerSentEvents(body)` for writing your own SSE adapter.',
        'Helpers: `messageText`, `isStreaming`, `applyStreamEvent`, `textDeltas`, `mapRookIntelligenceEvent`.',
      ],
    },
    variants: {
      items: [],
      notes: ['Not visual: it has no variants. The transport is the main choice.'],
    },
    states: {
      items: [
        { name: 'idle', description: 'Nothing is running.' },
        {
          name: 'submitted',
          description: 'Sent; waiting for the first event. Show a thinking indicator.',
        },
        { name: 'streaming', description: 'Events are arriving. Show Stop.' },
        {
          name: 'awaiting-approval',
          description:
            'A tool call has `state: "pending-approval"` and waits for approveTool or denyTool.',
        },
        {
          name: 'error',
          description: 'The turn failed; `error` and an error part say why and whether to retry.',
        },
        { name: 'stopped', description: 'The person pressed Stop; what arrived so far stays.' },
      ],
      notes: ['The hook renders no markup; components expose these as attributes.'],
    },
    interaction: {
      items: [
        '`send(text, { files?, body? })` adds the user message and an empty assistant reply, then streams into it.',
        '`regenerate(id?)` asks again for an assistant reply as a new branch; `branchTo(id, index)` shows another branch.',
        '`editAndResend(id, text)` adds an edited user message as a branch of the original and asks again; history sent to the transport is the visible branch only.',
        '`approveTool(id, input?)` / `denyTool(id, reason?)` answer a pending tool call; with `transport.respondToTool` the same reply continues streaming.',
        '`stop()` aborts the request; streaming parts are closed and running tool calls become `cancelled`.',
        'Unmounting aborts any request in progress.',
      ],
    },
    keyboard: {
      items: [],
      notes: ['Not interactive. Wire Escape to `stop` in the prompt input or thread.'],
    },
    accessibility: {
      items: [
        'Renders nothing. The thread and message components announce at sentence boundaries and on completion; see the AI accessibility guide.',
      ],
    },
    responsive: { items: ['Not visual.'] },
    theme: { items: ['Not visual; no styles.'] },
    examples: {
      items: [
        {
          title: 'A minimal chat on the mock transport',
          stack: true,
          render: () => <MinimalChat />,
        },
      ],
    },
    doDont: {
      do: [
        'Keep endpoints and credentials in the product; pass them to a transport.',
        'Return `pending-approval` tool calls for anything that sends, deletes or spends.',
        'Store conversations server-side and pass them back as `initialMessages`.',
      ],
      dont: [
        'Do not import a model vendor SDK into UI code; write a transport instead.',
        'Do not concatenate strings to render a reply: render the parts.',
      ],
    },
    related: {
      items: [
        {
          id: 'ai-thread',
          reason: 'Renders the messages with scroll anchoring and announcements.',
        },
        { id: 'ai-prompt-input', reason: 'Sends prompts; becomes Stop while busy.' },
        { id: 'ai-message', reason: 'Renders one message’s parts.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Message helpers: text, streaming detection, applying every event type, errors.',
        'SSE reader handles frames split across chunks and stops at [DONE].',
        'Rook Intelligence mapping for agent, chunk, tool, done and error; request body, headers, history limit; 429 → rate-limit.',
        'Send/stream/finish with onFinish; empty prompts ignored; stop keeps partial text.',
        'Regenerate and branchTo; editAndResend forks and sends only the visible history.',
        'Tool approval continues the reply; denial marks the call denied.',
        'Transport errors set status and an error part; agent and conversation id carry over.',
        'Feedback, reset, initial messages; server render.',
      ],
      axe: false,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
