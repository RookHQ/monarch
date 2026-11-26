'use client';

import {
  forwardRef,
  Fragment,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Checkbox } from '../checkbox/index.js';
import { Field, FieldError } from '../form-field/index.js';
import { Link } from '../../navigation/link/index.js';

/** A document the person agrees to, linked from the label. */
export interface TermsDocument {
  /** Link text, e.g. "Terms of Service". */
  label: string;
  /** Where the document is published. */
  href: string;
  /** The version shown, e.g. `2026-11-01`. Reported in `onAcceptedChange`. */
  version?: string;
  /** Stable key for `versions`. Default: `href`. */
  id?: string;
}

/** What was accepted, for proof of acceptance. */
export interface TermsAcceptanceDetails {
  /** Version of each document that has one, keyed by `id` (or `href`). */
  versions: Record<string, string>;
  /** When the box changed, as an ISO 8601 string. */
  at: string;
}

export interface TermsAcceptanceProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultChecked'
> {
  /** The documents, rendered as links inside the label in the order given. */
  documents: readonly TermsDocument[];
  /**
   * Builds the label around the formatted list of links. Default:
   * `(links) => <>I agree to the {links}</>`.
   */
  label?: (links: ReactNode) => ReactNode;
  /** Locale for joining the links ("A, B and C"). Default: the runtime locale. */
  locale?: string;
  /** Where the links open. Default: the same tab. `_blank` adds an "opens in a new tab" note. */
  target?: string;
  /** The box must be ticked to submit. Sets the native `required` attribute. Default `true`. */
  required?: boolean;
  /** Form field name. The box posts `name=value` when ticked (inside a `<form>`). */
  name?: string;
  /** Value posted when ticked. Default `accepted`. */
  value?: string;
  /**
   * When set, a hidden input with this name posts the accepted `versions` as
   * JSON while the box is ticked.
   */
  versionsName?: string;
  /** Controlled state. */
  checked?: boolean;
  /** Uncontrolled initial state. Leave unticked for sign-up. */
  defaultChecked?: boolean;
  disabled?: boolean;
  /**
   * Shows the error state. Without it, the error appears after a submit attempt
   * with the box unticked and clears once it is ticked.
   */
  invalid?: boolean;
  /** Error text. Default "Accept the terms to continue". */
  requiredMessage?: ReactNode;
  /** Called when the box is ticked or cleared, with the versions and the time. */
  onAcceptedChange?: (accepted: boolean, details: TermsAcceptanceDetails) => void;
  /** Id of the checkbox. */
  id?: string;
}

/** The versions of the documents that have one, keyed by `id` or `href`. */
export function termsVersions(documents: readonly TermsDocument[]): Record<string, string> {
  const versions: Record<string, string> = {};
  for (const doc of documents) {
    if (doc.version !== undefined) versions[doc.id ?? doc.href] = doc.version;
  }
  return versions;
}

/** Joins items with the locale's list format ("A, B and C"), keeping React nodes. */
function joinList(items: ReactNode[], locale?: string): ReactNode {
  if (items.length <= 1) return items[0] ?? null;
  const keys = items.map((_, i) => `[[${i}]]`);
  let parts: { type: string; value: string }[];
  try {
    parts = new Intl.ListFormat(locale, { type: 'conjunction' }).formatToParts(keys);
  } catch {
    parts = keys.flatMap((key, i) => [
      ...(i === 0 ? [] : [{ type: 'literal', value: i === keys.length - 1 ? ' and ' : ', ' }]),
      { type: 'element', value: key },
    ]);
  }
  return parts.map((part, i) => (
    <Fragment key={i}>
      {part.type === 'element' ? items[keys.indexOf(part.value)] : part.value}
    </Fragment>
  ));
}

const defaultLabel = (links: ReactNode) => <>I agree to the {links}</>;

/**
 * The "I agree to the Terms of Service and Privacy Policy" checkbox for
 * sign-up. The documents are links inside the label. It is required by
 * default: it works with a plain `<form>` (native `required`), shows an error
 * after a submit attempt while unticked, and reports the accepted document
 * versions and time for proof of acceptance.
 */
export const TermsAcceptance = forwardRef<HTMLDivElement, TermsAcceptanceProps>(
  function TermsAcceptance(
    {
      documents,
      label = defaultLabel,
      locale,
      target,
      required = true,
      name,
      value = 'accepted',
      versionsName,
      checked: checkedProp,
      defaultChecked = false,
      disabled,
      invalid: invalidProp,
      requiredMessage = 'Accept the terms to continue',
      onAcceptedChange,
      id,
      className,
      ...rest
    },
    ref,
  ) {
    const [checkedState, setCheckedState] = useState(defaultChecked);
    const checked = checkedProp ?? checkedState;
    const [attempted, setAttempted] = useState(false);
    const rootRef = useRef<HTMLDivElement>(null);
    const boxRef = useRef<HTMLButtonElement>(null);
    useImperativeHandle(ref, () => rootRef.current as HTMLDivElement);

    // A native submit with the box unticked fires `invalid` on Radix's hidden
    // input. Show our error instead of the browser bubble and focus the box.
    useEffect(() => {
      const root = rootRef.current;
      if (!root) return;
      const onInvalid = (event: Event) => {
        event.preventDefault();
        setAttempted(true);
        boxRef.current?.focus();
      };
      // Radix resets its own state on form reset; do the same for ours.
      const form = root.closest('form');
      const onReset = () => {
        setAttempted(false);
        if (checkedProp === undefined) setCheckedState(defaultChecked);
      };
      root.addEventListener('invalid', onInvalid, true);
      form?.addEventListener('reset', onReset);
      return () => {
        root.removeEventListener('invalid', onInvalid, true);
        form?.removeEventListener('reset', onReset);
      };
    }, [checkedProp, defaultChecked]);

    const invalid = invalidProp ?? (attempted && required && !checked);
    const versions = termsVersions(documents);
    const links = documents.map((doc) => (
      <Link key={doc.id ?? doc.href} href={doc.href} target={target}>
        {doc.label}
      </Link>
    ));

    return (
      <div
        ref={rootRef}
        {...rest}
        className={cx('mn-terms-acceptance', className)}
        data-invalid={invalid || undefined}
        data-state={checked ? 'checked' : 'unchecked'}
      >
        <Field
          {...(id !== undefined ? { id } : {})}
          required={required}
          invalid={invalid}
          disabled={disabled ?? false}
        >
          <Checkbox
            ref={boxRef}
            label={label(joinList(links, locale))}
            checked={checked}
            name={name}
            value={value}
            onCheckedChange={(next) => {
              const accepted = next === true;
              if (checkedProp === undefined) setCheckedState(accepted);
              if (accepted) setAttempted(false);
              onAcceptedChange?.(accepted, { versions, at: new Date().toISOString() });
            }}
          />
          <FieldError>{invalid ? requiredMessage : null}</FieldError>
        </Field>
        {versionsName && checked ? (
          <input type="hidden" name={versionsName} value={JSON.stringify(versions)} />
        ) : null}
      </div>
    );
  },
);
