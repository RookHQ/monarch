import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Switch } from './index.js';

describe('Switch', () => {
  it('toggles and exposes switch semantics', async () => {
    render(<Switch label="Two-step sign-in" />);
    const sw = screen.getByRole('switch', { name: 'Two-step sign-in' });
    const user = userEvent.setup();
    await user.click(sw);
    expect(sw).toHaveAttribute('aria-checked', 'true');
    await user.keyboard(' ');
    expect(sw).toHaveAttribute('aria-checked', 'false');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Switch label="On" />);
    expect(html).toContain('mn-switch');
  });
});
