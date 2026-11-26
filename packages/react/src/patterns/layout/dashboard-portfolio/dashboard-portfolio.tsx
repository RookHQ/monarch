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
  isDashboardSlotConfig,
  type DashboardSlot,
  type DashboardTemplateBaseProps,
} from '../dashboard-operational/index.js';
/** Visible and assistive-technology text. English defaults. */
export interface DashboardPortfolioMessages {
  /** Accessible name of the KPI row. Default "Key figures". */
  kpis: string;
  /** Title of the insight strip. Default "Insights". */
  insights: string;
  /** Accessible name of the widget grid. Default "Dashboard sections". */
  grid: string;
  /** Title of the Composition section. */
  composition: string;
  /** Title of the Needs attention section. */
  attention: string;
  /** Title of the Coming up section. */
  upcoming: string;
  /** Title of the Breakdown section. */
  breakdown: string;
  /** Title of the Top items section. */
  ranking: string;
  /** Title of the Recent activity section. */
  activity: string;
}

export const defaultDashboardPortfolioMessages: DashboardPortfolioMessages = {
  kpis: 'Key figures',
  insights: 'Insights',
  grid: 'Dashboard sections',
  composition: 'Composition',
  attention: 'Needs attention',
  upcoming: 'Coming up',
  breakdown: 'Breakdown',
  ranking: 'Top items',
  activity: 'Recent activity',
};

export interface DashboardPortfolioProps extends DashboardTemplateBaseProps {
  /** What the portfolio is made of, e.g. a chart by type. Section id `composition`. */
  composition?: DashboardSlot;
  /** Items with problems or missing information. Section id `attention`. */
  attention?: DashboardSlot;
  /** Renewals, expiries and deadlines in the coming months. Section id `upcoming`. */
  upcoming?: DashboardSlot;
  /** A second cut of the portfolio: by location, owner or source. Section id `breakdown`. */
  breakdown?: DashboardSlot;
  /** The largest or most important items, ranked. Section id `ranking`. */
  ranking?: readonly LeaderboardEntry[] | undefined;
  /** Extra Leaderboard props for `ranking`: valueLabel, format, limit, moreHref, loading. */
  rankingProps?: Partial<Omit<LeaderboardProps, 'entries'>> | undefined;
  /** Latest changes to the portfolio. Section id `activity`. */
  activity?: readonly ActivityEvent[] | undefined;
  /** Extra ActivityWidget props for `activity`: limit, viewAllHref, loading, now. */
  activityProps?: Partial<Omit<ActivityWidgetProps, 'events'>> | undefined;
  messages?: Partial<DashboardPortfolioMessages> | undefined;
}

/**
 * The home page of a portfolio product (assets, contracts, intellectual
 * property): what the portfolio is made of, what needs attention, what
 * comes up next (renewals, deadlines), a breakdown, the largest items and
 * recent activity, under a header with period and actions, a KPI row and an
 * insight strip. Every section is optional. Server-component safe.
 */
export const DashboardPortfolio = forwardRef<HTMLDivElement, DashboardPortfolioProps>(
  function DashboardPortfolio(
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
      composition,
      attention,
      upcoming,
      breakdown,
      ranking,
      activity,
      rankingProps,
      activityProps,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultDashboardPortfolioMessages, ...messages };
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
      slot('composition', m.composition, 2, composition),
      slot('attention', m.attention, 1, attention),
      slot('upcoming', m.upcoming, 2, upcoming),
      slot('breakdown', m.breakdown, 1, breakdown),
      ranking
        ? {
            id: 'ranking',
            label: m.ranking,
            span: 1,
            content: (
              <Widget title={m.ranking} headingLevel={sub}>
                <Leaderboard label={m.ranking} {...rankingProps} entries={ranking} />
              </Widget>
            ),
          }
        : null,
      activity
        ? {
            id: 'activity',
            label: m.activity,
            span: 2,
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
      <div ref={ref} {...rest} className={cx('mn-dashboard-portfolio', className)}>
        <header className="mn-dashboard-portfolio__header">
          <div className="mn-dashboard-portfolio__heading">
            <Title id={titleId} className="mn-dashboard-portfolio__title">
              {title}
            </Title>
            {description ? (
              <p className="mn-dashboard-portfolio__description">{description}</p>
            ) : null}
          </div>
          {scope || period || actions ? (
            <div className="mn-dashboard-portfolio__toolbar">
              {scope ? <div className="mn-dashboard-portfolio__scope">{scope}</div> : null}
              {period ? <div className="mn-dashboard-portfolio__period">{period}</div> : null}
              {actions ? <div className="mn-dashboard-portfolio__actions">{actions}</div> : null}
            </div>
          ) : null}
        </header>
        {notice ? <div className="mn-dashboard-portfolio__notice">{notice}</div> : null}
        {showKpis ? (
          <KPIRow
            label={m.kpis}
            {...kpiProps}
            className="mn-dashboard-portfolio__kpis"
            items={kpis ?? []}
          />
        ) : null}
        {showInsights ? (
          <InsightStrip
            title={m.insights}
            headingLevel={sub}
            {...insightProps}
            className="mn-dashboard-portfolio__insights"
            insights={insights ?? []}
          />
        ) : null}
        {items.length > 0 ? (
          <DashboardGrid
            className="mn-dashboard-portfolio__grid"
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
        {footer ? <div className="mn-dashboard-portfolio__footer">{footer}</div> : null}
      </div>
    );
  },
);
