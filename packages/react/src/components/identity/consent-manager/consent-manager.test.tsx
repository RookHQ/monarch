import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import {
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
  type ConsentRecord,
  type ConsentStorage,
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
