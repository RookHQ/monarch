/**
 * Keeps a memory list in step with a conversation: applies the `memory`
 * events a reply streams (`useAiConversation({ onMemoryEvent })`) to the list
 * AiMemoryPanel shows. Pure.
 */
import type { AiMemoryEvent } from '../ai-conversation/index.js';
import type { AiMemoryEntity } from '../ai-memory-chip/index.js';
import type { AiMemoryItem } from './ai-memory-panel.types.js';

export interface ApplyAiMemoryEventOptions {
  /** The time a written memory gets as `updatedAt`. Default: now, as an ISO 8601 string. */
  now?: (() => string | number | Date) | undefined;
  /**
   * Who or what a new memory is about. Default: an `other` entity named by
   * the event's `scope`, or `defaultEntityName` when it has none.
   */
  entity?: ((event: AiMemoryEvent) => AiMemoryEntity) | undefined;
  /** Name of the group for new memories without a scope. Default "General". */
  defaultEntityName?: string | undefined;
}

/**
 * Applies one memory event to a list. `write` updates the memory with that
 * id (text and time) or adds it at the end; `forget` removes it; `read`
 * changes nothing. Returns the same array when nothing changed, so it is
 * safe as a state updater.
 */
export function applyAiMemoryEvent(
  memories: readonly AiMemoryItem[],
  event: AiMemoryEvent,
  options: ApplyAiMemoryEventOptions = {},
): readonly AiMemoryItem[] {
  const at = memories.findIndex((memory) => memory.id === event.id);
  switch (event.action) {
    case 'read':
      return memories;
    case 'forget':
      return at < 0 ? memories : memories.filter((_, i) => i !== at);
    case 'write': {
      const updatedAt = options.now ? options.now() : new Date().toISOString();
      const existing = memories[at];
      if (existing) {
        if (existing.text === event.text) return memories;
        return memories.map((memory, i) =>
          i === at ? { ...memory, text: event.text, updatedAt } : memory,
        );
      }
      const entity: AiMemoryEntity = options.entity
        ? options.entity(event)
        : { kind: 'other', name: event.scope ?? options.defaultEntityName ?? 'General' };
      return [...memories, { id: event.id, text: event.text, entity, updatedAt }];
    }
  }
}

/** Applies several memory events in order. */
export function applyAiMemoryEvents(
  memories: readonly AiMemoryItem[],
  events: Iterable<AiMemoryEvent>,
  options: ApplyAiMemoryEventOptions = {},
): readonly AiMemoryItem[] {
  let out = memories;
  for (const event of events) out = applyAiMemoryEvent(out, event, options);
  return out;
}
