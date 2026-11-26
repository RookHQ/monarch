/**
 * Memory in the stream: reads a memory change from the loosely shaped data
 * that custom events carry, so each stream format can map its own event to
 * the one `memory` stream event. Pure.
 */
import type { AiMemoryEvent } from './ai-conversation.protocol.js';
import type { AiMemoryAction, AiMemoryPart, AiMessage } from './ai-conversation.types.js';
import { isObject, str } from './ai-conversation.json.js';

const ACTIONS: Record<string, AiMemoryAction> = {
  read: 'read',
  recall: 'read',
  recalled: 'read',
  retrieve: 'read',
  used: 'read',
  use: 'read',
  write: 'write',
  save: 'write',
  saved: 'write',
  create: 'write',
  add: 'write',
  update: 'write',
  remember: 'write',
  forget: 'forget',
  forgot: 'forget',
  delete: 'forget',
  remove: 'forget',
};

/**
 * Reads a memory change from unknown data, e.g. the payload of a custom
 * stream event: `{ action, id, text, scope? }`. `op` or `operation` may name
 * the action (`recall`, `save`, `delete`… are understood), `memoryId` the id
 * and `content` or `memory` the text. Returns null when there is no id or no
 * known action; a `forget` may come without text.
 */
export function parseAiMemoryEvent(value: unknown, fallbackId?: string): AiMemoryEvent | null {
  if (!isObject(value)) return null;
  const name = str(value.action) ?? str(value.op) ?? str(value.operation);
  const action = name ? ACTIONS[name.toLowerCase()] : undefined;
  const id = str(value.id) ?? str(value.memoryId) ?? fallbackId;
  if (!action || !id) return null;
  const text = str(value.text) ?? str(value.content) ?? str(value.memory) ?? '';
  if (!text && action !== 'forget') return null;
  const scope = str(value.scope);
  return { id, action, text, ...(scope ? { scope } : {}) };
}

/** The memory parts of a message, in order: what the reply used, saved and forgot. */
export function messageMemories(message: Pick<AiMessage, 'parts'>): AiMemoryPart[] {
  return message.parts.filter((p): p is AiMemoryPart => p.type === 'memory');
}
