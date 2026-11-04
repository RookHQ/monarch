import { afterEach, describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SwitchSettingsItem, SwitchSettingsList } from './index.js';

function deferred() {
  let resolve!: () => void;
  let reject!: (e: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

afterEach(() => {
  vi.useRealTimers();
});

describe('SwitchSettingsList', () => {
  it('is a named group of labelled, described switches', async () => {
    render(
      <SwitchSettingsList title="Email notifications" description="Sent to your sign-in address.">
        <SwitchSettingsItem
          label="Security alerts"
          description="New sign-ins and password changes."
          defaultChecked
        />
        <SwitchSettingsItem label="Product updates" />
      </SwitchSettingsList>,
    );
    const group = screen.getByRole('group', { name: 'Email notifications' });
    expect(group).toHaveAccessibleDescription('Sent to your sign-in address.');
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
    const alerts = screen.getByRole('switch', { name: 'Security alerts' });
    expect(alerts).toBeChecked();
    expect(alerts).toHaveAccessibleDescription('New sign-ins and password changes.');
    expect(screen.getByRole('switch', { name: 'Product updates' })).not.toBeChecked();
    await expectNoA11yViolations();
  });

  it('uncontrolled: optimistic toggle, saving, then an announced "Saved" that clears', async () => {
    const pending = deferred();
    const onCheckedChange = vi.fn(() => pending.promise);
    render(
      <SwitchSettingsList title="Alerts" savedDuration={50}>
        <SwitchSettingsItem label="Weekly digest" onCheckedChange={onCheckedChange} />
      </SwitchSettingsList>,
    );
    const sw = screen.getByRole('switch', { name: 'Weekly digest' });
    const user = userEvent.setup();
    await user.tab();
    expect(sw).toHaveFocus();
    await user.keyboard(' ');
    expect(onCheckedChange).toHaveBeenCalledWith(true);
    expect(sw).toBeChecked();
    expect(sw).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('status', { name: 'Saving' })).toBeInTheDocument();
    // Toggling again while saving is ignored.
    await user.keyboard(' ');
    expect(onCheckedChange).toHaveBeenCalledTimes(1);
    await act(async () => {
      pending.resolve();
      await pending.promise;
    });
    expect(sw).toBeChecked();
    expect(sw).not.toHaveAttribute('aria-busy');
    expect(screen.getAllByRole('status').some((s) => s.textContent === 'Saved')).toBe(true);
    expect(sw.closest('li')).toHaveAttribute('data-status', 'saved');
    await act(() => new Promise((r) => setTimeout(r, 80)));
    expect(sw.closest('li')).toHaveAttribute('data-status', 'idle');
  });

  it('rolls back on failure, announces the error, and retries the same value', async () => {
    const onCheckedChange = vi
      .fn<(v: boolean) => Promise<void>>()
      .mockRejectedValueOnce(new Error('Network unavailable.'))
      .mockResolvedValueOnce(undefined);
    const onStatusChange = vi.fn();
    render(
      <SwitchSettingsList title="Alerts">
        <SwitchSettingsItem
          label="New device sign-in"
          defaultChecked
          onCheckedChange={onCheckedChange}
          onStatusChange={onStatusChange}
        />
      </SwitchSettingsList>,
    );
    const sw = screen.getByRole('switch', { name: 'New device sign-in' });
    const user = userEvent.setup();
    await user.click(sw);
    expect(onCheckedChange).toHaveBeenLastCalledWith(false);
    expect(sw).toBeChecked();
    expect(sw).toHaveAccessibleDescription('Couldn’t save. Network unavailable.');
    expect(
      screen
        .getAllByRole('status')
        .some((s) => s.textContent === 'Couldn’t save. Network unavailable.'),
    ).toBe(true);
    const retry = screen.getByRole('button', { name: 'Retry New device sign-in' });
    await user.click(retry);
    expect(onCheckedChange).toHaveBeenLastCalledWith(false);
    expect(sw).not.toBeChecked();
    expect(screen.queryByRole('button', { name: /Retry/ })).toBeNull();
    expect(onStatusChange.mock.calls.map((c) => c[0])).toEqual([
      'saving',
      'error',
      'saving',
      'saved',
    ]);
    await expectNoA11yViolations();
  });

  it('controlled: follows the parent, keeps the optimistic value until the parent catches up', async () => {
    function Controlled() {
      const [on, setOn] = useState(false);
      return (
        <>
          <SwitchSettingsList title="Privacy">
            <SwitchSettingsItem
              label="Share usage data"
              checked={on}
              onCheckedChange={async (next) => {
                await Promise.resolve();
                setOn(next);
              }}
            />
          </SwitchSettingsList>
          <button type="button" onClick={() => setOn(false)}>
            Server reset
          </button>
        </>
      );
    }
    render(<Controlled />);
    const sw = screen.getByRole('switch', { name: 'Share usage data' });
    const user = userEvent.setup();
    await user.click(sw);
    expect(sw).toBeChecked();
    await user.click(screen.getByRole('button', { name: 'Server reset' }));
    expect(sw).not.toBeChecked();
  });

  it('a synchronous handler toggles without a "Saved" state; disabled rows do not toggle', async () => {
    const onChange = vi.fn();
    render(
      <SwitchSettingsList aria-label="Display">
        <SwitchSettingsItem label="Compact rows" onCheckedChange={onChange} />
        <SwitchSettingsItem label="Beta features" disabled onCheckedChange={onChange} />
      </SwitchSettingsList>,
    );
    expect(screen.getByRole('group', { name: 'Display' })).toBeInTheDocument();
    const user = userEvent.setup();
    const compact = screen.getByRole('switch', { name: 'Compact rows' });
    await user.click(compact);
    expect(compact).toBeChecked();
    expect(compact.closest('li')).toHaveAttribute('data-status', 'idle');
    await user.click(screen.getByRole('switch', { name: 'Beta features' }));
    expect(onChange).toHaveBeenCalledTimes(1);
  });

  it('labels can be translated per list and per row', async () => {
    render(
      <SwitchSettingsList
        title="Benachrichtigungen"
        labels={{ error: 'Nicht gespeichert.', retry: 'Erneut versuchen' }}
        getErrorMessage={() => 'Offline.'}
      >
        <SwitchSettingsItem
          label="Sicherheit"
          labels={{ retry: 'Nochmal' }}
          onCheckedChange={() => Promise.reject(new Error('x'))}
        />
      </SwitchSettingsList>,
    );
    await userEvent.setup().click(screen.getByRole('switch', { name: 'Sicherheit' }));
    expect(screen.getByRole('button', { name: 'Nochmal Sicherheit' })).toBeInTheDocument();
    expect(screen.getByRole('switch')).toHaveAccessibleDescription('Nicht gespeichert. Offline.');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <SwitchSettingsList title="Alerts">
        <SwitchSettingsItem label="Digest" defaultChecked />
      </SwitchSettingsList>,
    );
    expect(html).toContain('mn-switch-settings-list');
    expect(html).toContain('role="switch"');
    expect(html).toContain('aria-checked="true"');
  });
});
