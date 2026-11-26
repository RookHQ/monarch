'use client';

import { useCallback, useMemo, useRef } from 'react';
import type { AiAgentRef, AiMessage, UseAiConversationResult } from '../ai-conversation/index.js';
import type { AiMessageViewProps } from './ai-message.js';

/** Per-message props for AiMessageView, as AiThread's `messageProps` takes them. */
export type AiMessagePropsInput =
  | Partial<Omit<AiMessageViewProps, 'message'>>
  | ((message: AiMessage) => Partial<Omit<AiMessageViewProps, 'message'>>)
  | undefined;

export interface AiMessageWiringOptions {
  /** Shown on replies without their own agent. */
  agent?: AiAgentRef | undefined;
  /** Merged over the wiring, e.g. `onAlwaysAllowTool` or `editing` for one message. */
  messageProps?: AiMessagePropsInput;
}

/**
 * Wires AiMessageView to a `useAiConversation` result: regenerate, feedback,
 * branches, retry in place, resume after a dropped connection, cancel a
 * queued message, and approve or deny tool calls. The callbacks
 * keep their identity across renders (they read the latest conversation
 * through a ref), so memoised messages are not re-rendered while a reply
 * streams. Pass the result to AiThread as `messageProps`.
 */
export function useAiMessageWiring(
  conversation: UseAiConversationResult,
  options: AiMessageWiringOptions = {},
): (message: AiMessage) => Partial<Omit<AiMessageViewProps, 'message'>> {
  const { agent, messageProps } = options;
  const ref = useRef(conversation);
  ref.current = conversation;
  const handlers = useMemo(
    (): Pick<
      AiMessageViewProps,
      | 'onRegenerate'
      | 'onFeedback'
      | 'onBranch'
      | 'onRetry'
      | 'onResume'
      | 'onCancelQueued'
      | 'onApproveTool'
      | 'onDenyTool'
    > => ({
      onRegenerate: (m) => ref.current.regenerate(m.id),
      onFeedback: (f, m) => ref.current.setFeedback(m.id, f),
      onBranch: (i, m) => ref.current.branchTo(m.id, i),
      onRetry: (m) => ref.current.retry(m.id),
      onResume: (m) => ref.current.resumeRun(m.id),
      // Optional call: a hand-built conversation object may predate the queue.
      onCancelQueued: (m) => ref.current.cancelQueued?.(m.id),
      onApproveTool: (id, input) => ref.current.approveTool(id, input),
      onDenyTool: (id, reason) => ref.current.denyTool(id, reason),
    }),
    [],
  );
  return useCallback(
    (message: AiMessage) => {
      const extra = typeof messageProps === 'function' ? messageProps(message) : messageProps;
      return { agent, ...handlers, ...extra };
    },
    [agent, handlers, messageProps],
  );
}
