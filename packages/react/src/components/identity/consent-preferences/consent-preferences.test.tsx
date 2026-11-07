import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  ConsentProvider,
  useConsent,
  type ConsentProviderProps,
  type ConsentRecord,
  type ConsentRegime,
  type ConsentStorage,
} from '../consent-manager/index.js';
import { CookieConsentBanner } from '../cookie-consent-banner/index.js';
import { ConsentPreferences, ConsentSettingsLink, type ConsentPreferencesProps } from './index.js';

function memoryStorage(initial: ConsentRecord | null = null): ConsentStorage {
  let value = initial;
  return {
    read: () => value,
    write: (record) => {
      value = record;
    },
  };
}

function setGpc(on: boolean | undefined) {
  Object.defineProperty(window.navigator, 'globalPrivacyControl', {
    configurable: true,
    get: () => on,
  });
}

afterEach(() => setGpc(undefined));

const stored: ConsentRecord = {
  id: 'r1',
  regime: 'opt-in',
  policyVersion: '3',
  categories: { necessary: true, preferences: true, analytics: false, marketing: false },
  method: 'custom',
  gpc: false,
  at: '2026-01-02T10:00:00.000Z',
};

function Status() {
  const { status } = useConsent();
  return <output data-testid="status">{status}</output>;
}

function setup(
  regime: ConsentRegime,
  props: Partial<ConsentPreferencesProps> = {},
  provider: Partial<Omit<ConsentProviderProps, 'children'>> = {},
) {
  const onRecord = vi.fn<(record: ConsentRecord) => void>();
  const utils = render(
    <ConsentProvider
      policyVersion="3"
      regime={regime}
      storage={memoryStorage()}
      onRecord={onRecord}
      {...provider}
    >
      <footer>
        <ConsentSettingsLink />
      </footer>
      <ConsentPreferences {...props} />
      <Status />
    </ConsentProvider>,
  );
  return { ...utils, onRecord, user: userEvent.setup() };
}

const openFromFooter = async (user: ReturnType<typeof userEvent.setup>) => {
  const link = screen.getByRole('button', { name: 'Cookie settings' });
  await user.click(link);
  return screen.findByRole('dialog', { name: 'Cookie settings' });
};

describe('ConsentPreferences', () => {
  it('opens from ConsentSettingsLink (a plain button) and returns focus on Escape', async () => {
    const { user } = setup('opt-in');
    const link = screen.getByRole('button', { name: 'Cookie settings' });
    expect(link.tagName).toBe('BUTTON');
    expect(link).toHaveAttribute('type', 'button');
    expect(link).not.toHaveAttribute('href');
    expect(link).toHaveClass('mn-consent-settings-link');
    const dialog = await openFromFooter(user);
    expect(dialog).toHaveClass('mn-consent-preferences');
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(link).toHaveFocus();
  });

  it('opt-in: optional categories start off; necessary is "Always on" and locked', async () => {
    const { user } = setup('opt-in');
    await openFromFooter(user);
    const necessary = screen.getByRole('switch', { name: 'Strictly necessary' });
    expect(necessary).toBeChecked();
    expect(necessary).toBeDisabled();
    expect(necessary).toHaveAccessibleDescription(/Always on/);
    for (const name of ['Preferences', 'Analytics', 'Marketing']) {
      const control = screen.getByRole('switch', { name });
      expect(control).not.toBeChecked();
      expect(control).toBeEnabled();
    }
  });

  it('Reject all and Accept all share a variant; Save records a custom choice', async () => {
    const { user, onRecord } = setup('opt-in');
    const dialog = await openFromFooter(user);
    const reject = within(dialog).getByRole('button', { name: 'Reject all' });
    const accept = within(dialog).getByRole('button', { name: 'Accept all' });
    expect(reject.getAttribute('data-variant')).toBe(accept.getAttribute('data-variant'));
    expect(reject.getAttribute('data-size')).toBe(accept.getAttribute('data-size'));
    await user.click(screen.getByRole('switch', { name: 'Analytics' }));
    await user.click(within(dialog).getByRole('button', { name: 'Save choices' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onRecord).toHaveBeenCalledOnce();
    expect(onRecord.mock.calls[0]![0]).toMatchObject({
      method: 'custom',
      categories: { necessary: true, preferences: false, analytics: true, marketing: false },
    });
    expect(screen.getByTestId('status')).toHaveTextContent('decided');
  });

  it('Accept all and Reject all record and close', async () => {
    const { user, onRecord } = setup('opt-in');
    let dialog = await openFromFooter(user);
    await user.click(within(dialog).getByRole('button', { name: 'Accept all' }));
    expect(onRecord.mock.calls[0]![0].method).toBe('accept-all');
    dialog = await openFromFooter(user);
    expect(screen.getByRole('switch', { name: 'Marketing' })).toBeChecked();
    await user.click(within(dialog).getByRole('button', { name: 'Reject all' }));
    expect(onRecord.mock.calls[1]![0]).toMatchObject({
      method: 'reject-all',
      categories: { marketing: false, necessary: true },
    });
  });

  it('shows the saved policy version and time, and withdraws consent', async () => {
    const { user, onRecord } = setup('opt-in', {}, { storage: memoryStorage(stored) });
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('decided'));
    const dialog = await openFromFooter(user);
    expect(dialog).toHaveTextContent('Policy version: 3');
    expect(dialog.querySelector('time')).toHaveAttribute('datetime', stored.at);
    expect(screen.getByRole('switch', { name: 'Preferences' })).toBeChecked();
    await user.click(within(dialog).getByRole('button', { name: 'Withdraw consent' }));
    expect(onRecord.mock.calls[0]![0]).toMatchObject({
      method: 'withdrawn',
      categories: { preferences: false },
    });
    expect(screen.getByTestId('status')).toHaveTextContent('pending');
  });

  it('hides Withdraw without a record or when allowWithdraw is false', async () => {
    const { user, unmount } = setup('opt-in');
    let dialog = await openFromFooter(user);
    expect(within(dialog).queryByRole('button', { name: 'Withdraw consent' })).toBeNull();
    unmount();
    const second = setup('opt-in', { allowWithdraw: false }, { storage: memoryStorage(stored) });
    dialog = await openFromFooter(second.user);
    expect(within(dialog).queryByRole('button', { name: 'Withdraw consent' })).toBeNull();
  });

  it('GPC locks sale-or-sharing categories off with a note', async () => {
    setGpc(true);
    const { user, onRecord } = setup('opt-in');
    const dialog = await openFromFooter(user);
    expect(dialog).toHaveAttribute('data-gpc');
    const marketing = screen.getByRole('switch', { name: 'Marketing' });
    expect(marketing).not.toBeChecked();
    expect(marketing).toBeDisabled();
    expect(marketing).toHaveAccessibleDescription(/Global Privacy Control/);
    await user.click(within(dialog).getByRole('button', { name: 'Accept all' }));
    expect(onRecord.mock.calls[0]![0]).toMatchObject({
      gpc: true,
      categories: { analytics: true, marketing: false },
    });
  });

  it('opens from the banner Customize button', async () => {
    const user = userEvent.setup();
    render(
      <ConsentProvider policyVersion="1" regime="opt-in" storage={memoryStorage()}>
        <CookieConsentBanner />
        <ConsentPreferences />
      </ConsentProvider>,
    );
    await user.click(await screen.findByRole('button', { name: 'Customize' }));
    expect(await screen.findByRole('dialog', { name: 'Cookie settings' })).toBeInTheDocument();
  });

  it('can be controlled with open and onOpenChange', async () => {
    const onOpenChange = vi.fn();
    const user = userEvent.setup();
    render(
      <ConsentProvider policyVersion="1" regime="opt-in" storage={memoryStorage()}>
        <ConsentPreferences
          open
          onOpenChange={onOpenChange}
          messages={{ title: 'Privacy settings' }}
        />
      </ConsentProvider>,
    );
    await screen.findByRole('dialog', { name: 'Privacy settings' });
    await user.keyboard('{Escape}');
    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('renders on the server (closed)', () => {
    const html = renderToString(
      <ConsentProvider policyVersion="1" regime="opt-in" storage={memoryStorage()}>
        <ConsentSettingsLink />
        <ConsentPreferences />
      </ConsentProvider>,
    );
    expect(html).toContain('mn-consent-settings-link');
    expect(html).not.toContain('role="dialog"');
  });
});
