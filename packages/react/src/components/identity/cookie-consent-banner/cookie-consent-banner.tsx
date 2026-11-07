'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type FocusEvent,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { Link } from '../../navigation/link/index.js';
import { VisuallyHidden } from '../../layout/visually-hidden/index.js';
import { useConsent, type ConsentRegime } from '../consent-manager/index.js';

/** The heading and body text of the banner for one regime. */
export interface CookieConsentBannerText {
  /** Short heading; it also names the region. */
  title: ReactNode;
  /** One or two sentences saying what is used and what the person can do. */
  description: ReactNode;
}

/** Visible and announced strings. Override them to translate or to use your legal team's wording. */
export interface CookieConsentBannerMessages {
  /** Text when consent is asked for before optional cookies are set. */
  'opt-in': CookieConsentBannerText;
  /** Text when optional cookies run and the person can opt out of sale or sharing. */
  'opt-out': CookieConsentBannerText;
  /** Text when the banner only informs. */
  notice: CookieConsentBannerText;
  /** Turns every optional category on (opt-in). */
  acceptAll: string;
  /** Turns every optional category off (opt-in). */
  rejectAll: string;
  /** Opens the preferences dialog (opt-in). */
  customize: string;
  /** Dismisses the banner without changing choices (opt-out and notice). */
  acknowledge: string;
  /** Turns off every sale-or-sharing category (opt-out). */
  doNotSell: string;
  /** Shown in the opt-out banner when a Global Privacy Control signal was honoured. */
  gpcHonoured: string;
  /** Label of the policy link built from `policyHref`. */
  policyLabel: string;
  /** Announced politely once when the banner appears. */
  announcement: string;
}

const DEFAULT_MESSAGES: CookieConsentBannerMessages = {
  'opt-in': {
    title: 'Cookies on this site',
    description:
      'We use strictly necessary cookies to make this site work. With your permission we would also like to use optional cookies to remember your preferences, measure use and show relevant ads.',
  },
  'opt-out': {
    title: 'Cookies on this site',
    description:
      'We use cookies to make this site work, remember your preferences, measure use and show relevant ads. You can opt out of the sale or sharing of your personal information at any time.',
  },
  notice: {
    title: 'Cookies on this site',
    description: 'We use cookies to make this site work and to understand how it is used.',
  },
  acceptAll: 'Accept all',
  rejectAll: 'Reject all',
  customize: 'Customize',
  acknowledge: 'OK',
  doNotSell: 'Do not sell or share my personal information',
  gpcHonoured:
    'Your browser’s Global Privacy Control signal was honoured: sale and sharing are off.',
  policyLabel: 'Cookie policy',
  announcement: 'Cookie notice shown at the end of the page.',
};

/** Partial overrides: each regime's text can be overridden field by field. */
export type CookieConsentBannerMessageOverrides = {
  [
    K in keyof CookieConsentBannerMessages
  ]?: CookieConsentBannerMessages[K] extends CookieConsentBannerText
    ? Partial<CookieConsentBannerText>
    : CookieConsentBannerMessages[K];
};

export interface CookieConsentBannerProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /**
   * `bottom` (default): a full-width bar along the bottom of the viewport.
   * `bottom-start`: a compact card in the bottom start corner.
   */
  position?: 'bottom' | 'bottom-start';
  /** URL of the cookie or privacy policy. Rendered as a link after the description. */
  policyHref?: string;
  /** Label of the `policyHref` link. Default "Cookie policy". */
  policyLabel?: ReactNode;
  /** Your own link element (e.g. a router link), used instead of `policyHref`. */
  policyLink?: ReactNode;
  /**
   * Button variant used for both "Accept all" and "Reject all", so the two
   * always look the same. Default `secondary`.
   */
  choiceVariant?: 'primary' | 'secondary';
  /** Override visible and announced strings. */
  messages?: CookieConsentBannerMessageOverrides;
}

function mergeMessages(
  overrides: CookieConsentBannerMessageOverrides | undefined,
): CookieConsentBannerMessages {
  if (!overrides) return DEFAULT_MESSAGES;
  return {
    ...DEFAULT_MESSAGES,
    ...overrides,
    'opt-in': { ...DEFAULT_MESSAGES['opt-in'], ...overrides['opt-in'] },
    'opt-out': { ...DEFAULT_MESSAGES['opt-out'], ...overrides['opt-out'] },
    notice: { ...DEFAULT_MESSAGES.notice, ...overrides.notice },
  };
}

/**
 * The cookie banner. It reads the visitor's regime from `ConsentProvider` and
 * shows the matching choice: "Reject all", "Accept all" and "Customize" where
 * consent is asked first (opt-in), a "Do not sell or share" choice where
 * people opt out, or a plain notice. It renders nothing on the server and
 * after a choice, and it never blocks the page: it is a labelled region that
 * does not take focus, trap focus or close on Escape.
 */
export const CookieConsentBanner = forwardRef<HTMLDivElement, CookieConsentBannerProps>(
  function CookieConsentBanner(
    {
      position = 'bottom',
      policyHref,
      policyLabel,
      policyLink,
      choiceVariant = 'secondary',
      messages: messageOverrides,
      className,
      onFocusCapture,
      ...rest
    },
    ref,
  ) {
    const consent = useConsent();
    // Where focus was before it entered the banner, so a choice that removes
    // the banner can put focus back there instead of dropping it on <body>.
    const cameFrom = useRef<HTMLElement | null>(null);
    const titleId = useId();
    const descriptionId = useId();
    const messages = mergeMessages(messageOverrides);
    const visible = consent.status === 'pending';
    const [announcement, setAnnouncement] = useState('');

    // Fill the live region after it is in the document, so it is announced once.
    useEffect(() => {
      if (!visible) {
        setAnnouncement('');
        return;
      }
      const timer = setTimeout(() => setAnnouncement(messages.announcement), 50);
      return () => clearTimeout(timer);
    }, [visible, messages.announcement]);

    if (!visible) return null;

    const trackFocus = (event: FocusEvent<HTMLDivElement>) => {
      onFocusCapture?.(event);
      const from = event.relatedTarget;
      if (from instanceof HTMLElement && !event.currentTarget.contains(from))
        cameFrom.current = from;
    };

    /** Runs a choice; if focus was in the banner, returns it to where it came from. */
    const decide = (action: () => void) => () => {
      const banner = document.activeElement?.closest('.mn-cookie-consent-banner');
      action();
      const target = cameFrom.current;
      if (banner && target?.isConnected) setTimeout(() => target.focus(), 0);
    };

    const regime: ConsentRegime = consent.regime;
    const text = messages[regime];
    const policy =
      policyLink ??
      (policyHref ? (
        <Link href={policyHref} className="mn-cookie-consent-banner__policy">
          {policyLabel ?? messages.policyLabel}
        </Link>
      ) : null);

    return (
      <div
        ref={ref}
        role="region"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        {...rest}
        onFocusCapture={trackFocus}
        data-regime={regime}
        data-position={position}
        data-gpc={consent.gpc || undefined}
        className={cx('mn-cookie-consent-banner', className)}
      >
        <div className="mn-cookie-consent-banner__text">
          <p id={titleId} className="mn-cookie-consent-banner__title">
            {text.title}
          </p>
          <p id={descriptionId} className="mn-cookie-consent-banner__description">
            {text.description}
            {policy ? <> {policy}</> : null}
          </p>
          {regime === 'opt-out' && consent.gpc ? (
            <p className="mn-cookie-consent-banner__gpc">{messages.gpcHonoured}</p>
          ) : null}
        </div>
        <div className="mn-cookie-consent-banner__actions">
          {regime === 'opt-in' ? (
            <>
              <Button
                variant="ghost"
                className="mn-cookie-consent-banner__customize"
                aria-haspopup="dialog"
                onClick={() => consent.setPreferencesOpen(true)}
              >
                {messages.customize}
              </Button>
              <Button
                variant={choiceVariant}
                className="mn-cookie-consent-banner__choice"
                onClick={decide(consent.rejectAll)}
              >
                {messages.rejectAll}
              </Button>
              <Button
                variant={choiceVariant}
                className="mn-cookie-consent-banner__choice"
                onClick={decide(consent.acceptAll)}
              >
                {messages.acceptAll}
              </Button>
            </>
          ) : null}
          {regime === 'opt-out' ? (
            <>
              {consent.gpc ? null : (
                <Button
                  className="mn-cookie-consent-banner__opt-out"
                  onClick={decide(consent.optOutOfSaleOrSharing)}
                >
                  {messages.doNotSell}
                </Button>
              )}
              <Button
                className="mn-cookie-consent-banner__choice"
                onClick={decide(consent.acknowledge)}
              >
                {messages.acknowledge}
              </Button>
            </>
          ) : null}
          {regime === 'notice' ? (
            <Button
              className="mn-cookie-consent-banner__choice"
              onClick={decide(consent.acknowledge)}
            >
              {messages.acknowledge}
            </Button>
          ) : null}
        </div>
        <VisuallyHidden role="status" aria-live="polite">
          {announcement}
        </VisuallyHidden>
      </div>
    );
  },
);
