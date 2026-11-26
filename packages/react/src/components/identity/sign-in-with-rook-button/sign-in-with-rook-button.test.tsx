import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SignInWithRookButton } from './index.js';

describe('SignInWithRookButton', () => {
  it('is a named button that pins its theme', async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    const ref = createRef<HTMLButtonElement | HTMLAnchorElement>();
    render(<SignInWithRookButton ref={ref} onClick={onClick} markSrc="/mark.png" />);
    const button = screen.getByRole('button', { name: 'Sign in with Rook ID' });
    expect(button).toBe(ref.current);
    expect(button).toHaveClass('mn-sign-in-with-rook-button');
    expect(button).toHaveAttribute('type', 'button');
    expect(button).toHaveAttribute('data-variant', 'light');
    expect(button).toHaveAttribute('data-mn-theme', 'light');
    expect(button.querySelector('img')).toHaveAttribute('alt', '');
    button.focus();
    await user.keyboard('{Enter}');
    expect(onClick).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it('maps variants to themes and sizes to data attributes', () => {
    const { rerender } = render(<SignInWithRookButton variant="dark" size="lg" />);
    const button = screen.getByRole('button');
    expect(button).toHaveAttribute('data-mn-theme', 'dark');
    expect(button).toHaveAttribute('data-size', 'lg');
    rerender(<SignInWithRookButton variant="auto" />);
    expect(button).not.toHaveAttribute('data-mn-theme');
    rerender(<SignInWithRookButton variant="neutral" />);
    expect(button).toHaveAttribute('data-mn-theme', 'light');
  });

  it('renders a link when given href', async () => {
    render(<SignInWithRookButton href="/login" label="Continue with Rook ID" />);
    const link = screen.getByRole('link', { name: 'Continue with Rook ID' });
    expect(link).toHaveAttribute('href', '/login');
    await expectNoA11yViolations();
  });

  it('blocks clicks while loading and marks it busy', async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    render(<SignInWithRookButton loading onClick={onClick} />);
    const button = screen.getByRole('button', { name: /Sign in with Rook ID/ });
    expect(button).toHaveAttribute('aria-busy', 'true');
    await user.click(button);
    expect(onClick).not.toHaveBeenCalled();
  });

  it('takes a custom mark element', () => {
    render(<SignInWithRookButton mark={<svg data-testid="mark" />} />);
    expect(screen.getByTestId('mark').parentElement).toHaveAttribute('aria-hidden', 'true');
  });

  it('renders on the server', () => {
    const html = renderToString(<SignInWithRookButton />);
    expect(html).toContain('mn-sign-in-with-rook-button');
    expect(html).toContain('Sign in with Rook ID');
  });
});
