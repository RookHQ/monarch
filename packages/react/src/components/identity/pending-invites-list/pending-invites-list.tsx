'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { CopyButton } from '../../actions/copy-button/index.js';
import { Badge } from '../../data-display/badge/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { SectionCard } from '../../layout/section-card/index.js';
import { useConfirm } from '../../overlays/confirm-dialog/index.js';

/** One invitation that has not been accepted yet. */
export interface PendingInvite {
  /** Passed to the callbacks. */
  id: string;
  /** Who was invited. */
  email: string;
  /** The role they will get, e.g. "Member". */
  role?: ReactNode;
  /** Who sent it. */
  invitedBy?: ReactNode;
  /** When it was (last) sent. */
  sentAt?: DateInput | undefined;
  /** When it stops working. */
  expiresAt?: DateInput | undefined;
  /** It has expired. Default: computed from `expiresAt` and `now`. */
  expired?: boolean | undefined;
  /** Extra details, e.g. an AppAccessSummary of the apps it grants. */
  detail?: ReactNode;
}

/** Visible strings, for translation. */
export interface PendingInvitesListMessages {
  /** Card title. Default "Pending invitations". */
  title: string;
  /** Accessible name of the list. Default "Pending invitations". */
  listLabel: string;
  /** Before the sender. Default "Invited by". */
  invitedBy: string;
  /** Before the sent time. Default "Sent". */
  sent: string;
  /** Before the expiry time. Default "Expires". */
  expires: string;
  /** Badge on an expired invitation. Default "Expired". */
  expired: string;
  /** Default "Resend". */
  resend: string;
  /** Accessible name of Resend. */
  resendLabel: (email: string) => string;
  /** Resend button after success. Default "Sent". */
  resent: string;
  /** Default "Revoke". */
  revoke: string;
  /** Accessible name of Revoke. */
  revokeLabel: (email: string) => string;
  /** Default "Copy link". */
  copyLink: string;
  /** Visually hidden target for the copy button, e.g. `invite link for ${email}`. */
  copyTarget: (email: string) => string;
  /** Revoke confirmation title. */
  confirmTitle: (email: string) => string;
  /** Revoke confirmation description. */
  confirmDescription: string;
  /** Revoke confirmation button. Default "Revoke invitation". */
  confirmLabel: string;
  /** Default "Cancel". */
  cancel: string;
  /** Announced after a resend. */
  resentAnnouncement: (email: string) => string;
  /** Announced after a revoke. */
  revokedAnnouncement: (email: string) => string;
  /** Shown when a resend fails without a message. */
  resendError: string;
  /** Default "Loading invitations". */
  loading: string;
  /** Default "No pending invitations". */
  emptyTitle: string;
  /** Default "Couldn’t load invitations". */
  errorTitle: string;
}

export const defaultPendingInvitesListMessages: PendingInvitesListMessages = {
  title: 'Pending invitations',
  listLabel: 'Pending invitations',
  invitedBy: 'Invited by',
  sent: 'Sent',
  expires: 'Expires',
  expired: 'Expired',
  resend: 'Resend',
  resendLabel: (email) => `Resend invitation to ${email}`,
  resent: 'Sent',
  revoke: 'Revoke',
  revokeLabel: (email) => `Revoke invitation to ${email}`,
  copyLink: 'Copy link',
  copyTarget: (email) => `invite link for ${email}`,
  confirmTitle: (email) => `Revoke the invitation to ${email}?`,
  confirmDescription: 'The link in their email will stop working. You can invite them again later.',
  confirmLabel: 'Revoke invitation',
  cancel: 'Cancel',
  resentAnnouncement: (email) => `Invitation sent again to ${email}`,
  revokedAnnouncement: (email) => `Invitation to ${email} revoked`,
  resendError: 'Couldn’t resend. Try again.',
  loading: 'Loading invitations',
  emptyTitle: 'No pending invitations',
  errorTitle: 'Couldn’t load invitations',
};

export interface PendingInvitesListProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'title' | 'children'
> {
  /** The invitations waiting to be accepted. */
  invites: PendingInvite[];
  /** `loading`, `error` (with `onRetry`) or `ready` (default). An empty list shows the empty state. */
  state?: 'ready' | 'loading' | 'error' | undefined;
  /** Retries loading in the `error` state. */
  onRetry?: (() => void) | undefined;
  /** Overrides the card title. */
  title?: ReactNode;
  /** A line under the title. */
  description?: ReactNode;
  /** Heading element of the title. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | undefined;
  /** Sends the invitation again. Return a promise for the pending state. Without it, rows have no Resend. */
  onResend?: ((id: string) => unknown) | undefined;
  /**
   * Revokes the invitation, after a confirmation. Return a promise to keep the
   * confirmation pending; a rejection shows its message there.
   */
  onRevoke?: ((id: string) => unknown) | undefined;
  /** Returns the invitation link to copy. Without it, rows have no Copy link. */
  getInviteLink?: ((id: string) => string | Promise<string>) | undefined;
  /** Reference time for relative times and expiry, for identical server and client output. */
  now?: DateInput | undefined;
  /** Locale for relative times. */
  locale?: string | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<PendingInvitesListMessages> | undefined;
}

function toTime(input: DateInput | undefined): number | null {
  if (input === undefined) return null;
  const t = input instanceof Date ? input.getTime() : new Date(input).getTime();
  return Number.isNaN(t) ? null : t;
}

type RowState = { kind: 'pending' } | { kind: 'sent' } | { kind: 'error'; message: ReactNode };

/**
 * Invitations that have not been accepted yet: who, which role, who sent it,
 * when it was sent and when it expires, with Resend, Copy link and Revoke
 * (behind a confirmation).
 */
export const PendingInvitesList = forwardRef<HTMLDivElement, PendingInvitesListProps>(
  function PendingInvitesList(
    {
      invites,
      state = 'ready',
      onRetry,
      title,
      description,
      headingLevel = 'h2',
      onResend,
      onRevoke,
      getInviteLink,
      now,
      locale,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultPendingInvitesListMessages, ...messages };
    const { confirm, element } = useConfirm();
    const [rows, setRows] = useState<Record<string, RowState>>({});
    const [announcement, setAnnouncement] = useState('');
    const mounted = useRef(true);
    const timers = useRef(new Map<string, ReturnType<typeof setTimeout>>());
    useEffect(() => {
      mounted.current = true;
      const pending = timers.current;
      return () => {
        mounted.current = false;
        pending.forEach((t) => clearTimeout(t));
      };
    }, []);

    const [clock, setClock] = useState<number | null>(() => toTime(now));
    useEffect(() => {
      setClock(toTime(now) ?? Date.now());
    }, [now]);

    const setRow = (id: string, next: RowState | null) =>
      setRows((all) => {
        if (next) return { ...all, [id]: next };
        return Object.fromEntries(Object.entries(all).filter(([key]) => key !== id));
      });

    const resend = (invite: PendingInvite) => {
      if (!onResend || rows[invite.id]?.kind === 'pending') return;
      setRow(invite.id, { kind: 'pending' });
      setAnnouncement('');
      Promise.resolve()
        .then(() => onResend(invite.id))
        .then(
          () => {
            if (!mounted.current) return;
            setRow(invite.id, { kind: 'sent' });
            setAnnouncement(m.resentAnnouncement(invite.email));
            const old = timers.current.get(invite.id);
            if (old) clearTimeout(old);
            timers.current.set(
              invite.id,
              setTimeout(() => setRow(invite.id, null), 3000),
            );
          },
          (failure: unknown) => {
            if (!mounted.current) return;
            setRow(invite.id, {
              kind: 'error',
              message:
                failure instanceof Error && failure.message ? failure.message : m.resendError,
            });
          },
        );
    };

    const revoke = async (invite: PendingInvite) => {
      if (!onRevoke) return;
      setAnnouncement('');
      const result = await confirm({
        title: m.confirmTitle(invite.email),
        description: m.confirmDescription,
        tone: 'danger',
        confirmLabel: m.confirmLabel,
        cancelLabel: m.cancel,
        onConfirm: () => onRevoke(invite.id),
      });
      if (result && mounted.current) setAnnouncement(m.revokedAnnouncement(invite.email));
    };

    const cardState = state === 'ready' && invites.length === 0 ? 'empty' : state;
    const timeProps = {
      tooltip: false,
      ...(now !== undefined ? { now } : {}),
      ...(locale ? { locale } : {}),
    };

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-pending-invites-list', className)}
        data-state={cardState}
      >
        <SectionCard
          className="mn-pending-invites-list__card"
          headingLevel={headingLevel}
          title={title ?? m.title}
          {...(description ? { description } : {})}
          {...(onRetry ? { onRetry } : {})}
          state={cardState}
          loadingLabel={m.loading}
          emptyTitle={m.emptyTitle}
          errorTitle={m.errorTitle}
        >
          <ul className="mn-pending-invites-list__list" aria-label={m.listLabel}>
            {invites.map((invite) => {
              const expiresAt = toTime(invite.expiresAt);
              const expired =
                invite.expired ?? (expiresAt !== null && clock !== null && expiresAt <= clock);
              const row = rows[invite.id];
              return (
                <li
                  key={invite.id}
                  className="mn-pending-invites-list__item"
                  data-expired={expired || undefined}
                >
                  <div className="mn-pending-invites-list__main">
                    <div className="mn-pending-invites-list__name-row">
                      <span className="mn-pending-invites-list__email">{invite.email}</span>
                      {invite.role ? (
                        <Badge size="sm" variant="outline">
                          {invite.role}
                        </Badge>
                      ) : null}
                      {expired ? (
                        <Badge size="sm" tone="warning">
                          {m.expired}
                        </Badge>
                      ) : null}
                    </div>
                    <span className="mn-pending-invites-list__meta">
                      {invite.invitedBy ? (
                        <span>
                          {m.invitedBy} {invite.invitedBy}
                        </span>
                      ) : null}
                      {invite.sentAt !== undefined ? (
                        <span>
                          {m.sent} <RelativeTime date={invite.sentAt} {...timeProps} />
                        </span>
                      ) : null}
                      {invite.expiresAt !== undefined && !expired ? (
                        <span>
                          {m.expires} <RelativeTime date={invite.expiresAt} {...timeProps} />
                        </span>
                      ) : null}
                    </span>
                    {invite.detail ? (
                      <div className="mn-pending-invites-list__detail">{invite.detail}</div>
                    ) : null}
                    {row?.kind === 'error' ? (
                      <p className="mn-pending-invites-list__error" role="alert">
                        {row.message}
                      </p>
                    ) : null}
                  </div>
                  {onResend || onRevoke || getInviteLink ? (
                    <div className="mn-pending-invites-list__actions">
                      {getInviteLink && !expired ? (
                        <CopyButton
                          size="sm"
                          variant="ghost"
                          value={() => getInviteLink(invite.id)}
                          label={m.copyLink}
                          target={m.copyTarget(invite.email)}
                        />
                      ) : null}
                      {onResend ? (
                        <Button
                          size="sm"
                          variant={expired ? 'secondary' : 'ghost'}
                          aria-label={m.resendLabel(invite.email)}
                          loading={row?.kind === 'pending'}
                          data-sent={row?.kind === 'sent' || undefined}
                          onClick={() => resend(invite)}
                        >
                          {row?.kind === 'sent' ? m.resent : m.resend}
                        </Button>
                      ) : null}
                      {onRevoke ? (
                        <Button
                          size="sm"
                          variant="danger-subtle"
                          aria-label={m.revokeLabel(invite.email)}
                          onClick={() => void revoke(invite)}
                        >
                          {m.revoke}
                        </Button>
                      ) : null}
                    </div>
                  ) : null}
                </li>
              );
            })}
          </ul>
        </SectionCard>
        <p className="mn-pending-invites-list__status" role="status" aria-live="polite">
          {announcement}
        </p>
        {element}
      </div>
    );
  },
);
