import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { HoverCard, HoverCardContent, HoverCardTrigger, type HoverCardProps } from './index.js';

// jsdom has no PointerEvent, so `pointerType` would be dropped from fired events.
if (typeof window.PointerEvent === 'undefined') {
  class PointerEventPolyfill extends MouseEvent {
    pointerType: string;
    constructor(type: string, init: PointerEventInit = {}) {
      super(type, init);
      this.pointerType = init.pointerType ?? 'mouse';
    }
  }
  window.PointerEvent = PointerEventPolyfill as unknown as typeof PointerEvent;
}

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

function Preview(props: Partial<HoverCardProps>) {
  return (
    <>
      <HoverCard openDelay={40} closeDelay={40} {...props}>
        <HoverCardTrigger>
          <a href="#ana">Ana Ruiz</a>
        </HoverCardTrigger>
        <HoverCardContent>
          <p>Design lead, Lisbon</p>
          <a href="#profile">View profile</a>
        </HoverCardContent>
      </HoverCard>
      <button type="button">Next</button>
    </>
  );
}

describe('HoverCard', () => {
  it('opens after the delay on hover, named by its trigger, and closes on leave', async () => {
    const onOpenChange = vi.fn();
    render(<Preview onOpenChange={onOpenChange} />);
    const link = screen.getByRole('link', { name: 'Ana Ruiz' });
    fireEvent.pointerEnter(link, { pointerType: 'mouse' });
    expect(screen.queryByRole('dialog')).toBeNull();
    const card = await screen.findByRole('dialog', { name: 'Ana Ruiz' });
    expect(card).toHaveClass('mn-popover', 'mn-hover-card');
    expect(link).toHaveAccessibleDescription(/Design lead, Lisbon/);
    expect(document.activeElement).not.toBe(card);
    await expectNoA11yViolations();
    fireEvent.pointerLeave(link, { pointerType: 'mouse' });
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(onOpenChange.mock.calls).toEqual([[true], [false]]);
  });

  it('stays open while the pointer moves onto the card', async () => {
    render(<Preview />);
    const link = screen.getByRole('link', { name: 'Ana Ruiz' });
    fireEvent.pointerEnter(link, { pointerType: 'mouse' });
    const card = await screen.findByRole('dialog');
    fireEvent.pointerLeave(link, { pointerType: 'mouse' });
    fireEvent.pointerEnter(card, { pointerType: 'mouse' });
    await wait(80);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    fireEvent.pointerLeave(card, { pointerType: 'mouse' });
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('does not open for a quick pass of the pointer', async () => {
    render(<Preview openDelay={60} />);
    const link = screen.getByRole('link', { name: 'Ana Ruiz' });
    fireEvent.pointerEnter(link, { pointerType: 'mouse' });
    await wait(20);
    fireEvent.pointerLeave(link, { pointerType: 'mouse' });
    await wait(80);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('opens on keyboard focus without taking it; Tab enters the card, Escape closes', async () => {
    render(<Preview />);
    const user = userEvent.setup();
    await user.tab();
    const link = screen.getByRole('link', { name: 'Ana Ruiz' });
    expect(link).toHaveFocus();
    await screen.findByRole('dialog', { name: 'Ana Ruiz' });
    expect(link).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('link', { name: 'View profile' })).toHaveFocus();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(link).toHaveFocus();
  });

  it('closes when focus moves past the card', async () => {
    render(<Preview />);
    const user = userEvent.setup();
    await user.tab();
    await screen.findByRole('dialog');
    await user.tab();
    await user.tab();
    // Leaving the last link returns to the trigger and closes; the next Tab moves on.
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    await user.tab();
    expect(screen.getByRole('button', { name: 'Next' })).toHaveFocus();
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('never opens on touch', async () => {
    render(<Preview />);
    const link = screen.getByRole('link', { name: 'Ana Ruiz' });
    fireEvent.pointerEnter(link, { pointerType: 'touch' });
    fireEvent.pointerDown(link, { pointerType: 'touch' });
    link.focus();
    await wait(80);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<Preview />);
    expect(html).toContain('Ana Ruiz');
    expect(html).not.toContain('Design lead');
  });
});
