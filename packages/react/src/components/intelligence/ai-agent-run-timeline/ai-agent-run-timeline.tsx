'use client';

import { forwardRef, useId, useMemo, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import type { DateInput } from '../../data-display/relative-time/index.js';

/** What a run event was: a model call, a tool call, a hand-off between agents, or an error. */
export type AiRunEventKind = 'model' | 'tool' | 'handoff' | 'error';

/** One event in an agent run trace. */
export interface AiRunEvent {
  /** Stable id, used as the React key and for the expanded state. */
  id: string;
  kind: AiRunEventKind;
  /** What ran, e.g. a model name or a tool name. */
  name: string;
  /** When it started (Date, epoch milliseconds or ISO string). */
  startedAt: DateInput;
  /** When it ended. Omit while it is still running. */
  endedAt?: DateInput | undefined;
  inputTokens?: number | undefined;
  outputTokens?: number | undefined;
  /** Cost in millionths of the `currency` unit (1 000 000 = 1.00). */
  costMicros?: number | undefined;
  /** Shown when the event is expanded: arguments, a result, an error message. */
  detail?: ReactNode;
}

/** How the trace is shown. */
export type AiAgentRunTimelineView = 'timeline' | 'table';

/** Every visible and assistive-technology string. English defaults. */
export interface AiAgentRunTimelineMessages {
  /** Accessible name of the whole trace and the table caption. */
  label: string;
  /** Accessible name of the view switch. */
  viewLabel: string;
  views: Record<AiAgentRunTimelineView, string>;
  kinds: Record<AiRunEventKind, string>;
  /** Accessible name of the event list. */
  events: string;
  totalDuration: string;
  inputTokens: string;
  outputTokens: string;
  cost: string;
  /** Detail label and table column for the offset from the run start. */
  start: string;
  /** Detail label and table column for an event's duration. */
  duration: string;
  /** Table column for the event name. */
  name: string;
  /** Table column for the kind. */
  kind: string;
  /** Shown instead of a duration while an event has not ended. */
  running: string;
  /** Shown when there are no events. */
  empty: string;
  /** Shown for a value that is not known. */
  notAvailable: string;
  /** Formats a duration in milliseconds. */
  formatDuration: (ms: number) => string;
  /** Formats an offset from the run start, e.g. "+1.2 s". */
  formatOffset: (ms: number) => string;
}

function englishDuration(ms: number): string {
  const v = Math.max(0, Math.round(ms));
  if (v < 1000) return `${v} ms`;
  if (v < 60_000) return `${(v / 1000).toFixed(v < 10_000 ? 1 : 0)} s`;
  const min = Math.floor(v / 60_000);
  const s = Math.round((v % 60_000) / 1000);
  return s ? `${min} min ${s} s` : `${min} min`;
}

export const AI_AGENT_RUN_TIMELINE_MESSAGES: AiAgentRunTimelineMessages = {
  label: 'Agent run',
  viewLabel: 'View',
  views: { timeline: 'Timeline', table: 'Table' },
  kinds: { model: 'Model', tool: 'Tool', handoff: 'Hand-off', error: 'Error' },
  events: 'Run events',
  totalDuration: 'Total time',
  inputTokens: 'Input tokens',
  outputTokens: 'Output tokens',
  cost: 'Cost',
  start: 'Start',
  duration: 'Duration',
  name: 'Event',
  kind: 'Kind',
  running: 'Running',
  empty: 'No events yet',
  notAvailable: '–',
  formatDuration: englishDuration,
  formatOffset: (ms) => `+${englishDuration(ms)}`,
};

export interface AiAgentRunTimelineProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The run's events. Order does not matter; they are sorted by start time. */
  events: readonly AiRunEvent[];
  /** ISO 4217 currency for `costMicros`, formatted with `Intl.NumberFormat`. Default `USD`. */
  currency?: string | undefined;
  /** BCP 47 locale for numbers and cost. Default: the runtime locale. */
  locale?: string | undefined;
  /**
   * Reference time used as the end of events that are still running. Without
   * it a running event ends at the latest known timestamp, so rendering stays
   * deterministic on the server.
   */
  now?: DateInput | undefined;
  /** Controlled view: the bar timeline or the table. */
  view?: AiAgentRunTimelineView | undefined;
  /** Initial view when uncontrolled. Default `timeline`. */
  defaultView?: AiAgentRunTimelineView | undefined;
  /** Called when the person switches view. */
  onViewChange?: ((view: AiAgentRunTimelineView) => void) | undefined;
  /** Replaces any of the English strings; `views` and `kinds` may be partial. */
  messages?:
    | (Partial<Omit<AiAgentRunTimelineMessages, 'views' | 'kinds'>> & {
        views?: Partial<Record<AiAgentRunTimelineView, string>> | undefined;
        kinds?: Partial<Record<AiRunEventKind, string>> | undefined;
      })
    | undefined;
}

function toMs(d: DateInput): number {
  return d instanceof Date ? d.getTime() : typeof d === 'number' ? d : Date.parse(d);
}

interface Row {
  event: AiRunEvent;
  start: number;
  end: number;
  running: boolean;
}

/** Totals over a run: wall-clock latency, tokens and cost (in micros). */
export function aiRunTotals(events: readonly AiRunEvent[], now?: DateInput) {
  const starts = events.map((e) => toMs(e.startedAt)).filter(Number.isFinite);
  const ends = events
    .map((e) => (e.endedAt !== undefined ? toMs(e.endedAt) : NaN))
    .filter(Number.isFinite);
  const t0 = starts.length ? Math.min(...starts) : 0;
  const latest = Math.max(t0, ...starts, ...ends);
  const t1 =
    now !== undefined && events.some((e) => e.endedAt === undefined)
      ? Math.max(latest, toMs(now))
      : latest;
  const sum = (pick: (e: AiRunEvent) => number | undefined) =>
    events.reduce((n, e) => n + (pick(e) ?? 0), 0);
  return {
    start: t0,
    end: t1,
    durationMs: t1 - t0,
    inputTokens: sum((e) => e.inputTokens),
    outputTokens: sum((e) => e.outputTokens),
    costMicros: sum((e) => e.costMicros),
    hasCost: events.some((e) => e.costMicros !== undefined),
  };
}

const TICKS = [0, 0.25, 0.5, 0.75, 1];

/**
 * The trace of one agent run: every model call, tool call and hand-off as a
 * bar on a shared time axis, totals for time, tokens and cost, and details
 * that open per event. A table view shows the same data in rows and columns.
 */
export const AiAgentRunTimeline = forwardRef<HTMLDivElement, AiAgentRunTimelineProps>(
  function AiAgentRunTimeline(
    {
      events,
      currency = 'USD',
      locale,
      now,
      view: viewProp,
      defaultView = 'timeline',
      onViewChange,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const { views: _v, kinds: _k, ...overrides } = messagesProp ?? {};
    const m = { ...AI_AGENT_RUN_TIMELINE_MESSAGES, ...overrides };
    const views = { ...AI_AGENT_RUN_TIMELINE_MESSAGES.views, ...messagesProp?.views };
    const kinds = { ...AI_AGENT_RUN_TIMELINE_MESSAGES.kinds, ...messagesProp?.kinds };
    const [viewState, setViewState] = useState<AiAgentRunTimelineView>(defaultView);
    const view = viewProp ?? viewState;
    const [open, setOpen] = useState<ReadonlySet<string>>(() => new Set());
    const baseId = useId();
    const labelId = `${baseId}-label`;

    const totals = useMemo(() => aiRunTotals(events, now), [events, now]);
    const rows: Row[] = useMemo(
      () =>
        events
          .map((event) => {
            const start = toMs(event.startedAt);
            const running = event.endedAt === undefined;
            const end = running ? totals.end : toMs(event.endedAt as DateInput);
            return { event, start, end: Math.max(start, end), running };
          })
          .sort((a, b) => a.start - b.start),
      [events, totals.end],
    );

    const numbers = new Intl.NumberFormat(locale);
    const money = (micros: number) => {
      const value = micros / 1_000_000;
      return new Intl.NumberFormat(locale, {
        style: 'currency',
        currency,
        minimumFractionDigits: 2,
        maximumFractionDigits: value !== 0 && Math.abs(value) < 1 ? 4 : 2,
      }).format(value);
    };
    const span = Math.max(1, totals.durationMs);
    const tokens = (n: number | undefined) =>
      n === undefined ? m.notAvailable : numbers.format(n);
    const cost = (n: number | undefined) => (n === undefined ? m.notAvailable : money(n));
    const durationOf = (r: Row) => (r.running ? m.running : m.formatDuration(r.end - r.start));

    const setView = (next: AiAgentRunTimelineView) => {
      if (viewProp === undefined) setViewState(next);
      onViewChange?.(next);
    };
    const toggle = (id: string) =>
      setOpen((prev) => {
        const next = new Set(prev);
        if (next.has(id)) next.delete(id);
        else next.add(id);
        return next;
      });

    const timeline = (
      <div className="mn-ai-agent-run-timeline__chart">
        <div className="mn-ai-agent-run-timeline__axis" aria-hidden>
          {TICKS.map((t) => (
            <span
              key={t}
              className="mn-ai-agent-run-timeline__tick"
              data-edge={t === 0 ? 'start' : t === 1 ? 'end' : undefined}
              style={t > 0 && t < 1 ? { insetInlineStart: `${t * 100}%` } : undefined}
            >
              {m.formatDuration(totals.durationMs * t)}
            </span>
          ))}
        </div>
        <ol className="mn-ai-agent-run-timeline__list" aria-label={m.events}>
          {rows.map((r) => {
            const e = r.event;
            const expanded = open.has(e.id);
            const panelId = `${baseId}-${e.id}`;
            const left = ((r.start - totals.start) / span) * 100;
            const width = ((r.end - r.start) / span) * 100;
            return (
              <li
                key={e.id}
                className="mn-ai-agent-run-timeline__event"
                data-kind={e.kind}
                data-running={r.running || undefined}
                data-expanded={expanded || undefined}
              >
                <button
                  type="button"
                  className="mn-ai-agent-run-timeline__toggle"
                  aria-expanded={expanded}
                  aria-controls={panelId}
                  onClick={() => toggle(e.id)}
                >
                  <ChevronRightGlyph className="mn-ai-agent-run-timeline__chevron" />
                  <span className="mn-ai-agent-run-timeline__kind">{kinds[e.kind]}</span>
                  <span className="mn-ai-agent-run-timeline__name">{e.name}</span>
                  <span className="mn-ai-agent-run-timeline__duration">{durationOf(r)}</span>
                </button>
                <span className="mn-ai-agent-run-timeline__track" aria-hidden>
                  <span
                    className="mn-ai-agent-run-timeline__bar"
                    style={{ insetInlineStart: `${left}%`, inlineSize: `${width}%` }}
                  />
                </span>
                <div id={panelId} className="mn-ai-agent-run-timeline__panel" hidden={!expanded}>
                  <dl className="mn-ai-agent-run-timeline__facts">
                    <div>
                      <dt>{m.start}</dt>
                      <dd>{m.formatOffset(r.start - totals.start)}</dd>
                    </div>
                    <div>
                      <dt>{m.duration}</dt>
                      <dd>{durationOf(r)}</dd>
                    </div>
                    {e.inputTokens !== undefined ? (
                      <div>
                        <dt>{m.inputTokens}</dt>
                        <dd>{tokens(e.inputTokens)}</dd>
                      </div>
                    ) : null}
                    {e.outputTokens !== undefined ? (
                      <div>
                        <dt>{m.outputTokens}</dt>
                        <dd>{tokens(e.outputTokens)}</dd>
                      </div>
                    ) : null}
                    {e.costMicros !== undefined ? (
                      <div>
                        <dt>{m.cost}</dt>
                        <dd>{cost(e.costMicros)}</dd>
                      </div>
                    ) : null}
                  </dl>
                  {e.detail !== undefined && e.detail !== null ? (
                    <div className="mn-ai-agent-run-timeline__detail">{e.detail}</div>
                  ) : null}
                </div>
              </li>
            );
          })}
        </ol>
      </div>
    );

    const table = (
      <div className="mn-ai-agent-run-timeline__table-wrap">
        <table className="mn-ai-agent-run-timeline__table">
          <caption className="mn-visually-hidden">{m.label}</caption>
          <thead>
            <tr>
              <th scope="col">{m.name}</th>
              <th scope="col">{m.kind}</th>
              <th scope="col">{m.start}</th>
              <th scope="col">{m.duration}</th>
              <th scope="col">{m.inputTokens}</th>
              <th scope="col">{m.outputTokens}</th>
              {totals.hasCost ? <th scope="col">{m.cost}</th> : null}
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.event.id} data-kind={r.event.kind}>
                <th scope="row">{r.event.name}</th>
                <td>{kinds[r.event.kind]}</td>
                <td data-numeric="">{m.formatOffset(r.start - totals.start)}</td>
                <td data-numeric="">{durationOf(r)}</td>
                <td data-numeric="">{tokens(r.event.inputTokens)}</td>
                <td data-numeric="">{tokens(r.event.outputTokens)}</td>
                {totals.hasCost ? <td data-numeric="">{cost(r.event.costMicros)}</td> : null}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );

    return (
      <div
        ref={ref}
        role="group"
        aria-labelledby={labelId}
        {...rest}
        className={cx('mn-ai-agent-run-timeline', className)}
        data-view={view}
      >
        <div className="mn-ai-agent-run-timeline__header">
          <span id={labelId} className="mn-ai-agent-run-timeline__label">
            {m.label}
          </span>
          <SegmentedControl<AiAgentRunTimelineView>
            size="sm"
            aria-label={m.viewLabel}
            value={view}
            onValueChange={setView}
            options={[
              { value: 'timeline', label: views.timeline },
              { value: 'table', label: views.table },
            ]}
          />
        </div>
        <dl className="mn-ai-agent-run-timeline__totals">
          <div>
            <dt>{m.totalDuration}</dt>
            <dd>{m.formatDuration(totals.durationMs)}</dd>
          </div>
          <div>
            <dt>{m.inputTokens}</dt>
            <dd>{numbers.format(totals.inputTokens)}</dd>
          </div>
          <div>
            <dt>{m.outputTokens}</dt>
            <dd>{numbers.format(totals.outputTokens)}</dd>
          </div>
          {totals.hasCost ? (
            <div>
              <dt>{m.cost}</dt>
              <dd>{money(totals.costMicros)}</dd>
            </div>
          ) : null}
        </dl>
        {rows.length === 0 ? (
          <p className="mn-ai-agent-run-timeline__empty">{m.empty}</p>
        ) : view === 'table' ? (
          table
        ) : (
          timeline
        )}
      </div>
    );
  },
);
