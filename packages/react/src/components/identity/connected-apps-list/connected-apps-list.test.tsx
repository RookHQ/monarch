import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ConnectedAppsList, type ConnectedApp } from './index.js';

const NOW = '2026-03-10T12:00:00Z';
const APPS: ConnectedApp[] = [
  {
    id: 'app1',
    name: 'Notes',
    domain: 'notes.example.com',
    logoSrc: 'https://notes.example.com/logo.png',
    verified: true,
    workspace: 'Design team',
    scopes: ['See your name and profile picture', 'See your email address'],
    connectedAt: '2026-02-10T12:00:00Z',
    lastUsedAt: '2026-03-10T10:00:00Z',
  },
  {
    id: 'app2',
    name: 'Planner',
    logoSrc: 'javascript:alert(1)',
    suspended: true,
  },
];

describe('ConnectedAppsList', () => {
  it('lists apps with badges, domain, times, workspace and scopes', async () => {
    render(<ConnectedAppsList data-testid="root" apps={APPS} now={NOW} locale="en" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-connected-apps-list');
    const list = screen.getByRole('list', { name: 'Connected apps' });
    const rows = within(list)
      .getAllByRole('listitem')
      .filter((li) => li.parentElement === list);
    expect(rows).toHaveLength(2);
    expect(rows[0]).toHaveTextContent('Verified');
    expect(rows[0]).toHaveTextContent('notes.example.com');
    expect(rows[0]).toHaveTextContent('Last used 2 hours ago');
    expect(rows[0]).toHaveTextContent('Workspace: Design team');
    const scopes = screen.getByRole('list', { name: 'What Notes can access' });
    expect(within(scopes).getAllByRole('listitem')).toHaveLength(2);
    expect(rows[1]).toHaveTextContent('Suspended');
    expect(rows[1]).toHaveAttribute('data-suspended', 'true');
    expect(rows[1]!.querySelector('img')).toBeNull();
    await expectNoA11yViolations();
  });

  it('removes access after confirming and announces it', async () => {
    const user = userEvent.setup();
    const onRevoke = vi.fn(() => Promise.resolve());
    render(<ConnectedAppsList apps={APPS} onRevoke={onRevoke} />);
    const button = screen.getByRole('button', { name: 'Remove access for Notes' });
    await user.click(button);
    const dialog = screen.getByRole('alertdialog', { name: 'Remove access for Notes?' });
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus();
    await expectNoA11yViolations();
    await user.click(within(dialog).getByRole('button', { name: 'Remove access' }));
    expect(onRevoke).toHaveBeenCalledWith('app1');
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    expect(screen.getByRole('status')).toHaveTextContent('Removed access for Notes');
    await waitFor(() => expect(button).toHaveFocus());
  });

  it('keeps the dialog open with the error when removal fails', async () => {
    const user = userEvent.setup();
    render(
      <ConnectedAppsList apps={APPS} onRevoke={() => Promise.reject(new Error('Try later'))} />,
    );
    await user.click(screen.getByRole('button', { name: 'Remove access for Planner' }));
    await user.click(screen.getByRole('button', { name: 'Remove access' }));
    expect(await screen.findByText('Try later')).toBeInTheDocument();
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
  });

  it('calls straight through when confirm is false', async () => {
    const user = userEvent.setup();
    const onRevoke = vi.fn();
    render(<ConnectedAppsList apps={APPS} onRevoke={onRevoke} confirm={false} />);
    await user.click(screen.getByRole('button', { name: 'Remove access for Planner' }));
    expect(onRevoke).toHaveBeenCalledWith('app2');
    expect(screen.queryByRole('alertdialog')).toBeNull();
  });

  it('shows loading, empty and error states', () => {
    const { rerender } = render(<ConnectedAppsList apps={[]} state="loading" />);
    expect(screen.getByRole('region', { name: 'Connected apps' })).toHaveAttribute(
      'aria-busy',
      'true',
    );
    rerender(<ConnectedAppsList apps={[]} />);
    expect(screen.getByText('No connected apps')).toBeInTheDocument();
    rerender(<ConnectedAppsList apps={[]} state="error" onRetry={() => undefined} />);
    expect(screen.getByText('Couldn’t load your connected apps')).toBeInTheDocument();
  });

  it('takes translated messages', () => {
    render(
      <ConnectedAppsList
        apps={APPS}
        onRevoke={() => undefined}
        messages={{ title: 'Verbundene Apps', revoke: 'Zugriff entfernen' }}
      />,
    );
    expect(screen.getByRole('heading', { name: 'Verbundene Apps' })).toBeInTheDocument();
    expect(screen.getAllByText('Zugriff entfernen')).toHaveLength(2);
  });

  it('renders on the server', () => {
    const html = renderToString(<ConnectedAppsList apps={APPS} now={NOW} locale="en" />);
    expect(html).toContain('mn-connected-apps-list');
    expect(html).toContain('notes.example.com');
  });
});
