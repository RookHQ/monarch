'use client';

import { forwardRef, useCallback, useState, type ElementRef, type ReactNode } from 'react';
import {
  RelativeTime,
  type DateInput,
} from '../../../components/data-display/relative-time/index.js';
import {
  ConsequenceList,
  type ConsequenceItem,
} from '../../../components/feedback/consequence-list/index.js';
import {
  useStepUpAuth,
  type StepUpRequirement,
} from '../../../components/identity/step-up-auth-dialog/index.js';
import {
  ConfirmDialog,
  type ConfirmDialogProps,
} from '../../../components/overlays/confirm-dialog/index.js';
import { cx } from '../../../internal/cx.js';

/** What is being revoked. Drives the default copy and tone. */
export type RevokeAccessKind = 'api-key' | 'oauth-client' | 'portal-access' | 'session' | 'device';

/** One credential, client, person or device to revoke. */
export interface RevokeAccessItem {
  /** Passed to `onRevoke`. */
  id: string;
  /** What the person recognises it by: the key name, the app, the person, the browser. */
  label: ReactNode;
  /** A quieter second line, e.g. a key prefix, scopes or a location. */
  detail?: ReactNode;
  /** When it was last used, shown as relative time. `null` shows "Never used"; leave it out to show nothing. */
  lastUsed?: DateInput | null;
}

/** The default copy for one kind. `count` is the number of items; `n` is it formatted. */
export interface RevokeAccessKindCopy {
  /** The question. Default e.g. "Revoke API key?" or "Revoke 3 API keys?". */
  title: (count: number, n: string) => string;
  /** The confirm button. Default e.g. "Revoke key" or "Revoke 3 keys". */
  confirm: (count: number, n: string) => string;
  /** One line under the title. */
  description: (count: number) => string;
  /** What happens, as ConsequenceList items. */
  consequences: (count: number) => ConsequenceItem[];
}

/** Every visible string, for translation. */
export interface RevokeAccessFlowMessages {
  /** Copy per kind. */
  kinds: Record<RevokeAccessKind, RevokeAccessKindCopy>;
  /** Accessible name of the item list. Default "To revoke". */
  itemsLabel: string;
  /** Before the relative time. Default "Last used". */
  lastUsed: string;
  /** Shown when `lastUsed` is `null`. Default "Never used". */
  neverUsed: string;
  /** Heading of the consequence list. Default "What happens". */
  consequencesHeading: string;
  /** Default "Cancel". */
  cancel: string;
  /** Why the step-up dialog appears. Default "Revoking access needs you to confirm it’s you." */
  stepUpDescription: string;
}

/** Overrides for `RevokeAccessFlowMessages`; each kind can be overridden in part. */
export type RevokeAccessFlowMessagesInput = Partial<Omit<RevokeAccessFlowMessages, 'kinds'>> & {
  kinds?: Partial<Record<RevokeAccessKind, Partial<RevokeAccessKindCopy>>>;
};

const one = (count: number) => count === 1;

const DEFAULT_KINDS: Record<RevokeAccessKind, RevokeAccessKindCopy> = {
  'api-key': {
    title: (c, n) => (one(c) ? 'Revoke API key?' : `Revoke ${n} API keys?`),
    confirm: (c, n) => (one(c) ? 'Revoke key' : `Revoke ${n} keys`),
    description: (c) =>
      one(c)
        ? 'A revoked key cannot be restored. Create a new key if you still need access.'
        : 'Revoked keys cannot be restored. Create new keys if you still need access.',
    consequences: (c) => [
      {
        kind: 'removed',
        label: one(c)
          ? 'Apps and scripts using this key stop working immediately'
          : 'Apps and scripts using these keys stop working immediately',
      },
    ],
  },
  'oauth-client': {
    title: (c, n) => (one(c) ? 'Revoke OAuth client?' : `Revoke ${n} OAuth clients?`),
    confirm: (c, n) => (one(c) ? 'Revoke client' : `Revoke ${n} clients`),
    description: (c) =>
      one(c)
        ? 'Its credentials cannot be restored. Register the app again if you still need it.'
        : 'Their credentials cannot be restored. Register the apps again if you still need them.',
    consequences: () => [
      { kind: 'removed', label: 'Access tokens it issued stop working immediately' },
      { kind: 'changed', label: 'People can no longer sign in to the app with it' },
    ],
  },
  'portal-access': {
    title: (c, n) => (one(c) ? 'Remove portal access?' : `Remove portal access for ${n} people?`),
    confirm: (c, n) => (one(c) ? 'Remove access' : `Remove access for ${n}`),
    description: () => 'You can invite them again later.',
    consequences: () => [
      { kind: 'changed', label: 'They are signed out of the portal and cannot sign in again' },
    ],
  },
  session: {
    title: (c, n) => (one(c) ? 'Sign out this session?' : `Sign out ${n} sessions?`),
    confirm: (c, n) => (one(c) ? 'Sign out' : `Sign out ${n} sessions`),
    description: () => 'Anyone using it has to sign in again.',
    consequences: (c) => [
      {
        kind: 'changed',
        label: one(c)
          ? 'The browser is signed out immediately'
          : 'The browsers are signed out immediately',
      },
    ],
  },
  device: {
    title: (c, n) => (one(c) ? 'Remove this device?' : `Remove ${n} devices?`),
    confirm: (c, n) => (one(c) ? 'Remove device' : `Remove ${n} devices`),
    description: (c) =>
      one(c)
        ? 'It has to sign in again to get access.'
        : 'They have to sign in again to get access.',
    consequences: (c) => [
      {
        kind: 'changed',
        label: one(c)
          ? 'The device is signed out immediately'
          : 'The devices are signed out immediately',
      },
    ],
  },
};

const DEFAULT_MESSAGES: Omit<RevokeAccessFlowMessages, 'kinds'> = {
  itemsLabel: 'To revoke',
  lastUsed: 'Last used',
  neverUsed: 'Never used',
  consequencesHeading: 'What happens',
  cancel: 'Cancel',
  stepUpDescription: 'Revoking access needs you to confirm it’s you.',
};

const IRREVERSIBLE: ReadonlySet<RevokeAccessKind> = new Set(['api-key', 'oauth-client']);

export interface RevokeAccessFlowProps extends Omit<
  ConfirmDialogProps,
  | 'title'
  | 'description'
  | 'consequences'
  | 'confirmLabel'
  | 'cancelLabel'
  | 'confirmPhrase'
  | 'onConfirm'
  | 'reason'
  | 'children'
  | 'closeOnConfirm'
  | 'pending'
  | 'error'
> {
  /** What is revoked: drives the default title, button, description and consequences. */
  kind: RevokeAccessKind;
  /** One or more items to revoke, listed in the dialog. */
  items: RevokeAccessItem[];
  /** Overrides the title. */
  title?: ReactNode;
  /** Overrides the line under the title. */
  description?: ReactNode;
  /** Overrides the default consequences for the kind. */
  consequences?: ConsequenceItem[];
  /** Overrides the confirm button label. */
  confirmLabel?: ReactNode;
  /**
   * Type-to-confirm: the person must type this phrase (usually the key or
   * client name) before revoking. Meant for `api-key` and `oauth-client`.
   */
  requireConfirmPhrase?: string;
  /**
   * `danger` by default for `api-key` and `oauth-client` (cannot be undone),
   * `warning` for portal access, sessions and devices (they can sign in again).
   */
  tone?: 'default' | 'warning' | 'danger';
  /** Ask the person to re-authenticate before the revocation runs. */
  stepUp?: StepUpRequirement;
  /**
   * Revokes the items. Return a promise to keep the dialog pending; it closes
   * when it resolves. A rejection shows its message and the person can retry.
   */
  onRevoke: (ids: string[]) => unknown;
  /** Reference time for "last used". Default: now. */
  now?: DateInput;
  /** Locale for numbers and relative times. */
  locale?: string;
  /** Visible strings, for translation. */
  messages?: RevokeAccessFlowMessagesInput;
}

/**
 * Revokes API keys, OAuth clients, portal access, sessions or devices: one
 * ConfirmDialog that lists what is revoked with when each was last used,
 * states the consequences for the kind, and can require a typed phrase and a
 * step-up before calling `onRevoke`.
 */
export const RevokeAccessFlow = forwardRef<ElementRef<typeof ConfirmDialog>, RevokeAccessFlowProps>(
  function RevokeAccessFlow(
    {
      kind,
      items,
      title,
      description,
      consequences,
      confirmLabel,
      requireConfirmPhrase,
      tone,
      stepUp,
      onRevoke,
      now,
      locale,
      messages: messagesProp,
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      className,
      ...rest
    },
    ref,
  ) {
    const { kinds: kindsProp, ...stringsProp } = messagesProp ?? {};
    const m = { ...DEFAULT_MESSAGES, ...stringsProp };
    const copy: RevokeAccessKindCopy = { ...DEFAULT_KINDS[kind], ...kindsProp?.[kind] };
    const count = items.length;
    const n = new Intl.NumberFormat(locale).format(count);
    const shownConsequences = consequences ?? copy.consequences(count);

    const [openState, setOpenState] = useState(defaultOpen);
    const open = openProp ?? openState;
    const setOpen = useCallback(
      (next: boolean) => {
        if (openProp === undefined) setOpenState(next);
        onOpenChange?.(next);
      },
      [openProp, onOpenChange],
    );
    const { requireStepUp, element: stepUpElement } = useStepUpAuth();

    return (
      <>
        <ConfirmDialog
          ref={ref}
          {...rest}
          open={open}
          onOpenChange={setOpen}
          tone={tone ?? (IRREVERSIBLE.has(kind) ? 'danger' : 'warning')}
          title={title ?? copy.title(count, n)}
          description={description ?? copy.description(count)}
          confirmLabel={confirmLabel ?? copy.confirm(count, n)}
          cancelLabel={m.cancel}
          {...(requireConfirmPhrase !== undefined ? { confirmPhrase: requireConfirmPhrase } : {})}
          closeOnConfirm={false}
          onConfirm={async () => {
            if (stepUp) {
              const verified = await requireStepUp({
                description: m.stepUpDescription,
                ...stepUp,
              });
              if (!verified) return;
            }
            await onRevoke(items.map((item) => item.id));
            setOpen(false);
          }}
          data-kind={kind}
          className={cx('mn-revoke-access-flow', className)}
        >
          {count > 0 ? (
            <ul className="mn-revoke-access-flow__items" aria-label={m.itemsLabel}>
              {items.map((item) => (
                <li key={item.id} className="mn-revoke-access-flow__item">
                  <span className="mn-revoke-access-flow__label">{item.label}</span>
                  {item.detail ? (
                    <span className="mn-revoke-access-flow__detail">{item.detail}</span>
                  ) : null}
                  {item.lastUsed === null ? (
                    <span className="mn-revoke-access-flow__last-used">{m.neverUsed}</span>
                  ) : item.lastUsed !== undefined ? (
                    <span className="mn-revoke-access-flow__last-used">
                      {m.lastUsed}{' '}
                      <RelativeTime
                        date={item.lastUsed}
                        tooltip={false}
                        {...(now !== undefined ? { now } : {})}
                        {...(locale !== undefined ? { locale } : {})}
                      />
                    </span>
                  ) : null}
                </li>
              ))}
            </ul>
          ) : null}
          {shownConsequences.length > 0 ? (
            <ConsequenceList
              heading={m.consequencesHeading}
              headingLevel="h3"
              items={shownConsequences}
              {...(locale !== undefined ? { locale } : {})}
            />
          ) : null}
        </ConfirmDialog>
        {stepUpElement}
      </>
    );
  },
);
