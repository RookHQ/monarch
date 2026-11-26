import { useState } from 'react';
import {
  Field,
  FieldDescription,
  FieldLabel,
  UsernameAvailabilityInput,
  type AsyncValidationStatus,
  type AsyncValidator,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

// Demo only: a product calls its own API here.
const TAKEN = new Set(['admin', 'help', 'support', 'team']);
const demoCheck: AsyncValidator = (value, signal) => {
  if (value.length < 3) return { valid: false, message: 'Use at least 3 characters.' };
  if (!/^[a-z0-9-]+$/.test(value))
    return { valid: false, message: 'Use lowercase letters, numbers and hyphens.' };
  return new Promise((resolve, reject) => {
    const t = setTimeout(
      () =>
        resolve(
          TAKEN.has(value) ? { valid: false, message: 'That address is taken.' } : { valid: true },
        ),
      600,
    );
    signal.addEventListener('abort', () => {
      clearTimeout(t);
      reject(new DOMException('Aborted', 'AbortError'));
    });
  });
};
const slugify = (raw: string) =>
  raw
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^a-z0-9-]/g, '');

function AddressDemo() {
  const [status, setStatus] = useState<AsyncValidationStatus>('idle');
  return (
    <div style={{ width: '100%', maxWidth: 300 }}>
      <Field required>
        <FieldLabel>Workspace address</FieldLabel>
        <UsernameAvailabilityInput
          prefix="example.com/"
          placeholder="your-team"
          validate={demoCheck}
          transform={slugify}
          onStatusChange={(s) => setStatus(s)}
        />
        <FieldDescription>Try “team” to see a taken address.</FieldDescription>
      </Field>
      <p style={{ margin: '8px 0 0', fontSize: 13 }}>
        Submit enabled: {status === 'valid' ? 'yes' : 'no'}
      </p>
    </div>
  );
}

export default {
  id: 'username-availability-input',
  title: 'UsernameAvailabilityInput',
  summary: 'Input with an injected, debounced async check, a status icon and an announced result.',
  sections: {
    purpose: {
      summary:
        'A text input whose value is checked while the user types, such as a handle, workspace address or slug. The product supplies the check; the component debounces it, drops stale answers, shows the status and announces the result.',
      useWhen: [
        'A value must be unique or otherwise approved by a server before the form can be sent.',
        'The user benefits from knowing the answer before submitting (handles, addresses, slugs).',
      ],
      avoidWhen: [
        'Validation is purely local: use Input with FieldError.',
        'The check is expensive or rate-limited per keystroke: validate on submit instead.',
      ],
    },
    designPrinciples: {
      items: [
        'Generic by design: the product passes `validate`, so restricted names and lookups stay in the product.',
        'Status is never colour alone: every state has an icon and a text message.',
        'Only settled results are announced; "checking" is shown, not spoken on every pause.',
        'The latest value always wins: earlier checks are aborted and their late answers ignored.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`useAsyncValidation(value, validate, { debounce })` is the same logic as a hook, returning `{ status, message, revalidate }`, for products that need it on another control.',
        'The ref and native input props go to the `<input>`; `className` goes to the `.mn-username-availability-input` wrapper.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`validate(value, signal)` may return a result synchronously or as a promise. Return `null` for "nothing to check" (for example the user’s current handle); throw or reject for "could not check".',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Control height from `--mn-control-*`, as for Input.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Idle',
          attribute: 'data-status="idle"',
          description: 'Empty, or the validator returned null. No icon or message.',
        },
        {
          name: 'Checking',
          attribute: 'data-status="checking"',
          description:
            'Spinner and "Checking availability…" from the first keystroke until the answer arrives.',
        },
        {
          name: 'Valid',
          attribute: 'data-status="valid"',
          description: 'Check icon, success border and message (default "Available").',
        },
        {
          name: 'Invalid',
          attribute: 'data-status="invalid", aria-invalid="true"',
          description:
            'Warning icon, danger border and the validator’s message (default "Not available").',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description:
            'The check failed: an info icon, a message and a "Try again" button that re-runs it at once.',
        },
      ],
    },
    interaction: {
      items: [
        'Each change applies `transform`, calls `onValueChange`, then runs `validate` after `debounce` ms without typing (default 400).',
        'A new value aborts the previous check through its AbortSignal; a result that arrives late is ignored.',
        '`onStatusChange(status, message)` fires on every status change, so the form can disable submit until `valid`.',
        'Controlled with `value` + `onValueChange`, or uncontrolled with `defaultValue`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the input, then to "Try again" when a check has failed.' },
      ],
      notes: ['Typing and editing are the native input’s own; the component adds no key handling.'],
    },
    accessibility: {
      role: 'textbox',
      name: 'A FieldLabel, or `aria-label`.',
      items: [
        'The prefix and the status message are linked with `aria-describedby`, so the full address and the result are read with the field.',
        'Settled results are announced through a polite `role="status"` region that is always present.',
        '`aria-invalid` is set while the value is unavailable or the Field is invalid.',
        'Icons are `aria-hidden`; the text carries the meaning.',
        'Autocorrect, autocapitalise and spellcheck are off, since handles are not words.',
      ],
    },
    responsive: {
      items: [
        'Fills its container. A long prefix is truncated with an ellipsis at half the width, leaving room to type.',
        'The message wraps under the field at narrow widths.',
      ],
    },
    theme: {
      items: [
        'Status colours are semantic tokens (`--mn-color-success-*`, `--mn-color-danger-*`, `--mn-color-warning-strong`), so light, dark and more-contrast themes apply.',
        'The spinner slows under `prefers-reduced-motion` (Spinner’s own rule).',
      ],
    },
    examples: {
      items: [
        {
          title: 'Workspace address',
          stack: true,
          render: () => <AddressDemo />,
        },
        {
          title: 'Settled states',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 300 }}>
              <UsernameAvailabilityInput
                aria-label="Available handle"
                defaultValue="maya-lee"
                validate={() => ({ valid: true })}
                debounce={0}
              />
              <UsernameAvailabilityInput
                aria-label="Taken handle"
                defaultValue="admin"
                validate={() => ({ valid: false, message: 'That handle is taken.' })}
                debounce={0}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass the AbortSignal to `fetch` so abandoned checks stop.',
        'Return `null` for the user’s current value, so editing a profile does not report their own handle as taken.',
        'Keep submit disabled until the status is `valid`, and re-check on the server when the form is sent.',
      ],
      dont: [
        'Do not put restricted-name lists or uniqueness rules in Monarch; they belong in `validate`.',
        'Do not set `debounce` to 0 for a server check; it sends a request per keystroke.',
      ],
    },
    related: {
      items: [
        { id: 'text-field', reason: 'Plain Input when there is no asynchronous check.' },
        { id: 'form-field', reason: 'Label, help and error wiring around it.' },
        { id: 'spinner', reason: 'The checking indicator.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['form-field', 'spinner'],
      notes: ['Uses the `.mn-input` styles from text-field for the control.'],
    },
    testing: {
      unit: [
        'Debounces the check, shows "checking" without announcing it, then announces and describes the result.',
        'Marks unavailable values invalid with the default or the validator’s message.',
        'Aborts stale checks; a late answer never overwrites the latest one.',
        'A failed check shows an error and "Try again" re-runs it.',
        'Empty values and `null` results are idle; `onStatusChange` reports each change.',
        '`transform` rewrites the value in controlled mode.',
        'The prefix and Field description are part of the accessible description.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery',
        'Cross-browser axe: /components/username-availability-input',
      ],
    },
  },
} satisfies ComponentDocs;
