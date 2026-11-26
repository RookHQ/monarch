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
import { Badge } from '../../data-display/badge/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { InlineNotice } from '../../feedback/inline-notice/index.js';
import { InlineEditText } from '../../forms/inline-edit-text/index.js';
import { SectionCard } from '../../layout/section-card/index.js';
import { useConfirm } from '../../overlays/confirm-dialog/index.js';

/** One registered passkey. */
export interface Passkey {
  /** Passed to the callbacks. */
  id: string;
  /** The name the person gave it, e.g. "Work laptop". */
  name: string;
  /** When it was added. */
  createdAt?: DateInput | undefined;
  /** When it was last used to sign in. */
  lastUsedAt?: DateInput | undefined;
  /** It is synced across devices by a password manager or platform account. */
  synced?: boolean | undefined;
  /** A quieter line, e.g. the provider ("iCloud Keychain"). */
  detail?: ReactNode;
}

/** Visible strings, for translation. */
export interface PasskeyManagerMessages {
  /** Card title. Default "Passkeys". */
  title: string;
  /** Line under the title. */
  description: string;
  /** Accessible name of the list. Default "Your passkeys". */
  listLabel: string;
  /** Add button. Default "Add a passkey". */
  add: string;
  /** Label of the rename field. Default `Name of ${name}`. */
  renameLabel: (name: string) => string;
  /** Error shown when a passkey is renamed to nothing. Default "Enter a name." */
  renameEmpty?: string | undefined;
  /** Remove button. Default "Remove". */
  remove: string;
  /** Accessible name of Remove. Default `Remove ${name}`. */
  removeLabel: (name: string) => string;
  /** Before the creation time. Default "Added". */
  created: string;
  /** Before the last-used time. Default "Last used". */
  lastUsed: string;
  /** When it was never used. Default "Never used". */
  neverUsed: string;
  /** Badge for synced passkeys. Default "Synced". */
  synced: string;
  /** Remove confirmation title. */
  confirmTitle: (name: string) => string;
  /** Remove confirmation description. */
  confirmDescription: string;
  /** Extra warning when removing the last passkey with `lastSignInMethod`. */
  lastMethodWarning: string;
  /** Remove confirmation button. Default "Remove passkey". */
  confirmLabel: string;
  /** Default "Cancel". */
  cancel: string;
  /** Shown when the browser cannot create passkeys. */
  unsupported: string;
  /** Shown when the person cancels the browser prompt. */
  cancelled: string;
  /** Shown when adding fails without a message. */
  addError: string;
  /** Announced after adding. */
  added: string;
  /** Announced after removing. */
  removed: (name: string) => string;
  /** Default "Loading passkeys". */
  loading: string;
  /** Default "No passkeys yet". */
  emptyTitle: string;
  /** Under the empty title. */
  emptyDescription: string;
  /** Default "Couldn’t load your passkeys". */
  errorTitle: string;
}

export const defaultPasskeyManagerMessages: PasskeyManagerMessages = {
  title: 'Passkeys',
  description:
    'Sign in with your fingerprint, face or screen lock instead of a password. Passkeys can’t be phished or reused.',
  listLabel: 'Your passkeys',
  add: 'Add a passkey',
  renameLabel: (name) => `Name of ${name}`,
  renameEmpty: 'Enter a name.',
  remove: 'Remove',
  removeLabel: (name) => `Remove ${name}`,
  created: 'Added',
  lastUsed: 'Last used',
  neverUsed: 'Never used',
  synced: 'Synced',
  confirmTitle: (name) => `Remove ${name}?`,
  confirmDescription: 'You won’t be able to sign in with this passkey any more.',
  lastMethodWarning:
    'This is your only way to sign in. Add another passkey or a password first, or you may lose access to your account.',
  confirmLabel: 'Remove passkey',
  cancel: 'Cancel',
  unsupported:
    'This browser can’t create passkeys. Try a recent version of your browser or another device.',
  cancelled: 'Passkey setup was cancelled. Nothing was changed.',
  addError: 'Couldn’t add the passkey. Try again.',
  added: 'Passkey added',
  removed: (name) => `Removed ${name}`,
  loading: 'Loading passkeys',
  emptyTitle: 'No passkeys yet',
  emptyDescription: 'Add one to sign in faster and more safely.',
  errorTitle: 'Couldn’t load your passkeys',
};

export interface PasskeyManagerProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'title' | 'children'
> {
  /** The person's passkeys. */
  passkeys: Passkey[];
  /** `loading`, `error` (with `onRetry`) or `ready` (default). */
  state?: 'ready' | 'loading' | 'error' | undefined;
  /** Retries loading in the `error` state. */
  onRetry?: (() => void) | undefined;
  /**
   * Runs the product's WebAuthn registration (`navigator.credentials.create`
   * and the server round trip). Return a promise for the pending state. A
   * rejection named `NotAllowedError` or `AbortError` (the person cancelled)
   * shows a neutral note; other rejections show their message.
   */
  onAdd?: (() => unknown) | undefined;
  /** Saves a new name. Return a promise; a rejection keeps the editor open with the error. */
  onRename?: ((id: string, name: string) => void | Promise<void>) | undefined;
  /** Removes a passkey after a confirmation. Return a promise to keep the confirmation pending. */
  onRemove?: ((id: string) => unknown) | undefined;
  /**
   * Whether this browser can create passkeys. Default: detected from
   * `window.PublicKeyCredential` after mount.
   */
  supported?: boolean | undefined;
  /**
   * Passkeys are the person's only sign-in method: removing the last one
   * shows an extra warning.
   */
  lastSignInMethod?: boolean | undefined;
  /** Overrides the card title. */
  title?: ReactNode;
  /** Overrides the line under the title. */
  description?: ReactNode;
  /** Heading element of the title. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | undefined;
  /** Reference time for relative times, for identical server and client output. */
  now?: DateInput | undefined;
  /** Locale for relative times. */
  locale?: string | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<PasskeyManagerMessages> | undefined;
}

const CANCELLED = new Set(['NotAllowedError', 'AbortError']);

function isCancel(error: unknown): boolean {
  return (
    typeof error === 'object' &&
    error !== null &&
    'name' in error &&
    CANCELLED.has(String((error as { name: unknown }).name))
  );
}

/**
 * The person's passkeys: list with when each was added and last used,
 * rename in place, remove behind a confirmation, and Add a passkey. The
 * product runs WebAuthn through the callbacks; the component only detects
 * whether the browser supports it.
 */
export const PasskeyManager = forwardRef<HTMLDivElement, PasskeyManagerProps>(
  function PasskeyManager(
    {
      passkeys,
      state = 'ready',
      onRetry,
      onAdd,
      onRename,
      onRemove,
      supported: supportedProp,
      lastSignInMethod = false,
      title,
      description,
      headingLevel = 'h2',
      now,
      locale,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultPasskeyManagerMessages, ...messages };
    const { confirm, element } = useConfirm();
    const [detected, setDetected] = useState<boolean | null>(null);
    const [adding, setAdding] = useState(false);
    const [notice, setNotice] = useState<{ tone: 'neutral' | 'danger'; text: ReactNode } | null>(
      null,
    );
    const [announcement, setAnnouncement] = useState('');
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);
    useEffect(() => {
      if (supportedProp !== undefined) return;
      setDetected(
        typeof window !== 'undefined' &&
          typeof (window as { PublicKeyCredential?: unknown }).PublicKeyCredential === 'function',
      );
    }, [supportedProp]);
    const supported = supportedProp ?? detected;

    const add = () => {
      if (!onAdd || adding || supported === false) return;
      setNotice(null);
      setAnnouncement('');
      setAdding(true);
      Promise.resolve()
        .then(() => onAdd())
        .then(
          () => {
            if (!mounted.current) return;
            setAdding(false);
            setAnnouncement(m.added);
          },
          (failure: unknown) => {
            if (!mounted.current) return;
            setAdding(false);
            setNotice(
              isCancel(failure)
                ? { tone: 'neutral', text: m.cancelled }
                : {
                    tone: 'danger',
                    text:
                      failure instanceof Error && failure.message ? failure.message : m.addError,
                  },
            );
          },
        );
    };

    const remove = async (passkey: Passkey) => {
      if (!onRemove) return;
      setAnnouncement('');
      const last = lastSignInMethod && passkeys.length === 1;
      const result = await confirm({
        title: m.confirmTitle(passkey.name),
        description: m.confirmDescription,
        tone: 'danger',
        ...(last ? { consequences: [m.lastMethodWarning] } : {}),
        confirmLabel: m.confirmLabel,
        cancelLabel: m.cancel,
        onConfirm: () => onRemove(passkey.id),
      });
      if (result && mounted.current) setAnnouncement(m.removed(passkey.name));
    };

    const cardState = state === 'ready' && passkeys.length === 0 ? 'empty' : state;
    const timeProps = {
      tooltip: false,
      ...(now !== undefined ? { now } : {}),
      ...(locale ? { locale } : {}),
    };

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-passkey-manager', className)}
        data-state={cardState}
        data-supported={supported === null ? undefined : String(supported)}
      >
        <SectionCard
          className="mn-passkey-manager__card"
          headingLevel={headingLevel}
          title={title ?? m.title}
          description={description ?? m.description}
          {...(onRetry ? { onRetry } : {})}
          state={cardState}
          loadingLabel={m.loading}
          emptyTitle={m.emptyTitle}
          emptyDescription={m.emptyDescription}
          errorTitle={m.errorTitle}
          actions={
            onAdd ? (
              <Button
                size="sm"
                variant="primary"
                className="mn-passkey-manager__add"
                loading={adding}
                disabled={supported === false}
                onClick={add}
              >
                {m.add}
              </Button>
            ) : undefined
          }
          footer={
            supported === false || notice ? (
              <div className="mn-passkey-manager__notices">
                {supported === false ? (
                  <InlineNotice tone="warning">{m.unsupported}</InlineNotice>
                ) : null}
                {notice ? (
                  <InlineNotice tone={notice.tone} live>
                    {notice.text}
                  </InlineNotice>
                ) : null}
              </div>
            ) : undefined
          }
        >
          <ul className="mn-passkey-manager__list" aria-label={m.listLabel}>
            {passkeys.map((passkey) => (
              <li key={passkey.id} className="mn-passkey-manager__item">
                <span className="mn-passkey-manager__icon" aria-hidden="true">
                  <KeyGlyph />
                </span>
                <div className="mn-passkey-manager__main">
                  <div className="mn-passkey-manager__name-row">
                    {onRename ? (
                      <InlineEditText
                        className="mn-passkey-manager__name"
                        label={m.renameLabel(passkey.name)}
                        value={passkey.name}
                        maxLength={64}
                        validate={(draft) =>
                          draft.trim() ? null : (m.renameEmpty ?? m.renameLabel(passkey.name))
                        }
                        onSave={(next) => onRename(passkey.id, next)}
                      />
                    ) : (
                      <span className="mn-passkey-manager__name">{passkey.name}</span>
                    )}
                    {passkey.synced ? (
                      <Badge size="sm" tone="info">
                        {m.synced}
                      </Badge>
                    ) : null}
                  </div>
                  {passkey.detail ? (
                    <span className="mn-passkey-manager__detail">{passkey.detail}</span>
                  ) : null}
                  <span className="mn-passkey-manager__times">
                    {passkey.createdAt !== undefined ? (
                      <span>
                        {m.created} <RelativeTime date={passkey.createdAt} {...timeProps} />
                      </span>
                    ) : null}
                    <span>
                      {passkey.lastUsedAt !== undefined ? (
                        <>
                          {m.lastUsed} <RelativeTime date={passkey.lastUsedAt} {...timeProps} />
                        </>
                      ) : (
                        m.neverUsed
                      )}
                    </span>
                  </span>
                </div>
                {onRemove ? (
                  <Button
                    size="sm"
                    variant="ghost"
                    className="mn-passkey-manager__remove"
                    aria-label={m.removeLabel(passkey.name)}
                    onClick={() => void remove(passkey)}
                  >
                    {m.remove}
                  </Button>
                ) : null}
              </li>
            ))}
          </ul>
        </SectionCard>
        <p className="mn-passkey-manager__status" role="status" aria-live="polite">
          {announcement}
        </p>
        {element}
      </div>
    );
  },
);

function KeyGlyph() {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <circle cx="5.5" cy="10.5" r="3" />
      <path d="m7.6 8.4 6-6M11.5 4.5l1.5 1.5M10 6l1.25 1.25" />
    </svg>
  );
}
