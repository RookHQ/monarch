import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { MyStatus } from './index.js';

const now = new Date('2026-03-02T10:00:00Z');

describe('MyStatus', () => {
  it('is a link named by its text, inviting a status when there is none', async () => {
    render(<MyStatus name="Ada Lovelace" presence="online" href="/preferences" now={now} />);
    const link = screen.getByRole('link', {
      name: 'Your status and settings: Ada Lovelace , Online, Set a status',
    });
    expect(link).toHaveAttribute('href', '/preferences');
    expect(link).toHaveAttribute('data-presence', 'online');
    await expectNoA11yViolations();
  });

  it('shows a custom status until it expires', () => {
    const { rerender } = render(
      <MyStatus
        name="Ada Lovelace"
        presence="away"
        customStatus={{ emoji: '🍽️', text: 'Lunch', until: '2026-03-02T11:00:00Z' }}
        href="/preferences"
        now={now}
      />,
    );
    expect(screen.getByText('🍽️ Lunch')).toBeInTheDocument();
    expect(screen.getByRole('link')).toHaveAttribute('data-has-status');

    rerender(
      <MyStatus
        name="Ada Lovelace"
        presence="away"
        customStatus={{ text: 'Lunch', until: '2026-03-02T09:00:00Z' }}
        href="/preferences"
        now={now}
      />,
    );
    expect(screen.queryByText(/Lunch/)).not.toBeInTheDocument();
    // Without a status, away and do-not-disturb are written out.
    expect(screen.getByRole('link', { name: /Ada Lovelace , Away$/ })).toBeInTheDocument();
  });

  it('is a button with onClick, operable by keyboard', async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    render(<MyStatus name="Ada Lovelace" presence="dnd" onClick={onClick} now={now} />);
    const button = screen.getByRole('button', { name: /Do not disturb/ });
    await user.tab();
    expect(button).toHaveFocus();
    await user.keyboard('{Enter}');
    await user.keyboard(' ');
    expect(onClick).toHaveBeenCalledTimes(2);
    await expectNoA11yViolations();
  });

  it('uses a router link component and translates', () => {
    const Link = vi.fn(({ children, ...p }: { children?: ReactNode; href: string }) => (
      <a data-router="" {...p}>
        {children}
      </a>
    ));
    render(
      <MyStatus
        name="Ada"
        presence="online"
        href="/me"
        linkComponent={Link}
        now={now}
        messages={{ label: 'Votre statut', setStatus: 'Définir un statut', online: 'En ligne' }}
      />,
    );
    const link = screen.getByRole('link', {
      name: 'Votre statut: Ada , En ligne, Définir un statut',
    });
    expect(link).toHaveAttribute('data-router');
  });

  it('renders on the server', () => {
    const html = renderToString(<MyStatus name="Ada" presence="online" href="/me" now={now} />);
    expect(html).toContain('mn-my-status');
    expect(html).toContain('Set a status');
  });
});
