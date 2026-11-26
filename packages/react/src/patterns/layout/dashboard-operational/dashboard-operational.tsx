import { forwardRef, isValidElement, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  DashboardGrid,
  type DashboardGridItem,
  type DashboardGridMove,
  type DashboardGridProps,
} from '../../../components/data-display/dashboard-grid/index.js';
import {
  Widget,
  type WidgetProps,
  type WidgetSpan,
} from '../../../components/data-display/widget/index.js';
import {
  KPIRow,
  type KPIRowItem,
  type KPIRowProps,
} from '../../../components/data-display/kpi-row/index.js';
import {
  InsightStrip,
  type InsightStripItem,
  type InsightStripProps,
} from '../../../components/data-display/insight-strip/index.js';
import {
  Leaderboard,
  type LeaderboardEntry,
  type LeaderboardProps,
} from '../../../components/data-display/leaderboard/index.js';
import {
  ActivityWidget,
  type ActivityWidgetProps,
} from '../../../components/data-display/activity-widget/index.js';
import type { ActivityEvent } from '../../../components/collaboration/activity-timeline/index.js';

/**
 * A dashboard section's content. Pass content alone to get a Widget with the
 * section's default title, or an object to set the Widget's title, actions,
 * view-all link, loading, error and empty states.
 */
export interface DashboardSlotConfig extends Omit<
  WidgetProps,
  'children' | 'title' | 'span' | 'content'
> {
  /** What the widget shows. */
  content: ReactNode;
  /** Replaces the section's default title. */
  title?: ReactNode;
  /** Columns spanned on wide screens. Default: the section's own span. */
  span?: WidgetSpan | undefined;
}

/** Content for one dashboard section: content alone, or a DashboardSlotConfig. */
export type DashboardSlot = ReactNode | DashboardSlotConfig;

/** True when a slot is a DashboardSlotConfig object rather than plain content. */
export function isDashboardSlotConfig(slot: DashboardSlot): slot is DashboardSlotConfig {
  return (
    typeof slot === 'object' &&
    slot !== null &&
    !isValidElement(slot) &&
    !Array.isArray(slot) &&
    'content' in slot
  );
}

/** Props every dashboard template shares: the header, KPI row, insight strip and grid. */
export interface DashboardTemplateBaseProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** Page title, e.g. the workspace or "Home". */
  title: ReactNode;
  /** Level of the page title; widget headings use the next level. Default 1. */
  headingLevel?: 1 | 2 | undefined;
  /** One line under the title. */
  description?: ReactNode;
  /** A scope switcher before the period, such as a Space picker. */
  scope?: ReactNode;
  /** The period control, normally a PeriodSelector. */
  period?: ReactNode;
  /** Page actions, such as Arrange or a primary "New" button. */
  actions?: ReactNode;
  /** A full-width note above the figures: a setup step, a banner, an alert. */
  notice?: ReactNode;
  /** Headline figures in a KPIRow. */
  kpis?: readonly KPIRowItem[] | undefined;
  /** Extra KPIRow props: loading, error, columns, size. */
  kpiProps?: Partial<Omit<KPIRowProps, 'items'>> | undefined;
  /** Insights in an InsightStrip under the figures. */
  insights?: readonly InsightStripItem[] | undefined;
  /** Extra InsightStrip props: onDismiss, max, layout, loading. */
  insightProps?: Partial<Omit<InsightStripProps, 'insights'>> | undefined;
  /** More grid items after the template's own sections. */
  widgets?: readonly DashboardGridItem[] | undefined;
  /** Grid columns on wide screens. Default 3. */
  columns?: DashboardGridProps['columns'];
  /** Saved order of section ids (controlled). See DashboardGrid. */
  layout?: readonly string[] | undefined;
  /** Starting order when uncontrolled. */
  defaultLayout?: readonly string[] | undefined;
  /** Called after each move in arrange mode with the new order; store it per person. */
  onLayoutChange?: ((layout: string[], move: DashboardGridMove) => void) | undefined;
  /** Arrange mode: sections get move handles. */
  editing?: boolean | undefined;
  /** DashboardGrid strings. */
  gridMessages?: DashboardGridProps['messages'];
  /** Content after the grid. */
  footer?: ReactNode;
}

/** Visible and assistive-technology text. English defaults. */
export interface DashboardOperationalMessages {
  /** Accessible name of the KPI row. Default "Key figures". */
  kpis: string;
  /** Title of the insight strip. Default "Insights". */
  insights: string;
  /** Accessible name of the widget grid. Default "Dashboard sections". */
  grid: string;
  /** Title of the Status section. */
  status: string;
  /** Title of the Needs attention section. */
  attention: string;
  /** Title of the My queue section. */
  queue: string;
  /** Title of the Trend section. */
  trend: string;
  /** Title of the Team load section. */
  workload: string;
  /** Title of the Coming up section. */
  upcoming: string;
  /** Title of the Recent activity section. */
  activity: string;
}

export const defaultDashboardOperationalMessages: DashboardOperationalMessages = {
  kpis: 'Key figures',
  insights: 'Insights',
  grid: 'Dashboard sections',
  status: 'Status',
  attention: 'Needs attention',
  queue: 'My queue',
  trend: 'Trend',
  workload: 'Team load',
  upcoming: 'Coming up',
  activity: 'Recent activity',
};

export interface DashboardOperationalProps extends DashboardTemplateBaseProps {
  /** Live status of what the product watches, e.g. a service-health summary. Section id `status`. */
  status?: DashboardSlot;
  /** What needs action now: items about to breach, open incidents. Section id `attention`. */
  attention?: DashboardSlot;
  /** The signed-in person's own open items. Section id `queue`. */
  queue?: DashboardSlot;
  /** A chart over the selected period, e.g. volume or uptime. Section id `trend`. */
  trend?: DashboardSlot;
  /** Open work per person or team, ranked. Section id `workload`. */
  workload?: readonly LeaderboardEntry[] | undefined;
  /** Extra Leaderboard props for `workload`: valueLabel, format, limit, moreHref, loading. */
  workloadProps?: Partial<Omit<LeaderboardProps, 'entries'>> | undefined;
  /** Scheduled work: maintenance windows, expiring certificates, due items. Section id `upcoming`. */
  upcoming?: DashboardSlot;
  /** Latest events across the workspace. Section id `activity`. */
  activity?: readonly ActivityEvent[] | undefined;
  /** Extra ActivityWidget props for `activity`: limit, viewAllHref, loading, now. */
  activityProps?: Partial<Omit<ActivityWidgetProps, 'events'>> | undefined;
  messages?: Partial<DashboardOperationalMessages> | undefined;
}

/**
 * The home page of an operational product (service status, a support desk):
 * live status, what needs attention now, the person's queue, a trend, team
 * load, what is coming up and recent activity, under a header with period and
 * actions, a KPI row and an insight strip. Every section is optional; leave
 * one out and the grid closes up. Server-component safe.
 */
export const DashboardOperational = forwardRef<HTMLDivElement, DashboardOperationalProps>(
  function DashboardOperational(
    {
      title,
      headingLevel = 1,
      description,
      scope,
      period,
      actions,
      notice,
      kpis,
      kpiProps,
      insights,
      insightProps,
      widgets,
      columns = 3,
      layout,
      defaultLayout,
      onLayoutChange,
      editing,
      gridMessages,
      footer,
      status,
      attention,
      queue,
      trend,
      workload,
      upcoming,
      activity,
      workloadProps,
      activityProps,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultDashboardOperationalMessages, ...messages };
    const titleId = useId();
    const Title = `h${headingLevel}` as 'h1';
    const sub = (headingLevel + 1) as 2 | 3;

    const slot = (id: string, label: string, span: WidgetSpan, value: DashboardSlot) => {
      if (value === undefined || value === null || value === false) return null;
      if (isDashboardSlotConfig(value)) {
        const { content, title: own, span: ownSpan, headingLevel: ownLevel, ...widget } = value;
        return {
          id,
          label: typeof own === 'string' ? own : label,
          span: ownSpan ?? span,
          content: (
            <Widget {...widget} title={own ?? label} headingLevel={ownLevel ?? sub}>
              {content}
            </Widget>
          ),
        };
      }
      return {
        id,
        label,
        span,
        content: (
          <Widget title={label} headingLevel={sub}>
            {value}
          </Widget>
        ),
      };
    };

    const sections: (DashboardGridItem | null)[] = [
      slot('status', m.status, 'full', status),
      slot('attention', m.attention, 2, attention),
      slot('queue', m.queue, 1, queue),
      slot('trend', m.trend, 2, trend),
      workload
        ? {
            id: 'workload',
            label: m.workload,
            span: 1,
            content: (
              <Widget title={m.workload} headingLevel={sub}>
                <Leaderboard label={m.workload} {...workloadProps} entries={workload} />
              </Widget>
            ),
          }
        : null,
      slot('upcoming', m.upcoming, 1, upcoming),
      activity
        ? {
            id: 'activity',
            label: m.activity,
            span: 1,
            content: (
              <ActivityWidget
                title={m.activity}
                headingLevel={sub}
                {...activityProps}
                events={activity}
              />
            ),
          }
        : null,
    ];
    const items = [
      ...sections.filter((item): item is DashboardGridItem => item !== null),
      ...(widgets ?? []),
    ];
    const showKpis = (kpis !== undefined && kpis.length > 0) || kpiProps?.loading === true;
    const showInsights =
      (insights !== undefined && insights.length > 0) || insightProps?.loading === true;

    return (
      <div ref={ref} {...rest} className={cx('mn-dashboard-operational', className)}>
        <header className="mn-dashboard-operational__header">
          <div className="mn-dashboard-operational__heading">
            <Title id={titleId} className="mn-dashboard-operational__title">
              {title}
            </Title>
            {description ? (
              <p className="mn-dashboard-operational__description">{description}</p>
            ) : null}
          </div>
          {scope || period || actions ? (
            <div className="mn-dashboard-operational__toolbar">
              {scope ? <div className="mn-dashboard-operational__scope">{scope}</div> : null}
              {period ? <div className="mn-dashboard-operational__period">{period}</div> : null}
              {actions ? <div className="mn-dashboard-operational__actions">{actions}</div> : null}
            </div>
          ) : null}
        </header>
        {notice ? <div className="mn-dashboard-operational__notice">{notice}</div> : null}
        {showKpis ? (
          <KPIRow
            label={m.kpis}
            {...kpiProps}
            className="mn-dashboard-operational__kpis"
            items={kpis ?? []}
          />
        ) : null}
        {showInsights ? (
          <InsightStrip
            title={m.insights}
            headingLevel={sub}
            {...insightProps}
            className="mn-dashboard-operational__insights"
            insights={insights ?? []}
          />
        ) : null}
        {items.length > 0 ? (
          <DashboardGrid
            className="mn-dashboard-operational__grid"
            aria-label={m.grid}
            items={items}
            columns={columns}
            layout={layout}
            defaultLayout={defaultLayout}
            onLayoutChange={onLayoutChange}
            editing={editing}
            messages={gridMessages}
          />
        ) : null}
        {footer ? <div className="mn-dashboard-operational__footer">{footer}</div> : null}
      </div>
    );
  },
);
