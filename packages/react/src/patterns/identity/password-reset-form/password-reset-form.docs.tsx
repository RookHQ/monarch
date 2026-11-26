import { PasswordResetForm } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

export default {
  id: 'password-reset-form',
  title: 'PasswordResetForm',
  summary: 'Request a reset link by email, then choose a new password.',
  sections: {
    purpose: {
      summary:
        'Forgetting a password should cost a minute, not a support ticket. PasswordResetForm covers both halves of the flow: asking for a reset link (with a “check your email” step that never reveals whether an account exists, and a resend cooldown), and, on the page the link opens, choosing a new password with live requirements, a confirmation and a way back to sign in. It uses PasswordInput and PasswordStrengthMeter and has no page chrome, so it sits inside AuthPage. Needed by: Rook ID (forgot-password flow linked from `app/auth/page.tsx`) and any product with password sign-in.',
      useWhen: [
        'The forgot-password page (`request` step).',
        'The page a reset link opens (`step="reset"`).',
      ],
      avoidWhen: [
        'Changing a password while signed in (knowing the current one): use a settings form with the current password and StepUpAuthDialog.',
        'Passwordless accounts: offer a passkey or a sign-in link instead.',
      ],
    },
    designPrinciples: {
      items: [
        'Never reveal whether an email has an account: the sent step says “if an account exists”.',
        'Each step names itself in a heading that takes focus, so screen-reader users know where they are.',
        'The same requirements and meter as sign-up, so the new password is accepted first time.',
        'The product holds the reset token and calls its API; the form owns input and feedback.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onRequest(email)` sends the link; `onReset(password)` saves the new password. Both may reject to show a message.',
        'Pass `step="reset"` on the link’s page and `error="This link has expired"` when the token is no longer valid.',
        'No Rook app has its own reset form yet, so there is nothing to migrate.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'step',
          values: ['request', 'sent', 'reset', 'done'],
          description: 'Which step shows. Uncontrolled it moves request → sent and reset → done.',
        },
        {
          prop: 'confirmPassword',
          values: ['true', 'false'],
          description: 'Ask for the new password twice.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Step', attribute: 'data-step', description: 'The current step, on the root.' },
        {
          name: 'Pending',
          attribute: 'aria-busy',
          description: 'On the form while a request or reset runs.',
        },
        {
          name: 'Resend cooldown',
          attribute: 'aria-disabled (Resend)',
          description: 'Resend reads “Resend in 42s” and stays focusable.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description: 'A rejected callback or the `error` prop.',
        },
      ],
    },
    interaction: {
      items: [
        'Sending a valid email calls `onRequest` and shows the sent step.',
        'Resend calls `onRequest` again and starts a cooldown; “Use a different email” returns to the first step with the email kept.',
        'Changing the password validates the requirements and confirmation, calls `onReset`, then shows the done step.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the fields, buttons and links.' },
        { keys: 'Enter (in a field)', action: 'Submits the step.' },
        {
          keys: 'Enter / Space (Resend)',
          action: 'Sends another link when the cooldown has ended.',
        },
      ],
    },
    accessibility: {
      role: 'form (named by the step heading)',
      name: 'The step heading, e.g. "Reset your password".',
      items: [
        'The heading of a new step receives focus.',
        'Field errors are linked with `aria-describedby`; the new password is also described by the strength meter.',
        'A hidden `username` field helps password managers save the new password for the right account.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; buttons are full width.',
        'Links and buttons are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Uses Field, Input, PasswordInput, PasswordStrengthMeter, Button and Alert tokens.',
        'The done step’s sign-in link is styled as a primary button.',
        'Forced-colors mode uses LinkText and ButtonText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Request a link',
          render: () => (
            <PasswordResetForm
              onRequest={() => wait(700)}
              signInHref="#sign-in"
              cooldownSeconds={20}
            />
          ),
        },
        {
          title: 'Choose a new password',
          render: () => (
            <PasswordResetForm
              defaultStep="reset"
              defaultEmail="sam@example.com"
              onReset={() => wait(700)}
              signInHref="#sign-in"
            />
          ),
        },
        {
          title: 'Expired link',
          render: () => (
            <PasswordResetForm
              step="reset"
              error="This reset link has expired. Request a new one."
              onReset={() => undefined}
              messages={{ resetTitle: 'Link expired' }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use the same `passwordRules` as sign-up.',
        'Sign people out of other sessions after a reset, and say so on the done step if you do.',
      ],
      dont: [
        'Do not say whether an email address has an account.',
        'Do not sign people in silently after a reset without telling them.',
      ],
    },
    related: {
      items: [
        { id: 'sign-in-form', reason: 'Links here with “Forgot password?”.' },
        { id: 'password-input', reason: 'The password fields.' },
        { id: 'password-strength-meter', reason: 'The live requirements.' },
        {
          id: 'email-verification-banner',
          reason: 'Shares the resend cooldown (useResendCooldown).',
        },
        { id: 'auth-page', reason: 'The page layout.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'alert',
        'form-field',
        'password-input',
        'password-strength-meter',
        'text-field',
        'email-verification-banner',
        'sign-in-form',
      ],
    },
    testing: {
      unit: [
        'Requests a link, validates the email and moves focus to the next step; axe passes.',
        'Resends with a cooldown and returns to change the email.',
        'Sets a new password with requirements and confirmation; axe passes.',
        'Done step with a sign-in link.',
        'Rejections, outside errors and translated messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
