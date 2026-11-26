'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type ElementRef,
  type FormEvent,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { isThenable } from '../../../internal/dialog-helpers.js';
import { Button } from '../../../components/actions/button/index.js';
import { CopyField } from '../../../components/actions/copy-field/index.js';
import { Field, FieldError, FieldLabel } from '../../../components/forms/form-field/index.js';
import { OTPPinInput } from '../../../components/forms/otp-pin-input/index.js';
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
import { RecoveryCodes } from '../../../components/identity/recovery-codes/index.js';

/** The three steps: scan the QR code, type a code, save the recovery codes. */
export type TwoFactorSetupStep = 'scan' | 'verify' | 'recovery';

/** Visible strings, for translation. */
export interface TwoFactorSetupMessages {
  /** Above the title. Default "Step N of M". */
  stepOf: (step: number, total: number) => string;
  scanTitle: string;
  scanDescription: string;
  /** Alt text of the QR image. */
  qrAlt: string;
  /** Before the setup key. */
  manualEntry: string;
  /** Label of the setup key field. Default "Setup key". */
  secretLabel: string;
  verifyTitle: string;
  verifyDescription: string;
  /** Label of the code field. Default "6-digit code" for the default length. */
  codeLabel: (length: number) => string;
  /** Shown when Verify is pressed before the code is complete. */
  codeIncomplete: (length: number) => string;
  /** Shown when `onVerify` rejects without a message. */
  codeRejected: string;
  recoveryTitle: string;
  recoveryDescription: string;
  /** Accessible name of the code list. Default "Recovery codes". */
  recoveryListLabel: string;
  copyCodes: string;
  copiedCodes: string;
  downloadCodes: string;
  /** File name of the download. Default "recovery-codes.txt". */
  downloadFileName: string;
  /** The acknowledgement checkbox. */
  acknowledge: string;
  /** Shown when Done is pressed without the acknowledgement. */
  acknowledgeMissing: string;
  cancel: string;
  back: string;
  next: string;
  verify: string;
  done: string;
  close: string;
}

export const defaultTwoFactorSetupMessages: TwoFactorSetupMessages = {
  stepOf: (s, t) => `Step ${s} of ${t}`,
  scanTitle: 'Set up two-step verification',
  scanDescription:
    'Scan this QR code with an authenticator app, such as 1Password, Google Authenticator or Microsoft Authenticator.',
  qrAlt: 'QR code for your authenticator app',
  manualEntry: 'Can’t scan it? Enter this key in the app instead.',
  secretLabel: 'Setup key',
  verifyTitle: 'Enter the code from your app',
  verifyDescription: 'Your authenticator app now shows a code that changes every 30 seconds.',
  codeLabel: (n) => `${n}-digit code`,
  codeIncomplete: (n) => `Enter all ${n} digits.`,
  codeRejected: 'That code didn’t work. Check the time on your device and try the newest code.',
  recoveryTitle: 'Save your recovery codes',
  recoveryDescription:
    'If you lose your device, each code lets you sign in once. Keep them somewhere safe, such as a password manager.',
  recoveryListLabel: 'Recovery codes',
  copyCodes: 'Copy codes',
  copiedCodes: 'Copied',
  downloadCodes: 'Download',
  downloadFileName: 'recovery-codes.txt',
  acknowledge: 'I have saved my recovery codes',
  acknowledgeMissing: 'Confirm that you have saved the codes.',
  cancel: 'Cancel',
  back: 'Back',
  next: 'Next',
  verify: 'Verify',
  done: 'Done',
  close: 'Close',
};

export interface TwoFactorSetupProps extends Omit<
  DialogContentProps,
  'title' | 'children' | 'footer' | 'busy' | 'onSubmit'
> {
  /** Controlled open state. */
  open?: boolean | undefined;
  /** Uncontrolled initial open state. */
  defaultOpen?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** The element that opens the dialog (rendered with `asChild`), e.g. a Button. */
  trigger?: ReactElement | undefined;
  /** The shared secret (base32) for manual entry. Shown in groups of four. */
  secret: string;
  /**
   * The QR code image of the `otpauth://` URI, as a URL or data URL from the
   * server. Monarch does not encode QR codes; use `qrCode` to render your own.
   */
  qrCodeSrc?: string | undefined;
  /** The QR code as an element (for example an SVG); wins over `qrCodeSrc`. */
  qrCode?: ReactNode;
  /** The account the code is for, shown under the QR code (e.g. an email address). */
  accountName?: ReactNode;
  /** Digits in a code. Default 6. */
  codeLength?: number | undefined;
  /**
   * Checks the code and turns two-step verification on. Return (or resolve
   * to) the recovery codes to show them; a rejection shows its message and
   * clears the code.
   */
  onVerify: (code: string) => unknown;
  /** Recovery codes, when the product has them before verifying. */
  recoveryCodes?: string[] | undefined;
  /** Called when setup is finished (after the recovery codes are acknowledged, or right after verifying when there are none). */
  onComplete?: (() => void) | undefined;
  /** Called when the dialog is dismissed before finishing. */
  onCancel?: (() => void) | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<TwoFactorSetupMessages> | undefined;
}

function groupSecret(secret: string) {
  return secret
    .replace(/\s+/g, '')
    .replace(/(.{4})/g, '$1 ')
    .trim();
}

/**
 * Sets up two-step verification with an authenticator app, in a dialog of
 * three steps: scan a QR code (or type the key), enter a code to prove it
 * works, then save the recovery codes.
 */
export const TwoFactorSetup = forwardRef<ElementRef<typeof DialogContent>, TwoFactorSetupProps>(
  function TwoFactorSetup(
    {
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      trigger,
      secret,
      qrCodeSrc,
      qrCode,
      accountName,
      codeLength = 6,
      onVerify,
      recoveryCodes: recoveryCodesProp,
      onComplete,
      onCancel,
      messages,
      size = 'sm',
      closeOnOutsideClick = false,
      onOpenAutoFocus,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultTwoFactorSetupMessages, ...messages };
    const [openState, setOpenState] = useState(defaultOpen);
    const open = openProp ?? openState;
    const [step, setStep] = useState<TwoFactorSetupStep>('scan');
    const [code, setCode] = useState('');
    const [verifying, setVerifying] = useState(false);
    const [codeError, setCodeError] = useState<ReactNode>(null);
    const [codes, setCodes] = useState<string[]>([]);
    const titleRef = useRef<HTMLHeadingElement>(null);
    const codeRef = useRef<HTMLInputElement>(null);
    const mounted = useRef(true);
    const total = 3;
    const stepNumber = step === 'scan' ? 1 : step === 'verify' ? 2 : 3;

    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    // Every opening starts at the first step with nothing typed.
    useEffect(() => {
      if (open) {
        setStep('scan');
        setCode('');
        setCodeError(null);
        setCodes([]);
      }
    }, [open]);

    const setOpen = (next: boolean) => {
      if (openProp === undefined) setOpenState(next);
      onOpenChange?.(next);
    };

    const handleOpenChange = (next: boolean) => {
      if (!next && verifying) return;
      if (!next) onCancel?.();
      setOpen(next);
    };

    const goTo = (next: TwoFactorSetupStep) => {
      setStep(next);
      // Move focus to the new step: the code field, or the title.
      requestAnimationFrame(() => {
        if (next === 'verify') codeRef.current?.focus();
        else titleRef.current?.focus();
      });
    };

    const finish = () => {
      onComplete?.();
      setOpen(false);
    };

    const verify = async (value: string) => {
      if (verifying) return;
      if (value.length < codeLength) {
        setCodeError(m.codeIncomplete(codeLength));
        codeRef.current?.focus();
        return;
      }
      setCodeError(null);
      let result: unknown;
      try {
        const out = onVerify(value);
        if (isThenable(out)) {
          setVerifying(true);
          result = await out;
        } else {
          result = out;
        }
      } catch (error) {
        if (!mounted.current) return;
        setVerifying(false);
        setCode('');
        setCodeError(error instanceof Error && error.message ? error.message : m.codeRejected);
        requestAnimationFrame(() => codeRef.current?.focus());
        return;
      }
      if (!mounted.current) return;
      setVerifying(false);
      const list = Array.isArray(result)
        ? result.filter((c): c is string => typeof c === 'string')
        : (recoveryCodesProp ?? []);
      if (list.length === 0) {
        finish();
        return;
      }
      setCodes(list);
      goTo('recovery');
    };

    const submitCode = (event: FormEvent) => {
      event.preventDefault();
      void verify(code);
    };

    const title =
      step === 'scan' ? m.scanTitle : step === 'verify' ? m.verifyTitle : m.recoveryTitle;
    const description =
      step === 'scan'
        ? m.scanDescription
        : step === 'verify'
          ? m.verifyDescription
          : m.recoveryDescription;

    return (
      <Dialog open={open} onOpenChange={handleOpenChange}>
        {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
        <DialogContent
          ref={ref}
          {...rest}
          size={size}
          busy={verifying}
          closeLabel={m.close}
          closeOnOutsideClick={closeOnOutsideClick}
          data-step={step}
          className={cx('mn-two-factor-setup', className)}
          onOpenAutoFocus={(event) => {
            onOpenAutoFocus?.(event);
            if (event.defaultPrevented) return;
            event.preventDefault();
            titleRef.current?.focus();
          }}
        >
          <form
            className="mn-two-factor-setup__form"
            noValidate
            onSubmit={(event) => {
              if (step === 'verify') submitCode(event);
              else {
                event.preventDefault();
                // The recovery step finishes through RecoveryCodes' own Done button.
                if (step === 'scan') goTo('verify');
              }
            }}
          >
            <DialogHeader>
              <p className="mn-two-factor-setup__step">{m.stepOf(stepNumber, total)}</p>
              <DialogTitle ref={titleRef} tabIndex={-1} className="mn-two-factor-setup__title">
                {title}
              </DialogTitle>
              <DialogDescription>{description}</DialogDescription>
            </DialogHeader>
            <DialogBody className="mn-two-factor-setup__body">
              {step === 'scan' ? (
                <>
                  {qrCode || qrCodeSrc || accountName ? (
                    <figure className="mn-two-factor-setup__qr">
                      <div className="mn-two-factor-setup__qr-code" data-mn-theme="light">
                        {qrCode ??
                          (qrCodeSrc ? (
                            <img
                              className="mn-two-factor-setup__qr-img"
                              src={qrCodeSrc}
                              alt={m.qrAlt}
                              width={176}
                              height={176}
                            />
                          ) : null)}
                      </div>
                      {accountName ? (
                        <figcaption className="mn-two-factor-setup__account">
                          {accountName}
                        </figcaption>
                      ) : null}
                    </figure>
                  ) : null}
                  <p className="mn-two-factor-setup__manual">{m.manualEntry}</p>
                  <CopyField label={m.secretLabel} value={groupSecret(secret)} />
                </>
              ) : null}
              {step === 'verify' ? (
                <Field invalid={!!codeError}>
                  <FieldLabel>{m.codeLabel(codeLength)}</FieldLabel>
                  <OTPPinInput
                    ref={codeRef}
                    length={codeLength}
                    value={code}
                    {...(codeLength === 6 ? { groupSize: 3 } : {})}
                    readOnly={verifying}
                    onValueChange={(next) => {
                      setCode(next);
                      if (codeError) setCodeError(null);
                    }}
                    onComplete={(value) => void verify(value)}
                  />
                  <FieldError>{codeError}</FieldError>
                </Field>
              ) : null}
              {step === 'recovery' ? (
                <RecoveryCodes
                  codes={codes}
                  fileName={m.downloadFileName}
                  onAcknowledge={finish}
                  messages={{
                    listLabel: m.recoveryListLabel,
                    copy: m.copyCodes,
                    copied: m.copiedCodes,
                    download: m.downloadCodes,
                    acknowledge: m.acknowledge,
                    acknowledgeMissing: m.acknowledgeMissing,
                    continue: m.done,
                  }}
                />
              ) : null}
            </DialogBody>
            {step !== 'recovery' ? (
              <DialogFooter className="mn-two-factor-setup__footer">
                {step === 'scan' ? (
                  <>
                    <DialogClose asChild>
                      <Button>{m.cancel}</Button>
                    </DialogClose>
                    <Button type="submit" variant="primary">
                      {m.next}
                    </Button>
                  </>
                ) : null}
                {step === 'verify' ? (
                  <>
                    <Button disabled={verifying} onClick={() => goTo('scan')}>
                      {m.back}
                    </Button>
                    <Button type="submit" variant="primary" loading={verifying}>
                      {m.verify}
                    </Button>
                  </>
                ) : null}
              </DialogFooter>
            ) : null}
          </form>
        </DialogContent>
      </Dialog>
    );
  },
);
