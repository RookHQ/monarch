'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { AiAgentRef, AiMessage, AiPart, AiToolCallPart } from './ai-conversation.types.js';
import type { AiStreamEvent, AiTransport, AiTrigger } from './ai-conversation.transport.js';

export * from './ai-conversation.types.js';
export * from './ai-conversation.transport.js';

/**
 * - `idle`: nothing running.
 * - `submitted`: sent, waiting for the first event.
 * - `streaming`: events are arriving.
 * - `awaiting-approval`: a tool call waits for the person to approve or deny it.
 * - `error`: the last turn failed; `error` holds why.
 * - `stopped`: the person pressed Stop.
 */
export type AiConversationStatus =
  'idle' | 'submitted' | 'streaming' | 'awaiting-approval' | 'error' | 'stopped';

export interface AiConversationError {
  code: string;
  message: string;
  retryable: boolean;
}

export interface SendOptions {
  /** Attached files, as `file` parts on the user message. */
  files?: Extract<AiPart, { type: 'file' }>[];
  /** Extra request fields for the transport (`AiTransportRequest.body`). */
  body?: Record<string, unknown>;
}

export interface UseAiConversationOptions {
  transport: AiTransport;
  /** Messages to start with, e.g. a stored conversation. Shown as one branch. */
  initialMessages?: readonly AiMessage[];
  /** Id from an earlier session, sent with every request. */
  conversationId?: string;
  /** Request fields sent on every turn, merged under `send(…, { body })`. */
  body?: Record<string, unknown>;
  /** Called when a reply finishes (not when stopped or failed). */
  onFinish?: (message: AiMessage) => void;
  onError?: (error: AiConversationError) => void;
  /** Called when a tool call is approved or denied, for transports without `respondToTool`. */
  onToolResponse?: (response: {
    toolCallId: string;
    approved: boolean;
    input?: unknown;
    reason?: string;
  }) => void;
  /** Custom ids, e.g. for stable test output. */
  generateId?: () => string;
}

export interface UseAiConversationResult {
  /** The visible conversation: the active branch, oldest first. */
  messages: AiMessage[];
  status: AiConversationStatus;
  error: AiConversationError | null;
  conversationId: string | undefined;
  /** The agent from the latest `agent` event. */
  agent: AiAgentRef | undefined;
  /** True while `submitted` or `streaming`. */
  busy: boolean;
  send: (text: string, options?: SendOptions) => void;
  /** Aborts the reply in progress; what arrived so far stays. */
  stop: () => void;
  /** Asks again for an assistant message (default: the last one), as a new branch. */
  regenerate: (messageId?: string) => void;
  /** Replaces a user message with new text as a new branch and asks again. */
  editAndResend: (messageId: string, text: string, options?: SendOptions) => void;
  approveTool: (toolCallId: string, input?: unknown) => void;
  denyTool: (toolCallId: string, reason?: string) => void;
  /** Shows another branch of a message: `index` among its alternatives (0-based). */
  branchTo: (messageId: string, index: number) => void;
  /** Sets the feedback shown on a message (send it to your feedback API yourself). */
  setFeedback: (messageId: string, feedback: 'up' | 'down' | undefined) => void;
  /** Replaces the whole conversation, e.g. to load another one. */
  reset: (messages?: readonly AiMessage[]) => void;
}

interface Tree {
  nodes: Record<string, AiMessage>;
  parent: Record<string, string | null>;
  /** Children per parent id (`''` is the root). */
  children: Record<string, string[]>;
  /** Index of the shown child per parent id. */
  active: Record<string, number>;
}

const ROOT = '';

function treeFrom(messages: readonly AiMessage[]): Tree {
  const tree: Tree = { nodes: {}, parent: {}, children: {}, active: {} };
  let prev = ROOT;
  for (const m of messages) {
    tree.nodes[m.id] = m;
    tree.parent[m.id] = prev || null;
    (tree.children[prev] ??= []).push(m.id);
    tree.active[prev] = 0;
    prev = m.id;
  }
  return tree;
}

function visiblePath(tree: Tree): AiMessage[] {
  const out: AiMessage[] = [];
  let at = ROOT;
  for (;;) {
    const kids = tree.children[at];
    if (!kids?.length) break;
    const id = kids[Math.min(tree.active[at] ?? 0, kids.length - 1)];
    const node = id === undefined ? undefined : tree.nodes[id];
    if (id === undefined || !node) break;
    out.push(
      kids.length > 1 ? { ...node, branch: { index: kids.indexOf(id), count: kids.length } } : node,
    );
    at = id;
  }
  return out;
}

function addChild(tree: Tree, parentId: string, message: AiMessage): Tree {
  const kids = [...(tree.children[parentId] ?? []), message.id];
  return {
    nodes: { ...tree.nodes, [message.id]: message },
    parent: { ...tree.parent, [message.id]: parentId || null },
    children: { ...tree.children, [parentId]: kids },
    active: { ...tree.active, [parentId]: kids.length - 1 },
  };
}

function finishParts(parts: AiPart[]): AiPart[] {
  return parts.map((p) =>
    (p.type === 'text' || p.type === 'reasoning') && p.state === 'streaming'
      ? { ...p, state: 'done' }
      : p.type === 'tool-call' && p.state === 'running'
        ? { ...p, state: 'cancelled' }
        : p,
  );
}

/** Applies one stream event to the parts of the message being written. */
export function applyStreamEvent(message: AiMessage, event: AiStreamEvent): AiMessage {
  const parts = [...message.parts];
  const last = parts[parts.length - 1];
  switch (event.type) {
    case 'agent':
      return { ...message, agent: event.agent };
    case 'text-delta':
      if (last?.type === 'text' && last.state === 'streaming')
        parts[parts.length - 1] = { ...last, text: last.text + event.text };
      else {
        if (last?.type === 'reasoning' && last.state === 'streaming')
          parts[parts.length - 1] = { ...last, state: 'done' };
        parts.push({ type: 'text', text: event.text, state: 'streaming' });
      }
      return { ...message, parts };
    case 'reasoning-delta':
      if (last?.type === 'reasoning' && last.state === 'streaming')
        parts[parts.length - 1] = { ...last, text: last.text + event.text };
      else parts.push({ type: 'reasoning', text: event.text, state: 'streaming' });
      return { ...message, parts };
    case 'reasoning-end': {
      let i = parts.length - 1;
      while (i >= 0 && parts[i]?.type !== 'reasoning') i--;
      const r = parts[i];
      if (r?.type === 'reasoning')
        parts[i] = {
          ...r,
          state: 'done',
          ...(event.durationMs !== undefined ? { durationMs: event.durationMs } : {}),
          ...(event.redacted ? { redacted: true } : {}),
        };
      return { ...message, parts };
    }
    case 'part':
      if (last?.type === 'text' && last.state === 'streaming')
        parts[parts.length - 1] = { ...last, state: 'done' };
      parts.push(event.part);
      return { ...message, parts };
    case 'tool-update':
      return {
        ...message,
        parts: parts.map((p) =>
          p.type === 'tool-call' && p.id === event.id
            ? ({ ...p, ...event.patch } as AiToolCallPart)
            : p,
        ),
      };
    case 'usage':
      return { ...message, usage: event.usage, ...(event.model ? { model: event.model } : {}) };
    case 'error':
      return {
        ...message,
        parts: [
          ...finishParts(parts),
          {
            type: 'error',
            code: event.code ?? 'unknown',
            message: event.message,
            retryable: event.retryable ?? true,
          },
        ],
      };
    case 'done':
      return { ...message, parts: finishParts(parts) };
  }
}

let idCounter = 0;
const defaultId = () => {
  try {
    return crypto.randomUUID();
  } catch {
    return `m-${Date.now().toString(36)}-${(++idCounter).toString(36)}`;
  }
};

/** Schedules at most one flush per animation frame (a timer where there is none). */
function frameScheduler() {
  let handle: number | ReturnType<typeof setTimeout> | null = null;
  return {
    schedule(fn: () => void) {
      if (handle !== null) return;
      const run = () => {
        handle = null;
        fn();
      };
      handle =
        typeof requestAnimationFrame === 'function'
          ? requestAnimationFrame(run)
          : setTimeout(run, 16);
    },
    cancel() {
      if (handle === null) return;
      if (typeof cancelAnimationFrame === 'function' && typeof handle === 'number')
        cancelAnimationFrame(handle);
      else clearTimeout(handle as ReturnType<typeof setTimeout>);
      handle = null;
    },
  };
}

/**
 * Headless state for an AI conversation: sends prompts through a transport,
 * streams the reply into the message model, and handles stop, regenerate,
 * edit-and-resend (as branches), tool approval and feedback. Token updates are
 * batched to at most one render per animation frame. It knows no model
 * vendor; the transport decides where requests go.
 */
export function useAiConversation(options: UseAiConversationOptions): UseAiConversationResult {
  const { transport, generateId = defaultId } = options;
  const [tree, setTree] = useState<Tree>(() => treeFrom(options.initialMessages ?? []));
  const [status, setStatus] = useState<AiConversationStatus>('idle');
  const [error, setError] = useState<AiConversationError | null>(null);
  const [conversationId, setConversationId] = useState(options.conversationId);
  const [agent, setAgent] = useState<AiAgentRef | undefined>();

  const treeRef = useRef(tree);
  treeRef.current = tree;
  const opts = useRef(options);
  opts.current = options;
  const conversationRef = useRef(conversationId);
  conversationRef.current = conversationId;
  const abort = useRef<AbortController | null>(null);
  const scheduler = useRef(frameScheduler());
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    const pending = scheduler.current;
    return () => {
      mounted.current = false;
      abort.current?.abort();
      pending.cancel();
    };
  }, []);

  /** Streams events into the assistant message `targetId`. */
  const run = useCallback(
    async (targetId: string, events: AsyncIterable<AiStreamEvent>, controller: AbortController) => {
      const start = treeRef.current.nodes[targetId];
      if (!start) return;
      let draft = start;
      const flush = () => {
        if (!mounted.current) return;
        const snapshot = draft;
        setTree((t) => ({ ...t, nodes: { ...t.nodes, [targetId]: snapshot } }));
      };
      let failed: AiConversationError | null = null;
      let finished = false;
      try {
        for await (const event of events) {
          if (controller.signal.aborted) break;
          if (event.type === 'agent') setAgent(event.agent);
          if (event.type === 'done' && event.conversationId)
            setConversationId(event.conversationId);
          if (event.type === 'error')
            failed = {
              code: event.code ?? 'unknown',
              message: event.message,
              retryable: event.retryable ?? true,
            };
          draft = applyStreamEvent(draft, event);
          if (event.type === 'done') finished = true;
          setStatus((s) => (s === 'submitted' ? 'streaming' : s));
          scheduler.current.schedule(flush);
        }
      } catch (e) {
        if (!controller.signal.aborted) {
          failed = {
            code: 'network',
            message: e instanceof Error && e.message ? e.message : 'The connection was lost.',
            retryable: true,
          };
          draft = applyStreamEvent(draft, { type: 'error', ...failed });
        }
      }
      scheduler.current.cancel();
      if (!mounted.current) return;
      if (controller.signal.aborted) {
        draft = { ...draft, parts: finishParts(draft.parts) };
        flush();
        setStatus('stopped');
        return;
      }
      draft = { ...draft, parts: finishParts(draft.parts) };
      flush();
      if (abort.current === controller) abort.current = null;
      if (failed) {
        setError(failed);
        setStatus('error');
        opts.current.onError?.(failed);
        return;
      }
      const waiting = draft.parts.some(
        (p) => p.type === 'tool-call' && p.state === 'pending-approval',
      );
      setStatus(waiting ? 'awaiting-approval' : 'idle');
      if (finished && !waiting) opts.current.onFinish?.(draft);
    },
    [],
  );

  /** Adds an empty assistant reply under `parentId` and streams into it. */
  const ask = useCallback(
    (parentId: string, trigger: AiTrigger, base: Tree, body?: Record<string, unknown>) => {
      abort.current?.abort();
      const controller = new AbortController();
      abort.current = controller;
      const reply: AiMessage = {
        id: generateId(),
        role: 'assistant',
        parts: [],
        createdAt: new Date().toISOString(),
      };
      const next = addChild(base, parentId, reply);
      treeRef.current = next;
      setTree(next);
      setError(null);
      setStatus('submitted');
      const history = visiblePath(next).filter((m) => m.id !== reply.id);
      const merged = { ...opts.current.body, ...body };
      const events = transport.stream({
        messages: history,
        trigger,
        signal: controller.signal,
        ...(conversationRef.current ? { conversationId: conversationRef.current } : {}),
        ...(Object.keys(merged).length ? { body: merged } : {}),
      });
      void run(reply.id, events, controller);
    },
    [generateId, run, transport],
  );

  const userMessage = useCallback(
    (text: string, sendOptions?: SendOptions): AiMessage => ({
      id: generateId(),
      role: 'user',
      parts: [{ type: 'text', text, state: 'done' }, ...(sendOptions?.files ?? [])],
      createdAt: new Date().toISOString(),
    }),
    [generateId],
  );

  const send = useCallback(
    (text: string, sendOptions?: SendOptions) => {
      if (!text.trim() && !sendOptions?.files?.length) return;
      const base = treeRef.current;
      const path = visiblePath(base);
      const parentId = path[path.length - 1]?.id ?? ROOT;
      const user = userMessage(text, sendOptions);
      ask(user.id, 'send', addChild(base, parentId, user), sendOptions?.body);
    },
    [ask, userMessage],
  );

  const stop = useCallback(() => {
    abort.current?.abort();
    abort.current = null;
  }, []);

  const regenerate = useCallback(
    (messageId?: string) => {
      const base = treeRef.current;
      const path = visiblePath(base);
      const target = messageId
        ? base.nodes[messageId]
        : [...path].reverse().find((m) => m.role === 'assistant');
      if (!target || target.role !== 'assistant') return;
      const parentId = base.parent[target.id] ?? ROOT;
      ask(parentId, 'regenerate', base);
    },
    [ask],
  );

  const editAndResend = useCallback(
    (messageId: string, text: string, sendOptions?: SendOptions) => {
      const base = treeRef.current;
      const original = base.nodes[messageId];
      if (!original || original.role !== 'user') return;
      const parentId = base.parent[messageId] ?? ROOT;
      const files = original.parts.filter(
        (p): p is Extract<AiPart, { type: 'file' }> => p.type === 'file',
      );
      const user = userMessage(text, { files, ...sendOptions });
      ask(user.id, 'edit', addChild(base, parentId, user), sendOptions?.body);
    },
    [ask, userMessage],
  );

  const respond = useCallback(
    (toolCallId: string, approved: boolean, input?: unknown, reason?: string) => {
      const base = treeRef.current;
      const owner = visiblePath(base).find((m) =>
        m.parts.some((p) => p.type === 'tool-call' && p.id === toolCallId),
      );
      const stored = owner ? base.nodes[owner.id] : undefined;
      if (!owner || !stored) return;
      const patched: AiMessage = {
        ...stored,
        parts: stored.parts.map((p) =>
          p.type === 'tool-call' && p.id === toolCallId
            ? {
                ...p,
                state: approved ? 'running' : 'denied',
                ...(approved && input !== undefined ? { input } : {}),
                ...(approved ? { startedAt: new Date().toISOString() } : {}),
              }
            : p,
        ),
      };
      const next = { ...base, nodes: { ...base.nodes, [owner.id]: patched } };
      treeRef.current = next;
      setTree(next);
      const stillWaiting = patched.parts.some(
        (p) => p.type === 'tool-call' && p.state === 'pending-approval',
      );
      const response = {
        toolCallId,
        approved,
        ...(input !== undefined ? { input } : {}),
        ...(reason !== undefined ? { reason } : {}),
      };
      opts.current.onToolResponse?.(response);
      if (!transport.respondToTool || stillWaiting) {
        if (!stillWaiting) setStatus('idle');
        return;
      }
      abort.current?.abort();
      const controller = new AbortController();
      abort.current = controller;
      setStatus('streaming');
      const events = transport.respondToTool({
        ...response,
        messages: visiblePath(next),
        signal: controller.signal,
        ...(conversationRef.current ? { conversationId: conversationRef.current } : {}),
      });
      void run(owner.id, events, controller);
    },
    [run, transport],
  );

  const approveTool = useCallback(
    (id: string, input?: unknown) => respond(id, true, input),
    [respond],
  );
  const denyTool = useCallback(
    (id: string, reason?: string) => respond(id, false, undefined, reason),
    [respond],
  );

  const branchTo = useCallback((messageId: string, index: number) => {
    setTree((t) => {
      const parentId = t.parent[messageId] ?? ROOT;
      const kids = t.children[parentId] ?? [];
      if (index < 0 || index >= kids.length) return t;
      return { ...t, active: { ...t.active, [parentId]: index } };
    });
  }, []);

  const setFeedback = useCallback((messageId: string, feedback: 'up' | 'down' | undefined) => {
    setTree((t) => {
      const node = t.nodes[messageId];
      if (!node) return t;
      const { feedback: _old, ...rest } = node;
      return { ...t, nodes: { ...t.nodes, [messageId]: feedback ? { ...rest, feedback } : rest } };
    });
  }, []);

  const reset = useCallback((messages: readonly AiMessage[] = []) => {
    abort.current?.abort();
    abort.current = null;
    const next = treeFrom(messages);
    treeRef.current = next;
    setTree(next);
    setStatus('idle');
    setError(null);
  }, []);

  const messages = useMemo(() => visiblePath(tree), [tree]);

  return {
    messages,
    status,
    error,
    conversationId,
    agent,
    busy: status === 'submitted' || status === 'streaming',
    send,
    stop,
    regenerate,
    editAndResend,
    approveTool,
    denyTool,
    branchTo,
    setFeedback,
    reset,
  };
}
