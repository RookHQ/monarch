import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ImpersonationBanner } from './index.js';

const NOW = '2026-03-10T12:00:00Z';

describe('ImpersonationBanner', () => {
  it('announces who is being viewed as, with details and a named Stop button', async () => {
    render(
      <ImpersonationBanner
        name="Sam Rivera"
        detail="sam@example.com"
        reason="Case 4821"
        startedAt="2026-03-10T11:55:00Z"
        now={NOW}
        locale="en"
        onStop={() => undefined}
      />,
    );
    const banner = screen.getByRole('status');
    expect(banner).toHaveClass('mn-impersonation-banner');
    expect(banner).toHaveAttribute('data-sticky', 'true');
    expect(banner).toHaveTextContent('You are viewing as Sam Rivera (sam@example.com)');
    expect(banner).toHaveTextContent('Started 5 minutes ago');
    expect(banner).toHaveTextContent('Reason: Case 4821');
    expect(screen.getByRole('button', { name: 'Stop viewing as Sam Rivera' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /dismiss/i })).toBeNull();
    await expectNoA11yViolations();
  });

  it('stops from the keyboard and shows the pending state while the promise runs', async () => {
    const user = userEvent.setup();
    let finish: () => void = () => undefined;
    const onStop = vi.fn(() => new Promise<void>((resolve) => (finish = resolve)));
    render(<ImpersonationBanner name="Sam Rivera" onStop={onStop} />);
    await user.tab();
    const button = screen.getByRole('button', { name: 'Stop viewing as Sam Rivera' });
    expect(button).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onStop).toHaveBeenCalledOnce();
    expect(button).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('status')).toHaveAttribute('data-stopping', 'true');
    await user.keyboard('{Enter}');
    expect(onStop).toHaveBeenCalledOnce();
    finish();
    await waitFor(() => expect(button).not.toHaveAttribute('aria-busy', 'true'));
  });

  it('shows the error when stopping fails', async () => {
    const user = userEvent.setup();
    render(
      <ImpersonationBanner name="Sam" onStop={() => Promise.reject(new Error('Session lost'))} />,
    );
    await user.click(screen.getByRole('button', { name: 'Stop viewing as Sam' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Session lost');
  });

  it('takes translated messages and can be non-sticky', () => {
    render(
      <ImpersonationBanner
        name="Sam"
        sticky={false}
        stopping
        onStop={() => undefined}
        messages={{
          viewingAs: (n) => <>Ansicht als {n}</>,
          stop: 'Beenden',
          stopLabel: (n) => `Ansicht als ${n} beenden`,
        }}
      />,
    );
    const banner = screen.getByRole('status');
    expect(banner).not.toHaveAttribute('data-sticky');
    expect(banner).toHaveTextContent('Ansicht als Sam');
    expect(screen.getByRole('button', { name: 'Ansicht als Sam beenden' })).toHaveAttribute(
      'aria-busy',
      'true',
    );
  });

  it('renders on the server', () => {
    const html = renderToString(
      <ImpersonationBanner name="Sam" startedAt={NOW} now={NOW} onStop={() => undefined} />,
    );
    expect(html).toContain('mn-impersonation-banner');
    expect(html).toContain('role="status"');
  });
});
