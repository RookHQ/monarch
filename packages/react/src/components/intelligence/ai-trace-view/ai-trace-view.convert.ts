import type { DateInput } from '../../data-display/relative-time/index.js';
import type { AiRunEvent, AiRunEventKind } from '../ai-agent-run-timeline/index.js';
import type { AiSubagentNode, AiSubagentStatus } from '../ai-subagent-tree/index.js';
import type { AiTraceSpan, AiTraceSpanStatus } from './ai-trace-view.types.js';

export interface AiTraceConvertOptions {
  /**
   * When the run started (Date, epoch milliseconds or ISO string). Span times
   * are milliseconds from the run start, so this turns them into clock times.
   * Default: the Unix epoch for run events; no clock times for agents.
   */
  runStartedAt?: DateInput | undefined;
}

function baseMs(runStartedAt: DateInput | undefined): number | undefined {
  if (runStartedAt === undefined) return undefined;
  const ms =
    runStartedAt instanceof Date
      ? runStartedAt.getTime()
      : typeof runStartedAt === 'number'
        ? runStartedAt
        : Date.parse(runStartedAt);
  return Number.isFinite(ms) ? ms : undefined;
}

/**
 * The spans of a trace as the flat events `AiAgentRunTimeline` shows. A failed
 * span becomes an `error` event, a model call `model`, a sub-agent `handoff`
 * and everything else `tool`. `partId` and `messageId` are carried over.
 */
export function aiTraceToRunEvents(
  spans: readonly AiTraceSpan[],
  options: AiTraceConvertOptions = {},
): AiRunEvent[] {
  const base = baseMs(options.runStartedAt) ?? 0;
  return spans.map((span) => {
    const kind: AiRunEventKind =
      span.status === 'error'
        ? 'error'
        : span.kind === 'model'
          ? 'model'
          : span.kind === 'agent'
            ? 'handoff'
            : 'tool';
    const start = Number.isFinite(span.startMs) ? span.startMs : 0;
    const event: AiRunEvent = { id: span.id, kind, name: span.name, startedAt: base + start };
    if (span.endMs !== undefined && Number.isFinite(span.endMs)) {
      event.endedAt = base + Math.max(start, span.endMs);
    }
    if (span.inputTokens !== undefined) event.inputTokens = span.inputTokens;
    if (span.outputTokens !== undefined) event.outputTokens = span.outputTokens;
    if (span.costMicros !== undefined) event.costMicros = span.costMicros;
    if (span.error !== undefined) event.detail = span.error;
    if (span.partId !== undefined) event.partId = span.partId;
    if (span.messageId !== undefined) event.messageId = span.messageId;
    return event;
  });
}

const AGENT_STATUS: Record<AiTraceSpanStatus, AiSubagentStatus> = {
  ok: 'done',
  error: 'failed',
  cancelled: 'stopped',
  running: 'working',
};

/**
 * The `agent` spans of a trace as the tree `AiSubagentTree` shows. An agent is
 * nested under the nearest agent span above it; the tool calls it made itself
 * are counted, and the step it is on is its activity while it runs. Clock
 * times are set only when `runStartedAt` is given.
 */
export function aiTraceToSubagentTree(
  spans: readonly AiTraceSpan[],
  options: AiTraceConvertOptions = {},
): AiSubagentNode[] {
  const base = baseMs(options.runStartedAt);
  const byId = new Map<string, AiTraceSpan>();
  for (const span of spans) byId.set(span.id, span);

  /** The nearest agent span above `span`; the `seen` guard stops a parent loop. */
  const owner = (span: AiTraceSpan): AiTraceSpan | undefined => {
    const seen = new Set<string>([span.id]);
    let at = span.parentId === undefined ? undefined : byId.get(span.parentId);
    while (at && !seen.has(at.id)) {
      if (at.kind === 'agent') return at;
      seen.add(at.id);
      at = at.parentId === undefined ? undefined : byId.get(at.parentId);
    }
    return undefined;
  };

  interface Draft {
    node: AiSubagentNode;
    children: AiSubagentNode[];
    startMs: number;
    activityAt: number;
  }
  const drafts = new Map<string, Draft>();
  const agents = [...byId.values()]
    .filter((span) => span.kind === 'agent')
    .sort((a, b) => a.startMs - b.startMs);
  for (const span of agents) {
    const node: AiSubagentNode = {
      id: span.id,
      name: span.name,
      status: AGENT_STATUS[span.status],
    };
    const role = span.attributes?.['gen_ai.agent.description'];
    if (typeof role === 'string' && role !== '') node.role = role;
    if (span.error !== undefined) node.summary = span.error;
    if (base !== undefined && Number.isFinite(span.startMs)) {
      node.startedAt = new Date(base + span.startMs).toISOString();
      if (span.endMs !== undefined && Number.isFinite(span.endMs)) {
        node.endedAt = new Date(base + span.endMs).toISOString();
      }
    }
    if (span.partId !== undefined) node.partId = span.partId;
    if (span.messageId !== undefined) node.messageId = span.messageId;
    drafts.set(span.id, { node, children: [], startMs: span.startMs, activityAt: -Infinity });
  }

  const roots: AiSubagentNode[] = [];
  for (const span of byId.values()) {
    const above = owner(span);
    const draft = above ? drafts.get(above.id) : undefined;
    if (span.kind === 'agent') {
      const own = drafts.get(span.id);
      if (!own) continue;
      // An agent inside a parent loop is shown at the top, so the tree stays finite.
      const seen = new Set<string>();
      let at = above;
      let looped = false;
      while (at && !seen.has(at.id)) {
        if (at.id === span.id) {
          looped = true;
          break;
        }
        seen.add(at.id);
        at = owner(at);
      }
      if (draft && !looped) draft.children.push(own.node);
      else roots.push(own.node);
      continue;
    }
    if (!draft) continue;
    if (span.kind === 'tool') draft.node.toolCalls = (draft.node.toolCalls ?? 0) + 1;
    if (span.status === 'running' && span.startMs >= draft.activityAt) {
      draft.activityAt = span.startMs;
      draft.node.activity = span.name;
    }
  }

  const order = (list: AiSubagentNode[]) =>
    list.sort((a, b) => (drafts.get(a.id)?.startMs ?? 0) - (drafts.get(b.id)?.startMs ?? 0));
  for (const draft of drafts.values()) {
    if (draft.children.length > 0) draft.node.children = order(draft.children);
  }
  return order(roots);
}
