/**
 * Mappers for two widely used streaming formats, for `createSseTransport`.
 * They read the format; they do not call a vendor. Point the transport at
 * your own service, which holds the key and forwards the vendor's stream.
 */
import type { AiStreamEvent } from './ai-conversation.protocol.js';
import type { AiStreamMapper } from './ai-conversation.sse.js';
import type { AiFinishReason } from './ai-conversation.types.js';
import { AI_CONVERSATION_MESSAGES } from './ai-conversation.errors.js';
import {
  isObject,
  num,
  parseArguments,
  parseJson,
  str,
  vendorRetryable,
  type Json,
  usageDetails,
} from './ai-conversation.json.js';

/** Options shared by the stream-format mappers. */
export interface AiStreamMapperOptions {
  /** Shown when the service reports an error without text. Default "The assistant is unavailable." */
  unavailable?: string;
}

/** Maps a chat-completions `finish_reason`. */
function chatFinishReason(reason: string): AiFinishReason {
  switch (reason) {
    case 'stop':
      return 'stop';
    case 'length':
      return 'length';
    case 'content_filter':
      return 'content-filter';
    case 'tool_calls':
    case 'function_call':
      return 'tool-use';
    default:
      return 'other';
  }
}

/** Maps a content-block `stop_reason`. */
function contentBlockStopReason(reason: string): AiFinishReason {
  switch (reason) {
    case 'end_turn':
    case 'stop_sequence':
      return 'stop';
    case 'max_tokens':
    case 'model_context_window_exceeded':
      return 'length';
    case 'tool_use':
      return 'tool-use';
    case 'refusal':
      return 'refusal';
    default:
      return 'other';
  }
}

const now = () => new Date().toISOString();

// ------------------------------------------------- chat completions

/**
 * Reads the "chat completions" stream format (`data: { choices: [{ delta }] }`
 * frames ending with `data: [DONE]`), which many model services and gateways
 * speak. Text becomes `text-delta`, `delta.reasoning_content` (where a service
 * sends it) becomes `reasoning-delta`, `delta.refusal` becomes text with
 * finish reason `refusal`, and each tool call (several may stream in
 * parallel) becomes a `tool-call` part whose arguments stream as
 * `tool-input-delta` and whose `input` is filled in when they end. When the
 * turn ends to call tools (`finish_reason: tool_calls`), the calls wait in
 * `awaiting-result` for the client's results. `finish_reason` becomes a
 * `finish` event.
 */
export function chatCompletionsMapper(options: AiStreamMapperOptions = {}): AiStreamMapper {
  const unavailable = options.unavailable ?? AI_CONVERSATION_MESSAGES.unavailable;
  const tools = new Map<number, { id: string; name: string; args: string; open: boolean }>();
  let model: string | undefined;
  let reasoning = false;
  let refused = false;
  let finished = false;

  const closeTools = (waiting: boolean): AiStreamEvent[] => {
    const out: AiStreamEvent[] = [];
    for (const tool of tools.values()) {
      if (!tool.open) continue;
      tool.open = false;
      out.push({
        type: 'tool-update',
        id: tool.id,
        patch: {
          input: parseArguments(tool.args),
          ...(waiting ? { state: 'awaiting-result' as const } : {}),
        },
      });
    }
    return out;
  };
  const finish = (): AiStreamEvent[] => {
    if (finished) return [];
    finished = true;
    return [...closeTools(false), { type: 'done' }];
  };

  return {
    map(frame) {
      if (frame.data === '[DONE]') return finish();
      const chunk = parseJson(frame.data);
      if (!isObject(chunk)) return [];
      if (isObject(chunk.error)) {
        const code = str(chunk.error.code) ?? str(chunk.error.type) ?? 'service';
        return [
          {
            type: 'error',
            code,
            message: str(chunk.error.message) ?? unavailable,
            retryable: vendorRetryable(code),
          },
        ];
      }
      const out: AiStreamEvent[] = [];
      if (typeof chunk.model === 'string') model = chunk.model;
      const choice = Array.isArray(chunk.choices) ? chunk.choices[0] : undefined;
      const delta = isObject(choice) && isObject(choice.delta) ? choice.delta : undefined;
      if (delta) {
        if (typeof delta.reasoning_content === 'string' && delta.reasoning_content) {
          reasoning = true;
          out.push({ type: 'reasoning-delta', text: delta.reasoning_content });
        }
        const content = str(delta.content) ?? str(delta.refusal);
        if (str(delta.refusal)) refused = true;
        if (content) {
          if (reasoning) {
            reasoning = false;
            out.push({ type: 'reasoning-end' });
          }
          out.push({ type: 'text-delta', text: content });
        }
        if (Array.isArray(delta.tool_calls)) {
          for (const call of delta.tool_calls) {
            if (!isObject(call)) continue;
            const index = num(call.index);
            const fn = isObject(call.function) ? call.function : {};
            let tool = tools.get(index);
            if (!tool) {
              tool = {
                id: str(call.id) ?? `call-${index}`,
                name: str(fn.name) ?? 'tool',
                args: '',
                open: true,
              };
              tools.set(index, tool);
              out.push({
                type: 'part',
                part: {
                  type: 'tool-call',
                  id: tool.id,
                  name: tool.name,
                  input: {},
                  state: 'running',
                  startedAt: now(),
                },
              });
            }
            if (typeof fn.arguments === 'string' && fn.arguments) {
              tool.args += fn.arguments;
              out.push({ type: 'tool-input-delta', id: tool.id, delta: fn.arguments });
            }
          }
        }
      }
      if (isObject(choice) && typeof choice.finish_reason === 'string') {
        let reason = chatFinishReason(choice.finish_reason);
        if (refused && reason === 'stop') reason = 'refusal';
        out.push(...closeTools(reason === 'tool-use'));
        out.push({ type: 'finish', reason });
      }
      if (isObject(chunk.usage)) {
        out.push({
          type: 'usage',
          usage: {
            inputTokens: num(chunk.usage.prompt_tokens),
            outputTokens: num(chunk.usage.completion_tokens),
            ...usageDetails(
              isObject(chunk.usage.prompt_tokens_details)
                ? chunk.usage.prompt_tokens_details.cached_tokens
                : undefined,
              undefined,
              isObject(chunk.usage.completion_tokens_details)
                ? chunk.usage.completion_tokens_details.reasoning_tokens
                : undefined,
            ),
          },
          ...(model ? { model } : {}),
        });
      }
      return out;
    },
    flush: finish,
  };
}

// ------------------------------------------------- messages (content blocks)

const RETRYABLE_ERRORS = new Set(['overloaded_error', 'rate_limit_error', 'api_error']);

type Block =
  | { kind: 'thinking' }
  | { kind: 'tool'; id: string; json: string; server: boolean }
  | { kind: 'other' };

/**
 * Reads the "messages" stream format built from content blocks
 * (`message_start`, `content_block_start`, `content_block_delta`,
 * `content_block_stop`, `message_delta`, `message_stop`). Text blocks become
 * `text-delta`, thinking blocks `reasoning-delta` then `reasoning-end`,
 * `redacted_thinking` blocks a redacted reasoning part, `citations_delta` a
 * source and a citation part, and tool-use blocks a `tool-call` part whose
 * arguments stream as `tool-input-delta` and whose `input` is filled in when
 * the block stops. When the turn stops to use tools (`stop_reason:
 * tool_use`), client tools wait in `awaiting-result`; server tools are marked
 * succeeded when their result block arrives. `stop_reason` becomes a
 * `finish` event.
 */
export function contentBlockMessagesMapper(options: AiStreamMapperOptions = {}): AiStreamMapper {
  const unavailable = options.unavailable ?? AI_CONVERSATION_MESSAGES.unavailable;
  const blocks = new Map<number, Block>();
  /** Client tool calls of this message, which wait for results on `tool_use`. */
  const clientTools: string[] = [];
  const sources = new Set<string>();
  let citations = 0;
  let model: string | undefined;
  let inputTokens = 0;
  let cache: ReturnType<typeof usageDetails> = {};
  let finished = false;

  const citation = (raw: Json): AiStreamEvent[] => {
    const url = str(raw.url);
    const docIndex = typeof raw.document_index === 'number' ? raw.document_index : undefined;
    const sourceId = url ?? (docIndex !== undefined ? `document-${docIndex}` : 'document');
    const out: AiStreamEvent[] = [];
    if (!sources.has(sourceId)) {
      sources.add(sourceId);
      out.push({
        type: 'part',
        part: {
          type: 'source',
          id: sourceId,
          title: str(raw.title) ?? str(raw.document_title) ?? url ?? sourceId,
          kind: url ? 'web' : 'doc',
          ...(url ? { url } : {}),
        },
      });
    }
    const quote = str(raw.cited_text);
    out.push({
      type: 'part',
      part: {
        type: 'citation',
        id: `citation-${++citations}`,
        sourceId,
        ...(quote ? { quote } : {}),
      },
    });
    return out;
  };

  return {
    map(frame) {
      const event = parseJson(frame.data);
      if (!isObject(event)) return [];
      const type = typeof event.type === 'string' ? event.type : frame.event;
      switch (type) {
        case 'message_start': {
          const message = isObject(event.message) ? event.message : {};
          if (typeof message.model === 'string') model = message.model;
          if (isObject(message.usage)) {
            // This format counts cached tokens apart from `input_tokens`; `AiUsage.inputTokens` is the total.
            cache = usageDetails(
              message.usage.cache_read_input_tokens,
              message.usage.cache_creation_input_tokens,
              undefined,
            );
            inputTokens =
              num(message.usage.input_tokens) +
              (cache.cacheReadTokens ?? 0) +
              (cache.cacheWriteTokens ?? 0);
          }
          return [];
        }
        case 'content_block_start': {
          const index = num(event.index);
          const block = isObject(event.content_block) ? event.content_block : {};
          if (block.type === 'tool_use' || block.type === 'server_tool_use') {
            const id = str(block.id) ?? `tool-${index}`;
            const server = block.type === 'server_tool_use';
            blocks.set(index, { kind: 'tool', id, json: '', server });
            if (!server) clientTools.push(id);
            return [
              {
                type: 'part',
                part: {
                  type: 'tool-call',
                  id,
                  name: str(block.name) ?? 'tool',
                  input: {},
                  state: 'running',
                  startedAt: now(),
                },
              },
            ];
          }
          if (block.type === 'redacted_thinking') {
            blocks.set(index, { kind: 'other' });
            return [{ type: 'reasoning-end', redacted: true }];
          }
          // A server tool's result block: the call it answers has finished.
          if (typeof block.type === 'string' && block.type.endsWith('_tool_result')) {
            blocks.set(index, { kind: 'other' });
            const toolUseId = str(block.tool_use_id);
            const failed = isObject(block.content) && typeof block.content.error_code === 'string';
            return toolUseId
              ? [
                  {
                    type: 'tool-update',
                    id: toolUseId,
                    patch: {
                      state: failed ? 'failed' : 'succeeded',
                      output: block.content,
                      endedAt: now(),
                    },
                  },
                ]
              : [];
          }
          blocks.set(index, { kind: block.type === 'thinking' ? 'thinking' : 'other' });
          return [];
        }
        case 'content_block_delta': {
          const delta = isObject(event.delta) ? event.delta : {};
          if (delta.type === 'text_delta' && typeof delta.text === 'string' && delta.text)
            return [{ type: 'text-delta', text: delta.text }];
          if (
            delta.type === 'thinking_delta' &&
            typeof delta.thinking === 'string' &&
            delta.thinking
          )
            return [{ type: 'reasoning-delta', text: delta.thinking }];
          if (delta.type === 'citations_delta' && isObject(delta.citation))
            return citation(delta.citation);
          if (delta.type === 'input_json_delta' && typeof delta.partial_json === 'string') {
            const block = blocks.get(num(event.index));
            if (block?.kind === 'tool' && delta.partial_json) {
              block.json += delta.partial_json;
              return [{ type: 'tool-input-delta', id: block.id, delta: delta.partial_json }];
            }
          }
          return [];
        }
        case 'content_block_stop': {
          const block = blocks.get(num(event.index));
          blocks.delete(num(event.index));
          if (block?.kind === 'thinking') return [{ type: 'reasoning-end' }];
          if (block?.kind === 'tool')
            return [
              { type: 'tool-update', id: block.id, patch: { input: parseArguments(block.json) } },
            ];
          return [];
        }
        case 'message_delta': {
          const out: AiStreamEvent[] = [];
          const delta = isObject(event.delta) ? event.delta : {};
          if (typeof delta.stop_reason === 'string') {
            const reason = contentBlockStopReason(delta.stop_reason);
            if (reason === 'tool-use')
              for (const id of clientTools)
                out.push({ type: 'tool-update', id, patch: { state: 'awaiting-result' } });
            out.push({ type: 'finish', reason });
          }
          if (isObject(event.usage))
            out.push({
              type: 'usage',
              usage: { inputTokens, outputTokens: num(event.usage.output_tokens), ...cache },
              ...(model ? { model } : {}),
            });
          return out;
        }
        case 'message_stop':
          finished = true;
          return [{ type: 'done' }];
        case 'error': {
          const error = isObject(event.error) ? event.error : {};
          const code = str(error.type) ?? 'service';
          return [
            {
              type: 'error',
              code,
              message: str(error.message) ?? unavailable,
              retryable: RETRYABLE_ERRORS.has(code),
            },
          ];
        }
        default:
          return [];
      }
    },
    flush: () => (finished ? [] : [{ type: 'done' }]),
  };
}
