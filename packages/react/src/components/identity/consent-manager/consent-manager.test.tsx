import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import {
  ConsentAnalytics,
  ConsentGate,
  ConsentProvider,
  DEFAULT_CONSENT_REGIMES,
  cookieConsentStorage,
  localStorageConsentStorage,
  readGlobalPrivacyControl,
  resolveConsentRegime,
  useConsent,
  type ConsentContextValue,
  type ConsentProviderProps,
  type ConsentRegime,
  type ConsentRecord,
  type ConsentStorage,
  type ConsentSync,
} from './index.js';

function memoryStorage(initial: ConsentRecord | null = null) {
  let value = initial;
  const storage: ConsentStorage & { value: () => ConsentRecord | null } = {
    read: () => value,
    write: (next) => {
      value = next;
    },
    value: () => value,
  };
  return storage;
}

function setGpc(on: boolean | undefined) {
  Object.defineProperty(navigator, 'globalPrivacyControl', {
    configurable: true,
    get: () => on,
  });
}

afterEach(() => setGpc(undefined));

function mount(props: Partial<ConsentProviderProps> = {}) {
  const ref: { current: ConsentContextValue | null } = { current: null };
  function Grab() {
    ref.current = useConsent();
    return <p>status: {ref.current.status}</p>;
  }
  const storage = (props.storage as ReturnType<typeof memoryStorage>) ?? memoryStorage();
  const utils = render(
    <ConsentProvider policyVersion="2026-11" storage={storage} {...props}>
      <Grab />
      <ConsentGate category="analytics" fallback={<span>analytics off</span>}>
        <span>analytics on</span>
      </ConsentGate>
    </ConsentProvider>,
  );
  return { ...utils, ctx: ref as { current: ConsentContextValue }, storage };
}

describe('resolveConsentRegime', () => {
  it('prefers the region, then the country, then the fallback', () => {
    expect(resolveConsentRegime({ country: 'DE' })).toBe('opt-in');
    expect(resolveConsentRegime({ country: 'US', region: 'US-CA' })).toBe('opt-out');
    expect(resolveConsentRegime({ region: 'us-ca' })).toBe('opt-out');
    expect(resolveConsentRegime({ country: 'US', region: 'US-WY' })).toBe('notice');
    expect(resolveConsentRegime({ country: 'CA', region: 'CA-QC' })).toBe('opt-in');
    expect(resolveConsentRegime(undefined)).toBe('opt-in');
    expect(resolveConsentRegime({ country: 'ZZ' })).toBe(DEFAULT_CONSENT_REGIMES.fallback);
  });

  it('uses a custom map', () => {
    const map = { countries: { US: 'opt-in' as const }, regions: {}, fallback: 'notice' as const };
    expect(resolveConsentRegime({ country: 'US', region: 'US-CA' }, map)).toBe('opt-in');
    expect(resolveConsentRegime({ country: 'DE' }, map)).toBe('notice');
  });
});

describe('ConsentProvider', () => {
  it('opt-in: only necessary until the person chooses; accept all records the decision', () => {
    const onRecord = vi.fn();
    const onChange = vi.fn();
    const { ctx, storage } = mount({ regime: 'opt-in', onRecord, onChange });
    expect(ctx.current.status).toBe('pending');
    expect(ctx.current.choices).toEqual({
      necessary: true,
      preferences: false,
      analytics: false,
      marketing: false,
    });
    expect(screen.getByText('analytics off')).toBeInTheDocument();
    act(() => ctx.current.acceptAll());
    expect(ctx.current.status).toBe('decided');
    expect(screen.getByText('analytics on')).toBeInTheDocument();
    const record = onRecord.mock.calls[0]?.[0] as ConsentRecord;
    expect(record).toMatchObject({
      regime: 'opt-in',
      policyVersion: '2026-11',
      method: 'accept-all',
      gpc: false,
      categories: { necessary: true, preferences: true, analytics: true, marketing: true },
    });
    expect(typeof record.id).toBe('string');
    expect(Number.isNaN(Date.parse(record.at))).toBe(false);
    expect(storage.value()).toEqual(record);
    expect(onChange).toHaveBeenLastCalledWith(record.categories);
  });

  it('reject all and custom choices keep strictly necessary on', () => {
    const { ctx } = mount({ regime: 'opt-in' });
    act(() => ctx.current.rejectAll());
    expect(ctx.current.choices.necessary).toBe(true);
    expect(ctx.current.record?.method).toBe('reject-all');
    act(() => ctx.current.save({ necessary: false, analytics: true }));
    expect(ctx.current.choices).toMatchObject({
      necessary: true,
      analytics: true,
      marketing: false,
    });
    expect(ctx.current.record?.method).toBe('custom');
  });

  it('restores a stored decision for the same policy version and regime', () => {
    const stored: ConsentRecord = {
      id: 'x',
      regime: 'opt-in',
      policyVersion: '2026-11',
      categories: { necessary: true, preferences: false, analytics: true, marketing: false },
      method: 'custom',
      gpc: false,
      at: '2026-11-01T10:00:00.000Z',
    };
    const { ctx } = mount({ regime: 'opt-in', storage: memoryStorage(stored) });
    expect(ctx.current.status).toBe('decided');
    expect(ctx.current.allowed('analytics')).toBe(true);
    expect(ctx.current.record).toEqual(stored);
  });

  it('asks again when the policy version changes', () => {
    const stored: ConsentRecord = {
      id: 'x',
      regime: 'opt-in',
      policyVersion: '2025-01',
      categories: { necessary: true, analytics: true },
      method: 'accept-all',
      gpc: false,
      at: '2025-01-01T00:00:00.000Z',
    };
    const { ctx } = mount({ regime: 'opt-in', storage: memoryStorage(stored) });
    expect(ctx.current.status).toBe('pending');
    expect(ctx.current.allowed('analytics')).toBe(false);
  });

  it('opt-out: optional categories run, and opting out of sale or sharing turns marketing off', () => {
    const { ctx } = mount({ jurisdiction: { country: 'US', region: 'US-CA' } });
    expect(ctx.current.regime).toBe('opt-out');
    expect(ctx.current.status).toBe('pending');
    expect(ctx.current.allowed('analytics')).toBe(true);
    expect(ctx.current.allowed('marketing')).toBe(true);
    act(() => ctx.current.optOutOfSaleOrSharing());
    expect(ctx.current.allowed('marketing')).toBe(false);
    expect(ctx.current.allowed('analytics')).toBe(true);
  });

  it('opt-out with Global Privacy Control: recorded as the choice, sale or sharing stays off', () => {
    setGpc(true);
    expect(readGlobalPrivacyControl()).toBe(true);
    const onRecord = vi.fn();
    const { ctx } = mount({ regime: 'opt-out', onRecord });
    expect(ctx.current.gpc).toBe(true);
    expect(ctx.current.status).toBe('decided');
    expect(onRecord.mock.calls[0]?.[0]).toMatchObject({ method: 'gpc', gpc: true });
    act(() => ctx.current.acceptAll());
    expect(ctx.current.allowed('marketing')).toBe(false);
  });

  it('GPC can be ignored when the product turns it off', () => {
    setGpc(true);
    const { ctx } = mount({ regime: 'opt-out', honorGlobalPrivacyControl: false });
    expect(ctx.current.gpc).toBe(false);
    expect(ctx.current.status).toBe('pending');
  });

  it('notice: everything runs; acknowledging records it', () => {
    const { ctx } = mount({ regime: 'notice' });
    expect(ctx.current.allowed('marketing')).toBe(true);
    act(() => ctx.current.acknowledge());
    expect(ctx.current.status).toBe('decided');
    expect(ctx.current.record?.method).toBe('acknowledged');
  });

  it('withdraw returns to the defaults and asks again', () => {
    const { ctx } = mount({ regime: 'opt-in' });
    act(() => ctx.current.acceptAll());
    act(() => ctx.current.withdraw());
    expect(ctx.current.status).toBe('pending');
    expect(ctx.current.record?.method).toBe('withdrawn');
    expect(ctx.current.allowed('analytics')).toBe(false);
  });

  it('does not read the choice or report it again when an equal inline categories list is re-rendered', () => {
    const onChange = vi.fn();
    const load = vi.fn(() => Promise.resolve(null));
    const storage = memoryStorage();
    const read = vi.spyOn(storage, 'read');
    const tree = () => (
      <ConsentProvider
        policyVersion="2026-11"
        regime="opt-in"
        storage={storage}
        onChange={onChange}
        sync={{ load, save: () => Promise.resolve() }}
        categories={[
          { id: 'necessary', label: 'Necessary', description: 'Needed.', required: true },
          { id: 'analytics', label: 'Analytics', description: 'Counts visits.' },
        ]}
      >
        <p>child</p>
      </ConsentProvider>
    );
    const { rerender } = render(tree());
    rerender(tree());
    rerender(tree());
    expect(read).toHaveBeenCalledTimes(1);
    expect(load).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledTimes(1);
  });

  it('shares the preferences dialog state', () => {
    const { ctx } = mount();
    expect(ctx.current.preferencesOpen).toBe(false);
    act(() => ctx.current.setPreferencesOpen(true));
    expect(ctx.current.preferencesOpen).toBe(true);
  });

  it('renders nothing gated on the server, with status loading', () => {
    const html = renderToString(
      <ConsentProvider policyVersion="1" regime="notice" storage={memoryStorage()}>
        <ConsentGate category="analytics">
          <span>tracker</span>
        </ConsentGate>
        <Status />
      </ConsentProvider>,
    );
    expect(html).not.toContain('tracker');
    expect(html).toContain('loading');
  });

  it('useConsent throws outside a provider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<Status />)).toThrow(/ConsentProvider/);
    spy.mockRestore();
  });
});

function Status() {
  return <span>{useConsent().status}</span>;
}

describe('storage adapters', () => {
  const record: ConsentRecord = {
    id: 'r1',
    regime: 'opt-in',
    policyVersion: '3',
    categories: { necessary: true, analytics: false },
    method: 'reject-all',
    gpc: false,
    at: '2026-11-07T00:00:00.000Z',
  };

  it('localStorage round-trips and ignores corrupt values', () => {
    const data = new Map<string, string>();
    vi.stubGlobal('localStorage', {
      getItem: (k: string) => data.get(k) ?? null,
      setItem: (k: string, v: string) => void data.set(k, v),
      removeItem: (k: string) => void data.delete(k),
    });
    const s = localStorageConsentStorage('test-consent');
    s.write(record);
    expect(s.read()).toEqual(record);
    window.localStorage.setItem('test-consent', '{not json');
    expect(s.read()).toBeNull();
    s.write(null);
    expect(window.localStorage.getItem('test-consent')).toBeNull();
    vi.unstubAllGlobals();
  });

  it('localStorage that throws (blocked storage) reads null and writes nothing', () => {
    vi.stubGlobal('localStorage', {
      getItem: () => {
        throw new Error('blocked');
      },
      setItem: () => {
        throw new Error('blocked');
      },
      removeItem: () => {},
    });
    const s = localStorageConsentStorage();
    expect(() => s.write(record)).not.toThrow();
    expect(s.read()).toBeNull();
    vi.unstubAllGlobals();
  });

  it('cookie storage round-trips and clears', () => {
    const s = cookieConsentStorage('test_consent');
    s.write(record);
    expect(document.cookie).toContain('test_consent=');
    expect(s.read()).toEqual(record);
    s.write(null);
    expect(s.read()).toBeNull();
  });
});

const rec = (over: Partial<ConsentRecord> = {}): ConsentRecord => ({
  id: 'r',
  regime: 'opt-in',
  policyVersion: '2026-11',
  categories: { necessary: true, preferences: false, analytics: false, marketing: false },
  method: 'reject-all',
  gpc: false,
  at: '2026-11-01T10:00:00.000Z',
  ...over,
});

function fakeSync(remote: ConsentRecord | null) {
  const saved: ConsentRecord[] = [];
  const sync: ConsentSync = {
    load: vi.fn(async () => remote),
    save: vi.fn(async (r: ConsentRecord) => void saved.push(r)),
  };
  return { sync, saved };
}

describe('ConsentProvider sync', () => {
  it('applies a newer account record on a device with no local choice', async () => {
    const remote = rec({
      id: 'acct',
      method: 'accept-all',
      categories: { necessary: true, preferences: true, analytics: true, marketing: true },
    });
    const { sync } = fakeSync(remote);
    const { ctx, storage } = mount({ regime: 'opt-in', sync });
    await waitFor(() => expect(ctx.current.status).toBe('decided'));
    expect(ctx.current.allowed('analytics')).toBe(true);
    expect(storage.value()?.id).toBe('acct');
  });

  it('keeps a newer local record and sends it up', async () => {
    const local = rec({ id: 'local', at: '2026-11-05T10:00:00.000Z' });
    const { sync, saved } = fakeSync(
      rec({
        id: 'old',
        at: '2026-11-01T10:00:00.000Z',
        categories: { necessary: true, analytics: true },
      }),
    );
    const { ctx } = mount({ regime: 'opt-in', storage: memoryStorage(local), sync });
    await waitFor(() => expect(saved.map((r) => r.id)).toEqual(['local']));
    expect(ctx.current.allowed('analytics')).toBe(false);
  });

  it('ignores an account record for another policy version', async () => {
    const { sync } = fakeSync(rec({ policyVersion: '2025-01', method: 'accept-all' }));
    const { ctx } = mount({ regime: 'opt-in', sync });
    await waitFor(() => expect(sync.load).toHaveBeenCalled());
    expect(ctx.current.status).toBe('pending');
  });

  it('saves every new decision to the account and survives a failing load', async () => {
    const saved: ConsentRecord[] = [];
    const sync: ConsentSync = {
      load: () => Promise.reject(new Error('signed out')),
      save: async (r) => void saved.push(r),
    };
    const { ctx } = mount({ regime: 'opt-in', sync });
    act(() => ctx.current.acceptAll());
    await waitFor(() => expect(saved).toHaveLength(1));
    expect(saved[0]?.method).toBe('accept-all');
  });
});

describe('ConsentAnalytics', () => {
  afterEach(() => {
    document.head.querySelectorAll('script[id^="mn-consent-"]').forEach((n) => n.remove());
    const w = window as unknown as Record<string, unknown>;
    delete w.dataLayer;
    delete w.gtag;
    delete w.clarity;
    delete w.__mnConsentDefault;
  });

  const scripts = () =>
    [...document.head.querySelectorAll<HTMLScriptElement>('script[id^="mn-consent-"]')].map(
      (s) => s.src,
    );

  function renderAnalytics(storage = memoryStorage()) {
    const ref: { current: ConsentContextValue | null } = { current: null };
    function Grab() {
      ref.current = useConsent();
      return null;
    }
    render(
      <ConsentProvider policyVersion="2026-11" regime="opt-in" storage={storage}>
        <Grab />
        <ConsentAnalytics googleTagId="G-TEST123" clarityId="abc123" />
      </ConsentProvider>,
    );
    return ref as { current: ConsentContextValue };
  }

  it('loads nothing before consent and sends Consent Mode defaults as denied', () => {
    renderAnalytics();
    expect(scripts()).toEqual([]);
    const layer = (window as unknown as { dataLayer: IArguments[] }).dataLayer.map((a) => [...a]);
    expect(layer[0]).toEqual([
      'consent',
      'default',
      {
        ad_storage: 'denied',
        ad_user_data: 'denied',
        ad_personalization: 'denied',
        analytics_storage: 'denied',
      },
    ]);
  });

  it('loads Google and Clarity after analytics is allowed and grants analytics only', () => {
    const ctx = renderAnalytics();
    act(() => ctx.current.save({ analytics: true }));
    expect(scripts()).toEqual([
      'https://www.googletagmanager.com/gtag/js?id=G-TEST123',
      'https://www.clarity.ms/tag/abc123',
    ]);
    const layer = (window as unknown as { dataLayer: IArguments[] }).dataLayer.map((a) => [...a]);
    expect(layer).toContainEqual([
      'consent',
      'update',
      {
        analytics_storage: 'granted',
        ad_storage: 'denied',
        ad_user_data: 'denied',
        ad_personalization: 'denied',
      },
    ]);
  });

  it('removes analytics cookies and denies again when consent is withdrawn', () => {
    const ctx = renderAnalytics();
    act(() => ctx.current.acceptAll());
    document.cookie = '_ga=GA1.1.123; path=/';
    document.cookie = '_clck=abc; path=/';
    document.cookie = 'keep=1; path=/';
    act(() => ctx.current.withdraw());
    expect(document.cookie).not.toMatch(/_ga=|_clck=/);
    expect(document.cookie).toMatch(/keep=1/);
    document.cookie = 'keep=; max-age=0; path=/';
  });

  it('loads Tag Manager for a GTM container id', () => {
    const storage = memoryStorage(
      rec({ method: 'accept-all', categories: { necessary: true, analytics: true } }),
    );
    render(
      <ConsentProvider policyVersion="2026-11" regime="opt-in" storage={storage}>
        <ConsentAnalytics googleTagId="GTM-ABC" />
      </ConsentProvider>,
    );
    expect(scripts()).toEqual(['https://www.googletagmanager.com/gtm.js?id=GTM-ABC']);
  });

  it('loads Clarity only when its own category is allowed', () => {
    const categories = [
      { id: 'necessary', label: 'Necessary', description: 'Needed.', required: true },
      { id: 'analytics', label: 'Analytics', description: 'Counts visits.' },
      { id: 'recordings', label: 'Recordings', description: 'Records sessions.', askFirst: true },
    ];
    const ref: { current: ConsentContextValue | null } = { current: null };
    function Grab() {
      ref.current = useConsent();
      return null;
    }
    render(
      <ConsentProvider
        policyVersion="2026-11"
        regime="opt-out"
        storage={memoryStorage()}
        categories={categories}
      >
        <Grab />
        <ConsentAnalytics googleTagId="G-TEST123" clarityId="abc123" clarityCategory="recordings" />
      </ConsentProvider>,
    );
    expect(scripts()).toEqual(['https://www.googletagmanager.com/gtag/js?id=G-TEST123']);
    act(() => ref.current!.acceptAll());
    expect(scripts()).toContain('https://www.clarity.ms/tag/abc123');
  });

  it('renders nothing on the server', () => {
    expect(
      renderToString(
        <ConsentProvider policyVersion="2026-11" regime="opt-in">
          <ConsentAnalytics googleTagId="G-TEST123" clarityId="abc123" />
        </ConsentProvider>,
      ),
    ).toBe('');
  });
});

const daysAgo = (n: number) => new Date(Date.now() - n * 86_400_000).toISOString();

describe('ConsentProvider expiry', () => {
  const accepted = (days: number) =>
    rec({
      method: 'accept-all',
      at: daysAgo(days),
      categories: { necessary: true, preferences: true, analytics: true, marketing: true },
    });
  const refused = (days: number) => rec({ method: 'reject-all', at: daysAgo(days) });

  it('remembers a refusal for 180 days, then asks again', () => {
    expect(
      mount({ regime: 'opt-in', storage: memoryStorage(refused(120)) }).ctx.current.status,
    ).toBe('decided');
    expect(
      mount({ regime: 'opt-in', storage: memoryStorage(refused(200)) }).ctx.current.status,
    ).toBe('pending');
  });

  it('remembers an acceptance for 365 days, then asks again', () => {
    expect(
      mount({ regime: 'opt-in', storage: memoryStorage(accepted(300)) }).ctx.current.status,
    ).toBe('decided');
    expect(
      mount({ regime: 'opt-in', storage: memoryStorage(accepted(400)) }).ctx.current.status,
    ).toBe('pending');
  });

  it('does not ask again after 10 days, whatever was chosen', () => {
    expect(
      mount({ regime: 'opt-in', storage: memoryStorage(refused(10)) }).ctx.current.status,
    ).toBe('decided');
  });

  it('takes custom limits', () => {
    const { ctx } = mount({
      regime: 'opt-in',
      storage: memoryStorage(refused(60)),
      expiry: { declined: 30 },
    });
    expect(ctx.current.status).toBe('pending');
  });

  it('never expires a Global Privacy Control record', () => {
    setGpc(true);
    const { ctx } = mount({
      regime: 'opt-out',
      storage: memoryStorage(rec({ regime: 'opt-out', method: 'gpc', at: daysAgo(900) })),
    });
    expect(ctx.current.status).toBe('decided');
  });
});

describe('ConsentProvider askFirst and locating', () => {
  const categories = [
    { id: 'necessary', label: 'Necessary', description: 'Needed.', required: true },
    { id: 'analytics', label: 'Analytics', description: 'Counts visits.' },
    { id: 'recordings', label: 'Recordings', description: 'Records sessions.', askFirst: true },
  ];

  it('keeps an askFirst category off where optional storage otherwise runs', () => {
    const { ctx } = mount({ regime: 'opt-out', categories });
    expect(ctx.current.allowed('analytics')).toBe(true);
    expect(ctx.current.allowed('recordings')).toBe(false);
  });

  it('accept all still turns an askFirst category on', () => {
    const { ctx } = mount({ regime: 'opt-out', categories });
    act(() => ctx.current.acceptAll());
    expect(ctx.current.allowed('recordings')).toBe(true);
  });

  it('decides nothing while the location is being looked up', () => {
    const { ctx, rerender } = mount({ regime: 'opt-in', locating: true });
    expect(ctx.current.status).toBe('loading');
    expect(ctx.current.allowed('analytics')).toBe(false);
    rerender(
      <ConsentProvider
        policyVersion="2026-11"
        storage={memoryStorage()}
        regime="opt-in"
        locating={false}
      >
        <span>done</span>
      </ConsentProvider>,
    );
  });
});

describe('ConsentProvider stored choices across rules', () => {
  const accepted = (regime: ConsentRegime) =>
    rec({
      regime,
      method: regime === 'opt-in' ? 'accept-all' : 'acknowledged',
      categories: { necessary: true, preferences: true, analytics: true, marketing: true },
    });

  it('keeps a refusal when the rule changes to a lighter one', () => {
    const { ctx } = mount({
      regime: 'opt-out',
      storage: memoryStorage(rec({ regime: 'opt-in', method: 'reject-all' })),
    });
    expect(ctx.current.status).toBe('decided');
    expect(ctx.current.allowed('analytics')).toBe(false);
  });

  it('keeps a refusal made under a lighter rule when the rule becomes opt-in', () => {
    const { ctx } = mount({
      regime: 'opt-in',
      storage: memoryStorage(rec({ regime: 'opt-out', method: 'reject-all' })),
    });
    expect(ctx.current.status).toBe('decided');
    expect(ctx.current.allowed('analytics')).toBe(false);
  });

  it('keeps an explicit acceptance given when consent was asked first, under a lighter rule', () => {
    const { ctx } = mount({ regime: 'opt-out', storage: memoryStorage(accepted('opt-in')) });
    expect(ctx.current.status).toBe('decided');
    expect(ctx.current.allowed('analytics')).toBe(true);
  });

  it('asks again when an acknowledgement under a lighter rule meets opt-in', () => {
    const { ctx } = mount({ regime: 'opt-in', storage: memoryStorage(accepted('opt-out')) });
    expect(ctx.current.status).toBe('pending');
    expect(ctx.current.allowed('analytics')).toBe(false);
  });
});
