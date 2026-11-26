/**
 * Pure reducers from stream events to the message model. No React, so the
 * hook, the testing kit and server code share one implementation.
 */
import type { AiMemoryEvent, AiStreamEvent } from './ai-conversation.protocol.js';
import type {
  AiFinishReason,
  AiMemoryPart,
  AiMessage,
  AiPart,
  AiToolCallPart,
} from './ai-conversation.types.js';
import { isRetryableErrorCode } from './ai-conversation.errors.js';

/** How a reply ended, which decides what happens to tool calls still `running`. */
export type AiReplyEnd = 'done' | 'aborted' | 'error';

/**
 * Closes a reply's parts: streaming text and reasoning become `done`. A tool
 * call still `running` becomes `awaiting-result` when the model stopped to
 * call tools (`finishReason: 'tool-use'`), and `cancelled` otherwise.
 * Returns the same array when nothing changed.
 */
export function finishParts(
  parts: readonly AiPart[],
  end: AiReplyEnd = 'done',
  finishReason?: AiFinishReason,
): AiPart[] {
  let changed = false;
  const out = parts.map((p): AiPart => {
    if ((p.type === 'text' || p.type === 'reasoning') && p.state === 'streaming') {
      changed = true;
      return { ...p, state: 'done' };
    }
    if (p.type === 'tool-call' && p.state === 'running') {
      changed = true;
      const waits = end === 'done' && finishReason === 'tool-use';
      return { ...p, state: waits ? 'awaiting-result' : 'cancelled' };
    }
    return p;
  });
  return changed ? out : (parts as AiPart[]);
}

function lastIndex<T>(list: readonly T[], test: (item: T) => boolean): number {
  for (let i = list.length - 1; i >= 0; i--) if (test(list[i] as T)) return i;
  return -1;
}

function closeLast(parts: AiPart[], type: 'text' | 'reasoning') {
  const last = parts[parts.length - 1];
  if (last?.type === type && last.state === 'streaming')
    parts[parts.length - 1] = { ...last, state: 'done' };
}

function patchTool(
  parts: readonly AiPart[],
  id: string,
  patch: (p: AiToolCallPart) => AiToolCallPart,
): AiPart[] {
  return parts.map((p) => (p.type === 'tool-call' && p.id === id ? patch(p) : p));
}

/** The `memory` part for a memory event. */
function memoryPart(memory: AiMemoryEvent): AiMemoryPart {
  return {
    type: 'memory',
    id: memory.id,
    action: memory.action,
    text: memory.text,
    ...(memory.scope !== undefined ? { scope: memory.scope } : {}),
  };
}

/** Applies one stream event to the parts of the message being written. Pure. */
export function applyStreamEvent(message: AiMessage, event: AiStreamEvent): AiMessage {
  switch (event.type) {
    case 'agent':
      return { ...message, agent: event.agent };
    case 'text-delta': {
      const parts = [...message.parts];
      const last = parts[parts.length - 1];
      if (last?.type === 'text' && last.state === 'streaming')
        parts[parts.length - 1] = { ...last, text: last.text + event.text };
      else {
        closeLast(parts, 'reasoning');
        parts.push({ type: 'text', text: event.text, state: 'streaming' });
      }
      return { ...message, parts };
    }
    case 'reasoning-delta': {
      const parts = [...message.parts];
      const last = parts[parts.length - 1];
      if (last?.type === 'reasoning' && last.state === 'streaming')
        parts[parts.length - 1] = { ...last, text: last.text + event.text };
      else {
        // Text followed by reasoning: the text is finished.
        closeLast(parts, 'text');
        parts.push({ type: 'reasoning', text: event.text, state: 'streaming' });
      }
      return { ...message, parts };
    }
    case 'reasoning-end': {
      const parts = [...message.parts];
      const streamingAt = lastIndex(
        parts,
        (p) => p.type === 'reasoning' && p.state === 'streaming',
      );
      // A redacted block with nothing streaming is a reasoning part of its own.
      let i =
        streamingAt >= 0 || event.redacted
          ? streamingAt
          : lastIndex(parts, (p) => p.type === 'reasoning');
      const r = i >= 0 ? parts[i] : undefined;
      if (r?.type === 'reasoning')
        parts[i] = {
          ...r,
          state: 'done',
          ...(event.durationMs !== undefined ? { durationMs: event.durationMs } : {}),
          ...(event.redacted ? { redacted: true } : {}),
        };
      else if (event.redacted) {
        i = parts.length;
        parts.push({
          type: 'reasoning',
          text: '',
          state: 'done',
          redacted: true,
          ...(event.durationMs !== undefined ? { durationMs: event.durationMs } : {}),
        });
      }
      return { ...message, parts };
    }
    case 'part': {
      const incoming = event.part;
      // A tool call announced twice (start, then complete input) is merged.
      if (
        incoming.type === 'tool-call' &&
        message.parts.some((p) => p.type === 'tool-call' && p.id === incoming.id)
      )
        return {
          ...message,
          parts: patchTool(message.parts, incoming.id, (p) => ({ ...p, ...incoming })),
        };
      const parts = [...message.parts];
      closeLast(parts, 'text');
      parts.push(incoming);
      return { ...message, parts };
    }
    case 'tool-update':
      return {
        ...message,
        parts: patchTool(message.parts, event.id, (p) => ({ ...p, ...event.patch })),
      };
    case 'tool-input-delta':
      return {
        ...message,
        parts: patchTool(message.parts, event.id, (p) => ({
          ...p,
          inputText: (p.inputText ?? '') + event.delta,
        })),
      };
    case 'memory': {
      const next = memoryPart(event.memory);
      const at = message.parts.findIndex(
        (p) => p.type === 'memory' && p.id === next.id && p.action === next.action,
      );
      if (at >= 0) return { ...message, parts: message.parts.map((p, i) => (i === at ? next : p)) };
      const parts = [...message.parts];
      closeLast(parts, 'text');
      closeLast(parts, 'reasoning');
      parts.push(next);
      return { ...message, parts };
    }
    case 'finish':
      return { ...message, finishReason: event.reason };
    case 'run':
      return { ...message, runId: event.runId };
    case 'usage':
      return { ...message, usage: event.usage, ...(event.model ? { model: event.model } : {}) };
    case 'error': {
      const code = event.code ?? 'unknown';
      return {
        ...message,
        parts: [
          ...finishParts(message.parts, 'error'),
          {
            type: 'error',
            code,
            message: event.message,
            retryable: event.retryable ?? isRetryableErrorCode(code),
            ...(event.retryAfterMs !== undefined ? { retryAfterMs: event.retryAfterMs } : {}),
            ...(event.resumable ? { resumable: true } : {}),
          },
        ],
      };
    }
    case 'done':
      return { ...message, parts: finishParts(message.parts, 'done', message.finishReason) };
  }
}

/** Applies a list of events in order. */
export function applyStreamEvents(message: AiMessage, events: Iterable<AiStreamEvent>): AiMessage {
  let out = message;
  for (const e of events) out = applyStreamEvent(out, e);
  return out;
}

/** Tool calls of a message that still wait for a person or for the client. */
export function unresolvedToolCalls(message: Pick<AiMessage, 'parts'>): AiToolCallPart[] {
  return message.parts.filter(
    (p): p is AiToolCallPart =>
      p.type === 'tool-call' && (p.state === 'pending-approval' || p.state === 'awaiting-result'),
  );
}
