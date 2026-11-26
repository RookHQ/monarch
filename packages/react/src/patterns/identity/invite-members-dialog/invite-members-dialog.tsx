'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type ElementRef,
  type FormEvent,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../../components/actions/button/index.js';
import { Alert } from '../../../components/feedback/alert/index.js';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from '../../../components/forms/form-field/index.js';
import { TagInput } from '../../../components/forms/tag-input/index.js';
import { Textarea } from '../../../components/forms/text-field/index.js';
import {
  AppAccessPicker,
  type AppAccessOption,
} from '../../../components/identity/app-access-picker/index.js';
import { RolePicker, type RoleOption } from '../../../components/identity/role-picker/index.js';
import {
  Dialog,
  DialogBody,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  type DialogContentProps,
} from '../../../components/overlays/dialog/index.js';

/** What `onInvite` receives. */
export interface InviteMembersPayload {
  /** The addresses (or usernames, with a custom `validateEmail`) to invite. */
  emails: string[];
  /** The chosen role's value. */
  role: string;
  /** Ids of the chosen apps; empty when no `apps` are offered. */
  apps: string[];
  /** The optional personal note, trimmed; omitted when empty. */
  message?: string;
}

/** One address that could not be invited. */
export interface InviteMembersFailure {
  email: string;
  /** Why, e.g. "Already a member". */
  message: ReactNode;
}

/** What `onInvite` may resolve with. Every address not listed in `failed` counts as invited. */
export interface InviteMembersResult {
  failed?: InviteMembersFailure[] | undefined;
}

/** Visible strings, for translation. */
export interface InviteMembersDialogMessages {
  /** Default "Invite people". */
  title: string;
  /** Line under the title. */
  description: string;
  /** Default "Email addresses". */
  emailsLabel: string;
  /** Help under the address field. */
  emailsHelp: string;
  /** Placeholder of the address field. */
  emailsPlaceholder: string;
  /** Shown when the field is empty on submit. */
  emailsRequired: string;
  /** Shown for an address that does not look like one. */
  invalidEmail: (value: string) => string;
  /** Shown when there are more than `maxEmails`. */
  tooMany: (max: number) => string;
  /** Default "Role". */
  roleLabel: string;
  /** Default "App access". */
  appsLabel: string;
  /** Default "Personal note". */
  messageLabel: string;
  /** Default "Optional". */
  optional: string;
  /** Placeholder of the note. */
  messagePlaceholder: string;
  /** Submit button. Default `Send invite` / `Send ${n} invites`. */
  submit: (count: number) => string;
  /** Default "Cancel". */
  cancel: string;
  /** Close button's accessible name. Default "Close". */
  close: string;
  /** Heading of the per-address errors. */
  partialFailure: (invited: number, failed: number) => string;
  /** Announced after everyone was invited. */
  invited: (count: number) => string;
  /** Shown when `onInvite` rejects without a message. */
  error: string;
}

const plural = (n: number, one: string, many: string) => (n === 1 ? one : many);

export const defaultInviteMembersDialogMessages: InviteMembersDialogMessages = {
  title: 'Invite people',
  description: 'They’ll get an email and join once they accept the invitation.',
  emailsLabel: 'Email addresses',
  emailsHelp: 'Separate several addresses with commas, spaces or new lines.',
  emailsPlaceholder: 'name@example.com',
  emailsRequired: 'Add at least one email address.',
  invalidEmail: (v) => `“${v}” is not an email address.`,
  tooMany: (max) => `You can invite up to ${max} people at once.`,
  roleLabel: 'Role',
  appsLabel: 'App access',
  messageLabel: 'Personal note',
  optional: 'Optional',
  messagePlaceholder: 'Add a note to the invitation email',
  submit: (n) => (n > 1 ? `Send ${n} invites` : 'Send invite'),
  cancel: 'Cancel',
  close: 'Close',
  partialFailure: (invited, failed) =>
    invited > 0
      ? `Invited ${invited} ${plural(invited, 'person', 'people')}. ${failed} ${plural(failed, 'address', 'addresses')} couldn’t be invited:`
      : `${failed} ${plural(failed, 'address', 'addresses')} couldn’t be invited:`,
  invited: (n) => `Invited ${n} ${plural(n, 'person', 'people')}`,
  error: 'Couldn’t send the invitations. Try again.',
};

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export interface InviteMembersDialogProps extends Omit<
  DialogContentProps,
  'title' | 'children' | 'footer' | 'busy' | 'onSubmit'
> {
  /** Controlled open state. */
  open?: boolean | undefined;
  /** Initial open state when uncontrolled. */
  defaultOpen?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** The element that opens the dialog, e.g. an "Invite" Button. */
  trigger?: ReactElement | undefined;
  /** Roles to choose from, least access first. */
  roles: RoleOption[];
  /** Role chosen when the dialog opens. Default: the first enabled role. */
  defaultRole?: string | undefined;
  /** Apps that can be granted. Without it the app picker is not shown. */
  apps?: AppAccessOption[] | undefined;
  /** Apps chosen when the dialog opens. */
  defaultApps?: string[] | undefined;
  /** Show the optional personal note. Default `false`. */
  allowMessage?: boolean | undefined;
  /** Addresses already filled in when the dialog opens. */
  defaultEmails?: string[] | undefined;
  /** Most addresses per submit. Default 50. */
  maxEmails?: number | undefined;
  /**
   * Checks each entry before it becomes a token. Return a message to reject
   * it. Default: a basic email check. Override to also accept usernames.
   */
  validateEmail?: ((value: string) => string | null | undefined) | undefined;
  /**
   * Sends the invitations. Resolve with `{ failed: [{ email, message }] }` to
   * keep those addresses with their errors while the rest are cleared; a
   * rejection shows its message above the buttons.
   */
  onInvite: (payload: InviteMembersPayload) => unknown;
  /** Called with the addresses that were invited, after a submit. */
  onInvited?: ((emails: string[]) => void) | undefined;
  /** Close when every address was invited. Default `true`. */
  closeOnSuccess?: boolean | undefined;
  /** Overrides the title. */
  title?: ReactNode;
  /** Overrides the line under the title. */
  description?: ReactNode;
  /** Visible strings, for translation. */
  messages?: Partial<InviteMembersDialogMessages> | undefined;
}

/**
 * Invite people to a workspace: email addresses as tokens (paste a list),
 * a role with what it can do, which apps they get, and an optional note.
 * Sends with a pending state and keeps any addresses that failed, each with
 * its reason, so they can be fixed and sent again.
 */
export const InviteMembersDialog = forwardRef<
  ElementRef<typeof DialogContent>,
  InviteMembersDialogProps
>(function InviteMembersDialog(
  {
    open: openProp,
    defaultOpen = false,
    onOpenChange,
    trigger,
    roles,
    defaultRole,
    apps,
    defaultApps,
    allowMessage = false,
    defaultEmails,
    maxEmails = 50,
    validateEmail,
    onInvite,
    onInvited,
    closeOnSuccess = true,
    title,
    description,
    messages,
    size = 'md',
    className,
    ...rest
  },
  ref,
) {
  const m = { ...defaultInviteMembersDialogMessages, ...messages };
  const [openState, setOpenState] = useState(defaultOpen);
  const open = openProp ?? openState;
  const firstRole = defaultRole ?? roles.find((r) => !r.disabled)?.value ?? '';
  const [emails, setEmails] = useState<string[]>(defaultEmails ?? []);
  const [text, setText] = useState('');
  const [role, setRole] = useState(firstRole);
  const [selectedApps, setSelectedApps] = useState<string[]>(defaultApps ?? []);
  const [note, setNote] = useState('');
  const [pending, setPending] = useState(false);
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [failures, setFailures] = useState<InviteMembersFailure[]>([]);
  const [invitedCount, setInvitedCount] = useState(0);
  const [formError, setFormError] = useState<ReactNode>(null);
  const [announcement, setAnnouncement] = useState('');
  const mounted = useRef(true);
  const failuresRef = useRef<HTMLDivElement | null>(null);
  const formId = useId();
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const reset = () => {
    setEmails(defaultEmails ?? []);
    setText('');
    setRole(firstRole);
    setSelectedApps(defaultApps ?? []);
    setNote('');
    setFieldError(null);
    setFailures([]);
    setInvitedCount(0);
    setFormError(null);
  };

  const setOpen = (next: boolean) => {
    if (pending && !next) return;
    if (next) {
      reset();
      setAnnouncement('');
    }
    if (openProp === undefined) setOpenState(next);
    onOpenChange?.(next);
  };

  // A controlled open starts clean too.
  const wasOpen = useRef(open);
  useEffect(() => {
    if (open && !wasOpen.current && openProp !== undefined) reset();
    wasOpen.current = open;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const check = (value: string) =>
    validateEmail ? validateEmail(value) : EMAIL.test(value) ? null : m.invalidEmail(value);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (pending) return;
    let list = emails;
    const typed = text.trim();
    if (typed) {
      const parts = typed.split(/[\s,;]+/).filter(Boolean);
      const bad = parts.map(check).find(Boolean);
      if (bad) {
        setFieldError(bad);
        return;
      }
      list = [
        ...emails,
        ...parts.filter((p) => !emails.some((e) => e.toLowerCase() === p.toLowerCase())),
      ];
      setEmails(list);
      setText('');
    }
    if (list.length === 0) {
      setFieldError(m.emailsRequired);
      return;
    }
    if (list.length > maxEmails) {
      setFieldError(m.tooMany(maxEmails));
      return;
    }
    setFieldError(null);
    setFormError(null);
    setFailures([]);
    setPending(true);
    const trimmed = note.trim();
    try {
      const result = await onInvite({
        emails: list,
        role,
        apps: apps ? selectedApps : [],
        ...(allowMessage && trimmed ? { message: trimmed } : {}),
      });
      if (!mounted.current) return;
      const failed = (result as InviteMembersResult | undefined)?.failed ?? [];
      const failedSet = new Set(failed.map((f) => f.email.toLowerCase()));
      const invited = list.filter((e) => !failedSet.has(e.toLowerCase()));
      setPending(false);
      if (invited.length > 0) onInvited?.(invited);
      if (failed.length === 0) {
        setAnnouncement(m.invited(invited.length));
        if (closeOnSuccess) {
          if (openProp === undefined) setOpenState(false);
          onOpenChange?.(false);
        } else {
          reset();
        }
        return;
      }
      setEmails(list.filter((e) => failedSet.has(e.toLowerCase())));
      setInvitedCount(invited.length);
      setFailures(failed);
      requestAnimationFrame(() => failuresRef.current?.focus());
    } catch (failure) {
      if (!mounted.current) return;
      setPending(false);
      setFormError(failure instanceof Error && failure.message ? failure.message : m.error);
    }
  };

  const count = emails.length + (text.trim() ? 1 : 0);

  return (
    <>
      <Dialog open={open} onOpenChange={setOpen}>
        {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
        <DialogContent
          ref={ref}
          {...rest}
          size={size}
          busy={pending}
          closeLabel={m.close}
          closeOnOutsideClick={false}
          className={cx('mn-invite-members-dialog', className)}
        >
          <DialogHeader>
            <DialogTitle>{title ?? m.title}</DialogTitle>
            <DialogDescription>{description ?? m.description}</DialogDescription>
          </DialogHeader>
          <DialogBody>
            <form
              id={formId}
              className="mn-invite-members-dialog__form"
              noValidate
              onSubmit={(event) => void submit(event)}
            >
              <Field invalid={fieldError !== null} required>
                <FieldLabel>{m.emailsLabel}</FieldLabel>
                <FieldDescription>{m.emailsHelp}</FieldDescription>
                <TagInput
                  value={emails}
                  onValueChange={(next) => {
                    setEmails(next);
                    setFieldError(null);
                    setFailures((all) =>
                      all.filter((f) =>
                        next.some((e) => e.toLowerCase() === f.email.toLowerCase()),
                      ),
                    );
                  }}
                  inputValue={text}
                  onInputValueChange={setText}
                  separators={[',', ' ', ';']}
                  normalize={(raw) => raw.trim().replace(/^<|>$/g, '')}
                  validate={(tag) => check(tag) ?? undefined}
                  max={maxEmails}
                  addOnBlur
                  disabled={pending}
                  autoComplete="off"
                  placeholder={emails.length === 0 ? m.emailsPlaceholder : undefined}
                  className="mn-invite-members-dialog__emails"
                />
                {fieldError ? <FieldError>{fieldError}</FieldError> : null}
              </Field>
              {failures.length > 0 ? (
                <div
                  ref={failuresRef}
                  tabIndex={-1}
                  role="alert"
                  className="mn-invite-members-dialog__failures"
                >
                  <p className="mn-invite-members-dialog__failures-title">
                    {m.partialFailure(invitedCount, failures.length)}
                  </p>
                  <ul className="mn-invite-members-dialog__failure-list">
                    {failures.map((failure) => (
                      <li key={failure.email} className="mn-invite-members-dialog__failure">
                        <span className="mn-invite-members-dialog__failure-email">
                          {failure.email}
                        </span>
                        <span className="mn-invite-members-dialog__failure-message">
                          {failure.message}
                        </span>
                      </li>
                    ))}
                  </ul>
                </div>
              ) : null}
              <RolePicker
                roles={roles}
                value={role}
                onValueChange={setRole}
                layout="compact"
                disabled={pending}
                required
                messages={{ label: m.roleLabel }}
                className="mn-invite-members-dialog__role"
              />
              {apps ? (
                <AppAccessPicker
                  apps={apps}
                  value={selectedApps}
                  onValueChange={setSelectedApps}
                  disabled={pending}
                  messages={{ label: m.appsLabel }}
                  className="mn-invite-members-dialog__apps"
                />
              ) : null}
              {allowMessage ? (
                <Field>
                  <FieldLabel optionalText={m.optional}>{m.messageLabel}</FieldLabel>
                  <Textarea
                    value={note}
                    onChange={(event) => setNote(event.target.value)}
                    placeholder={m.messagePlaceholder}
                    rows={3}
                    maxLength={1000}
                    disabled={pending}
                  />
                </Field>
              ) : null}
              {formError ? (
                <Alert tone="danger" live="assertive">
                  {formError}
                </Alert>
              ) : null}
            </form>
          </DialogBody>
          <DialogFooter>
            <DialogClose asChild>
              <Button disabled={pending}>{m.cancel}</Button>
            </DialogClose>
            <Button type="submit" form={formId} variant="primary" loading={pending}>
              {m.submit(Math.max(1, count))}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <span className="mn-invite-members-dialog__status" role="status" aria-live="polite">
        {announcement}
      </span>
    </>
  );
});
