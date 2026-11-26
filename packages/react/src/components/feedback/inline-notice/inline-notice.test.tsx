import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { InlineNotice } from './index.js';

describe('InlineNotice', () => {
  it('renders the message with tone, variant and a decorative icon', async () => {
    render(
      <InlineNotice data-testid="n" tone="warning" title="Unverified.">
        Emails from this domain may land in spam.
      </InlineNotice>,
    );
    const n = screen.getByTestId('n');
    expect(n).toHaveClass('mn-inline-notice');
    expect(n).toHaveAttribute('data-tone', 'warning');
    expect(n).toHaveAttribute('data-variant', 'subtle');
    expect(n).not.toHaveAttribute('role');
    expect(n.querySelector('.mn-inline-notice__message')).toHaveTextContent(
      'Unverified. Emails from this domain may land in spam.',
    );
    expect(n.querySelector('.mn-inline-notice__icon')).toHaveAttribute('aria-hidden');
    await expectNoA11yViolations();
  });

  it('forwards the ref and native attributes', () => {
    const ref = createRef<HTMLDivElement>();
    render(<InlineNotice ref={ref} id="x" className="extra" />);
    expect(ref.current).toHaveAttribute('id', 'x');
    expect(ref.current).toHaveClass('mn-inline-notice', 'extra');
  });

  it.each([
    [true, 'danger', 'alert'],
    [true, 'warning', 'status'],
    [true, 'info', 'status'],
    ['assertive', 'info', 'alert'],
    ['polite', 'danger', 'status'],
  ] as const)('live=%s with tone %s is role %s', (live, tone, role) => {
    render(
      <InlineNotice live={live} tone={tone}>
        Message
      </InlineNotice>,
    );
    expect(screen.getByRole(role)).toHaveTextContent('Message');
  });

  it('renders the trailing action and runs it', async () => {
    const onRetry = vi.fn();
    render(
      <InlineNotice
        tone="danger"
        action={
          <Button size="xs" onClick={onRetry}>
            Retry
          </Button>
        }
      >
        Could not load comments.
      </InlineNotice>,
    );
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Retry' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onRetry).toHaveBeenCalledOnce();
    await expectNoA11yViolations();
  });

  it('dismisses with the keyboard and keeps focus on the page', async () => {
    const onDismiss = vi.fn();
    render(
      <>
        <InlineNotice onDismiss={onDismiss}>Draft restored.</InlineNotice>
        <input aria-label="Subject" />
      </>,
    );
    const user = userEvent.setup();
    await user.tab();
    await user.keyboard(' ');
    expect(onDismiss).toHaveBeenCalledOnce();
    expect(screen.queryByText('Draft restored.')).not.toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Subject' })).toHaveFocus();
  });

  it('controlled open wins over dismiss', async () => {
    const onDismiss = vi.fn();
    const { rerender } = render(
      <InlineNotice open onDismiss={onDismiss}>
        Pinned
      </InlineNotice>,
    );
    await userEvent.setup().click(screen.getByRole('button', { name: 'Dismiss' }));
    expect(onDismiss).toHaveBeenCalledOnce();
    expect(screen.getByText('Pinned')).toBeInTheDocument();
    rerender(
      <InlineNotice open={false} onDismiss={onDismiss}>
        Pinned
      </InlineNotice>,
    );
    expect(screen.queryByText('Pinned')).not.toBeInTheDocument();
  });

  it('plain variant, custom icon and no icon', () => {
    const { rerender, container } = render(
      <InlineNotice variant="plain" tone="success" icon={<svg data-testid="i" />}>
        Connected
      </InlineNotice>,
    );
    expect(container.firstChild).toHaveAttribute('data-variant', 'plain');
    expect(screen.getByTestId('i')).toBeInTheDocument();
    rerender(<InlineNotice icon={null}>Connected</InlineNotice>);
    expect(container.querySelector('.mn-inline-notice__icon')).toBeNull();
  });

  it('dismissible without a callback still closes, with a custom label', async () => {
    render(
      <InlineNotice dismissible dismissLabel="Hide tip">
        Tip
      </InlineNotice>,
    );
    await userEvent.setup().click(screen.getByRole('button', { name: 'Hide tip' }));
    expect(screen.queryByText('Tip')).not.toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <InlineNotice tone="danger" live>
        Payment failed
      </InlineNotice>,
    );
    expect(html).toContain('mn-inline-notice');
    expect(html).toContain('role="alert"');
  });
});
