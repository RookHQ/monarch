import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Spinner } from './index.js';

describe('Spinner', () => {
  it('is a status only when labelled', () => {
    const { rerender, container } = render(<Spinner />);
    expect(container.firstChild).toHaveAttribute('aria-hidden', 'true');
    rerender(<Spinner label="Loading tickets" />);
    expect(screen.getByRole('status', { name: 'Loading tickets' })).toBeInTheDocument();
  });

  it('has no axe violations', async () => {
    render(<Spinner label="Loading tickets" />);
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Spinner label="Loading" />);
    expect(html).toContain('mn-spinner');
  });
});
