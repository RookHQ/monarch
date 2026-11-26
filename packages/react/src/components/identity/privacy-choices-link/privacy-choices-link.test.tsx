import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  ConsentProvider,
  type ConsentRecord,
  type ConsentRegime,
  type ConsentStorage,
} from '../consent-manager/index.js';
import { PrivacyChoicesLink, type PrivacyChoicesLinkProps } from './index.js';

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

function setup(regime: ConsentRegime, props: Partial<PrivacyChoicesLinkProps> = {}) {
  const onRecord = vi.fn<(record: ConsentRecord) => void>();
  const utils = render(
    <ConsentProvider
      policyVersion="1"
      regime={regime}
      storage={memoryStorage()}
      onRecord={onRecord}
    >
      <PrivacyChoicesLink {...props} />
    </ConsentProvider>,
  );
  return { ...utils, onRecord, user: userEvent.setup() };
}

const trigger = () => screen.getByRole('button', { name: 'Your privacy choices' });

describe('PrivacyChoicesLink', () => {
  it('renders only in opt-out by default', () => {
    const { unmount } = setup('opt-in');
    expect(screen.queryByRole('button')).toBeNull();
    unmount();
    setup('notice');
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('showIn widens where it renders', () => {
    setup('opt-in', { showIn: ['opt-in', 'opt-out'] });
    expect(trigger()).toHaveClass('mn-privacy-choices-link');
  });

  it('opens a dialog; turning the switch off and saving opts out of sale or sharing', async () => {
    const { user, onRecord } = setup('opt-out', { policyHref: '/privacy' });
    await user.click(trigger());
    const dialog = await screen.findByRole('dialog', { name: 'Your privacy choices' });
    await expectNoA11yViolations(dialog);
    expect(within(dialog).getByRole('link', { name: 'Privacy policy' })).toHaveAttribute(
      'href',
      '/privacy',
    );
    const toggle = within(dialog).getByRole('switch', { name: 'Allow sale or sharing' });
    expect(toggle).toBeChecked();
    await user.click(toggle);
    expect(toggle).not.toBeChecked();
    await user.click(within(dialog).getByRole('button', { name: 'Save' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger()).toHaveFocus();
    expect(onRecord).toHaveBeenCalledOnce();
    expect(onRecord.mock.calls[0]![0]).toMatchObject({
      regime: 'opt-out',
      categories: { necessary: true, analytics: true, marketing: false },
    });
  });

  it('saving with the switch on keeps sale or sharing allowed; Cancel and Escape save nothing', async () => {
    const { user, onRecord } = setup('opt-out');
    await user.click(trigger());
    let dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('switch'));
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger()).toHaveFocus();
    await user.click(trigger());
    dialog = await screen.findByRole('dialog');
    // Every opening starts from the current choice.
    expect(within(dialog).getByRole('switch')).toBeChecked();
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    expect(onRecord).not.toHaveBeenCalled();
    await user.click(trigger());
    dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(onRecord.mock.calls[0]![0]).toMatchObject({
      method: 'custom',
      categories: { marketing: true },
    });
  });

  it('GPC: the switch is off and locked, and the dialog says why', async () => {
    setGpc(true);
    const { user, onRecord } = setup('opt-out');
    await waitFor(() => expect(onRecord).toHaveBeenCalledOnce()); // the gpc record
    expect(trigger()).toHaveAttribute('data-gpc');
    await user.click(trigger());
    const dialog = await screen.findByRole('dialog');
    const toggle = within(dialog).getByRole('switch', { name: 'Allow sale or sharing' });
    expect(toggle).not.toBeChecked();
    expect(toggle).toBeDisabled();
    expect(toggle).toHaveAccessibleDescription(
      'Your browser’s Global Privacy Control signal is on, so sale and sharing stay off.',
    );
  });

  it('custom label, icon slot and messages', async () => {
    const { user } = setup('opt-out', {
      label: 'Do not sell or share my personal information',
      icon: <svg data-testid="icon" />,
      messages: { title: 'Sale and sharing' },
    });
    const button = screen.getByRole('button', {
      name: 'Do not sell or share my personal information',
    });
    expect(screen.getByTestId('icon').parentElement).toHaveAttribute('aria-hidden');
    await user.click(button);
    expect(await screen.findByRole('dialog', { name: 'Sale and sharing' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <ConsentProvider policyVersion="1" regime="opt-out" storage={memoryStorage()}>
        <PrivacyChoicesLink />
      </ConsentProvider>,
    );
    expect(html).toContain('mn-privacy-choices-link');
    expect(html).toContain('Your privacy choices');
  });
});
