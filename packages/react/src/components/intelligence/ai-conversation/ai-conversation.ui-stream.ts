/**
 * The "UI message stream" format (server-sent events whose data is a JSON
 * chunk such as `{ "type": "text-delta", "id": "t1", "delta": "Hi" }`,
 * ending with `[DONE]`), spoken by popular TypeScript AI toolkits. A mapper
 * for `createSseTransport` and a request builder. No toolkit dependency.
 */
import type { AiStreamEvent, AiTransportRequest } from './ai-conversation.protocol.js';
import type { AiStreamMapper } from './ai-conversation.sse.js';
import type { AiFinishReason, AiMessage } from './ai-conversation.types.js';
import { AI_CONVERSATION_MESSAGES } from './ai-conversation.errors.js';
import { isObject, nowIso, parseJson, str } from './ai-conversation.json.js';
import { parseAiMemoryEvent } from './ai-conversation.memory.js';
import type { AiStreamMapperOptions } from './ai-conversation.providers.js';
import { isResolvedToolCall } from './ai-conversation.requests.js';

function uiFinishReason(reason: string | undefined): AiFinishReason | undefined {
  switch (reason) {
    case undefined:
      return undefined;
    case 'stop':
      return 'stop';
    case 'length':
      return 'length';
    case 'content-filter':
      return 'content-filter';
    case 'tool-calls':
      return 'tool-use';
    default:
      return 'other';
  }
}

/**
 * Reads the UI message stream: text and reasoning deltas, tool input
 * streaming (`tool-input-start`, `tool-input-delta`, `tool-input-available`),
 * tool outputs and errors, approval requests, URL and document sources,
 * files, `data-memory` parts (a `memory` event), `finish` and `error`. A tool call that has input but no output when
 * the stream finishes waits in `awaiting-result` for the client.
 */
export function uiMessageStreamMapper(options: AiStreamMapperOptions = {}): AiStreamMapper {
  const unavailable = options.unavailable ?? AI_CONVERSATION_MESSAGES.unavailable;
  /** Tool calls without an output yet, and whether the service runs them. */
  const pending = new Map<string, { providerExecuted: boolean }>();
  let finished = false;

  const toolPart = (id: string, name: string, input: unknown): AiStreamEvent => ({
    type: 'part',
    part: { type: 'tool-call', id, name, input, state: 'running', startedAt: nowIso() },
  });

  const finish = (reason: AiFinishReason | undefined): AiStreamEvent[] => {
    if (finished) return [];
    finished = true;
    const out: AiStreamEvent[] = [];
    const clientCalls = [...pending].filter(([, t]) => !t.providerExecuted).map(([id]) => id);
    for (const id of clientCalls)
      out.push({ type: 'tool-update', id, patch: { state: 'awaiting-result' } });
    const final = reason ?? (clientCalls.length ? 'tool-use' : 'stop');
    out.push({ type: 'finish', reason: final }, { type: 'done' });
    return out;
  };

  return {
    map(frame) {
      if (frame.data === '[DONE]') return finish(undefined);
      const chunk = parseJson(frame.data);
      if (!isObject(chunk) || typeof chunk.type !== 'string') return [];
      const toolCallId = str(chunk.toolCallId);
      switch (chunk.type) {
        case 'text-delta':
          return str(chunk.delta) ? [{ type: 'text-delta', text: chunk.delta as string }] : [];
        case 'reasoning-delta':
          return str(chunk.delta) ? [{ type: 'reasoning-delta', text: chunk.delta as string }] : [];
        case 'reasoning-end':
          return [{ type: 'reasoning-end' }];
        case 'tool-input-start':
          if (!toolCallId) return [];
          pending.set(toolCallId, { providerExecuted: chunk.providerExecuted === true });
          return [toolPart(toolCallId, str(chunk.toolName) ?? 'tool', {})];
        case 'tool-input-delta':
          return toolCallId && str(chunk.inputTextDelta)
            ? [{ type: 'tool-input-delta', id: toolCallId, delta: chunk.inputTextDelta as string }]
            : [];
        case 'tool-input-available': {
          if (!toolCallId) return [];
          const known = pending.has(toolCallId);
          pending.set(toolCallId, { providerExecuted: chunk.providerExecuted === true });
          return known
            ? [{ type: 'tool-update', id: toolCallId, patch: { input: chunk.input } }]
            : [toolPart(toolCallId, str(chunk.toolName) ?? 'tool', chunk.input)];
        }
        case 'tool-approval-request':
          return toolCallId
            ? [{ type: 'tool-update', id: toolCallId, patch: { state: 'pending-approval' } }]
            : [];
        case 'tool-output-available':
          if (!toolCallId) return [];
          pending.delete(toolCallId);
          return [
            {
              type: 'tool-update',
              id: toolCallId,
              patch: { state: 'succeeded', output: chunk.output, endedAt: nowIso() },
            },
          ];
        case 'tool-input-error':
        case 'tool-output-error':
          if (!toolCallId) return [];
          pending.delete(toolCallId);
          return [
            {
              type: 'tool-update',
              id: toolCallId,
              patch: {
                state: 'failed',
                ...(str(chunk.errorText) ? { error: chunk.errorText as string } : {}),
                endedAt: nowIso(),
              },
            },
          ];
        case 'source-url': {
          const url = str(chunk.url);
          const id = str(chunk.sourceId) ?? url;
          if (!id) return [];
          return [
            {
              type: 'part',
              part: {
                type: 'source',
                id,
                title: str(chunk.title) ?? url ?? id,
                kind: 'web',
                ...(url ? { url } : {}),
              },
            },
          ];
        }
        case 'source-document': {
          const id = str(chunk.sourceId);
          if (!id) return [];
          return [
            {
              type: 'part',
              part: {
                type: 'source',
                id,
                title: str(chunk.title) ?? str(chunk.filename) ?? id,
                kind: 'doc',
              },
            },
          ];
        }
        case 'file': {
          const url = str(chunk.url);
          if (!url) return [];
          return [
            {
              type: 'part',
              part: {
                type: 'file',
                name: str(chunk.filename) ?? url.split('/').pop() ?? 'file',
                mediaType: str(chunk.mediaType) ?? 'application/octet-stream',
                url,
              },
            },
          ];
        }
        case 'data-memory': {
          // A custom data part: `{ type: 'data-memory', id?, data: { action, id, text, scope? } }`.
          const memory = parseAiMemoryEvent(chunk.data, str(chunk.id));
          return memory ? [{ type: 'memory', memory }] : [];
        }
        case 'error':
          return [
            {
              type: 'error',
              code: 'service',
              message: str(chunk.errorText) ?? unavailable,
              retryable: false,
            },
          ];
        case 'abort':
          return finish('other');
        case 'finish':
          return finish(uiFinishReason(str(chunk.finishReason)));
        default:
          return [];
      }
    },
    flush: () => finish(undefined),
  };
}

/** One part of a UI message, as the format's request side expects. */
export type UiMessagePart =
  | { type: 'text'; text: string }
  | { type: 'reasoning'; text: string }
  | { type: 'file'; mediaType: string; url: string; filename?: string }
  | { type: 'source-url'; sourceId: string; url: string; title?: string }
  | {
      type: `tool-${string}`;
      toolCallId: string;
      state: 'output-available' | 'output-error';
      input: unknown;
      output?: unknown;
      errorText?: string;
    };

export interface UiMessage {
  id: string;
  role: 'system' | 'user' | 'assistant';
  parts: UiMessagePart[];
}

/** The conversation as UI messages, with files, sources and resolved tool calls. */
export function toUiMessages(messages: readonly AiMessage[]): UiMessage[] {
  const out: UiMessage[] = [];
  for (const m of messages) {
    if (m.role === 'tool') continue;
    const parts: UiMessagePart[] = [];
    for (const p of m.parts) {
      if (p.type === 'text' && p.text) parts.push({ type: 'text', text: p.text });
      else if (p.type === 'reasoning' && p.text && !p.redacted)
        parts.push({ type: 'reasoning', text: p.text });
      else if (p.type === 'file' && p.url)
        parts.push({ type: 'file', mediaType: p.mediaType, url: p.url, filename: p.name });
      else if (p.type === 'source' && p.url)
        parts.push({ type: 'source-url', sourceId: p.id, url: p.url, title: p.title });
      else if (isResolvedToolCall(p))
        parts.push(
          p.state === 'succeeded'
            ? {
                type: `tool-${p.name}`,
                toolCallId: p.id,
                state: 'output-available',
                input: p.input,
                output: p.output,
              }
            : {
                type: `tool-${p.name}`,
                toolCallId: p.id,
                state: 'output-error',
                input: p.input,
                errorText:
                  p.state === 'denied'
                    ? `denied${p.error ? `: ${p.error}` : ''}`
                    : (p.error ?? 'failed'),
              },
        );
    }
    if (parts.length) out.push({ id: m.id, role: m.role, parts });
  }
  return out;
}

/** `buildBody` for a UI message stream service: `{ id?, messages, trigger, ...body }`. */
export function buildUiMessageBody(request: AiTransportRequest): Record<string, unknown> {
  return {
    ...(request.conversationId ? { id: request.conversationId } : {}),
    messages: toUiMessages(request.messages),
    trigger: request.trigger === 'regenerate' ? 'regenerate-message' : 'submit-message',
    ...request.body,
  };
}
