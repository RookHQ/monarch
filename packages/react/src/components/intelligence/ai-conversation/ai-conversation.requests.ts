/**
 * Request builders: the message model serialised for the two common vendor
 * formats, with attachments as image and file content and assistant tool
 * calls followed by their results, so a conversation that used tools can be
 * sent back. Use them as `createSseTransport({ buildBody })` on your own
 * service, which adds the model and the key.
 */
import type { AiTransportRequest } from './ai-conversation.protocol.js';
import type { AiFilePart, AiMessage, AiPart, AiToolCallPart } from './ai-conversation.types.js';

/** A tool call that has an outcome the model can be told about. */
export function isResolvedToolCall(part: AiPart): part is AiToolCallPart {
  return (
    part.type === 'tool-call' &&
    (part.state === 'succeeded' || part.state === 'failed' || part.state === 'denied')
  );
}

/** The result text sent to the model for a resolved tool call. */
export function toolResultText(part: AiToolCallPart): string {
  if (part.state === 'denied')
    return JSON.stringify({ error: 'denied', ...(part.error ? { reason: part.error } : {}) });
  if (part.state === 'failed') return JSON.stringify({ error: part.error ?? 'failed' });
  if (typeof part.output === 'string') return part.output;
  return part.output === undefined ? '' : JSON.stringify(part.output);
}

/** Splits a `data:` URL into its media type and base64 payload. */
function parseDataUrl(url: string): { mediaType: string; data: string } | null {
  const match = /^data:([^;,]+)?(?:;[^;,]*)*;base64,(.*)$/s.exec(url);
  if (!match) return null;
  return { mediaType: match[1] ?? 'application/octet-stream', data: match[2] ?? '' };
}

/** One stretch of an assistant message: text, then the tool calls that followed it. */
interface AssistantSegment {
  text: string;
  calls: AiToolCallPart[];
}

/**
 * Cuts an assistant message where it called tools: each segment is the text
 * written before a run of resolved tool calls, and those calls. Unresolved
 * calls (waiting, running, cancelled) are left out, since the model must not
 * see a call without its result.
 */
function assistantSegments(message: AiMessage): AssistantSegment[] {
  const out: AssistantSegment[] = [];
  let current: AssistantSegment = { text: '', calls: [] };
  for (const part of message.parts) {
    if (part.type === 'text') {
      if (current.calls.length) {
        out.push(current);
        current = { text: '', calls: [] };
      }
      current.text += part.text;
    } else if (isResolvedToolCall(part)) current.calls.push(part);
  }
  if (current.text || current.calls.length) out.push(current);
  return out;
}

const textOf = (m: AiMessage) =>
  m.parts
    .filter((p): p is Extract<AiPart, { type: 'text' }> => p.type === 'text')
    .map((p) => p.text)
    .join('');

const filesOf = (m: AiMessage) => m.parts.filter((p): p is AiFilePart => p.type === 'file');

// ------------------------------------------------- chat completions

export type ChatCompletionsContentPart =
  | { type: 'text'; text: string }
  | { type: 'image_url'; image_url: { url: string } }
  | { type: 'file'; file: { file_id?: string; file_data?: string; filename?: string } };

export interface ChatCompletionsToolCall {
  id: string;
  type: 'function';
  function: { name: string; arguments: string };
}

export type ChatCompletionsMessage =
  | { role: 'system'; content: string }
  | { role: 'user'; content: string | ChatCompletionsContentPart[] }
  | { role: 'assistant'; content: string | null; tool_calls?: ChatCompletionsToolCall[] }
  | { role: 'tool'; tool_call_id: string; content: string };

function chatFilePart(file: AiFilePart): ChatCompletionsContentPart | null {
  if (file.mediaType.startsWith('image/') && file.url)
    return { type: 'image_url', image_url: { url: file.url } };
  if (file.fileId) return { type: 'file', file: { file_id: file.fileId, filename: file.name } };
  if (file.url?.startsWith('data:'))
    return { type: 'file', file: { file_data: file.url, filename: file.name } };
  if (file.url) return { type: 'text', text: `[${file.name}](${file.url})` };
  return null;
}

/**
 * The conversation in the "chat completions" message format: system and user
 * turns (attachments as `image_url` and `file` content), assistant turns with
 * `tool_calls`, and a `tool` message with each call's result.
 */
export function toChatCompletionsMessages(
  messages: readonly AiMessage[],
): ChatCompletionsMessage[] {
  const out: ChatCompletionsMessage[] = [];
  for (const m of messages) {
    switch (m.role) {
      case 'system': {
        const text = textOf(m);
        if (text) out.push({ role: 'system', content: text });
        break;
      }
      case 'user': {
        const text = textOf(m);
        const files = filesOf(m)
          .map(chatFilePart)
          .filter((p): p is ChatCompletionsContentPart => p !== null);
        if (!files.length) {
          if (text) out.push({ role: 'user', content: text });
        } else
          out.push({
            role: 'user',
            content: [...(text ? [{ type: 'text' as const, text }] : []), ...files],
          });
        break;
      }
      case 'assistant':
        for (const seg of assistantSegments(m)) {
          out.push({
            role: 'assistant',
            content: seg.text || null,
            ...(seg.calls.length
              ? {
                  tool_calls: seg.calls.map((c) => ({
                    id: c.id,
                    type: 'function' as const,
                    function: {
                      name: c.name,
                      arguments:
                        typeof c.input === 'string' ? c.input : JSON.stringify(c.input ?? {}),
                    },
                  })),
                }
              : {}),
          });
          for (const c of seg.calls)
            out.push({ role: 'tool', tool_call_id: c.id, content: toolResultText(c) });
        }
        break;
      case 'tool':
        // Tool messages are represented as parts of the assistant turn.
        break;
    }
  }
  return out;
}

/** `buildBody` for a chat-completions service: `{ messages, conversationId?, ...body }`. */
export function buildChatCompletionsBody(request: AiTransportRequest): Record<string, unknown> {
  return {
    messages: toChatCompletionsMessages(request.messages),
    ...(request.conversationId ? { conversationId: request.conversationId } : {}),
    ...request.body,
  };
}

// ------------------------------------------------- content blocks

type BlockSource =
  | { type: 'url'; url: string }
  | { type: 'base64'; media_type: string; data: string }
  | { type: 'file'; file_id: string };

export type ContentBlock =
  | { type: 'text'; text: string }
  | { type: 'image'; source: BlockSource }
  | { type: 'document'; source: BlockSource; title?: string }
  | { type: 'tool_use'; id: string; name: string; input: unknown }
  | { type: 'tool_result'; tool_use_id: string; content: string; is_error?: boolean };

export interface ContentBlockMessage {
  role: 'user' | 'assistant';
  content: ContentBlock[];
}

function blockSource(file: AiFilePart): BlockSource | null {
  if (file.fileId) return { type: 'file', file_id: file.fileId };
  if (!file.url) return null;
  const inline = parseDataUrl(file.url);
  if (inline) return { type: 'base64', media_type: inline.mediaType, data: inline.data };
  return { type: 'url', url: file.url };
}

function fileBlock(file: AiFilePart): ContentBlock | null {
  const source = blockSource(file);
  if (!source) return null;
  return file.mediaType.startsWith('image/')
    ? { type: 'image', source }
    : { type: 'document', source, title: file.name };
}

/** Joins neighbouring turns of the same role, which the format requires to alternate. */
function alternate(list: ContentBlockMessage[]): ContentBlockMessage[] {
  const out: ContentBlockMessage[] = [];
  for (const m of list) {
    const prev = out[out.length - 1];
    if (prev && prev.role === m.role) prev.content = [...prev.content, ...m.content];
    else out.push({ role: m.role, content: [...m.content] });
  }
  return out;
}

/**
 * The conversation in the "messages" content-block format: `system` text
 * apart, user turns with `image` and `document` blocks for attachments,
 * assistant turns with `tool_use` blocks, each followed by a user turn of
 * `tool_result` blocks. Turns of the same role are joined.
 */
export function toContentBlockMessages(messages: readonly AiMessage[]): {
  system?: string;
  messages: ContentBlockMessage[];
} {
  const system: string[] = [];
  const out: ContentBlockMessage[] = [];
  for (const m of messages) {
    switch (m.role) {
      case 'system': {
        const text = textOf(m);
        if (text) system.push(text);
        break;
      }
      case 'user': {
        const text = textOf(m);
        const content: ContentBlock[] = [
          ...filesOf(m)
            .map(fileBlock)
            .filter((b): b is ContentBlock => b !== null),
          ...(text ? [{ type: 'text' as const, text }] : []),
        ];
        if (content.length) out.push({ role: 'user', content });
        break;
      }
      case 'assistant':
        for (const seg of assistantSegments(m)) {
          out.push({
            role: 'assistant',
            content: [
              ...(seg.text ? [{ type: 'text' as const, text: seg.text }] : []),
              ...seg.calls.map((c) => ({
                type: 'tool_use' as const,
                id: c.id,
                name: c.name,
                input: c.input ?? {},
              })),
            ],
          });
          if (seg.calls.length)
            out.push({
              role: 'user',
              content: seg.calls.map((c) => ({
                type: 'tool_result' as const,
                tool_use_id: c.id,
                content: toolResultText(c),
                ...(c.state !== 'succeeded' ? { is_error: true } : {}),
              })),
            });
        }
        break;
      case 'tool':
        break;
    }
  }
  return { ...(system.length ? { system: system.join('\n\n') } : {}), messages: alternate(out) };
}

/** `buildBody` for a content-block service: `{ system?, messages, conversationId?, ...body }`. */
export function buildContentBlocksBody(request: AiTransportRequest): Record<string, unknown> {
  return {
    ...toContentBlockMessages(request.messages),
    ...(request.conversationId ? { conversationId: request.conversationId } : {}),
    ...request.body,
  };
}
