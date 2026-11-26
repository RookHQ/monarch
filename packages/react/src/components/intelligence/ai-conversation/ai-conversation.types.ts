/**
 * The vendor-neutral message model every `./ai` component renders (ADR 0013).
 * A message is an ordered list of parts, so text, reasoning, tool calls,
 * sources and artifacts appear in the order the model produced them.
 */

/** Who wrote a message. */
export type AiRole = 'user' | 'assistant' | 'system' | 'tool';

/** The agent that answered, from the stream's `agent` event. */
export interface AiAgentRef {
  /** Stable id, e.g. `dossier-assistant`. */
  id?: string;
  /** Display name, e.g. "Rook Intelligence". */
  name: string;
  /** Short codename shown in compact places. */
  codename?: string;
  /** What the agent does, e.g. "Writing assistant". */
  role?: string;
  /** Image URL for the agent's avatar. */
  avatar?: string;
}

/**
 * Lifecycle of a tool call. `pending-approval` waits for a person to approve
 * or deny it; `awaiting-result` waits for the client to run the tool and send
 * its result back (`useAiConversation().submitToolResult`), which is how
 * vendor formats end a turn that calls a tool.
 */
export type AiToolCallState =
  | 'pending-approval'
  | 'awaiting-result'
  | 'running'
  | 'succeeded'
  | 'failed'
  | 'denied'
  | 'cancelled';

/** How much harm running a tool can do. Shown by AiToolApproval; `destructive` cannot be undone. */
export type AiToolCallRisk = 'low' | 'medium' | 'high' | 'destructive';

/**
 * Why the model stopped writing: `stop` (finished), `length` (hit the output
 * limit, so the answer is cut short), `content-filter` (a safety filter
 * stopped it), `refusal` (the model declined), `tool-use` (it is waiting for
 * tool results) or `other`.
 */
export type AiFinishReason =
  'stop' | 'length' | 'content-filter' | 'refusal' | 'tool-use' | 'other';

export type AiTextPart = { type: 'text'; text: string; state: 'streaming' | 'done' };
export type AiReasoningPart = {
  type: 'reasoning';
  text: string;
  state: 'streaming' | 'done';
  /** How long the model reasoned, once known. */
  durationMs?: number;
  /** The reasoning exists but its text is not shown. */
  redacted?: boolean;
};
export type AiToolCallPart = {
  type: 'tool-call';
  id: string;
  /** Tool name as the model called it, e.g. `search_documents`. */
  name: string;
  input: unknown;
  state: AiToolCallState;
  output?: unknown;
  error?: string;
  /** ISO 8601 times, for durations. */
  startedAt?: string;
  endedAt?: string;
  /** The arguments as raw JSON text while they stream (`tool-input-delta`), before `input` is parsed. */
  inputText?: string;
  /** How much harm running it can do, when the service says. AiToolApproval shows it. */
  risk?: AiToolCallRisk;
};
export type AiCitationPart = { type: 'citation'; id: string; sourceId: string; quote?: string };
export type AiSourcePart = {
  type: 'source';
  id: string;
  title: string;
  url?: string;
  kind: 'web' | 'doc' | 'record' | 'file';
  /** Product the source lives in, e.g. `dossier`. */
  product?: string;
  favicon?: string;
};
export type AiFilePart = {
  type: 'file';
  name: string;
  mediaType: string;
  /** Where the file lives: an upload URL, or a `data:` URL for an inlined file. */
  url?: string;
  size?: number;
  /** Id from your upload service, sent to the model service instead of the bytes. */
  fileId?: string;
};
export type AiArtifactPart = {
  type: 'artifact';
  id: string;
  kind: 'document' | 'code' | 'table' | 'chart' | 'image' | 'html';
  title: string;
  version: number;
};
export type AiEditProposalPart = {
  type: 'edit-proposal';
  id: string;
  /** What is being edited, e.g. a document id. */
  target: string;
  diff: unknown;
  state: 'proposed' | 'applied' | 'rejected' | 'reverted';
};
export type AiPlanStepState = 'todo' | 'active' | 'done' | 'skipped' | 'failed';
export type AiPlanPart = {
  type: 'plan';
  steps: { id: string; label: string; state: AiPlanStepState }[];
};
/** Generative UI: rendered only from an allow-listed registry of Monarch components. */
export type AiUiPart = {
  type: 'ui';
  component: string;
  /** The props, or (while `streaming`) JSON text that is still arriving. */
  props: unknown;
  /** Stable id, so actions from the component can name the part. */
  id?: string;
  /** `streaming` while the props are still arriving: rendered progressively, validated once `done`. */
  state?: 'streaming' | 'done';
  /** The tool call this UI answers, so an action can be sent back as its result. */
  toolCallId?: string;
};
export type AiErrorPart = {
  type: 'error';
  code: string;
  message: string;
  retryable: boolean;
  /** Milliseconds until a retry is allowed (from `Retry-After`), when the service said. */
  retryAfterMs?: number;
  /**
   * The connection dropped mid-reply but the run can be picked up where the
   * service has it (`useAiConversation().resumeRun`), instead of asked again.
   */
  resumable?: boolean;
};

/**
 * What happened to a memory: the assistant `read` (used) one it had, `write`
 * (saved or updated) one, or was told to `forget` one.
 */
export type AiMemoryAction = 'read' | 'write' | 'forget';
/** A memory the assistant used, saved or forgot while writing this reply. */
export type AiMemoryPart = {
  type: 'memory';
  /** The memory's id in your memory store. */
  id: string;
  action: AiMemoryAction;
  /** The remembered fact, in plain language. */
  text: string;
  /** Where the memory applies, e.g. `user`, `project` or a workspace name. */
  scope?: string;
};

/** One piece of a message. */
export type AiPart =
  | AiTextPart
  | AiReasoningPart
  | AiToolCallPart
  | AiCitationPart
  | AiSourcePart
  | AiFilePart
  | AiArtifactPart
  | AiEditProposalPart
  | AiPlanPart
  | AiUiPart
  | AiMemoryPart
  | AiErrorPart;

export type AiPartType = AiPart['type'];

/** Token counts for one turn, and its cost in millionths of the billing currency. */
export interface AiUsage {
  /** Every token the model read for this turn, including any served from or written to a prompt cache. */
  inputTokens: number;
  /** Every token the model wrote, including reasoning tokens. */
  outputTokens: number;
  costMicros?: number;
  /** How many of `inputTokens` were read from a prompt cache (usually billed at a lower rate). */
  cacheReadTokens?: number;
  /** How many of `inputTokens` were written to a prompt cache (usually billed at a higher rate). */
  cacheWriteTokens?: number;
  /** How many of `outputTokens` were reasoning the person does not see. */
  reasoningTokens?: number;
}

/** One turn of a conversation. */
export interface AiMessage {
  id: string;
  role: AiRole;
  parts: AiPart[];
  agent?: AiAgentRef | undefined;
  /** ISO 8601 time the message was created. */
  createdAt: string;
  /** Model that produced it, e.g. `gemini-2.5-pro`. */
  model?: string | undefined;
  usage?: AiUsage | undefined;
  feedback?: 'up' | 'down' | undefined;
  /** Set when this message has alternatives (regenerations or edits): which one is shown. */
  branch?: { index: number; count: number } | undefined;
  /** Why the model stopped, from the stream's `finish` event. `length` means the answer was cut short. */
  finishReason?: AiFinishReason | undefined;
  /** The service's id for the run that wrote this reply, from a `run` event; used to resume after a reload. */
  runId?: string | undefined;
  /**
   * True on a user message written while offline that has not been sent yet
   * (`useAiConversation().queued`). It is sent, in order, once the connection
   * is back; `cancelQueued(id)` removes it.
   */
  queued?: boolean | undefined;
}

/** Plain text of a message: its text parts joined. Reasoning and tool parts are left out. */
export function messageText(message: Pick<AiMessage, 'parts'>): string {
  return message.parts
    .filter((p): p is AiTextPart => p.type === 'text')
    .map((p) => p.text)
    .join('');
}

/** True while any text or reasoning part of the message is still streaming. */
export function isStreaming(message: Pick<AiMessage, 'parts'>): boolean {
  return message.parts.some(
    (p) => (p.type === 'text' || p.type === 'reasoning') && p.state === 'streaming',
  );
}
