import { useState } from 'react';
import { Button, StepUpAuthDialog, useStepUpAuth } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

// Demo check: the password "correct" and the code "123456" are accepted.
async function demoVerify(
  credential:
    | { method: 'password'; password: string }
    | { method: 'totp'; code: string }
    | { method: 'passkey' },
) {
  await wait(800);
  if (credential.method === 'password' && credential.password !== 'correct')
    throw new Error('That password is not right. Try again.');
  if (credential.method === 'totp' && credential.code !== '123456')
    throw new Error('That code is not valid. Enter the current code from your app.');
}

function PasswordOnly() {
  return (
    <StepUpAuthDialog
      trigger={<Button variant="danger">Delete workspace</Button>}
      description="Deleting a workspace needs your password. Try “correct”."
      forgotHref="#recover"
      onVerify={demoVerify}
    />
  );
}

function SeveralMethods() {
  return (
    <StepUpAuthDialog
      trigger={<Button>Show signing secret</Button>}
      methods={['passkey', 'totp', 'password']}
      description="Viewing a secret needs a fresh check. The demo code is 123456."
      onVerify={demoVerify}
    />
  );
}

function CodeOnly() {
  return (
    <StepUpAuthDialog
      trigger={<Button>Revoke all keys</Button>}
      methods={['totp']}
      title="Enter your authentication code"
      description="Revoking keys needs a code from your authenticator app. Try 123456."
      onVerify={demoVerify}
    />
  );
}

function Imperative() {
  const { requireStepUp, element } = useStepUpAuth();
  const [result, setResult] = useState<string | null>(null);
  return (
    <>
      <Button
        variant="danger"
        onClick={async () => {
          const ok = await requireStepUp({
            methods: ['password', 'totp'],
            description: 'Erasing this person’s data needs your password. Try “correct”.',
            onVerify: demoVerify,
          });
          setResult(ok ? 'Verified: the erase would run now.' : 'Cancelled.');
        }}
      >
        Erase personal data
      </Button>
      {element}
      {result ? <p role="status">{result}</p> : null}
    </>
  );
}

export default {
  id: 'step-up-auth-dialog',
  title: 'StepUpAuthDialog',
  summary:
    'Re-authenticate with a password, authenticator code or passkey before a high-risk action.',
  sections: {
    purpose: {
      summary:
        'Asks a signed-in person to prove it is still them before an action that is dangerous in the wrong hands: deleting a workspace or account, erasing personal data, revoking keys, viewing a secret. Verification runs inside the dialog, so a wrong password or code is shown in place and the person can retry without losing their place.',
      useWhen: [
        'Deleting or transferring a workspace or account, or erasing personal data.',
        'Revoking or creating credentials, or revealing a secret.',
        'Any action your security policy says needs a recent sign-in.',
      ],
      avoidWhen: [
        'Signing in from scratch: use the product’s sign-in page.',
        'Confirming intent only: use ConfirmDialog (optionally with a typed phrase).',
        'Every routine action: ask only when the risk justifies the interruption, and remember a recent check on the server.',
      ],
    },
    designPrinciples: {
      items: [
        'Say why: the description names the action that needs the check.',
        'Preferred method first: `methods` is in order of preference, and the person can switch when more than one is allowed.',
        'Fast paths: password managers fill the password (`current-password`), SMS and app codes autofill (`one-time-code`), and a complete code submits itself.',
        'Honest progress: while checking, nothing dismisses the dialog; a failure is announced and the person stays in the field to retry.',
        'A calm lock icon, not a warning: re-authentication is routine, not an error.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`StepUpAuthDialog` is controlled (`open` + `onOpenChange`) or uncontrolled (`trigger`, `defaultOpen`). It forwards its ref to the dialog element (`role="dialog"`).',
        '`onVerify(credential)` receives `{ method: "password", password }`, `{ method: "totp", code }` or `{ method: "passkey" }`. Resolve to accept (the dialog closes, then `onVerified` runs); reject to show the error. For passkeys, run the WebAuthn ceremony inside `onVerify`.',
        '`useStepUpAuth()` returns `{ requireStepUp, element }`: render `element` once and `await requireStepUp(options)`, which resolves `true` when verified and `false` when cancelled. It mirrors `useConfirm()`.',
        'Other DialogContent props (`className`, `onEscapeKeyDown`, `data-*`, …) pass through to the dialog.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`messages` overrides any built-in string (labels, method names, errors, buttons) for translation; `title` and `description` accept rich content.',
        '`forgotHref` renders a link under the password field; `onForgot` alone renders a button.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'methods',
          values: ['password', 'totp', 'passkey'],
          description:
            'Any non-empty list, in order of preference. With more than one, a SegmentedControl switches between them. Exposed as `data-method` on the dialog.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg', 'xl'],
          description: 'Dialog width; default `sm`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Missing input',
          attribute: 'aria-invalid (field)',
          description:
            'Confirming with an empty password or an incomplete code shows a field error; `onVerify` is not called.',
        },
        {
          name: 'Verifying',
          attribute: 'aria-busy (dialog and confirm button), data-busy',
          description:
            'The confirm button shows a spinner and keeps focus; Cancel, the method switcher and the password toggle are disabled; the field is read-only; Escape and outside clicks are ignored.',
        },
        {
          name: 'Failed',
          attribute: 'role="alert", aria-invalid (field)',
          description:
            'A danger Alert above the actions, linked to the field. The code is cleared and focus returns to the field.',
        },
        {
          name: 'Password shown',
          attribute: 'aria-pressed (toggle), data-revealed',
          description: 'The password is shown as plain text until toggled back.',
        },
      ],
    },
    interaction: {
      items: [
        'Opening focuses the active method’s control: the password field, the code field or the passkey button.',
        'Password: Enter or Confirm submits. Authenticator code: the sixth digit (typed, pasted or autofilled) submits.',
        'Passkey: the single “Use passkey” button calls `onVerify({ method: "passkey" })`.',
        'On success the dialog closes and `onVerified` runs; on failure it stays open with the error, the code is cleared and the field is focused.',
        'Switching method clears any error. Every opening starts clean on the preferred method.',
        'Escape and Cancel call `onCancel` and close it, except while verifying. Outside clicks never close it.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action:
            'Moves between the method switcher, the field, the show/hide toggle, Cancel and Confirm; focus stays inside.',
        },
        {
          keys: 'Arrow keys (method switcher)',
          action: 'Choose another verification method.',
        },
        { keys: 'Enter (field)', action: 'Submits the password or code.' },
        { keys: 'Enter / Space (button)', action: 'Activates the focused button.' },
        { keys: 'Escape', action: 'Cancels and returns focus; ignored while verifying.' },
      ],
    },
    accessibility: {
      role: 'dialog (aria-modal)',
      name: '`title` (default “Confirm it’s you”); `description` becomes the accessible description.',
      items: [
        'It is a `dialog`, not an `alertdialog`: it asks for input, it does not warn.',
        'The password field uses `autocomplete="current-password"` so password managers fill it; the toggle is a pressed button named “Show password”.',
        'The code field is one real input with `autocomplete="one-time-code"` and the numeric keypad.',
        'A failed check is announced assertively (`role="alert"`) and linked to the field with `aria-describedby`.',
        'The method switcher is a labelled radio group (“Verification method”).',
        'The lock icon is decorative (`aria-hidden`).',
      ],
    },
    responsive: {
      items: [
        'Default width 24rem (`sm`), never wider than the screen minus 2rem; fits a 390px screen without scrolling sideways.',
        'The method switcher shares its width equally; the code boxes stay within the dialog.',
        'On touch screens the show/hide toggle and the forgot link have 44px hit areas.',
      ],
    },
    theme: {
      items: [
        'Dialog surface tokens, the muted surface behind the lock icon, danger tokens for the error.',
        'Light, dark and more-contrast themes apply automatically; forced colours use system colours for the icon and pressed toggle; motion follows `prefers-reduced-motion` through Dialog.',
      ],
    },
    examples: {
      items: [
        { title: 'Password with a forgot link', render: () => <PasswordOnly /> },
        { title: 'Several methods, passkey preferred', render: () => <SeveralMethods /> },
        { title: 'Authenticator code only', render: () => <CodeOnly /> },
        { title: 'Before an action, with useStepUpAuth', render: () => <Imperative /> },
      ],
    },
    doDont: {
      do: [
        'Verify on the server and remember the check for a short time, so people are not asked again for every action.',
        'List methods in the order you want people to use them, strongest first.',
        'Reject with an Error whose message says what to do next.',
        'Run the action only after `requireStepUp` resolves `true` (or in `onVerified`).',
      ],
      dont: [
        'Do not use it to confirm intent; pair it with ConfirmDialog when both are needed.',
        'Do not reveal whether an account exists or which part of a credential was wrong.',
        'Do not close the dialog yourself before `onVerify` has settled.',
      ],
    },
    related: {
      items: [
        {
          id: 'confirm-dialog',
          reason: 'Confirms intent; use it before or after the identity check.',
        },
        { id: 'otp-pin-input', reason: 'The code field used for authenticator codes.' },
        { id: 'segmented-control', reason: 'Switches between verification methods.' },
        { id: 'type-to-confirm-input', reason: 'Typed confirmation for irreversible actions.' },
        { id: 'dialog', reason: 'The base it is built on.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'alert',
        'form-field',
        'otp-pin-input',
        'segmented-control',
        'text-field',
        'link',
        'dialog',
      ],
    },
    testing: {
      unit: [
        'Opens as a dialog (not alertdialog) with focus in the password field; axe passes; Escape cancels and restores focus.',
        'Shows and hides the password with a pressed toggle.',
        'Asks for a password before calling `onVerify`.',
        'Verifying: aria-busy, Cancel disabled, field read-only, Escape blocked; success closes and calls `onVerified`.',
        'A rejection is shown assertively, linked to the field, with focus kept in the field.',
        'Generic message for non-Error rejections; `errorMessage` maps errors; a synchronous throw is shown too.',
        'Authenticator code submits itself at six digits; a failure clears the code and keeps focus; an incomplete code asks for all digits.',
        'Passkey: one button calls `onVerify({ method: "passkey" })`.',
        'Switches methods in preference order with a radio group.',
        'Forgot link or button.',
        'Starts clean on every opening.',
        'useStepUpAuth resolves true when verified and false when cancelled, returning focus to the trigger.',
        'Renders on the server (closed and open).',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
