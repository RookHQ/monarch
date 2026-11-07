'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useState,
  type ButtonHTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { Switch } from '../../forms/switch/index.js';
import { Link } from '../../navigation/link/index.js';
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
} from '../../overlays/dialog/index.js';
import { useConsent, type ConsentRegime } from '../consent-manager/index.js';

/** Visible strings. Override them to translate or to use your legal team's wording. */
export interface PrivacyChoicesLinkMessages {
  /** Dialog title. */
  title: string;
  /** What sale or sharing means here, under the title. */
  description: ReactNode;
  /** Label of the switch. */
  switchLabel: string;
  /** Help under the switch label. */
  switchDescription: ReactNode;
  /** Shown instead of the help while a Global Privacy Control signal is on; the switch is locked off. */
  gpcLocked: string;
  /** Saves the choice and closes. */
  save: string;
  /** Closes without saving. */
  cancel: string;
  /** Label of the policy link built from `policyHref`. */
  policyLabel: string;
}

const DEFAULT_MESSAGES: PrivacyChoicesLinkMessages = {
  title: 'Your privacy choices',
  description:
    'We may share information about your visits, such as pages viewed and device identifiers, with advertising partners. You can turn this off at any time.',
  switchLabel: 'Allow sale or sharing',
  switchDescription: 'When off, we stop selling or sharing your personal information.',
  gpcLocked: 'Your browser’s Global Privacy Control signal is on, so sale and sharing stay off.',
  save: 'Save',
  cancel: 'Cancel',
  policyLabel: 'Privacy policy',
};

export interface PrivacyChoicesLinkProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'children'
> {
  /** The visible label. Default "Your privacy choices". */
  label?: ReactNode;
  /** An icon before the label, e.g. your approved privacy-choices icon. Decorative. */
  icon?: ReactNode;
  /** The regimes in which the control renders. Default `['opt-out']`; elsewhere it renders nothing. */
  showIn?: readonly ConsentRegime[];
  /** URL of the privacy policy, linked in the dialog. */
  policyHref?: string;
  /** Your own link element (e.g. a router link), used instead of `policyHref`. */
  policyLink?: ReactNode;
  /** Override the dialog's strings. */
  messages?: Partial<PrivacyChoicesLinkMessages>;
}

/**
 * The footer "Your privacy choices" control for regions where people opt out.
 * It opens a small dialog with one switch, "Allow sale or sharing", bound to
 * the sale-or-sharing consent categories; turning it off and saving opts the
 * person out. While a Global Privacy Control signal is on, the switch is off
 * and locked, and the dialog says why. Renders nothing outside `showIn`.
 */
export const PrivacyChoicesLink = forwardRef<HTMLButtonElement, PrivacyChoicesLinkProps>(
  function PrivacyChoicesLink(
    {
      label = 'Your privacy choices',
      icon,
      showIn = ['opt-out'],
      policyHref,
      policyLink,
      messages: overrides,
      className,
      ...rest
    },
    ref,
  ) {
    const consent = useConsent();
    const messages = { ...DEFAULT_MESSAGES, ...overrides };
    const [open, setOpen] = useState(false);
    const saleCategories = consent.categories.filter((c) => c.saleOrSharing && !c.required);
    const currentlyAllowed =
      !consent.gpc &&
      saleCategories.length > 0 &&
      saleCategories.every((c) => consent.choices[c.id] === true);
    const [allow, setAllow] = useState(currentlyAllowed);
    const switchId = useId();
    const noteId = useId();

    // Every opening starts from the current choice.
    useEffect(() => {
      if (open) setAllow(currentlyAllowed);
      // Only on opening: later changes come from the switch.
    }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

    if (!showIn.includes(consent.regime)) return null;

    const save = () => {
      if (!allow || consent.gpc) consent.optOutOfSaleOrSharing();
      else consent.save(Object.fromEntries(saleCategories.map((c) => [c.id, true])));
      setOpen(false);
    };
    const policy =
      policyLink ??
      (policyHref ? (
        <Link href={policyHref} className="mn-privacy-choices-link__policy">
          {messages.policyLabel}
        </Link>
      ) : null);

    return (
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogTrigger asChild>
          <button
            ref={ref}
            type="button"
            {...rest}
            data-gpc={consent.gpc || undefined}
            className={cx('mn-privacy-choices-link', className)}
          >
            {icon ? (
              <span className="mn-privacy-choices-link__icon" aria-hidden>
                {icon}
              </span>
            ) : null}
            <span className="mn-privacy-choices-link__label">{label}</span>
          </button>
        </DialogTrigger>
        <DialogContent
          size="sm"
          className="mn-privacy-choices-link__dialog"
          data-gpc={consent.gpc || undefined}
        >
          <DialogHeader>
            <DialogTitle>{messages.title}</DialogTitle>
            <DialogDescription>{messages.description}</DialogDescription>
            {policy ? <p className="mn-privacy-choices-link__policy-line">{policy}</p> : null}
          </DialogHeader>
          <DialogBody>
            <div className="mn-privacy-choices-link__setting">
              <div className="mn-privacy-choices-link__setting-text">
                <label htmlFor={switchId} className="mn-privacy-choices-link__setting-label">
                  {messages.switchLabel}
                </label>
                <p id={noteId} className="mn-privacy-choices-link__setting-note">
                  {consent.gpc ? messages.gpcLocked : messages.switchDescription}
                </p>
              </div>
              <Switch
                id={switchId}
                checked={consent.gpc ? false : allow}
                disabled={consent.gpc}
                aria-describedby={noteId}
                onCheckedChange={setAllow}
              />
            </div>
          </DialogBody>
          <DialogFooter>
            <DialogClose asChild>
              <Button>{messages.cancel}</Button>
            </DialogClose>
            <Button variant="primary" onClick={save}>
              {messages.save}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  },
);
