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

/** Lifecycle of a tool call. `pending-approval` waits for a person to approve or deny it. */
export type AiToolCallState =
  'pending-approval' | 'running' | 'succeeded' | 'failed' | 'denied' | 'cancelled';

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
  url?: string;
  size?: number;
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
export type AiUiPart = { type: 'ui'; component: string; props: unknown };
export type AiErrorPart = { type: 'error'; code: string; message: string; retryable: boolean };

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
  | AiErrorPart;

export type AiPartType = AiPart['type'];

/** Token counts for one turn, and its cost in millionths of the billing currency. */
export interface AiUsage {
  inputTokens: number;
  outputTokens: number;
  costMicros?: number;
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
