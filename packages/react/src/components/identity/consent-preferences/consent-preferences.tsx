'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type ElementRef,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { focusReturnTarget } from '../../../internal/dialog-helpers.js';
import { Button } from '../../actions/button/index.js';
import { Switch } from '../../forms/switch/index.js';
import { Link } from '../../navigation/link/index.js';
import { RelativeTime } from '../../data-display/relative-time/index.js';
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
import { useConsent, type ConsentCategory } from '../consent-manager/index.js';

/** Visible strings. Override them to translate or to use your legal team's wording. */
export interface ConsentPreferencesMessages {
  /** Dialog title. */
  title: string;
  /** Dialog description, under the title. */
  description: ReactNode;
  /** Shown next to the locked switch of a required category. */
  alwaysOn: string;
  /** Shown under a sale-or-sharing category while a Global Privacy Control signal is on. */
  gpcLocked: string;
  /** Turns every optional category off and closes. */
  rejectAll: string;
  /** Turns every optional category on and closes. */
  acceptAll: string;
  /** Saves the switches as they are and closes. */
  save: string;
  /** Withdraws the saved choice; the banner asks again. */
  withdraw: string;
  /** Label before the policy version of the saved choice. */
  policyVersion: string;
  /** Label before the time of the saved choice. */
  lastUpdated: string;
  /** Label of the policy link built from `policyHref`. */
  policyLabel: string;
}

const DEFAULT_MESSAGES: ConsentPreferencesMessages = {
  title: 'Cookie settings',
  description:
    'Choose which optional cookies we may use. Strictly necessary cookies keep the site working and are always on.',
  alwaysOn: 'Always on',
  gpcLocked: 'Off because your browser’s Global Privacy Control signal is on.',
  rejectAll: 'Reject all',
  acceptAll: 'Accept all',
  save: 'Save choices',
  withdraw: 'Withdraw consent',
  policyVersion: 'Policy version',
  lastUpdated: 'Saved',
  policyLabel: 'Cookie policy',
};

export interface ConsentPreferencesProps extends Omit<
  DialogContentProps,
  'title' | 'children' | 'footer'
> {
  /** Controlled open state. Default: the shared `preferencesOpen` from `ConsentProvider`. */
  open?: boolean;
  /** Called when the dialog opens or closes. */
  onOpenChange?: (open: boolean) => void;
  /** URL of the cookie or privacy policy, linked under the description. */
  policyHref?: string;
  /** Your own link element (e.g. a router link), used instead of `policyHref`. */
  policyLink?: ReactNode;
  /** Show "Withdraw consent" when a saved choice exists. Default `true`. */
  allowWithdraw?: boolean;
  /** Locale for the time of the saved choice. Default: the runtime locale. */
  locale?: string;
  /** Override visible strings. */
  messages?: Partial<ConsentPreferencesMessages>;
}

function CategoryRow({
  category,
  checked,
  locked,
  gpcLocked,
  messages,
  onCheckedChange,
}: {
  category: ConsentCategory;
  checked: boolean;
  locked: boolean;
  gpcLocked: boolean;
  messages: ConsentPreferencesMessages;
  onCheckedChange: (next: boolean) => void;
}) {
  const id = useId();
  const switchId = `${id}-switch`;
  const descriptionId = `${id}-description`;
  const noteId = `${id}-note`;
  const required = category.required === true;
  return (
    <li
      className="mn-consent-preferences__category"
      data-category={category.id}
      data-required={required || undefined}
      data-locked={locked || undefined}
    >
      <div className="mn-consent-preferences__category-text">
        <label htmlFor={switchId} className="mn-consent-preferences__category-label">
          {category.label}
        </label>
        <p id={descriptionId} className="mn-consent-preferences__category-description">
          {category.description}
        </p>
        {gpcLocked ? (
          <p id={noteId} className="mn-consent-preferences__note">
            {messages.gpcLocked}
          </p>
        ) : null}
      </div>
      <div className="mn-consent-preferences__control">
        {required ? (
          <span id={noteId} className="mn-consent-preferences__always-on">
            {messages.alwaysOn}
          </span>
        ) : null}
        <Switch
          id={switchId}
          checked={checked}
          disabled={locked}
          aria-describedby={cx(descriptionId, locked ? noteId : undefined)}
          onCheckedChange={onCheckedChange}
        />
      </div>
    </li>
  );
}

/**
 * The cookie settings dialog. One row per consent category with a switch;
 * strictly necessary storage is shown as "Always on" and cannot be switched
 * off, and sale-or-sharing categories stay off while a Global Privacy
 * Control signal is on. "Reject all" and "Accept all" look the same, and
 * nothing optional is switched on until the person chooses. Opens from the
 * banner's "Customize" or a `ConsentSettingsLink`, through `ConsentProvider`.
 */
export const ConsentPreferences = forwardRef<
  ElementRef<typeof DialogContent>,
  ConsentPreferencesProps
>(function ConsentPreferences(
  {
    open: openProp,
    onOpenChange,
    policyHref,
    policyLink,
    allowWithdraw = true,
    locale,
    messages: overrides,
    size = 'md',
    onOpenAutoFocus,
    onCloseAutoFocus,
    className,
    ...rest
  },
  ref,
) {
  const consent = useConsent();
  const messages = { ...DEFAULT_MESSAGES, ...overrides };
  const open = openProp ?? consent.preferencesOpen;
  const [draft, setDraft] = useState<Record<string, boolean>>(consent.choices);
  // The dialog usually opens without a DialogTrigger (from the banner or a
  // footer button), so remember what had focus and return there on close.
  const returnFocus = useRef<HTMLElement | null>(null);

  // Every opening starts from the current choices.
  useEffect(() => {
    if (open) setDraft(consent.choices);
    // Only on opening: later changes come from the person's own edits.
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

  const setOpen = (next: boolean) => {
    if (openProp === undefined) consent.setPreferencesOpen(next);
    onOpenChange?.(next);
  };
  const decide = (action: () => void) => {
    action();
    setOpen(false);
  };

  const policy =
    policyLink ??
    (policyHref ? (
      <Link href={policyHref} className="mn-consent-preferences__policy">
        {messages.policyLabel}
      </Link>
    ) : null);
  const record = consent.record;

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent
        ref={ref}
        {...rest}
        size={size}
        onOpenAutoFocus={(event) => {
          returnFocus.current = focusReturnTarget();
          onOpenAutoFocus?.(event);
        }}
        onCloseAutoFocus={(event) => {
          onCloseAutoFocus?.(event);
          if (event.defaultPrevented) return;
          const target = returnFocus.current;
          returnFocus.current = null;
          if (target?.isConnected) {
            event.preventDefault();
            target.focus();
          }
        }}
        data-regime={consent.regime}
        data-gpc={consent.gpc || undefined}
        className={cx('mn-consent-preferences', className)}
      >
        <DialogHeader>
          <DialogTitle>{messages.title}</DialogTitle>
          <DialogDescription>{messages.description}</DialogDescription>
          {policy ? <p className="mn-consent-preferences__policy-line">{policy}</p> : null}
        </DialogHeader>
        <DialogBody className="mn-consent-preferences__body">
          <ul className="mn-consent-preferences__categories">
            {consent.categories.map((category) => {
              const gpcLocked = consent.gpc && category.saleOrSharing === true;
              const locked = category.required === true || gpcLocked;
              const checked =
                category.required === true ? true : gpcLocked ? false : draft[category.id] === true;
              return (
                <CategoryRow
                  key={category.id}
                  category={category}
                  checked={checked}
                  locked={locked}
                  gpcLocked={gpcLocked && !category.required}
                  messages={messages}
                  onCheckedChange={(next) =>
                    setDraft((current) => ({ ...current, [category.id]: next }))
                  }
                />
              );
            })}
          </ul>
          {record ? (
            <p className="mn-consent-preferences__record">
              <span>
                {messages.policyVersion}: {record.policyVersion}
              </span>
              <span aria-hidden> · </span>
              <span>
                {messages.lastUpdated}{' '}
                <RelativeTime
                  date={record.at}
                  tooltip={false}
                  absoluteAfter={7 * 86_400_000}
                  absoluteFormat={{ dateStyle: 'medium' }}
                  {...(locale ? { locale } : {})}
                />
              </span>
            </p>
          ) : null}
        </DialogBody>
        <DialogFooter className="mn-consent-preferences__footer">
          {allowWithdraw && record && record.method !== 'withdrawn' ? (
            <button
              type="button"
              className="mn-consent-preferences__withdraw"
              onClick={() => decide(consent.withdraw)}
            >
              {messages.withdraw}
            </button>
          ) : null}
          <div className="mn-consent-preferences__actions">
            <Button
              className="mn-consent-preferences__choice"
              onClick={() => decide(consent.rejectAll)}
            >
              {messages.rejectAll}
            </Button>
            <Button
              className="mn-consent-preferences__choice"
              onClick={() => decide(consent.acceptAll)}
            >
              {messages.acceptAll}
            </Button>
            <Button variant="primary" onClick={() => decide(() => consent.save(draft))}>
              {messages.save}
            </Button>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
});

export interface ConsentSettingsLinkProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** The label. Default "Cookie settings". */
  children?: ReactNode;
}

/**
 * A small link-style button, usually in the footer, that opens the cookie
 * settings dialog (`ConsentPreferences`) so people can change or withdraw
 * consent at any time. It is a real button, not a link, and needs no `href`.
 */
export const ConsentSettingsLink = forwardRef<HTMLButtonElement, ConsentSettingsLinkProps>(
  function ConsentSettingsLink({ children = 'Cookie settings', className, onClick, ...rest }, ref) {
    const consent = useConsent();
    return (
      <button
        ref={ref}
        type="button"
        aria-haspopup="dialog"
        {...rest}
        className={cx('mn-consent-settings-link', className)}
        onClick={(event) => {
          onClick?.(event);
          if (!event.defaultPrevented) consent.setPreferencesOpen(true);
        }}
      >
        {children}
      </button>
    );
  },
);
