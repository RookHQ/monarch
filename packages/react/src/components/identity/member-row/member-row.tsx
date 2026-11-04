'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { Badge } from '../../data-display/badge/index.js';
import { NativeSelect, type NativeSelectOption } from '../../forms/form-field/index.js';
import { ConfirmDialog } from '../../overlays/confirm-dialog/index.js';
import { Avatar, type AvatarStatus } from '../avatar/index.js';

/** Where a member is in their lifecycle. */
export type MemberStatus = 'invited' | 'active' | 'suspended';

/** Visible and announced strings. Override them to translate. */
export interface MemberRowMessages {
  /** After the name of the signed-in person. */
  you: string;
  invited: string;
  suspended: string;
  /** Hidden text for the presence dot, read after the name. */
  presence: Record<AvatarStatus, string>;
  /** Accessible name of the role select. */
  roleLabel: (name: string) => string;
  resend: string;
  /** Announced after `onResend` succeeds. */
  resent: string;
  /** Accessible name of the remove button for an active or suspended member. */
  remove: (name: string) => string;
  /** Accessible name of the remove button for an invitation. */
  revoke: (name: string) => string;
  removeTitle: (name: string) => string;
  revokeTitle: (name: string) => string;
  removeDescription: string;
  revokeDescription: string;
  removeConfirm: string;
  revokeConfirm: string;
  cancel: string;
  /** Shown when an action rejects without an Error message. */
  failed: string;
}

const DEFAULT_MESSAGES: MemberRowMessages = {
  you: 'You',
  invited: 'Invited',
  suspended: 'Suspended',
  presence: { online: 'Online', away: 'Away', busy: 'Busy', offline: 'Offline' },
  roleLabel: (name) => `Role for ${name}`,
  resend: 'Resend invite',
  resent: 'Invite sent again.',
  remove: (name) => `Remove ${name}`,
  revoke: (name) => `Revoke invite for ${name}`,
  removeTitle: (name) => `Remove ${name}?`,
  revokeTitle: (name) => `Revoke the invite for ${name}?`,
  removeDescription: 'They lose access straight away. You can invite them again later.',
  revokeDescription: 'The invite link stops working. You can invite them again later.',
  removeConfirm: 'Remove',
  revokeConfirm: 'Revoke invite',
  cancel: 'Cancel',
  failed: 'That did not work. Try again.',
};

function TrashGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable={false}
      {...props}
    >
      <path d="M2.75 4.25h10.5M6.25 4.25V2.75h3.5v1.5M4.25 4.25l.6 9h6.3l.6-9M6.75 6.75v4M9.25 6.75v4" />
    </svg>
  );
}

function messageOf(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}

export interface MemberRowProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** Display name. Also seeds the avatar initials and colour. */
  name: string;
  /** Email or handle, shown under the name. */
  email?: ReactNode;
  /** Avatar image URL; initials are shown without it or when it fails. */
  avatarUrl?: string | null;
  /** Presence dot on the avatar. */
  presence?: AvatarStatus;
  /** A line of extra detail under the email: joined date, invite expiry, availability. */
  meta?: ReactNode;
  /** Extra markers after the name, such as a Guest badge. */
  badges?: ReactNode;
  /** The row is the signed-in person: shows "You" and hides Remove. */
  isSelf?: boolean;
  /** Default `active`. `invited` and `suspended` show a badge and change the actions. */
  status?: MemberStatus;
  /** The member's role (controlled), one of `roleOptions`' values. Not the ARIA `role`. */
  memberRole?: string;
  /** Initial member role (uncontrolled). */
  defaultMemberRole?: string;
  /** The roles the product allows here. Their labels also show for read-only roles. */
  roleOptions?: readonly NativeSelectOption[];
  /**
   * Makes the role a select. Return a promise to show it busy; reject to
   * restore the previous role and show the error in the row.
   */
  onMemberRoleChange?: (role: string) => unknown;
  /** Show the role select. Default: when `onMemberRoleChange` is given and it is not your own row. */
  canChangeRole?: boolean;
  /** Resend an invitation. Shown for `status="invited"`. */
  onResend?: () => unknown;
  /**
   * Remove the member (or revoke the invitation), after a confirmation dialog.
   * Return a promise to keep the dialog pending; reject to show the error there.
   */
  onRemove?: () => unknown;
  /** Show Remove. Default: when `onRemove` is given and it is not your own row. */
  canRemove?: boolean;
  /** Consequences listed in the remove dialog ("3 assigned tickets go back to the queue"). */
  removeConsequences?: ReactNode[];
  /** More trailing actions, such as a DropdownMenu. Name them after the member. */
  actions?: ReactNode;
  /** Disable every control in the row. */
  disabled?: boolean;
  /** Element to render. Default `li`, for use inside `MemberList`. */
  as?: 'li' | 'div';
  /** Override visible and announced strings. */
  messages?: Partial<MemberRowMessages>;
}

/**
 * One member of a workspace, space or channel: avatar, name, email, role,
 * status (invited, active, suspended) and actions. Role changes, resends and
 * removals go through product callbacks; removal is confirmed first. Use
 * inside `MemberList`.
 */
export const MemberRow = forwardRef<HTMLElement, MemberRowProps>(function MemberRow(
  {
    name,
    email,
    avatarUrl,
    presence,
    meta,
    badges,
    isSelf = false,
    status = 'active',
    memberRole: roleProp,
    defaultMemberRole,
    roleOptions: roles = [],
    onMemberRoleChange: onRoleChange,
    canChangeRole,
    onResend,
    onRemove,
    canRemove,
    removeConsequences,
    actions,
    disabled = false,
    as: Tag = 'li',
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const [innerRole, setInnerRole] = useState(defaultMemberRole);
  const controlled = roleProp !== undefined;
  const role = controlled ? roleProp : innerRole;
  const [rolePending, setRolePending] = useState(false);
  const [resendPending, setResendPending] = useState(false);
  const [notice, setNotice] = useState<{ tone: 'error' | 'done'; text: string } | null>(null);
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const editable = (canChangeRole ?? (onRoleChange !== undefined && !isSelf)) && roles.length > 0;
  const removable = (canRemove ?? (onRemove !== undefined && !isSelf)) && onRemove !== undefined;
  const invited = status === 'invited';
  const roleLabel = roles.find((r) => r.value === role)?.label ?? role;

  const changeRole = async (next: string) => {
    const previous = role;
    if (!controlled) setInnerRole(next);
    setNotice(null);
    setRolePending(true);
    try {
      await onRoleChange?.(next);
    } catch (error) {
      if (!mounted.current) return;
      if (!controlled) setInnerRole(previous);
      setNotice({ tone: 'error', text: messageOf(error, messages.failed) });
    } finally {
      if (mounted.current) setRolePending(false);
    }
  };

  const resend = async () => {
    if (!onResend || resendPending) return;
    setNotice(null);
    setResendPending(true);
    try {
      await onResend();
      if (mounted.current) setNotice({ tone: 'done', text: messages.resent });
    } catch (error) {
      if (mounted.current) setNotice({ tone: 'error', text: messageOf(error, messages.failed) });
    } finally {
      if (mounted.current) setResendPending(false);
    }
  };

  return (
    <Tag
      ref={ref as never}
      {...rest}
      className={cx('mn-member-row', className)}
      data-status={status}
      data-self={isSelf || undefined}
    >
      <Avatar
        className="mn-member-row__avatar"
        name={name}
        src={avatarUrl ?? null}
        size="md"
        decorative
        {...(presence ? { status: presence } : {})}
      />
      <div className="mn-member-row__identity">
        <p className="mn-member-row__name-line">
          <span className="mn-member-row__name">{name}</span>
          {presence ? (
            <span className="mn-visually-hidden"> ({messages.presence[presence]})</span>
          ) : null}
          {isSelf ? <span className="mn-member-row__self">({messages.you})</span> : null}
          {status !== 'active' ? (
            <Badge size="sm" tone={invited ? 'info' : 'warning'} className="mn-member-row__status">
              {invited ? messages.invited : messages.suspended}
            </Badge>
          ) : null}
          {badges}
        </p>
        {email ? <p className="mn-member-row__email">{email}</p> : null}
        {meta ? <p className="mn-member-row__meta">{meta}</p> : null}
        {/* Kept rendered so the confirmation is announced when it appears. */}
        <p className="mn-member-row__notice" role="status">
          {notice?.tone === 'done' ? notice.text : null}
        </p>
        {notice?.tone === 'error' ? (
          <p className="mn-member-row__notice" role="alert" data-tone="error">
            {notice.text}
          </p>
        ) : null}
      </div>
      <div className="mn-member-row__controls">
        {editable ? (
          <NativeSelect
            size="sm"
            aria-label={messages.roleLabel(name)}
            value={role ?? ''}
            options={roles}
            {...(role === undefined ? { placeholder: '' } : {})}
            disabled={disabled || rolePending}
            aria-busy={rolePending || undefined}
            onChange={(e) => void changeRole(e.target.value)}
            wrapperClassName="mn-member-row__role-select"
          />
        ) : roleLabel ? (
          <span className="mn-member-row__role">{roleLabel}</span>
        ) : null}
        {invited && onResend ? (
          <Button
            size="sm"
            variant="ghost"
            loading={resendPending}
            disabled={disabled}
            onClick={resend}
            aria-label={`${messages.resend}, ${name}`}
          >
            {messages.resend}
          </Button>
        ) : null}
        {actions}
        {removable ? (
          <ConfirmDialog
            tone="danger"
            title={invited ? messages.revokeTitle(name) : messages.removeTitle(name)}
            description={invited ? messages.revokeDescription : messages.removeDescription}
            {...(removeConsequences ? { consequences: removeConsequences } : {})}
            confirmLabel={invited ? messages.revokeConfirm : messages.removeConfirm}
            cancelLabel={messages.cancel}
            onConfirm={() => onRemove?.()}
            trigger={
              <IconButton
                size="sm"
                icon={<TrashGlyph />}
                label={invited ? messages.revoke(name) : messages.remove(name)}
                disabled={disabled}
                className="mn-member-row__remove"
              />
            }
          />
        ) : null}
      </div>
    </Tag>
  );
});

export type MemberListProps = HTMLAttributes<HTMLUListElement>;

/** The list that holds MemberRows, so screen readers announce "list, 12 items". */
export const MemberList = forwardRef<HTMLUListElement, MemberListProps>(function MemberList(
  { className, ...rest },
  ref,
) {
  return <ul ref={ref} {...rest} className={cx('mn-member-list', className)} />;
});
