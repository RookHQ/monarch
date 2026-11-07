import { useState } from 'react';
import {
  ConsentProvider,
  PrivacyChoicesLink,
  type ConsentRecord,
  type ConsentStorage,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { GearIcon } from '../../../internal/docs-demo.js';

/** Keeps the demo's choice in memory. */
function memoryStorage(): ConsentStorage {
  let value: ConsentRecord | null = null;
  return {
    read: () => value,
    write: (record) => {
      value = record;
    },
  };
}

function FooterDemo({ withIcon = false }: { withIcon?: boolean }) {
  const [storage] = useState(memoryStorage);
  return (
    <ConsentProvider policyVersion="2026-01" regime="opt-out" storage={storage}>
      <footer>
        <PrivacyChoicesLink
          policyHref="#privacy-policy"
          {...(withIcon ? { icon: <GearIcon /> } : {})}
        />
      </footer>
    </ConsentProvider>
  );
}

function DoNotSellDemo() {
  const [storage] = useState(memoryStorage);
  return (
    <ConsentProvider policyVersion="2026-01" regime="opt-out" storage={storage}>
      <PrivacyChoicesLink label="Do not sell or share my personal information" />
    </ConsentProvider>
  );
}

export default {
  id: 'privacy-choices-link',
  title: 'PrivacyChoicesLink',
  summary:
    'Footer "Your privacy choices" control: a dialog with one switch for the sale or sharing of personal information.',
  sections: {
    purpose: {
      summary:
        'Gives people in opt-out regions a permanent, easy way to stop the sale or sharing of their personal information. It opens a small dialog with an "Allow sale or sharing" switch bound to the provider’s sale-or-sharing categories, and respects a Global Privacy Control signal. The component implements an interaction pattern; your legal team decides where it is needed, the label and the icon.',
      useWhen: [
        'In the site footer for visitors in regions where people can opt out of sale or sharing.',
        'On an account privacy page, next to other privacy requests.',
      ],
      avoidWhen: [
        'Choosing category by category: use ConsentSettingsLink and ConsentPreferences.',
        'Regions where consent is asked first: the banner and ConsentPreferences already cover it (it renders nothing there by default).',
      ],
    },
    designPrinciples: {
      items: [
        'One clear choice: a single switch, saved with one button.',
        'The signal wins: with Global Privacy Control on, the switch is off and locked, and the dialog says why in words.',
        'Shown only where it applies (`showIn`), so other visitors are not offered a choice that means nothing for them.',
        'No built-in third-party logo: the icon slot takes whatever your legal team approves.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Must be inside a `ConsentProvider`. Turning the switch off and saving calls `optOutOfSaleOrSharing()`; turning it on calls `save()` with the sale-or-sharing categories on.',
        'Forwards its ref and extra button attributes to the footer button.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`label` changes the button text, e.g. "Do not sell or share my personal information".',
        '`messages` overrides the dialog’s strings.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showIn',
          values: ["['opt-out'] (default)", 'any ConsentRegime[]'],
          description: 'Regimes in which the control renders; elsewhere it renders nothing.',
        },
        {
          prop: 'icon',
          values: ['undefined', 'ReactNode'],
          description: 'A decorative icon before the label.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Hidden', description: 'The provider’s regime is not in `showIn`.' },
        {
          name: 'Allowed / opted out',
          attribute: 'aria-checked (switch)',
          description: 'The switch reflects whether every sale-or-sharing category is allowed.',
        },
        {
          name: 'Locked by GPC',
          attribute: 'data-gpc',
          description:
            'A Global Privacy Control signal is on: the switch is off and disabled, with a note.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the button opens the dialog; each opening starts from the saved choice.',
        'Save applies the switch, records the choice (`onRecord` on the provider) and closes.',
        'Cancel, Escape or the close button close without saving; focus returns to the footer button.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'Opens the dialog from the footer button.' },
        { keys: 'Tab / Shift+Tab', action: 'Moves between the switch and the buttons.' },
        { keys: 'Space', action: 'Toggles the switch.' },
        { keys: 'Escape', action: 'Closes without saving.' },
      ],
    },
    accessibility: {
      role: 'button (opens a dialog)',
      name: 'The visible `label`; the dialog is named by its title.',
      items: [
        'The trigger is a real button with `aria-haspopup="dialog"` and `aria-expanded`.',
        'The switch is labelled "Allow sale or sharing" and described by the help or the GPC note.',
        'The icon is decorative (`aria-hidden`); the label carries the meaning.',
      ],
    },
    responsive: {
      items: [
        'The label wraps in narrow footers; the dialog fits a 390px screen.',
        'The button and dialog buttons are at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Link colour and focus ring tokens for the trigger; dialog surface tokens inside.',
        'Light, dark and more-contrast themes apply automatically; forced colours use LinkText.',
      ],
    },
    examples: {
      items: [
        { title: 'Footer control', render: () => <FooterDemo /> },
        { title: 'With an icon', render: () => <FooterDemo withIcon /> },
        { title: 'Alternative label', render: () => <DoNotSellDemo /> },
      ],
    },
    doDont: {
      do: [
        'Place it in the footer of every page for visitors in opt-out regions.',
        'Mark advertising and cross-site tracking categories `saleOrSharing` on the provider.',
        'Store each record from `onRecord`.',
      ],
      dont: [
        'Do not hide it behind a menu or a settings page only.',
        'Do not let the switch turn sale or sharing back on while a Global Privacy Control signal is on.',
        'Do not tell people the control alone meets a law; your legal team decides what is required.',
      ],
    },
    related: {
      items: [
        { id: 'consent-manager', reason: 'ConsentProvider: regime, GPC and records.' },
        { id: 'cookie-consent-banner', reason: 'The first-visit banner with the same choice.' },
        { id: 'consent-preferences', reason: 'Category-by-category settings.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'switch', 'link', 'dialog', 'consent-manager'],
    },
    testing: {
      unit: [
        'Renders only in opt-out by default; `showIn` widens it.',
        'Opens a dialog (axe passes); switching off and saving opts out of sale or sharing and returns focus.',
        'Saving with the switch on keeps sale or sharing; Cancel and Escape save nothing; each opening starts from the saved choice.',
        'GPC (stubbed navigator.globalPrivacyControl): the switch is off, locked and described by the note.',
        'Custom label, decorative icon slot and messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
