'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type ElementRef,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { focusReturnTarget } from '../../../internal/dialog-helpers.js';
import { Button } from '../../actions/button/index.js';
import { CopyField } from '../../actions/copy-field/index.js';
import { Alert } from '../../feedback/alert/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { Field, FieldLabel } from '../../forms/form-field/index.js';
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  type DialogContentProps,
} from '../../overlays/dialog/index.js';
import { SecretField } from '../secret-field/index.js';

/** Every visible string in KeyCreatedDialog, with English defaults. */
export interface KeyCreatedDialogMessages {
  /** Dialog title when no `title` prop is given. Default "Copy your new key". */
  title: string;
  /** Heading of the warning shown for a secret. */
  secretNoticeTitle: string;
  /** Body of the warning shown for a secret. */
  secretNotice: string;
  /** Heading of the notice shown for a public (viewable again) value. */
  publicNoticeTitle: string;
  /** Body of the notice shown for a public value. */
  publicNotice: string;
  /** The acknowledgement checkbox. Receives the `label` prop. */
  acknowledge: (label: string) => string;
  /** Announced, and shown, when Done is pressed before acknowledging. */
  acknowledgeRequired: string;
  /** The button that closes the dialog. Default "Done". */
  done: string;
  /** Copy button text. Default "Copy". */
  copy: string;
  /** Copy button text after copying. Default "Copied". */
  copied: string;
  /** Label of the close button, shown once the value is acknowledged. Default "Close". */
  close: string;
}

const DEFAULT_MESSAGES: KeyCreatedDialogMessages = {
  title: 'Copy your new key',
  secretNoticeTitle: 'Copy it now. It will not be shown again.',
  secretNotice:
    'Keep it in a secret manager or a server environment variable, never in source control. If you lose it, create a new one.',
  publicNoticeTitle: 'You can view this again later.',
  publicNotice: 'It is meant to be public, so you can copy it again from its details page.',
  acknowledge: (label) => `I have saved this ${label} somewhere safe.`,
  acknowledgeRequired: 'Confirm that you have saved it before you close this dialog.',
  done: 'Done',
  copy: 'Copy',
  copied: 'Copied',
  close: 'Close',
};

export interface KeyCreatedDialogProps extends Omit<
  DialogContentProps,
  'title' | 'children' | 'footer' | 'closeLabel' | 'closeOnOutsideClick' | 'busy' | 'onCopy'
> {
  /** Whether the dialog is open. Open it right after the server returns the new value. */
  open: boolean;
  /**
   * Called when the dialog asks to open or close. While a secret is not yet
   * acknowledged, the dialog never asks to close.
   */
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** The value to show once: an API key, token, signing secret or client secret. */
  secret: string;
  /**
   * What the value is ("API key", "Signing secret"). Names the copy button
   * ("Copy API key") and goes into the acknowledgement. Default "API key".
   */
  label?: string;
  /** The name the person gave it ("Production server"). Shown as the field label. */
  name?: ReactNode;
  /** Replaces `messages.title`. Becomes the dialog's accessible name. */
  title?: ReactNode;
  /** Text under the title. Becomes the dialog's accessible description. */
  description?: ReactNode;
  /**
   * `secret` (default): shown once, with a warning, and the dialog cannot be
   * closed until the person ticks the acknowledgement. `public`: a value that
   * stays viewable later (a publishable key), so there is nothing to confirm.
   */
  kind?: 'secret' | 'public';
  /** Require the acknowledgement checkbox before closing. Default true for `secret`, false for `public`. */
  requireAcknowledgement?: boolean;
  /** Mask the value behind a reveal toggle (copy still works). Default false: show it plainly. */
  masked?: boolean;
  /** Show the value in a multi-line box (certificates, private keys). Ignored when `masked`. */
  multiline?: boolean;
  /** Extra content under the value, such as when an old key stops working. */
  children?: ReactNode;
  /** Called once when the dialog closes after the value was acknowledged (or for a `public` value). */
  onDone?: (() => void) | undefined;
  /** Called with the copied text after a successful copy. */
  onCopy?: ((text: string) => void) | undefined;
  /** Called when copying fails. The value is selected so it can be copied by hand. */
  onCopyError?: ((error: Error) => void) | undefined;
  /** Replaces any of the default English strings. */
  messages?: Partial<KeyCreatedDialogMessages> | undefined;
}

/**
 * Shows a newly created secret exactly once: the value with a copy button, a
 * "store it now" warning and an acknowledgement the person must tick before
 * the dialog can close. Escape, outside clicks and the close button are
 * blocked until then, so the secret cannot be lost by accident.
 */
export const KeyCreatedDialog = forwardRef<ElementRef<typeof DialogContent>, KeyCreatedDialogProps>(
  function KeyCreatedDialog(
    {
      open,
      onOpenChange,
      secret,
      label = 'API key',
      name,
      title,
      description,
      kind = 'secret',
      requireAcknowledgement,
      masked = false,
      multiline = false,
      children,
      onDone,
      onCopy,
      onCopyError,
      messages: messagesProp,
      size = 'md',
      onEscapeKeyDown,
      onOpenAutoFocus,
      onCloseAutoFocus,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
    const needsAck = requireAcknowledgement ?? kind === 'secret';
    const [acknowledged, setAcknowledged] = useState(false);
    const [nudge, setNudge] = useState(false);
    const checkboxRef = useRef<HTMLButtonElement>(null);
    const returnFocus = useRef<HTMLElement | null>(null);
    const hintId = `mn-key-created-${useId()}`;
    const confirmed = !needsAck || acknowledged;

    // A new value (or reopening) always starts unacknowledged.
    useEffect(() => {
      if (open) {
        setAcknowledged(false);
        setNudge(false);
      }
    }, [open, secret]);

    const close = () => {
      if (!confirmed) return;
      onDone?.();
      onOpenChange?.(false);
    };

    const fieldLabel = name ?? label;

    return (
      <Dialog
        open={open}
        onOpenChange={(next) => {
          if (next) onOpenChange?.(true);
          else close();
        }}
      >
        <DialogContent
          ref={ref}
          {...(description ? {} : { 'aria-describedby': undefined })}
          {...rest}
          size={size}
          closeLabel={confirmed ? messages.close : null}
          closeOnOutsideClick={false}
          onOpenAutoFocus={(event) => {
            // The dialog usually opens after an async create, with no DialogTrigger:
            // remember what had focus so it can be restored on close.
            returnFocus.current = focusReturnTarget();
            onOpenAutoFocus?.(event);
          }}
          onCloseAutoFocus={(event) => {
            onCloseAutoFocus?.(event);
            const target = returnFocus.current;
            if (event.defaultPrevented || !target?.isConnected) return;
            event.preventDefault();
            target.focus();
          }}
          onEscapeKeyDown={(event) => {
            onEscapeKeyDown?.(event);
            if (!confirmed) event.preventDefault();
          }}
          data-kind={kind}
          data-acknowledged={confirmed || undefined}
          className={cx('mn-key-created-dialog', className)}
        >
          <DialogHeader>
            <DialogTitle>{title ?? messages.title}</DialogTitle>
            {description ? <DialogDescription>{description}</DialogDescription> : null}
          </DialogHeader>
          <DialogBody className="mn-key-created-dialog__body">
            {kind === 'secret' ? (
              <Alert tone="warning" title={messages.secretNoticeTitle}>
                {messages.secretNotice}
              </Alert>
            ) : (
              <Alert tone="info" title={messages.publicNoticeTitle}>
                {messages.publicNotice}
              </Alert>
            )}
            <Field className="mn-key-created-dialog__field">
              <FieldLabel>{fieldLabel}</FieldLabel>
              {masked ? (
                <SecretField
                  label={label}
                  value={secret}
                  readOnly
                  copyable
                  {...(onCopy ? { onCopy } : {})}
                  {...(onCopyError ? { onCopyError } : {})}
                />
              ) : (
                <CopyField
                  value={secret}
                  label={label}
                  multiline={multiline}
                  copyLabel={messages.copy}
                  copiedLabel={messages.copied}
                  copyButtonProps={{ variant: 'primary' }}
                  {...(onCopy ? { onCopy } : {})}
                  {...(onCopyError ? { onCopyError } : {})}
                />
              )}
            </Field>
            {children ? <div className="mn-key-created-dialog__extra">{children}</div> : null}
            {needsAck ? (
              <div className="mn-key-created-dialog__ack" data-nudge={nudge || undefined}>
                <Checkbox
                  ref={checkboxRef}
                  label={messages.acknowledge(label)}
                  checked={acknowledged}
                  invalid={nudge && !acknowledged}
                  aria-describedby={nudge && !acknowledged ? hintId : undefined}
                  onCheckedChange={(value) => {
                    setAcknowledged(value === true);
                    if (value === true) setNudge(false);
                  }}
                />
              </div>
            ) : null}
            <p id={hintId} className="mn-key-created-dialog__hint" role="status" aria-live="polite">
              {nudge && !acknowledged ? messages.acknowledgeRequired : null}
            </p>
          </DialogBody>
          <DialogFooter>
            <Button
              type="button"
              variant="primary"
              // Stays enabled: pressing it early explains what is missing instead.
              data-ready={confirmed || undefined}
              className="mn-key-created-dialog__done"
              onClick={() => {
                if (confirmed) {
                  close();
                } else {
                  setNudge(true);
                  checkboxRef.current?.focus();
                }
              }}
            >
              {messages.done}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  },
);
