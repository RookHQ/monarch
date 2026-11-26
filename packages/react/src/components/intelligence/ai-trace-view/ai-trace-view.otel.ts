import type {
  AiTraceAttributeValue,
  AiTraceSpan,
  AiTraceSpanKind,
  AiTraceSpanStatus,
} from './ai-trace-view.types.js';

/** An OTLP/JSON `AnyValue`. Integers arrive as strings, because JSON numbers cannot hold 64 bits. */
export interface OtelAnyValue {
  stringValue?: string | undefined;
  intValue?: string | number | undefined;
  doubleValue?: number | string | undefined;
  boolValue?: boolean | undefined;
  bytesValue?: string | undefined;
  arrayValue?: { values?: readonly OtelAnyValue[] | undefined } | undefined;
  kvlistValue?: { values?: readonly OtelKeyValue[] | undefined } | undefined;
}

/** An OTLP/JSON attribute. */
export interface OtelKeyValue {
  key: string;
  value?: OtelAnyValue | undefined;
}

/**
 * Attributes in either wire shape: the OTLP/JSON list of key and value
 * records, or the plain object an SDK span exporter writes.
 */
export type OtelAttributes = readonly OtelKeyValue[] | Readonly<Record<string, unknown>>;

/** A point in time: nanoseconds since the Unix epoch as a string, number or bigint, or an SDK `[seconds, nanoseconds]` pair. */
export type OtelTime = string | number | bigint | readonly [number, number];

/** The status of a span. `code` is 0/1/2, or the enum name (`STATUS_CODE_ERROR`, `ERROR`). */
export interface OtelStatus {
  code?: number | string | undefined;
  message?: string | undefined;
}

/** An event recorded on a span, e.g. `exception`. */
export interface OtelSpanEvent {
  name?: string | undefined;
  timeUnixNano?: OtelTime | undefined;
  time?: OtelTime | undefined;
  attributes?: OtelAttributes | undefined;
}

/** A span as OTLP/JSON writes it, inside `resourceSpans[].scopeSpans[].spans[]`. */
export interface OtlpSpan {
  traceId?: string | undefined;
  spanId: string;
  parentSpanId?: string | undefined;
  name?: string | undefined;
  kind?: number | string | undefined;
  startTimeUnixNano?: OtelTime | undefined;
  /** Missing, empty or `0` while the span is still running. */
  endTimeUnixNano?: OtelTime | undefined;
  attributes?: OtelAttributes | undefined;
  status?: OtelStatus | undefined;
  events?: readonly OtelSpanEvent[] | undefined;
}

/**
 * A span in the flat shape SDK exporters write (the console exporter and
 * `ReadableSpan` serialised to JSON): `id` and `parentId`, `timestamp` and
 * `duration` in microseconds or `startTime` and `endTime` pairs, and
 * attributes as a plain object.
 */
export interface OtelExportedSpan {
  traceId?: string | undefined;
  /** The span id. `spanId` is read too. */
  id?: string | undefined;
  spanId?: string | undefined;
  parentId?: string | undefined;
  parentSpanId?: string | undefined;
  parentSpanContext?: { spanId?: string | undefined } | undefined;
  name?: string | undefined;
  kind?: number | string | undefined;
  /** Start in microseconds since the Unix epoch. */
  timestamp?: number | undefined;
  /** Length in microseconds. */
  duration?: number | readonly [number, number] | undefined;
  startTime?: OtelTime | undefined;
  endTime?: OtelTime | undefined;
  startTimeUnixNano?: OtelTime | undefined;
  endTimeUnixNano?: OtelTime | undefined;
  /** `false` while an SDK span has not ended. */
  ended?: boolean | undefined;
  attributes?: OtelAttributes | undefined;
  status?: OtelStatus | undefined;
  events?: readonly OtelSpanEvent[] | undefined;
}

/** Any span shape `otelSpansToAiTrace` reads. */
export type OtelSpanInput = OtlpSpan | OtelExportedSpan;

/** An OTLP/JSON trace export: `resourceSpans` → `scopeSpans` → `spans`. */
export interface OtlpTraceData {
  resourceSpans?:
    | readonly {
        resource?: { attributes?: OtelAttributes | undefined } | undefined;
        scopeSpans?:
          | readonly {
              scope?: { name?: string | undefined; version?: string | undefined } | undefined;
              spans?: readonly OtlpSpan[] | undefined;
            }[]
          | undefined;
      }[]
    | undefined;
}

/** What `otelSpansToAiTrace` accepts: one OTLP/JSON export, several, or a flat list of spans. */
export type OtelTraceInput = OtlpTraceData | readonly (OtlpTraceData | OtelSpanInput)[];

/** A span after its attributes are read, handed to the `kind`, `cost` and `link` options. */
export interface OtelSpanContext {
  /** The span as it was passed in. */
  span: OtelSpanInput;
  /** Its attributes as plain values. Lists and maps are JSON text. */
  attributes: Readonly<Record<string, AiTraceAttributeValue>>;
  /** The mapped span, before `cost` and `link` are applied. */
  mapped: AiTraceSpan;
}

export interface OtelSpansToAiTraceOptions {
  /** Keep only the spans of this trace. Default: every span given. */
  traceId?: string | undefined;
  /**
   * The run start in nanoseconds since the Unix epoch; `startMs` and `endMs`
   * count from it. Default: the earliest span start.
   */
  originUnixNano?: OtelTime | undefined;
  /**
   * Copies prompt and completion content (`gen_ai.input.messages`,
   * `gen_ai.output.messages`, tool arguments and results) into `input` and
   * `output`. Default `true`; pass `false` where content must not reach the
   * screen.
   */
  content?: boolean | undefined;
  /**
   * Which attributes are copied to `attributes`: `rest` (default) keeps those
   * the mapping did not use, `all` keeps every one, `none` keeps none.
   * Content attributes are never copied.
   */
  attributes?: 'rest' | 'all' | 'none' | undefined;
  /** Overrides the kind of a span; return `undefined` to keep the mapped kind. */
  kind?: ((context: OtelSpanContext) => AiTraceSpanKind | undefined) | undefined;
  /** Prices a span: OpenTelemetry carries no cost. Return micros of `currency`, or `undefined` for none. */
  cost?:
    | ((
        context: OtelSpanContext,
      ) => { micros: number; currency?: string | undefined } | number | undefined)
    | undefined;
  /**
   * Links a span to a conversation part. Default: `partId` from
   * `gen_ai.tool.call.id` and `messageId` from `gen_ai.response.id`.
   */
  link?:
    | ((
        context: OtelSpanContext,
      ) => { partId?: string | undefined; messageId?: string | undefined } | undefined)
    | undefined;
}

const MODEL_OPERATIONS = new Set([
  'chat',
  'text_completion',
  'generate_content',
  'embeddings',
  'completion',
]);
const AGENT_OPERATIONS = new Set(['invoke_agent', 'create_agent', 'invoke_workflow']);
const RETRIEVAL_OPERATIONS = new Set(['retrieval', 'retrieve', 'search', 'query']);
const GUARDRAIL_OPERATIONS = new Set(['guardrail', 'apply_guardrail', 'evaluate_guardrail']);

const INPUT_KEYS = [
  'gen_ai.tool.call.arguments',
  'gen_ai.input.messages',
  'gen_ai.prompt',
  'gen_ai.retrieval.query.text',
  'input.value',
] as const;
const OUTPUT_KEYS = [
  'gen_ai.tool.call.result',
  'gen_ai.output.messages',
  'gen_ai.completion',
  'output.value',
] as const;
/** Attributes that hold prompts or answers: never copied into `attributes`. */
const CONTENT_KEYS = new Set<string>([
  ...INPUT_KEYS,
  ...OUTPUT_KEYS,
  'gen_ai.system_instructions',
  'gen_ai.retrieval.documents',
]);

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value);

function anyValue(value: OtelAnyValue | undefined): unknown {
  if (!value) return undefined;
  if (value.stringValue !== undefined) return value.stringValue;
  if (value.boolValue !== undefined) return value.boolValue;
  if (value.intValue !== undefined) {
    const n = Number(value.intValue);
    return Number.isSafeInteger(n) ? n : String(value.intValue);
  }
  if (value.doubleValue !== undefined) return Number(value.doubleValue);
  if (value.arrayValue) return (value.arrayValue.values ?? []).map(anyValue);
  if (value.kvlistValue) {
    const out: Record<string, unknown> = {};
    for (const kv of value.kvlistValue.values ?? []) out[kv.key] = anyValue(kv.value);
    return out;
  }
  if (value.bytesValue !== undefined) return value.bytesValue;
  return undefined;
}

function plain(value: unknown): AiTraceAttributeValue | undefined {
  if (typeof value === 'string' || typeof value === 'boolean') return value;
  if (typeof value === 'number') return Number.isFinite(value) ? value : String(value);
  if (typeof value === 'bigint') return value.toString();
  if (value === undefined || value === null) return undefined;
  try {
    return JSON.stringify(value);
  } catch {
    return undefined;
  }
}

/** Attributes of either wire shape as one plain record. */
function readAttributes(
  attributes: OtelAttributes | undefined,
): Record<string, AiTraceAttributeValue> {
  const out: Record<string, AiTraceAttributeValue> = {};
  if (!attributes) return out;
  if (Array.isArray(attributes)) {
    for (const kv of attributes as readonly OtelKeyValue[]) {
      if (!isRecord(kv) || typeof kv.key !== 'string') continue;
      const value = plain(anyValue(kv.value));
      if (value !== undefined) out[kv.key] = value;
    }
    return out;
  }
  for (const [key, raw] of Object.entries(attributes)) {
    const value = plain(raw);
    if (value !== undefined) out[key] = value;
  }
  return out;
}

const NANOS_PER_MS = 1_000_000n;

/** A time as nanoseconds since the epoch, or `undefined` when it is missing, zero or unreadable. */
function toNanos(time: OtelTime | undefined): bigint | undefined {
  if (time === undefined || time === null) return undefined;
  let nanos: bigint | undefined;
  if (typeof time === 'bigint') nanos = time;
  else if (typeof time === 'number') {
    nanos = Number.isFinite(time) ? BigInt(Math.round(time)) : undefined;
  } else if (typeof time === 'string') {
    const text = time.trim();
    if (/^\d+$/.test(text)) nanos = BigInt(text);
    else {
      const ms = text === '' ? NaN : Date.parse(text);
      nanos = Number.isFinite(ms) ? BigInt(ms) * NANOS_PER_MS : undefined;
    }
  } else if (Array.isArray(time)) {
    const [seconds, rest] = time as readonly [number, number];
    if (Number.isFinite(seconds) && Number.isFinite(rest)) {
      nanos = BigInt(Math.round(seconds)) * 1_000_000_000n + BigInt(Math.round(rest));
    }
  }
  return nanos !== undefined && nanos > 0n ? nanos : undefined;
}

/** Nanoseconds between two instants as milliseconds, keeping sub-millisecond precision. */
function msBetween(from: bigint, to: bigint): number {
  const diff = to - from;
  return Number(diff / NANOS_PER_MS) + Number(diff % NANOS_PER_MS) / 1_000_000;
}

const microsToNanos = (micros: number | undefined): bigint | undefined =>
  micros !== undefined && Number.isFinite(micros) && micros > 0
    ? BigInt(Math.round(micros)) * 1000n
    : undefined;

function spanId(span: OtelSpanInput): string | undefined {
  const id = ('id' in span ? span.id : undefined) ?? span.spanId;
  return typeof id === 'string' && id !== '' ? id : undefined;
}

function parentId(span: OtelSpanInput): string | undefined {
  const exported = span as OtelExportedSpan;
  const id = exported.parentSpanId ?? exported.parentId ?? exported.parentSpanContext?.spanId;
  // A root span has no parent, an empty one, or the all-zero id.
  return typeof id === 'string' && id !== '' && !/^0+$/.test(id) ? id : undefined;
}

function times(span: OtelSpanInput): { start: bigint | undefined; end: bigint | undefined } {
  const exported = span as OtelExportedSpan;
  const start =
    toNanos(span.startTimeUnixNano) ??
    toNanos(exported.startTime) ??
    microsToNanos(exported.timestamp);
  let end = toNanos(span.endTimeUnixNano) ?? toNanos(exported.endTime);
  if (end === undefined && start !== undefined && exported.duration !== undefined) {
    const { duration } = exported;
    if (typeof duration === 'number') {
      if (Number.isFinite(duration) && duration >= 0) {
        end = start + BigInt(Math.round(duration)) * 1000n;
      }
    } else if (Array.isArray(duration)) {
      const [seconds, rest] = duration as readonly [number, number];
      if (Number.isFinite(seconds) && Number.isFinite(rest)) {
        end = start + BigInt(Math.round(seconds)) * 1_000_000_000n + BigInt(Math.round(rest));
      }
    }
  }
  if (exported.ended === false) end = undefined;
  return { start, end };
}

function isError(status: OtelStatus | undefined): boolean {
  const code = status?.code;
  if (typeof code === 'number') return code === 2;
  return typeof code === 'string' && /(^|_)ERROR$/i.test(code);
}

function text(
  attributes: Readonly<Record<string, AiTraceAttributeValue>>,
  key: string,
): string | undefined {
  const value = attributes[key];
  if (value === undefined || value === '') return undefined;
  return String(value);
}

function count(
  attributes: Readonly<Record<string, AiTraceAttributeValue>>,
  ...keys: string[]
): number | undefined {
  for (const key of keys) {
    const value = attributes[key];
    const n = typeof value === 'string' && value.trim() !== '' ? Number(value) : value;
    if (typeof n === 'number' && Number.isFinite(n) && n >= 0) return n;
  }
  return undefined;
}

function kindOf(attributes: Readonly<Record<string, AiTraceAttributeValue>>): AiTraceSpanKind {
  const operation = text(attributes, 'gen_ai.operation.name')?.toLowerCase();
  if (operation) {
    if (operation === 'execute_tool') return 'tool';
    if (MODEL_OPERATIONS.has(operation)) return 'model';
    if (AGENT_OPERATIONS.has(operation)) return 'agent';
    if (RETRIEVAL_OPERATIONS.has(operation)) return 'retrieval';
    if (GUARDRAIL_OPERATIONS.has(operation)) return 'guardrail';
  }
  if (attributes['gen_ai.tool.name'] !== undefined) return 'tool';
  if (
    attributes['gen_ai.agent.name'] !== undefined ||
    attributes['gen_ai.agent.id'] !== undefined
  ) {
    return 'agent';
  }
  if (
    attributes['gen_ai.request.model'] !== undefined ||
    attributes['gen_ai.response.model'] !== undefined
  ) {
    return 'model';
  }
  if (
    attributes['db.system'] !== undefined ||
    attributes['db.system.name'] !== undefined ||
    attributes['gen_ai.data_source.id'] !== undefined
  ) {
    return 'retrieval';
  }
  return 'other';
}

/** The keys the mapping reads into typed fields; `attributes: 'rest'` leaves them out. */
const USED_KEYS = new Set([
  'gen_ai.operation.name',
  'gen_ai.request.model',
  'gen_ai.response.model',
  'gen_ai.usage.input_tokens',
  'gen_ai.usage.output_tokens',
  'gen_ai.usage.prompt_tokens',
  'gen_ai.usage.completion_tokens',
  'gen_ai.tool.name',
  'gen_ai.tool.call.id',
  'gen_ai.agent.name',
  'gen_ai.response.id',
]);

function exceptionMessage(events: readonly OtelSpanEvent[] | undefined): string | undefined {
  if (!Array.isArray(events)) return undefined;
  for (const event of events) {
    if (!isRecord(event) || event.name !== 'exception') continue;
    const attributes = readAttributes(event.attributes as OtelAttributes | undefined);
    const message = text(attributes, 'exception.message') ?? text(attributes, 'exception.type');
    if (message) return message;
  }
  return undefined;
}

function collect(input: OtelTraceInput): OtelSpanInput[] {
  const out: OtelSpanInput[] = [];
  const visit = (item: unknown) => {
    if (!isRecord(item)) return;
    if (Array.isArray(item.resourceSpans)) {
      for (const resource of item.resourceSpans as unknown[]) {
        if (!isRecord(resource) || !Array.isArray(resource.scopeSpans)) continue;
        for (const scope of resource.scopeSpans as unknown[]) {
          if (!isRecord(scope) || !Array.isArray(scope.spans)) continue;
          for (const span of scope.spans as unknown[]) visit(span);
        }
      }
      return;
    }
    out.push(item as unknown as OtelSpanInput);
  };
  if (Array.isArray(input)) for (const item of input as readonly unknown[]) visit(item);
  else visit(input);
  return out;
}

/**
 * Converts OpenTelemetry spans that follow the GenAI semantic conventions
 * into the `AiTraceSpan` list `AiTraceView` renders. Reads OTLP/JSON
 * (`resourceSpans` → `scopeSpans` → `spans`) and the flat shape SDK exporters
 * write. A pure function: nothing is fetched and the input is not changed.
 *
 * - Times: nanosecond strings, numbers, bigints and `[seconds, nanoseconds]`
 *   pairs become milliseconds from the earliest span start.
 * - A span without an end is `running`; an error status is `error`, with the
 *   status message, `error.type` or the `exception` event as its text.
 * - A parent that is not in the input is dropped, so an orphan span is shown
 *   at the top level; spans without an id and repeated ids are skipped.
 * - Attributes the mapping does not know are kept in `attributes`.
 */
export function otelSpansToAiTrace(
  input: OtelTraceInput,
  options: OtelSpansToAiTraceOptions = {},
): AiTraceSpan[] {
  const keep = options.attributes ?? 'rest';
  const withContent = options.content !== false;

  interface Read {
    span: OtelSpanInput;
    id: string;
    start: bigint | undefined;
    end: bigint | undefined;
  }
  const seen = new Set<string>();
  const read: Read[] = [];
  for (const span of collect(input)) {
    const id = spanId(span);
    if (id === undefined || seen.has(id)) continue;
    if (options.traceId !== undefined && span.traceId !== options.traceId) continue;
    seen.add(id);
    read.push({ span, id, ...times(span) });
  }

  let origin = toNanos(options.originUnixNano);
  if (origin === undefined) {
    for (const { start } of read) {
      if (start !== undefined && (origin === undefined || start < origin)) origin = start;
    }
  }
  const zero = origin ?? 0n;

  const out: AiTraceSpan[] = [];
  for (const { span, id, start, end } of read) {
    const attributes = readAttributes(span.attributes);
    const failed = isError(span.status);
    const errorType = text(attributes, 'error.type');
    const cancelled = /^(cancell?ed|aborted|aborterror)$/i.test(errorType ?? '');
    const status: AiTraceSpanStatus = cancelled
      ? 'cancelled'
      : failed
        ? 'error'
        : end === undefined
          ? 'running'
          : 'ok';
    const kind = kindOf(attributes);
    const operation = text(attributes, 'gen_ai.operation.name');
    const model =
      text(attributes, 'gen_ai.response.model') ?? text(attributes, 'gen_ai.request.model');
    const name =
      (kind === 'tool' ? text(attributes, 'gen_ai.tool.name') : undefined) ??
      (kind === 'agent' ? text(attributes, 'gen_ai.agent.name') : undefined) ??
      (typeof span.name === 'string' && span.name !== '' ? span.name : undefined) ??
      (operation && model ? `${operation} ${model}` : operation) ??
      id;
    const startMs = start === undefined ? 0 : msBetween(zero, start);
    const mapped: AiTraceSpan = { id, kind, name, status, startMs };

    const parent = parentId(span);
    if (parent !== undefined && parent !== id && seen.has(parent)) mapped.parentId = parent;
    // A span that failed or was cancelled has ended, even when no end time was written.
    if (end !== undefined) mapped.endMs = Math.max(startMs, msBetween(zero, end));
    else if (status === 'error' || status === 'cancelled') mapped.endMs = startMs;

    const inputTokens = count(
      attributes,
      'gen_ai.usage.input_tokens',
      'gen_ai.usage.prompt_tokens',
    );
    const outputTokens = count(
      attributes,
      'gen_ai.usage.output_tokens',
      'gen_ai.usage.completion_tokens',
    );
    if (inputTokens !== undefined) mapped.inputTokens = inputTokens;
    if (outputTokens !== undefined) mapped.outputTokens = outputTokens;
    if (model !== undefined) mapped.model = model;

    if (status === 'error') {
      const message =
        (typeof span.status?.message === 'string' && span.status.message !== ''
          ? span.status.message
          : undefined) ??
        exceptionMessage(span.events) ??
        errorType;
      if (message !== undefined) mapped.error = message;
    }

    if (withContent) {
      for (const key of INPUT_KEYS) {
        const value = text(attributes, key);
        if (value !== undefined) {
          mapped.input = value;
          break;
        }
      }
      for (const key of OUTPUT_KEYS) {
        const value = text(attributes, key);
        if (value !== undefined) {
          mapped.output = value;
          break;
        }
      }
    }

    if (keep !== 'none') {
      const rest: Record<string, AiTraceAttributeValue> = {};
      for (const [key, value] of Object.entries(attributes)) {
        if (CONTENT_KEYS.has(key)) continue;
        if (keep === 'rest' && USED_KEYS.has(key)) continue;
        rest[key] = value;
      }
      if (Object.keys(rest).length > 0) mapped.attributes = rest;
    }

    const context: OtelSpanContext = { span, attributes, mapped };
    const overridden = options.kind?.(context);
    if (overridden !== undefined) mapped.kind = overridden;

    const link = options.link
      ? options.link(context)
      : {
          partId: text(attributes, 'gen_ai.tool.call.id'),
          messageId: text(attributes, 'gen_ai.response.id'),
        };
    if (link?.partId !== undefined) mapped.partId = link.partId;
    if (link?.messageId !== undefined) mapped.messageId = link.messageId;

    const cost = options.cost?.(context);
    if (typeof cost === 'number') {
      if (Number.isFinite(cost)) mapped.costMicros = cost;
    } else if (cost && Number.isFinite(cost.micros)) {
      mapped.costMicros = cost.micros;
      if (cost.currency !== undefined) mapped.currency = cost.currency;
    }

    out.push(mapped);
  }

  // Start order, input order for ties: a stable list for the same input.
  return out
    .map((span, index) => ({ span, index }))
    .sort((a, b) => a.span.startMs - b.span.startMs || a.index - b.index)
    .map(({ span }) => span);
}
