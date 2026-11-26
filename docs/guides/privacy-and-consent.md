# Privacy and consent

How to add consent for cookies and tracking, privacy choices and privacy
requests to a product. These components implement interaction patterns; they
are not legal advice. Your legal team decides which regime applies where, the
categories, the vendors and the wording.

## 1. Mount the provider

Resolve where the visitor is on the server (most hosts send a country and
region header) and pass it in:

```tsx
<ConsentProvider
  policyVersion="2026-11"
  jurisdiction={{ country: geo.country, region: geo.region }}
  storage={cookieConsentStorage()}
  onRecord={(record) => saveConsentRecord(record)}
>
  <App />
  <CookieConsentBanner policyHref="/cookies" />
  <ConsentPreferences />
</ConsentProvider>
```

| Regime    | Default before a choice | Banner                                                     |
| --------- | ----------------------- | ---------------------------------------------------------- |
| `opt-in`  | Only strictly necessary | Reject all · Accept all (equal), Customize                 |
| `opt-out` | Optional categories on  | Notice, "Do not sell or share my personal information", OK |
| `notice`  | Everything on           | Notice, OK                                                 |

`DEFAULT_CONSENT_REGIMES` is a starting map (opt-in for the EEA, the UK,
Switzerland and others; opt-out for US states with a consumer privacy law;
opt-in when the location is unknown). Copy it, have it reviewed, and pass your
own `regimes`.

## 2. Gate every optional script

```tsx
<ConsentGate category="analytics">
  <AnalyticsScript />
</ConsentGate>
```

Nothing inside a gate renders on the server or before the stored choice is
read. With `cookieConsentStorage()` the server can read the record too and not
send the script at all.

## 3. Let people change their mind

- `ConsentSettingsLink` in the footer opens the preferences dialog.
- `PrivacyChoicesLink` shows "Your privacy choices" where the regime is opt-out.
- `withdraw()` resets to the defaults and asks again.
- Bump `policyVersion` when categories, vendors or purposes change; everyone is asked again.

## 4. Global Privacy Control

When the browser sends the signal, sale-or-sharing categories (`saleOrSharing: true`)
stay off whatever is chosen. In opt-out regimes the signal is recorded as the
decision (`method: 'gpc'`) and the banner does not appear. Turn it off only
after legal review (`honorGlobalPrivacyControl={false}`).

## 5. Records

Every decision calls `onRecord` with
`{ id, regime, policyVersion, categories, method, gpc, at }`. Store it with the
account or visitor id. `ConsentHistory` displays stored records.

## Sign-up, terms and requests

| Need                               | Component                               |
| ---------------------------------- | --------------------------------------- |
| Marketing opt-in on a form         | `MarketingConsentCheckbox`              |
| Agree to terms at sign-up          | `TermsAcceptance`                       |
| Terms changed since last agreement | `TermsUpdateDialog`                     |
| "Download your data" in settings   | `DataExportRequest`                     |
| Public privacy-request form        | `DataSubjectRequestForm` (`./patterns`) |
| Erase one person's data (admin)    | `GdprEraseFlow` (`./patterns`)          |
| Delete my account                  | `DeleteAccountFlow` (`./patterns`)      |

## Writing

- Say what is used and why in plain words; no "we value your privacy" filler.
- Name the buttons by what they do: "Accept all", "Reject all", "Save choices".
- Never describe the product as compliant with a law in the interface.
