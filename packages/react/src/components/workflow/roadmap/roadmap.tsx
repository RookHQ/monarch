'use client';

import { forwardRef, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Badge, type Tone } from '../../data-display/badge/index.js';
import {
  GanttTimeline,
  type GanttDates,
  type GanttDependency,
  type GanttItem,
  type GanttMilestone,
  type GanttTimelineMessages,
  type GanttTimelineProps,
  type GanttTone,
  type GanttZoom,
} from '../gantt-timeline/index.js';

/** Reported health of a project or initiative. */
export type RoadmapHealth = 'on-track' | 'at-risk' | 'off-track';

/** One project (or epic, release) on the roadmap. */
export interface RoadmapItem {
  id: string;
  /** Plain-text name. */
  label: string;
  /** Row heading content, such as an icon and the name. Default: `label`. */
  title?: ReactNode;
  /** Start and target dates, ISO. Either may be missing. */
  start?: string | null | undefined;
  end?: string | null | undefined;
  /** The group (initiative, theme, team) it belongs to. */
  groupId?: string | null | undefined;
  /** Completion from 0 to 100, drawn inside the bar. */
  progress?: number | undefined;
  /** Short progress text in the row, such as "12/20". */
  progressText?: ReactNode;
  /** Last reported health; `null` when none was reported. */
  health?: RoadmapHealth | null | undefined;
  /** Bar colour. Default: from health (at risk → warning, off track → danger), otherwise accent. */
  tone?: GanttTone | undefined;
}

/** A group of items, such as an initiative. */
export interface RoadmapGroup {
  id: string;
  label: ReactNode;
  start?: string | null | undefined;
  end?: string | null | undefined;
  health?: RoadmapHealth | null | undefined;
}

/** Visible strings of the roadmap itself. Timeline strings go in `timelineMessages`. */
export interface RoadmapMessages {
  health: Record<RoadmapHealth, string>;
  /** Hidden text when no health was reported. */
  noHealth: string;
  /** Count of items in a group heading. */
  count: (n: number) => string;
}

export const DEFAULT_ROADMAP_MESSAGES: RoadmapMessages = {
  health: { 'on-track': 'On track', 'at-risk': 'At risk', 'off-track': 'Off track' },
  noHealth: 'No health reported',
  count: (n) => `${n} ${n === 1 ? 'project' : 'projects'}`,
};

const HEALTH_TONE: Record<RoadmapHealth, Tone> = {
  'on-track': 'success',
  'at-risk': 'warning',
  'off-track': 'danger',
};
const HEALTH_BAR: Record<RoadmapHealth, GanttTone> = {
  'on-track': 'accent',
  'at-risk': 'warning',
  'off-track': 'danger',
};

export interface RoadmapProps extends Omit<
  GanttTimelineProps,
  'items' | 'groups' | 'messages' | 'onChange' | 'onOpen' | 'undated' | 'defaultZoom'
> {
  /** Projects to plot. */
  items: RoadmapItem[];
  /** Groups shown as heading rows, in order; items without a known group follow them. */
  groups?: RoadmapGroup[] | undefined;
  dependencies?: GanttDependency[] | undefined;
  milestones?: GanttMilestone[] | undefined;
  /** Called when a project bar is chosen. */
  onOpen?: ((item: RoadmapItem) => void) | undefined;
  /** Makes dates editable by dragging; read-only by default. */
  onChange?: ((item: RoadmapItem, dates: GanttDates) => unknown) | undefined;
  /** Zoom at first. Default `quarter`. */
  defaultZoom?: GanttZoom;
  /** Override the roadmap's own strings. */
  messages?: Partial<RoadmapMessages>;
  /** Override timeline strings (row header, zoom, navigation, table). */
  timelineMessages?: Partial<GanttTimelineMessages>;
}

/**
 * Projects over months and quarters, grouped by initiative, with progress,
 * reported health, milestones and cross-project dependencies. A read-only
 * planning view built on GanttTimeline, with the same table view and keyboard support.
 */
export const Roadmap = forwardRef<HTMLDivElement, RoadmapProps>(function Roadmap(
  {
    items,
    groups = [],
    onOpen,
    onChange,
    defaultZoom = 'quarter',
    zoomLevels = ['month', 'quarter', 'year'],
    messages: messagesProp,
    timelineMessages,
    className,
    ...rest
  },
  ref,
) {
  const messages = {
    ...DEFAULT_ROADMAP_MESSAGES,
    ...messagesProp,
    health: { ...DEFAULT_ROADMAP_MESSAGES.health, ...messagesProp?.health },
  };
  const byId = new Map(items.map((i) => [i.id, i]));

  const healthBadge = (health: RoadmapHealth | null | undefined) =>
    health ? (
      <Badge size="sm" dot tone={HEALTH_TONE[health]}>
        {messages.health[health]}
      </Badge>
    ) : health === null ? (
      <span className="mn-roadmap__no-health">
        <span aria-hidden>–</span>
        <span className="mn-visually-hidden">{messages.noHealth}</span>
      </span>
    ) : null;

  const ganttItems: GanttItem[] = items.map((i) => ({
    id: i.id,
    label: i.label,
    title: i.title,
    start: i.start ?? null,
    end: i.end ?? null,
    groupId: i.groupId ?? null,
    ...(i.progress !== undefined ? { progress: i.progress } : {}),
    tone: i.tone ?? (i.health ? HEALTH_BAR[i.health] : 'accent'),
    meta:
      i.progressText !== undefined || i.health !== undefined ? (
        <>
          {i.progressText !== undefined ? (
            <span className="mn-roadmap__progress">{i.progressText}</span>
          ) : null}
          {healthBadge(i.health)}
        </>
      ) : undefined,
  }));

  const ganttGroups = groups.map((g) => ({
    id: g.id,
    label: g.label,
    start: g.start ?? null,
    end: g.end ?? null,
    meta: (
      <>
        <span>{messages.count(items.filter((i) => i.groupId === g.id).length)}</span>
        {healthBadge(g.health)}
      </>
    ),
  }));

  return (
    <GanttTimeline
      ref={ref}
      {...rest}
      className={cx('mn-roadmap', className)}
      items={ganttItems}
      groups={ganttGroups}
      undated="row"
      defaultZoom={defaultZoom}
      zoomLevels={zoomLevels}
      messages={{ label: 'Roadmap', rowHeader: 'Project', ...timelineMessages }}
      onOpen={
        onOpen
          ? (g) => {
              const item = byId.get(g.id);
              if (item) onOpen(item);
            }
          : undefined
      }
      onChange={
        onChange
          ? (g, dates) => {
              const item = byId.get(g.id);
              return item ? onChange(item, dates) : undefined;
            }
          : undefined
      }
    />
  );
});
