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
import { CheckGlyph } from '../../../internal/icons.js';
import { isSafeImageSrc } from '../../../internal/sanitize.js';
import { Button } from '../../actions/button/index.js';
import { Avatar } from '../avatar/index.js';
import { Badge } from '../../data-display/badge/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { ConfirmDialog } from '../../overlays/confirm-dialog/index.js';
import { SectionCard } from '../../layout/section-card/index.js';

/** One app the person signed in to with their account, and what it can see. */
export interface ConnectedApp {
  /** Passed to `onRevoke`. */
  id: string;
  /** The app's name. Always rendered as text. */
  name: string;
  /** Logo image URL. Only `https:`, `http:`, `blob:` and relative sources are used; otherwise, or if it fails to load, the initial is shown. */
  logoSrc?: string | null | undefined;
  /** The app's domain, shown under the name, e.g. "notes.example.com". */
  domain?: string | null | undefined;
  /** The app has been reviewed by the identity provider. */
  verified?: boolean | undefined;
  /** The app is suspended and cannot use its access. */
  suspended?: boolean | undefined;
  /** The workspace the grant belongs to, when grants are per workspace. */
  workspace?: string | null | undefined;
  /** What the app can see or do, in plain words ("See your email address"). */
  scopes?: ReactNode[] | undefined;
  /** When the person first connected the app. */
  connectedAt?: DateInput | null | undefined;
  /** When the app last used its access. */
  lastUsedAt?: DateInput | null | undefined;
}

/** Visible strings, for translation. */
export interface ConnectedAppsListMessages {
  /** Card title. Default "Connected apps". */
  title: string;
  /** Line under the title. */
  description: string;
  /** Accessible name of the list. Default "Connected apps". */
  listLabel: string;
  /** Badge. Default "Verified". */
  verified: string;
  /** Badge. Default "Suspended". */
  suspended: string;
  /** Before the connected time. Default "Connected". */
  connected: string;
  /** Before the last-used time. Default "Last used". */
  lastUsed: string;
  /** Before the workspace name. Default "Workspace". */
  workspace: string;
  /** Accessible name of a scope list. Default `What ${name} can access`. */
  scopesLabel: (name: string) => string;
  /** Row button. Default "Remove access". */
  revoke: string;
  /** Accessible name of a row button. Default `Remove access for ${name}`. */
  revokeLabel: (name: string) => string;
  /** Confirmation title. Default `Remove access for ${name}?`. */
  confirmTitle: (name: string) => string;
  /** Confirmation description. */
  confirmDescription: string;
  /** Default "Cancel". */
  cancel: string;
  /** Announced after access is removed. */
  revoked: (name: string) => string;
  /** Announced while loading. Default "Loading connected apps". */
  loading: string;
  /** Empty state title. Default "No connected apps". */
  emptyTitle: string;
  /** Empty state description. */
  emptyDescription: string;
  /** Error state title. Default "Couldn’t load your connected apps". */
  errorTitle: string;
}

export const defaultConnectedAppsListMessages: ConnectedAppsListMessages = {
  title: 'Connected apps',
  description: 'Apps you signed in to with your account, and what they can see.',
  listLabel: 'Connected apps',
  verified: 'Verified',
  suspended: 'Suspended',
  connected: 'Connected',
  lastUsed: 'Last used',
  workspace: 'Workspace',
  scopesLabel: (name) => `What ${name} can access`,
  revoke: 'Remove access',
  revokeLabel: (name) => `Remove access for ${name}`,
  confirmTitle: (name) => `Remove access for ${name}?`,
  confirmDescription:
    'You’ll be signed out of this app and it won’t see your information any more. You can sign in to it again later.',
  cancel: 'Cancel',
  revoked: (name) => `Removed access for ${name}`,
  loading: 'Loading connected apps',
  emptyTitle: 'No connected apps',
  emptyDescription: 'When you sign in to an app with your account, it shows up here.',
  errorTitle: 'Couldn’t load your connected apps',
};

export interface ConnectedAppsListProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'title' | 'children'
> {
  /** The connected apps. */
  apps: ConnectedApp[];
  /** `loading`, `error` (with `onRetry`) or `ready` (default). An empty `apps` list shows the empty state. */
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
   * Removes an app's access. Shows "Remove access" on every row. Return a
   * promise to keep the confirmation pending; a rejection shows its message.
   */
  onRevoke?: ((id: string) => unknown) | undefined;
  /**
   * Ask before removing. Default `true`. Set `false` when the product opens
   * its own confirmation (for example RevokeAccessFlow) from `onRevoke`.
   */
  confirm?: boolean | undefined;
  /** Reference time for relative times, for identical server and client output. */
  now?: DateInput | undefined;
  /** Locale for relative times. */
  locale?: string | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<ConnectedAppsListMessages> | undefined;
}

/**
 * The apps a person signed in to with their account: logo, name, domain,
 * verified or suspended badges, when it was connected and last used, the
 * workspace and what it can access, with "Remove access" behind a confirmation.
 */
export const ConnectedAppsList = forwardRef<HTMLDivElement, ConnectedAppsListProps>(
  function ConnectedAppsList(
    {
      apps,
      state = 'ready',
      title,
      description,
      headingLevel = 'h2',
      onRetry,
      retrying = false,
      onRevoke,
      confirm = true,
      now,
      locale,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultConnectedAppsListMessages, ...messages };
    const [pending, setPending] = useState<ConnectedApp | null>(null);
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

    const run = async (app: ConnectedApp) => {
      await onRevoke?.(app.id);
      setAnnouncement(m.revoked(app.name));
    };

    const start = (app: ConnectedApp, button: HTMLElement) => {
      opener.current = button;
      setAnnouncement('');
      if (confirm) setPending(app);
      else void Promise.resolve(run(app)).catch(() => undefined);
    };

    const cardState = state === 'ready' && apps.length === 0 ? 'empty' : state;
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
        className={cx('mn-connected-apps-list', className)}
        data-state={cardState}
      >
        <SectionCard
          className="mn-connected-apps-list__card"
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
          <ul className="mn-connected-apps-list__list" aria-label={m.listLabel}>
            {apps.map((app) => {
              const logo = app.logoSrc && isSafeImageSrc(app.logoSrc) ? app.logoSrc : null;
              return (
                <li
                  key={app.id}
                  className="mn-connected-apps-list__item"
                  data-suspended={app.suspended || undefined}
                >
                  <div className="mn-connected-apps-list__head">
                    <Avatar
                      className="mn-connected-apps-list__logo"
                      name={app.name}
                      src={logo}
                      size="lg"
                      shape="square"
                      seed={app.id}
                      palette="tokens"
                      decorative
                    />
                    <div className="mn-connected-apps-list__main">
                      <div className="mn-connected-apps-list__name-row">
                        <span className="mn-connected-apps-list__name">{app.name}</span>
                        {app.verified ? (
                          <Badge tone="success" size="sm">
                            <CheckGlyph
                              aria-hidden="true"
                              className="mn-connected-apps-list__check"
                            />
                            {m.verified}
                          </Badge>
                        ) : null}
                        {app.suspended ? (
                          <Badge tone="warning" size="sm">
                            {m.suspended}
                          </Badge>
                        ) : null}
                      </div>
                      {app.domain ? (
                        <span className="mn-connected-apps-list__domain">{app.domain}</span>
                      ) : null}
                      {app.connectedAt || app.lastUsedAt || app.workspace ? (
                        <span className="mn-connected-apps-list__meta">
                          {app.connectedAt ? (
                            <span>
                              {m.connected} {time(app.connectedAt)}
                            </span>
                          ) : null}
                          {app.lastUsedAt ? (
                            <span>
                              {m.lastUsed} {time(app.lastUsedAt)}
                            </span>
                          ) : null}
                          {app.workspace ? (
                            <span>
                              {m.workspace}:{' '}
                              <span className="mn-connected-apps-list__workspace">
                                {app.workspace}
                              </span>
                            </span>
                          ) : null}
                        </span>
                      ) : null}
                    </div>
                    {onRevoke ? (
                      <Button
                        size="sm"
                        variant="danger-subtle"
                        className="mn-connected-apps-list__revoke"
                        aria-label={m.revokeLabel(app.name)}
                        onClick={(event) => start(app, event.currentTarget)}
                      >
                        {m.revoke}
                      </Button>
                    ) : null}
                  </div>
                  {app.scopes && app.scopes.length > 0 ? (
                    <ul
                      className="mn-connected-apps-list__scopes"
                      aria-label={m.scopesLabel(app.name)}
                    >
                      {app.scopes.map((scope, index) => (
                        <li key={index}>{scope}</li>
                      ))}
                    </ul>
                  ) : null}
                </li>
              );
            })}
          </ul>
        </SectionCard>
        <p className="mn-connected-apps-list__status" role="status" aria-live="polite">
          {announcement}
        </p>
        {confirm && onRevoke ? (
          <ConfirmDialog
            open={pending !== null}
            onOpenChange={(open) => {
              if (!open) setPending(null);
            }}
            tone="warning"
            title={pending ? m.confirmTitle(pending.name) : ''}
            description={m.confirmDescription}
            confirmLabel={m.revoke}
            cancelLabel={m.cancel}
            onConfirm={() => (pending ? run(pending) : undefined)}
            onCloseAutoFocus={(event) => {
              event.preventDefault();
              const next = rootRef.current?.querySelector<HTMLElement>(
                '.mn-connected-apps-list__revoke',
              );
              const back = opener.current?.isConnected ? opener.current : (next ?? rootRef.current);
              back?.focus();
            }}
          />
        ) : null}
      </div>
    );
  },
);
