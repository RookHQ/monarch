import { SignUpForm } from '@rookcom/monarch-react/patterns';
import { SignInWithRookButton } from '@rookcom/monarch-react/rook';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const TERMS = [
  { label: 'Terms of Service', href: '#terms', version: '2026-01' },
  { label: 'Privacy Policy', href: '#privacy', version: '2026-01' },
];

async function demoSignUp({ email }: { email: string }) {
  await wait(800);
  // Sample rule for the demo: addresses at example.org are "already registered".
  if (email.endsWith('@example.org')) {
    return { fieldErrors: { email: 'That email is already registered. Sign in instead.' } };
  }
  return undefined;
}

export default {
  id: 'sign-up-form',
  title: 'SignUpForm',
  summary: 'Name, email and a new password with strength meter, terms and SSO.',
  sections: {
    purpose: {
      summary:
        'Creating an account is the first thing people do, and forms that reject passwords without saying why, lose typed values on errors or hide the terms make them leave. SignUpForm asks only for what is needed, shows the password requirements and strength while typing, records which terms versions were accepted, and maps server errors (such as an email already registered) to the right field. It has no page chrome, so it fits inside AuthPage. Needed by: Rook ID (`app/auth/signup/page.tsx`) and any product with its own sign-up.',
      useWhen: [
        'The create-account step of an auth page.',
        'Accepting an invitation, with the invited email prefilled and read-only.',
      ],
      avoidWhen: [
        'Signing in: use SignInForm.',
        'Long onboarding (company details, address): collect those after the account exists.',
      ],
    },
    designPrinciples: {
      items: [
        'Show every password requirement up front and tick them off while typing.',
        'Never clear what people typed because of an error.',
        'Terms acceptance is explicit and recorded with document versions.',
        'One password field with a reveal button by default; a confirmation field is optional.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onSubmit({ name, email, password, terms? })` may resolve with `{ error?, fieldErrors? }` keyed by `name`, `email`, `password`, `confirmPassword`, `terms`.',
        '`passwordRules` are PasswordStrengthMeter rules; every one must be met.',
        'Migrating from shadcn/ui (Rook ID `app/auth/signup/page.tsx`): the `Label`/`Input` pairs for first and last name, email and password, and the terms paragraph become `<SignUpForm terms onSubmit />`. Split the full name in the product if the API needs first and last names. Business and address fields belong to a later onboarding step (WorkspaceOnboardingForm).',
      ],
    },
    variants: {
      items: [
        { prop: 'askName', values: ['true', 'false'], description: 'Ask for a name.' },
        {
          prop: 'confirmPassword',
          values: ['false', 'true'],
          description: 'Ask for the password a second time.',
        },
        {
          prop: 'terms',
          values: ['unset', 'TermsDocument[]'],
          description: 'Show a required terms acceptance checkbox.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Pending',
          attribute: 'aria-busy',
          description: 'While the account is being created; fields are disabled.',
        },
        {
          name: 'Field error',
          attribute: 'aria-invalid',
          description: 'Validation or server error on a field, described by its message.',
        },
        {
          name: 'Form error',
          attribute: 'role="alert"',
          description: 'A form-level error at the top.',
        },
        {
          name: 'Password strength',
          attribute: 'data-score (meter)',
          description: 'The meter’s score from 0 to 4, announced politely.',
        },
      ],
    },
    interaction: {
      items: [
        'Submitting validates every field and focuses the first problem.',
        'The strength meter updates while the password is typed.',
        'Typing in a field clears its error.',
        'Server field errors move focus to that field.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through providers, fields, terms and the button.' },
        { keys: 'Enter (in a field)', action: 'Submits the form.' },
        { keys: 'Space', action: 'Ticks the terms box or toggles the password view.' },
      ],
    },
    accessibility: {
      role: 'form',
      name: 'Give it `aria-label` or `aria-labelledby` pointing at the page heading.',
      items: [
        'Every field has a visible label; errors are linked with `aria-describedby`.',
        'The password is also described by the strength meter and its requirements.',
        'Autocomplete: `name`, `email` and `new-password`, so password managers offer to generate one.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; one column at every width.',
        'Links are 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Uses Field, Input, PasswordInput, PasswordStrengthMeter, TermsAcceptance and Button tokens.',
        'Forced-colors mode uses LinkText for links and CanvasText for the divider.',
      ],
    },
    examples: {
      items: [
        {
          title: 'With terms (try an @example.org address)',
          render: () => (
            <SignUpForm
              aria-label="Create account"
              terms={TERMS}
              signInHref="#sign-in"
              onSubmit={demoSignUp}
            />
          ),
        },
        {
          title: 'Invited, with single sign-on',
          render: () => (
            <SignUpForm
              aria-label="Create account from an invitation"
              defaultEmail="invited@example.com"
              emailReadOnly
              confirmPassword
              providers={<SignInWithRookButton href="#rook" block />}
              onSubmit={() => wait(600)}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep the form to name, email and password; ask the rest later.',
        'Return `fieldErrors.email` for an address that is already registered, with a way to sign in.',
      ],
      dont: ['Do not pre-tick the terms box.', 'Do not add password rules the meter cannot show.'],
    },
    related: {
      items: [
        { id: 'sign-in-form', reason: 'Signing in.' },
        { id: 'password-strength-meter', reason: 'The live strength and requirements.' },
        { id: 'terms-acceptance', reason: 'The terms checkbox.' },
        { id: 'email-verification-banner', reason: 'After sign-up, until the email is verified.' },
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
        'terms-acceptance',
        'text-field',
        'sign-in-form',
      ],
    },
    testing: {
      unit: [
        'Labelled fields, autocomplete and strength meter; axe passes.',
        'Validates every field, including weak and mismatched passwords, and focuses the first problem.',
        'Submits values with terms proof and shows a pending state.',
        'Maps server errors to fields and shows rejections.',
        'Providers, read-only invited email and translated messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
