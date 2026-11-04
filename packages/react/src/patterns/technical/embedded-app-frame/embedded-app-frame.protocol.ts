import { hasExactKeys, isRecord } from '../../../internal/shell/embed.js';

/**
 * Rook app embed contract v1 (the App Settings hub contract in Rook ID).
 *
 * Every message is a plain object `{ protocol, v, type, ...fields }` posted to
 * an exact origin. Receivers check `event.origin` against an allow list and
 * `event.source` against the one window they talk to, then decode the data
 * with the functions below; anything else is dropped.
 *
 * Embedded page → host
 *   ready       the page has mounted (`{ protocol, v, type }`, exactly three keys,
 *               as Rook ID's hub expects)
 *   resize      the page's content height in CSS pixels, for auto-height
 *   focus-exit  the person tabbed or pressed Escape out of the embedded page
 *
 * Host → embedded page
 *   ping        "are you there?": a page answers with `ready` again
 *   focus       move focus to the first or last control inside the page
 *
 * `resize`, `focus-exit` and `focus` are additions to the hub's `ready`/`ping`
 * pair. Adding a type is backwards compatible (receivers drop types they do
 * not know); changing a shape bumps the version.
 */
export const APP_EMBED_PROTOCOL = 'rook-app-settings';
export const APP_EMBED_VERSION = 1;

/** Where focus goes in a `focus` or `focus-exit` message. */
export type EmbedFocusDirection = 'forward' | 'backward';

export type AppEmbedChildMessage =
  | { type: 'ready' }
  | { type: 'resize'; height: number }
  | { type: 'focus-exit'; direction: EmbedFocusDirection };

export type AppEmbedHostMessage =
  { type: 'ping' } | { type: 'focus'; direction: EmbedFocusDirection };

/** The largest height a frame may report; larger values are ignored. */
export const MAX_EMBED_HEIGHT = 100_000;

const DIRECTIONS: readonly EmbedFocusDirection[] = ['forward', 'backward'];

/** The wire form of a message: `{ protocol, v, ...message }`. */
export function encodeAppEmbedMessage(
  message: AppEmbedChildMessage | AppEmbedHostMessage,
  protocol: string = APP_EMBED_PROTOCOL,
): Record<string, unknown> {
  return { protocol, v: APP_EMBED_VERSION, ...message };
}

function envelope(data: unknown, protocol: string): Record<string, unknown> | null {
  if (!isRecord(data)) return null;
  if (data.protocol !== protocol || data.v !== APP_EMBED_VERSION) return null;
  return typeof data.type === 'string' ? data : null;
}

/** A message from the embedded page, or null when it is malformed or unknown. */
export function decodeAppEmbedChildMessage(
  data: unknown,
  protocol: string = APP_EMBED_PROTOCOL,
): AppEmbedChildMessage | null {
  const d = envelope(data, protocol);
  if (!d) return null;
  switch (d.type) {
    case 'ready':
      return hasExactKeys(d, ['protocol', 'v', 'type']) ? { type: 'ready' } : null;
    case 'resize': {
      const h = d.height;
      return hasExactKeys(d, ['protocol', 'v', 'type', 'height']) &&
        typeof h === 'number' &&
        Number.isFinite(h) &&
        h >= 0 &&
        h <= MAX_EMBED_HEIGHT
        ? { type: 'resize', height: Math.ceil(h) }
        : null;
    }
    case 'focus-exit':
      return hasExactKeys(d, ['protocol', 'v', 'type', 'direction']) &&
        DIRECTIONS.includes(d.direction as EmbedFocusDirection)
        ? { type: 'focus-exit', direction: d.direction as EmbedFocusDirection }
        : null;
    default:
      return null;
  }
}

/** A message from the host page, or null when it is malformed or unknown. */
export function decodeAppEmbedHostMessage(
  data: unknown,
  protocol: string = APP_EMBED_PROTOCOL,
): AppEmbedHostMessage | null {
  const d = envelope(data, protocol);
  if (!d) return null;
  switch (d.type) {
    case 'ping':
      return hasExactKeys(d, ['protocol', 'v', 'type']) ? { type: 'ping' } : null;
    case 'focus':
      return hasExactKeys(d, ['protocol', 'v', 'type', 'direction']) &&
        DIRECTIONS.includes(d.direction as EmbedFocusDirection)
        ? { type: 'focus', direction: d.direction as EmbedFocusDirection }
        : null;
    default:
      return null;
  }
}
