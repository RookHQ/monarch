import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from './index.js';

describe('Dialog', () => {
  it('opens, traps focus, closes on Escape and restores focus', async () => {
    render(
      <Dialog>
        <DialogTrigger asChild>
          <Button>Delete project</Button>
        </DialogTrigger>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete project?</DialogTitle>
            <DialogDescription>This removes all tasks.</DialogDescription>
          </DialogHeader>
          <DialogBody>Content</DialogBody>
          <DialogFooter>
            <Button>Cancel</Button>
            <Button variant="danger">Delete</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>,
    );
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Delete project' });
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Delete project?' });
    expect(dialog).toHaveAccessibleDescription('This removes all tasks.');
    expect(dialog.contains(document.activeElement)).toBe(true);
    for (let i = 0; i < 5; i++) {
      await user.tab();
      expect(dialog.contains(document.activeElement)).toBe(true);
    }
    expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument();
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();
  });

  it('Dialog with closeLabel null has no close button', () => {
    render(
      <Dialog open>
        <DialogContent closeLabel={null}>
          <DialogTitle>T</DialogTitle>
        </DialogContent>
      </Dialog>,
    );
    expect(screen.queryByRole('button', { name: 'Close' })).toBeNull();
  });

  it('renders the footer prop after the children', () => {
    render(
      <Dialog open>
        <DialogContent footer={<Button>Save</Button>}>
          <DialogTitle>Edit</DialogTitle>
          <DialogBody>Body</DialogBody>
        </DialogContent>
      </Dialog>,
    );
    const save = screen.getByRole('button', { name: 'Save' });
    expect(save.parentElement).toHaveClass('mn-dialog__footer');
    expect(screen.getByText('Body').compareDocumentPosition(save)).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    );
  });

  it('busy blocks Escape, outside clicks and the close button', async () => {
    const onOpenChange = vi.fn();
    render(
      <Dialog open onOpenChange={onOpenChange}>
        <DialogContent busy>
          <DialogTitle>Saving</DialogTitle>
        </DialogContent>
      </Dialog>,
    );
    const user = userEvent.setup();
    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-busy', 'true');
    expect(dialog).toHaveAttribute('data-busy');
    expect(screen.getByRole('button', { name: 'Close' })).toBeDisabled();
    await user.keyboard('{Escape}');
    await user.click(document.querySelector('.mn-overlay')!);
    expect(onOpenChange).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('closeOnOutsideClick={false} keeps it open on an outside click but Escape still closes', async () => {
    function Demo() {
      const [open, setOpen] = useState(true);
      return (
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogContent closeOnOutsideClick={false}>
            <DialogTitle>Draft</DialogTitle>
          </DialogContent>
        </Dialog>
      );
    }
    render(<Demo />);
    const user = userEvent.setup();
    await user.click(document.querySelector('.mn-overlay')!);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('closes on an outside click by default (uncontrolled)', async () => {
    render(
      <Dialog defaultOpen>
        <DialogContent>
          <DialogTitle>Note</DialogTitle>
        </DialogContent>
      </Dialog>,
    );
    const user = userEvent.setup();
    await user.click(document.querySelector('.mn-overlay')!);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('escape guard: Escape on a control with an open popup does not close the dialog', async () => {
    const onOpenChange = vi.fn();
    render(
      <Dialog open onOpenChange={onOpenChange}>
        <DialogContent>
          <DialogTitle>Assign</DialogTitle>
          <input role="combobox" aria-expanded="true" aria-controls="list" aria-label="Person" />
          <div id="list" role="listbox" aria-label="People" />
        </DialogContent>
      </Dialog>,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('combobox'));
    await user.keyboard('{Escape}');
    expect(onOpenChange).not.toHaveBeenCalled();
  });

  it('escapeGuard={false} lets Escape through', async () => {
    const onOpenChange = vi.fn();
    render(
      <Dialog open onOpenChange={onOpenChange}>
        <DialogContent escapeGuard={false}>
          <DialogTitle>Assign</DialogTitle>
          <input role="combobox" aria-expanded="true" aria-controls="list2" aria-label="Person" />
          <div id="list2" role="listbox" aria-label="People" />
        </DialogContent>
      </Dialog>,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('combobox'));
    await user.keyboard('{Escape}');
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('still calls a consumer onEscapeKeyDown and honours its preventDefault', async () => {
    const onOpenChange = vi.fn();
    const onEscapeKeyDown = vi.fn((event: KeyboardEvent) => event.preventDefault());
    render(
      <Dialog open onOpenChange={onOpenChange}>
        <DialogContent onEscapeKeyDown={onEscapeKeyDown}>
          <DialogTitle>T</DialogTitle>
        </DialogContent>
      </Dialog>,
    );
    await userEvent.setup().keyboard('{Escape}');
    expect(onEscapeKeyDown).toHaveBeenCalledOnce();
    expect(onOpenChange).not.toHaveBeenCalled();
  });

  it('marks phone full-screen and size presets as data attributes', () => {
    render(
      <Dialog open>
        <DialogContent size="lg" fullScreenOnPhone>
          <DialogTitle>Wide</DialogTitle>
        </DialogContent>
      </Dialog>,
    );
    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('data-size', 'lg');
    expect(dialog).toHaveAttribute('data-fullscreen-phone');
  });

  it('renders on the server (closed)', () => {
    const html = renderToString(
      <Dialog>
        <DialogTrigger asChild>
          <Button>Delete project</Button>
        </DialogTrigger>
        <DialogContent>
          <DialogTitle>Delete project?</DialogTitle>
        </DialogContent>
      </Dialog>,
    );
    expect(html).toContain('aria-haspopup="dialog"');
  });
});
