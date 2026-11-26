'use client';

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';

/**
 * How a jurisdiction treats optional cookies and similar tracking:
 * - `opt-in`: ask first; only strictly necessary storage until the person agrees.
 * - `opt-out`: optional storage may run; the person can refuse sale or sharing at any time.
 * - `notice`: tell the person what is used; no choice is required.
 */
export type ConsentRegime = 'opt-in' | 'opt-out' | 'notice';

/** One kind of optional storage the product uses. */
export interface ConsentCategory {
  /** Stable id, e.g. `analytics`. `necessary` is reserved for the always-on category. */
  id: string;
  /** Short name, e.g. "Analytics". */
  label: string;
  /** What it is used for, in one or two sentences. */
  description: string;
  /** Always on and cannot be switched off (strictly necessary storage). */
  required?: boolean;
  /**
   * Counts as selling or sharing personal information (advertising, cross-site
   * tracking). Switched off by a Global Privacy Control signal and by the
   * "Do not sell or share" choice.
   */
  saleOrSharing?: boolean;
  /**
   * Off until the person turns it on, in every jurisdiction (even where optional storage
   * otherwise runs by default). Use it for the most intrusive tools, such as session recording.
   * "Accept all" still turns it on.
   */
  askFirst?: boolean;
}

/** The categories most products need. Pass your own to change labels or add vendors' purposes. */
export const DEFAULT_CONSENT_CATEGORIES: readonly ConsentCategory[] = [
  {
    id: 'necessary',
    label: 'Strictly necessary',
    description: 'Keeps you signed in, remembers your security settings and makes the site work.',
    required: true,
  },
  {
    id: 'preferences',
    label: 'Preferences',
    description: 'Remembers choices such as language and theme.',
  },
  {
    id: 'analytics',
    label: 'Analytics',
    description: 'Counts visits and measures how features are used, so we can improve them.',
  },
  {
    id: 'marketing',
    label: 'Marketing',
    description: 'Measures campaigns and shows relevant ads on other sites.',
    saleOrSharing: true,
  },
];

/** Where the visitor is. Codes are ISO 3166-1 alpha-2 (`DE`) and ISO 3166-2 (`US-CA`). */
export interface ConsentJurisdiction {
  country?: string | null;
  region?: string | null;
}

/** Which regime applies where. Region entries win over country entries. */
export interface ConsentRegimeMap {
  countries: Readonly<Record<string, ConsentRegime>>;
  regions: Readonly<Record<string, ConsentRegime>>;
  /** Used when the jurisdiction is unknown or not listed. */
  fallback: ConsentRegime;
}

const OPT_IN_COUNTRIES = [
  // European Economic Area
  'AT',
  'BE',
  'BG',
  'HR',
  'CY',
  'CZ',
  'DK',
  'EE',
  'FI',
  'FR',
  'DE',
  'GR',
  'HU',
  'IE',
  'IT',
  'LV',
  'LT',
  'LU',
  'MT',
  'NL',
  'PL',
  'PT',
  'RO',
  'SK',
  'SI',
  'ES',
  'SE',
  'IS',
  'LI',
  'NO',
  // Others that ask before optional tracking
  'GB',
  'CH',
  'BR',
  'IN',
  'ZA',
  'KR',
  'TR',
];
const OPT_OUT_REGIONS = [
  'US-CA',
  'US-CO',
  'US-CT',
  'US-VA',
  'US-UT',
  'US-TX',
  'US-OR',
  'US-MT',
  'US-IA',
  'US-DE',
  'US-NH',
  'US-NJ',
  'US-NE',
  'US-TN',
  'US-MN',
  'US-MD',
  'US-IN',
  'US-KY',
  'US-RI',
];

/**
 * A starting map, not legal advice: opt-in for the EEA, the UK, Switzerland and
 * several other countries; opt-out for US states with a consumer privacy law;
 * Quebec opt-in; `opt-in` when the location is unknown, the most protective
 * choice. Have your legal team review it and pass your own `regimes`.
 */
export const DEFAULT_CONSENT_REGIMES: ConsentRegimeMap = {
  countries: {
    ...Object.fromEntries(OPT_IN_COUNTRIES.map((c) => [c, 'opt-in' as const])),
    US: 'notice',
  },
  regions: {
    ...Object.fromEntries(OPT_OUT_REGIONS.map((r) => [r, 'opt-out' as const])),
    'CA-QC': 'opt-in',
  },
  fallback: 'opt-in',
};

/** The regime for a jurisdiction: the region's entry, else the country's, else the fallback. */
export function resolveConsentRegime(
  jurisdiction: ConsentJurisdiction | null | undefined,
  map: ConsentRegimeMap = DEFAULT_CONSENT_REGIMES,
): ConsentRegime {
  const region = jurisdiction?.region?.toUpperCase();
  const country = (jurisdiction?.country ?? region?.split('-')[0])?.toUpperCase();
  if (region && map.regions[region]) return map.regions[region];
  if (country && map.countries[country]) return map.countries[country];
  return map.fallback;
}

/** How a decision was made. */
export type ConsentMethod =
  'accept-all' | 'reject-all' | 'custom' | 'gpc' | 'acknowledged' | 'withdrawn';

/** Proof of one consent decision, for the product to store. */
export interface ConsentRecord {
  /** Random id of this decision. */
  id: string;
  regime: ConsentRegime;
  /** The `policyVersion` the person saw. */
  policyVersion: string;
  /** Every category id with whether it is allowed. */
  categories: Record<string, boolean>;
  method: ConsentMethod;
  /** Whether a Global Privacy Control signal was present. */
  gpc: boolean;
  /** ISO 8601 time of the decision. */
  at: string;
}

/**
 * How long a choice is remembered before the banner asks again, in days. A refusal is not
 * re-asked for a long time (regulators treat repeated prompts as pressure), and an acceptance
 * should not outlive about a year.
 */
export interface ConsentExpiry {
  /** After accepting (accept all, a custom choice with anything optional on, or acknowledging). Default 365. */
  accepted?: number | undefined;
  /** After refusing (reject all, or a custom choice with every optional category off). Default 180. */
  declined?: number | undefined;
}

const DAY = 86_400_000;

/** Whether a stored choice is old enough that the person should be asked again. */
function isExpired(
  record: ConsentRecord,
  categories: readonly ConsentCategory[],
  expiry: ConsentExpiry | undefined,
): boolean {
  // A Global Privacy Control signal is the person's standing choice; nothing to re-ask.
  if (record.method === 'gpc') return false;
  const accepted =
    record.method === 'acknowledged' ||
    categories.some((c) => !c.required && record.categories[c.id] === true);
  const days = accepted ? (expiry?.accepted ?? 365) : (expiry?.declined ?? 180);
  const at = Date.parse(record.at);
  return Number.isFinite(at) && Date.now() - at > days * DAY;
}

/**
 * Whether a stored choice still counts under the rule that applies now. The same rule always counts. Beyond
 * that, a choice carries over only when it can never switch something on that the person did not ask for:
 * a choice made when consent was asked first (`opt-in`) was explicit, so it holds under any rule, and a
 * refusal holds under any rule. An acceptance that was only acknowledged under a lighter rule does not hold
 * where consent must be asked for, so the person is asked.
 */
function holdsUnder(
  record: ConsentRecord,
  regime: ConsentRegime,
  categories: readonly ConsentCategory[],
): boolean {
  if (record.regime === regime || record.regime === 'opt-in') return true;
  return !categories.some((c) => !c.required && record.categories[c.id] === true);
}

/** Reads and writes the latest record. The default uses localStorage. */
export interface ConsentStorage {
  read(): ConsentRecord | null;
  write(record: ConsentRecord | null): void;
}

function parseRecord(text: string | null | undefined): ConsentRecord | null {
  if (!text) return null;
  try {
    const value = JSON.parse(text) as Partial<ConsentRecord>;
    return value && typeof value.policyVersion === 'string' && value.categories
      ? (value as ConsentRecord)
      : null;
  } catch {
    return null;
  }
}

/** Keeps the record in localStorage under `key`. Fails quietly where storage is blocked. */
export function localStorageConsentStorage(key = 'mn-consent'): ConsentStorage {
  return {
    read() {
      try {
        return parseRecord(window.localStorage.getItem(key));
      } catch {
        return null;
      }
    },
    write(record) {
      try {
        if (record) window.localStorage.setItem(key, JSON.stringify(record));
        else window.localStorage.removeItem(key);
      } catch {
        // Storage blocked: the choice lasts for this page view only.
      }
    },
  };
}

/**
 * Keeps the record in a first-party cookie, so the server can read it too and
 * skip loading scripts the person refused.
 */
export function cookieConsentStorage(
  name = 'mn_consent',
  options: { maxAgeDays?: number; domain?: string; path?: string } = {},
): ConsentStorage {
  const { maxAgeDays = 365, domain, path = '/' } = options;
  const attrs = () =>
    [
      `path=${path}`,
      domain ? `domain=${domain}` : '',
      'SameSite=Lax',
      location.protocol === 'https:' ? 'Secure' : '',
    ]
      .filter(Boolean)
      .join('; ');
  return {
    read() {
      try {
        const raw = document.cookie
          .split('; ')
          .find((c) => c.startsWith(`${name}=`))
          ?.slice(name.length + 1);
        return parseRecord(raw ? decodeURIComponent(raw) : null);
      } catch {
        return null;
      }
    },
    write(record) {
      try {
        document.cookie = record
          ? `${name}=${encodeURIComponent(JSON.stringify(record))}; max-age=${maxAgeDays * 86400}; ${attrs()}`
          : `${name}=; max-age=0; ${attrs()}`;
      } catch {
        // Cookies blocked: the choice lasts for this page view only.
      }
    },
  };
}

/**
 * Keeps the latest record in step with the person's account, so a choice made
 * on one device or site applies on the others once they sign in. Both calls may
 * fail (signed out, offline); the local record still applies.
 */
export interface ConsentSync {
  /** The account's latest record, or null when there is none or the person is signed out. */
  load(): Promise<ConsentRecord | null>;
  /** Stores a new record on the account. */
  save(record: ConsentRecord): Promise<void>;
}

const time = (record: ConsentRecord | null | undefined) => {
  const t = record ? Date.parse(record.at) : NaN;
  return Number.isNaN(t) ? 0 : t;
};

/** Whether the browser sends a Global Privacy Control signal. False on the server. */
export function readGlobalPrivacyControl(): boolean {
  if (typeof navigator === 'undefined') return false;
  return (
    (navigator as Navigator & { globalPrivacyControl?: boolean }).globalPrivacyControl === true
  );
}

function newId(): string {
  try {
    return crypto.randomUUID();
  } catch {
    return `c-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
  }
}

/**
 * - `loading`: on the server and before the stored choice has been read. Render no banner.
 * - `pending`: a choice or acknowledgement is needed; show the banner.
 * - `decided`: the person has chosen (or acknowledged) for this policy version.
 */
export type ConsentStatus = 'loading' | 'pending' | 'decided';

export interface ConsentContextValue {
  status: ConsentStatus;
  regime: ConsentRegime;
  categories: readonly ConsentCategory[];
  /** Current allow/deny per category id. Required categories are always `true`. */
  choices: Record<string, boolean>;
  /** Whether a category is allowed now. Unknown ids are not allowed. */
  allowed: (id: string) => boolean;
  /** A Global Privacy Control signal is present and honoured. */
  gpc: boolean;
  /** The latest record, if any. */
  record: ConsentRecord | null;
  policyVersion: string;
  acceptAll: () => void;
  rejectAll: () => void;
  /** Saves a custom choice. Required categories stay on; GPC keeps sale-or-sharing off. */
  save: (choices: Record<string, boolean>) => void;
  /** Dismisses a `notice` or `opt-out` banner without changing choices. */
  acknowledge: () => void;
  /** Turns off every sale-or-sharing category ("Do not sell or share"). */
  optOutOfSaleOrSharing: () => void;
  /** Withdraws consent: back to the regime's defaults and asks again. */
  withdraw: () => void;
  /** Whether the preferences dialog is open (shared so a footer link can open it). */
  preferencesOpen: boolean;
  setPreferencesOpen: (open: boolean) => void;
}

const ConsentContext = createContext<ConsentContextValue | null>(null);

export interface ConsentProviderProps {
  /** Version of the cookie or privacy policy. A new version asks everyone again. */
  policyVersion: string;
  /** Where the visitor is, usually from a geo header on the server. */
  jurisdiction?: ConsentJurisdiction;
  /** Forces a regime, ignoring `jurisdiction`. */
  regime?: ConsentRegime;
  /**
   * The visitor's location is still being looked up. While `true`, nothing is decided: the status
   * stays `loading`, no banner shows and no optional category is allowed. Set it to `false` once
   * `jurisdiction` is known (or known to be unavailable, which picks the fallback regime).
   */
  locating?: boolean;
  /** How long each kind of choice is remembered before asking again. Default 365 days accepted, 180 refused. */
  expiry?: ConsentExpiry;
  /** Your jurisdiction → regime map. Default `DEFAULT_CONSENT_REGIMES`. */
  regimes?: ConsentRegimeMap;
  /** The categories the product uses. Default `DEFAULT_CONSENT_CATEGORIES`. */
  categories?: readonly ConsentCategory[];
  /** Where the record is kept. Default `localStorageConsentStorage()`. */
  storage?: ConsentStorage;
  /** Honour the Global Privacy Control signal. Default `true`. */
  honorGlobalPrivacyControl?: boolean;
  /** Called with every new record: store it as proof of consent. */
  onRecord?: (record: ConsentRecord) => void;
  /**
   * Syncs the record with the person's account. After the local record is read,
   * a newer account record for the same policy version and regime replaces it;
   * a newer local record is sent up. Every new decision is saved.
   */
  sync?: ConsentSync;
  /** Called whenever the allowed categories change, e.g. to load or unload scripts. */
  onChange?: (choices: Record<string, boolean>) => void;
  children: ReactNode;
}

function defaults(
  regime: ConsentRegime,
  categories: readonly ConsentCategory[],
  gpc: boolean,
): Record<string, boolean> {
  return Object.fromEntries(
    categories.map((c) => [
      c.id,
      c.required ? true : c.askFirst || (gpc && c.saleOrSharing) ? false : regime !== 'opt-in',
    ]),
  );
}

/**
 * Holds the visitor's consent for optional storage, adapted to their
 * jurisdiction: opt-in, opt-out or notice. Reads the stored record after
 * hydration (so the server and first client render agree), honours Global
 * Privacy Control, versions every decision and reports it through `onRecord`.
 * Mount once near the root; the banner, preferences and gates read it.
 */
export function ConsentProvider({
  policyVersion,
  jurisdiction,
  regime: regimeProp,
  locating = false,
  expiry,
  regimes = DEFAULT_CONSENT_REGIMES,
  categories: categoriesProp = DEFAULT_CONSENT_CATEGORIES,
  storage,
  honorGlobalPrivacyControl = true,
  onRecord,
  onChange,
  sync,
  children,
}: ConsentProviderProps) {
  const regime = regimeProp ?? resolveConsentRegime(jurisdiction, regimes);
  // An equal list written inline (`categories={[…]}`) is a new array on every render of the
  // parent. Keep the first one, so the stored choice is not read, reported through `onChange`
  // and fetched from `sync` again each time.
  const categoriesKey = JSON.stringify(categoriesProp);
  // eslint-disable-next-line react-hooks/exhaustive-deps -- compared by content, through the key
  const categories = useMemo(() => categoriesProp, [categoriesKey]);
  const acceptedDays = expiry?.accepted;
  const declinedDays = expiry?.declined;
  const store = useMemo(() => storage ?? localStorageConsentStorage(), [storage]);
  const [status, setStatus] = useState<ConsentStatus>('loading');
  const [gpc, setGpc] = useState(false);
  const [record, setRecord] = useState<ConsentRecord | null>(null);
  const [choices, setChoices] = useState<Record<string, boolean>>(() =>
    defaults(regime, categories, false),
  );
  const [preferencesOpen, setPreferencesOpen] = useState(false);
  const callbacks = useRef({ onRecord, onChange, sync });
  callbacks.current = { onRecord, onChange, sync };

  const normalize = useCallback(
    (next: Record<string, boolean>, signal: boolean) =>
      Object.fromEntries(
        categories.map((c) => [
          c.id,
          c.required ? true : signal && c.saleOrSharing ? false : next[c.id] === true,
        ]),
      ),
    [categories],
  );

  const commit = useCallback(
    (next: Record<string, boolean>, method: ConsentMethod, signal: boolean) => {
      const entry: ConsentRecord = {
        id: newId(),
        regime,
        policyVersion,
        categories: normalize(next, signal),
        method,
        gpc: signal,
        at: new Date().toISOString(),
      };
      store.write(entry);
      setRecord(entry);
      setChoices(entry.categories);
      setStatus('decided');
      callbacks.current.onRecord?.(entry);
      callbacks.current.onChange?.(entry.categories);
      callbacks.current.sync?.save(entry).catch(() => undefined);
    },
    [regime, policyVersion, normalize, store],
  );

  // Read the stored choice and the GPC signal after hydration.
  useEffect(() => {
    if (locating) {
      setStatus('loading');
      return;
    }
    const signal = honorGlobalPrivacyControl && readGlobalPrivacyControl();
    setGpc(signal);
    const matches = (r: ConsentRecord | null | undefined): r is ConsentRecord =>
      !!r &&
      r.policyVersion === policyVersion &&
      holdsUnder(r, regime, categories) &&
      !isExpired(r, categories, { accepted: acceptedDays, declined: declinedDays });
    const apply = (r: ConsentRecord) => {
      const applied = normalize(r.categories, signal);
      setRecord(r);
      setChoices(applied);
      setStatus('decided');
      callbacks.current.onChange?.(applied);
    };

    const stored = store.read();
    let local: ConsentRecord | null = null;
    if (matches(stored)) {
      local = stored;
      apply(stored);
    } else {
      const base = defaults(regime, categories, signal);
      setRecord(null);
      setChoices(base);
      if (regime === 'opt-out' && signal) {
        // The signal is the person's choice: record it and do not ask again.
        commit(base, 'gpc', signal);
        return;
      }
      setStatus('pending');
      callbacks.current.onChange?.(base);
    }

    // Then reconcile with the account: the newer record wins.
    const remoteSync = callbacks.current.sync;
    if (!remoteSync) return;
    let cancelled = false;
    remoteSync
      .load()
      .then((remote) => {
        if (cancelled) return;
        const usable = matches(remote) ? remote : null;
        if (usable && time(usable) > time(local)) {
          store.write(usable);
          apply(usable);
        } else if (local && (!usable || usable.id !== local.id)) {
          remoteSync.save(local).catch(() => undefined);
        }
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [
    store,
    policyVersion,
    regime,
    categories,
    honorGlobalPrivacyControl,
    normalize,
    commit,
    locating,
    acceptedDays,
    declinedDays,
  ]);

  const value = useMemo<ConsentContextValue>(() => {
    const all = (on: boolean) => Object.fromEntries(categories.map((c) => [c.id, on]));
    return {
      status,
      regime,
      categories,
      choices,
      allowed: (id) => choices[id] === true,
      gpc,
      record,
      policyVersion,
      acceptAll: () => commit(all(true), 'accept-all', gpc),
      rejectAll: () => commit(all(false), 'reject-all', gpc),
      save: (next) => commit({ ...choices, ...next }, 'custom', gpc),
      acknowledge: () => commit(choices, 'acknowledged', gpc),
      optOutOfSaleOrSharing: () =>
        commit(
          Object.fromEntries(
            categories.map((c) => [c.id, c.saleOrSharing ? false : choices[c.id] === true]),
          ),
          'custom',
          gpc,
        ),
      withdraw: () => {
        const base = defaults(regime, categories, gpc);
        commit(base, 'withdrawn', gpc);
        if (regime !== 'notice') setStatus('pending');
      },
      preferencesOpen,
      setPreferencesOpen,
    };
  }, [status, regime, categories, choices, gpc, record, policyVersion, commit, preferencesOpen]);

  return <ConsentContext.Provider value={value}>{children}</ConsentContext.Provider>;
}

/** The consent state and actions. Throws outside a ConsentProvider. */
export function useConsent(): ConsentContextValue {
  const value = useContext(ConsentContext);
  if (!value) throw new Error('useConsent must be used inside a <ConsentProvider>.');
  return value;
}

/** Like `useConsent`, but returns null outside a provider. */
export function useOptionalConsent(): ConsentContextValue | null {
  return useContext(ConsentContext);
}

export interface ConsentGateProps {
  /** The category that must be allowed, e.g. `analytics`. */
  category: string;
  /** Rendered only when the category is allowed (never on the server or while loading). */
  children: ReactNode;
  /** Rendered otherwise, e.g. a placeholder with a button that opens preferences. */
  fallback?: ReactNode;
}

/**
 * Renders its children only when a consent category is allowed: wrap analytics
 * scripts, embeds that set cookies and ad pixels. Nothing renders on the server
 * or before the stored choice is read, so nothing loads early.
 */
export function ConsentGate({ category, children, fallback = null }: ConsentGateProps) {
  const consent = useConsent();
  if (consent.status === 'loading') return null;
  return <>{consent.allowed(category) ? children : fallback}</>;
}

type AnalyticsWindow = Window & {
  __mnConsentDefault?: boolean;
  dataLayer?: unknown[];
  gtag?: (...args: unknown[]) => void;
  clarity?: ((...args: unknown[]) => void) & { q?: unknown[] };
};

/** Cookies Google Analytics and Microsoft Clarity set, removed when consent is withdrawn. */
const GOOGLE_COOKIE = /^(_ga|_gid|_gat|_gcl_)/;
const CLARITY_COOKIE = /^(_clck|_clsk|CLID$|ANONCHK$|MR$|MUID$|SM$)/;

function loadScript(id: string, src: string) {
  if (document.getElementById(id)) return;
  const el = document.createElement('script');
  el.id = id;
  el.async = true;
  el.src = src;
  document.head.appendChild(el);
}

function ensureGtag(w: AnalyticsWindow) {
  const layer = (w.dataLayer = w.dataLayer || []);
  if (!w.gtag) {
    w.gtag = function gtag() {
      // gtag.js only reads real `arguments` objects, not arrays.
      // eslint-disable-next-line prefer-rest-params
      layer.push(arguments);
    };
  }
  return { gtag: w.gtag, layer };
}

function clearCookies(match: RegExp) {
  const parts = location.hostname.split('.');
  const domains = [''];
  for (let i = 0; i < parts.length - 1; i++) domains.push(`; domain=.${parts.slice(i).join('.')}`);
  for (const pair of document.cookie.split('; ')) {
    const name = pair.split('=')[0];
    if (!name || !match.test(name)) continue;
    for (const d of domains) document.cookie = `${name}=; max-age=0; path=/${d}`;
  }
}

export interface ConsentAnalyticsProps {
  /**
   * Google tag id: a GA4 measurement id (`G-…`) loads gtag.js, a Tag Manager
   * container (`GTM-…`) loads gtm.js.
   */
  googleTagId?: string;
  /** Microsoft Clarity project id. */
  clarityId?: string;
  /** The category that must be allowed for Google. Default `analytics`. */
  category?: string;
  /** The category that must be allowed for Microsoft Clarity. Default: the same as `category`. */
  clarityCategory?: string;
  /** The category that allows advertising storage, if the product has one. Default `marketing`. */
  adsCategory?: string;
}

/**
 * Loads Google Analytics (or Google Tag Manager) and Microsoft Clarity only
 * once the analytics category is allowed. Google Consent Mode v2 starts with
 * every signal denied and is updated with the person's choice; Clarity gets its
 * consent signal too. Nothing is requested from either service before then.
 * When consent is withdrawn, both are told and their cookies are removed.
 * Render once inside ConsentProvider; it renders nothing.
 */
export function ConsentAnalytics({
  googleTagId,
  clarityId,
  category = 'analytics',
  clarityCategory = category,
  adsCategory = 'marketing',
}: ConsentAnalyticsProps) {
  const consent = useConsent();
  const ready = consent.status !== 'loading';
  const analytics = ready && consent.allowed(category);
  const clarity = ready && consent.allowed(clarityCategory);
  const ads = ready && consent.allowed(adsCategory);

  useEffect(() => {
    if (!ready) return;
    const w = window as AnalyticsWindow;

    if (googleTagId) {
      const { gtag, layer } = ensureGtag(w);
      if (!w.__mnConsentDefault) {
        w.__mnConsentDefault = true;
        gtag('consent', 'default', {
          ad_storage: 'denied',
          ad_user_data: 'denied',
          ad_personalization: 'denied',
          analytics_storage: 'denied',
        });
      }
      gtag('consent', 'update', {
        analytics_storage: analytics ? 'granted' : 'denied',
        ad_storage: ads ? 'granted' : 'denied',
        ad_user_data: ads ? 'granted' : 'denied',
        ad_personalization: ads ? 'granted' : 'denied',
      });
      if (analytics && !document.getElementById('mn-consent-google')) {
        if (googleTagId.startsWith('GTM-')) {
          layer.push({ 'gtm.start': Date.now(), event: 'gtm.js' });
          loadScript(
            'mn-consent-google',
            `https://www.googletagmanager.com/gtm.js?id=${encodeURIComponent(googleTagId)}`,
          );
        } else {
          gtag('js', new Date());
          gtag('config', googleTagId);
          loadScript(
            'mn-consent-google',
            `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(googleTagId)}`,
          );
        }
      }
    }

    if (clarityId) {
      if (clarity && !w.clarity) {
        const q: unknown[] = [];
        w.clarity = Object.assign((...args: unknown[]) => void q.push(args), { q });
        loadScript(
          'mn-consent-clarity',
          `https://www.clarity.ms/tag/${encodeURIComponent(clarityId)}`,
        );
      }
      w.clarity?.('consentv2', {
        ad_Storage: ads ? 'granted' : 'denied',
        analytics_Storage: clarity ? 'granted' : 'denied',
      });
    }

    if (!analytics) clearCookies(GOOGLE_COOKIE);
    if (!clarity) clearCookies(CLARITY_COOKIE);
  }, [ready, analytics, clarity, ads, googleTagId, clarityId]);

  return null;
}
