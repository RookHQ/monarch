import { SignInForm } from '@rookcom/monarch-react/patterns';
import { SignInWithRookButton } from '@rookcom/monarch-react/rook';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './sign-in-form.fixture.json';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

async function demoSignIn({ password }: { password: string }) {
  await wait(800);
  // Sample rule for the demo: any password other than "demo-password" is wrong.
  if (password !== 'demo-password') {
    return { fieldErrors: { password: 'That password is not right. Try again or reset it.' } };
  }
  return undefined;
}

export default {
  id: 'sign-in-form',
  title: 'SignInForm',
  summary: 'Email (or username) and password, with passkey, SSO, remember me and links.',
  sections: {
    purpose: {
      summary:
        'Every product with accounts has a sign-in form, and small mistakes there (wrong autocomplete, errors that are not tied to fields, focus lost after a failed attempt) cost people their way in. SignInForm gets those details right: password-manager friendly autocomplete including passkey autofill, validation on submit with focus on the first problem, server errors on the right field, a calm note when a passkey prompt is cancelled, and slots for single sign-on. It has no page chrome, so it sits inside AuthPage or any layout. Needed by: Rook ID (`app/auth/page.tsx`) and any app with its own sign-in screen.',
      useWhen: [
        'The sign-in step of an auth page.',
        'Re-entering credentials in a dialog after a session expires.',
      ],
      avoidWhen: [
        'Confirming a sensitive action for someone already signed in: use StepUpAuthDialog.',
        'Apps that sign in only through Rook ID: use SignInWithRookButton alone.',
      ],
    },
    designPrinciples: {
      items: [
        'Work with password managers and passkey autofill, never against them.',
        'Errors say what to do next and sit on the field they are about.',
        'Cancelling a passkey prompt is normal, not an error.',
        'The product owns authentication; the form owns input, validation and feedback.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onSubmit({ identifier, password, remember })` may resolve with `{ error?, fieldErrors?: { identifier?, password? } }` or reject.',
        '`renderLink({ href, className, children })` renders links with the product’s router; `AuthFormLinkProps` and `plainAuthLink` are shared with SignUpForm and PasswordResetForm.',
        'Migrating from shadcn/ui (Rook ID `app/auth/page.tsx`): the `Label` + `Input id="emailOrUsername"` pair, the submit `Button` with its spinner and the `toast.error` calls become `<SignInForm identifier="email-or-username" onSubmit forgotPasswordHref signUpHref />`; return field errors instead of toasting.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'identifier',
          values: ['email', 'username', 'email-or-username'],
          description: 'What people sign in with; sets the label, input type and validation.',
        },
        {
          prop: 'password',
          values: ['true', 'false'],
          description: 'Show the password field, or sign in with passkey or SSO only.',
        },
        {
          prop: 'rememberMe',
          values: ['false', 'true'],
          description: 'Show “Keep me signed in”.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Pending',
          attribute: 'aria-busy',
          description:
            'On the form and the pressed button while sign-in runs; fields are disabled.',
        },
        {
          name: 'Field error',
          attribute: 'aria-invalid',
          description:
            'A field failed validation or the server rejected it; the message describes it.',
        },
        {
          name: 'Form error',
          attribute: 'role="alert"',
          description: 'A form-level error at the top.',
        },
        {
          name: 'Passkey cancelled',
          attribute: 'role="status"',
          description: 'A neutral note after the browser prompt was dismissed.',
        },
      ],
    },
    interaction: {
      items: [
        'Submitting validates first; the first invalid field receives focus.',
        'Typing in a field clears its error.',
        'A wrong password is cleared so it can be typed again.',
        'The passkey button calls `onPasskey`; SSO buttons come from `providers`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the methods, fields, links and buttons.' },
        { keys: 'Enter (in a field)', action: 'Submits the form.' },
        { keys: 'Space', action: 'Ticks “Keep me signed in” or toggles the password view.' },
      ],
    },
    accessibility: {
      role: 'form',
      name: 'Give it `aria-label` or `aria-labelledby` pointing at the page heading.',
      items: [
        'Every field has a visible label; required fields are marked.',
        'Field errors are linked with `aria-describedby` and set `aria-invalid`.',
        'The form-level error is an assertive alert; the passkey cancel note is polite.',
        'Autocomplete: `username` (plus `webauthn` with a passkey) and `current-password`.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; buttons are full width.',
        'Links are 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Uses Field, Input, PasswordInput, Button and Alert tokens.',
        'Links use the link text colour; the divider the subtle border.',
        'Forced-colors mode uses LinkText for links.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Email and password (password: demo-password)',
          render: () => (
            <SignInForm
              aria-label="Sign in"
              onSubmit={demoSignIn}
              forgotPasswordHref="#reset"
              signUpHref="#sign-up"
              rememberMe
              defaultIdentifier={fixture.data.email}
              defaultRemember={fixture.data.keepSignedIn}
            />
          ),
        },
        {
          title: 'With a passkey and single sign-on',
          render: () => (
            <SignInForm
              aria-label="Sign in with a passkey or single sign-on"
              identifier="email-or-username"
              onSubmit={() => wait(600)}
              onPasskey={() => wait(900)}
              providers={<SignInWithRookButton href="#rook" />}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Say “email or password is incorrect” as a form error when you cannot tell which one is wrong.',
        'Pass `forgotPasswordHref` whenever passwords are allowed.',
        'Wire `onPasskey` to conditional mediation so passkey autofill works.',
      ],
      dont: [
        'Do not disable paste in the password field.',
        'Do not clear the identifier after a failed attempt.',
      ],
    },
    related: {
      items: [
        { id: 'sign-up-form', reason: 'Creating an account.' },
        { id: 'password-reset-form', reason: 'Resetting a forgotten password.' },
        { id: 'auth-page', reason: 'The page layout these forms sit in.' },
        { id: 'sign-in-with-rook-button', reason: 'Single sign-on with Rook ID.' },
        { id: 'password-input', reason: 'The password field.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'alert', 'checkbox', 'form-field', 'password-input', 'text-field'],
    },
    testing: {
      unit: [
        'Labelled fields, links and autocomplete hints; axe passes.',
        'Validates on submit and focuses the first invalid field.',
        'Submits with the keyboard and shows a pending state.',
        'Server field errors, cleared password, and rejection alerts.',
        'Passkey sign-in; a cancelled prompt is neutral; providers slot.',
        'Passkey-only mode, router links, external error and translated messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
