import { boundedString, httpUrl, isRecord } from '../../../internal/shell/embed.js';

/**
 * The Rook Chat embed protocol (`rook-chat-embed` v1), host side: the only
 * contract between a Rook app (the host, which draws the dock) and Rook Chat
 * running inside the dock's iframe. Mirrors Rook Chat's own protocol file.
 *
 * Every message is `{ protocol, v, type, ...fields }`, posted to an exact
 * origin. The host checks `event.origin` against its allow list and
 * `event.source` against the frame's window, then decodes the data; anything
 * malformed or unknown is dropped.
 *
 * Host → embed: init, open, context, visibility, refresh-result.
 * Embed → host: ready, unread, layout, ring, close, refresh-request, status, location.
 *
 * The embed never refreshes the session itself: it sends `refresh-request` and
 * the host answers with its own single-flight refresh (Rook ID rotates refresh
 * tokens; two racing refreshes sign the person out).
 */
export const CHAT_EMBED_PROTOCOL = 'rook-chat-embed';
export const CHAT_EMBED_VERSION = 1;

/** Panel sizes the embed may ask for. */
export type ChatDockLayout = 'panel' | 'expanded' | 'call' | 'fullscreen';
/** Whether Chat is usable for this person here. */
export type ChatEmbedStatus = 'ok' | 'signed-out' | 'unavailable';

export interface ChatShareTarget {
  title: string;
  url: string;
}

/** Create a task or a ticket about a page of the host app. */
export interface ChatCreateRequest {
  kind: 'task' | 'ticket';
  title: string;
  url: string;
}

/** The page of the host app the person is on. */
export interface ChatHostContext {
  app: string;
  url: string;
  title?: string;
}

/** What `open()` can ask the embed to show. */
export interface ChatOpenDetail {
  conversationId?: string;
  share?: ChatShareTarget;
  create?: ChatCreateRequest;
}

export type ChatEmbedHostMessage =
  | { type: 'rook-chat:init'; app: string; workspaceId: string }
  | ({ type: 'rook-chat:open' } & ChatOpenDetail)
  | ({ type: 'rook-chat:context' } & ChatHostContext)
  | { type: 'rook-chat:visibility'; open: boolean }
  | { type: 'rook-chat:refresh-result'; id: string; ok: boolean };

export type ChatEmbedMessage =
  | { type: 'rook-chat:ready' }
  | { type: 'rook-chat:unread'; count: number }
  | { type: 'rook-chat:layout'; layout: ChatDockLayout }
  | { type: 'rook-chat:ring'; callId: string; conversationId: string; from: string }
  | { type: 'rook-chat:close' }
  | { type: 'rook-chat:refresh-request'; id: string }
  | { type: 'rook-chat:status'; status: ChatEmbedStatus }
  | { type: 'rook-chat:location'; conversationId: string | null };

const LAYOUTS: readonly ChatDockLayout[] = ['panel', 'expanded', 'call', 'fullscreen'];
const STATUSES: readonly ChatEmbedStatus[] = ['ok', 'signed-out', 'unavailable'];
const ID = /^[A-Za-z0-9_-]{1,64}$/;
const id = (v: unknown): v is string => boundedString(v, 64) && ID.test(v);

/** The wire form of a host message. */
export function encodeChatEmbedMessage(message: ChatEmbedHostMessage): Record<string, unknown> {
  return { ...message, protocol: CHAT_EMBED_PROTOCOL, v: CHAT_EMBED_VERSION };
}

/** A message from the embed, or null when it is malformed, unknown or a host message. */
export function decodeChatEmbedMessage(data: unknown): ChatEmbedMessage | null {
  if (!isRecord(data)) return null;
  if (data.protocol !== CHAT_EMBED_PROTOCOL || data.v !== CHAT_EMBED_VERSION) return null;
  const d = data;
  switch (d.type) {
    case 'rook-chat:ready':
    case 'rook-chat:close':
      return { type: d.type };
    case 'rook-chat:unread':
      return typeof d.count === 'number' &&
        Number.isInteger(d.count) &&
        d.count >= 0 &&
        d.count < 1e6
        ? { type: d.type, count: d.count }
        : null;
    case 'rook-chat:layout':
      return LAYOUTS.includes(d.layout as ChatDockLayout)
        ? { type: d.type, layout: d.layout as ChatDockLayout }
        : null;
    case 'rook-chat:ring':
      return boundedString(d.callId, 64) &&
        boundedString(d.conversationId, 64) &&
        boundedString(d.from, 64)
        ? { type: d.type, callId: d.callId, conversationId: d.conversationId, from: d.from }
        : null;
    case 'rook-chat:refresh-request':
      return id(d.id) ? { type: d.type, id: d.id } : null;
    case 'rook-chat:status':
      return STATUSES.includes(d.status as ChatEmbedStatus)
        ? { type: d.type, status: d.status as ChatEmbedStatus }
        : null;
    case 'rook-chat:location':
      if (d.conversationId === null) return { type: d.type, conversationId: null };
      return id(d.conversationId) ? { type: d.type, conversationId: d.conversationId } : null;
    default:
      return null;
  }
}

/** Drops an `open` detail that the embed would refuse (bad ids or non-http URLs). */
export function cleanOpenDetail(detail: ChatOpenDetail | undefined): ChatOpenDetail {
  const out: ChatOpenDetail = {};
  if (!detail) return out;
  if (detail.conversationId !== undefined && id(detail.conversationId))
    out.conversationId = detail.conversationId;
  if (detail.share && boundedString(detail.share.title, 300) && httpUrl(detail.share.url))
    out.share = { title: detail.share.title, url: detail.share.url };
  const c = detail.create;
  if (
    c &&
    (c.kind === 'task' || c.kind === 'ticket') &&
    boundedString(c.title, 300) &&
    httpUrl(c.url)
  )
    out.create = { kind: c.kind, title: c.title, url: c.url };
  return out;
}

/** The page the person is on, for the embed: URL without fragment, document title. */
export function chatPageContext(app: string): ChatHostContext {
  const u = new URL(window.location.href);
  u.hash = '';
  const title = document.title.trim().slice(0, 300);
  return { app, url: u.toString(), ...(title ? { title } : {}) };
}

export interface ChatFrameWindow {
  postMessage(message: unknown, targetOrigin: string): void;
}

export interface ChatDockBridgeOptions {
  /** The frame's window now (null before the iframe exists). */
  frameWindow: () => ChatFrameWindow | null;
  /** Exact origins messages are accepted from. */
  allowedOrigins: ReadonlySet<string>;
  /** The origin messages are posted to (Rook Chat's own). Must be in `allowedOrigins`. */
  targetOrigin: string | null;
  appName: () => string;
  workspaceId: () => string;
  /** The host app's own single-flight session refresh. */
  refresh: () => Promise<unknown>;
  context: () => ChatHostContext;
  on: (message: ChatEmbedMessage) => void;
}

export interface ChatDockBridge {
  /** Feed every window `message` event here. True when it was a valid message from the frame. */
  handle(event: { origin: string; source: unknown; data: unknown }): boolean;
  /** Send to the frame, or queue until it says it is ready. */
  post(message: ChatEmbedHostMessage): void;
  isReady(): boolean;
  /** The frame is reloading: it must say ready again. Queued messages are kept. */
  reset(): void;
}

/**
 * The host side of the protocol, framework-free: listens only to the frame's
 * own window at an allowed origin, never posts to `*`, queues messages until
 * the frame is ready, and answers refresh requests with the host's refresh.
 */
export function createChatDockBridge(opts: ChatDockBridgeOptions): ChatDockBridge {
  let ready = false;
  const outbox: ChatEmbedHostMessage[] = [];
  const target = () =>
    opts.targetOrigin && opts.allowedOrigins.has(opts.targetOrigin) ? opts.targetOrigin : null;
  const send = (win: ChatFrameWindow, m: ChatEmbedHostMessage) => {
    const to = target();
    if (!to) return;
    try {
      win.postMessage(encodeChatEmbedMessage(m), to);
    } catch {
      /* the frame navigated away */
    }
  };

  return {
    handle(e) {
      const win = opts.frameWindow();
      if (!win || e.source !== win || !opts.allowedOrigins.has(e.origin)) return false;
      const m = decodeChatEmbedMessage(e.data);
      if (!m) return false;
      if (m.type === 'rook-chat:ready') {
        send(win, { type: 'rook-chat:init', app: opts.appName(), workspaceId: opts.workspaceId() });
        send(win, { type: 'rook-chat:context', ...opts.context() });
        ready = true;
        for (const queued of outbox.splice(0)) send(win, queued);
      } else if (m.type === 'rook-chat:refresh-request') {
        Promise.resolve()
          .then(() => opts.refresh())
          .then(
            () => send(win, { type: 'rook-chat:refresh-result', id: m.id, ok: true }),
            () => send(win, { type: 'rook-chat:refresh-result', id: m.id, ok: false }),
          );
      }
      opts.on(m);
      return true;
    },
    post(message) {
      const win = opts.frameWindow();
      if (win && ready) send(win, message);
      else outbox.push(message);
    },
    isReady: () => ready,
    reset() {
      ready = false;
    },
  };
}
