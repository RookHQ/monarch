import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
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
import { PromptDialog, usePrompt, type PromptDialogProps } from './index.js';

function deferred() {
  let resolve!: () => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

function Rename(props: Partial<PromptDialogProps>) {
  return (
    <PromptDialog
      trigger={<Button>Rename group</Button>}
      title="Rename group"
      description="Everyone in the group sees the new name."
      label="Group name"
      defaultValue="Design team"
      submitLabel="Rename"
      onSubmit={() => {}}
      {...props}
    />
  );
}

const field = () => screen.getByRole('textbox', { name: /Group name/ }) as HTMLInputElement;
const submitButton = () => screen.getByRole('button', { name: 'Rename' });

describe('PromptDialog', () => {
  it('opens as a labelled dialog with the prefilled text selected; Escape cancels and restores focus', async () => {
    const onCancel = vi.fn();
    const onSubmit = vi.fn();
    render(<Rename onCancel={onCancel} onSubmit={onSubmit} />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Rename group' });
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Rename group' });
    expect(dialog).toHaveAccessibleDescription('Everyone in the group sees the new name.');
    expect(field()).toHaveFocus();
    expect(field()).toHaveValue('Design team');
    expect(field().selectionStart).toBe(0);
    expect(field().selectionEnd).toBe('Design team'.length);
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onCancel).toHaveBeenCalledOnce();
    expect(onSubmit).not.toHaveBeenCalled();
    expect(trigger).toHaveFocus();
  });

  it('Enter submits the trimmed value and closes', async () => {
    const onSubmit = vi.fn();
    render(<Rename defaultOpen onSubmit={onSubmit} />);
    const user = userEvent.setup();
    await user.keyboard('  Research  {Enter}');
    expect(onSubmit).toHaveBeenCalledWith('Research');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('Enter while an IME is composing does not submit', () => {
    const onSubmit = vi.fn();
    render(<Rename defaultOpen onSubmit={onSubmit} />);
    fireEvent.compositionStart(field());
    fireEvent.keyDown(field(), { key: 'Enter', isComposing: true });
    fireEvent.keyDown(field(), { key: 'Enter', keyCode: 229 });
    expect(onSubmit).not.toHaveBeenCalled();
    fireEvent.keyDown(field(), { key: 'Escape' });
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    fireEvent.compositionEnd(field());
    fireEvent.keyDown(field(), { key: 'Enter' });
    expect(onSubmit).toHaveBeenCalledWith('Design team');
  });

  it('required: an empty value shows an announced field error and does not submit', async () => {
    const onSubmit = vi.fn();
    render(<Rename defaultOpen required onSubmit={onSubmit} />);
    const user = userEvent.setup();
    await user.clear(field());
    await user.type(field(), '   {Enter}');
    expect(onSubmit).not.toHaveBeenCalled();
    expect(field()).toBeRequired();
    expect(field()).toHaveAttribute('aria-invalid', 'true');
    expect(field()).toHaveAccessibleDescription('Enter a value.');
    expect(screen.getByText('Enter a value.')).toHaveAttribute('aria-live', 'polite');
    await user.type(field(), 'x');
    expect(field()).not.toHaveAttribute('aria-invalid');
  });

  it('url: rejects addresses that are not absolute, then accepts a valid one', async () => {
    const onSubmit = vi.fn();
    render(
      <PromptDialog
        defaultOpen
        title="Add link"
        label="Link address"
        inputType="url"
        submitLabel="Add link"
        onSubmit={onSubmit}
      />,
    );
    const user = userEvent.setup();
    const input = screen.getByRole('textbox', { name: 'Link address' });
    expect(input).toHaveAttribute('type', 'url');
    expect(input).toHaveAttribute('inputmode', 'url');
    await user.type(input, 'example.com{Enter}');
    expect(onSubmit).not.toHaveBeenCalled();
    expect(input).toHaveAccessibleDescription('Enter a full web address, starting with https://.');
    await user.clear(input);
    await user.type(input, 'https://example.com/page');
    await user.click(screen.getByRole('button', { name: 'Add link' }));
    expect(onSubmit).toHaveBeenCalledWith('https://example.com/page');
  });

  it('email: checks the format and honours invalidMessage', async () => {
    const onSubmit = vi.fn();
    render(
      <PromptDialog
        defaultOpen
        title="Invite"
        label="Email"
        inputType="email"
        invalidMessage="Check the address."
        onSubmit={onSubmit}
      />,
    );
    const user = userEvent.setup();
    const input = screen.getByRole('textbox', { name: 'Email' });
    await user.type(input, 'name@{Enter}');
    expect(input).toHaveAccessibleDescription('Check the address.');
    await user.type(input, 'example.com{Enter}');
    expect(onSubmit).toHaveBeenCalledWith('name@example.com');
  });

  it('validate: a returned message blocks the submit and is linked to the field', async () => {
    const onSubmit = vi.fn();
    const validate = vi.fn((value: string) =>
      value === 'Design team' ? 'That name is already used.' : null,
    );
    render(<Rename defaultOpen validate={validate} onSubmit={onSubmit} />);
    const user = userEvent.setup();
    await user.click(submitButton());
    expect(validate).toHaveBeenCalledWith('Design team');
    expect(onSubmit).not.toHaveBeenCalled();
    expect(field()).toHaveAccessibleDescription('That name is already used.');
    expect(field()).toHaveFocus();
    await user.type(field(), ' 2{Enter}');
    expect(onSubmit).toHaveBeenCalledWith('Design team 2');
  });

  it('multiline: Enter adds a line, Ctrl+Enter and Cmd+Enter submit', async () => {
    const onSubmit = vi.fn();
    render(
      <PromptDialog
        defaultOpen
        multiline
        title="Add a note"
        label="Note"
        submitLabel="Add note"
        closeOnSubmit={false}
        onSubmit={onSubmit}
      />,
    );
    const user = userEvent.setup();
    const area = screen.getByRole('textbox', { name: 'Note' });
    expect(area.tagName).toBe('TEXTAREA');
    expect(area).toHaveFocus();
    await user.type(area, 'First{Enter}Second');
    expect(onSubmit).not.toHaveBeenCalled();
    expect(area).toHaveValue('First\nSecond');
    await user.keyboard('{Control>}{Enter}{/Control}');
    expect(onSubmit).toHaveBeenLastCalledWith('First\nSecond');
    await user.keyboard('{Meta>}{Enter}{/Meta}');
    expect(onSubmit).toHaveBeenCalledTimes(2);
  });

  it('async submit: pending locks the dialog, success closes it', async () => {
    const work = deferred();
    const onOpenChange = vi.fn();
    render(<Rename defaultOpen onSubmit={() => work.promise} onOpenChange={onOpenChange} />);
    const user = userEvent.setup();
    await user.click(submitButton());
    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-busy', 'true');
    expect(submitButton()).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    expect(field()).toHaveAttribute('readonly');
    await user.keyboard('{Escape}');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    work.resolve();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onOpenChange).toHaveBeenCalledTimes(1);
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('async submit: a rejection is shown in the dialog and the person can retry', async () => {
    let attempt = 0;
    const onSubmit = vi.fn(async () => {
      attempt += 1;
      if (attempt === 1) throw new Error('The group could not be renamed.');
    });
    render(<Rename defaultOpen onSubmit={onSubmit} />);
    const user = userEvent.setup();
    await user.click(submitButton());
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('The group could not be renamed.');
    expect(submitButton()).toHaveAccessibleDescription('The group could not be renamed.');
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeEnabled();
    await user.click(submitButton());
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onSubmit).toHaveBeenCalledTimes(2);
  });

  it('uses a generic message for non-Error rejections', async () => {
    render(<Rename defaultOpen onSubmit={() => Promise.reject('nope')} />);
    await userEvent.setup().click(submitButton());
    expect(await screen.findByRole('alert')).toHaveTextContent('That did not work. Try again.');
  });

  it('starts from the default value every time it opens', async () => {
    render(<Rename />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Rename group' }));
    await user.type(field(), 'zzz');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: 'Rename group' }));
    expect(field()).toHaveValue('Design team');
  });

  it('passes maxLength and placeholder to the field and uses the default labels', () => {
    render(
      <PromptDialog
        open
        title="Describe the image"
        label="Alt text"
        maxLength={140}
        placeholder="A short description"
        onSubmit={() => {}}
      />,
    );
    const input = screen.getByRole('textbox', { name: 'Alt text' });
    expect(input).toHaveAttribute('maxlength', '140');
    expect(input).toHaveAttribute('placeholder', 'A short description');
    expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<Rename />)).toContain('aria-haspopup="dialog"');
    expect(() => renderToString(<Rename defaultOpen />)).not.toThrow();
  });
});

describe('usePrompt', () => {
  function Views({ onResult }: { onResult: (r: string | null) => void }) {
    const { prompt, element } = usePrompt();
    return (
      <>
        <Button
          onClick={async () =>
            onResult(
              await prompt({
                title: 'Name this view',
                label: 'View name',
                submitLabel: 'Save view',
                required: true,
              }),
            )
          }
        >
          Save view as
        </Button>
        {element}
      </>
    );
  }

  it('resolves with the value on submit and null on cancel', async () => {
    const onResult = vi.fn();
    render(<Views onResult={onResult} />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Save view as' });
    await user.click(trigger);
    await screen.findByRole('dialog', { name: 'Name this view' });
    await user.keyboard('Open tasks{Enter}');
    await waitFor(() => expect(onResult).toHaveBeenCalledWith('Open tasks'));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await waitFor(() => expect(trigger).toHaveFocus());

    await user.click(trigger);
    await screen.findByRole('dialog');
    expect(screen.getByRole('textbox', { name: /View name/ })).toHaveValue('');
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(onResult).toHaveBeenLastCalledWith(null));
  });

  it('runs async work inside the dialog before resolving', async () => {
    const work = deferred();
    const onResult = vi.fn();
    function Alt() {
      const { prompt, element } = usePrompt();
      return (
        <>
          <Button
            onClick={async () =>
              onResult(
                await prompt({
                  title: 'Describe the image',
                  label: 'Alt text',
                  defaultValue: 'A chart',
                  onSubmit: () => work.promise,
                }),
              )
            }
          >
            Edit alt text
          </Button>
          {element}
        </>
      );
    }
    render(<Alt />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Edit alt text' }));
    await user.click(await screen.findByRole('button', { name: 'Save' }));
    expect(screen.getByRole('button', { name: 'Save' })).toHaveAttribute('aria-busy', 'true');
    expect(onResult).not.toHaveBeenCalled();
    work.resolve();
    await waitFor(() => expect(onResult).toHaveBeenCalledWith('A chart'));
  });

  it('settles the earlier call with null when replaced or unmounted', async () => {
    const results: (string | null)[] = [];
    let api: ReturnType<typeof usePrompt> | null = null;
    function Host() {
      api = usePrompt();
      return api.element;
    }
    const { unmount } = render(<Host />);
    const first = api!.prompt({ title: 'First', label: 'A' }).then((r) => results.push(r));
    const second = api!.prompt({ title: 'Second', label: 'B' }).then((r) => results.push(r));
    await first;
    expect(results).toEqual([null]);
    await screen.findByRole('dialog', { name: 'Second' });
    unmount();
    await second;
    expect(results).toEqual([null, null]);
  });

  it('opens from a menu item and returns focus to the menu trigger', async () => {
    const onResult = vi.fn();
    function Row() {
      const { prompt, element } = usePrompt();
      return (
        <>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button>Group actions</Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent>
              <DropdownMenuItem
                onSelect={async () =>
                  onResult(await prompt({ title: 'Rename group', label: 'Group name' }))
                }
              >
                Rename
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
          {element}
        </>
      );
    }
    render(<Row />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Group actions' });
    await user.click(trigger);
    await user.click(await screen.findByRole('menuitem', { name: 'Rename' }));
    await screen.findByRole('dialog', { name: 'Rename group' });
    await waitFor(() => expect(screen.getByRole('textbox', { name: 'Group name' })).toHaveFocus());
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await waitFor(() => expect(onResult).toHaveBeenCalledWith(null));
    await waitFor(() => expect(trigger).toHaveFocus());
    expect(document.body.style.pointerEvents).not.toBe('none');
  });
});
