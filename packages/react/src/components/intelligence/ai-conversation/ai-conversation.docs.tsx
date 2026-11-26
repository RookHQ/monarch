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
        'One connection core: `createSseTransport` owns the request, retry before the first event (`retry`), resume after a drop with `Last-Event-ID` when the server numbers its events (`resume`) and an idle timeout; a mapper says what each frame means. `chatCompletionsMapper` and `contentBlockMessagesMapper` read the two common stream formats. Point it at your own service: a model key never belongs in the browser.',
        'Nothing is lost: edits and regenerations become branches, never overwrites.',
        'People approve consequential tool calls before they run.',
        'Streaming is cheap: token updates render at most once per animation frame, the streaming reply is held apart from the stored tree, and unchanged messages keep their object identity, so memoised message views do not re-render.',
        'Every run owns its abort controller and frame scheduler. A run that was stopped, replaced (send, regenerate, retry, approve) or reset never touches the status, the tree or the screen again.',
        'Errors are honest: HTTP statuses map to `auth`, `bad-request`, `rate-limited` and `http-5xx`; only network drops, timeouts, rate limits, overload and server errors are retryable; `retryAfterMs` comes from `Retry-After`. Raw exception text such as “Failed to fetch” is never shown; every string the hook and the transports raise themselves can be localised through `messages`.',
        'Attachments are never dropped silently: `File`s are uploaded with `uploadFile`, inlined only when `inlineFiles` allows it, and otherwise the send fails with an `attachments-unsupported` error and the prompt keeps its text and files.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`useAiConversation({ transport, initialMessages?, conversationId?, body?, onFinish?, onError?, onToolResponse?, onToolCall?, toolPolicy?, uploadFile?, inlineFiles?, onUploadProgress?, messages?, generateId?, onMemoryEvent?, network?, offlineQueue?, slowAfterMs? })`.',
        'Returns `{ messages, status, error, conversationId, agent, busy, canSend, uploads, offline, slow, queued, cancelQueued, send, submit, stop, regenerate, retry, editAndResend, approveTool, denyTool, submitToolResult, resumeRun, branchTo, setFeedback, reset }`.',
        'Stream events: `text-delta`, `reasoning-delta`, `reasoning-end` (with `redacted`), `part`, `tool-update`, `tool-input-delta` (arguments as they stream), `finish` (`stop`, `length`, `content-filter`, `refusal`, `tool-use`, `other`; sets `message.finishReason`), `run` (sets `message.runId`), `usage` (with optional cache and reasoning token counts), `memory`, `agent`, `done`, and `error` (with `retryAfterMs` and `resumable`).',
        'Transports: `rookIntelligenceTransport({ url, headers?, credentials?, historyLimit?, retry?, idleTimeoutMs?, messages?, fetch? })` for `POST /v1/agents/{id}/chat` (attachments go in `attachments`); `createSseTransport({ url, createMapper, buildBody?, retry?, resume?, resumeUrl?, idleTimeoutMs?, buildToolBody?, toolUrl?, messages? })` for any SSE service; `mockTransport` and `replayTransport` for docs and tests.',
        'Stream formats (mappers) and request builders: chat completions (`chatCompletionsMapper`, `buildChatCompletionsBody`), content blocks (`contentBlockMessagesMapper`, `buildContentBlocksBody`), the UI message stream (`uiMessageStreamMapper`, `buildUiMessageBody`), responses events (`responsesMapper`, `buildResponsesBody`) and agent–user interaction events (`agUiMapper`, `buildAgUiRunInput`). Builders send images and files and serialise assistant tool calls followed by their results.',
        'Testing kit, with no test-runner dependency: `createStreamFixture`, `sseFromEvents` (with `chunking: "byte" | { size } | { at } | { seed }`), `everySplit`, `bodyFromChunks`, `replayTransport`, `runTurn`, `simulateConversation`, `collectStream`.',
        'Helpers: `messageText`, `isStreaming`, `applyStreamEvent`, `applyStreamEvents`, `finishParts`, `unresolvedToolCalls`, `textDeltas`, `mapRookIntelligenceEvent`, `classifyHttpStatus`, `isRetryableErrorCode`, `parseRetryAfter`, `parseAiMemoryEvent`, `messageMemories`, `browserNetworkSource`, `manualNetworkSource`.',
        'Offline: `network?` (an `AiNetworkSource`; default `browserNetworkSource()`, which reads `navigator.onLine` and the `online` / `offline` events; `manualNetworkSource()` for tests and stories) and `offlineQueue?` (default true). While offline, `send` keeps the message in `queued` and shows it at the end of `messages` with `queued: true`; nothing is uploaded or sent. Back online, queued messages go out one at a time, each after the reply to the one before, keeping their ids. `cancelQueued(id)` removes one, `reset()` empties the queue, and a message sent while others still wait goes behind them. A queued message that fails holds the rest until the person retries or the connection returns again. The result also has `offline`.',
        'Slow replies: `slowAfterMs?` (default 10000, 0 turns it off). `slow` becomes true when a running reply has sent no event for that long, and false again with the next event or when the reply ends.',
        'Dropped connections: a reply whose stream fails keeps what arrived and ends in `error` with code `network`. When the transport has `resume` and the reply had a `run` id, the error and its error part carry `resumable: true`; `resumeRun()` then picks the run up instead of asking again.',
        'Memory: the `memory` stream event `{ type: "memory", memory: { id, action: "read" | "write" | "forget", text, scope? } }` adds a `memory` part to the reply (the same id and action again updates it) and calls `onMemoryEvent(memory, message)`; a `memory` part sent through a `part` event calls it too. `uiMessageStreamMapper` maps `data-memory` chunks and `agUiMapper` maps `CUSTOM` events named `memory`; `parseAiMemoryEvent(value)` reads the same loose shapes for your own mapper, and `messageMemories(message)` lists a reply’s memory parts.',
        'Usage: `AiUsage` has optional `cacheReadTokens`, `cacheWriteTokens` and `reasoningTokens`. `inputTokens` is every input token, cached or not, and `outputTokens` includes reasoning. The chat completions, content blocks and responses mappers fill them when the service reports them; the content blocks mapper adds cache reads and writes into `inputTokens`, because that format counts them apart.',
        '`regenerate(messageId?, { body })` and `retry(messageId?, { body })` take request fields, merged over the hook’s `body`, like `send`.',
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
          name: 'awaiting-tool-result',
          description:
            'The model called tools the client runs (`awaiting-result`). `onToolCall` runs them, or call `submitToolResult` for each; the reply continues once all have a result. A new send cancels calls nobody answered.',
        },
        {
          name: 'error',
          description:
            'The turn failed; `error` and an error part say why, whether to retry and, with `retryAfterMs`, when.',
        },
        { name: 'stopped', description: 'The person pressed Stop; what arrived so far stays.' },
        {
          name: 'offline',
          description:
            'Not a status: `offline` is true while the device has no network. Messages sent now wait in `queued` and go out, in order, on reconnect.',
        },
        {
          name: 'slow',
          description:
            'Not a status: `slow` is true while a running reply has sent nothing for `slowAfterMs`.',
        },
      ],
      notes: ['The hook renders no markup; components expose these as attributes.'],
    },
    interaction: {
      items: [
        '`send(text, { files?, body? })` adds the user message and an empty assistant reply, then streams into it. `submit` does the same and returns a promise that rejects when nothing was sent (an approval is pending, or an attachment could not be uploaded), which AiPromptInput uses to give the text and files back.',
        'Attachments: browser `File`s are uploaded with `uploadFile(file, { signal, onProgress })` before the message is added; progress is in `uploads` (AiAttachmentTray’s shape). Without `uploadFile`, small files are inlined as `data:` URLs only when `inlineFiles: { maxBytes, mediaTypes? }` allows it.',
        '`retry(id?)` asks again for a failed reply in place, without adding a branch.',
        '`submitToolResult(id, { output } | { error })` answers a client-side tool call; several calls of one reply (parallel tool calls) are sent together once all have a result. `toolPolicy(call)` can approve or deny calls before anyone is asked, e.g. for “Always allow this tool”.',
        'While a tool call waits for approval, `canSend` is false and `send` is refused, so the history sent to the model never has a call without its answer.',
        '`resumeRun(id?)` reconnects to the run behind a reply by its `runId` when the transport has `resume` (`createSseTransport({ resumeUrl })`), e.g. after a reload; the run is replayed into the message.',
        '`regenerate(id?)` asks again for an assistant reply as a new branch; `branchTo(id, index)` shows another branch.',
        '`editAndResend(id, text)` adds an edited user message as a branch of the original and asks again; history sent to the transport is the visible branch only.',
        '`approveTool(id, input?)` / `denyTool(id, reason?)` answer a pending tool call; with `transport.respondToTool` the same reply continues streaming.',
        '`stop()` ends the reply at once, even when a transport ignores the abort; streaming parts are closed and running tool calls become `cancelled`.',
        'A reply that ends to call tools (`finish: tool-use`) keeps its calls in `awaiting-result` instead of cancelling them.',
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
        'Return `pending-approval` tool calls for anything that sends, deletes or spends, with a `risk` so the approval card can say how much is at stake.',
        'Give `uploadFile` when people can attach files, and localise `messages` with the rest of your strings.',
        'Pin your service’s stream format with `sseFromEvents(…, { chunking: "byte" })` and `replayTransport` of a real recording.',
        'Store conversations server-side and pass them back as `initialMessages`.',
        'Show `offline`, `queued` and `slow` (AiThread does, from the layouts): a message that waits silently looks lost, and a silent reply looks broken.',
      ],
      dont: [
        'Do not import a model vendor SDK into UI code; write a transport instead.',
        'Do not concatenate strings to render a reply: render the parts.',
        'Do not turn on `inlineFiles` for large files: every later request carries them again.',
        'Do not show `error.message` from your own exceptions to people; pass a code and let the UI choose the words.',
        'Do not treat `navigator.onLine` as proof of a working connection: it only knows when there is no network at all. Failures on a bad connection still arrive as `network` errors.',
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
        'Rook Intelligence mapping for agent, chunk, tool, done and error; request body, headers, history limit; 429 → rate-limited.',
        'Send/stream/finish with onFinish; empty prompts ignored; stop keeps partial text.',
        'Regenerate and branchTo; editAndResend forks and sends only the visible history.',
        'Tool approval continues the reply; denial marks the call denied.',
        'Transport errors set status and an error part; agent and conversation id carry over.',
        'Feedback, reset, initial messages; server render.',
        'Race tests: send, regenerate, approve and reset during a stream never let the old run change status or the new reply; stop ends at once.',
        'Client tools: `awaiting-result`, parallel `submitToolResult`, `onToolCall`, `toolPolicy`; a new send cancels unanswered calls.',
        'Retry in place; `retryAfterMs`; unknown codes not retryable; a throwing transport ends in an error; localised messages.',
        'Attachments refused without `uploadFile`, uploaded with progress, inlined only when allowed.',
        'SSE reader: a `\\r\\n` split between chunks, every byte offset of a fixture, single-byte and random chunking; resume de-duplicates replayed ids; `resumeUrl`.',
        'HTTP classification (401/403, 400/422, 429 with Retry-After, 5xx, other 4xx).',
        'Finish reasons, refusals, redacted thinking, citations and parallel tool calls from both vendor formats; the UI message stream, responses and agent event mappers.',
        'Request builders for chat completions, content blocks, responses, UI messages, agent run input and the Rook body, with images, files, tool calls and results.',
        'Testing kit: fixtures, replay, runTurn, simulateConversation, chunking.',
        'Offline: messages sent offline are queued, shown with `queued: true` and sent in order on reconnect with their ids and request fields; cancel and reset; queued attachments upload only when sent; a later message does not overtake waiting ones; a failed queued message holds the rest until retry; `offlineQueue: false`; `browserNetworkSource` follows the window events.',
        '`slow` turns on after `slowAfterMs` without an event, clears with the next event and on Stop, and never turns on with 0.',
        'A connection lost mid-reply is `resumable` only with a run id and a transport that can resume; `resumeRun` completes it. `regenerate` and `retry` send the request fields they are given.',
        'Memory: the `memory` event adds and updates memory parts; `parseAiMemoryEvent` accepts loose shapes and rejects the rest; the UI message stream and agent–user interaction mappers map their custom events; `onMemoryEvent` fires for events and parts.',
        'Usage details: cached and reasoning tokens from the chat completions, content blocks and responses formats.',
      ],
      axe: false,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
