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
  const { maxAgeDays = 180, domain, path = '/' } = options;
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
      c.required ? true : gpc && c.saleOrSharing ? false : regime !== 'opt-in',
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
  regimes = DEFAULT_CONSENT_REGIMES,
  categories = DEFAULT_CONSENT_CATEGORIES,
  storage,
  honorGlobalPrivacyControl = true,
  onRecord,
  onChange,
  children,
}: ConsentProviderProps) {
  const regime = regimeProp ?? resolveConsentRegime(jurisdiction, regimes);
  const store = useMemo(() => storage ?? localStorageConsentStorage(), [storage]);
  const [status, setStatus] = useState<ConsentStatus>('loading');
  const [gpc, setGpc] = useState(false);
  const [record, setRecord] = useState<ConsentRecord | null>(null);
  const [choices, setChoices] = useState<Record<string, boolean>>(() =>
    defaults(regime, categories, false),
  );
  const [preferencesOpen, setPreferencesOpen] = useState(false);
  const callbacks = useRef({ onRecord, onChange });
  callbacks.current = { onRecord, onChange };

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
    },
    [regime, policyVersion, normalize, store],
  );

  // Read the stored choice and the GPC signal after hydration.
  useEffect(() => {
    const signal = honorGlobalPrivacyControl && readGlobalPrivacyControl();
    setGpc(signal);
    const stored = store.read();
    if (stored && stored.policyVersion === policyVersion && stored.regime === regime) {
      const applied = normalize(stored.categories, signal);
      setRecord(stored);
      setChoices(applied);
      setStatus('decided');
      callbacks.current.onChange?.(applied);
      return;
    }
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
  }, [store, policyVersion, regime, categories, honorGlobalPrivacyControl, normalize, commit]);

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
