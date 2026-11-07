import { useState } from 'react';
import {
  ConsentPreferences,
  ConsentProvider,
  ConsentSettingsLink,
  type ConsentRecord,
  type ConsentRegime,
  type ConsentStorage,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** Keeps the demo's choice in memory, starting from `initial`. */
function memoryStorage(initial: ConsentRecord | null): ConsentStorage {
  let value = initial;
  return {
    read: () => value,
    write: (record) => {
      value = record;
    },
  };
}

const SAVED: ConsentRecord = {
  id: 'demo',
  regime: 'opt-in',
  policyVersion: '2026-01',
  categories: { necessary: true, preferences: true, analytics: false, marketing: false },
  method: 'custom',
  gpc: false,
  at: '2026-01-15T09:30:00.000Z',
};

function PreferencesDemo({
  regime = 'opt-in',
  saved = false,
}: {
  regime?: ConsentRegime;
  saved?: boolean;
}) {
  const [storage] = useState(() => memoryStorage(saved ? { ...SAVED, regime } : null));
  return (
    <ConsentProvider policyVersion="2026-01" regime={regime} storage={storage}>
      <footer>
        <ConsentSettingsLink />
      </footer>
      <ConsentPreferences policyHref="#cookie-policy" />
    </ConsentProvider>
  );
}

export default {
  id: 'consent-preferences',
  title: 'ConsentPreferences',
  summary:
    'Cookie settings dialog: one switch per category, Reject all / Accept all / Save choices, and a footer "Cookie settings" button.',
  sections: {
    purpose: {
      summary:
        'Lets people choose category by category which optional storage the product may use, and change or withdraw that choice at any time. It reads and saves through `ConsentProvider`; `ConsentSettingsLink` is the small footer button that opens it. The component implements an interaction pattern; your legal team decides the categories, the regime for each country and the wording.',
      useWhen: [
        'Behind the cookie banner’s "Customize" button.',
        'From a permanent "Cookie settings" entry in the footer or account settings.',
      ],
      avoidWhen: [
        'The single "Do not sell or share" choice in opt-out regions: use PrivacyChoicesLink.',
        'General app settings that are not about consent: use SettingsSection with Switch.',
      ],
    },
    designPrinciples: {
      items: [
        'Nothing optional is on until the person turns it on (in opt-in regions); strictly necessary is always on and says so in words, not just with a disabled switch.',
        '"Reject all" and "Accept all" share the same variant and size; "Save choices" applies the switches as they are.',
        'A Global Privacy Control signal wins: sale-or-sharing categories are off and locked, with a note saying why.',
        'Choices can be withdrawn as easily as they were given.',
        'Each opening starts from the saved choice, so closing without saving changes nothing.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`ConsentPreferences` opens with the provider’s shared `preferencesOpen`, so the banner and `ConsentSettingsLink` can open it. Pass `open` and `onOpenChange` to control it yourself.',
        '`ConsentSettingsLink` is a `<button type="button">` styled as a link; it needs no `href`.',
        'Other DialogContent props (`size`, `className`, `data-*`) pass through to the dialog.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Categories come from the provider’s `categories` (label, description, `required`, `saleOrSharing`).',
        '`messages` overrides every visible string for translation.',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No visual variants. The rows follow the provider’s categories and the Global Privacy Control signal.',
      ],
    },
    states: {
      items: [
        {
          name: 'Always on',
          attribute: 'data-required (row)',
          description:
            'A required category: checked, disabled switch with "Always on" text, linked as its description.',
        },
        {
          name: 'Locked by GPC',
          attribute: 'data-locked (row), data-gpc (dialog)',
          description:
            'A sale-or-sharing category while a Global Privacy Control signal is on: off, disabled, with a note.',
        },
        {
          name: 'Saved choice',
          description:
            'When a record exists: its policy version and when it was saved, and a "Withdraw consent" button.',
        },
      ],
    },
    interaction: {
      items: [
        'Switches change a draft; nothing is saved until a footer button is pressed.',
        'Reject all, Accept all and Save choices save a record (`onRecord` on the provider) and close.',
        'Withdraw consent resets to the regime’s defaults, records `withdrawn`, closes, and the banner asks again.',
        'Escape, the close button or an outside click close without saving.',
        'Focus returns to whatever opened it (the footer button) when that is still on the page.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through the switches and buttons; focus stays inside the dialog.',
        },
        { keys: 'Space', action: 'Toggles the focused switch.' },
        { keys: 'Enter / Space (button)', action: 'Activates the focused button.' },
        { keys: 'Escape', action: 'Closes without saving.' },
      ],
    },
    accessibility: {
      role: 'dialog (aria-modal)',
      name: '`messages.title` ("Cookie settings"); `messages.description` is the description.',
      items: [
        'Each switch is labelled by its category label and described by its description, plus "Always on" or the GPC note.',
        'Locked state is conveyed in text, not only by colour or a disabled switch.',
        'The time of the saved choice is a `<time datetime>` element.',
        '`ConsentSettingsLink` is a real button with `aria-haspopup="dialog"`.',
      ],
    },
    responsive: {
      items: [
        'Below 640px the three main buttons share the footer width; Withdraw consent sits above them.',
        'Long category descriptions wrap; the switch stays aligned to the top of its row.',
        'Buttons and link-style buttons are at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Dialog surface tokens; subtle borders between rows; link colour for the link-style buttons.',
        'Light, dark and more-contrast themes apply automatically; forced colours use system colours.',
      ],
    },
    examples: {
      items: [
        { title: 'First visit (opt-in)', render: () => <PreferencesDemo /> },
        { title: 'With a saved choice', render: () => <PreferencesDemo saved /> },
        { title: 'Opt-out region', render: () => <PreferencesDemo regime="opt-out" /> },
      ],
    },
    doDont: {
      do: [
        'Mount one ConsentPreferences inside ConsentProvider, next to the banner.',
        'Put ConsentSettingsLink in the footer of every page so people can change their mind.',
        'Write category descriptions that say what the storage is for, in plain words.',
      ],
      dont: [
        'Do not pre-tick optional categories in opt-in regions.',
        'Do not hide Reject all or make it smaller than Accept all.',
        'Do not let a saved choice override a Global Privacy Control signal for sale or sharing.',
        'Do not tell people the dialog alone meets a law; your legal team decides what is required.',
      ],
    },
    related: {
      items: [
        {
          id: 'consent-manager',
          reason: 'ConsentProvider and useConsent: the state the dialog reads and saves.',
        },
        { id: 'cookie-consent-banner', reason: 'The banner whose "Customize" opens it.' },
        {
          id: 'privacy-choices-link',
          reason: 'A single sale-or-sharing switch for opt-out regions.',
        },
        { id: 'switch', reason: 'The on/off control in each row.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'switch', 'link', 'relative-time', 'dialog', 'consent-manager'],
    },
    testing: {
      unit: [
        'Opens from ConsentSettingsLink (a plain button with no href); axe passes; Escape returns focus.',
        'Opt-in: optional categories start off; necessary is checked, disabled and described "Always on".',
        'Reject all and Accept all share data-variant and data-size; Save records a custom choice.',
        'Accept all and Reject all record and close; reopening shows the saved state.',
        'Shows the saved policy version and time; Withdraw consent records `withdrawn` and asks again.',
        'Withdraw is hidden without a record or with allowWithdraw={false}.',
        'GPC (stubbed navigator.globalPrivacyControl) locks sale-or-sharing categories off with a note; Accept all keeps them off.',
        'Opens from the banner’s Customize; controlled open and onOpenChange.',
        'Renders on the server (closed).',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
