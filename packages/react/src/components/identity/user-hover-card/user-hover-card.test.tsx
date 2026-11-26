import { describe, expect, it, vi } from 'vitest';
import { act, createEvent, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { UserHoverCard, type UserHoverCardUser } from './index.js';

const USER: UserHoverCardUser = {
  id: 'u-1',
  name: 'Sam Rivera',
  handle: '@sam',
  title: 'Design lead',
  status: 'online',
  statusText: 'Working from the north office',
};

function Demo(props: Partial<Parameters<typeof UserHoverCard>[0]>) {
  return (
    <>
      <UserHoverCard
        user={USER}
        openDelay={0}
        closeDelay={0}
        actions={<button type="button">Message</button>}
        {...props}
      >
        <a href="#sam">Sam Rivera</a>
      </UserHoverCard>
      <button type="button">After</button>
    </>
  );
}

describe('UserHoverCard', () => {
  it('opens on keyboard focus without moving focus, and closes on Escape', async () => {
    const user = userEvent.setup();
    render(<Demo />);
    await user.tab();
    const link = screen.getByRole('link', { name: 'Sam Rivera' });
    expect(link).toHaveFocus();
    const card = await screen.findByRole('dialog', { name: 'Profile of Sam Rivera' });
    expect(card).toHaveClass('mn-user-hover-card');
    expect(card).toHaveTextContent('@sam');
    expect(card).toHaveTextContent('Design lead');
    expect(card).toHaveTextContent('Working from the north office');
    expect(link).toHaveAttribute('aria-describedby');
    expect(link).toHaveFocus();
    await expectNoA11yViolations();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(link).toHaveFocus();
  });

  it('Tab moves into the card actions and out again to the trigger', async () => {
    const user = userEvent.setup();
    render(<Demo />);
    await user.tab();
    await screen.findByRole('dialog');
    await user.tab();
    expect(screen.getByRole('button', { name: 'Message' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('link', { name: 'Sam Rivera' })).toHaveFocus();
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('opens after the hover delay and closes when the pointer leaves', async () => {
    vi.useFakeTimers();
    try {
      const onOpenChange = vi.fn();
      render(<Demo openDelay={400} closeDelay={100} onOpenChange={onOpenChange} />);
      const link = screen.getByRole('link', { name: 'Sam Rivera' });
      fireEvent.pointerEnter(link, { pointerType: 'mouse' });
      act(() => vi.advanceTimersByTime(300));
      expect(screen.queryByRole('dialog')).toBeNull();
      act(() => vi.advanceTimersByTime(150));
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(onOpenChange).toHaveBeenLastCalledWith(true);
      fireEvent.pointerLeave(link, { pointerType: 'mouse' });
      fireEvent.pointerEnter(screen.getByRole('dialog'), { pointerType: 'mouse' });
      act(() => vi.advanceTimersByTime(200));
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      fireEvent.pointerLeave(screen.getByRole('dialog'), { pointerType: 'mouse' });
      act(() => vi.advanceTimersByTime(200));
      expect(onOpenChange).toHaveBeenLastCalledWith(false);
    } finally {
      vi.useRealTimers();
    }
  });

  it('shows a loading line and translated messages', async () => {
    render(
      <Demo
        defaultOpen
        loading
        messages={{ loading: 'Profil wird geladen', cardLabel: (n) => `Profil: ${n}` }}
      />,
    );
    const card = await screen.findByRole('dialog', { name: 'Profil: Sam Rivera' });
    expect(card).toHaveAttribute('data-loading', 'true');
    expect(screen.getByRole('status')).toHaveTextContent('Profil wird geladen');
  });

  it('keeps the trigger’s own description, closed and open', () => {
    const { rerender } = render(
      <>
        <span id="own-note">Assigned yesterday</span>
        <UserHoverCard user={USER} open={false}>
          <a href="/people/sam" aria-describedby="own-note">
            Sam
          </a>
        </UserHoverCard>
      </>,
    );
    const link = screen.getByRole('link', { name: 'Sam' });
    expect(link).toHaveAttribute('aria-describedby', 'own-note');
    expect(link).toHaveAccessibleDescription('Assigned yesterday');
    rerender(
      <>
        <span id="own-note">Assigned yesterday</span>
        <UserHoverCard user={USER} open>
          <a href="/people/sam" aria-describedby="own-note">
            Sam
          </a>
        </UserHoverCard>
      </>,
    );
    expect(link.getAttribute('aria-describedby')).toMatch(/^own-note \S+/);
  });

  it('renders details and respects the controlled open prop', () => {
    const { rerender } = render(<Demo open={false} details={<span>Team: Platform</span>} />);
    expect(screen.queryByRole('dialog')).toBeNull();
    rerender(<Demo open details={<span>Team: Platform</span>} />);
    expect(screen.getByRole('dialog')).toHaveTextContent('Team: Platform');
  });

  it('opens at once on keyboard focus despite the hover delay, but not after a tap', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(<Demo openDelay={5000} onOpenChange={onOpenChange} />);
    const link = screen.getByRole('link', { name: 'Sam Rivera' });
    // jsdom has no PointerEvent, so set pointerType by hand.
    const tap = createEvent.pointerDown(link);
    Object.defineProperty(tap, 'pointerType', { value: 'touch' });
    fireEvent(link, tap);
    act(() => link.focus());
    expect(screen.queryByRole('dialog')).toBeNull();
    act(() => link.blur());
    await user.tab();
    expect(link).toHaveFocus();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(link.getAttribute('aria-describedby')).toBeTruthy();
    expect(onOpenChange).toHaveBeenCalledTimes(1);
    expect(onOpenChange).toHaveBeenLastCalledWith(true);
  });

  it('renders on the server', () => {
    const html = renderToString(<Demo />);
    expect(html).toContain('href="#sam"');
  });
});
