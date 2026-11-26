import { createRef, useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DangerZoneItem } from '../danger-zone/index.js';
import { SettingsSection } from '../settings-section/index.js';
import type { UnsavedChangesGuardHandle } from '../../../components/overlays/unsaved-changes-guard/index.js';
import { SettingsPage } from './index.js';

const nav = [
  { id: 'general', label: 'General' },
  { id: 'notifications', label: 'Notifications' },
];

function Page(props: Partial<Parameters<typeof SettingsPage>[0]>) {
  return (
    <SettingsPage title="Workspace settings" description="Sample settings." nav={nav} {...props}>
      <SettingsSection id="general" title="General">
        <p>General rows</p>
      </SettingsSection>
      <SettingsSection id="notifications" title="Notifications">
        <p>Notification rows</p>
      </SettingsSection>
    </SettingsPage>
  );
}

describe('SettingsPage', () => {
  it('renders the title, a named section nav and the sections', async () => {
    render(<Page data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-settings-page');
    expect(screen.getByRole('heading', { level: 1, name: 'Workspace settings' })).toBeVisible();
    const navigation = screen.getByRole('navigation', { name: 'Settings sections' });
    const links = within(navigation).getAllByRole('link');
    expect(links.map((l) => l.getAttribute('href'))).toEqual(['#general', '#notifications']);
    expect(links[0]).toHaveAttribute('aria-current', 'location');
    expect(screen.getByRole('region', { name: 'Notifications' })).toBeVisible();
    await expectNoA11yViolations();
  });

  it('moves the current marker when a nav link is chosen by keyboard', async () => {
    const user = userEvent.setup();
    const onActiveSectionChange = vi.fn();
    render(<Page onActiveSectionChange={onActiveSectionChange} />);
    await user.tab();
    await user.tab();
    const link = screen.getByRole('link', { name: 'Notifications' });
    expect(link).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onActiveSectionChange).toHaveBeenCalledWith('notifications');
    expect(link).toHaveAttribute('aria-current', 'location');
    expect(screen.getByRole('link', { name: 'General' })).not.toHaveAttribute('aria-current');
  });

  it('wraps danger rows in a DangerZone and links to it; unsafe hrefs fall back to the id', () => {
    render(
      <Page
        nav={[...nav, { id: 'billing', label: 'Billing', href: 'javascript:alert(1)' }]}
        dangerZone={
          <DangerZoneItem
            title="Delete this workspace"
            action={<button type="button">Delete workspace</button>}
          />
        }
      />,
    );
    expect(screen.getByRole('link', { name: 'Billing' })).toHaveAttribute('href', '#billing');
    const dangerLink = screen.getByRole('link', { name: 'Danger zone' });
    expect(dangerLink).toHaveAttribute('href', '#danger-zone');
    const zone = screen.getByRole('region', { name: 'Danger zone' });
    expect(zone).toHaveAttribute('id', 'danger-zone');
    expect(within(zone).getByRole('group', { name: 'Delete this workspace' })).toBeVisible();
  });

  it('shows the save bar only while dirty and runs Save and Discard', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    const onDiscard = vi.fn();
    const { rerender } = render(<Page onSave={onSave} onDiscard={onDiscard} />);
    expect(screen.queryByRole('region', { name: 'Unsaved changes' })).toBeNull();
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
    rerender(<Page dirty onSave={onSave} onDiscard={onDiscard} />);
    const bar = screen.getByRole('region', { name: 'Unsaved changes' });
    expect(screen.getByRole('status')).toHaveTextContent('You have unsaved changes.');
    await user.click(within(bar).getByRole('button', { name: 'Save changes' }));
    expect(onSave).toHaveBeenCalledTimes(1);
    await user.click(within(bar).getByRole('button', { name: 'Discard' }));
    expect(onDiscard).toHaveBeenCalledTimes(1);
    rerender(<Page dirty saving onSave={onSave} onDiscard={onDiscard} />);
    expect(screen.getByRole('button', { name: 'Discard' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('keeps focus on the page when Save removes the bar, and swallows a rejected save', async () => {
    const unhandled = vi.fn();
    process.on('unhandledRejection', unhandled);
    const user = userEvent.setup();
    function Editing() {
      const [dirty, setDirty] = useState(true);
      return (
        <Page
          dirty={dirty}
          onSave={() => {
            setDirty(false);
            return Promise.reject(new Error('offline'));
          }}
        />
      );
    }
    render(<Editing />);
    await user.click(screen.getByRole('button', { name: 'Save changes' }));
    expect(screen.queryByRole('region', { name: 'Unsaved changes' })).toBeNull();
    // Not on <body>: the next Tab continues from the settings content.
    expect(document.activeElement).toBe(document.querySelector('.mn-settings-page__content'));
    await new Promise((resolve) => setImmediate(resolve));
    process.off('unhandledRejection', unhandled);
    expect(unhandled).not.toHaveBeenCalled();
  });

  it('guards the app’s own navigation while dirty', async () => {
    const user = userEvent.setup();
    const guardRef = createRef<UnsavedChangesGuardHandle>();
    const proceed = vi.fn();
    render(<Page dirty onSave={() => {}} guardRef={guardRef} />);
    await waitFor(() => expect(guardRef.current).not.toBeNull());
    const result = guardRef.current!.guard(proceed);
    const dialog = await screen.findByRole('alertdialog', { name: 'Discard unsaved changes?' });
    expect(proceed).not.toHaveBeenCalled();
    await user.click(within(dialog).getByRole('button', { name: 'Discard changes' }));
    await expect(result).resolves.toBe(true);
    expect(proceed).toHaveBeenCalledTimes(1);
  });

  it('accepts a custom header, translated text and no nav; forwards the ref', () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <SettingsPage
        ref={ref}
        title="unused"
        header={<h1>Ajustes</h1>}
        dirty
        onSave={() => {}}
        messages={{ save: 'Guardar', saveBarLabel: 'Cambios sin guardar' }}
      >
        <p>Body</p>
      </SettingsPage>,
    );
    expect(ref.current).toHaveClass('mn-settings-page');
    expect(screen.queryByRole('navigation')).toBeNull();
    expect(screen.getByRole('heading', { name: 'Ajustes' })).toBeVisible();
    expect(screen.getByRole('region', { name: 'Cambios sin guardar' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeVisible();
  });

  it('renders on the server', () => {
    const html = renderToString(<Page dirty onSave={() => {}} />);
    expect(html).toContain('mn-settings-page');
    expect(html).toContain('Settings sections');
  });
});
