import { createRef, useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { CalloutBanner } from './index.js';

describe('CalloutBanner', () => {
  it('renders title, body, icon and actions with tone data attributes', async () => {
    render(
      <CalloutBanner
        data-testid="root"
        tone="warning"
        title="Your trial ends in 3 days"
        actions={<Button size="sm">Choose a plan</Button>}
      >
        Add a payment method to keep your workspace.
      </CalloutBanner>,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-callout-banner');
    expect(root).toHaveAttribute('data-tone', 'warning');
    expect(root).toHaveAttribute('data-placement', 'section');
    expect(root).toHaveAttribute('data-variant', 'callout');
    expect(root.tagName).toBe('DIV');
    expect(root).not.toHaveAttribute('role');
    expect(screen.getByText('Your trial ends in 3 days')).toHaveClass('mn-callout-banner__title');
    expect(root.querySelector('.mn-callout-banner__icon')).toHaveAttribute('aria-hidden');
    expect(screen.getByRole('button', { name: 'Choose a plan' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('forwards the ref and native attributes', () => {
    const ref = createRef<HTMLElement>();
    render(<CalloutBanner ref={ref} id="b" className="extra" title="Hi" />);
    expect(ref.current).toHaveAttribute('id', 'b');
    expect(ref.current).toHaveClass('mn-callout-banner', 'extra');
  });

  it.each([
    ['region', 'SECTION'],
    ['complementary', 'ASIDE'],
  ] as const)('landmark="%s" is a named landmark', async (landmark, tag) => {
    render(
      <main>
        <CalloutBanner landmark={landmark} title="Scheduled maintenance">
          Sign-in is unavailable on Sunday from 02:00 to 03:00 UTC.
        </CalloutBanner>
      </main>,
    );
    const lm = screen.getByRole(landmark, { name: 'Scheduled maintenance' });
    expect(lm.tagName).toBe(tag);
    await expectNoA11yViolations();
  });

  it('a landmark without a title is named by aria-label', () => {
    render(<CalloutBanner landmark="region" aria-label="Billing" />);
    expect(screen.getByRole('region', { name: 'Billing' })).toBeInTheDocument();
  });

  it('puts the live role on the text, so a landmark keeps its role', async () => {
    render(
      <CalloutBanner landmark="region" live="assertive" tone="danger" title="Sync failed">
        The registry did not respond.
      </CalloutBanner>,
    );
    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Sync failed');
    expect(screen.getByRole('region', { name: 'Sync failed' })).toContainElement(alert);
    await expectNoA11yViolations();
  });

  it('live="polite" is a status', () => {
    render(<CalloutBanner live="polite" title="Saved" />);
    expect(screen.getByRole('status')).toHaveTextContent('Saved');
  });

  it('dismisses (uncontrolled), calls onDismiss and moves focus to the next control', async () => {
    const onDismiss = vi.fn();
    render(
      <>
        <CalloutBanner title="New: shared views" onDismiss={onDismiss} />
        <button type="button">Next</button>
      </>,
    );
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Dismiss' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onDismiss).toHaveBeenCalledOnce();
    expect(screen.queryByText('New: shared views')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Next' })).toHaveFocus();
  });

  it('moves focus to the previous control when nothing follows', async () => {
    render(
      <>
        <button type="button">Before</button>
        <CalloutBanner title="Heads up" dismissible />
      </>,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Dismiss' }));
    expect(screen.getByRole('button', { name: 'Before' })).toHaveFocus();
  });

  it('controlled: stays until the parent closes it', async () => {
    function Controlled() {
      const [open, setOpen] = useState(true);
      const [count, setCount] = useState(0);
      return (
        <>
          <CalloutBanner
            title="Maintenance"
            open={open}
            onDismiss={() => {
              setCount((c) => c + 1);
              if (count >= 1) setOpen(false);
            }}
          />
          <span>Dismissed {count}</span>
        </>
      );
    }
    render(<Controlled />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Dismiss' }));
    expect(screen.getByText('Maintenance')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Dismiss' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Dismiss' }));
    expect(screen.queryByText('Maintenance')).not.toBeInTheDocument();
    expect(screen.getByText('Dismissed 2')).toBeInTheDocument();
  });

  it('defaultOpen={false} renders nothing (product already stored the dismissal)', () => {
    const { container } = render(<CalloutBanner title="Hidden" defaultOpen={false} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('uses a custom dismiss label', () => {
    render(<CalloutBanner title="Hi" dismissible dismissLabel="Hide announcement" />);
    expect(screen.getByRole('button', { name: 'Hide announcement' })).toBeInTheDocument();
  });

  it('scope variant: compact, no icon by default, inline action', async () => {
    const onClear = vi.fn();
    render(
      <CalloutBanner
        data-testid="scope"
        variant="scope"
        tone="neutral"
        live="polite"
        actionPlacement="inline"
        actions={
          <Button size="xs" variant="ghost" onClick={onClear}>
            Show all Spaces
          </Button>
        }
      >
        Showing tickets in Support.
      </CalloutBanner>,
    );
    const root = screen.getByTestId('scope');
    expect(root).toHaveAttribute('data-variant', 'scope');
    expect(root).toHaveAttribute('data-action-placement', 'inline');
    expect(root.querySelector('.mn-callout-banner__icon')).toBeNull();
    // Inline actions sit with the text, not on the trailing edge.
    expect(
      root.querySelector('.mn-callout-banner__main .mn-callout-banner__actions'),
    ).not.toBeNull();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Show all Spaces' }));
    expect(onClear).toHaveBeenCalledOnce();
    await expectNoA11yViolations();
  });

  it('icon can be replaced or removed', () => {
    const { rerender, container } = render(
      <CalloutBanner title="A" icon={<svg data-testid="custom" />} />,
    );
    expect(screen.getByTestId('custom')).toBeInTheDocument();
    rerender(<CalloutBanner title="A" icon={null} />);
    expect(container.querySelector('.mn-callout-banner__icon')).toBeNull();
  });

  it('page placement', () => {
    render(<CalloutBanner data-testid="p" placement="page" tone="accent" title="Trial" />);
    expect(screen.getByTestId('p')).toHaveAttribute('data-placement', 'page');
    expect(screen.getByTestId('p')).toHaveAttribute('data-tone', 'accent');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <CalloutBanner landmark="region" title="Billing" onDismiss={() => {}}>
        Your card was declined.
      </CalloutBanner>,
    );
    expect(html).toContain('mn-callout-banner');
    expect(html).toContain('<section');
    expect(html).toContain('aria-labelledby');
  });
});
