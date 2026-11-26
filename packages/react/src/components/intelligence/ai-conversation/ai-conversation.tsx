'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { AiAgentRef, AiFilePart, AiMessage, AiToolCallPart } from './ai-conversation.types.js';
import type {
  AiMemoryEvent,
  AiStreamEvent,
  AiTransport,
  AiTrigger,
} from './ai-conversation.transport.js';
import { browserNetworkSource, type AiNetworkSource } from './ai-conversation.network.js';
import {
  conversationMessages,
  isRetryableErrorCode,
  type AiConversationMessages,
} from './ai-conversation.errors.js';
import { applyStreamEvent, finishParts } from './ai-conversation.reduce.js';
import {
  ROOT,
  addChild,
  lastVisibleId,
  replaceNode,
  treeFrom,
  visiblePath,
  type AiBranchCache,
  type AiMessageTree,
} from './ai-conversation.tree.js';
import {
  isFilePart,
  resolveAttachments,
  type AiInlineFilesOptions,
  type AiUploadFile,
  type AiUploadState,
} from './ai-conversation.files.js';

export * from './ai-conversation.types.js';
export * from './ai-conversation.transport.js';
export * from './ai-conversation.sse.js';
export * from './ai-conversation.providers.js';
export * from './ai-conversation.errors.js';
export * from './ai-conversation.reduce.js';
export {
  isFilePart,
  type AiUploadContext,
  type AiUploadFile,
  type AiInlineFilesOptions,
  type AiUploadState,
} from './ai-conversation.files.js';
export * from './ai-conversation.requests.js';
export * from './ai-conversation.ui-stream.js';
export * from './ai-conversation.responses.js';
export * from './ai-conversation.ag-ui.js';
export * from './ai-conversation.testing.js';

/**
 * - `idle`: nothing running.
 * - `submitted`: sent (or attachments uploading), waiting for the first event.
 * - `streaming`: events are arriving.
 * - `awaiting-approval`: a tool call waits for the person to approve or deny it.
 * - `awaiting-tool-result`: the model called a tool the client runs; send the
 *   result with `submitToolResult` (or pass `onToolCall`).
 * - `error`: the last turn failed; `error` holds why.
 * - `stopped`: the person pressed Stop.
 */
export type AiConversationStatus =
  | 'idle'
  | 'submitted'
  | 'streaming'
  | 'awaiting-approval'
  | 'awaiting-tool-result'
  | 'error'
  | 'stopped';

export interface AiConversationError {
  code: string;
  message: string;
  retryable: boolean;
  /** Milliseconds until a retry is allowed, when the service said (`Retry-After`). */
  retryAfterMs?: number;
  /**
   * The connection dropped mid-reply and the run can be picked up with
   * `resumeRun()` rather than asked again: the transport supports `resume`
   * and the reply had a `run` id.
   */
  resumable?: boolean;
}

/** A message written while offline, waiting to be sent. */
export interface AiQueuedMessage {
  /** Id of the user message it becomes; pass it to `cancelQueued`. */
  id: string;
  text: string;
  /** Attached files, uploaded when the message is sent. */
  files: readonly (AiFilePart | File)[];
  /** Extra request fields given to `send`. */
  body?: Record<string, unknown>;
  /** ISO 8601 time it was written. */
  createdAt: string;
}

export interface SendOptions {
  /**
   * Attached files, as `file` parts on the user message. Browser `File`s
   * (what AiPromptInput gives) are uploaded with `uploadFile` first, or
   * inlined when `inlineFiles` allows; otherwise sending fails with an
   * `attachments-unsupported` error and nothing is sent.
   */
  files?: readonly (AiFilePart | File)[];
  /** Extra request fields for the transport (`AiTransportRequest.body`). */
  body?: Record<string, unknown>;
}

/** What `onToolCall` receives besides the call. */
export interface AiToolCallContext {
  /** Aborted when the person presses Stop, sends again or the conversation unmounts. */
  signal: AbortSignal;
}

/** A client-side tool result. `error` marks the call `failed`. */
export type AiToolResult =
  { output: unknown; error?: undefined } | { error: string; output?: unknown };

/** Decides whether a tool call needs a person's approval; see `toolPolicy`. */
export type AiToolPolicy = (call: AiToolCallPart) => 'ask' | 'allow' | 'deny';

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
  /**
   * Runs client-side tools: called for every call the model leaves in
   * `awaiting-result`. Return the output (or throw to mark the call failed);
   * once every call has a result, the reply continues. Without it, call
   * `submitToolResult` yourself.
   */
  onToolCall?: (call: AiToolCallPart, context: AiToolCallContext) => unknown;
  /**
   * Approves or denies tool calls before anyone is asked, e.g. for tools the
   * person chose to "always allow". Return `ask` to show the approval card.
   */
  toolPolicy?: AiToolPolicy;
  /**
   * Uploads attached `File`s and returns the part that refers to each. Report
   * progress through `context.onProgress`; it shows in `uploads`.
   */
  uploadFile?: AiUploadFile;
  /**
   * Opt-in: without `uploadFile`, send small files inline as `data:` URLs.
   * Off by default, because attached files are otherwise refused, not dropped.
   */
  inlineFiles?: AiInlineFilesOptions;
  /** Called as each attachment uploads. */
  onUploadProgress?: (file: File, fraction: number) => void;
  /** Text of the errors the hook raises itself (connection lost, upload failed…). */
  messages?: Partial<AiConversationMessages>;
  /**
   * Called when the reply uses, saves or forgets a memory (a `memory` event,
   * or a `memory` part), e.g. to update a memory list with
   * `applyAiMemoryEvent`.
   */
  onMemoryEvent?: (memory: AiMemoryEvent, message: AiMessage) => void;
  /**
   * Where "online" comes from. Default: the browser (`navigator.onLine` and
   * the `online` / `offline` events). Pass your own for tests, a native
   * shell or a health check.
   */
  network?: AiNetworkSource;
  /**
   * While offline, `send` keeps the message in `queued` (shown at the end of
   * `messages` with `queued: true`) and sends it, in order, once the
   * connection is back. Default true. With false, sending offline is tried
   * at once and fails with a `network` error.
   */
  offlineQueue?: boolean;
  /**
   * Milliseconds without any event from a running reply before `slow`
   * becomes true ("taking longer than usual"). Default 10000; 0 turns it off.
   */
  slowAfterMs?: number;
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
  /** False while a tool call waits for approval: `send` is ignored until it is approved or denied. */
  canSend: boolean;
  /** Attachments being uploaded for the message being sent, in AiAttachmentTray's shape. */
  uploads: readonly AiUploadState[];
  /** True while the device is offline (see `network`). Messages sent now are queued. */
  offline: boolean;
  /**
   * True when a running reply has sent nothing for `slowAfterMs`: show a
   * "taking longer than usual" hint. It clears with the next event.
   */
  slow: boolean;
  /** Messages written while offline, oldest first, waiting for the connection. */
  queued: readonly AiQueuedMessage[];
  /** Removes a queued message before it is sent. */
  cancelQueued: (id: string) => void;
  send: (text: string, options?: SendOptions) => void;
  /**
   * `send` that reports the outcome: resolves once the message is on its way,
   * rejects when it was not sent (an approval is pending, an attachment could
   * not be uploaded). AiPromptInput puts the text and files back on rejection.
   */
  submit: (text: string, options?: SendOptions) => Promise<void>;
  /** Aborts the reply in progress; what arrived so far stays. */
  stop: () => void;
  /** Asks again for an assistant message (default: the last one), as a new branch. `options.body` is sent with the request. */
  regenerate: (messageId?: string, options?: Pick<SendOptions, 'body'>) => void;
  /** Asks again for a failed assistant message in place, without adding a branch. `options.body` is sent with the request. */
  retry: (messageId?: string, options?: Pick<SendOptions, 'body'>) => void;
  /** Replaces a user message with new text as a new branch and asks again. */
  editAndResend: (messageId: string, text: string, options?: SendOptions) => void;
  approveTool: (toolCallId: string, input?: unknown) => void;
  denyTool: (toolCallId: string, reason?: string) => void;
  /**
   * Sends a client-side tool's result. Once every call of the reply has one,
   * the reply continues (trigger `tool-result`).
   */
  submitToolResult: (toolCallId: string, result: AiToolResult) => void;
  /**
   * Reconnects to the run behind a reply (default: the last assistant message
   * with a `runId`), when the transport supports `resume`. The run is replayed
   * into the message from its start.
   */
  resumeRun: (messageId?: string) => void;
  /** Shows another branch of a message: `index` among its alternatives (0-based). */
  branchTo: (messageId: string, index: number) => void;
  /** Sets the feedback shown on a message (send it to your feedback API yourself). */
  setFeedback: (messageId: string, feedback: 'up' | 'down' | undefined) => void;
  /** Replaces the whole conversation, e.g. to load another one. */
  reset: (messages?: readonly AiMessage[]) => void;
}

let idCounter = 0;
const defaultId = () => {
  try {
    return crypto.randomUUID();
  } catch {
    return `m-${Date.now().toString(36)}-${(++idCounter).toString(36)}`;
  }
};

interface FrameScheduler {
  schedule(fn: () => void): void;
  cancel(): void;
}

/** Schedules at most one flush per animation frame (a timer where there is none). */
function frameScheduler(): FrameScheduler {
  let handle: number | ReturnType<typeof setTimeout> | null = null;
  let usesFrame = false;
  return {
    schedule(fn) {
      if (handle !== null) return;
      const run = () => {
        handle = null;
        fn();
      };
      usesFrame = typeof requestAnimationFrame === 'function';
      handle = usesFrame ? requestAnimationFrame(run) : setTimeout(run, 16);
    },
    cancel() {
      if (handle === null) return;
      if (usesFrame && typeof cancelAnimationFrame === 'function')
        cancelAnimationFrame(handle as number);
      else clearTimeout(handle as ReturnType<typeof setTimeout>);
      handle = null;
    },
  };
}

/**
 * One run: a reply streaming into one message (or attachments uploading
 * before it). Each run owns its abort controller and frame scheduler; a run
 * that is no longer `current` never touches status, the tree or the screen.
 */
interface RunHandle {
  controller: AbortController;
  scheduler: FrameScheduler;
  /** The assistant message being written; empty while uploading. */
  targetId: string;
  draft: AiMessage | undefined;
}

type Live = { id: string; message: AiMessage } | null;

const isTool =
  (id: string) =>
  (p: AiMessage['parts'][number]): p is AiToolCallPart =>
    p.type === 'tool-call' && p.id === id;

const lastAssistant = (path: readonly AiMessage[]) =>
  [...path].reverse().find((m) => m.role === 'assistant');

const asError = (e: unknown) => (e instanceof Error ? e : new Error('rejected'));

/** A queued message as the user message it will become, so the thread can show it. */
function queuedAsMessage(item: AiQueuedMessage): AiMessage {
  return {
    id: item.id,
    role: 'user',
    parts: [
      ...(item.text ? [{ type: 'text' as const, text: item.text, state: 'done' as const }] : []),
      ...item.files.map((f): AiFilePart =>
        isFilePart(f)
          ? f
          : {
              type: 'file',
              name: f.name,
              mediaType: f.type || 'application/octet-stream',
              size: f.size,
            },
      ),
    ],
    createdAt: item.createdAt,
    queued: true,
  };
}

/**
 * Headless state for an AI conversation: sends prompts through a transport,
 * streams the reply into the message model, and handles stop, regenerate,
 * retry, edit-and-resend (as branches), tool approval, client-side tool
 * results, attachments and feedback. Token updates are batched to at most one
 * render per animation frame. It knows no model vendor; the transport decides
 * where requests go.
 */
export function useAiConversation(options: UseAiConversationOptions): UseAiConversationResult {
  const { transport, generateId = defaultId } = options;
  const [tree, setTreeState] = useState<AiMessageTree>(() =>
    treeFrom(options.initialMessages ?? []),
  );
  const [live, setLive] = useState<Live>(null);
  const [status, setStatusState] = useState<AiConversationStatus>('idle');
  const [error, setError] = useState<AiConversationError | null>(null);
  const [conversationId, setConversationId] = useState(options.conversationId);
  const [agent, setAgent] = useState<AiAgentRef | undefined>();
  const [uploads, setUploads] = useState<readonly AiUploadState[]>([]);
  const [queue, setQueueState] = useState<readonly AiQueuedMessage[]>([]);
  const [online, setOnline] = useState(true);
  const [slow, setSlowState] = useState(false);

  const treeRef = useRef(tree);
  const statusRef = useRef(status);
  const opts = useRef(options);
  opts.current = options;
  const transportRef = useRef(transport);
  transportRef.current = transport;
  const conversationRef = useRef(conversationId);
  conversationRef.current = conversationId;
  const current = useRef<RunHandle | null>(null);
  const toolRuns = useRef<AbortController | null>(null);
  const mounted = useRef(true);
  const branchCache = useRef<AiBranchCache>(new WeakMap());

  const setTree = useCallback((next: AiMessageTree) => {
    treeRef.current = next;
    setTreeState(next);
  }, []);
  const setStatus = useCallback((next: AiConversationStatus) => {
    statusRef.current = next;
    setStatusState(next);
  }, []);
  const queueRef = useRef(queue);
  const setQueue = useCallback((next: readonly AiQueuedMessage[]) => {
    queueRef.current = next;
    setQueueState(next);
  }, []);

  // ------------------------------------------------------------ network
  const networkOption = options.network;
  const network = useMemo(() => networkOption ?? browserNetworkSource(), [networkOption]);
  const networkRef = useRef(network);
  networkRef.current = network;
  /** A queued message failed to send: wait for the person (or a reconnect) before the next. */
  const flushHeld = useRef(false);
  const flushing = useRef(false);
  useEffect(() => {
    setOnline(network.isOnline());
    return network.subscribe((next) => {
      if (next) flushHeld.current = false;
      setOnline(next);
    });
  }, [network]);

  // ------------------------------------------------------------ slow replies
  const slowRef = useRef(false);
  const slowTimer = useRef<ReturnType<typeof setTimeout>>(undefined);
  const setSlow = useCallback((next: boolean) => {
    if (slowRef.current === next) return;
    slowRef.current = next;
    setSlowState(next);
  }, []);
  const clearSlow = useCallback(() => {
    clearTimeout(slowTimer.current);
    slowTimer.current = undefined;
    setSlow(false);
  }, [setSlow]);
  const strings = () => conversationMessages(opts.current.messages);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      const run = current.current;
      current.current = null;
      run?.controller.abort();
      run?.scheduler.cancel();
      toolRuns.current?.abort();
      clearTimeout(slowTimer.current);
    };
  }, []);

  /** The message holding a tool call: the reply still streaming, or one on the visible path. */
  const findToolOwner = useCallback((toolCallId: string): AiMessage | undefined => {
    const draft = current.current?.draft;
    if (draft?.parts.some(isTool(toolCallId))) return draft;
    return visiblePath(treeRef.current).find((m) => m.parts.some(isTool(toolCallId)));
  }, []);

  /**
   * Ends the current run, if any, keeping what arrived: its draft is closed
   * and written to the tree. The run itself is told it is no longer current,
   * so nothing it does later reaches the state.
   */
  const supersede = useCallback(() => {
    toolRuns.current?.abort();
    toolRuns.current = null;
    const run = current.current;
    if (!run) return false;
    current.current = null;
    run.controller.abort();
    run.scheduler.cancel();
    if (run.draft) {
      const closed = { ...run.draft, parts: finishParts(run.draft.parts, 'aborted') };
      setTree(replaceNode(treeRef.current, closed));
    }
    setLive(null);
    setUploads([]);
    clearSlow();
    return true;
  }, [clearSlow, setTree]);

  const fail = useCallback(
    (failure: AiConversationError) => {
      setError(failure);
      setStatus('error');
      opts.current.onError?.(failure);
    },
    [setStatus],
  );

  /** Decides what a finished reply waits for, and runs client tools. Declared below. */
  const settleRef = useRef<(messageId: string, finished: boolean) => void>(() => {});

  /** Streams events into the assistant message `handle.targetId`. */
  const drive = useCallback(
    async (handle: RunHandle, open: (signal: AbortSignal) => AsyncIterable<AiStreamEvent>) => {
      const { controller, scheduler, targetId } = handle;
      const isCurrent = () => mounted.current && current.current === handle;
      const flush = () => {
        if (isCurrent() && handle.draft) setLive({ id: targetId, message: handle.draft });
      };
      let failed: AiConversationError | null = null;
      let finished = false;
      // "Taking longer than usual": armed at the start and again after every event.
      const slowAfter = opts.current.slowAfterMs ?? 10000;
      const watch = () => {
        clearTimeout(slowTimer.current);
        setSlow(false);
        if (slowAfter > 0)
          slowTimer.current = setTimeout(() => {
            if (isCurrent()) setSlow(true);
          }, slowAfter);
      };
      /** A dropped connection can be resumed when the service gave the run an id. */
      const resumable = () => Boolean(transportRef.current.resume && handle.draft?.runId);
      watch();
      try {
        const events = open(controller.signal);
        for await (const event of events) {
          if (!isCurrent() || controller.signal.aborted) break;
          watch();
          if (event.type === 'agent') setAgent(event.agent);
          if (event.type === 'done' && event.conversationId)
            setConversationId(event.conversationId);
          if (event.type === 'error') {
            const code = event.code ?? 'unknown';
            failed = {
              code,
              message: event.message,
              retryable: event.retryable ?? isRetryableErrorCode(code),
              ...(event.retryAfterMs !== undefined ? { retryAfterMs: event.retryAfterMs } : {}),
              ...(event.resumable || ((code === 'network' || code === 'timeout') && resumable())
                ? { resumable: true }
                : {}),
            };
          }
          if (handle.draft)
            handle.draft = applyStreamEvent(
              handle.draft,
              event.type === 'error' && failed?.resumable ? { ...event, resumable: true } : event,
            );
          if (handle.draft && opts.current.onMemoryEvent) {
            if (event.type === 'memory') opts.current.onMemoryEvent(event.memory, handle.draft);
            else if (event.type === 'part' && event.part.type === 'memory') {
              const { type: _type, ...memory } = event.part;
              opts.current.onMemoryEvent(memory, handle.draft);
            }
          }
          if (statusRef.current === 'submitted') setStatus('streaming');
          scheduler.schedule(flush);
          // Terminal events end the reply even if the transport keeps the stream open.
          if (event.type === 'done') {
            finished = true;
            break;
          }
          if (event.type === 'error') break;
        }
      } catch {
        if (isCurrent() && !controller.signal.aborted) {
          failed = {
            code: 'network',
            message: strings().connectionLost,
            retryable: true,
            ...(resumable() ? { resumable: true } : {}),
          };
          if (handle.draft)
            handle.draft = applyStreamEvent(handle.draft, { type: 'error', ...failed });
        }
      }
      scheduler.cancel();
      // Superseded, stopped or unmounted: whoever ended the run owns the state now.
      if (!isCurrent()) return;
      clearSlow();
      current.current = null;
      if (controller.signal.aborted) return;
      const draft = handle.draft;
      if (!draft) return;
      const closed: AiMessage = {
        ...draft,
        parts: finishParts(draft.parts, failed ? 'error' : 'done', draft.finishReason),
      };
      setTree(replaceNode(treeRef.current, closed));
      setLive(null);
      if (failed) {
        fail(failed);
        return;
      }
      settleRef.current(targetId, finished);
    },
    [clearSlow, fail, setSlow, setStatus, setTree],
  );

  /** Starts a run into an existing assistant message. */
  const runInto = useCallback(
    (
      targetId: string,
      open: (signal: AbortSignal) => AsyncIterable<AiStreamEvent>,
      nextStatus: AiConversationStatus = 'streaming',
    ) => {
      const draft = treeRef.current.nodes[targetId];
      const handle: RunHandle = {
        controller: new AbortController(),
        scheduler: frameScheduler(),
        targetId,
        draft,
      };
      current.current = handle;
      setError(null);
      setStatus(nextStatus);
      void drive(handle, open);
    },
    [drive, setStatus],
  );

  /** Adds an empty assistant reply under `parentId` and streams into it. */
  const ask = useCallback(
    (parentId: string, trigger: AiTrigger, base: AiMessageTree, body?: Record<string, unknown>) => {
      const reply: AiMessage = {
        id: generateId(),
        role: 'assistant',
        parts: [],
        createdAt: new Date().toISOString(),
      };
      const next = addChild(base, parentId, reply);
      setTree(next);
      const history = visiblePath(next).filter((m) => m.id !== reply.id);
      const merged = { ...opts.current.body, ...body };
      runInto(
        reply.id,
        (signal) =>
          transportRef.current.stream({
            messages: history,
            trigger,
            signal,
            ...(conversationRef.current ? { conversationId: conversationRef.current } : {}),
            ...(Object.keys(merged).length ? { body: merged } : {}),
          }),
        'submitted',
      );
    },
    [generateId, runInto, setTree],
  );

  /** Marks calls nobody will answer as cancelled, so the next request's history is consistent. */
  const dropAwaitingResults = useCallback(() => {
    const base = treeRef.current;
    let next = base;
    for (const m of visiblePath(base)) {
      if (!m.parts.some((p) => p.type === 'tool-call' && p.state === 'awaiting-result')) continue;
      const stored = base.nodes[m.id];
      if (!stored) continue;
      next = replaceNode(next, {
        ...stored,
        parts: stored.parts.map((p) =>
          p.type === 'tool-call' && p.state === 'awaiting-result'
            ? { ...p, state: 'cancelled' as const }
            : p,
        ),
      });
    }
    if (next !== base) setTree(next);
  }, [setTree]);

  const userMessage = useCallback(
    (text: string, files: readonly AiFilePart[], id?: string): AiMessage => ({
      id: id ?? generateId(),
      role: 'user',
      parts: [...(text ? [{ type: 'text' as const, text, state: 'done' as const }] : []), ...files],
      createdAt: new Date().toISOString(),
    }),
    [generateId],
  );

  /**
   * Resolves attachments, then calls `then` with the parts. Uploads run as a
   * run of their own, so Stop and a new send cancel them.
   */
  const withAttachments = useCallback(
    (files: readonly (AiFilePart | File)[], then: (parts: AiFilePart[]) => void): Promise<void> => {
      if (files.every((f) => isFilePart(f))) {
        then(files as AiFilePart[]);
        return Promise.resolve();
      }
      const handle: RunHandle = {
        controller: new AbortController(),
        scheduler: frameScheduler(),
        targetId: '',
        draft: undefined,
      };
      current.current = handle;
      setError(null);
      setStatus('submitted');
      const raw = files
        .map((f, i) => ({ f, i }))
        .filter((x): x is { f: File; i: number } => !isFilePart(x.f));
      const ids = new Map(raw.map(({ i }) => [i, generateId()]));
      setUploads(
        raw.map(({ f, i }) => ({
          id: ids.get(i) ?? String(i),
          name: f.name,
          size: f.size,
          type: f.type,
          progress: 0,
        })),
      );
      const m = strings();
      return resolveAttachments(files, {
        uploadFile: opts.current.uploadFile,
        inlineFiles: opts.current.inlineFiles,
        signal: handle.controller.signal,
        messages: m,
        onProgress: (index, fraction) => {
          const file = files[index];
          if (file && !isFilePart(file)) opts.current.onUploadProgress?.(file, fraction);
          if (current.current !== handle || !mounted.current) return;
          const id = ids.get(index);
          setUploads((list) => list.map((u) => (u.id === id ? { ...u, progress: fraction } : u)));
        },
      }).then((result) => {
        if (current.current !== handle || !mounted.current) {
          throw new Error('cancelled');
        }
        current.current = null;
        setUploads([]);
        if (!result.ok) {
          if (result.aborted) {
            setStatus('stopped');
            throw new Error('cancelled');
          }
          fail({ code: result.code, message: result.message, retryable: false });
          throw new Error(result.message);
        }
        then(result.parts);
      });
    },
    [fail, generateId, setStatus],
  );

  /** Sends now, whatever the network says. `id` keeps a queued message's id. */
  const submitNow = useCallback(
    (text: string, sendOptions?: SendOptions, id?: string): Promise<void> => {
      const files = sendOptions?.files ?? [];
      supersede();
      dropAwaitingResults();
      try {
        return withAttachments(files, (parts) => {
          const base = treeRef.current;
          const user = userMessage(text, parts, id);
          ask(user.id, 'send', addChild(base, lastVisibleId(base), user), sendOptions?.body);
        });
      } catch (e) {
        return Promise.reject(asError(e));
      }
    },
    [ask, dropAwaitingResults, supersede, userMessage, withAttachments],
  );

  const submit = useCallback(
    (text: string, sendOptions?: SendOptions): Promise<void> => {
      const files = sendOptions?.files ?? [];
      if (!text.trim() && !files.length) return Promise.resolve();
      if (statusRef.current === 'awaiting-approval')
        return Promise.reject(new Error('A tool call is waiting for approval.'));
      // Offline, or behind messages that still wait: keep it, in order, for later.
      const queueing = opts.current.offlineQueue !== false;
      if (queueing && (!networkRef.current.isOnline() || queueRef.current.length > 0)) {
        setQueue([
          ...queueRef.current,
          {
            id: generateId(),
            text,
            files,
            ...(sendOptions?.body ? { body: sendOptions.body } : {}),
            createdAt: new Date().toISOString(),
          },
        ]);
        return Promise.resolve();
      }
      flushHeld.current = false;
      return submitNow(text, sendOptions);
    },
    [generateId, setQueue, submitNow],
  );

  const cancelQueued = useCallback(
    (id: string) => {
      const next = queueRef.current.filter((q) => q.id !== id);
      if (next.length !== queueRef.current.length) setQueue(next);
    },
    [setQueue],
  );

  // Back online: send what was queued, one at a time, each after the reply
  // before it. A queued message that fails holds the rest until the person
  // retries or the connection comes back again.
  useEffect(() => {
    if (status === 'idle' || status === 'stopped') {
      flushing.current = false;
      flushHeld.current = false;
    } else if (status === 'error' && flushing.current) {
      flushing.current = false;
      flushHeld.current = true;
    }
    // The refs guard against a stale render: only the latest queue is sent from.
    if (!online || !networkRef.current.isOnline()) return;
    if (queue.length === 0 || queue !== queueRef.current) return;
    if (status !== 'idle' && status !== 'stopped' && status !== 'error') return;
    if (status === 'error' && flushHeld.current) return;
    const [next, ...rest] = queue;
    if (!next) return;
    setQueue(rest);
    flushing.current = true;
    submitNow(
      next.text,
      { files: next.files, ...(next.body ? { body: next.body } : {}) },
      next.id,
    ).catch(() => undefined);
  }, [online, queue, setQueue, status, submitNow]);

  const send = useCallback(
    (text: string, sendOptions?: SendOptions) => {
      submit(text, sendOptions).catch(() => undefined);
    },
    [submit],
  );

  const stop = useCallback(() => {
    if (supersede()) setStatus('stopped');
  }, [setStatus, supersede]);

  const regenerate = useCallback(
    (messageId?: string, sendOptions?: Pick<SendOptions, 'body'>) => {
      supersede();
      const base = treeRef.current;
      const target = messageId ? base.nodes[messageId] : lastAssistant(visiblePath(base));
      if (!target || target.role !== 'assistant') return;
      const parentId = base.parent[target.id] ?? ROOT;
      ask(parentId, 'regenerate', base, sendOptions?.body);
    },
    [ask, supersede],
  );

  const retry = useCallback(
    (messageId?: string, sendOptions?: Pick<SendOptions, 'body'>) => {
      supersede();
      const base = treeRef.current;
      const target = messageId ? base.nodes[messageId] : lastAssistant(visiblePath(base));
      if (!target || target.role !== 'assistant') return;
      const { finishReason: _f, usage: _u, runId: _r, ...rest } = target;
      const cleared: AiMessage = { ...rest, parts: [], createdAt: new Date().toISOString() };
      const next = replaceNode(base, cleared);
      setTree(next);
      const ids = new Set<string>();
      let at: string | null = base.parent[target.id] ?? null;
      while (at) {
        ids.add(at);
        at = base.parent[at] ?? null;
      }
      const history = visiblePath(next).filter((m) => ids.has(m.id));
      const merged = { ...opts.current.body, ...sendOptions?.body };
      runInto(
        target.id,
        (signal) =>
          transportRef.current.stream({
            messages: history,
            trigger: 'regenerate',
            signal,
            ...(conversationRef.current ? { conversationId: conversationRef.current } : {}),
            ...(Object.keys(merged).length ? { body: merged } : {}),
          }),
        'submitted',
      );
    },
    [runInto, setTree, supersede],
  );

  const editAndResend = useCallback(
    (messageId: string, text: string, sendOptions?: SendOptions) => {
      const original = treeRef.current.nodes[messageId];
      if (!original || original.role !== 'user') return;
      supersede();
      const files =
        sendOptions?.files ?? original.parts.filter((p): p is AiFilePart => p.type === 'file');
      withAttachments(files, (parts) => {
        const base = treeRef.current;
        const parentId = base.parent[messageId] ?? ROOT;
        const user = userMessage(text, parts);
        ask(user.id, 'edit', addChild(base, parentId, user), sendOptions?.body);
      }).catch(() => undefined);
    },
    [ask, supersede, userMessage, withAttachments],
  );

  /** Patches one tool call wherever it is (streaming draft or tree). Returns the owner. */
  const patchToolCall = useCallback(
    (toolCallId: string, patch: (p: AiToolCallPart) => AiToolCallPart): AiMessage | undefined => {
      const run = current.current;
      if (run?.draft?.parts.some(isTool(toolCallId))) {
        run.draft = {
          ...run.draft,
          parts: run.draft.parts.map((p) => (isTool(toolCallId)(p) ? patch(p) : p)),
        };
        setLive({ id: run.targetId, message: run.draft });
        return run.draft;
      }
      const base = treeRef.current;
      const owner = visiblePath(base).find((m) => m.parts.some(isTool(toolCallId)));
      const stored = owner ? base.nodes[owner.id] : undefined;
      if (!stored) return undefined;
      const patched: AiMessage = {
        ...stored,
        parts: stored.parts.map((p) => (isTool(toolCallId)(p) ? patch(p) : p)),
      };
      setTree(replaceNode(base, patched));
      return patched;
    },
    [setTree],
  );

  /** Continues a reply after tool results, with trigger `tool-result`. */
  const continueWithResults = useCallback(
    (ownerId: string) => {
      const history = visiblePath(treeRef.current);
      const cut = history.findIndex((m) => m.id === ownerId);
      const messages = cut >= 0 ? history.slice(0, cut + 1) : history;
      runInto(ownerId, (signal) =>
        transportRef.current.stream({
          messages,
          trigger: 'tool-result',
          signal,
          ...(conversationRef.current ? { conversationId: conversationRef.current } : {}),
          ...(opts.current.body && Object.keys(opts.current.body).length
            ? { body: opts.current.body }
            : {}),
        }),
      );
    },
    [runInto],
  );

  const submitToolResult = useCallback(
    (toolCallId: string, result: AiToolResult) => {
      const owner = patchToolCall(toolCallId, (p) =>
        p.state !== 'awaiting-result' && p.state !== 'running'
          ? p
          : {
              ...p,
              state: result.error !== undefined ? 'failed' : 'succeeded',
              ...(result.output !== undefined ? { output: result.output } : {}),
              ...(result.error !== undefined ? { error: result.error } : {}),
              endedAt: new Date().toISOString(),
            },
      );
      if (!owner || current.current?.targetId === owner.id) return;
      if (owner.parts.some((p) => p.type === 'tool-call' && p.state === 'awaiting-result')) return;
      if (owner.parts.some((p) => p.type === 'tool-call' && p.state === 'pending-approval')) return;
      continueWithResults(owner.id);
    },
    [continueWithResults, patchToolCall],
  );

  const respond = useCallback(
    (toolCallId: string, approved: boolean, input?: unknown, reason?: string) => {
      const before = findToolOwner(toolCallId);
      const part = before?.parts.find(isTool(toolCallId));
      if (!before || !part || part.state !== 'pending-approval') return;
      const clientTool = Boolean(opts.current.onToolCall) && !transportRef.current.respondToTool;
      const owner = patchToolCall(toolCallId, (p) => ({
        ...p,
        state: approved ? (clientTool ? 'awaiting-result' : 'running') : 'denied',
        ...(approved && input !== undefined ? { input } : {}),
        ...(approved ? { startedAt: new Date().toISOString() } : {}),
        ...(!approved && reason ? { error: reason } : {}),
      }));
      if (!owner) return;
      const response = {
        toolCallId,
        approved,
        ...(input !== undefined ? { input } : {}),
        ...(reason !== undefined ? { reason } : {}),
      };
      opts.current.onToolResponse?.(response);
      // Still streaming: the run settles the reply when it ends.
      if (current.current?.targetId === owner.id) return;
      const stillWaiting = owner.parts.some(
        (p) => p.type === 'tool-call' && p.state === 'pending-approval',
      );
      if (stillWaiting) return;
      const respondToTool = transportRef.current.respondToTool;
      if (!respondToTool) {
        settleRef.current(owner.id, false);
        return;
      }
      const messages = visiblePath(treeRef.current);
      // A transport that throws here ends the reply with an error (see `drive`).
      runInto(owner.id, (signal) =>
        respondToTool.call(transportRef.current, {
          ...response,
          messages,
          signal,
          ...(conversationRef.current ? { conversationId: conversationRef.current } : {}),
        }),
      );
    },
    [findToolOwner, patchToolCall, runInto],
  );

  /** After a reply ends: wait for approvals, run client tools, or finish. */
  settleRef.current = (messageId: string, finished: boolean) => {
    const message = treeRef.current.nodes[messageId];
    if (!message) return;
    const calls = message.parts.filter((p): p is AiToolCallPart => p.type === 'tool-call');
    const policy = opts.current.toolPolicy;
    if (policy) {
      for (const call of calls) {
        if (call.state !== 'pending-approval') continue;
        const decision = policy(call);
        if (decision === 'allow' || decision === 'deny') {
          // Decided without asking: patch, then settle again from the new state.
          respond(call.id, decision === 'allow');
          return;
        }
      }
    }
    if (calls.some((p) => p.state === 'pending-approval')) {
      setStatus('awaiting-approval');
      return;
    }
    const awaiting = calls.filter((p) => p.state === 'awaiting-result');
    if (awaiting.length) {
      setStatus('awaiting-tool-result');
      const run = opts.current.onToolCall;
      if (!run) return;
      const controller = new AbortController();
      toolRuns.current = controller;
      for (const call of awaiting) {
        Promise.resolve()
          .then(() => run(call, { signal: controller.signal }))
          .then(
            (output) => ({ output }) as AiToolResult,
            (e: unknown) =>
              ({
                error: e instanceof Error && e.message ? e.message : strings().toolResultFailed,
              }) as AiToolResult,
          )
          .then((result) => {
            if (controller.signal.aborted || !mounted.current) return;
            submitToolResult(call.id, result);
          });
      }
      return;
    }
    setStatus('idle');
    if (finished) opts.current.onFinish?.(message);
  };

  const approveTool = useCallback(
    (id: string, input?: unknown) => respond(id, true, input),
    [respond],
  );
  const denyTool = useCallback(
    (id: string, reason?: string) => respond(id, false, undefined, reason),
    [respond],
  );

  const resumeRun = useCallback(
    (messageId?: string) => {
      const resume = transportRef.current.resume;
      if (!resume) return;
      supersede();
      const base = treeRef.current;
      const path = visiblePath(base);
      const target = messageId
        ? base.nodes[messageId]
        : [...path].reverse().find((m) => m.role === 'assistant' && m.runId);
      if (!target?.runId) return;
      const runId = target.runId;
      setTree(replaceNode(base, { ...target, parts: [] }));
      const messages = path.slice(
        0,
        Math.max(
          0,
          path.findIndex((m) => m.id === target.id),
        ),
      );
      runInto(target.id, (signal) =>
        resume.call(transportRef.current, {
          runId,
          messages,
          signal,
          ...(conversationRef.current ? { conversationId: conversationRef.current } : {}),
        }),
      );
    },
    [runInto, setTree, supersede],
  );

  const branchTo = useCallback(
    (messageId: string, index: number) => {
      const t = treeRef.current;
      const parentId = t.parent[messageId] ?? ROOT;
      const kids = t.children[parentId] ?? [];
      if (index < 0 || index >= kids.length) return;
      setTree({ ...t, active: { ...t.active, [parentId]: index } });
    },
    [setTree],
  );

  const setFeedback = useCallback(
    (messageId: string, feedback: 'up' | 'down' | undefined) => {
      const node = treeRef.current.nodes[messageId];
      if (!node) return;
      const { feedback: _old, ...rest } = node;
      setTree(replaceNode(treeRef.current, feedback ? { ...rest, feedback } : rest));
    },
    [setTree],
  );

  const reset = useCallback(
    (messages: readonly AiMessage[] = []) => {
      toolRuns.current?.abort();
      toolRuns.current = null;
      const run = current.current;
      current.current = null;
      run?.controller.abort();
      run?.scheduler.cancel();
      setLive(null);
      setUploads([]);
      clearSlow();
      setQueue([]);
      setTree(treeFrom(messages));
      setStatus('idle');
      setError(null);
    },
    [clearSlow, setQueue, setStatus, setTree],
  );

  // Queued messages keep one object each while they wait, so the thread does not re-render them.
  const queuedCache = useRef(new WeakMap<AiQueuedMessage, AiMessage>());
  const messages = useMemo(() => {
    const path = visiblePath(tree, branchCache.current);
    if (live) {
      const i = path.findIndex((m) => m.id === live.id);
      const shown = i >= 0 ? path[i] : undefined;
      if (i >= 0)
        path[i] = shown?.branch ? { ...live.message, branch: shown.branch } : live.message;
    }
    for (const item of queue) {
      let shown = queuedCache.current.get(item);
      if (!shown) {
        shown = queuedAsMessage(item);
        queuedCache.current.set(item, shown);
      }
      path.push(shown);
    }
    return path;
  }, [tree, live, queue]);

  return {
    messages,
    status,
    error,
    conversationId,
    agent,
    busy: status === 'submitted' || status === 'streaming',
    canSend: status !== 'awaiting-approval',
    uploads,
    offline: !online,
    slow,
    queued: queue,
    cancelQueued,
    send,
    submit,
    stop,
    regenerate,
    retry,
    editAndResend,
    approveTool,
    denyTool,
    submitToolResult,
    resumeRun,
    branchTo,
    setFeedback,
    reset,
  };
}
