import { useState } from 'react';
import {
  Button,
  ConsentPreferences,
  ConsentProvider,
  CookieConsentBanner,
  useConsent,
  type ConsentRecord,
  type ConsentRegime,
  type ConsentStorage,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** Keeps the demo's choice in memory, so reloading the page asks again. */
function memoryStorage(): ConsentStorage {
  let value: ConsentRecord | null = null;
  return {
    read: () => value,
    write: (record) => {
      value = record;
    },
  };
}

function ShowAgain() {
  const { status, withdraw } = useConsent();
  return status === 'decided' ? <Button onClick={withdraw}>Show the banner again</Button> : null;
}

function BannerDemo({
  regime,
  position = 'bottom',
}: {
  regime: ConsentRegime;
  position?: 'bottom' | 'bottom-start';
}) {
  const [storage] = useState(memoryStorage);
  return (
    <ConsentProvider policyVersion="2026-01" regime={regime} storage={storage}>
      <ShowAgain />
      {/* `position: relative` keeps the preview inside this box; in an app the banner is fixed. */}
      <CookieConsentBanner
        position={position}
        policyHref="#cookie-policy"
        style={{ position: 'relative' }}
      />
      <ConsentPreferences policyHref="#cookie-policy" />
    </ConsentProvider>
  );
}

export default {
  id: 'cookie-consent-banner',
  title: 'CookieConsentBanner',
  summary:
    'Regime-aware cookie banner: Accept all / Reject all / Customize, a "Do not sell or share" choice, or a notice.',
  sections: {
    purpose: {
      summary:
        'Tells visitors which cookies and similar storage the product uses and records their choice, with the layout the visitor’s regime calls for: ask first (opt-in), offer an opt-out of sale or sharing (opt-out), or inform (notice). It reads everything from `ConsentProvider`, so the regime, the Global Privacy Control signal and the record of each choice are handled in one place. The component implements an interaction pattern; your legal team decides the regime for each country and the wording.',
      useWhen: [
        'A site or app sets analytics, preference or advertising cookies and needs to ask or inform visitors.',
        'The same product serves visitors from places with different rules and should adapt per visitor.',
      ],
      avoidWhen: [
        'Marketing email consent at sign-up: use MarketingConsentCheckbox.',
        'Accepting terms of service: use TermsAcceptance or TermsUpdateDialog.',
        'Changing consent later: use ConsentSettingsLink (opens ConsentPreferences) in the footer.',
      ],
    },
    designPrinciples: {
      items: [
        'Never blocks the page: a labelled region at the bottom, no overlay, no focus trap, nothing hidden behind it.',
        'Parity where consent is asked: "Reject all" and "Accept all" always share the same variant and size (`choiceVariant` changes both), side by side.',
        'Dismissing is a choice: Escape and clicks elsewhere do not close the banner; only its buttons do.',
        'No surprises on load: nothing renders on the server or before the stored choice is read, so people who already chose never see a flash of the banner.',
        'Global Privacy Control is honoured and said so, not silently overridden.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Must be inside a `ConsentProvider`. Mount `ConsentPreferences` beside it so "Customize" has a dialog to open.',
        'Forwards its ref and extra HTML attributes to the region element.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`messages` overrides every string; per-regime text is `messages["opt-in" | "opt-out" | "notice"] = { title, description }`, each field optional.',
        '`policyLink` takes your own link element (e.g. a router link) and wins over `policyHref`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'regime (from ConsentProvider)',
          values: ['opt-in', 'opt-out', 'notice'],
          description:
            '`opt-in`: Customize, Reject all and Accept all. `opt-out`: "Do not sell or share my personal information" and OK. `notice`: OK only. Exposed as `data-regime`.',
        },
        {
          prop: 'position',
          values: ['bottom', 'bottom-start'],
          description:
            '`bottom` (default) is a full-width bar; `bottom-start` is a card in the bottom start corner. Exposed as `data-position`.',
        },
        {
          prop: 'choiceVariant',
          values: ['secondary', 'primary'],
          description: 'Button variant for both Accept all and Reject all.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Hidden',
          description:
            'On the server, while the stored choice is read, and once the person has chosen for the current policy version.',
        },
        {
          name: 'Shown',
          attribute: 'data-regime',
          description: 'The provider status is `pending`: no choice for this policy version yet.',
        },
        {
          name: 'Privacy signal honoured',
          attribute: 'data-gpc',
          description:
            'A Global Privacy Control signal is on. In the opt-out layout a note says it was honoured and the "Do not sell or share" button is not shown, because sale and sharing are already off.',
        },
      ],
    },
    interaction: {
      items: [
        'Accept all and Reject all save a record (`onRecord` on the provider) and hide the banner.',
        '"Do not sell or share" turns off every sale-or-sharing category; OK acknowledges without changing choices.',
        '"Customize" opens `ConsentPreferences` through the provider; the banner stays until a choice is saved.',
        'A new `policyVersion` on the provider shows the banner again.',
        'Withdrawing consent (from ConsentPreferences) shows it again.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action:
            'Reaches the policy link and buttons in page order; focus moves on out of the banner.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
        { keys: 'Escape', action: 'Does nothing: dismissing the banner is a choice.' },
      ],
      notes: ['The banner never takes focus when it appears.'],
    },
    accessibility: {
      role: 'region',
      name: 'The visible title (`aria-labelledby`); the description is the region’s description.',
      items: [
        'Announced once, politely, through a visually hidden status message (`messages.announcement`).',
        'Not modal: no `aria-modal`, no focus trap, no inert page.',
        'Button labels say what happens ("Reject all", not "No").',
        'Touch targets are at least 44px on coarse pointers.',
      ],
    },
    responsive: {
      items: [
        'Below 640px the actions take the full width; Reject all and Accept all share a row equally, Customize and the opt-out action sit on their own row.',
        'Respects safe-area insets on phones with a home indicator or notch.',
        'At 390px wide the long opt-out label wraps inside its button; nothing scrolls sideways.',
        'Tall content scrolls inside the banner (max 80% of the viewport height).',
      ],
    },
    theme: {
      items: [
        'Raised surface, default border and large shadow tokens; the GPC note uses the info border.',
        'Light, dark and more-contrast themes apply automatically; forced colours use system colours; the entrance animation is off with reduced motion.',
      ],
    },
    examples: {
      items: [
        { title: 'Opt-in (ask first)', stack: true, render: () => <BannerDemo regime="opt-in" /> },
        {
          title: 'Opt-out (sale or sharing)',
          stack: true,
          render: () => <BannerDemo regime="opt-out" />,
        },
        { title: 'Notice', stack: true, render: () => <BannerDemo regime="notice" /> },
        {
          title: 'Corner card',
          stack: true,
          render: () => <BannerDemo regime="opt-in" position="bottom-start" />,
        },
      ],
    },
    doDont: {
      do: [
        'Mount ConsentProvider once near the root, with the banner and ConsentPreferences inside it.',
        'Store every record from `onRecord` as proof of the choice.',
        'Bump `policyVersion` when the cookie policy changes in a way that needs a new choice.',
        'Have your legal team choose the regime map and review the wording in `messages`.',
        'Leave room at the bottom of the page (or accept the overlap) so the footer stays reachable.',
      ],
      dont: [
        'Do not make Accept all more prominent than Reject all.',
        'Do not load analytics or advertising scripts before the choice: wrap them in ConsentGate.',
        'Do not tell people the banner alone meets a law; it implements the interaction, and your legal team owns the rest.',
        'Do not hide the banner on Escape or on an outside click.',
      ],
    },
    related: {
      items: [
        {
          id: 'consent-manager',
          reason: 'ConsentProvider, useConsent and ConsentGate: the state the banner reads.',
        },
        {
          id: 'consent-preferences',
          reason: 'The settings dialog behind "Customize", and the footer settings button.',
        },
        {
          id: 'privacy-choices-link',
          reason: 'The footer "Your privacy choices" control for opt-out regions.',
        },
        { id: 'marketing-consent-checkbox', reason: 'Email marketing consent in forms.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'link', 'visually-hidden', 'consent-manager'],
    },
    testing: {
      unit: [
        'Opt-in: Reject all and Accept all share data-variant and data-size; choiceVariant changes both; axe passes.',
        'Opt-in: Reject all and Accept all produce records (id, regime, policyVersion, categories, ISO time), store them and hide the banner.',
        'Customize opens the shared preferences state.',
        'Does not take focus on appear, does not trap focus, and ignores Escape.',
        'Announces politely once.',
        'Opt-out: "Do not sell or share" turns off sale-or-sharing categories only; OK acknowledges.',
        'GPC (stubbed navigator.globalPrivacyControl): opt-out records the signal and shows no banner; after a withdrawal the banner says the signal was honoured.',
        'Notice: text and OK only; per-regime messages.',
        'A stored choice for the policy version hides the banner; a new version shows it.',
        'bottom-start position and a custom policy link node.',
        'Renders no banner on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
