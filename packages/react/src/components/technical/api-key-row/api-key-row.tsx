'use client';

import { forwardRef, useId, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { Badge, type Tone } from '../../data-display/badge/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { useConfirm, type ConfirmOptions } from '../../overlays/confirm-dialog/index.js';

/** Lifecycle of a key. Revoked and expired keys show no actions. */
export type ApiKeyStatus = 'active' | 'expiring' | 'expired' | 'revoked';

/** Every visible string in ApiKeyRow, with English defaults. */
export interface ApiKeyRowMessages {
  /** Accessible name of the masked prefix. */
  prefixLabel: (prefix: string) => string;
  created: string;
  lastUsed: string;
  /** Shown instead of a time when the key was never used. */
  neverUsed: string;
  expires: string;
  /** Accessible name of the scope list. */
  scopes: string;
  /** Shown when the key has no scopes. */
  noScopes: string;
  /** The "+N" badge for scopes beyond `maxScopes`; its title lists them. */
  moreScopes: (count: number) => string;
  status: Record<ApiKeyStatus, string>;
  rotate: string;
  revoke: string;
  /** Accessible name of the action group. */
  actions: (name: string) => string;
  rotateTitle: (name: string) => string;
  rotateDescription: string;
  rotateConfirm: string;
  revokeTitle: (name: string) => string;
  revokeDescription: string;
  revokeConsequences: string[];
  revokeConfirm: string;
  cancel: string;
}

const DEFAULT_MESSAGES: ApiKeyRowMessages = {
  prefixLabel: (p) => `Key starting with ${p}`,
  created: 'Created',
  lastUsed: 'Last used',
  neverUsed: 'Never used',
  expires: 'Expires',
  scopes: 'Scopes',
  noScopes: 'No scopes',
  moreScopes: (n) => `+${n}`,
  status: { active: 'Active', expiring: 'Expiring soon', expired: 'Expired', revoked: 'Revoked' },
  rotate: 'Rotate',
  revoke: 'Revoke',
  actions: (name) => `Actions for ${name}`,
  rotateTitle: (name) => `Rotate ${name}?`,
  rotateDescription:
    'A new key is issued and shown once. Update every place that uses this key before the old one stops working.',
  rotateConfirm: 'Rotate key',
  revokeTitle: (name) => `Revoke ${name}?`,
  revokeDescription: 'Requests that use this key will be rejected.',
  revokeConsequences: ['This cannot be undone.', 'Apps using this key stop working right away.'],
  revokeConfirm: 'Revoke key',
  cancel: 'Cancel',
};

const STATUS_TONE: Record<ApiKeyStatus, Tone> = {
  active: 'success',
  expiring: 'warning',
  expired: 'neutral',
  revoked: 'danger',
};

export interface ApiKeyRowProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
  /** The key's name, e.g. "Reporting job". Pass a link element to open its details page. */
  name: ReactNode;
  /** Plain-text name for dialogs and accessible names when `name` is an element. Default: `name` if it is a string. */
  nameText?: string | undefined;
  /** The visible, non-secret start of the key, e.g. `sk_live_4f2a`. The rest is shown masked. */
  prefix: string;
  /** When the key was created. A Date, epoch milliseconds or an ISO 8601 string. */
  createdAt: DateInput;
  /** When the key was last used; `null` or omitted shows "Never used". */
  lastUsedAt?: DateInput | null | undefined;
  /** When the key expires, if it does. */
  expiresAt?: DateInput | null | undefined;
  /** Scope ids or labels, e.g. `["items:read", "items:write"]`. */
  scopes?: readonly string[] | undefined;
  /** Scopes shown before the "+N" badge. Default 3. */
  maxScopes?: number | undefined;
  /** Default `active`. `revoked` and `expired` hide the actions. */
  status?: ApiKeyStatus | undefined;
  /** Reference time for the relative dates, for identical server and client output. */
  now?: DateInput | undefined;
  /** BCP 47 locale for dates. Default: the runtime locale. */
  locale?: string | undefined;
  /**
   * Rotates the key after the person confirms. Return a promise to keep the
   * dialog pending until it settles; a rejection shows its message in the
   * dialog. Omit to hide Rotate. Show the new secret with KeyCreatedDialog.
   */
  onRotate?: (() => unknown) | undefined;
  /** Revokes the key after the person confirms, like `onRotate`. Omit to hide Revoke. */
  onRevoke?: (() => unknown) | undefined;
  /** Ask the person to type this (usually the key name) before revoking. */
  revokeConfirmPhrase?: string | undefined;
  /** Extra ConfirmDialog options for the rotate dialog, e.g. `consequences`. */
  rotateConfirmOptions?: Partial<ConfirmOptions> | undefined;
  /** Extra ConfirmDialog options for the revoke dialog. */
  revokeConfirmOptions?: Partial<ConfirmOptions> | undefined;
  /** More actions after Rotate and Revoke, e.g. an "Edit" link. */
  actions?: ReactNode;
  /** Render as a list item inside a `<ul>` of keys. Default `div`. */
  as?: 'div' | 'li' | undefined;
  /** Replaces any of the English strings. */
  messages?:
    | (Partial<Omit<ApiKeyRowMessages, 'status'>> & {
        status?: Partial<Record<ApiKeyStatus, string>> | undefined;
      })
    | undefined;
}

/**
 * One API key in a list: name, status, masked prefix, created and last used
 * times, scopes, and Rotate and Revoke actions that ask for confirmation in a
 * ConfirmDialog before calling the product. The secret itself is never shown.
 */
export const ApiKeyRow = forwardRef<HTMLElement, ApiKeyRowProps>(function ApiKeyRow(
  {
    name,
    nameText,
    prefix,
    createdAt,
    lastUsedAt,
    expiresAt,
    scopes = [],
    maxScopes = 3,
    status = 'active',
    now,
    locale,
    onRotate,
    onRevoke,
    revokeConfirmPhrase,
    rotateConfirmOptions,
    revokeConfirmOptions,
    actions,
    as = 'div',
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m: ApiKeyRowMessages = {
    ...DEFAULT_MESSAGES,
    ...messagesProp,
    status: { ...DEFAULT_MESSAGES.status, ...messagesProp?.status },
  };
  const id = useId();
  const nameId = `${id}-name`;
  const label = nameText ?? (typeof name === 'string' ? name : prefix);
  const { confirm, element } = useConfirm();
  const live = status === 'active' || status === 'expiring';
  const shown = scopes.slice(0, maxScopes);
  const hidden = scopes.slice(maxScopes);
  const time = (date: DateInput) => (
    <RelativeTime
      date={date}
      tooltip={false}
      {...(now !== undefined ? { now } : {})}
      {...(locale ? { locale } : {})}
    />
  );

  const rotate = () =>
    void confirm({
      title: m.rotateTitle(label),
      description: m.rotateDescription,
      tone: 'warning',
      confirmLabel: m.rotateConfirm,
      cancelLabel: m.cancel,
      ...rotateConfirmOptions,
      onConfirm: () => onRotate?.(),
    });
  const revoke = () =>
    void confirm({
      title: m.revokeTitle(label),
      description: m.revokeDescription,
      consequences: m.revokeConsequences,
      tone: 'danger',
      confirmLabel: m.revokeConfirm,
      cancelLabel: m.cancel,
      ...(revokeConfirmPhrase ? { confirmPhrase: revokeConfirmPhrase } : {}),
      ...revokeConfirmOptions,
      onConfirm: () => onRevoke?.(),
    });

  const Root = as;
  return (
    <Root
      ref={ref as never}
      aria-labelledby={nameId}
      {...rest}
      className={cx('mn-api-key-row', className)}
      data-status={status}
    >
      <div className="mn-api-key-row__main">
        <div className="mn-api-key-row__title">
          <span id={nameId} className="mn-api-key-row__name">
            {name}
          </span>
          <Badge size="sm" tone={STATUS_TONE[status]} dot className="mn-api-key-row__status">
            {m.status[status]}
          </Badge>
        </div>
        <code className="mn-api-key-row__prefix" aria-label={m.prefixLabel(prefix)}>
          <span>{prefix}</span>
          <span className="mn-api-key-row__mask" aria-hidden="true">
            ••••••••
          </span>
        </code>
        <dl className="mn-api-key-row__meta">
          <div>
            <dt>{m.created}</dt>
            <dd>{time(createdAt)}</dd>
          </div>
          <div>
            <dt>{m.lastUsed}</dt>
            <dd>{lastUsedAt != null ? time(lastUsedAt) : m.neverUsed}</dd>
          </div>
          {expiresAt != null ? (
            <div>
              <dt>{m.expires}</dt>
              <dd>{time(expiresAt)}</dd>
            </div>
          ) : null}
        </dl>
      </div>
      <div className="mn-api-key-row__scopes">
        {scopes.length === 0 ? (
          <span className="mn-api-key-row__none">{m.noScopes}</span>
        ) : (
          <ul aria-label={m.scopes} className="mn-api-key-row__scope-list">
            {shown.map((s) => (
              <li key={s}>
                <Badge size="sm" variant="outline">
                  {s}
                </Badge>
              </li>
            ))}
            {hidden.length > 0 ? (
              <li>
                <Badge size="sm" variant="outline" title={hidden.join(', ')}>
                  <span aria-hidden="true">{m.moreScopes(hidden.length)}</span>
                  <span className="mn-visually-hidden">{hidden.join(', ')}</span>
                </Badge>
              </li>
            ) : null}
          </ul>
        )}
      </div>
      {live && (onRotate || onRevoke || actions) ? (
        <div role="group" aria-label={m.actions(label)} className="mn-api-key-row__actions">
          {onRotate ? (
            <Button size="sm" variant="secondary" onClick={rotate}>
              {m.rotate}
            </Button>
          ) : null}
          {onRevoke ? (
            <Button size="sm" variant="danger-subtle" onClick={revoke}>
              {m.revoke}
            </Button>
          ) : null}
          {actions}
        </div>
      ) : null}
      {element}
    </Root>
  );
});
