import { Suspense, lazy, useEffect, useState, type ReactNode } from 'react';
import {
  ConsentProvider,
  cookieConsentStorage,
  useOptionalConsent,
  type ConsentCategory,
} from '@rookcom/monarch-react';

/** Set at build time from DOCS_GTM_ID and DOCS_CLARITY_ID; empty in forks, local builds and CI. */
const GOOGLE_TAG_ID = __DOCS_GTM_ID__;
const CLARITY_ID = __DOCS_CLARITY_ID__;

/** Bump when the cookie policy changes in a way people should see again. */
const POLICY_VERSION = '2026-11-09.2';
const COOKIE_POLICY = 'https://rookhq.com/cookies';

const CATEGORIES: readonly ConsentCategory[] = [
  {
    id: 'necessary',
    label: 'Essential',
    description: 'Remember your theme and your cookie choice. The site needs these to work.',
    required: true,
  },
  {
    id: 'analytics',
    label: 'Analytics',
    description: 'Helps us understand which pages are read, so we can improve these docs.',
  },
];

// The banner, the preferences dialog and the tag loader are not needed for the first paint, so they load after
// hydration and stay out of every page's initial JS (scripts/docs/check-performance-budget.mjs).
const DocsConsentUi = lazy(() =>
  import('./docs-consent-ui.js').then((m) => ({ default: m.DocsConsentUi })),
);

const storage = typeof document === 'undefined' ? undefined : cookieConsentStorage('rook_consent');

/**
 * Cookie consent for the hosted docs site. With no analytics IDs configured
 * there is nothing optional to ask about, so nothing renders.
 */
export function DocsConsent({ children }: { children: ReactNode }) {
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  if (!GOOGLE_TAG_ID && !CLARITY_ID) return <>{children}</>;
  return (
    <ConsentProvider
      policyVersion={POLICY_VERSION}
      regime="opt-in"
      categories={CATEGORIES}
      {...(storage ? { storage } : {})}
    >
      {children}
      {mounted ? (
        <Suspense fallback={null}>
          <DocsConsentUi
            googleTagId={GOOGLE_TAG_ID}
            clarityId={CLARITY_ID}
            policyHref={COOKIE_POLICY}
          />
        </Suspense>
      ) : null}
    </ConsentProvider>
  );
}

/** A small cookie icon for the footer that opens Cookie settings; renders nothing when the site has no analytics. */
export function CookieSettingsIcon() {
  const consent = useOptionalConsent();
  if (!consent) return null;
  return (
    <button
      type="button"
      className="docs-footer__icon"
      aria-label="Cookie settings"
      title="Cookie settings"
      aria-haspopup="dialog"
      onClick={() => consent.setPreferencesOpen(true)}
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <path d="M12 2a10 10 0 1 0 10 10 4 4 0 0 1-5-5 4 4 0 0 1-5-5" />
        <path d="M8.5 8.5v.01" />
        <path d="M16 15.5v.01" />
        <path d="M12 12v.01" />
        <path d="M11 17v.01" />
        <path d="M7 14v.01" />
      </svg>
    </button>
  );
}
