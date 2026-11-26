'use client';

import {
  forwardRef,
  useCallback,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { Badge } from '../../data-display/badge/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { ConfirmDialog } from '../../overlays/confirm-dialog/index.js';
import { SectionCard } from '../../layout/section-card/index.js';

/** The kind of device; picks the icon. */
export type DeviceKind = 'desktop' | 'mobile' | 'tablet' | 'unknown';

/** One device that has accessed the account. */
export interface DeviceListItem {
  /** Passed to `onRemove` and `renderActions`. */
  id: string;
  /** The device's name, e.g. "Work laptop" or "Pixel 8". */
  name: string;
  /** Picks the icon. Default `desktop`. */
  kind?: DeviceKind | undefined;
  /** Operating system, e.g. "macOS 14". */
  os?: string | null | undefined;
  /** Browser or app, e.g. "Firefox". */
  browser?: string | null | undefined;
  /** IP address or approximate location. */
  location?: string | null | undefined;
  /** When the device was last used. */
  lastUsedAt?: DateInput | null | undefined;
  /** When the device was first seen. */
  firstSeenAt?: DateInput | null | undefined;
  /** The device is blocked from signing in. */
  blocked?: boolean | undefined;
  /** This is the device the person is using now. */
  current?: boolean | undefined;
}

/** Visible strings, for translation. */
export interface DeviceListMessages {
  /** Card title. Default "Devices". */
  title: string;
  /** Line under the title. */
  description: string;
  /** Accessible name of the list. Default "Devices". */
  listLabel: string;
  /** Shown when a device has no name. Default "Unknown device". */
  unnamed: string;
  /** Badge on the current device. Default "This device". */
  current: string;
  /** Badge on a blocked device. Default "Blocked". */
  blocked: string;
  /** Before the last-used time. Default "Last used". */
  lastUsed: string;
  /** Before the first-seen time. Default "First seen". */
  firstSeen: string;
  /** Row button. Default "Remove". */
  remove: string;
  /** Accessible name of a row button. Default `Remove ${name}`. */
  removeLabel: (name: string) => string;
  /** Confirmation title. Default `Remove ${name}?`. */
  confirmTitle: (name: string) => string;
  /** Confirmation description. */
  confirmDescription: string;
  /** Default "Cancel". */
  cancel: string;
  /** Announced after a device is removed. */
  removed: (name: string) => string;
  /** Announced while loading. Default "Loading devices". */
  loading: string;
  /** Empty state title. Default "No devices yet". */
  emptyTitle: string;
  /** Empty state description. */
  emptyDescription: string;
  /** Error state title. Default "Couldn’t load your devices". */
  errorTitle: string;
}

export const defaultDeviceListMessages: DeviceListMessages = {
  title: 'Devices',
  description: 'Devices that have accessed your account.',
  listLabel: 'Devices',
  unnamed: 'Unknown device',
  current: 'This device',
  blocked: 'Blocked',
  lastUsed: 'Last used',
  firstSeen: 'First seen',
  remove: 'Remove',
  removeLabel: (name) => `Remove ${name}`,
  confirmTitle: (name) => `Remove ${name}?`,
  confirmDescription: 'It is signed out and has to sign in again to get access.',
  cancel: 'Cancel',
  removed: (name) => `Removed ${name}`,
  loading: 'Loading devices',
  emptyTitle: 'No devices yet',
  emptyDescription: 'Devices appear here when they access your account.',
  errorTitle: 'Couldn’t load your devices',
};

export interface DeviceListProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'title' | 'children'
> {
  /** The devices to list. */
  devices: DeviceListItem[];
  /** `loading`, `error` (with `onRetry`) or `ready` (default). An empty `devices` list shows the empty state. */
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
  /**
   * Removes (signs out and forgets) a device. Shows a Remove button on every
   * row except the current device. Return a promise to keep the confirmation
   * pending; a rejection shows its message in the dialog.
   */
  onRemove?: ((id: string) => unknown) | undefined;
  /**
   * Ask before removing. Default `true`. Set `false` when the product opens
   * its own confirmation (for example RevokeAccessFlow with `kind="device"`).
   */
  confirm?: boolean | undefined;
  /** Extra controls at the end of a row, e.g. a block or rename menu. */
  renderActions?: ((device: DeviceListItem) => ReactNode) | undefined;
  /** Reference time for relative times, for identical server and client output. */
  now?: DateInput | undefined;
  /** Locale for relative times. */
  locale?: string | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<DeviceListMessages> | undefined;
}

/**
 * The devices that have accessed an account: name, system and browser,
 * location, when each was first seen and last used, and blocked or current
 * badges, with an optional Remove action behind a confirmation.
 */
export const DeviceList = forwardRef<HTMLDivElement, DeviceListProps>(function DeviceList(
  {
    devices,
    state = 'ready',
    title,
    description,
    headingLevel = 'h2',
    onRetry,
    retrying = false,
    onRemove,
    confirm = true,
    renderActions,
    now,
    locale,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultDeviceListMessages, ...messages };
  const [pending, setPending] = useState<DeviceListItem | null>(null);
  const [announcement, setAnnouncement] = useState('');
  const opener = useRef<HTMLElement | null>(null);
  const rootRef = useRef<HTMLDivElement | null>(null);

  const setRefs = useCallback(
    (node: HTMLDivElement | null) => {
      rootRef.current = node;
      if (typeof ref === 'function') ref(node);
      else if (ref) ref.current = node;
    },
    [ref],
  );

  const nameOf = (device: DeviceListItem) => device.name || m.unnamed;

  const run = async (device: DeviceListItem) => {
    await onRemove?.(device.id);
    setAnnouncement(m.removed(nameOf(device)));
  };

  const start = (device: DeviceListItem, button: HTMLElement) => {
    opener.current = button;
    setAnnouncement('');
    if (confirm) setPending(device);
    else void Promise.resolve(run(device)).catch(() => undefined);
  };

  const cardState = state === 'ready' && devices.length === 0 ? 'empty' : state;
  const time = (date: DateInput) => (
    <RelativeTime
      date={date}
      tooltip={false}
      {...(now !== undefined ? { now } : {})}
      {...(locale ? { locale } : {})}
    />
  );

  return (
    <div
      ref={setRefs}
      tabIndex={-1}
      {...rest}
      className={cx('mn-device-list', className)}
      data-state={cardState}
    >
      <SectionCard
        className="mn-device-list__card"
        headingLevel={headingLevel}
        retrying={retrying}
        {...(onRetry ? { onRetry } : {})}
        title={title ?? m.title}
        description={description ?? m.description}
        state={cardState}
        loadingLabel={m.loading}
        emptyTitle={m.emptyTitle}
        emptyDescription={m.emptyDescription}
        errorTitle={m.errorTitle}
      >
        <ul className="mn-device-list__list" aria-label={m.listLabel}>
          {devices.map((device) => {
            const system = [device.os, device.browser].filter(Boolean).join(' · ');
            const extra = renderActions?.(device);
            return (
              <li
                key={device.id}
                className="mn-device-list__item"
                data-blocked={device.blocked || undefined}
                data-current={device.current || undefined}
              >
                <span className="mn-device-list__icon" aria-hidden="true">
                  <DeviceGlyph kind={device.kind ?? 'desktop'} />
                </span>
                <div className="mn-device-list__main">
                  <div className="mn-device-list__name-row">
                    <span className="mn-device-list__name">{nameOf(device)}</span>
                    {device.current ? (
                      <Badge tone="success" size="sm">
                        {m.current}
                      </Badge>
                    ) : null}
                    {device.blocked ? (
                      <Badge tone="danger" size="sm">
                        {m.blocked}
                      </Badge>
                    ) : null}
                  </div>
                  {system || device.location ? (
                    <span className="mn-device-list__detail">
                      {[system, device.location].filter(Boolean).join(' · ')}
                    </span>
                  ) : null}
                  {device.lastUsedAt || device.firstSeenAt ? (
                    <span className="mn-device-list__times">
                      {device.lastUsedAt ? (
                        <span>
                          {m.lastUsed} {time(device.lastUsedAt)}
                        </span>
                      ) : null}
                      {device.firstSeenAt ? (
                        <span>
                          {m.firstSeen} {time(device.firstSeenAt)}
                        </span>
                      ) : null}
                    </span>
                  ) : null}
                </div>
                {(onRemove && !device.current) || extra ? (
                  <div className="mn-device-list__actions">
                    {extra}
                    {onRemove && !device.current ? (
                      <Button
                        size="sm"
                        variant="ghost"
                        className="mn-device-list__remove"
                        aria-label={m.removeLabel(nameOf(device))}
                        onClick={(event) => start(device, event.currentTarget)}
                      >
                        {m.remove}
                      </Button>
                    ) : null}
                  </div>
                ) : null}
              </li>
            );
          })}
        </ul>
      </SectionCard>
      <p className="mn-device-list__status" role="status" aria-live="polite">
        {announcement}
      </p>
      {confirm && onRemove ? (
        <ConfirmDialog
          open={pending !== null}
          onOpenChange={(open) => {
            if (!open) setPending(null);
          }}
          tone="warning"
          title={pending ? m.confirmTitle(nameOf(pending)) : ''}
          description={m.confirmDescription}
          confirmLabel={m.remove}
          cancelLabel={m.cancel}
          onConfirm={() => (pending ? run(pending) : undefined)}
          onCloseAutoFocus={(event) => {
            event.preventDefault();
            const next = rootRef.current?.querySelector<HTMLElement>('.mn-device-list__remove');
            const back = opener.current?.isConnected ? opener.current : (next ?? rootRef.current);
            back?.focus();
          }}
        />
      ) : null}
    </div>
  );
});

function DeviceGlyph({ kind, ...props }: { kind: DeviceKind } & SVGProps<SVGSVGElement>) {
  const common = {
    width: 16,
    height: 16,
    viewBox: '0 0 16 16',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.5,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    ...props,
  };
  if (kind === 'mobile' || kind === 'tablet') {
    return (
      <svg {...common}>
        <rect
          x={kind === 'mobile' ? 4.5 : 2.5}
          y="1.5"
          width={kind === 'mobile' ? 7 : 11}
          height="13"
          rx="1.5"
        />
        <path d="M7.25 12.25h1.5" />
      </svg>
    );
  }
  return (
    <svg {...common}>
      <rect x="1.5" y="2.5" width="13" height="8.5" rx="1" />
      <path d="M5.5 14h5M8 11v3" />
    </svg>
  );
}
