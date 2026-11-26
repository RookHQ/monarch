'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useImperativeHandle,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Checkbox } from '../checkbox/index.js';
import { Field, FieldDescription } from '../form-field/index.js';
import { Link } from '../../navigation/link/index.js';

/** A way of sending marketing messages. */
export type MarketingChannel = 'email' | 'sms' | 'phone' | 'post';

/** The person's choice, with the wording they saw, for proof of consent. */
export interface MarketingConsent {
  /** One entry per channel shown: `true` when ticked. */
  channels: Partial<Record<MarketingChannel, boolean>>;
  /** The exact wording shown (label, channels, description and notes), whitespace collapsed. */
  text: string;
  /** When the choice changed, as an ISO 8601 string. */
  at: string;
}

export interface MarketingConsentCheckboxProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultChecked'
> {
  /** Channels to ask about. One checkbox each when more than one. Default `['email']`. */
  channels?: readonly MarketingChannel[];
  /**
   * The question. With one channel it labels the checkbox (default "Send me
   * product news by email"); with several it is the group legend (default
   * "Send me product news").
   */
  label?: ReactNode;
  /** Labels of the channel checkboxes when there are several. */
  channelLabels?: Partial<Record<MarketingChannel, ReactNode>>;
  /** What is sent and how often, e.g. "Product updates and offers, about twice a month." */
  description?: ReactNode;
  /** Default "You can unsubscribe at any time." Pass `null` to hide it. */
  unsubscribeNote?: ReactNode;
  /** A link to the privacy policy, shown after the notes. */
  policyLink?: { label: ReactNode; href: string; target?: string };
  /**
   * Form field name. Hidden inputs post one `name=<channel>` per ticked
   * channel, so a plain `<form>` submits the choice.
   */
  name?: string;
  /** Name of a hidden input that posts the exact wording shown. Default `<name>_text` when `name` is set. */
  textName?: string;
  /**
   * Controlled state, for showing a choice the person made earlier (a
   * settings page). Without it the boxes always start unticked.
   */
  checked?: Partial<Record<MarketingChannel, boolean>>;
  /**
   * Ignored. Marketing consent must be an active choice, so the boxes always
   * start unticked; passing `true` logs a warning once in development.
   */
  defaultChecked?: boolean;
  disabled?: boolean;
  /** Called on every change with the channels, the wording shown and the time. */
  onChange?: (consent: MarketingConsent) => void;
}

const CHANNEL_WORDS: Record<MarketingChannel, string> = {
  email: 'email',
  sms: 'text message',
  phone: 'phone',
  post: 'post',
};

const CHANNEL_LABELS: Record<MarketingChannel, string> = {
  email: 'Email',
  sms: 'Text message (SMS)',
  phone: 'Phone',
  post: 'Post',
};

let warnedDefaultChecked = false;

function isDev() {
  const env = (globalThis as { process?: { env?: Record<string, string | undefined> } }).process
    ?.env;
  return env?.NODE_ENV !== 'production';
}

function unticked(channels: readonly MarketingChannel[]) {
  return Object.fromEntries(channels.map((c) => [c, false])) as Partial<
    Record<MarketingChannel, boolean>
  >;
}

/** The visible wording inside `root`, in reading order, whitespace collapsed. */
function shownText(root: HTMLElement | null): string {
  if (!root) return '';
  return Array.from(root.querySelectorAll('[data-consent-text]'))
    .map((el) => {
      // Screen-reader-only notes (such as "opens in a new tab") are not shown.
      const copy = el.cloneNode(true) as HTMLElement;
      copy.querySelectorAll('.mn-visually-hidden').forEach((hidden) => hidden.remove());
      return (copy.textContent ?? '').replace(/\s+/g, ' ').trim();
    })
    .filter(Boolean)
    .join(' ');
}

/**
 * Opt-in to marketing messages for sign-up and contact forms ("Send me
 * product news by email"). It is never pre-ticked: it ignores
 * `defaultChecked` (and warns in development). With several channels it shows
 * one separately unticked checkbox per channel. Every change reports the exact
 * wording shown, so the product can keep proof of consent, and `name` posts
 * the choice with a plain `<form>`.
 */
export const MarketingConsentCheckbox = forwardRef<HTMLDivElement, MarketingConsentCheckboxProps>(
  function MarketingConsentCheckbox(
    {
      channels = ['email'],
      label,
      channelLabels,
      description,
      unsubscribeNote = 'You can unsubscribe at any time.',
      policyLink,
      name,
      textName,
      checked: checkedProp,
      defaultChecked,
      disabled,
      onChange,
      className,
      ...domProps
    },
    ref,
  ) {
    const [checkedState, setCheckedState] = useState(() => unticked(channels));
    const checked = checkedProp ?? checkedState;
    const rootRef = useRef<HTMLDivElement>(null);
    const [text, setText] = useState('');
    const auto = useId();
    const notesId = `mn-marketing-consent-${auto}-notes`;
    useImperativeHandle(ref, () => rootRef.current as HTMLDivElement);

    useEffect(() => {
      if (defaultChecked && !warnedDefaultChecked && isDev()) {
        warnedDefaultChecked = true;
        console.warn(
          'MarketingConsentCheckbox: `defaultChecked` is ignored. Marketing consent must be an active choice, so the boxes always start unticked.',
        );
      }
    }, [defaultChecked]);

    // Keep the posted wording in step with what is on screen. The props are
    // React nodes, so this runs after most renders; it only sets state when the
    // text really changed.
    const channelKey = channels.join(',');
    useEffect(() => {
      if (!name) return;
      const next = shownText(rootRef.current);
      if (next !== text) setText(next);
    }, [name, text, label, channelLabels, description, unsubscribeNote, policyLink, channelKey]);

    // A form reset unticks everything.
    useEffect(() => {
      const form = rootRef.current?.closest('form');
      if (!form) return;
      const onReset = () => setCheckedState(unticked(channelKey.split(',') as MarketingChannel[]));
      form.addEventListener('reset', onReset);
      return () => form.removeEventListener('reset', onReset);
    }, [channelKey]);

    const multiple = channels.length > 1;
    const single = channels[0] ?? 'email';
    const notes =
      description || unsubscribeNote || policyLink ? (
        <>
          {description ? <span data-consent-text>{description}</span> : null}
          {description && unsubscribeNote ? ' ' : null}
          {unsubscribeNote ? <span data-consent-text>{unsubscribeNote}</span> : null}
          {policyLink ? (
            <>
              {' '}
              <Link href={policyLink.href} target={policyLink.target} data-consent-text>
                {policyLink.label}
              </Link>
            </>
          ) : null}
        </>
      ) : null;

    const toggle = (channel: MarketingChannel, value: boolean) => {
      const next = { ...unticked(channels), ...checked, [channel]: value };
      if (checkedProp === undefined) setCheckedState(next);
      onChange?.({
        channels: next,
        text: shownText(rootRef.current),
        at: new Date().toISOString(),
      });
    };

    const posted = name ? (
      <>
        {channels
          .filter((c) => checked[c])
          .map((c) => (
            <input key={c} type="hidden" name={name} value={c} />
          ))}
        <input type="hidden" name={textName ?? `${name}_text`} value={text} />
      </>
    ) : null;

    return (
      <div
        ref={rootRef}
        {...domProps}
        className={cx('mn-marketing-consent-checkbox', className)}
        data-channels={channels.length}
        data-disabled={disabled || undefined}
      >
        {multiple ? (
          <fieldset
            className="mn-marketing-consent-checkbox__group"
            aria-describedby={notes ? notesId : undefined}
            disabled={disabled}
          >
            <legend className="mn-marketing-consent-checkbox__legend" data-consent-text>
              {label ?? 'Send me product news'}
            </legend>
            {notes ? (
              <p id={notesId} className="mn-marketing-consent-checkbox__notes">
                {notes}
              </p>
            ) : null}
            <div className="mn-marketing-consent-checkbox__channels">
              {channels.map((channel) => (
                <Checkbox
                  key={channel}
                  label={
                    <span data-consent-text>
                      {channelLabels?.[channel] ?? CHANNEL_LABELS[channel]}
                    </span>
                  }
                  checked={checked[channel] === true}
                  disabled={disabled}
                  data-channel={channel}
                  onCheckedChange={(next) => toggle(channel, next === true)}
                />
              ))}
            </div>
          </fieldset>
        ) : (
          <Field disabled={disabled ?? false}>
            <Checkbox
              label={
                <span data-consent-text>
                  {label ?? `Send me product news by ${CHANNEL_WORDS[single]}`}
                </span>
              }
              checked={checked[single] === true}
              data-channel={single}
              onCheckedChange={(next) => toggle(single, next === true)}
            />
            {notes ? (
              <FieldDescription className="mn-marketing-consent-checkbox__notes">
                {notes}
              </FieldDescription>
            ) : null}
          </Field>
        )}
        {posted}
      </div>
    );
  },
);
