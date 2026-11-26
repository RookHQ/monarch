import { forwardRef, useId } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  DashboardGrid,
  type DashboardGridItem,
} from '../../../components/data-display/dashboard-grid/index.js';
import { Widget, type WidgetSpan } from '../../../components/data-display/widget/index.js';
import { KPIRow } from '../../../components/data-display/kpi-row/index.js';
import { InsightStrip } from '../../../components/data-display/insight-strip/index.js';
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
import {
  TodayPanel,
  type TodayPanelProps,
} from '../../../components/data-display/today-panel/index.js';
import {
  isDashboardSlotConfig,
  type DashboardSlot,
  type DashboardTemplateBaseProps,
} from '../dashboard-operational/index.js';
/** Visible and assistive-technology text. English defaults. */
export interface DashboardPipelineMessages {
  /** Accessible name of the KPI row. Default "Key figures". */
  kpis: string;
  /** Title of the insight strip. Default "Insights". */
  insights: string;
  /** Accessible name of the widget grid. Default "Dashboard sections". */
  grid: string;
  /** Title of the Pipeline section. */
  pipeline: string;
  /** Title of the Today section. */
  today: string;
  /** Title of the Waiting on you section. */
  followUps: string;
  /** Title of the Coming up section. */
  upcoming: string;
  /** Title of the Performance section. */
  performance: string;
  /** Title of the Top performers section. */
  leaders: string;
  /** Title of the Recent activity section. */
  activity: string;
}

export const defaultDashboardPipelineMessages: DashboardPipelineMessages = {
  kpis: 'Key figures',
  insights: 'Insights',
  grid: 'Dashboard sections',
  pipeline: 'Pipeline',
  today: 'Today',
  followUps: 'Waiting on you',
  upcoming: 'Coming up',
  performance: 'Performance',
  leaders: 'Top performers',
  activity: 'Recent activity',
};

export interface DashboardPipelineProps extends DashboardTemplateBaseProps {
  /** Items by stage, e.g. a stage summary or funnel. Section id `pipeline`. */
  pipeline?: DashboardSlot;
  /** Today's meetings and tasks (a TodayPanel). Section id `today`. */
  today?: TodayPanelProps | undefined;
  /** Replies, approvals and hand-offs waiting on the signed-in person. Section id `followUps`. */
  followUps?: DashboardSlot;
  /** Interviews, renewals or expected closes in the coming days. Section id `upcoming`. */
  upcoming?: DashboardSlot;
  /** What is working over the period: sources, conversion, reply rates. Section id `performance`. */
  performance?: DashboardSlot;
  /** A ranking of people, sources or campaigns. Section id `leaders`. */
  leaders?: readonly LeaderboardEntry[] | undefined;
  /** Extra Leaderboard props for `leaders`: valueLabel, format, limit, moreHref, loading. */
  leadersProps?: Partial<Omit<LeaderboardProps, 'entries'>> | undefined;
  /** Latest events across the pipeline. Section id `activity`. */
  activity?: readonly ActivityEvent[] | undefined;
  /** Extra ActivityWidget props for `activity`: limit, viewAllHref, loading, now. */
  activityProps?: Partial<Omit<ActivityWidgetProps, 'events'>> | undefined;
  messages?: Partial<DashboardPipelineMessages> | undefined;
}

/**
 * The home page of a pipeline product (deals, candidates, campaigns): the
 * pipeline by stage, today's agenda, follow-ups waiting on the person, what
 * is coming up, performance over the period, a ranking and recent activity,
 * under a header with period and actions, a KPI row and an insight strip.
 * Every section is optional. Server-component safe.
 */
export const DashboardPipeline = forwardRef<HTMLDivElement, DashboardPipelineProps>(
  function DashboardPipeline(
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
      pipeline,
      today,
      followUps,
      upcoming,
      performance,
      leaders,
      activity,
      leadersProps,
      activityProps,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultDashboardPipelineMessages, ...messages };
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
      slot('pipeline', m.pipeline, 'full', pipeline),
      today
        ? {
            id: 'today',
            label: m.today,
            span: 1,
            content: (
              <div className="mn-dashboard-pipeline__panel">
                <TodayPanel title={m.today} headingLevel={sub} {...today} />
              </div>
            ),
          }
        : null,
      slot('followUps', m.followUps, 1, followUps),
      slot('upcoming', m.upcoming, 1, upcoming),
      slot('performance', m.performance, 2, performance),
      leaders
        ? {
            id: 'leaders',
            label: m.leaders,
            span: 1,
            content: (
              <Widget title={m.leaders} headingLevel={sub}>
                <Leaderboard label={m.leaders} {...leadersProps} entries={leaders} />
              </Widget>
            ),
          }
        : null,
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
      <div ref={ref} {...rest} className={cx('mn-dashboard-pipeline', className)}>
        <header className="mn-dashboard-pipeline__header">
          <div className="mn-dashboard-pipeline__heading">
            <Title id={titleId} className="mn-dashboard-pipeline__title">
              {title}
            </Title>
            {description ? (
              <p className="mn-dashboard-pipeline__description">{description}</p>
            ) : null}
          </div>
          {scope || period || actions ? (
            <div className="mn-dashboard-pipeline__toolbar">
              {scope ? <div className="mn-dashboard-pipeline__scope">{scope}</div> : null}
              {period ? <div className="mn-dashboard-pipeline__period">{period}</div> : null}
              {actions ? <div className="mn-dashboard-pipeline__actions">{actions}</div> : null}
            </div>
          ) : null}
        </header>
        {notice ? <div className="mn-dashboard-pipeline__notice">{notice}</div> : null}
        {showKpis ? (
          <KPIRow
            label={m.kpis}
            {...kpiProps}
            className="mn-dashboard-pipeline__kpis"
            items={kpis ?? []}
          />
        ) : null}
        {showInsights ? (
          <InsightStrip
            title={m.insights}
            headingLevel={sub}
            {...insightProps}
            className="mn-dashboard-pipeline__insights"
            insights={insights ?? []}
          />
        ) : null}
        {items.length > 0 ? (
          <DashboardGrid
            className="mn-dashboard-pipeline__grid"
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
        {footer ? <div className="mn-dashboard-pipeline__footer">{footer}</div> : null}
      </div>
    );
  },
);
