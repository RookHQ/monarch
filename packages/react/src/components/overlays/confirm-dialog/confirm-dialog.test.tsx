import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../dropdown-menu/index.js';
import {
  ConfirmDialog,
  useConfirm,
  type ConfirmDetails,
  type ConfirmDialogProps,
} from './index.js';

function deferred() {
  let resolve!: () => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

function Delete(props: Partial<ConfirmDialogProps>) {
  return (
    <ConfirmDialog
      trigger={<Button variant="danger">Delete project</Button>}
      title="Delete this project?"
      description="Tasks, files and history are removed for everyone."
      tone="danger"
      confirmLabel="Delete project"
      onConfirm={() => {}}
      {...props}
    />
  );
}

const confirmButton = () => screen.getByRole('button', { name: 'Delete project', hidden: false });
const inDialog = (name: string) =>
  screen.getAllByRole('button', { name }).find((b) => b.closest('[role="alertdialog"]'))!;

describe('ConfirmDialog', () => {
  it('opens as an alertdialog with focus on Cancel; Escape cancels and restores focus', async () => {
    const onCancel = vi.fn();
    render(<Delete onCancel={onCancel} />);
    const user = userEvent.setup();
    const trigger = confirmButton();
    await user.click(trigger);
    const dialog = await screen.findByRole('alertdialog', { name: 'Delete this project?' });
    expect(dialog).toHaveAccessibleDescription(
      'Tasks, files and history are removed for everyone.',
    );
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus();
    expect(dialog.querySelector('.mn-confirm-dialog__icon')).toHaveAttribute('aria-hidden');
    expect(inDialog('Delete project')).toHaveAttribute('data-variant', 'danger');
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    expect(onCancel).toHaveBeenCalledOnce();
    expect(trigger).toHaveFocus();
  });

  it('keeps focus inside and ignores outside clicks', async () => {
    render(<Delete defaultOpen />);
    const user = userEvent.setup();
    const dialog = await screen.findByRole('alertdialog');
    for (let i = 0; i < 4; i++) {
      await user.tab();
      expect(dialog.contains(document.activeElement)).toBe(true);
    }
    await user.click(document.querySelector('.mn-overlay')!);
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
  });

  it('confirms a synchronous action and closes', async () => {
    const onConfirm = vi.fn();
    render(<Delete defaultOpen onConfirm={onConfirm} />);
    const user = userEvent.setup();
    await user.click(inDialog('Delete project'));
    expect(onConfirm).toHaveBeenCalledWith({});
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
  });

  it('type-to-confirm: blocked until the exact phrase, mismatch announced on Enter', async () => {
    const onConfirm = vi.fn();
    render(
      <Delete defaultOpen confirmPhrase="acme-site" onConfirm={onConfirm} initialFocus="field" />,
    );
    const user = userEvent.setup();
    const field = screen.getByRole('textbox', { name: 'Type acme-site to confirm' });
    expect(field).toHaveFocus();
    expect(field).toHaveAttribute('autocomplete', 'off');
    const confirm = inDialog('Delete project');
    expect(confirm).toBeDisabled();
    await user.type(field, 'Acme-site{Enter}');
    expect(onConfirm).not.toHaveBeenCalled();
    expect(field).toHaveAttribute('aria-invalid', 'true');
    expect(field).toHaveAccessibleDescription('The text does not match.');
    await user.clear(field);
    await user.type(field, ' acme-site ');
    expect(confirm).toBeEnabled();
    expect(field).not.toHaveAttribute('aria-invalid', 'true');
    await user.keyboard('{Enter}');
    expect(onConfirm).toHaveBeenCalledOnce();
  });

  it('async confirm: pending locks the dialog, success closes it', async () => {
    const work = deferred();
    const onOpenChange = vi.fn();
    render(<Delete defaultOpen onConfirm={() => work.promise} onOpenChange={onOpenChange} />);
    const user = userEvent.setup();
    const confirm = inDialog('Delete project');
    await user.click(confirm);
    const dialog = screen.getByRole('alertdialog');
    expect(dialog).toHaveAttribute('aria-busy', 'true');
    expect(confirm).toHaveAttribute('aria-busy', 'true');
    expect(confirm).toHaveFocus();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    await user.keyboard('{Escape}');
    await user.click(confirm);
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    work.resolve();
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(onOpenChange).toHaveBeenCalledTimes(1);
  });

  it('async confirm: a rejection shows the error, keeps it open and allows retry', async () => {
    let attempt = 0;
    const onConfirm = vi.fn(async () => {
      attempt += 1;
      if (attempt === 1) throw new Error('The project is locked by an export.');
    });
    render(<Delete defaultOpen onConfirm={onConfirm} />);
    const user = userEvent.setup();
    const confirm = inDialog('Delete project');
    await user.click(confirm);
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('The project is locked by an export.');
    expect(confirm).toHaveAccessibleDescription('The project is locked by an export.');
    expect(confirm).not.toHaveAttribute('aria-busy');
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeEnabled();
    await user.click(confirm);
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    expect(onConfirm).toHaveBeenCalledTimes(2);
  });

  it('uses a generic message for non-Error rejections and honours errorMessage', async () => {
    const user = userEvent.setup();
    const { unmount } = render(<Delete defaultOpen onConfirm={() => Promise.reject('nope')} />);
    await user.click(inDialog('Delete project'));
    expect(await screen.findByRole('alert')).toHaveTextContent('That did not work. Try again.');
    unmount();

    render(
      <Delete
        defaultOpen
        onConfirm={() => Promise.reject(new Error('503'))}
        errorMessage={() => 'The server is busy. Try again in a minute.'}
      />,
    );
    await user.click(inDialog('Delete project'));
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The server is busy. Try again in a minute.',
    );
  });

  it('a synchronous throw is shown as an error too', async () => {
    render(
      <Delete
        defaultOpen
        onConfirm={() => {
          throw new Error('No permission.');
        }}
      />,
    );
    await userEvent.setup().click(inDialog('Delete project'));
    expect(await screen.findByRole('alert')).toHaveTextContent('No permission.');
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
  });

  it('required reason: blocked until filled, passed trimmed to onConfirm', async () => {
    const onConfirm = vi.fn();
    render(
      <ConfirmDialog
        defaultOpen
        title="Decline request?"
        confirmLabel="Decline"
        reason={{ label: 'Reason', required: true, description: 'The requester sees this.' }}
        onConfirm={onConfirm}
      />,
    );
    const user = userEvent.setup();
    const field = screen.getByRole('textbox', { name: /Reason/ });
    expect(field).toBeRequired();
    expect(field).toHaveAttribute('maxlength', '1000');
    const confirm = screen.getByRole('button', { name: 'Decline' });
    expect(confirm).toBeDisabled();
    await user.type(field, '   ');
    expect(confirm).toBeDisabled();
    await user.type(field, ' Duplicate of an open request  ');
    await user.click(confirm);
    expect(onConfirm).toHaveBeenCalledWith({ reason: 'Duplicate of an open request' });
  });

  it('optional reason: empty is sent as no reason', async () => {
    const onConfirm = vi.fn();
    render(
      <ConfirmDialog
        defaultOpen
        title="Archive?"
        confirmLabel="Archive"
        reason={{ label: 'Note' }}
        onConfirm={onConfirm}
      />,
    );
    await userEvent.setup().click(screen.getByRole('button', { name: 'Archive' }));
    expect(onConfirm).toHaveBeenCalledWith({});
  });

  it('shows consequences as a list and children before the fields', () => {
    render(
      <Delete
        defaultOpen
        consequences={['12 tasks are deleted', '3 people lose access']}
        confirmPhrase="DELETE"
      >
        <p>Extra verification goes here.</p>
      </Delete>,
    );
    const items = screen.getAllByRole('listitem');
    expect(items.map((li) => li.textContent)).toEqual([
      '12 tasks are deleted',
      '3 people lose access',
    ]);
    const extra = screen.getByText('Extra verification goes here.');
    expect(extra.compareDocumentPosition(screen.getByRole('textbox'))).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    );
  });

  it('controlled pending and error props', () => {
    render(
      <ConfirmDialog
        open
        title="Revoke key?"
        confirmLabel="Revoke"
        pending
        error="Revoking failed."
        onConfirm={() => {}}
      />,
    );
    expect(screen.getByRole('button', { name: 'Revoke' })).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('alert')).toHaveTextContent('Revoking failed.');
    expect(screen.getByRole('alertdialog')).toHaveAttribute('data-tone', 'default');
  });

  it('controlled open: the parent decides, and closeOnConfirm={false} keeps it open', async () => {
    function Demo() {
      const [open, setOpen] = useState(false);
      return (
        <>
          <Button onClick={() => setOpen(true)}>Restore</Button>
          <ConfirmDialog
            open={open}
            onOpenChange={setOpen}
            title="Restore version?"
            confirmLabel="Restore version"
            closeOnConfirm={false}
            onConfirm={() => {}}
          />
        </>
      );
    }
    render(<Demo />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Restore' }));
    await user.click(screen.getByRole('button', { name: 'Restore version' }));
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
  });

  it('starts clean every time it opens', async () => {
    render(<Delete confirmPhrase="acme" />);
    const user = userEvent.setup();
    await user.click(confirmButton());
    await user.type(screen.getByRole('textbox'), 'ac');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    await user.click(confirmButton());
    expect(screen.getByRole('textbox')).toHaveValue('');
  });

  it('renders on the server', () => {
    const html = renderToString(<Delete />);
    expect(html).toContain('aria-haspopup="dialog"');
    expect(() => renderToString(<Delete defaultOpen confirmPhrase="x" />)).not.toThrow();
  });
});

describe('useConfirm', () => {
  function Notes({ onResult }: { onResult: (r: ConfirmDetails | null) => void }) {
    const { confirm, element } = useConfirm();
    return (
      <>
        <Button
          onClick={async () =>
            onResult(
              await confirm({
                title: 'Delete note?',
                tone: 'danger',
                confirmLabel: 'Delete note',
                reason: { label: 'Why' },
              }),
            )
          }
        >
          Delete note
        </Button>
        {element}
      </>
    );
  }

  it('resolves with the details on confirm and null on cancel', async () => {
    const onResult = vi.fn();
    render(<Notes onResult={onResult} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Delete note' }));
    await screen.findByRole('alertdialog', { name: 'Delete note?' });
    await user.type(screen.getByRole('textbox', { name: 'Why' }), 'Outdated');
    await user.click(inDialog('Delete note'));
    await waitFor(() => expect(onResult).toHaveBeenCalledWith({ reason: 'Outdated' }));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());

    await user.click(screen.getByRole('button', { name: 'Delete note' }));
    await screen.findByRole('alertdialog');
    expect(screen.getByRole('textbox', { name: 'Why' })).toHaveValue('');
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(onResult).toHaveBeenLastCalledWith(null));
  });

  it('runs async work inside the dialog before resolving', async () => {
    const work = deferred();
    const onResult = vi.fn();
    function Stop() {
      const { confirm, element } = useConfirm();
      return (
        <>
          <Button
            onClick={async () =>
              onResult(
                await confirm({
                  title: 'Stop tracking?',
                  confirmLabel: 'Stop tracking',
                  onConfirm: () => work.promise,
                }),
              )
            }
          >
            Stop
          </Button>
          {element}
        </>
      );
    }
    render(<Stop />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Stop' }));
    await user.click(await screen.findByRole('button', { name: 'Stop tracking' }));
    expect(screen.getByRole('button', { name: 'Stop tracking' })).toHaveAttribute(
      'aria-busy',
      'true',
    );
    expect(onResult).not.toHaveBeenCalled();
    work.resolve();
    await waitFor(() => expect(onResult).toHaveBeenCalledWith({}));
  });

  it('opens from a menu item and returns focus to the menu trigger', async () => {
    const onResult = vi.fn();
    function Row() {
      const { confirm, element } = useConfirm();
      return (
        <>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button>Note actions</Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent>
              <DropdownMenuItem
                tone="danger"
                onSelect={async () =>
                  onResult(await confirm({ title: 'Delete note?', confirmLabel: 'Delete note' }))
                }
              >
                Delete
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
          {element}
        </>
      );
    }
    render(<Row />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Note actions' });
    await user.click(trigger);
    await user.click(await screen.findByRole('menuitem', { name: 'Delete' }));
    await screen.findByRole('alertdialog', { name: 'Delete note?' });
    await waitFor(() => expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus());
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    await waitFor(() => expect(onResult).toHaveBeenCalledWith(null));
    await waitFor(() => expect(trigger).toHaveFocus());
    expect(document.body.style.pointerEvents).not.toBe('none');
  });
});
