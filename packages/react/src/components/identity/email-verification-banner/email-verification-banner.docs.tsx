import { EmailVerificationBanner } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './email-verification-banner.fixture.json';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

export default {
  id: 'email-verification-banner',
  title: 'EmailVerificationBanner',
  summary: 'An “unverified email” notice with Resend (with a cooldown) and Change email.',
  sections: {
    purpose: {
      summary:
        'Until people confirm their email address, some features stay locked and account recovery may not work. EmailVerificationBanner tells them, shows which address the link went to, and lets them resend it, waiting out a cooldown between sends so they cannot flood their inbox or hit rate limits. It also exports `useResendCooldown` for any other “resend” control. Needed by: Rook ID (sign-up verification, `app/auth/verify`), and every app that signs people in with an unverified address.',
      useWhen: [
        'A signed-in person has not verified their email yet.',
        'After changing the email address, until the new one is verified.',
      ],
      avoidWhen: [
        'The verification screen itself, where entering a code is the main task: use OtpPinInput with `useResendCooldown`.',
        'General announcements: use CalloutBanner.',
      ],
    },
    designPrinciples: {
      items: [
        'Show the exact address, so a typo is obvious and “Change email” is the natural next step.',
        'The cooldown is visible as a countdown instead of an unexplained disabled button.',
        'The button stays focusable during the cooldown (`aria-disabled`), so keyboard users keep their place.',
        'Callbacks out: the product sends the email and decides the cooldown length.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Accepts the CalloutBanner layout props (`placement`, `landmark`, `dismissible`, `onDismiss`, `open`, `actionPlacement`).',
        '`useResendCooldown({ onResend, cooldownSeconds?, initialCooldown?, errorMessage? })` returns `{ resend, pending, remaining, sent, error }`.',
        'Migrating from the app copy (Rook ID `app/auth/verify/page.tsx`): the `resending` / `resendCooldown` state, the 1-second interval and the toast become `onResend={() => api.resend(email)}` and `cooldownSeconds={60}`; pass `initialCooldown` when the first email was just sent.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['warning', 'info'],
          description:
            '`warning` (default) when features are locked until verified; `info` when nothing is blocked yet.',
        },
        {
          prop: 'placement',
          values: ['section', 'page'],
          description: 'A rounded callout in content, or a full-width strip across the page.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Sending',
          attribute: 'aria-busy (Resend)',
          description: 'The resend promise is running; the button shows a spinner.',
        },
        {
          name: 'Cooldown',
          attribute: 'data-cooldown',
          description:
            'After a send, the button reads “Resend in 42s” and is `aria-disabled` until the countdown ends.',
        },
        {
          name: 'Sent',
          attribute: 'role="status"',
          description: '“Verification email sent” is shown and announced.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description: 'The send failed; the message shows and Resend is available at once.',
        },
      ],
    },
    interaction: {
      items: [
        'Resend calls `onResend`; on success the cooldown starts, on failure the error shows and no cooldown starts.',
        'Clicks during sending or the cooldown do nothing.',
        'Change email calls `onChangeEmail`; Dismiss (with `onDismiss`) hides the banner.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between Resend, Change email and Dismiss.' },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
      ],
    },
    accessibility: {
      role: 'none by default (a `div`); `landmark` makes it a region',
      name: 'The title, when it is a landmark.',
      items: [
        'Success is announced through a polite status line; failure through an alert.',
        'During the cooldown the button stays in the tab order with `aria-disabled="true"` and its label says how long to wait.',
        'The address is part of the text, not only a tooltip.',
      ],
    },
    responsive: {
      items: [
        'On narrow screens the actions move below the text (CalloutBanner layout).',
        'Long addresses wrap anywhere instead of scrolling.',
        'Buttons are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Warning or info tone colours from CalloutBanner.',
        'The sent line uses the success strong colour; errors the danger strong colour.',
        'The countdown uses tabular numbers so the button does not jitter.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Resend with a cooldown',
          stack: true,
          render: () => (
            <EmailVerificationBanner
              email={fixture.data.email}
              cooldownSeconds={fixture.data.cooldownSeconds}
              onResend={() => wait(700)}
              onChangeEmail={() => undefined}
            />
          ),
        },
        {
          title: 'Page strip, dismissible',
          stack: true,
          render: () => (
            <EmailVerificationBanner
              email="priya@example.com"
              placement="page"
              tone="info"
              onResend={() => wait(500)}
              onDismiss={() => undefined}
            />
          ),
        },
        {
          title: 'Just signed up (cooldown already running)',
          stack: true,
          render: () => (
            <EmailVerificationBanner
              email="alex@example.com"
              initialCooldown={45}
              onResend={() => wait(500)}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Match `cooldownSeconds` to the server’s rate limit.',
        'Offer Change email so a mistyped address is not a dead end.',
      ],
      dont: [
        'Do not hide the address: people need it to find the email or spot a typo.',
        'Do not make the banner dismissible if features stay locked until verification.',
      ],
    },
    related: {
      items: [
        { id: 'callout-banner', reason: 'The banner it is built on.' },
        { id: 'otp-pin-input', reason: 'Entering a verification code.' },
        { id: 'sign-up-form', reason: 'Where the first verification email is sent.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'callout-banner'] },
    testing: {
      unit: [
        'Shows the address with Resend and Change email; axe passes.',
        'Resends, announces it, keeps focus and counts down the cooldown; clicks during the cooldown are ignored.',
        'A failure shows the error and allows another try at once.',
        'Initial cooldown, dismiss and translated messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
