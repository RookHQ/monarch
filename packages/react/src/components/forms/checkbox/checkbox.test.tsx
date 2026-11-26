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

  it('draws a dash for an uncontrolled indeterminate default, then a check once ticked', async () => {
    render(<Checkbox aria-label="Select all" defaultChecked="indeterminate" />);
    const box = screen.getByRole('checkbox', { name: 'Select all' });
    expect(box).toHaveAttribute('aria-checked', 'mixed');
    // The dash glyph is one horizontal stroke; the check has a V in its path.
    const path = () => box.querySelector('svg path')!.getAttribute('d')!;
    const dash = path();
    await userEvent.setup().click(box);
    expect(box).toHaveAttribute('aria-checked', 'true');
    expect(path()).not.toBe(dash);
    const { container } = render(<Checkbox aria-label="Other" defaultChecked />);
    expect(container.querySelector('svg path')!.getAttribute('d')).toBe(path());
    expect(container.querySelector('svg path')!.getAttribute('d')).not.toBe(dash);
  });

  it('renders on the server', () => {
    const html = renderToString(<Checkbox label="Agree" />);
    expect(html).toContain('mn-checkbox');
  });
});
