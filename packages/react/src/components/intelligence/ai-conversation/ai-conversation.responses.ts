/**
 * The "responses" event format (`response.output_text.delta`,
 * `response.function_call_arguments.delta`, `response.completed`, …): a
 * mapper for `createSseTransport` and a request builder for its `input`
 * items. Reads the format only; your service holds the key.
 */
import type { AiStreamEvent, AiTransportRequest } from './ai-conversation.protocol.js';
import type { AiStreamMapper } from './ai-conversation.sse.js';
import type { AiFinishReason, AiMessage, AiPart } from './ai-conversation.types.js';
import { AI_CONVERSATION_MESSAGES } from './ai-conversation.errors.js';
import {
  isObject,
  num,
  nowIso,
  parseArguments,
  parseJson,
  str,
  vendorRetryable,
  usageDetails,
} from './ai-conversation.json.js';
import type { AiStreamMapperOptions } from './ai-conversation.providers.js';
import { isResolvedToolCall, toolResultText } from './ai-conversation.requests.js';

/**
 * Reads the responses event format: output text, refusals, reasoning
 * summaries, function calls (several in parallel, arguments streamed as
 * `tool-input-delta`), URL citations, usage, and the end of the response
 * (`completed`, `incomplete` with its reason, `failed`). The response id
 * becomes a `run` event. Function calls wait in `awaiting-result` when the
 * response ends.
 */
export function responsesMapper(options: AiStreamMapperOptions = {}): AiStreamMapper {
  const unavailable = options.unavailable ?? AI_CONVERSATION_MESSAGES.unavailable;
  /** Output item id → call id, for argument events that name the item. */
  const calls = new Map<string, { callId: string; args: string; open: boolean }>();
  const sources = new Set<string>();
  let citations = 0;
  let reasoning = false;
  let refused = false;
  let finished = false;

  const end = (reason: AiFinishReason, response: Record<string, unknown>): AiStreamEvent[] => {
    if (finished) return [];
    finished = true;
    const out: AiStreamEvent[] = [];
    if (reasoning) out.push({ type: 'reasoning-end' });
    const waiting = [...calls.values()];
    const final = reason === 'stop' && waiting.length ? 'tool-use' : reason;
    if (final === 'tool-use')
      for (const c of waiting)
        out.push({ type: 'tool-update', id: c.callId, patch: { state: 'awaiting-result' } });
    const usage = isObject(response.usage) ? response.usage : undefined;
    if (usage)
      out.push({
        type: 'usage',
        usage: {
          inputTokens: num(usage.input_tokens),
          outputTokens: num(usage.output_tokens),
          ...usageDetails(
            isObject(usage.input_tokens_details)
              ? usage.input_tokens_details.cached_tokens
              : undefined,
            undefined,
            isObject(usage.output_tokens_details)
              ? usage.output_tokens_details.reasoning_tokens
              : undefined,
          ),
        },
        ...(str(response.model) ? { model: response.model as string } : {}),
      });
    out.push({ type: 'finish', reason: refused && final === 'stop' ? 'refusal' : final });
    out.push({ type: 'done' });
    return out;
  };

  const errorEvent = (raw: Record<string, unknown>): AiStreamEvent => {
    const code = str(raw.code) ?? str(raw.type) ?? 'service';
    return {
      type: 'error',
      code,
      message: str(raw.message) ?? unavailable,
      retryable: vendorRetryable(code),
    };
  };

  return {
    map(frame) {
      const event = parseJson(frame.data);
      if (!isObject(event)) return [];
      const type = str(event.type) ?? frame.event;
      const response = isObject(event.response) ? event.response : {};
      switch (type) {
        case 'response.created':
          return str(response.id) ? [{ type: 'run', runId: response.id as string }] : [];
        case 'response.output_text.delta':
          return str(event.delta) ? [{ type: 'text-delta', text: event.delta as string }] : [];
        case 'response.refusal.delta':
          refused = true;
          return str(event.delta) ? [{ type: 'text-delta', text: event.delta as string }] : [];
        case 'response.reasoning_summary_text.delta':
        case 'response.reasoning_text.delta':
          if (!str(event.delta)) return [];
          reasoning = true;
          return [{ type: 'reasoning-delta', text: event.delta as string }];
        case 'response.output_item.added': {
          const item = isObject(event.item) ? event.item : {};
          if (item.type !== 'function_call') return [];
          const callId = str(item.call_id) ?? str(item.id) ?? `call-${calls.size}`;
          calls.set(str(item.id) ?? callId, { callId, args: '', open: true });
          return [
            {
              type: 'part',
              part: {
                type: 'tool-call',
                id: callId,
                name: str(item.name) ?? 'tool',
                input: {},
                state: 'running',
                startedAt: nowIso(),
              },
            },
          ];
        }
        case 'response.output_item.done': {
          const item = isObject(event.item) ? event.item : {};
          if (item.type === 'reasoning' && reasoning) {
            reasoning = false;
            return [{ type: 'reasoning-end' }];
          }
          return [];
        }
        case 'response.function_call_arguments.delta': {
          const call = calls.get(str(event.item_id) ?? '');
          if (!call || !str(event.delta)) return [];
          call.args += event.delta as string;
          return [{ type: 'tool-input-delta', id: call.callId, delta: event.delta as string }];
        }
        case 'response.function_call_arguments.done': {
          const call = calls.get(str(event.item_id) ?? '');
          if (!call) return [];
          call.open = false;
          const args = typeof event.arguments === 'string' ? event.arguments : call.args;
          return [{ type: 'tool-update', id: call.callId, patch: { input: parseArguments(args) } }];
        }
        case 'response.output_text.annotation.added': {
          const a = isObject(event.annotation) ? event.annotation : {};
          const url = str(a.url);
          if (a.type !== 'url_citation' || !url) return [];
          const out: AiStreamEvent[] = [];
          if (!sources.has(url)) {
            sources.add(url);
            out.push({
              type: 'part',
              part: { type: 'source', id: url, title: str(a.title) ?? url, kind: 'web', url },
            });
          }
          out.push({
            type: 'part',
            part: { type: 'citation', id: `citation-${++citations}`, sourceId: url },
          });
          return out;
        }
        case 'response.completed':
          return end('stop', response);
        case 'response.incomplete': {
          const details = isObject(response.incomplete_details) ? response.incomplete_details : {};
          const reason =
            details.reason === 'max_output_tokens'
              ? 'length'
              : details.reason === 'content_filter'
                ? 'content-filter'
                : 'other';
          return end(reason, response);
        }
        case 'response.failed':
          finished = true;
          return [errorEvent(isObject(response.error) ? response.error : {})];
        case 'error':
          finished = true;
          return [errorEvent(isObject(event.error) ? event.error : event)];
        default:
          return [];
      }
    },
    flush: () => (finished ? [] : end('other', {})),
  };
}

export type ResponsesInputContent =
  | { type: 'input_text'; text: string }
  | { type: 'input_image'; image_url?: string; file_id?: string }
  | {
      type: 'input_file';
      file_id?: string;
      file_data?: string;
      file_url?: string;
      filename?: string;
    }
  | { type: 'output_text'; text: string };

export type ResponsesInputItem =
  | { role: 'system' | 'user' | 'assistant'; content: ResponsesInputContent[] }
  | { type: 'function_call'; call_id: string; name: string; arguments: string }
  | { type: 'function_call_output'; call_id: string; output: string };

function fileContent(p: Extract<AiPart, { type: 'file' }>): ResponsesInputContent | null {
  if (p.mediaType.startsWith('image/'))
    return p.url || p.fileId
      ? { type: 'input_image', ...(p.url ? { image_url: p.url } : { file_id: p.fileId as string }) }
      : null;
  if (p.fileId) return { type: 'input_file', file_id: p.fileId, filename: p.name };
  if (p.url?.startsWith('data:')) return { type: 'input_file', file_data: p.url, filename: p.name };
  if (p.url) return { type: 'input_file', file_url: p.url, filename: p.name };
  return null;
}

/**
 * The conversation as responses `input` items: messages with text, image and
 * file content, then each resolved function call and its output, in order.
 */
export function toResponsesInput(messages: readonly AiMessage[]): ResponsesInputItem[] {
  const out: ResponsesInputItem[] = [];
  for (const m of messages) {
    if (m.role === 'tool') continue;
    let content: ResponsesInputContent[] = [];
    const flush = () => {
      if (content.length) out.push({ role: m.role as 'system' | 'user' | 'assistant', content });
      content = [];
    };
    for (const p of m.parts) {
      if (p.type === 'text' && p.text)
        content.push({ type: m.role === 'assistant' ? 'output_text' : 'input_text', text: p.text });
      else if (p.type === 'file' && m.role === 'user') {
        const c = fileContent(p);
        if (c) content.push(c);
      } else if (isResolvedToolCall(p) && m.role === 'assistant') {
        flush();
        out.push({
          type: 'function_call',
          call_id: p.id,
          name: p.name,
          arguments: typeof p.input === 'string' ? p.input : JSON.stringify(p.input ?? {}),
        });
        out.push({ type: 'function_call_output', call_id: p.id, output: toolResultText(p) });
      }
    }
    flush();
  }
  return out;
}

/** `buildBody` for a responses service: `{ input, conversationId?, ...body }`. */
export function buildResponsesBody(request: AiTransportRequest): Record<string, unknown> {
  return {
    input: toResponsesInput(request.messages),
    ...(request.conversationId ? { conversationId: request.conversationId } : {}),
    ...request.body,
  };
}
