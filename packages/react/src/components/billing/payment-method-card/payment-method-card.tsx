'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { useHydrated } from '../../../internal/dates/client-time.js';
import { Button } from '../../actions/button/index.js';
import { Badge } from '../../data-display/badge/index.js';
import { ConfirmDialog, type ConfirmDetails } from '../../overlays/confirm-dialog/index.js';

/** Whether a card can still be charged. */
export type CardExpiryStatus = 'valid' | 'expiring' | 'expired';

/** Display names for common card networks, keyed by lower-case id. */
export const cardBrandNames: Readonly<Record<string, string>> = {
  visa: 'Visa',
  mastercard: 'Mastercard',
  amex: 'American Express',
  rupay: 'RuPay',
  discover: 'Discover',
  diners: 'Diners Club',
  jcb: 'JCB',
  unionpay: 'UnionPay',
  maestro: 'Maestro',
};

/**
 * Works out whether a card has expired or expires within `soonMonths`
 * (default 1: this month or next). Cards are valid to the end of the expiry
 * month. Pass `now` for a stable result on the server and in tests.
 */
export function cardExpiryStatus(
  month: number,
  year: number,
  now: Date = new Date(),
  soonMonths = 1,
): CardExpiryStatus {
  const fullYear = year < 100 ? 2000 + year : year;
  const expiry = fullYear * 12 + (month - 1);
  const current = now.getFullYear() * 12 + now.getMonth();
  if (expiry < current) return 'expired';
  if (expiry - current <= soonMonths) return 'expiring';
  return 'valid';
}

/** Visible and assistive-technology text. English defaults. */
export interface PaymentMethodCardMessages {
  /** The card's name, e.g. "Visa ending in 4242". */
  name: (brand: string, last4: string) => string;
  /** Expiry line, given "MM/YY". */
  expires: (date: string) => string;
  /** Expiry line for an expired card, given "MM/YY". */
  expiredOn: (date: string) => string;
  /** Badge on the default method. */
  defaultBadge: string;
  /** Badge when the card expires soon. */
  expiringBadge: string;
  /** Badge when the card has expired. */
  expiredBadge: string;
  makeDefault: string;
  remove: string;
  /**
   * Accessible name of an action button: the visible label first, then the
   * card, so "Remove" buttons on a list of cards can be told apart.
   */
  actionLabel: (action: string, name: string) => string;
  /** Confirmation title. */
  removeTitle: (name: string) => string;
  /** Confirmation description. */
  removeDescription: string;
  /** Confirming button label. */
  removeConfirm: string;
  cancel: string;
}

export const defaultPaymentMethodCardMessages: PaymentMethodCardMessages = {
  name: (brand, last4) => `${brand} ending in ${last4}`,
  expires: (date) => `Expires ${date}`,
  expiredOn: (date) => `Expired ${date}`,
  defaultBadge: 'Default',
  expiringBadge: 'Expires soon',
  expiredBadge: 'Expired',
  makeDefault: 'Make default',
  remove: 'Remove',
  actionLabel: (action, name) => `${action}, ${name}`,
  removeTitle: (name) => `Remove ${name}?`,
  removeDescription:
    'It will no longer be charged. To use it again, add it as a new payment method.',
  removeConfirm: 'Remove card',
  cancel: 'Cancel',
};

export interface PaymentMethodCardProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** Card network id (`visa`, `mastercard`, `amex`, `rupay`…) or any display name. */
  brand: string;
  /** The last four digits, exactly as the payment provider returns them. */
  last4: string;
  /** Expiry month, 1–12. */
  expiryMonth?: number | undefined;
  /** Expiry year, two or four digits. */
  expiryYear?: number | undefined;
  /**
   * Overrides the worked-out expiry status. Pass it when the payment provider
   * already says the card is expired, or to keep server and client in step.
   */
  expiryStatus?: CardExpiryStatus | undefined;
  /**
   * "Today" for working out the expiry status. Default: the current date,
   * read after hydration (server HTML shows the card as valid until then;
   * pass `now` or `expiryStatus` to render the status on the server).
   */
  now?: Date | undefined;
  /** Cardholder name, shown under the card name. */
  holderName?: ReactNode;
  /** A logo for the network. Default: a generic card glyph. Always decorative. */
  brandIcon?: ReactNode;
  /** This is the method charged by default: shows a "Default" badge and hides "Make default". */
  isDefault?: boolean | undefined;
  /**
   * Shows "Make default" on a non-default card. Return a promise to show
   * progress on the button until it settles.
   */
  onMakeDefault?: (() => unknown) | undefined;
  /**
   * Called when `onMakeDefault` throws or its promise rejects, so the product
   * can show the failure. The button returns to its idle state either way.
   */
  onMakeDefaultError?: ((error: unknown) => void) | undefined;
  /**
   * Shows "Remove", which asks for confirmation first. Return a promise to
   * keep the confirmation open in a pending state; a rejection is shown in
   * the dialog so the person can retry.
   */
  onRemove?: ((details: ConfirmDetails) => unknown) | undefined;
  /** Disables Remove, for example on the only method of an active subscription. */
  removeDisabled?: boolean | undefined;
  /** Extra actions after Make default and Remove. */
  actions?: ReactNode;
  /** Heading level of the card name. Default 3. */
  headingLevel?: 2 | 3 | 4 | undefined;
  messages?: Partial<PaymentMethodCardMessages> | undefined;
}

function CardGlyph() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.75} aria-hidden>
      <rect x="2.5" y="5" width="19" height="14" rx="2" />
      <path d="M2.5 10h19M6 15h4" strokeLinecap="round" />
    </svg>
  );
}

/**
 * A saved payment card: network, last four digits, expiry, holder, a default
 * badge, and actions to make it the default or remove it after confirmation.
 * It never shows or collects full card numbers; adding a card belongs to the
 * payment provider's own hosted form.
 */
export const PaymentMethodCard = forwardRef<HTMLElement, PaymentMethodCardProps>(
  function PaymentMethodCard(
    {
      brand,
      last4,
      expiryMonth,
      expiryYear,
      expiryStatus,
      now,
      holderName,
      brandIcon,
      isDefault = false,
      onMakeDefault,
      onMakeDefaultError,
      onRemove,
      removeDisabled = false,
      actions,
      headingLevel = 3,
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultPaymentMethodCardMessages, ...messages };
    const titleId = useId();
    const Heading = `h${headingLevel}` as 'h3';
    const brandName = cardBrandNames[brand.toLowerCase()] ?? brand;
    const name = m.name(brandName, last4);
    const hasExpiry = expiryMonth !== undefined && expiryYear !== undefined;
    // The server's clock is not the viewer's: without `now`, the status is
    // worked out only after hydration so the two renders agree.
    const hydrated = useHydrated();
    const today = now ?? (hydrated ? new Date() : null);
    const status: CardExpiryStatus =
      expiryStatus ??
      (hasExpiry && today ? cardExpiryStatus(expiryMonth, expiryYear, today) : 'valid');
    const expiryText = hasExpiry
      ? `${String(expiryMonth).padStart(2, '0')}/${String(expiryYear % 100).padStart(2, '0')}`
      : null;

    const [makingDefault, setMakingDefault] = useState(false);
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);
    const makeDefault = async () => {
      if (!onMakeDefault || makingDefault) return;
      setMakingDefault(true);
      try {
        await onMakeDefault();
      } catch (error) {
        // Reported to the product; never an unhandled rejection.
        onMakeDefaultError?.(error);
      } finally {
        if (mounted.current) setMakingDefault(false);
      }
    };

    const showMakeDefault = !isDefault && onMakeDefault !== undefined;
    const hasActions = showMakeDefault || onRemove !== undefined || actions;

    return (
      <article
        ref={ref}
        aria-labelledby={titleId}
        {...rest}
        className={cx('mn-payment-method-card', className)}
        data-default={isDefault || undefined}
        data-expiry={status}
      >
        <span className="mn-payment-method-card__icon" aria-hidden>
          {brandIcon ?? <CardGlyph />}
        </span>
        <div className="mn-payment-method-card__body">
          <div className="mn-payment-method-card__title-row">
            <Heading className="mn-payment-method-card__name" id={titleId}>
              {name}
            </Heading>
            {isDefault ? (
              <Badge tone="accent" variant="subtle" size="sm">
                {m.defaultBadge}
              </Badge>
            ) : null}
            {status === 'expired' ? (
              <Badge tone="danger" variant="subtle" size="sm">
                {m.expiredBadge}
              </Badge>
            ) : status === 'expiring' ? (
              <Badge tone="warning" variant="subtle" size="sm">
                {m.expiringBadge}
              </Badge>
            ) : null}
          </div>
          {holderName || expiryText ? (
            <p className="mn-payment-method-card__meta">
              {holderName ? (
                <span className="mn-payment-method-card__holder">{holderName}</span>
              ) : null}
              {expiryText ? (
                <span className="mn-payment-method-card__expiry">
                  {status === 'expired' ? m.expiredOn(expiryText) : m.expires(expiryText)}
                </span>
              ) : null}
            </p>
          ) : null}
        </div>
        {hasActions ? (
          <div className="mn-payment-method-card__actions">
            {showMakeDefault ? (
              <Button
                size="sm"
                variant="secondary"
                loading={makingDefault}
                disabled={status === 'expired'}
                onClick={() => void makeDefault()}
                aria-label={m.actionLabel(m.makeDefault, name)}
              >
                {m.makeDefault}
              </Button>
            ) : null}
            {onRemove ? (
              <ConfirmDialog
                tone="danger"
                title={m.removeTitle(name)}
                description={m.removeDescription}
                confirmLabel={m.removeConfirm}
                cancelLabel={m.cancel}
                onConfirm={onRemove}
                trigger={
                  <Button
                    size="sm"
                    variant="ghost"
                    disabled={removeDisabled}
                    aria-label={m.actionLabel(m.remove, name)}
                  >
                    {m.remove}
                  </Button>
                }
              />
            ) : null}
            {actions}
          </div>
        ) : null}
      </article>
    );
  },
);
