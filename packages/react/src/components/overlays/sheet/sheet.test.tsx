import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import {
  Sheet,
  SheetBody,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
  type SheetContentProps,
} from './index.js';

function Filters(props: SheetContentProps) {
  return (
    <Sheet>
      <SheetTrigger asChild>
        <Button>Filters</Button>
      </SheetTrigger>
      <SheetContent {...props}>
        <SheetHeader>
          <SheetTitle>Filters</SheetTitle>
          <SheetDescription>Narrow the list.</SheetDescription>
        </SheetHeader>
        <SheetBody>
          <label>
            Owner <input />
          </label>
        </SheetBody>
        <SheetFooter>
          <SheetClose asChild>
            <Button>Cancel</Button>
          </SheetClose>
          <Button variant="primary">Apply</Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}

const overlay = () => document.querySelector('.mn-overlay')!;

describe('Sheet', () => {
  it('opens from its trigger, traps focus, closes on Escape and restores focus', async () => {
    render(<Filters />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Filters' });
    await user.click(trigger);
    const sheet = await screen.findByRole('dialog', { name: 'Filters' });
    expect(sheet).toHaveAccessibleDescription('Narrow the list.');
    expect(sheet).toHaveClass('mn-sheet');
    expect(sheet).toHaveAttribute('data-side', 'right');
    expect(sheet).toHaveAttribute('data-size', 'md');
    expect(sheet.contains(document.activeElement)).toBe(true);
    for (let i = 0; i < 6; i++) {
      await user.tab();
      expect(sheet.contains(document.activeElement)).toBe(true);
    }
    await expectNoA11yViolations(sheet);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();
  });

  it('opens from the keyboard and closes from a SheetClose', async () => {
    render(<Filters />);
    const user = userEvent.setup();
    screen.getByRole('button', { name: 'Filters' }).focus();
    await user.keyboard('{Enter}');
    await screen.findByRole('dialog');
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('locks page scroll while open (Radix scroll lock)', async () => {
    render(<Filters />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Filters' }));
    await screen.findByRole('dialog');
    expect(document.body).toHaveAttribute('data-scroll-locked');
  });

  it('closes on an outside click unless closeOnOutsideClick is false', async () => {
    const user = userEvent.setup();
    const { unmount } = render(<Filters />);
    await user.click(screen.getByRole('button', { name: 'Filters' }));
    await screen.findByRole('dialog');
    await user.click(overlay());
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    unmount();

    render(<Filters closeOnOutsideClick={false} />);
    await user.click(screen.getByRole('button', { name: 'Filters' }));
    await screen.findByRole('dialog');
    await user.click(overlay());
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('busy blocks every dismissal and disables the close button', async () => {
    const onOpenChange = vi.fn();
    render(
      <Sheet open onOpenChange={onOpenChange}>
        <SheetContent busy>
          <SheetTitle>Saving</SheetTitle>
        </SheetContent>
      </Sheet>,
    );
    const user = userEvent.setup();
    expect(screen.getByRole('dialog')).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: 'Close' })).toBeDisabled();
    await user.keyboard('{Escape}');
    await user.click(overlay());
    expect(onOpenChange).not.toHaveBeenCalled();
  });

  it('is controlled through open and onOpenChange', async () => {
    function Demo() {
      const [open, setOpen] = useState(false);
      return (
        <>
          <Button onClick={() => setOpen(true)}>Show details</Button>
          <span data-testid="state">{open ? 'open' : 'closed'}</span>
          <Sheet open={open} onOpenChange={setOpen}>
            <SheetContent side="left" size="lg" closeLabel="Close details">
              <SheetTitle>Details</SheetTitle>
            </SheetContent>
          </Sheet>
        </>
      );
    }
    render(<Demo />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Show details' }));
    const sheet = await screen.findByRole('dialog', { name: 'Details' });
    expect(sheet).toHaveAttribute('data-side', 'left');
    expect(sheet).toHaveAttribute('data-size', 'lg');
    await user.click(screen.getByRole('button', { name: 'Close details' }));
    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('closed'));
  });

  it('shows the grab handle on bottom sheets only, unless overridden', () => {
    const { rerender } = render(
      <Sheet open>
        <SheetContent side="bottom">
          <SheetTitle>Share</SheetTitle>
        </SheetContent>
      </Sheet>,
    );
    expect(document.querySelector('.mn-sheet__handle')).toHaveAttribute('aria-hidden');
    rerender(
      <Sheet open>
        <SheetContent side="bottom" handle={false}>
          <SheetTitle>Share</SheetTitle>
        </SheetContent>
      </Sheet>,
    );
    expect(document.querySelector('.mn-sheet__handle')).toBeNull();
    rerender(
      <Sheet open>
        <SheetContent side="right">
          <SheetTitle>Share</SheetTitle>
        </SheetContent>
      </Sheet>,
    );
    expect(document.querySelector('.mn-sheet__handle')).toBeNull();
  });

  it('renders the footer prop and hides the close button with closeLabel null', () => {
    render(
      <Sheet open>
        <SheetContent closeLabel={null} footer={<Button>Done</Button>}>
          <SheetTitle>Panel</SheetTitle>
        </SheetContent>
      </Sheet>,
    );
    expect(screen.getByRole('button', { name: 'Done' }).parentElement).toHaveClass(
      'mn-sheet__footer',
    );
    expect(screen.queryByRole('button', { name: 'Close' })).toBeNull();
  });

  it('non-modal sheets leave the page interactive', async () => {
    render(
      <>
        <button type="button">Behind</button>
        <Sheet open modal={false}>
          <SheetContent closeOnOutsideClick={false}>
            <SheetTitle>Support</SheetTitle>
          </SheetContent>
        </Sheet>
      </>,
    );
    expect(document.querySelector('.mn-overlay')).toBeNull();
    expect(document.body).not.toHaveAttribute('data-scroll-locked');
  });

  it('renders on the server', () => {
    const closed = renderToString(<Filters />);
    expect(closed).toContain('aria-haspopup="dialog"');
    // Open content is portalled on the client only; the server render must not throw.
    expect(() =>
      renderToString(
        <Sheet open>
          <SheetContent>
            <SheetTitle>Server</SheetTitle>
          </SheetContent>
        </Sheet>,
      ),
    ).not.toThrow();
  });
});
