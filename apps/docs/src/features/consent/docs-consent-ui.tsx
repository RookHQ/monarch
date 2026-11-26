import { ConsentAnalytics, ConsentPreferences, CookieConsentBanner } from '@rookcom/monarch-react';

/**
 * The consent UI and the tag loader, split from docs-consent.tsx so they are not
 * in any page's initial JS: they load after hydration, and only on builds with
 * analytics IDs.
 */
export function DocsConsentUi({
  googleTagId,
  clarityId,
  policyHref,
}: {
  googleTagId: string;
  clarityId: string;
  policyHref: string;
}) {
  return (
    <>
      <ConsentAnalytics
        {...(googleTagId ? { googleTagId: googleTagId } : {})}
        {...(clarityId ? { clarityId: clarityId } : {})}
      />
      <CookieConsentBanner
        position="bottom-center"
        policyHref={policyHref}
        messages={{
          'opt-in': {
            title: 'Cookies',
            description:
              'We use essential cookies to run these docs. With your OK, we’d also use optional cookies to improve them.',
          },
          acceptAll: 'Accept all',
          rejectAll: 'Essential only',
          customize: 'Manage',
          policyLabel: 'Learn more',
        }}
      />
      <ConsentPreferences
        mode="choice"
        policyHref={policyHref}
        messages={{
          description:
            'Essential cookies are always on. Optional cookies are used only if you accept them.',
          acceptAll: 'Accept all',
          rejectAll: 'Essential only',
          policyLabel: 'Cookie Policy',
        }}
      />
    </>
  );
}
