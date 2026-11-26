import { useState, type ChangeEvent } from 'react';
import {
  Field,
  FieldDescription,
  FieldLabel,
  PasswordInput,
  PasswordStrengthMeter,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function NewPasswordDemo() {
  const [password, setPassword] = useState('');
  return (
    <Field required style={{ width: '100%', maxWidth: 320 }}>
      <FieldLabel>New password</FieldLabel>
      <PasswordInput
        purpose="new"
        value={password}
        onChange={(e: ChangeEvent<HTMLInputElement>) => setPassword(e.target.value)}
        aria-describedby="new-password-strength"
      />
      <PasswordStrengthMeter id="new-password-strength" password={password} />
    </Field>
  );
}

export default {
  id: 'password-input',
  title: 'PasswordInput',
  summary: 'Password field with a reveal toggle, the right autocomplete and a Caps Lock hint.',
  sections: {
    purpose: {
      summary:
        'A password field people can check before submitting: a reveal toggle, `autoComplete` set for password managers from `purpose`, and a Caps Lock warning. Needed by: rookid (sign-in, delete and deactivate account confirmation), rookcalendar, rookdossier, rookdots and rookpulse frontends, which use a bare `type="password"` today. There is no shadcn/ui password copy in the apps.',
      useWhen: [
        'Signing in, confirming a destructive account action with the current password: `purpose="current"`.',
        'Creating or resetting a password: `purpose="new"`, with PasswordStrengthMeter under it.',
        'Any secret the user types and may want to check (API secrets they paste, Wi-Fi keys).',
      ],
      avoidWhen: [
        'One-time codes: use OtpPinInput.',
        'Showing a stored secret or token: use CopyField, which masks and copies.',
        'Typing a word to confirm a deletion: use TypeToConfirmInput.',
      ],
    },
    designPrinciples: {
      items: [
        'Let people see what they typed: masking with no way to check causes lockouts and resets.',
        'Help password managers: `current-password` and `new-password` are set for you.',
        'The toggle is a real toggle button: one name ("Show password") and `aria-pressed`, so the state is announced, not guessed from a changing label.',
        'One focus ring around the input; the toggle keeps its own ring when it has focus.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `.mn-password-input` around a bordered control (input and toggle) and a hint line. The ref and native input props go to the `<input>`; `className` and `style` go to the wrapper.',
        '`PASSWORD_INPUT_MESSAGES` holds the English defaults for `messages`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`purpose` only sets `autoComplete`; an explicit `autoComplete` wins.',
        'The input always has `autoCapitalize="off"`, `autoCorrect="off"` and `spellCheck={false}`, so revealed text is never sent to a spelling service or changed by the keyboard.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'purpose',
          values: ['current', 'new'],
          description: '`current-password` or `new-password` autocomplete.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Height from `--mn-control-*`, matching Input.',
        },
        {
          prop: 'capsLockWarning',
          values: ['true', 'false'],
          description:
            'Shows "Caps Lock is on." under the field while it is focused and Caps Lock is on.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Revealed',
          attribute: 'data-visible on the wrapper, aria-pressed="true" on the toggle',
          description: 'The input is `type="text"`; the icon changes to a crossed-out eye.',
        },
        {
          name: 'Caps Lock on',
          attribute: 'hint text joined to aria-describedby',
          description: 'A warning-coloured line under the field, announced politely.',
        },
        {
          name: 'Invalid',
          attribute: 'data-invalid, aria-invalid="true"',
          description: 'Danger border, from `invalid` or the surrounding Field.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled, disabled on input and toggle',
          description: 'Muted surface; the toggle cannot be pressed.',
        },
      ],
    },
    interaction: {
      items: [
        'Pressing the toggle switches between hidden and shown; the typed value and focus stay where they are.',
        'Caps Lock is read from key events while typing; the hint disappears on blur.',
        'Edge’s built-in reveal button is hidden so there is only one.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves from the input to the toggle, then on.' },
        { keys: 'Enter / Space (on the toggle)', action: 'Shows or hides the password.' },
      ],
    },
    accessibility: {
      role: 'textbox (password) and button (toggle)',
      name: 'FieldLabel inside a Field, or `aria-label`. The toggle is named by `messages.show`.',
      items: [
        'The toggle has `aria-pressed` and `aria-controls` pointing at the input.',
        'The toggle name stays "Show password" in both states, following the toggle-button pattern.',
        'The Caps Lock hint is an `aria-live="polite"` line and is added to `aria-describedby` while shown.',
        'Inside a Field it gets id, description, required and invalid wiring through `useFieldControl`.',
      ],
    },
    responsive: {
      items: [
        'Fills its container at every width, including 390px.',
        'On coarse pointers the control is at least 44px tall and the toggle at least 44px wide.',
      ],
    },
    theme: {
      items: [
        'Uses the Input tokens (`--mn-color-border-input`, `--mn-color-bg-surface`, `--mn-color-focus-ring`); the hint uses `--mn-color-warning-strong`.',
        'Forced colours: system field colours, a Highlight outline on focus and a 2px border when invalid.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Sign in',
          stack: true,
          render: () => (
            <Field style={{ width: '100%', maxWidth: 320 }}>
              <FieldLabel>Password</FieldLabel>
              <PasswordInput />
              <FieldDescription>The password for your account.</FieldDescription>
            </Field>
          ),
        },
        { title: 'New password with strength', stack: true, render: () => <NewPasswordDemo /> },
        {
          title: 'Revealed by default, small',
          render: () => (
            <PasswordInput
              aria-label="Access key"
              size="sm"
              defaultVisible
              defaultValue="k3y-sample"
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Set `purpose="new"` on sign-up and reset forms.',
        'Pair a new password with PasswordStrengthMeter and say the rules before submit.',
        'Ask for the current password before a destructive account action.',
      ],
      dont: [
        'Do not block paste: password managers need it.',
        'Do not add a second "confirm password" field when the value can be revealed.',
        'Do not change the toggle label between "Show" and "Hide"; `aria-pressed` carries the state.',
      ],
    },
    related: {
      items: [
        { id: 'password-strength-meter', reason: 'Strength bar and rules for a new password.' },
        { id: 'otp-pin-input', reason: 'One-time codes.' },
        { id: 'type-to-confirm-input', reason: 'Typed confirmation for destructive actions.' },
        { id: 'copy-field', reason: 'Show and copy a stored secret.' },
        { id: 'form-field', reason: 'Label, description and error wiring.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['form-field'] },
    testing: {
      unit: [
        'Labelled by the Field, described by FieldDescription, `type="password"` and `current-password` by default.',
        'Toggle by Enter, Space and click: type and aria-pressed change, value and focus are kept, the name stays the same.',
        '`purpose="new"` sets `new-password`; an explicit `autoComplete` wins.',
        'Controlled `visible`.',
        'Caps Lock hint appears, is linked by aria-describedby, and clears on blur.',
        'Disabled and invalid from the Field; translated toggle name.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
