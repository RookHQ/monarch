import { Button } from '@rookcom/monarch-react';
import { TwoFactorSetup } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

/** A placeholder pattern standing in for the server's QR image. */
const FINDERS: ReadonlyArray<readonly [number, number]> = [
  [0, 0],
  [14, 0],
  [0, 14],
];
const DOTS: ReadonlyArray<readonly [number, number]> = [
  [9, 8],
  [11, 13],
  [13, 9],
  [8, 11],
  [10, 16],
  [12, 18],
  [16, 10],
  [18, 15],
];

function DemoQr() {
  const light = { fill: 'var(--mn-color-bg-surface)' };
  return (
    <svg viewBox="0 0 21 21" role="img" aria-label="Sample QR code" shapeRendering="crispEdges">
      <rect width="21" height="21" style={light} />
      {FINDERS.map(([x, y]) => (
        <g key={`${x}-${y}`}>
          <rect x={x} y={y} width="7" height="7" fill="currentColor" />
          <rect x={x + 1} y={y + 1} width="5" height="5" style={light} />
          <rect x={x + 2} y={y + 2} width="3" height="3" fill="currentColor" />
        </g>
      ))}
      {DOTS.map(([x, y]) => (
        <rect key={`${x}-${y}`} x={x} y={y} width="1" height="1" fill="currentColor" />
      ))}
    </svg>
  );
}

function Setup() {
  return (
    <TwoFactorSetup
      trigger={<Button variant="primary">Turn on two-step verification</Button>}
      secret="JBSWY3DPEHPK3PXPJBSWY3DP"
      qrCode={<DemoQr />}
      accountName="sam@example.com"
      onVerify={async (code) => {
        await wait(700);
        if (code === '000000') throw new Error('That code didn’t work. Try the newest code.');
        return ['k7f2-9qd4', 'm3x8-2hp6', 'r5c1-8vt3', 'w9n4-6bz2', 'a2j7-4ls9', 'e6y3-1gu5'];
      }}
    />
  );
}

export default {
  id: 'two-factor-setup',
  title: 'TwoFactorSetup',
  summary: 'Turns on two-step verification: scan a QR code, confirm a code, save recovery codes.',
  sections: {
    purpose: {
      summary:
        'Turning on two-step verification has three parts people must not skip: add the account to an authenticator app, prove it works with a code, and save recovery codes. TwoFactorSetup walks through them in one dialog. Needed by: Rook ID (security settings; its TwoFactorModal is a placeholder that says the feature is coming).',
      useWhen: [
        'Turning on app-based (TOTP) two-step verification for an account.',
        'Re-enrolling after the person has lost their device.',
      ],
      avoidWhen: [
        'Asking for a code to confirm a sensitive action: use StepUpAuthDialog.',
        'Typing a code sent by email or text during sign-in: use OTPPinInput in the sign-in form.',
      ],
    },
    designPrinciples: {
      items: [
        'Verify before enabling: the product only turns it on in `onVerify` after a correct code.',
        'Recovery codes cannot be skipped past by accident: Done asks for an explicit acknowledgement.',
        'Always offer the key as text for people who cannot scan.',
        'No QR encoder in Monarch: the server already has the `otpauth://` URI and returns the image.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onVerify(code)` returns or resolves to the recovery codes (`string[]`), or nothing; reject to say the code was wrong.',
        'Pass the QR as `qrCodeSrc` (URL or data URL) or `qrCode` (an element).',
        'Migrating from the app copy (Rook ID `TwoFactorModal`, a placeholder): `open` and `onOpenChange` are unchanged. The product adds an enrolment call that returns `secret` and the QR image, and a verify call for `onVerify`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'codeLength',
          values: ['6 (default)', '8'],
          description: 'Digits in a code; six are shown as two groups of three.',
        },
        {
          prop: 'qrCode / qrCodeSrc',
          values: ['element', 'URL', 'omitted'],
          description: 'Without either, only the key is shown.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Step',
          attribute: 'data-step',
          description: '`scan`, `verify` or `recovery`, with "Step N of 3" above the title.',
        },
        {
          name: 'Verifying',
          attribute: 'aria-busy',
          description: 'The Verify button spins and nothing dismisses the dialog.',
        },
        {
          name: 'Wrong code',
          attribute: 'aria-invalid (code field)',
          description: 'The message shows under the field and the code is cleared.',
        },
        {
          name: 'Not acknowledged',
          attribute: 'aria-invalid (checkbox)',
          description: 'Done was pressed before ticking the box.',
        },
      ],
    },
    interaction: {
      items: [
        'Next moves to the code step; Back returns to the QR code.',
        'The code is checked as soon as the last digit is typed or pasted, or with Verify.',
        'After a correct code the recovery codes are shown with Copy codes and Download (a .txt file).',
        'Done finishes once the acknowledgement is ticked; when there are no recovery codes, setup finishes right after verifying.',
        'Every opening starts again at the first step.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through the controls; focus stays in the dialog.',
        },
        { keys: 'Enter', action: 'Next, Verify or Done for the current step.' },
        { keys: 'Escape', action: 'Cancels, except while verifying.' },
      ],
    },
    accessibility: {
      role: 'dialog',
      name: 'The current step’s title; its description is the accessible description.',
      items: [
        'Focus moves to the title on each step, and to the code field on the code step.',
        'The QR image has alt text; the key is a labelled, copyable text field.',
        'The code is one input (autofill and paste work), labelled "6-digit code".',
        'Recovery codes are a list named "Recovery codes".',
      ],
    },
    responsive: {
      items: [
        'The QR code scales down to the dialog width.',
        'Recovery codes show in two columns, one below 352px.',
        'Buttons are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'The QR code sits on a light field in every theme (`data-mn-theme="light"`), so it scans in dark mode.',
        'Recovery codes use the mono font on the subtle background.',
        'In forced-colors mode the QR field and code list get system-colour borders.',
      ],
    },
    examples: {
      items: [{ title: 'Full setup (enter 000000 to see a wrong code)', render: () => <Setup /> }],
    },
    doDont: {
      do: [
        'Turn two-step verification on in `onVerify`, only after the code checks out.',
        'Return fresh recovery codes from `onVerify`.',
        'Offer the same dialog again when someone loses their device.',
      ],
      dont: [
        'Do not show the secret anywhere after setup.',
        'Do not log or store the recovery codes in the browser.',
      ],
    },
    related: {
      items: [
        { id: 'otp-pin-input', reason: 'The code field.' },
        { id: 'copy-field', reason: 'The setup key.' },
        { id: 'step-up-auth-dialog', reason: 'Asks for a code later, before sensitive actions.' },
        { id: 'wizard-dialog', reason: 'A generic multi-step dialog.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'copy-button',
        'copy-field',
        'checkbox',
        'form-field',
        'otp-pin-input',
        'dialog',
      ],
    },
    testing: {
      unit: [
        'Scan step with QR, account and grouped key; title focused; axe passes.',
        'Verifies the code, shows recovery codes, requires the acknowledgement, then completes; axe passes.',
        'Incomplete and rejected codes show messages and clear the code.',
        'Finishes right after verifying when there are no recovery codes.',
        'recoveryCodes prop, Back and Escape (onCancel).',
        'Not dismissable while verifying.',
        'Translated messages.',
        'Renders its trigger on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
