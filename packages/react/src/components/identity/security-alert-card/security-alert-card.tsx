'use client';

import {
  forwardRef,
  useCallback,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  BellGlyph,
  CloseGlyph,
  ErrorGlyph,
  InfoGlyph,
  WarningGlyph,
} from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Badge, type Tone } from '../../data-display/badge/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { SectionCard } from '../../layout/section-card/index.js';

/** How serious an alert is. Drives the icon, the badge tone and the order of attention. */
export type SecurityAlertSeverity = 'info' | 'low' | 'medium' | 'high' | 'critical';

/** One security alert, such as a sign-in from a new device or a changed password. */
export interface SecurityAlert {
  /** Passed to the callbacks. */
  id: string;
  /** What happened, e.g. "New sign-in from Firefox on Linux". */
  title: string;
  /** More detail: where, which device, what to do. */
  description?: ReactNode;
  /** Default `info`. */
  severity?: SecurityAlertSeverity | undefined;
  /** When it happened. */
  createdAt?: DateInput | undefined;
  /** The person has seen it. Unread alerts are marked and counted. */
  read?: boolean | undefined;
  /** A follow-up action, e.g. "Review sessions". Use `href` for a link. */
  action?:
    { label: string; href?: string | undefined; onClick?: (() => void) | undefined } | undefined;
}

/** Visible strings, for translation. */
export interface SecurityAlertCardMessages {
  /** Card title. Default "Security alerts". */
  title: string;
  /** Line under the title. */
  description: string;
  /** Accessible name of the list. Default "Security alerts". */
  listLabel: string;
  /** Severity names, shown in the badge. */
  severity: Record<SecurityAlertSeverity, string>;
  /** Spoken before an unread alert's title. Default "Unread". */
  unread: string;
  /** Count in the header. Default "N unread". */
  unreadCount: (count: number) => string;
  /** Row button. Default "Mark as read". */
  markRead: string;
  /** Header button. Default "Mark all as read". */
  markAllRead: string;
  /** Accessible name of the dismiss button. Default `Dismiss ${title}`. */
  dismissLabel: (title: string) => string;
  /** Announced after an alert is dismissed. */
  dismissed: (title: string) => string;
  /** Announced while loading. Default "Loading alerts". */
  loading: string;
  /** Empty state title. Default "No security alerts". */
  emptyTitle: string;
  /** Empty state description. */
  emptyDescription: string;
  /** Error state title. Default "Couldn’t load security alerts". */
  errorTitle: string;
}

export const defaultSecurityAlertCardMessages: SecurityAlertCardMessages = {
  title: 'Security alerts',
  description: 'Recent security notifications and account activity.',
  listLabel: 'Security alerts',
  severity: {
    info: 'Info',
    low: 'Low',
    medium: 'Medium',
    high: 'High',
    critical: 'Critical',
  },
  unread: 'Unread',
  unreadCount: (n) => `${n} unread`,
  markRead: 'Mark as read',
  markAllRead: 'Mark all as read',
  dismissLabel: (title) => `Dismiss ${title}`,
  dismissed: (title) => `Dismissed ${title}`,
  loading: 'Loading alerts',
  emptyTitle: 'No security alerts',
  emptyDescription: 'Alerts about sign-ins and account changes appear here.',
  errorTitle: 'Couldn’t load security alerts',
};

const TONE: Record<SecurityAlertSeverity, Tone> = {
  info: 'info',
  low: 'neutral',
  medium: 'warning',
  high: 'danger',
  critical: 'danger',
};

function SeverityGlyph({ severity }: { severity: SecurityAlertSeverity }) {
  if (severity === 'high' || severity === 'critical') return <ErrorGlyph />;
  if (severity === 'medium') return <WarningGlyph />;
  return <InfoGlyph />;
}

export interface SecurityAlertCardProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'title' | 'children'
> {
  /** The alerts, newest first. */
  alerts: SecurityAlert[];
  /** `loading`, `error` (with `onRetry`) or `ready` (default). An empty `alerts` list shows the empty state. */
  state?: 'ready' | 'loading' | 'error' | undefined;
  /** Overrides the card title. */
  title?: ReactNode;
  /** Overrides the line under the title. */
  description?: ReactNode;
  /** Heading element of the title. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | undefined;
  /** Retries loading in the `error` state. */
  onRetry?: (() => void) | undefined;
  /** The retry is in flight. */
  retrying?: boolean | undefined;
  /** Marks one alert read. Shows "Mark as read" on unread alerts. */
  onMarkRead?: ((id: string) => void) | undefined;
  /** Marks every alert read. Shows "Mark all as read" while any are unread. */
  onMarkAllRead?: (() => void) | undefined;
  /** Dismisses (hides) an alert. Shows a dismiss button on every alert. */
  onDismiss?: ((id: string) => void) | undefined;
  /** A trailing link in the header, e.g. a SectionHeaderLink to the full activity log. */
  link?: ReactNode;
  /** Reference time for relative times, for identical server and client output. */
  now?: DateInput | undefined;
  /** Locale for relative times. */
  locale?: string | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<Omit<SecurityAlertCardMessages, 'severity'>> & {
    severity?: Partial<Record<SecurityAlertSeverity, string>> | undefined;
  };
}

/**
 * Recent security alerts for an account (new sign-ins, password changes,
 * blocked attempts), each with a severity badge, time, unread marker and
 * optional follow-up, mark-as-read and dismiss actions.
 */
export const SecurityAlertCard = forwardRef<HTMLDivElement, SecurityAlertCardProps>(
  function SecurityAlertCard(
    {
      alerts,
      state = 'ready',
      title,
      description,
      headingLevel = 'h2',
      onRetry,
      retrying = false,
      onMarkRead,
      onMarkAllRead,
      onDismiss,
      link,
      now,
      locale,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m: SecurityAlertCardMessages = {
      ...defaultSecurityAlertCardMessages,
      ...messages,
      severity: { ...defaultSecurityAlertCardMessages.severity, ...messages?.severity },
    };
    const [announcement, setAnnouncement] = useState('');
    const rootRef = useRef<HTMLDivElement | null>(null);
    const setRefs = useCallback(
      (node: HTMLDivElement | null) => {
        rootRef.current = node;
        if (typeof ref === 'function') ref(node);
        else if (ref) ref.current = node;
      },
      [ref],
    );
    const unread = alerts.filter((a) => !a.read).length;
    const cardState = state === 'ready' && alerts.length === 0 ? 'empty' : state;

    return (
      <div
        ref={setRefs}
        tabIndex={-1}
        {...rest}
        className={cx('mn-security-alert-card', className)}
        data-state={cardState}
      >
        <SectionCard
          className="mn-security-alert-card__card"
          icon={<BellGlyph />}
          title={title ?? m.title}
          description={description ?? m.description}
          meta={cardState === 'ready' && unread > 0 ? m.unreadCount(unread) : undefined}
          headingLevel={headingLevel}
          state={cardState}
          loadingLabel={m.loading}
          emptyTitle={m.emptyTitle}
          emptyDescription={m.emptyDescription}
          errorTitle={m.errorTitle}
          retrying={retrying}
          {...(onRetry ? { onRetry } : {})}
          link={link}
          actions={
            onMarkAllRead && unread > 0 && cardState === 'ready' ? (
              <Button size="sm" variant="ghost" onClick={onMarkAllRead}>
                {m.markAllRead}
              </Button>
            ) : undefined
          }
        >
          <ul className="mn-security-alert-card__list" aria-label={m.listLabel}>
            {alerts.map((alert, index) => {
              const severity = alert.severity ?? 'info';
              return (
                <li
                  key={alert.id}
                  className="mn-security-alert-card__item"
                  data-severity={severity}
                  data-unread={!alert.read || undefined}
                >
                  <span className="mn-security-alert-card__icon" aria-hidden="true">
                    <SeverityGlyph severity={severity} />
                  </span>
                  <div className="mn-security-alert-card__main">
                    <p className="mn-security-alert-card__title">
                      {!alert.read ? (
                        <span className="mn-security-alert-card__unread">
                          <span className="mn-security-alert-card__sr">{m.unread}: </span>
                        </span>
                      ) : null}
                      {alert.title}
                    </p>
                    {alert.description ? (
                      <div className="mn-security-alert-card__description">{alert.description}</div>
                    ) : null}
                    <div className="mn-security-alert-card__meta">
                      <Badge tone={TONE[severity]} size="sm">
                        {m.severity[severity]}
                      </Badge>
                      {alert.createdAt !== undefined ? (
                        <RelativeTime
                          date={alert.createdAt}
                          tooltip={false}
                          {...(now !== undefined ? { now } : {})}
                          {...(locale ? { locale } : {})}
                        />
                      ) : null}
                    </div>
                    {alert.action || (onMarkRead && !alert.read) ? (
                      <div className="mn-security-alert-card__actions">
                        {alert.action?.href ? (
                          <Button size="sm" asChild>
                            <a href={alert.action.href} onClick={alert.action.onClick}>
                              {alert.action.label}
                            </a>
                          </Button>
                        ) : alert.action ? (
                          <Button size="sm" onClick={alert.action.onClick}>
                            {alert.action.label}
                          </Button>
                        ) : null}
                        {onMarkRead && !alert.read ? (
                          <Button size="sm" variant="ghost" onClick={() => onMarkRead(alert.id)}>
                            {m.markRead}
                          </Button>
                        ) : null}
                      </div>
                    ) : null}
                  </div>
                  {onDismiss ? (
                    <IconButton
                      size="sm"
                      className="mn-security-alert-card__dismiss"
                      label={m.dismissLabel(alert.title)}
                      icon={<CloseGlyph />}
                      onClick={() => {
                        // Keep focus in the list: the next alert's dismiss button, else the previous one.
                        onDismiss(alert.id);
                        setAnnouncement(m.dismissed(alert.title));
                        requestAnimationFrame(() => {
                          const buttons = rootRef.current?.querySelectorAll<HTMLElement>(
                            '.mn-security-alert-card__dismiss',
                          );
                          const next = buttons?.[Math.min(index, buttons.length - 1)];
                          (next ?? rootRef.current)?.focus();
                        });
                      }}
                    />
                  ) : null}
                </li>
              );
            })}
          </ul>
        </SectionCard>
        <p className="mn-security-alert-card__status" role="status" aria-live="polite">
          {announcement}
        </p>
      </div>
    );
  },
);
