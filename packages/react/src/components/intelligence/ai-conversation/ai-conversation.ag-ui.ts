/**
 * The agent–user interaction event format (`RUN_STARTED`,
 * `TEXT_MESSAGE_CONTENT`, `TOOL_CALL_ARGS`, `RUN_FINISHED`, …) used by agent
 * frameworks: a mapper for `createSseTransport` and a builder for its run
 * input. Reads the format only; no framework dependency.
 */
import type { AiStreamEvent, AiTransportRequest } from './ai-conversation.protocol.js';
import type { AiStreamMapper } from './ai-conversation.sse.js';
import type { AiMessage } from './ai-conversation.types.js';
import { AI_CONVERSATION_MESSAGES } from './ai-conversation.errors.js';
import { isObject, nowIso, parseArguments, parseJson, str } from './ai-conversation.json.js';
import { parseAiMemoryEvent } from './ai-conversation.memory.js';
import type { AiStreamMapperOptions } from './ai-conversation.providers.js';
import { isResolvedToolCall, toolResultText } from './ai-conversation.requests.js';

/**
 * Reads agent–user interaction events: text and thinking content, tool calls
 * (start, streamed arguments, end, result; several in parallel), run start
 * (a `run` event) and finish, run errors, and `CUSTOM` events named `memory`
 * (a `memory` event). A tool call without a result
 * when the run finishes is a frontend tool: it waits in `awaiting-result`.
 */
export function agUiMapper(options: AiStreamMapperOptions = {}): AiStreamMapper {
  const unavailable = options.unavailable ?? AI_CONVERSATION_MESSAGES.unavailable;
  const tools = new Map<string, { args: string; done: boolean }>();
  let thinking = false;
  let finished = false;

  const start = (id: string, name: string | undefined): AiStreamEvent[] => {
    if (tools.has(id)) return [];
    tools.set(id, { args: '', done: false });
    return [
      {
        type: 'part',
        part: {
          type: 'tool-call',
          id,
          name: name ?? 'tool',
          input: {},
          state: 'running',
          startedAt: nowIso(),
        },
      },
    ];
  };
  const args = (id: string, delta: string): AiStreamEvent[] => {
    const tool = tools.get(id);
    if (!tool || !delta) return [];
    tool.args += delta;
    return [{ type: 'tool-input-delta', id, delta }];
  };
  const finish = (): AiStreamEvent[] => {
    if (finished) return [];
    finished = true;
    const out: AiStreamEvent[] = [];
    if (thinking) out.push({ type: 'reasoning-end' });
    const waiting = [...tools].filter(([, t]) => !t.done).map(([id]) => id);
    for (const id of waiting)
      out.push({ type: 'tool-update', id, patch: { state: 'awaiting-result' } });
    out.push({ type: 'finish', reason: waiting.length ? 'tool-use' : 'stop' }, { type: 'done' });
    return out;
  };

  return {
    map(frame) {
      const event = parseJson(frame.data);
      if (!isObject(event)) return [];
      const type = str(event.type) ?? frame.event;
      const toolCallId = str(event.toolCallId);
      switch (type) {
        case 'RUN_STARTED':
          return str(event.runId) ? [{ type: 'run', runId: event.runId as string }] : [];
        case 'TEXT_MESSAGE_CONTENT':
        case 'TEXT_MESSAGE_CHUNK': {
          if (!str(event.delta)) return [];
          const out: AiStreamEvent[] = [];
          if (thinking) {
            thinking = false;
            out.push({ type: 'reasoning-end' });
          }
          out.push({ type: 'text-delta', text: event.delta as string });
          return out;
        }
        case 'THINKING_TEXT_MESSAGE_CONTENT':
        case 'REASONING_MESSAGE_CONTENT':
          if (!str(event.delta)) return [];
          thinking = true;
          return [{ type: 'reasoning-delta', text: event.delta as string }];
        case 'THINKING_END':
        case 'THINKING_TEXT_MESSAGE_END':
        case 'REASONING_END':
          if (!thinking) return [];
          thinking = false;
          return [{ type: 'reasoning-end' }];
        case 'TOOL_CALL_START':
          return toolCallId ? start(toolCallId, str(event.toolCallName)) : [];
        case 'TOOL_CALL_ARGS':
          return toolCallId ? args(toolCallId, str(event.delta) ?? '') : [];
        case 'TOOL_CALL_CHUNK':
          return toolCallId
            ? [
                ...start(toolCallId, str(event.toolCallName)),
                ...args(toolCallId, str(event.delta) ?? ''),
              ]
            : [];
        case 'TOOL_CALL_END': {
          const tool = toolCallId ? tools.get(toolCallId) : undefined;
          if (!toolCallId || !tool) return [];
          return [
            { type: 'tool-update', id: toolCallId, patch: { input: parseArguments(tool.args) } },
          ];
        }
        case 'TOOL_CALL_RESULT': {
          const tool = toolCallId ? tools.get(toolCallId) : undefined;
          if (!toolCallId) return [];
          if (tool) tool.done = true;
          const content = typeof event.content === 'string' ? event.content : '';
          const parsed = parseJson(content);
          return [
            {
              type: 'tool-update',
              id: toolCallId,
              patch: {
                state: 'succeeded',
                output: parsed === undefined ? content : parsed,
                endedAt: nowIso(),
              },
            },
          ];
        }
        case 'CUSTOM': {
          // `{ type: 'CUSTOM', name: 'memory', value: { action, id, text, scope? } }`.
          if (str(event.name)?.toLowerCase() !== 'memory') return [];
          const memory = parseAiMemoryEvent(event.value);
          return memory ? [{ type: 'memory', memory }] : [];
        }
        case 'RUN_FINISHED':
          return finish();
        case 'RUN_ERROR':
          finished = true;
          return [
            {
              type: 'error',
              code: str(event.code) ?? 'service',
              message: str(event.message) ?? unavailable,
              retryable: false,
            },
          ];
        default:
          return [];
      }
    },
    flush: finish,
  };
}

export type AgUiInputContent =
  | { type: 'text'; text: string }
  | {
      type: 'binary';
      mimeType: string;
      url?: string;
      data?: string;
      id?: string;
      filename?: string;
    };

export type AgUiMessage =
  | { id: string; role: 'system' | 'user'; content: string | AgUiInputContent[] }
  | {
      id: string;
      role: 'assistant';
      content?: string;
      toolCalls?: { id: string; type: 'function'; function: { name: string; arguments: string } }[];
    }
  | { id: string; role: 'tool'; toolCallId: string; content: string };

/** The conversation as agent–user interaction messages, with tool calls and their results. */
export function toAgUiMessages(messages: readonly AiMessage[]): AgUiMessage[] {
  const out: AgUiMessage[] = [];
  for (const m of messages) {
    const text = m.parts
      .filter((p): p is Extract<typeof p, { type: 'text' }> => p.type === 'text')
      .map((p) => p.text)
      .join('');
    if (m.role === 'system') {
      if (text) out.push({ id: m.id, role: 'system', content: text });
    } else if (m.role === 'user') {
      const files: AgUiInputContent[] = m.parts.flatMap((p) =>
        p.type === 'file' && (p.url || p.fileId)
          ? [
              {
                type: 'binary' as const,
                mimeType: p.mediaType,
                filename: p.name,
                ...(p.url?.startsWith('data:')
                  ? { data: p.url.slice(p.url.indexOf(',') + 1) }
                  : p.url
                    ? { url: p.url }
                    : { id: p.fileId as string }),
              },
            ]
          : [],
      );
      if (files.length)
        out.push({
          id: m.id,
          role: 'user',
          content: [...(text ? [{ type: 'text' as const, text }] : []), ...files],
        });
      else if (text) out.push({ id: m.id, role: 'user', content: text });
    } else if (m.role === 'assistant') {
      const calls = m.parts.filter(isResolvedToolCall);
      out.push({
        id: m.id,
        role: 'assistant',
        ...(text ? { content: text } : {}),
        ...(calls.length
          ? {
              toolCalls: calls.map((c) => ({
                id: c.id,
                type: 'function' as const,
                function: {
                  name: c.name,
                  arguments: typeof c.input === 'string' ? c.input : JSON.stringify(c.input ?? {}),
                },
              })),
            }
          : {}),
      });
      for (const c of calls)
        out.push({
          id: `${m.id}-${c.id}`,
          role: 'tool',
          toolCallId: c.id,
          content: toolResultText(c),
        });
    }
  }
  return out;
}

export interface AgUiRunInputOptions {
  /** Default: the conversation id, else a new id. */
  threadId?: string;
  /** Default: a new id. */
  runId?: string;
  /** Tool definitions the frontend offers. */
  tools?: readonly unknown[];
  /** Context entries (`{ description, value }`). */
  context?: readonly { description: string; value: string }[];
  /** Shared agent state. */
  state?: unknown;
}

let runSeq = 0;
const newId = (prefix: string) => `${prefix}-${Date.now().toString(36)}-${(++runSeq).toString(36)}`;

/**
 * The run input for an agent–user interaction endpoint:
 * `{ threadId, runId, messages, tools, context, state, forwardedProps }`.
 * `request.body` is sent as `forwardedProps`.
 */
export function buildAgUiRunInput(
  request: AiTransportRequest,
  options: AgUiRunInputOptions = {},
): Record<string, unknown> {
  return {
    threadId: options.threadId ?? request.conversationId ?? newId('thread'),
    runId: options.runId ?? newId('run'),
    messages: toAgUiMessages(request.messages),
    tools: options.tools ?? [],
    context: options.context ?? [],
    state: options.state ?? {},
    forwardedProps: request.body ?? {},
  };
}
