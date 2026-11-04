import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Checkbox } from './index.js';

describe('Checkbox', () => {
  it('toggles with Space and click, and labels itself', async () => {
    render(<Checkbox label="Email me updates" description="About once a month." />);
    const box = screen.getByRole('checkbox', { name: 'Email me updates' });
    expect(box).toHaveAccessibleDescription('About once a month.');
    const user = userEvent.setup();
    await user.click(box);
    expect(box).toBeChecked();
    await user.keyboard(' ');
    expect(box).not.toBeChecked();
    await user.click(screen.getByText('Email me updates'));
    expect(box).toBeChecked();
    await expectNoA11yViolations();
  });

  it('supports indeterminate', () => {
    render(<Checkbox aria-label="Select all" checked="indeterminate" />);
    expect(screen.getByRole('checkbox', { name: 'Select all' })).toHaveAttribute(
      'aria-checked',
      'mixed',
    );
  });

  it('renders on the server', () => {
    const html = renderToString(<Checkbox label="Agree" />);
    expect(html).toContain('mn-checkbox');
  });
});
