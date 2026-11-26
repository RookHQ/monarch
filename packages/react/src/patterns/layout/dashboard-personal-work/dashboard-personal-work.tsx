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
export interface DashboardPersonalWorkMessages {
  /** Accessible name of the KPI row. Default "Key figures". */
  kpis: string;
  /** Title of the insight strip. Default "Insights". */
  insights: string;
  /** Accessible name of the widget grid. Default "Dashboard sections". */
  grid: string;
  /** Title of the Today section. */
  today: string;
  /** Title of the Assigned to you section. */
  assigned: string;
  /** Title of the Due soon section. */
  dueSoon: string;
  /** Title of the Progress section. */
  progress: string;
  /** Title of the Announcements section. */
  announcements: string;
  /** Title of the Your team section. */
  team: string;
  /** Title of the Recent activity section. */
  activity: string;
}

export const defaultDashboardPersonalWorkMessages: DashboardPersonalWorkMessages = {
  kpis: 'Key figures',
  insights: 'Insights',
  grid: 'Dashboard sections',
  today: 'Today',
  assigned: 'Assigned to you',
  dueSoon: 'Due soon',
  progress: 'Progress',
  announcements: 'Announcements',
  team: 'Your team',
  activity: 'Recent activity',
};

export interface DashboardPersonalWorkProps extends DashboardTemplateBaseProps {
  /** Today's meetings and tasks (a TodayPanel). Section id `today`. */
  today?: TodayPanelProps | undefined;
  /** Open items assigned to the person, e.g. grouped by status. Section id `assigned`. */
  assigned?: DashboardSlot;
  /** Items, milestones or requests due in the coming days. Section id `dueSoon`. */
  dueSoon?: DashboardSlot;
  /** Goals, balances or completion the person tracks. Section id `progress`. */
  progress?: DashboardSlot;
  /** Notices from the organisation. Section id `announcements`. */
  announcements?: DashboardSlot;
  /** For managers: the team's requests, approvals and load. Section id `team`. */
  team?: DashboardSlot;
  /** The person's recent activity. Section id `activity`. */
  activity?: readonly ActivityEvent[] | undefined;
  /** Extra ActivityWidget props for `activity`: limit, viewAllHref, loading, now. */
  activityProps?: Partial<Omit<ActivityWidgetProps, 'events'>> | undefined;
  messages?: Partial<DashboardPersonalWorkMessages> | undefined;
}

/**
 * A person's own home page ("My work"): today's agenda and tasks, what is
 * assigned to them, what is due soon, their progress, announcements, a team
 * section for managers and recent activity, under a header with period and
 * actions, a KPI row and an insight strip. Every section is optional.
 * Server-component safe.
 */
export const DashboardPersonalWork = forwardRef<HTMLDivElement, DashboardPersonalWorkProps>(
  function DashboardPersonalWork(
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
      today,
      assigned,
      dueSoon,
      progress,
      announcements,
      team,
      activity,
      activityProps,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultDashboardPersonalWorkMessages, ...messages };
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
      today
        ? {
            id: 'today',
            label: m.today,
            span: 1,
            content: (
              <div className="mn-dashboard-personal-work__panel">
                <TodayPanel title={m.today} headingLevel={sub} {...today} />
              </div>
            ),
          }
        : null,
      slot('assigned', m.assigned, 2, assigned),
      slot('dueSoon', m.dueSoon, 1, dueSoon),
      slot('progress', m.progress, 1, progress),
      slot('announcements', m.announcements, 1, announcements),
      slot('team', m.team, 'full', team),
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
      <div ref={ref} {...rest} className={cx('mn-dashboard-personal-work', className)}>
        <header className="mn-dashboard-personal-work__header">
          <div className="mn-dashboard-personal-work__heading">
            <Title id={titleId} className="mn-dashboard-personal-work__title">
              {title}
            </Title>
            {description ? (
              <p className="mn-dashboard-personal-work__description">{description}</p>
            ) : null}
          </div>
          {scope || period || actions ? (
            <div className="mn-dashboard-personal-work__toolbar">
              {scope ? <div className="mn-dashboard-personal-work__scope">{scope}</div> : null}
              {period ? <div className="mn-dashboard-personal-work__period">{period}</div> : null}
              {actions ? (
                <div className="mn-dashboard-personal-work__actions">{actions}</div>
              ) : null}
            </div>
          ) : null}
        </header>
        {notice ? <div className="mn-dashboard-personal-work__notice">{notice}</div> : null}
        {showKpis ? (
          <KPIRow
            label={m.kpis}
            {...kpiProps}
            className="mn-dashboard-personal-work__kpis"
            items={kpis ?? []}
          />
        ) : null}
        {showInsights ? (
          <InsightStrip
            title={m.insights}
            headingLevel={sub}
            {...insightProps}
            className="mn-dashboard-personal-work__insights"
            insights={insights ?? []}
          />
        ) : null}
        {items.length > 0 ? (
          <DashboardGrid
            className="mn-dashboard-personal-work__grid"
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
        {footer ? <div className="mn-dashboard-personal-work__footer">{footer}</div> : null}
      </div>
    );
  },
);
