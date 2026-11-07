import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import {
  ConsentProvider,
  useConsent,
  type ConsentProviderProps,
  type ConsentRecord,
  type ConsentRegime,
  type ConsentStorage,
} from '../consent-manager/index.js';
import { CookieConsentBanner, type CookieConsentBannerProps } from './index.js';

type MemoryStorage = ConsentStorage & { value: () => ConsentRecord | null };

function memoryStorage(initial: ConsentRecord | null = null): MemoryStorage {
  let value = initial;
  const storage: MemoryStorage = {
    read: () => value,
    write: (record) => {
      value = record;
    },
    value: () => value,
  };
  return storage;
}

function setGpc(on: boolean | undefined) {
  Object.defineProperty(window.navigator, 'globalPrivacyControl', {
    configurable: true,
    get: () => on,
  });
}

afterEach(() => setGpc(undefined));

function PreferencesState() {
  const { preferencesOpen } = useConsent();
  return <output data-testid="prefs">{String(preferencesOpen)}</output>;
}

function setup(
  regime: ConsentRegime,
  props: Partial<CookieConsentBannerProps> = {},
  provider: Partial<Omit<ConsentProviderProps, 'children' | 'storage'>> & {
    storage?: MemoryStorage;
  } = {},
) {
  const storage = provider.storage ?? memoryStorage();
  const onRecord = vi.fn<(record: ConsentRecord) => void>();
  const utils = render(
    <ConsentProvider
      policyVersion="2026-01"
      regime={regime}
      storage={storage}
      onRecord={onRecord}
      {...provider}
    >
      <Button>Page content</Button>
      <CookieConsentBanner {...props} />
      <PreferencesState />
    </ConsentProvider>,
  );
  return { ...utils, storage, onRecord, user: userEvent.setup() };
}

const banner = () => screen.findByRole('region', { name: 'Cookies on this site' });

describe('CookieConsentBanner', () => {
  it('opt-in: Reject all and Accept all have the same variant and size; axe passes', async () => {
    setup('opt-in', { policyHref: '/cookies' });
    const region = await banner();
    expect(region).toHaveClass('mn-cookie-consent-banner');
    expect(region).toHaveAttribute('data-regime', 'opt-in');
    expect(region).toHaveAttribute('data-position', 'bottom');
    const reject = screen.getByRole('button', { name: 'Reject all' });
    const accept = screen.getByRole('button', { name: 'Accept all' });
    expect(reject.getAttribute('data-variant')).toBe(accept.getAttribute('data-variant'));
    expect(reject.getAttribute('data-size')).toBe(accept.getAttribute('data-size'));
    expect(screen.getByRole('link', { name: 'Cookie policy' })).toHaveAttribute('href', '/cookies');
    await expectNoA11yViolations();
  });

  it('choiceVariant applies to both choices', async () => {
    setup('opt-in', { choiceVariant: 'primary' });
    await banner();
    expect(screen.getByRole('button', { name: 'Reject all' })).toHaveAttribute(
      'data-variant',
      'primary',
    );
    expect(screen.getByRole('button', { name: 'Accept all' })).toHaveAttribute(
      'data-variant',
      'primary',
    );
  });

  it('opt-in: Reject all records the choice, stores it and hides the banner', async () => {
    const { user, onRecord, storage } = setup('opt-in');
    await banner();
    await user.click(screen.getByRole('button', { name: 'Reject all' }));
    expect(screen.queryByRole('region')).not.toBeInTheDocument();
    expect(onRecord).toHaveBeenCalledOnce();
    const record = onRecord.mock.calls[0]![0];
    expect(record).toMatchObject({
      method: 'reject-all',
      regime: 'opt-in',
      policyVersion: '2026-01',
      categories: { necessary: true, preferences: false, analytics: false, marketing: false },
    });
    expect(typeof record.id).toBe('string');
    expect(Number.isNaN(Date.parse(record.at))).toBe(false);
    expect(storage.value()).toEqual(record);
  });

  it('opt-in: Accept all turns every category on', async () => {
    const { user, onRecord } = setup('opt-in');
    await banner();
    await user.click(screen.getByRole('button', { name: 'Accept all' }));
    expect(onRecord.mock.calls[0]![0]).toMatchObject({
      method: 'accept-all',
      categories: { necessary: true, preferences: true, analytics: true, marketing: true },
    });
  });

  it('opt-in: Customize opens the shared preferences state', async () => {
    const { user } = setup('opt-in');
    await banner();
    expect(screen.getByTestId('prefs')).toHaveTextContent('false');
    await user.click(screen.getByRole('button', { name: 'Customize' }));
    expect(screen.getByTestId('prefs')).toHaveTextContent('true');
  });

  it('does not take focus, does not trap it and ignores Escape', async () => {
    const { user } = setup('opt-in');
    await banner();
    expect(document.body).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Page content' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Customize' })).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(screen.getByRole('region')).toBeInTheDocument();
    await user.tab();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Accept all' })).toHaveFocus();
    await user.tab();
    expect(document.body).toHaveFocus();
  });

  it('after a keyboard choice, focus returns to where it was before the banner', async () => {
    const { user } = setup('opt-in');
    await banner();
    await user.tab();
    const page = screen.getByRole('button', { name: 'Page content' });
    expect(page).toHaveFocus();
    await user.tab();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Reject all' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(screen.queryByRole('region')).toBeNull();
    await waitFor(() => expect(page).toHaveFocus());
  });

  it('announces politely once', async () => {
    setup('opt-in');
    const region = await banner();
    const status = region.querySelector('[role="status"]')!;
    expect(status).toHaveAttribute('aria-live', 'polite');
    await waitFor(() =>
      expect(status).toHaveTextContent('Cookie notice shown at the end of the page.'),
    );
  });

  it('opt-out: Do not sell or share turns off sale-or-sharing categories only', async () => {
    const { user, onRecord } = setup('opt-out');
    const region = await banner();
    expect(region).toHaveAttribute('data-regime', 'opt-out');
    expect(screen.queryByRole('button', { name: 'Accept all' })).not.toBeInTheDocument();
    await expectNoA11yViolations();
    await user.click(
      screen.getByRole('button', { name: 'Do not sell or share my personal information' }),
    );
    expect(screen.queryByRole('region')).not.toBeInTheDocument();
    expect(onRecord.mock.calls[0]![0]).toMatchObject({
      method: 'custom',
      regime: 'opt-out',
      categories: { necessary: true, preferences: true, analytics: true, marketing: false },
    });
  });

  it('opt-out: OK acknowledges without changing choices', async () => {
    const { user, onRecord } = setup('opt-out');
    await banner();
    await user.click(screen.getByRole('button', { name: 'OK' }));
    expect(onRecord.mock.calls[0]![0]).toMatchObject({
      method: 'acknowledged',
      categories: { marketing: true },
    });
  });

  it('opt-out with GPC: records the signal and shows no banner', async () => {
    setGpc(true);
    const { onRecord } = setup('opt-out');
    await waitFor(() => expect(onRecord).toHaveBeenCalledOnce());
    expect(onRecord.mock.calls[0]![0]).toMatchObject({
      method: 'gpc',
      gpc: true,
      categories: { marketing: false },
    });
    expect(screen.queryByRole('region')).not.toBeInTheDocument();
  });

  it('opt-out with GPC after a withdrawal: says the signal was honoured', async () => {
    setGpc(true);
    function Withdraw() {
      const { withdraw } = useConsent();
      return <Button onClick={withdraw}>Withdraw</Button>;
    }
    const onRecord = vi.fn();
    render(
      <ConsentProvider
        policyVersion="1"
        regime="opt-out"
        storage={memoryStorage()}
        onRecord={onRecord}
      >
        <Withdraw />
        <CookieConsentBanner />
      </ConsentProvider>,
    );
    await waitFor(() => expect(onRecord).toHaveBeenCalledOnce());
    await userEvent.setup().click(screen.getByRole('button', { name: 'Withdraw' }));
    const region = await banner();
    expect(region).toHaveAttribute('data-gpc');
    expect(region).toHaveTextContent('Global Privacy Control signal was honoured');
    expect(
      screen.queryByRole('button', { name: 'Do not sell or share my personal information' }),
    ).not.toBeInTheDocument();
  });

  it('notice: shows text and OK only', async () => {
    const { user, onRecord } = setup('notice', {
      messages: { notice: { description: 'Custom notice text.' } },
    });
    const region = await banner();
    expect(region).toHaveTextContent('Custom notice text.');
    expect(screen.getAllByRole('button')).toHaveLength(2); // page content + OK
    await user.click(screen.getByRole('button', { name: 'OK' }));
    expect(onRecord.mock.calls[0]![0]).toMatchObject({ method: 'acknowledged', regime: 'notice' });
    expect(screen.queryByRole('region')).not.toBeInTheDocument();
  });

  it('stays hidden when a choice for this policy version is stored; a new version asks again', async () => {
    const stored: ConsentRecord = {
      id: 'r1',
      regime: 'opt-in',
      policyVersion: '2026-01',
      categories: { necessary: true, analytics: true },
      method: 'custom',
      gpc: false,
      at: '2026-01-02T00:00:00.000Z',
    };
    const { unmount } = setup('opt-in', {}, { storage: memoryStorage(stored) });
    await waitFor(() => expect(screen.getByTestId('prefs')).toBeInTheDocument());
    expect(screen.queryByRole('region')).not.toBeInTheDocument();
    unmount();
    setup('opt-in', {}, { storage: memoryStorage(stored), policyVersion: '2026-06' });
    expect(await banner()).toBeInTheDocument();
  });

  it('bottom-start position and a custom policy link node', async () => {
    setup('opt-in', {
      position: 'bottom-start',
      policyLink: <a href="/privacy">Read the policy</a>,
      className: 'extra',
    });
    const region = await banner();
    expect(region).toHaveAttribute('data-position', 'bottom-start');
    expect(region).toHaveClass('extra');
    expect(screen.getByRole('link', { name: 'Read the policy' })).toBeInTheDocument();
  });

  it('renders no banner on the server', () => {
    const html = renderToString(
      <ConsentProvider policyVersion="1" regime="opt-in" storage={memoryStorage()}>
        <CookieConsentBanner />
      </ConsentProvider>,
    );
    expect(html).not.toContain('mn-cookie-consent-banner');
  });
});
