import { useState } from 'react';
import { RecoveryCodes, formatRecoveryCodes } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const SAMPLE = [
  '4f7k-29dm',
  'q8zt-mx41',
  'h2vc-0pla',
  'r6yn-3ke8',
  'w1bs-7dqj',
  'c5ug-9fhr',
  'm3xe-6tnw',
  'p0lo-2ziv',
];

function fakeCodes() {
  return SAMPLE.map((code) => code.split('').reverse().join(''));
}

function Settings() {
  const [codes, setCodes] = useState<Array<string | { code: string; used: boolean }>>([
    ...SAMPLE.slice(0, 6),
    { code: SAMPLE[6] as string, used: true },
    { code: SAMPLE[7] as string, used: true },
  ]);
  return (
    <RecoveryCodes
      codes={codes}
      defaultRevealed={false}
      formatText={(list) => formatRecoveryCodes(list, 'Recovery codes for sam@example.com')}
      onRegenerate={async () => {
        await wait(700);
        setCodes(fakeCodes());
      }}
    />
  );
}

export default {
  id: 'recovery-codes',
  title: 'RecoveryCodes',
  summary: 'One-time recovery codes with copy, download, regenerate and an acknowledgement.',
  sections: {
    purpose: {
      summary:
        'When someone loses the device with their passkey or authenticator app, recovery codes are how they get back in, but only if they saved them. RecoveryCodes shows the codes in a readable monospace grid, makes them easy to copy or download, can hide them until asked, strikes through used ones, regenerates them behind a confirmation that the old ones stop working, and in setup flows asks people to confirm they saved them. Needed by: Rook ID security settings (`app/security/page.tsx`) and TwoFactorSetup, which renders the same codes inline today.',
      useWhen: [
        'The last step of two-step verification or passkey setup.',
        'A security settings page where people can view or regenerate their codes.',
      ],
      avoidWhen: [
        'API keys or other secrets that are shown once: use SecretField or ApiKeyRow.',
        'Entering a recovery code at sign-in: use OtpPinInput or a text field.',
      ],
    },
    designPrinciples: {
      items: [
        'Saving the codes is the point: Copy and Download are always one click away.',
        'Regenerating invalidates the old codes, so it is a danger action with a confirmation.',
        'Codes can start hidden on settings pages, so they are not shown to onlookers by default.',
        'The product generates codes; the component only shows them.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`codes` takes strings or `{ code, used }`.',
        '`formatRecoveryCodes(codes, heading?)` builds the copied and downloaded text with a heading line.',
        'TwoFactorSetup currently renders its own copy of the code grid, copy and download; it can adopt RecoveryCodes with `onAcknowledge`.',
        'No Rook app has its own copy of this component, so there is nothing to migrate.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'defaultRevealed',
          values: ['true', 'false'],
          description: 'Show the codes at once (setup), or hide them behind Show codes (settings).',
        },
      ],
      notes: [
        'Regenerate and the acknowledgement appear only with `onRegenerate` and `onAcknowledge`.',
      ],
    },
    states: {
      items: [
        {
          name: 'Revealed',
          attribute: 'data-revealed',
          description: 'The codes are visible; otherwise a hint replaces them.',
        },
        {
          name: 'Used code',
          attribute: 'data-used',
          description:
            'Struck through with hidden “(used)” text, and left out of copy and download.',
        },
        {
          name: 'Acknowledgement missing',
          attribute: 'role="alert"',
          description: 'Continue was pressed before ticking the box.',
        },
      ],
    },
    interaction: {
      items: [
        'Copy codes copies the unused codes; Download saves them as a text file.',
        'Show codes / Hide codes toggles visibility.',
        'Generate new codes asks for confirmation, calls `onRegenerate`, then shows and announces the new codes.',
        'Continue calls `onAcknowledge` only after the box is ticked; new codes reset the box.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between the actions, the checkbox and Continue.' },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
        { keys: 'Space', action: 'Ticks the acknowledgement.' },
        { keys: 'Escape', action: 'Closes the regenerate confirmation without changing anything.' },
      ],
    },
    accessibility: {
      role: 'ordered list of codes',
      name: '"Recovery codes" (`messages.listLabel`).',
      items: [
        'The Show/Hide toggle is a toggle button with `aria-pressed`.',
        'Used codes say “(used)” to screen readers, not only through strike-through.',
        'The missing acknowledgement is announced and describes the checkbox.',
      ],
    },
    responsive: {
      items: [
        'Two columns of codes, one below 352px wide.',
        'Actions wrap; Continue fills the width below 480px.',
        'Buttons are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Codes in the mono font on the subtle background.',
        'Generate new codes uses the danger-subtle button.',
        'Forced-colors mode draws the code box border in CanvasText and used codes in GrayText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Setup step with acknowledgement',
          stack: true,
          render: () => <RecoveryCodes codes={SAMPLE} onAcknowledge={() => undefined} />,
        },
        {
          title: 'Settings: hidden, some used, regenerate',
          stack: true,
          render: () => <Settings />,
        },
      ],
    },
    doDont: {
      do: [
        'Ask for the acknowledgement in setup flows before finishing.',
        'Add the account to the downloaded file with `formatText`, so people know what the codes are for.',
      ],
      dont: [
        'Do not email recovery codes.',
        'Do not regenerate without confirmation; the old codes stop working at once.',
      ],
    },
    related: {
      items: [
        { id: 'two-factor-setup', reason: 'The setup flow that ends with these codes.' },
        { id: 'passkey-manager', reason: 'Passkeys, the other sign-in method.' },
        { id: 'copy-button', reason: 'The copy action.' },
        { id: 'confirm-dialog', reason: 'The regenerate confirmation (useConfirm).' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'copy-button', 'checkbox', 'confirm-dialog'],
    },
    testing: {
      unit: [
        'Lists codes and marks used ones; axe passes.',
        'Copies unused codes and downloads a text file.',
        'Hides and reveals the codes with a pressed toggle.',
        'Confirms before regenerating; Escape cancels and focus returns; success is announced.',
        'Requires the acknowledgement before continuing; new codes reset it.',
        'Translated messages and formatRecoveryCodes.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
