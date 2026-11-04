'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

/** Who wrote a support message. */
export type SupportMessageAuthor = 'visitor' | 'agent' | 'system' | 'bot';

export interface SupportChatMessage {
  /** Stable id (dedupes repeats from the live stream). */
  id: string;
  author: SupportMessageAuthor;
  /** Agent or bot name; never an email. */
  authorName?: string | undefined;
  /** Plain text. Rendered as text, never as HTML. */
  text: string;
  /** ISO timestamp or Date. */
  createdAt: string | Date;
}

export interface SupportChatAvailability {
  online: boolean;
  /** e.g. "Typically replies in a few minutes". */
  expectedReply?: string | undefined;
}

/** What `connect` resolves with. */
export interface SupportChatSnapshot {
  messages: SupportChatMessage[];
  availability?: SupportChatAvailability | undefined;
}

export type SupportChatEvent =
  | { type: 'message'; message: SupportChatMessage }
  | { type: 'typing'; typing: boolean; name?: string | undefined }
  | { type: 'availability'; availability: SupportChatAvailability }
  /** The session ended (signed out) or support went away: the panel shows the fallback. */
  | { type: 'ended'; reason: 'session' | 'unavailable' };

/**
 * The product's Support Chat API client. Monarch makes no network calls: the
 * product implements this over its own Support Chat API (or SDK), with its own
 * identity and single-flight session refresh, and passes it to SupportPanel.
 */
export interface SupportChatClient {
  /** Start or resume this person's conversation. Called on first open and on Try again. */
  connect(options: { signal: AbortSignal }): Promise<SupportChatSnapshot>;
  /** Send a message; resolve with the stored message. Retried with the same `clientMessageId`. */
  send(text: string, options: { clientMessageId: string }): Promise<SupportChatMessage>;
  /** Live events while the panel is mounted. Returns an unsubscribe function. */
  subscribe?(listener: (event: SupportChatEvent) => void): () => void;
  /** The person has seen the conversation up to this message. */
  markRead?(messageId: string): void;
}

/** A message in the panel: a stored one, or one of the person's still sending or failed. */
export interface SupportChatEntry extends SupportChatMessage {
  pending?: 'sending' | 'failed' | undefined;
  clientMessageId?: string | undefined;
}

export type SupportChatPhase = 'idle' | 'connecting' | 'ready' | 'error' | 'ended';

export interface SupportChatState {
  phase: SupportChatPhase;
  /** Why the chat ended or failed. */
  reason?: 'timeout' | 'failed' | 'session' | 'unavailable' | undefined;
  entries: SupportChatEntry[];
  availability?: SupportChatAvailability | undefined;
  typing: { name?: string | undefined } | null;
}

export interface SupportChatController {
  state: SupportChatState;
  connect: () => void;
  send: (text: string) => void;
  resend: (clientMessageId: string) => void;
  markRead: () => void;
}

let counter = 0;
function newClientMessageId(): string {
  counter += 1;
  const rand = Math.random().toString(36).slice(2, 10);
  return `c_${Date.now().toString(36)}${counter.toString(36)}${rand}`;
}

function merge(entries: SupportChatEntry[], incoming: SupportChatMessage[]): SupportChatEntry[] {
  const byId = new Map(entries.map((e) => [e.id, e]));
  for (const m of incoming) byId.set(m.id, { ...m });
  return [...byId.values()];
}

const INITIAL: SupportChatState = { phase: 'idle', entries: [], typing: null };

/** Maximum length of a message (the Support Chat API limit). */
export const SUPPORT_MESSAGE_MAX = 5000;

/**
 * Drives the panel's chat from a SupportChatClient: connects on demand with a
 * timeout, keeps messages across close and reopen, sends with optimistic
 * entries and retry, and follows live events. Exported for products that
 * build their own chat surface on the same client.
 */
export function useSupportChat(
  client: SupportChatClient | undefined,
  { timeout = 15_000 }: { timeout?: number } = {},
): SupportChatController {
  const [state, setState] = useState<SupportChatState>(INITIAL);
  const stateRef = useRef(state);
  stateRef.current = state;
  const clientRef = useRef(client);
  clientRef.current = client;
  const abort = useRef<AbortController | null>(null);
  const alive = useRef(true);
  const lastRead = useRef<string | null>(null);

  useEffect(() => {
    alive.current = true;
    return () => {
      alive.current = false;
      abort.current?.abort();
    };
  }, []);

  const connect = useCallback(() => {
    const c = clientRef.current;
    if (!c) return;
    abort.current?.abort();
    const ctl = new AbortController();
    abort.current = ctl;
    setState((s) => ({ ...s, phase: 'connecting', reason: undefined }));
    const current = () => alive.current && abort.current === ctl;
    let timedOut = false;
    const timer =
      timeout > 0
        ? setTimeout(() => {
            timedOut = true;
            ctl.abort();
            if (current()) setState((s) => ({ ...s, phase: 'error', reason: 'timeout' }));
          }, timeout)
        : undefined;
    c.connect({ signal: ctl.signal }).then(
      (snap) => {
        clearTimeout(timer);
        if (!current() || timedOut) return;
        setState((s) => ({
          ...s,
          phase: 'ready',
          entries: merge(
            s.entries.filter((e) => e.pending),
            snap.messages,
          ),
          availability: snap.availability ?? s.availability,
        }));
      },
      () => {
        clearTimeout(timer);
        if (!current() || timedOut) return;
        setState((s) => ({ ...s, phase: 'error', reason: 'failed' }));
      },
    );
  }, [timeout]);

  // Live events, for as long as the panel lives and has connected once.
  const subscribed = state.phase !== 'idle';
  useEffect(() => {
    if (!subscribed || !client?.subscribe) return;
    return client.subscribe((event) => {
      if (!alive.current) return;
      setState((s) => {
        switch (event.type) {
          case 'message': {
            // The stored copy of a message we sent replaces its optimistic entry.
            return { ...s, entries: merge(s.entries, [event.message]), typing: null };
          }
          case 'typing':
            return { ...s, typing: event.typing ? { name: event.name } : null };
          case 'availability':
            return { ...s, availability: event.availability };
          case 'ended':
            return { ...s, phase: 'ended', reason: event.reason, typing: null };
          default:
            return s;
        }
      });
    });
  }, [subscribed, client]);

  const deliver = useCallback((clientMessageId: string, text: string) => {
    const c = clientRef.current;
    if (!c) return;
    c.send(text, { clientMessageId }).then(
      (stored) => {
        if (!alive.current) return;
        setState((s) => ({
          ...s,
          entries: merge(
            s.entries.filter((e) => e.clientMessageId !== clientMessageId),
            [stored],
          ),
        }));
      },
      () => {
        if (!alive.current) return;
        setState((s) => ({
          ...s,
          entries: s.entries.map((e) =>
            e.clientMessageId === clientMessageId ? { ...e, pending: 'failed' } : e,
          ),
        }));
      },
    );
  }, []);

  const send = useCallback(
    (raw: string) => {
      const text = raw.trim().slice(0, SUPPORT_MESSAGE_MAX);
      if (!text) return;
      const clientMessageId = newClientMessageId();
      setState((s) => ({
        ...s,
        entries: [
          ...s.entries,
          {
            id: clientMessageId,
            clientMessageId,
            author: 'visitor',
            text,
            createdAt: new Date(),
            pending: 'sending',
          },
        ],
      }));
      deliver(clientMessageId, text);
    },
    [deliver],
  );

  const resend = useCallback(
    (clientMessageId: string) => {
      const entry = stateRef.current.entries.find((e) => e.clientMessageId === clientMessageId);
      if (!entry || entry.pending !== 'failed') return;
      setState((s) => ({
        ...s,
        entries: s.entries.map((e) =>
          e.clientMessageId === clientMessageId ? { ...e, pending: 'sending' } : e,
        ),
      }));
      deliver(clientMessageId, entry.text);
    },
    [deliver],
  );

  const markRead = useCallback(() => {
    const last = [...state.entries].reverse().find((e) => !e.pending && e.author !== 'visitor');
    if (!last || last.id === lastRead.current) return;
    lastRead.current = last.id;
    clientRef.current?.markRead?.(last.id);
  }, [state.entries]);

  return { state, connect, send, resend, markRead };
}
