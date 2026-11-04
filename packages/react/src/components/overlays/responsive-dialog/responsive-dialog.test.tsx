import { afterEach, describe, expect, it, vi } from 'vitest';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { useState } from 'react';
import { hydrateRoot } from 'react-dom/client';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import {
  DialogBody,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../dialog/index.js';
import {
  ResponsiveDialog,
  ResponsiveDialogClose,
  ResponsiveDialogContent,
  ResponsiveDialogTrigger,
  type ResponsiveDialogContentProps,
} from './index.js';

function Invite(props: ResponsiveDialogContentProps) {
  return (
    <ResponsiveDialog>
      <ResponsiveDialogTrigger asChild>
        <Button>Invite people</Button>
      </ResponsiveDialogTrigger>
      <ResponsiveDialogContent {...props}>
        <DialogHeader>
          <DialogTitle>Invite people</DialogTitle>
          <DialogDescription>They get an email with a link.</DialogDescription>
        </DialogHeader>
        <DialogBody>
          <label>
            Email <input type="email" />
          </label>
        </DialogBody>
        <DialogFooter>
          <ResponsiveDialogClose asChild>
            <Button>Cancel</Button>
          </ResponsiveDialogClose>
          <Button variant="primary">Send invite</Button>
        </DialogFooter>
      </ResponsiveDialogContent>
    </ResponsiveDialog>
  );
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe('ResponsiveDialog', () => {
  it('opens as a dialog, traps focus, closes on Escape and restores focus', async () => {
    render(<Invite />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Invite people' });
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Invite people' });
    expect(dialog).toHaveClass('mn-dialog', 'mn-responsive-dialog');
    expect(dialog).toHaveAttribute('data-sheet-below', 'sm');
    expect(dialog).toHaveAccessibleDescription('They get an email with a link.');
    for (let i = 0; i < 5; i++) {
      await user.tab();
      expect(dialog.contains(document.activeElement)).toBe(true);
    }
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();
  });

  it('passes the Dialog options through (sheetBelow, size, busy, footer)', () => {
    render(
      <ResponsiveDialog open>
        <ResponsiveDialogContent sheetBelow="md" size="lg" busy footer={<Button>Save</Button>}>
          <DialogTitle>Edit</DialogTitle>
        </ResponsiveDialogContent>
      </ResponsiveDialog>,
    );
    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('data-sheet-below', 'md');
    expect(dialog).toHaveAttribute('data-size', 'lg');
    expect(dialog).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
  });

  it('renders a decorative handle (shown by CSS in the sheet layout), removable', () => {
    const { rerender } = render(
      <ResponsiveDialog open>
        <ResponsiveDialogContent>
          <DialogTitle>Edit</DialogTitle>
        </ResponsiveDialogContent>
      </ResponsiveDialog>,
    );
    expect(document.querySelector('.mn-responsive-dialog__handle')).toHaveAttribute('aria-hidden');
    rerender(
      <ResponsiveDialog open>
        <ResponsiveDialogContent handle={false}>
          <DialogTitle>Edit</DialogTitle>
        </ResponsiveDialogContent>
      </ResponsiveDialog>,
    );
    expect(document.querySelector('.mn-responsive-dialog__handle')).toBeNull();
  });

  it('is controlled and keeps typed values on an outside click when asked', async () => {
    function Demo() {
      const [open, setOpen] = useState(true);
      return (
        <>
          <span data-testid="state">{open ? 'open' : 'closed'}</span>
          <ResponsiveDialog open={open} onOpenChange={setOpen}>
            <ResponsiveDialogContent closeOnOutsideClick={false}>
              <DialogTitle>Draft</DialogTitle>
            </ResponsiveDialogContent>
          </ResponsiveDialog>
        </>
      );
    }
    render(<Demo />);
    const user = userEvent.setup();
    await user.click(document.querySelector('.mn-overlay')!);
    expect(screen.getByTestId('state')).toHaveTextContent('open');
    await user.click(screen.getByRole('button', { name: 'Close' }));
    await waitFor(() => expect(screen.getByTestId('state')).toHaveTextContent('closed'));
  });

  it('chooses its layout in CSS: never reads matchMedia or the window size', async () => {
    const matchMedia = vi.fn();
    vi.stubGlobal('matchMedia', matchMedia);
    render(<Invite />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Invite people' }));
    await screen.findByRole('dialog');
    expect(matchMedia).not.toHaveBeenCalled();
    vi.unstubAllGlobals();

    const css = readFileSync(join(dirname(__filename), 'responsive-dialog.css'), 'utf8');
    expect(css).toContain('@media (max-width: 639.98px)');
    expect(css).toContain("[data-sheet-below='md']");
  });

  it('renders on the server and hydrates without a mismatch', async () => {
    const tree = <Invite />;
    const html = renderToString(tree);
    expect(html).toContain('aria-haspopup="dialog"');
    const container = document.createElement('div');
    container.innerHTML = html;
    document.body.appendChild(container);
    const errors = vi.spyOn(console, 'error').mockImplementation(() => {});
    const recoverable = vi.fn();
    let root: ReturnType<typeof hydrateRoot> | undefined;
    await act(async () => {
      root = hydrateRoot(container, tree, { onRecoverableError: recoverable });
    });
    expect(recoverable).not.toHaveBeenCalled();
    expect(errors).not.toHaveBeenCalled();
    await act(async () => root?.unmount());
    container.remove();
  });

  it('renders open on the server without touching browser globals', () => {
    expect(() =>
      renderToString(
        <ResponsiveDialog open>
          <ResponsiveDialogContent>
            <DialogTitle>Server</DialogTitle>
          </ResponsiveDialogContent>
        </ResponsiveDialog>,
      ),
    ).not.toThrow();
  });
});
