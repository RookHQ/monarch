import { useMemo } from 'react';
import {
  Button,
  ConsentGate,
  ConsentProvider,
  useConsent,
  type ConsentRecord,
  type ConsentRegime,
  type ConsentStorage,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** Docs demos keep the choice in memory, so the page never stores anything. */
function useMemoryStorage(): ConsentStorage {
  return useMemo(() => {
    let value: ConsentRecord | null = null;
    return {
      read: () => value,
      write: (next) => {
        value = next;
      },
    };
  }, []);
}

function StateReadout() {
  const consent = useConsent();
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)' }}>
      <p style={{ margin: 0 }}>
        Regime: <strong>{consent.regime}</strong> · Status: <strong>{consent.status}</strong>
      </p>
      <ul style={{ margin: 0 }}>
        {consent.categories.map((c) => (
          <li key={c.id}>
            {c.label}: {consent.allowed(c.id) ? 'allowed' : 'not allowed'}
          </li>
        ))}
      </ul>
      <div style={{ display: 'flex', gap: 'var(--mn-space-2)', flexWrap: 'wrap' }}>
        <Button size="sm" onClick={consent.rejectAll}>
          Reject all
        </Button>
        <Button size="sm" onClick={consent.acceptAll}>
          Accept all
        </Button>
        <Button size="sm" variant="ghost" onClick={consent.withdraw}>
          Withdraw
        </Button>
      </div>
      <ConsentGate
        category="analytics"
        fallback={<p style={{ margin: 0 }}>The analytics script is not loaded.</p>}
      >
        <p style={{ margin: 0 }}>The analytics script would load here.</p>
      </ConsentGate>
    </div>
  );
}

function Demo({ regime }: { regime: ConsentRegime }) {
  const storage = useMemoryStorage();
  return (
    <ConsentProvider policyVersion="2026-11" regime={regime} storage={storage}>
      <StateReadout />
    </ConsentProvider>
  );
}

export default {
  id: 'consent-manager',
  title: 'ConsentProvider',
  summary:
    'Consent for optional cookies and tracking that adapts to the visitor’s jurisdiction: opt-in, opt-out or notice.',
  sections: {
    purpose: {
      summary:
        'Holds one visitor’s consent for optional storage (analytics, preferences, marketing), decides which rules apply from where they are, honours the Global Privacy Control signal, and records every decision so the product can keep proof. The cookie banner, preferences dialog, privacy-choices link and `ConsentGate` all read it.',
      useWhen: [
        'A product or marketing site sets cookies or loads scripts that are not strictly necessary.',
        'Scripts, embeds or pixels must wait until the person allows them (`ConsentGate`).',
      ],
      avoidWhen: [
        'Everything the site stores is strictly necessary (sign-in, security): no consent is needed, though a privacy notice still is.',
        'You need legal advice: this implements interaction patterns. Your legal team decides the regime per country, the categories and the wording.',
      ],
    },
    designPrinciples: {
      items: [
        'Ask before optional tracking where the rules require it; unknown location falls back to opt-in, the most protective choice.',
        'Strictly necessary is always on; nothing else is pre-allowed in an opt-in regime.',
        'A browser privacy signal is a choice, and is recorded as one.',
        'Every decision is versioned: a new policy version asks again.',
        'Decide nothing on the server, so the first render matches and nothing loads early.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`useConsent()` returns `{ status, regime, categories, choices, allowed(id), gpc, record, policyVersion, acceptAll, rejectAll, save, acknowledge, optOutOfSaleOrSharing, withdraw, preferencesOpen, setPreferencesOpen }`.',
        '`resolveConsentRegime(jurisdiction, map?)` is pure and can run on the server, e.g. from a geo header.',
        '`DEFAULT_CONSENT_REGIMES` and `DEFAULT_CONSENT_CATEGORIES` are starting points, not legal advice; pass your own.',
        '`localStorageConsentStorage(key?)` (default) and `cookieConsentStorage(name?, options?)` (readable by the server) implement `ConsentStorage`; any `{ read, write }` works.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'regime',
          values: ['opt-in', 'opt-out', 'notice'],
          description:
            '`opt-in`: only strictly necessary until the person agrees. `opt-out`: optional categories run; the person can refuse sale or sharing. `notice`: everything runs and the banner informs. Forced with `regime`, otherwise resolved from `jurisdiction` and `regimes`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'loading',
          description:
            'On the server and before the stored choice is read. Gates render nothing and the banner stays hidden.',
        },
        {
          name: 'pending',
          description: 'A choice or acknowledgement is needed for this policy version.',
        },
        {
          name: 'decided',
          description:
            'The person chose, acknowledged, or sent a Global Privacy Control signal in an opt-out regime.',
        },
      ],
      notes: ['The provider renders no markup, so these states have no attributes; read `status`.'],
    },
    interaction: {
      items: [
        'After hydration the provider reads the stored record. If it matches the policy version and regime it is applied; otherwise the regime’s defaults apply and the status is `pending`.',
        'Every action writes a new record, calls `onRecord` (store it as proof) and `onChange` (load or unload scripts).',
        '`withdraw()` returns to the defaults and asks again.',
        'With Global Privacy Control on, sale-or-sharing categories stay off whatever is chosen; in an opt-out regime the signal is recorded as the decision.',
      ],
    },
    keyboard: {
      items: [],
      notes: [
        'Not interactive: the provider and the gate render no controls. See the cookie banner and preferences for keyboard support.',
      ],
    },
    accessibility: {
      items: [
        'Renders no markup; the components that read it carry the accessible names and announcements.',
        '`ConsentGate` fallbacks should say what is missing and offer a way to open preferences, not leave an empty box.',
      ],
    },
    responsive: {
      items: ['Not visual; nothing changes with screen size.'],
    },
    theme: {
      items: ['Not visual; it has no styles of its own.'],
    },
    examples: {
      items: [
        { title: 'Opt-in', stack: true, render: () => <Demo regime="opt-in" /> },
        { title: 'Opt-out', stack: true, render: () => <Demo regime="opt-out" /> },
        { title: 'Notice', stack: true, render: () => <Demo regime="notice" /> },
      ],
    },
    doDont: {
      do: [
        'Resolve the jurisdiction on the server (geo header) and pass it in.',
        'Bump `policyVersion` whenever the categories, vendors or purposes change.',
        'Store every `onRecord` record with the account or visitor id.',
        'Wrap every optional script, embed and pixel in `ConsentGate`.',
      ],
      dont: [
        'Do not load optional scripts before `status` is `decided` or the gate allows it.',
        'Do not mark a category `required` unless the site cannot work without it.',
        'Do not describe the product as compliant with a law because it uses this component.',
      ],
    },
    related: {
      items: [
        { id: 'cookie-consent-banner', reason: 'The banner that asks for the choice.' },
        { id: 'consent-preferences', reason: 'Per-category choices and the footer settings link.' },
        {
          id: 'privacy-choices-link',
          reason: 'The “Do not sell or share” choice for opt-out regimes.',
        },
        { id: 'consent-history', reason: 'Shows the stored records.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
    },
    testing: {
      unit: [
        'resolveConsentRegime: region, then country, then fallback; custom maps.',
        'Opt-in: only necessary until a choice; accept all records the decision, stores it and calls onChange.',
        'Reject all and custom choices keep strictly necessary on.',
        'A stored decision is restored; a new policy version asks again.',
        'Opt-out: optional categories run; opting out of sale or sharing turns marketing off.',
        'Global Privacy Control: recorded as the choice in opt-out, keeps sale or sharing off, and can be ignored.',
        'Notice: acknowledging records it. Withdraw asks again.',
        'Server render: status loading, gated content absent.',
        'localStorage and cookie storage round-trip; blocked storage fails quietly.',
      ],
      axe: false,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
