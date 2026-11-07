'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type ElementRef,
  type FormEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { CopyButton } from '../../actions/copy-button/index.js';
import { CopyField } from '../../actions/copy-field/index.js';
import { Combobox, type ComboboxOption } from '../../forms/combobox/index.js';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  NativeSelect,
  type NativeSelectOption,
} from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';
import { MemberList, MemberRow, type MemberStatus } from '../../identity/member-row/index.js';
import { useConfirm } from '../../overlays/confirm-dialog/index.js';
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '../../overlays/dialog/index.js';

// ------------------------------------------------------------------ types

/** Someone who already has access. */
export interface SharePerson {
  /** Stable id, passed back to `onRoleChange` and `onRemovePerson`. */
  id: string;
  /** Display name. */
  name: string;
  /** Email or handle, shown under the name. */
  email?: string | undefined;
  /** Avatar image URL. */
  avatarUrl?: string | null | undefined;
  /** One of `roles`' values. */
  role?: string | undefined;
  /** `invited` shows an Invited badge and turns Remove into "Revoke invite". Default `active`. */
  status?: MemberStatus | undefined;
  /** The signed-in person: shows "You" and cannot be removed or have their role changed. */
  isSelf?: boolean | undefined;
  /** The role cannot be changed (for example the owner). */
  roleLocked?: boolean | undefined;
  /** Cannot be removed (for example the owner). */
  removeLocked?: boolean | undefined;
}

/** One choice for who can open the item without being invited. */
export interface ShareAccessOption {
  value: string;
  /** e.g. "Only people invited" or "Anyone in the workspace". */
  label: string;
  /** What the choice means, shown under the select. */
  description?: string | undefined;
}

/** A share link, as in a list of public or guest links. */
export interface ShareLink {
  /** Stable id, passed back to `onRevokeLink`. */
  id: string;
  /** The full URL that is copied. */
  url: string;
  /** Shown instead of the URL, e.g. a shortened path. */
  label?: string | undefined;
  /** Whether the link still works. Default `active`. */
  status?: 'active' | 'expired' | 'revoked' | undefined;
  /** How many times it was opened. */
  views?: number | undefined;
  /** Whether it asks for a password. */
  hasPassword?: boolean | undefined;
  /** When it stops working. */
  expiresAt?: Date | string | number | undefined;
}

/** What `onCreateLink` receives. */
export interface ShareLinkRequest {
  /** The password typed, or undefined for none. */
  password: string | undefined;
  /** The chosen expiry option's value, or null for never. */
  expiresInDays: number | null;
}

/** One choice in the link expiry select. */
export interface ShareExpiryOption {
  /** Days until the link expires, or null for never. */
  days: number | null;
  label: string;
}

/** Every string the share dialog shows or announces. */
export interface ShareDialogMessages {
  /** Dialog title. */
  title: (itemName: string) => string;
  /** Close button label. */
  close: string;
  /** Invite field label. */
  inviteLabel: string;
  /** Invite field placeholder. */
  invitePlaceholder: string;
  /** Option offered for a typed email address. */
  inviteEmail: (email: string) => string;
  /** Role select next to the invite field. */
  inviteRoleLabel: string;
  /** Invite button. */
  invite: string;
  /** Announced after inviting. */
  invited: (count: number) => string;
  /** Heading of the people list. */
  peopleHeading: string;
  /** Heading of the general access section. */
  accessHeading: string;
  /** Label of the general access select. */
  accessLabel: string;
  /** Announced after general access changes. */
  accessChanged: (label: string) => string;
  /** Label of the main link field. */
  linkLabel: string;
  /** Heading of the share links section. */
  linksHeading: string;
  /** Password field label. */
  passwordLabel: string;
  /** Shown after the password label. */
  optional: string;
  /** Shown when the password is too short. */
  passwordTooShort: (min: number) => string;
  /** Expiry select label. */
  expiryLabel: string;
  /** Create link button. */
  createLink: string;
  /** Announced after a link is created. */
  linkCreated: string;
  /** Shown while links load. */
  linksLoading: string;
  /** Shown when there are no links. */
  noLinks: string;
  /** Views count on a link. */
  views: (count: number) => string;
  /** Marker on a password-protected link. */
  passwordProtected: string;
  /** Expiry on a link. */
  expires: (date: string) => string;
  /** Status markers on a link. */
  expired: string;
  revoked: string;
  /** Copy button on a link; receives the link label. */
  copyLink: (label: string) => string;
  /** Revoke button and its confirmation. */
  revoke: string;
  revokeLabel: (label: string) => string;
  revokeTitle: string;
  revokeDescription: string;
  revokeConfirm: string;
  cancel: string;
  /** Fallback error when a callback fails without a message. */
  failed: string;
}

export const SHARE_DIALOG_MESSAGES: ShareDialogMessages = {
  title: (name) => `Share “${name}”`,
  close: 'Close',
  inviteLabel: 'Invite people',
  invitePlaceholder: 'Name or email',
  inviteEmail: (email) => `Invite ${email}`,
  inviteRoleLabel: 'Role for new people',
  invite: 'Invite',
  invited: (n) => (n === 1 ? '1 person invited.' : `${n} people invited.`),
  peopleHeading: 'People with access',
  accessHeading: 'General access',
  accessLabel: 'Who can open it',
  accessChanged: (label) => `General access: ${label}.`,
  linkLabel: 'Link',
  linksHeading: 'Share links',
  passwordLabel: 'Password',
  optional: 'optional',
  passwordTooShort: (min) => `Use at least ${min} characters, or leave it blank.`,
  expiryLabel: 'Expires',
  createLink: 'Create link',
  linkCreated: 'Link created.',
  linksLoading: 'Loading links…',
  noLinks: 'No share links yet.',
  views: (n) => (n === 1 ? '1 view' : `${n} views`),
  passwordProtected: 'Password',
  expires: (date) => `Expires ${date}`,
  expired: 'Expired',
  revoked: 'Revoked',
  copyLink: (label) => `Copy ${label}`,
  revoke: 'Revoke',
  revokeLabel: (label) => `Revoke ${label}`,
  revokeTitle: 'Revoke this link?',
  revokeDescription: 'Anyone who opens it afterwards sees an error. You can create a new link.',
  revokeConfirm: 'Revoke link',
  cancel: 'Cancel',
  failed: 'That did not work. Try again.',
};

export const SHARE_EXPIRY_OPTIONS: readonly ShareExpiryOption[] = [
  { days: null, label: 'Never' },
  { days: 7, label: 'In 7 days' },
  { days: 30, label: 'In 30 days' },
  { days: 90, label: 'In 90 days' },
];

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function errorText(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}

/** Tracks a callback that may return a promise: pending while it runs, the error if it rejects. */
function useAction() {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);
  const run = async (fn: () => unknown, fallback: string): Promise<boolean> => {
    setError(null);
    setPending(true);
    try {
      await fn();
      return true;
    } catch (e) {
      if (mounted.current) setError(errorText(e, fallback));
      return false;
    } finally {
      if (mounted.current) setPending(false);
    }
  };
  return { pending, error, setError, run };
}

// ------------------------------------------------------------------ component

export interface ShareDialogProps {
  /** Name of what is shared, used in the title. */
  itemName: string;
  /** A sentence under the title, e.g. what people can do with the item. */
  description?: ReactNode | undefined;
  /** Whether the dialog is open (controlled). */
  open?: boolean | undefined;
  /** Initial open state (uncontrolled). */
  defaultOpen?: boolean | undefined;
  /** Called when the dialog opens or closes. */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Element that opens the dialog, such as a Share button. */
  trigger?: ReactNode | undefined;

  /** The roles the product allows, e.g. Viewer, Commenter, Editor. Used for invites and the people list. */
  roles?: readonly NativeSelectOption[] | undefined;
  /** Role preselected for new invites. Default: the first role. */
  defaultInviteRole?: string | undefined;
  /** People who can be invited, shown as suggestions. */
  inviteOptions?: readonly ComboboxOption[] | undefined;
  /** Searches people on the server as the person types. */
  onInviteSearch?:
    ((query: string, context: { signal: AbortSignal }) => Promise<ComboboxOption[]>) | undefined;
  /** Accept typed email addresses as invites. Default true. */
  allowEmailInvites?: boolean | undefined;
  /**
   * Shows the invite row. Receives the picked values (option values or typed
   * email addresses) and the role. Return a promise to show the button busy;
   * reject to show the error under the field and keep the picks.
   */
  onInvite?: ((values: string[], role: string | undefined) => unknown) | undefined;

  /** People who already have access. Shows the people list. */
  people?: readonly SharePerson[] | undefined;
  /** Changes someone's role. Return a promise to show the select busy; reject to restore it and show the error. */
  onRoleChange?: ((personId: string, role: string) => unknown) | undefined;
  /** Removes someone after a confirmation dialog. Return a promise to keep the dialog pending. */
  onRemovePerson?: ((personId: string) => unknown) | undefined;

  /** The current general access value. Shows the general access section with `accessOptions`. */
  access?: string | undefined;
  /** The general access choices. */
  accessOptions?: readonly ShareAccessOption[] | undefined;
  /** Changes general access. Return a promise to show the select busy; reject to restore it and show the error. */
  onAccessChange?: ((value: string) => unknown) | undefined;

  /** The item's own link, shown in a read-only field with a Copy button. */
  link?: string | undefined;

  /** Share links (public or guest links). Shows the links section with `onCreateLink`. */
  links?: readonly ShareLink[] | undefined;
  /** Links are loading. */
  linksLoading?: boolean | undefined;
  /**
   * Shows the create link form. Return a promise to show the button busy;
   * reject to show the error. Resolve with the new link to copy it at once.
   */
  onCreateLink?: ((request: ShareLinkRequest) => unknown) | undefined;
  /** Show a password field in the create link form. Default true. */
  linkPassword?: boolean | undefined;
  /** Shortest password accepted. Default 4. */
  passwordMinLength?: number | undefined;
  /** Expiry choices. Default `SHARE_EXPIRY_OPTIONS` (never, 7, 30, 90 days). */
  expiryOptions?: readonly ShareExpiryOption[] | undefined;
  /** Revokes a link after a confirmation dialog. Return a promise to keep the dialog pending. */
  onRevokeLink?: ((linkId: string) => unknown) | undefined;

  /** Extra content at the end of the body, such as product-specific permissions. */
  children?: ReactNode | undefined;
  /** BCP 47 locale for dates. Default: the runtime's. */
  locale?: string | undefined;
  /** Override any string. */
  messages?: Partial<ShareDialogMessages> | undefined;
  /** Class on the dialog content. */
  className?: string | undefined;
}

/**
 * The Share dialog: invite people with a role, change or remove who has
 * access, set general access, copy the link and manage share links with an
 * optional password and expiry. Every section appears only when its data and
 * callbacks are given; all changes go through product callbacks, and removals
 * and revocations are confirmed first.
 */
export const ShareDialog = forwardRef<ElementRef<typeof DialogContent>, ShareDialogProps>(
  function ShareDialog(
    {
      itemName,
      description,
      open,
      defaultOpen,
      onOpenChange,
      trigger,
      roles = [],
      defaultInviteRole,
      inviteOptions,
      onInviteSearch,
      allowEmailInvites = true,
      onInvite,
      people,
      onRoleChange,
      onRemovePerson,
      access,
      accessOptions,
      onAccessChange,
      link,
      links,
      linksLoading = false,
      onCreateLink,
      linkPassword = true,
      passwordMinLength = 4,
      expiryOptions = SHARE_EXPIRY_OPTIONS,
      onRevokeLink,
      children,
      locale,
      messages: messagesProp,
      className,
    },
    ref,
  ) {
    const m = { ...SHARE_DIALOG_MESSAGES, ...messagesProp };
    const uid = useId().replace(/:/g, '');
    const { confirm, element: confirmElement } = useConfirm();
    const [status, setStatus] = useState('');

    // ---- invite
    const [inviteValues, setInviteValues] = useState<string[]>([]);
    const [created, setCreated] = useState<ComboboxOption[]>([]);
    const [inviteRole, setInviteRole] = useState(defaultInviteRole ?? roles[0]?.value);
    const inviteAction = useAction();
    const submitInvite = async (event: FormEvent) => {
      event.preventDefault();
      if (!onInvite || !inviteValues.length || inviteAction.pending) return;
      const values = inviteValues;
      const ok = await inviteAction.run(() => onInvite(values, inviteRole), m.failed);
      if (ok) {
        setInviteValues([]);
        setCreated([]);
        setStatus(m.invited(values.length));
      }
    };

    // ---- general access
    const [accessValue, setAccessValue] = useState(access);
    useEffect(() => setAccessValue(access), [access]);
    const accessAction = useAction();
    const currentAccess = accessOptions?.find((o) => o.value === accessValue);
    const changeAccess = async (next: string) => {
      const previous = accessValue;
      setAccessValue(next);
      const ok = await accessAction.run(() => onAccessChange?.(next), m.failed);
      if (ok) {
        const label = accessOptions?.find((o) => o.value === next)?.label ?? next;
        setStatus(m.accessChanged(label));
      } else setAccessValue(previous);
    };

    // ---- links
    const [password, setPassword] = useState('');
    const [expiry, setExpiry] = useState<number | null>(expiryOptions[0]?.days ?? null);
    const createAction = useAction();
    const trimmed = password.trim();
    const passwordInvalid = trimmed.length > 0 && trimmed.length < passwordMinLength;
    const submitLink = async (event: FormEvent) => {
      event.preventDefault();
      if (!onCreateLink || passwordInvalid || createAction.pending) return;
      let result: unknown;
      const ok = await createAction.run(async () => {
        result = await onCreateLink({ password: trimmed || undefined, expiresInDays: expiry });
      }, m.failed);
      if (!ok) return;
      setPassword('');
      // Creating a link and copying it are one intent; copy the new link if the product returned it.
      const url =
        result && typeof result === 'object' && 'url' in result ? (result as ShareLink).url : null;
      if (url && typeof navigator !== 'undefined' && navigator.clipboard) {
        navigator.clipboard.writeText(url).catch(() => {});
      }
      setStatus(m.linkCreated);
    };

    const dateFormat = (value: Date | string | number) =>
      new Intl.DateTimeFormat(locale, { dateStyle: 'medium' }).format(new Date(value));

    const revoke = (item: ShareLink) =>
      void confirm({
        tone: 'danger',
        title: m.revokeTitle,
        description: m.revokeDescription,
        confirmLabel: m.revokeConfirm,
        cancelLabel: m.cancel,
        onConfirm: () => onRevokeLink?.(item.id),
      });

    const showAccess = Boolean(accessOptions?.length && accessValue !== undefined);
    const showLinks = Boolean(onCreateLink || links);

    return (
      <Dialog
        {...(open !== undefined ? { open } : {})}
        {...(defaultOpen !== undefined ? { defaultOpen } : {})}
        {...(onOpenChange ? { onOpenChange } : {})}
      >
        {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
        <DialogContent
          ref={ref}
          size="md"
          closeLabel={m.close}
          closeOnOutsideClick={false}
          fullScreenOnPhone
          className={cx('mn-share-dialog', className)}
          {...(description ? {} : { 'aria-describedby': undefined })}
        >
          <DialogHeader>
            <DialogTitle>{m.title(itemName)}</DialogTitle>
            {description ? <DialogDescription>{description}</DialogDescription> : null}
          </DialogHeader>
          <DialogBody className="mn-share-dialog__body">
            {onInvite ? (
              <form className="mn-share-dialog__invite" onSubmit={submitInvite} noValidate>
                <Field
                  invalid={Boolean(inviteAction.error)}
                  className="mn-share-dialog__invite-field"
                >
                  <FieldLabel>{m.inviteLabel}</FieldLabel>
                  <Combobox
                    multiple
                    values={inviteValues}
                    onValuesChange={(v) => {
                      setInviteValues(v);
                      inviteAction.setError(null);
                    }}
                    options={[...(inviteOptions ?? []), ...created]}
                    valueOptions={created}
                    placeholder={m.invitePlaceholder}
                    sortByMatch
                    {...(onInviteSearch ? { onSearch: onInviteSearch } : {})}
                    {...(allowEmailInvites
                      ? {
                          onCreate: (query: string) => {
                            const email = query.trim();
                            if (!EMAIL.test(email)) return null;
                            const option = { value: email, label: email };
                            setCreated((c) =>
                              c.some((o) => o.value === email) ? c : [...c, option],
                            );
                            return option;
                          },
                          messages: { create: m.inviteEmail },
                        }
                      : {})}
                  />
                  {inviteAction.error ? <FieldError>{inviteAction.error}</FieldError> : null}
                </Field>
                <div className="mn-share-dialog__invite-actions">
                  {roles.length ? (
                    <NativeSelect
                      aria-label={m.inviteRoleLabel}
                      options={roles}
                      value={inviteRole ?? ''}
                      onChange={(e) => setInviteRole(e.target.value)}
                      wrapperClassName="mn-share-dialog__invite-role"
                    />
                  ) : null}
                  <Button
                    type="submit"
                    variant="primary"
                    loading={inviteAction.pending}
                    disabled={!inviteValues.length}
                  >
                    {m.invite}
                  </Button>
                </div>
              </form>
            ) : null}

            {people?.length ? (
              <section className="mn-share-dialog__section" aria-labelledby={`${uid}-people`}>
                <h3 id={`${uid}-people`} className="mn-share-dialog__heading">
                  {m.peopleHeading}
                </h3>
                <MemberList className="mn-share-dialog__people">
                  {people.map((person) => (
                    <MemberRow
                      key={person.id}
                      name={person.name}
                      email={person.email}
                      avatarUrl={person.avatarUrl ?? null}
                      isSelf={person.isSelf ?? false}
                      status={person.status ?? 'active'}
                      {...(person.role !== undefined ? { memberRole: person.role } : {})}
                      roleOptions={roles}
                      {...(onRoleChange
                        ? { onMemberRoleChange: (role: string) => onRoleChange(person.id, role) }
                        : {})}
                      canChangeRole={Boolean(onRoleChange) && !person.isSelf && !person.roleLocked}
                      {...(onRemovePerson ? { onRemove: () => onRemovePerson(person.id) } : {})}
                      canRemove={Boolean(onRemovePerson) && !person.isSelf && !person.removeLocked}
                    />
                  ))}
                </MemberList>
              </section>
            ) : null}

            {showAccess || link ? (
              <section className="mn-share-dialog__section" aria-labelledby={`${uid}-access`}>
                <h3 id={`${uid}-access`} className="mn-share-dialog__heading">
                  {m.accessHeading}
                </h3>
                {showAccess ? (
                  <Field invalid={Boolean(accessAction.error)}>
                    <FieldLabel>{m.accessLabel}</FieldLabel>
                    <NativeSelect
                      options={(accessOptions ?? []).map((o) => ({
                        value: o.value,
                        label: o.label,
                      }))}
                      value={accessValue ?? ''}
                      disabled={!onAccessChange || accessAction.pending}
                      aria-busy={accessAction.pending || undefined}
                      onChange={(e) => void changeAccess(e.target.value)}
                    />
                    {currentAccess?.description ? (
                      <FieldDescription>{currentAccess.description}</FieldDescription>
                    ) : null}
                    {accessAction.error ? <FieldError>{accessAction.error}</FieldError> : null}
                  </Field>
                ) : null}
                {link ? <CopyField label={m.linkLabel} value={link} mono /> : null}
              </section>
            ) : null}

            {showLinks ? (
              <section className="mn-share-dialog__section" aria-labelledby={`${uid}-links`}>
                <h3 id={`${uid}-links`} className="mn-share-dialog__heading">
                  {m.linksHeading}
                </h3>
                {onCreateLink ? (
                  <form className="mn-share-dialog__create" onSubmit={submitLink} noValidate>
                    <div className="mn-share-dialog__create-fields">
                      {linkPassword ? (
                        <Field invalid={passwordInvalid}>
                          <FieldLabel optionalText={m.optional}>{m.passwordLabel}</FieldLabel>
                          <Input
                            type="password"
                            autoComplete="new-password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                          />
                          {passwordInvalid ? (
                            <FieldError>{m.passwordTooShort(passwordMinLength)}</FieldError>
                          ) : null}
                        </Field>
                      ) : null}
                      {expiryOptions.length ? (
                        <Field>
                          <FieldLabel>{m.expiryLabel}</FieldLabel>
                          <NativeSelect
                            options={expiryOptions.map((o) => ({
                              value: String(o.days),
                              label: o.label,
                            }))}
                            value={String(expiry)}
                            onChange={(e) =>
                              setExpiry(e.target.value === 'null' ? null : Number(e.target.value))
                            }
                          />
                        </Field>
                      ) : null}
                    </div>
                    {createAction.error ? (
                      <p className="mn-share-dialog__error" role="alert">
                        {createAction.error}
                      </p>
                    ) : null}
                    <Button
                      type="submit"
                      variant="secondary"
                      loading={createAction.pending}
                      disabled={passwordInvalid}
                    >
                      {m.createLink}
                    </Button>
                  </form>
                ) : null}
                {linksLoading ? (
                  <p className="mn-share-dialog__muted" aria-busy="true">
                    {m.linksLoading}
                  </p>
                ) : links?.length ? (
                  <ul className="mn-share-dialog__links">
                    {links.map((item) => {
                      const state = item.status ?? 'active';
                      const label = item.label ?? item.url;
                      return (
                        <li key={item.id} className="mn-share-dialog__link" data-status={state}>
                          <div className="mn-share-dialog__link-text">
                            <span className="mn-share-dialog__link-url">{label}</span>
                            <span className="mn-share-dialog__link-meta">
                              {[
                                item.views !== undefined ? m.views(item.views) : null,
                                item.hasPassword ? m.passwordProtected : null,
                                state === 'active' && item.expiresAt !== undefined
                                  ? m.expires(dateFormat(item.expiresAt))
                                  : null,
                                state === 'expired' ? m.expired : null,
                                state === 'revoked' ? m.revoked : null,
                              ]
                                .filter(Boolean)
                                .join(' · ')}
                            </span>
                          </div>
                          {state === 'active' ? (
                            <div className="mn-share-dialog__link-actions">
                              <CopyButton
                                value={item.url}
                                iconOnly
                                size="sm"
                                variant="ghost"
                                label={m.copyLink(label)}
                              />
                              {onRevokeLink ? (
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  className="mn-share-dialog__revoke"
                                  aria-label={m.revokeLabel(label)}
                                  onClick={() => revoke(item)}
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
                ) : links ? (
                  <p className="mn-share-dialog__muted">{m.noLinks}</p>
                ) : null}
              </section>
            ) : null}

            {children}
            <p role="status" className="mn-visually-hidden">
              {status}
            </p>
          </DialogBody>
          {confirmElement}
        </DialogContent>
      </Dialog>
    );
  },
);
