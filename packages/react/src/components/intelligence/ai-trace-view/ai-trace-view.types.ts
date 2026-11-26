/** What a span did: a model call, a tool call, a retrieval, a sub-agent, a guardrail check, or something else. */
export type AiTraceSpanKind = 'model' | 'tool' | 'retrieval' | 'agent' | 'guardrail' | 'other';

/** How a span ended, or `running` while it has not. */
export type AiTraceSpanStatus = 'ok' | 'error' | 'cancelled' | 'running';

/** A value in a span's `attributes`. */
export type AiTraceAttributeValue = string | number | boolean;

/** One step of an agent run. Times are milliseconds from the start of the run. */
export interface AiTraceSpan {
  /** Stable id, unique in the trace. Used for the tree, selection and expansion. */
  id: string;
  /** The span this one ran inside. Omit for a top-level span; an unknown id is treated as top level. */
  parentId?: string | undefined;
  kind: AiTraceSpanKind;
  /** What ran, e.g. a tool name or a step name. */
  name: string;
  status: AiTraceSpanStatus;
  /** When it started, in milliseconds from the run start. */
  startMs: number;
  /** When it ended, in milliseconds from the run start. Omit while it is still running. */
  endMs?: number | undefined;
  /** Tokens this span itself read. Do not roll children up into parents: totals add every span. */
  inputTokens?: number | undefined;
  /** Tokens this span itself wrote. */
  outputTokens?: number | undefined;
  /** This span's own cost in millionths of `currency` (1 000 000 = 1.00). */
  costMicros?: number | undefined;
  /** ISO 4217 currency of `costMicros`. Default: the component's `currency`. */
  currency?: string | undefined;
  /** Label of the model that served a model call. */
  model?: string | undefined;
  /** Why it failed. Shown as plain text in the details. */
  error?: string | undefined;
  /** Extra facts, shown as a description list in the details. */
  attributes?: Readonly<Record<string, AiTraceAttributeValue>> | undefined;
  /** What went in, as plain text. Never rendered as HTML. */
  input?: string | undefined;
  /** What came out, as plain text. Never rendered as HTML. */
  output?: string | undefined;
  /**
   * Id of the conversation part this span produced or served, e.g. the
   * `toolCallId` of a tool-call part. Written on the row as `data-part-id` and
   * handed to `onOpenPart`.
   */
  partId?: string | undefined;
  /** Id of the conversation message this span belongs to. Written on the row as `data-message-id`. */
  messageId?: string | undefined;
}

/** Where a span, a run event or an agent points in a conversation. */
export interface AiTracePartLink {
  /** Id of the conversation part, when known. */
  partId?: string | undefined;
  /** Id of the conversation message, when known. */
  messageId?: string | undefined;
}
