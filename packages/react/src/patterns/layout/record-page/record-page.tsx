'use client';

import { forwardRef, useId, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  type TabsCountTone,
} from '../../../components/navigation/tabs/index.js';
import { RecordHeader, type RecordHeaderProps } from '../../crm/record-header/index.js';

/** One tab of the record's main column. */
export interface RecordPageTab {
  /** Stable value, used by `tab` / `onTabChange` (and URL binding). */
  value: string;
  /** Tab label. */
  label: ReactNode;
  /** Count bubble after the label, read as part of the tab name. */
  count?: ReactNode;
  countTone?: TabsCountTone | undefined;
  disabled?: boolean | undefined;
  /** The tab's main-column content. */
  content: ReactNode;
}

/** Visible and assistive-technology text. English defaults. */
export interface RecordPageMessages {
  /** Accessible name of the tab list. */
  tabsLabel: string;
  /** Accessible name of the side column. */
  asideLabel: string;
  /** Heading of the activity section. */
  activity: string;
}

export const defaultRecordPageMessages: RecordPageMessages = {
  tabsLabel: 'Record sections',
  asideLabel: 'Record details',
  activity: 'Activity',
};

export interface RecordPageProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** The record's name. The page heading (in RecordHeader). */
  title: ReactNode;
  /**
   * Everything else RecordHeader shows: avatar, status, badges, subtitle,
   * facts, actions, back link, previous/next navigator, banner, editable title.
   */
  headerProps?: Partial<Omit<RecordHeaderProps, 'title' | 'tabs'>> | undefined;
  /** Replaces the built-in RecordHeader. Tabs are then rendered under it. */
  header?: ReactNode;
  /** Tabs of the main column. Without tabs, `children` is the main column. */
  tabs?: readonly RecordPageTab[] | undefined;
  /** The selected tab (controlled). */
  tab?: string | undefined;
  /** The first selected tab (uncontrolled). Default: the first tab. */
  defaultTab?: string | undefined;
  /** Called when another tab is chosen. */
  onTabChange?: ((value: string) => void) | undefined;
  /** Main-column content when there are no tabs, or shown above every tab’s content. */
  children?: ReactNode;
  /** Side column: properties, related records, owners. Stays the same across tabs. */
  aside?: ReactNode;
  /** The record's activity or timeline (an ActivityFeed or comment thread). */
  activity?: ReactNode;
  /** Where the activity goes: under the side column (default) or under the main column. */
  activityPlacement?: 'aside' | 'main' | undefined;
  /** Heading of the activity section. Default `messages.activity`. `null` hides the heading. */
  activityTitle?: ReactNode;
  /** Width of the side column on wide screens. Default `md` (20rem). */
  asideWidth?: 'sm' | 'md' | 'lg' | undefined;
  messages?: Partial<RecordPageMessages> | undefined;
}

/**
 * A record detail page template: a RecordHeader with section tabs, the
 * selected tab's content in the main column, a side column of details and
 * an activity section. The side column moves under the main column on
 * narrow screens. The product supplies every piece of data.
 */
export const RecordPage = forwardRef<HTMLDivElement, RecordPageProps>(function RecordPage(
  {
    title,
    headerProps,
    header,
    tabs,
    tab,
    defaultTab,
    onTabChange,
    children,
    aside,
    activity,
    activityPlacement = 'aside',
    activityTitle,
    asideWidth = 'md',
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultRecordPageMessages, ...messages };
  const activityId = useId();
  const hasTabs = tabs !== undefined && tabs.length > 0;
  const [tabState, setTabState] = useState(defaultTab ?? tabs?.[0]?.value ?? '');
  const current = tab ?? tabState;
  const changeTab = (value: string) => {
    if (tab === undefined) setTabState(value);
    onTabChange?.(value);
  };

  const tabList = hasTabs ? (
    <TabsList aria-label={m.tabsLabel} scrollable className="mn-record-page__tabs">
      {tabs.map((t) => (
        <TabsTrigger
          key={t.value}
          value={t.value}
          {...(t.disabled !== undefined && { disabled: t.disabled })}
          count={t.count}
          {...(t.countTone !== undefined && { countTone: t.countTone })}
        >
          {t.label}
        </TabsTrigger>
      ))}
    </TabsList>
  ) : null;

  const headingShown = activityTitle !== null;
  const activitySection = activity ? (
    <section
      className="mn-record-page__activity"
      {...(headingShown ? { 'aria-labelledby': activityId } : { 'aria-label': m.activity })}
    >
      {headingShown ? (
        <h2 id={activityId} className="mn-record-page__section-title">
          {activityTitle ?? m.activity}
        </h2>
      ) : null}
      {activity}
    </section>
  ) : null;

  const main = (
    <div className="mn-record-page__main">
      {children}
      {hasTabs
        ? tabs.map((t) => (
            <TabsContent key={t.value} value={t.value} className="mn-record-page__panel">
              {t.content}
            </TabsContent>
          ))
        : null}
      {activityPlacement === 'main' ? activitySection : null}
    </div>
  );

  const side =
    aside || (activity && activityPlacement === 'aside') ? (
      <aside className="mn-record-page__aside" aria-label={m.asideLabel}>
        {aside}
        {activityPlacement === 'aside' ? activitySection : null}
      </aside>
    ) : null;

  const body = (
    <>
      {header !== undefined ? (
        <>
          {header}
          {tabList ? <div className="mn-record-page__tab-row">{tabList}</div> : null}
        </>
      ) : (
        <RecordHeader {...headerProps} title={title} tabs={tabList ?? undefined} />
      )}
      <div className="mn-record-page__body" data-has-aside={side ? '' : undefined}>
        {main}
        {side}
      </div>
    </>
  );

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-record-page', className)}
      data-aside-width={asideWidth}
    >
      {hasTabs ? (
        <Tabs value={current} onValueChange={changeTab} className="mn-record-page__tabs-root">
          {body}
        </Tabs>
      ) : (
        body
      )}
    </div>
  );
});
