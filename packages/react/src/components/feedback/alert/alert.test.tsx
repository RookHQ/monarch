import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Alert } from './index.js';

describe('Alert', () => {
  it('announces only when live', () => {
    const { rerender } = render(<Alert tone="warning" title="Sync paused" />);
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    rerender(<Alert tone="danger" title="Payment failed" live="assertive" />);
    expect(screen.getByRole('alert')).toHaveTextContent('Payment failed');
  });

  it('has no axe violations', async () => {
    render(
      <main>
        <Alert tone="info" title="Sync scheduled">
          Members refresh every 15 minutes.
        </Alert>
      </main>,
    );
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Alert title="Heads up" />);
    expect(html).toContain('mn-alert');
  });
});
