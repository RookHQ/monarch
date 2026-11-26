'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  ClockGlyph,
  DashGlyph,
  ErrorGlyph,
  HelpGlyph,
  SuccessGlyph,
} from '../../../internal/icons.js';
import { useControllable } from '../../../internal/use-controllable.js';
import { Button } from '../../actions/button/index.js';
import { TreeView, type TreeViewItem } from '../../data-display/tree-view/index.js';
import { Spinner } from '../../feedback/spinner/index.js';

/** Where one agent in a multi-agent run is. */
export type AiSubagentStatus =
  'waiting' | 'working' | 'needs-input' | 'done' | 'failed' | 'stopped';

/** Every status, in the order the header summary lists them. */
export const AI_SUBAGENT_STATUSES: readonly AiSubagentStatus[] = [
  'working',
  'needs-input',
  'waiting',
  'done',
  'failed',
  'stopped',
];

/** One agent: the lead, or a sub-agent it handed work to. */
export interface AiSubagentNode {
  /** Stable, unique id across the whole tree. */
  id: string;
  /** Display name, e.g. "Researcher". Also what typeahead matches. */
  name: string;
  /** Its role or assignment, e.g. "Find the three latest filings". */
  role?: string | undefined;
  /** Where the agent is. */
  status: AiSubagentStatus;
  /** What it is doing right now, shown while it is working. */
  activity?: string | undefined;
  /** Steps finished so far. */
  stepsDone?: number | undefined;
  /** Steps planned in total; leave out when unknown. */
  stepsTotal?: number | undefined;
  /** Tool calls made so far. */
  toolCalls?: number | undefined;
  /** When it started, as an ISO 8601 string. */
  startedAt?: string | undefined;
  /** When it ended, as an ISO 8601 string. */
  endedAt?: string | undefined;
  /** What it produced or why it ended, shown once it is done, failed or stopped. */
  summary?: string | undefined;
  /** The agents it handed work to, nested to any depth. */
  children?: readonly AiSubagentNode[] | undefined;
  /**
   * Id of the conversation part that started this agent, e.g. the `toolCallId`
   * of the hand-off. Handed to `onOpenPart` and written as `data-part-id`.
   */
  partId?: string | undefined;
  /** Id of the conversation message this agent belongs to. Written as `data-message-id`. */
  messageId?: string | undefined;
}

/** One agent with its place in the tree, as returned by `flattenSubagentTree`. */
export interface AiSubagentFlatNode {
  /** The agent itself. */
  agent: AiSubagentNode;
  /** Depth, starting at 1 for the top level. */
  level: number;
  /** Id of the agent that handed it the work; `null` at the top level. */
  parentId: string | null;
  /** 1-based position among its siblings. */
  position: number;
  /** Number of siblings, itself included. */
  setSize: number;
}

/** How many agents are in each status, plus the total. */
export type AiSubagentStatusCounts = Record<AiSubagentStatus, number> & { total: number };

/** Every agent of the tree in reading order (parent first, then its children). */
export function flattenSubagentTree(agents: readonly AiSubagentNode[]): AiSubagentFlatNode[] {
  const out: AiSubagentFlatNode[] = [];
  const walk = (list: readonly AiSubagentNode[], level: number, parentId: string | null) => {
    list.forEach((agent, i) => {
      out.push({ agent, level, parentId, position: i + 1, setSize: list.length });
      if (agent.children?.length) walk(agent.children, level + 1, agent.id);
    });
  };
  walk(agents, 1, null);
  return out;
}

/** Counts the agents of the whole tree by status. */
export function countSubagentStatuses(agents: readonly AiSubagentNode[]): AiSubagentStatusCounts {
  const counts: AiSubagentStatusCounts = {
    total: 0,
    waiting: 0,
    working: 0,
    'needs-input': 0,
    done: 0,
    failed: 0,
    stopped: 0,
  };
  for (const { agent } of flattenSubagentTree(agents)) {
    counts.total += 1;
    counts[agent.status] += 1;
  }
  return counts;
}

const isActive = (status: AiSubagentStatus) => status === 'working' || status === 'needs-input';

/**
 * The ids expanded by default: every agent with sub-agents that is working or
 * needs input, or has such an agent somewhere under it, so nothing active is
 * hidden inside a closed branch.
 */
export function defaultExpandedSubagentIds(agents: readonly AiSubagentNode[]): string[] {
  const out: string[] = [];
  const walk = (agent: AiSubagentNode): boolean => {
    let active = isActive(agent.status);
    const at = out.length;
    if (agent.children?.length) {
      out.push(agent.id);
      for (const child of agent.children) active = walk(child) || active;
      if (!active) out.splice(at, 1);
    }
    return active;
  };
  agents.forEach(walk);
  return out;
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiSubagentTreeMessages {
  /** Accessible name of the tree when `label` is not given. */
  label: string;
  /** Status text shown beside each name and read with it. */
  statuses: Record<AiSubagentStatus, string>;
  /** Total in the header, e.g. "4 agents". `formatted` is the count in the locale's digits. */
  agentCount: (count: number, formatted: string) => string;
  /** One part of the header breakdown per status, e.g. "1 needs you". */
  statusCounts: Record<AiSubagentStatus, (count: number, formatted: string) => string>;
  /** Joins the total and the breakdown, e.g. "4 agents: 2 working, 1 done". */
  summary: (agents: string, breakdown: string) => string;
  /** Shown instead of the tree when there are no agents. */
  empty: string;
  /** Progress, e.g. "3 of 7 steps"; `total` is undefined when the plan length is unknown. */
  steps: (done: string, total: string | undefined, doneCount: number) => string;
  /** Tool calls, e.g. "12 tool calls". */
  toolCalls: (count: number, formatted: string) => string;
  /** Start time of an agent that has not ended, e.g. "Started 12:01". */
  started: (time: string) => string;
  /** Start and end time of an agent that has ended, e.g. "12:01 to 12:04". */
  ran: (start: string, end: string) => string;
  /** Accessible name of the action bar for the selected agent. */
  actionsFor: (name: string) => string;
  /** Stop button. */
  stop: string;
  /** Open button. */
  open: string;
  /** Respond button. */
  respond: string;
  /** Button that shows the selected agent's conversation part. Shown with `onOpenPart`. */
  openPart: string;
  /** Announced when an agent's status changes. */
  announce: (name: string, status: string) => string;
}

export const AI_SUBAGENT_TREE_MESSAGES: AiSubagentTreeMessages = {
  label: 'Agents',
  statuses: {
    waiting: 'Waiting',
    working: 'Working',
    'needs-input': 'Needs input',
    done: 'Done',
    failed: 'Failed',
    stopped: 'Stopped',
  },
  agentCount: (count, formatted) => `${formatted} ${count === 1 ? 'agent' : 'agents'}`,
  statusCounts: {
    waiting: (_, n) => `${n} waiting`,
    working: (_, n) => `${n} working`,
    'needs-input': (count, n) => `${n} ${count === 1 ? 'needs' : 'need'} you`,
    done: (_, n) => `${n} done`,
    failed: (_, n) => `${n} failed`,
    stopped: (_, n) => `${n} stopped`,
  },
  summary: (agents, breakdown) => (breakdown ? `${agents}: ${breakdown}` : agents),
  empty: 'No agents yet',
  steps: (done, total, doneCount) =>
    total === undefined
      ? `${done} ${doneCount === 1 ? 'step' : 'steps'}`
      : `${done} of ${total} steps`,
  toolCalls: (count, formatted) => `${formatted} ${count === 1 ? 'tool call' : 'tool calls'}`,
  started: (time) => `Started ${time}`,
  ran: (start, end) => `${start} to ${end}`,
  actionsFor: (name) => `Actions for ${name}`,
  stop: 'Stop',
  open: 'Open',
  respond: 'Respond',
  openPart: 'Show in conversation',
  announce: (name, status) => `${name}: ${status}`,
};

export interface AiSubagentTreeProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'onSelect'
> {
  /** The top-level agents (usually one lead), each with the sub-agents it handed work to. */
  agents: readonly AiSubagentNode[];
  /** Accessible name of the tree. Default: `messages.label` ("Agents"). */
  label?: string | undefined;
  /** Id of the selected agent (controlled); `null` for none. */
  selectedId?: string | null | undefined;
  /** Initially selected agent (uncontrolled). */
  defaultSelectedId?: string | null | undefined;
  /** Called with the agent's id when the person selects it (click, Enter or Space). */
  onSelect?: ((id: string, agent: AiSubagentNode) => void) | undefined;
  /** Expanded agent ids (controlled). */
  expandedIds?: readonly string[] | undefined;
  /**
   * Initially expanded ids (uncontrolled). Default: `defaultExpandedSubagentIds(agents)`,
   * and an agent that becomes active later is opened once, unless the person closed it.
   */
  defaultExpandedIds?: readonly string[] | undefined;
  /** Called with the new list when the person expands or collapses an agent. */
  onExpandedChange?: ((ids: string[]) => void) | undefined;
  /** Shows a Stop button for a selected agent that is waiting or working. Nothing stops until it is pressed. */
  onStop?: ((id: string, agent: AiSubagentNode) => void) | undefined;
  /** Shows an Open button for the selected agent, e.g. to show its transcript. */
  onOpen?: ((id: string, agent: AiSubagentNode) => void) | undefined;
  /** Shows a Respond button for a selected agent that needs input. */
  onRespond?: ((id: string, agent: AiSubagentNode) => void) | undefined;
  /**
   * Shows a "Show in conversation" button for a selected agent that has a
   * `partId` or `messageId`, and is called with them when it is pressed.
   */
  onOpenPart?:
    | ((
        link: { partId?: string | undefined; messageId?: string | undefined },
        agent: AiSubagentNode,
      ) => void)
    | undefined;
  /** BCP 47 locale for numbers, times and the header list. Default: the runtime's locale. */
  locale?: string | undefined;
  /** IANA time zone for start and end times; pass it for identical server rendering. */
  timeZone?: string | undefined;
  /** Replaces any of the English strings; `statuses` and `statusCounts` may be partial. */
  messages?:
    | (Partial<Omit<AiSubagentTreeMessages, 'statuses' | 'statusCounts'>> & {
        statuses?: Partial<AiSubagentTreeMessages['statuses']> | undefined;
        statusCounts?: Partial<AiSubagentTreeMessages['statusCounts']> | undefined;
      })
    | undefined;
}

function StatusIcon({ status }: { status: AiSubagentStatus }) {
  switch (status) {
    case 'working':
      return <Spinner size="sm" />;
    case 'needs-input':
      return <HelpGlyph />;
    case 'done':
      return <SuccessGlyph />;
    case 'failed':
      return <ErrorGlyph />;
    case 'stopped':
      return <DashGlyph />;
    default:
      return <ClockGlyph />;
  }
}

function toItems(agents: readonly AiSubagentNode[]): TreeViewItem[] {
  return agents.map((agent) => ({
    id: agent.id,
    label: agent.name,
    ...(agent.children?.length ? { children: toItems(agent.children) } : {}),
  }));
}

function validDate(iso: string | undefined): Date | null {
  if (!iso) return null;
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? null : date;
}

/**
 * A lead AI agent and the sub-agents it handed work to, as a tree: each
 * agent's status, what it is doing, how far it is and what it produced. The
 * person can follow the run, select an agent, and stop, open or answer it.
 * Data comes in through props; every action goes out through a callback.
 */
export const AiSubagentTree = forwardRef<HTMLDivElement, AiSubagentTreeProps>(
  function AiSubagentTree(
    {
      agents,
      label,
      selectedId: selectedProp,
      defaultSelectedId,
      onSelect,
      expandedIds: expandedProp,
      defaultExpandedIds,
      onExpandedChange,
      onStop,
      onOpen,
      onRespond,
      onOpenPart,
      locale,
      timeZone,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const { statuses: _s, statusCounts: _c, ...overrides } = messagesProp ?? {};
    const m = { ...AI_SUBAGENT_TREE_MESSAGES, ...overrides };
    const statuses = { ...AI_SUBAGENT_TREE_MESSAGES.statuses, ...messagesProp?.statuses };
    const statusCounts = {
      ...AI_SUBAGENT_TREE_MESSAGES.statusCounts,
      ...messagesProp?.statusCounts,
    };

    const flat = useMemo(() => flattenSubagentTree(agents), [agents]);
    const byId = useMemo(() => new Map(flat.map((f) => [f.agent.id, f.agent])), [flat]);
    const items = useMemo(() => toItems(agents), [agents]);

    const number = useMemo(() => new Intl.NumberFormat(locale), [locale]);
    const time = useMemo(
      () =>
        new Intl.DateTimeFormat(locale, {
          hour: 'numeric',
          minute: '2-digit',
          ...(timeZone ? { timeZone } : {}),
        }),
      [locale, timeZone],
    );

    // Selection.
    const [selectedId, setSelectedId] = useControllable<string | null>(
      selectedProp,
      defaultSelectedId ?? null,
    );
    const selected = selectedId === null ? undefined : byId.get(selectedId);

    // Expansion. Uncontrolled and without an explicit default, an agent that is (or
    // becomes) active is opened once; after that the person's choice stands.
    const automatic = expandedProp === undefined && defaultExpandedIds === undefined;
    const [innerExpanded, setInnerExpanded] = useState<readonly string[]>(
      () => defaultExpandedIds ?? defaultExpandedSubagentIds(agents),
    );
    const opened = useRef<Set<string> | null>(null);
    opened.current ??= new Set(innerExpanded);
    useEffect(() => {
      if (!automatic) return;
      const seen = opened.current as Set<string>;
      const fresh = defaultExpandedSubagentIds(agents).filter((id) => !seen.has(id));
      if (!fresh.length) return;
      for (const id of fresh) seen.add(id);
      setInnerExpanded((current) => [...current, ...fresh]);
    }, [agents, automatic]);
    const expanded = expandedProp ?? innerExpanded;
    const handleExpanded = useCallback(
      (next: string[]) => {
        for (const id of next) opened.current?.add(id);
        if (expandedProp === undefined) setInnerExpanded(next);
        onExpandedChange?.(next);
      },
      [expandedProp, onExpandedChange],
    );

    // Announce each status change once; never the statuses of the first render. An agent
    // that joins already needing input or failed is news too.
    const known = useRef<Map<string, AiSubagentStatus> | null>(null);
    const [live, setLive] = useState('');
    useEffect(() => {
      const before = known.current;
      known.current = new Map(flat.map((f) => [f.agent.id, f.agent.status]));
      if (!before) return;
      const news = flat.filter(({ agent }) => {
        const previous = before.get(agent.id);
        return previous === undefined
          ? agent.status === 'needs-input' || agent.status === 'failed'
          : previous !== agent.status;
      });
      if (news.length) {
        setLive(news.map(({ agent }) => m.announce(agent.name, statuses[agent.status])).join(' '));
      }
      // Only a change in the agents is news; the message functions are read when it happens.
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [flat]);

    // Header: "4 agents: 2 working, 1 needs you, 1 done".
    const counts = countSubagentStatuses(agents);
    const parts = AI_SUBAGENT_STATUSES.filter((s) => counts[s] > 0).map((s) =>
      statusCounts[s](counts[s], number.format(counts[s])),
    );
    const breakdown = new Intl.ListFormat(locale, { type: 'unit', style: 'short' }).format(parts);
    const summary = counts.total
      ? m.summary(m.agentCount(counts.total, number.format(counts.total)), breakdown)
      : m.empty;

    const rootRef = useRef<HTMLDivElement | null>(null);
    const setRoot = (el: HTMLDivElement | null) => {
      rootRef.current = el;
      if (typeof ref === 'function') ref(el);
      else if (ref) ref.current = el;
    };
    // Stop removes its own button once the agent stops, so focus goes back to the agent's row.
    const focusRow = (id: string) => {
      const rows = rootRef.current?.querySelectorAll<HTMLElement>('[role="treeitem"]') ?? [];
      for (const row of rows) if (row.dataset.id === id) row.focus();
    };

    const renderAgent = (item: TreeViewItem): ReactNode => {
      const agent = byId.get(item.id);
      if (!agent) return item.label;
      const meta: ReactNode[] = [];
      if (agent.stepsDone !== undefined) {
        meta.push(
          <span key="steps" className="mn-ai-subagent-tree__steps">
            {m.steps(
              number.format(agent.stepsDone),
              agent.stepsTotal === undefined ? undefined : number.format(agent.stepsTotal),
              agent.stepsDone,
            )}
          </span>,
        );
      }
      if (agent.toolCalls !== undefined) {
        meta.push(
          <span key="tools" className="mn-ai-subagent-tree__tools">
            {m.toolCalls(agent.toolCalls, number.format(agent.toolCalls))}
          </span>,
        );
      }
      const start = validDate(agent.startedAt);
      const end = validDate(agent.endedAt);
      if (start) {
        meta.push(
          <time key="time" className="mn-ai-subagent-tree__time" dateTime={start.toISOString()}>
            {end ? m.ran(time.format(start), time.format(end)) : m.started(time.format(start))}
          </time>,
        );
      }
      const ended =
        agent.status === 'done' || agent.status === 'failed' || agent.status === 'stopped';
      // The spaces keep the parts apart in the row's accessible name; the layout hides them.
      return (
        <span
          className="mn-ai-subagent-tree__agent"
          data-status={agent.status}
          data-part-id={agent.partId}
          data-message-id={agent.messageId}
        >
          <span className="mn-ai-subagent-tree__heading">
            <bdi className="mn-ai-subagent-tree__name">{agent.name}</bdi>{' '}
            <span className="mn-ai-subagent-tree__status" data-status={agent.status}>
              <span className="mn-ai-subagent-tree__status-icon" aria-hidden="true">
                <StatusIcon status={agent.status} />
              </span>
              {statuses[agent.status]}
            </span>
          </span>
          {agent.role ? ' ' : null}
          {agent.role ? (
            <span className="mn-ai-subagent-tree__role" dir="auto">
              {agent.role}
            </span>
          ) : null}
          {agent.status === 'working' && agent.activity ? ' ' : null}
          {agent.status === 'working' && agent.activity ? (
            <span className="mn-ai-subagent-tree__activity" dir="auto">
              {agent.activity}
            </span>
          ) : null}
          {meta.length ? ' ' : null}
          {meta.length ? (
            <span className="mn-ai-subagent-tree__meta">
              {meta.flatMap((node, i) => (i ? [' ', node] : [node]))}
            </span>
          ) : null}
          {ended && agent.summary ? ' ' : null}
          {ended && agent.summary ? (
            <span className="mn-ai-subagent-tree__summary-text" dir="auto">
              {agent.summary}
            </span>
          ) : null}
        </span>
      );
    };

    const canStop =
      selected && onStop && (selected.status === 'waiting' || selected.status === 'working');
    const canRespond = selected && onRespond && selected.status === 'needs-input';
    const canOpen = selected && onOpen;
    const canOpenPart =
      selected && onOpenPart && (selected.partId !== undefined || selected.messageId !== undefined);

    return (
      <div
        ref={setRoot}
        {...rest}
        className={cx('mn-ai-subagent-tree', className)}
        data-empty={counts.total === 0 || undefined}
      >
        <p className="mn-ai-subagent-tree__summary">{summary}</p>
        {counts.total ? (
          <TreeView
            className="mn-ai-subagent-tree__tree"
            variant="outline"
            label={label ?? m.label}
            items={items}
            expanded={expanded}
            onExpandedChange={handleExpanded}
            selected={selected ? [selected.id] : []}
            onSelectedChange={(ids) => {
              const id = ids[0];
              const agent = id === undefined ? undefined : byId.get(id);
              if (id === undefined || !agent) return;
              setSelectedId(id);
              onSelect?.(id, agent);
            }}
            renderItem={renderAgent}
          />
        ) : null}
        {selected && (canStop || canRespond || canOpen || canOpenPart) ? (
          <div
            className="mn-ai-subagent-tree__actions"
            role="group"
            aria-label={m.actionsFor(selected.name)}
          >
            <bdi className="mn-ai-subagent-tree__actions-name">{selected.name}</bdi>
            <span className="mn-ai-subagent-tree__actions-buttons">
              {canRespond ? (
                <Button
                  size="sm"
                  variant="primary"
                  onClick={() => onRespond?.(selected.id, selected)}
                >
                  {m.respond}
                </Button>
              ) : null}
              {canOpen ? (
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => onOpen?.(selected.id, selected)}
                >
                  {m.open}
                </Button>
              ) : null}
              {canOpenPart ? (
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() =>
                    onOpenPart?.(
                      { partId: selected.partId, messageId: selected.messageId },
                      selected,
                    )
                  }
                >
                  {m.openPart}
                </Button>
              ) : null}
              {canStop ? (
                <Button
                  size="sm"
                  variant="danger-subtle"
                  onClick={() => {
                    focusRow(selected.id);
                    onStop?.(selected.id, selected);
                  }}
                >
                  {m.stop}
                </Button>
              ) : null}
            </span>
          </div>
        ) : null}
        <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
          {live}
        </span>
      </div>
    );
  },
);
