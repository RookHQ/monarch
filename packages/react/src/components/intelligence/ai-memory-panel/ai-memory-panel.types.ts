import type { AiMemory, AiMemoryEntity } from '../ai-memory-chip/index.js';

/** A remembered fact as AiMemoryPanel lists it: always about an entity, with a last-updated time. */
export interface AiMemoryItem extends AiMemory {
  /** Who or what the memory is about; memories are grouped by it. */
  entity: AiMemoryEntity;
  /** When the memory was saved or last changed: ISO 8601 string, epoch milliseconds or a Date. */
  updatedAt: string | number | Date;
}
