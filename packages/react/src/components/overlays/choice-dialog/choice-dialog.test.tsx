import { describe, expect, it, vi } from 'vitest';
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
  ChoiceDialog,
  useChoice,
  type ChoiceDialogChoice,
  type ChoiceDialogProps,
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

const visibility: ChoiceDialogChoice[] = [
  { value: 'private', label: 'Only me', description: 'Nobody else sees this view.' },
  { value: 'team', label: 'Everyone in the team', description: 'Team members can open it.' },
  { value: 'archived', label: 'Archive it', tone: 'danger' },
];

function Visibility(props: Partial<ChoiceDialogProps>) {
  return (
    <ChoiceDialog
      trigger={<Button>Save view</Button>}
      title="Who can see this view?"
      description="You can change this later."
      choices={visibility}
      defaultValue="private"
      confirmLabel="Save"
      onChoose={() => {}}
      {...props}
    />
  );
}

const radio = (name: RegExp | string) => screen.getByRole('radio', { name });
const confirmButton = () => screen.getByRole('button', { name: 'Save' });

describe('ChoiceDialog', () => {
  it('opens with the options as a labelled radio group; focus on the selected one; Escape cancels', async () => {
    const onCancel = vi.fn();
    const onChoose = vi.fn();
    render(<Visibility onCancel={onCancel} onChoose={onChoose} />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Save view' });
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Who can see this view?' });
    expect(dialog).toHaveAccessibleDescription('You can change this later.');
    expect(screen.getByRole('radiogroup', { name: 'Who can see this view?' })).toBeInTheDocument();
    expect(radio('Only me')).toBeChecked();
    expect(radio('Only me')).toHaveFocus();
    expect(radio('Only me')).toHaveAccessibleDescription('Nobody else sees this view.');
    expect(screen.getAllByRole('radio')).toHaveLength(3);
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onCancel).toHaveBeenCalledOnce();
    expect(onChoose).not.toHaveBeenCalled();
    expect(trigger).toHaveFocus();
  });

  it('arrow keys move the selection; confirming sends the chosen value', async () => {
    const onChoose = vi.fn();
    render(<Visibility defaultOpen onChoose={onChoose} />);
    const user = userEvent.setup();
    expect(radio('Only me')).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(radio('Everyone in the team')).toBeChecked();
    expect(radio('Everyone in the team')).toHaveFocus();
    expect(
      screen.getByText('Everyone in the team').closest('.mn-choice-dialog__choice'),
    ).toHaveAttribute('data-state', 'checked');
    await user.keyboard('{ArrowUp}');
    expect(radio('Only me')).toBeChecked();
    await user.keyboard('{ArrowDown}');
    await user.click(confirmButton());
    expect(onChoose).toHaveBeenCalledWith('team');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('Enter on a radio confirms the selected choice', async () => {
    const onChoose = vi.fn();
    render(<Visibility defaultOpen onChoose={onChoose} />);
    await userEvent.setup().keyboard('{Enter}');
    expect(onChoose).toHaveBeenCalledWith('private');
  });

  it('clicking a card selects it; a danger choice turns the confirm button red', async () => {
    render(<Visibility defaultOpen />);
    const user = userEvent.setup();
    expect(confirmButton()).toHaveAttribute('data-variant', 'primary');
    await user.click(screen.getByText('Archive it'));
    expect(radio('Archive it')).toBeChecked();
    expect(confirmButton()).toHaveAttribute('data-variant', 'danger');
  });

  it('without a default, confirming asks for a choice first', async () => {
    const onChoose = vi.fn();
    render(<Visibility defaultOpen defaultValue={undefined} onChoose={onChoose} />);
    const user = userEvent.setup();
    expect(screen.getAllByRole('radio').every((r) => !(r as HTMLInputElement).checked)).toBe(true);
    await user.click(confirmButton());
    expect(onChoose).not.toHaveBeenCalled();
    const group = screen.getByRole('radiogroup');
    expect(group).toHaveAttribute('aria-invalid', 'true');
    expect(group).toHaveAccessibleDescription('Choose an option to continue.');
    expect(radio('Only me')).toHaveFocus();
    await user.click(screen.getByText('Everyone in the team'));
    expect(group).not.toHaveAttribute('aria-invalid');
    await user.click(confirmButton());
    expect(onChoose).toHaveBeenCalledWith('team');
  });

  it('confirmLabel can follow the selection; disabled choices cannot be picked', async () => {
    render(
      <ChoiceDialog
        defaultOpen
        title="Export as"
        choices={[
          { value: 'csv', label: 'CSV' },
          { value: 'pdf', label: 'PDF', disabled: true },
          { value: 'json', label: 'JSON' },
        ]}
        defaultValue="csv"
        confirmLabel={(value) => (value ? `Export ${value.toUpperCase()}` : 'Export')}
        onChoose={() => {}}
      />,
    );
    const user = userEvent.setup();
    expect(screen.getByRole('button', { name: 'Export CSV' })).toBeInTheDocument();
    expect(radio('PDF')).toBeDisabled();
    await user.keyboard('{ArrowDown}');
    expect(radio('JSON')).toBeChecked();
    expect(screen.getByRole('button', { name: 'Export JSON' })).toBeInTheDocument();
  });

  it('async choose: pending locks the dialog and the choices, success closes it', async () => {
    const work = deferred();
    const onOpenChange = vi.fn();
    render(<Visibility defaultOpen onChoose={() => work.promise} onOpenChange={onOpenChange} />);
    const user = userEvent.setup();
    await user.click(confirmButton());
    expect(screen.getByRole('dialog')).toHaveAttribute('aria-busy', 'true');
    expect(confirmButton()).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    await user.click(screen.getByText('Everyone in the team'));
    expect(radio('Only me')).toBeChecked();
    await user.keyboard('{Escape}');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    work.resolve();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onOpenChange).toHaveBeenCalledTimes(1);
  });

  it('async choose: a rejection shows the error and allows retry', async () => {
    let attempt = 0;
    const onChoose = vi.fn(async () => {
      attempt += 1;
      if (attempt === 1) throw new Error('The view could not be saved.');
    });
    render(<Visibility defaultOpen onChoose={onChoose} />);
    const user = userEvent.setup();
    await user.click(confirmButton());
    expect(await screen.findByRole('alert')).toHaveTextContent('The view could not be saved.');
    expect(confirmButton()).toHaveAccessibleDescription('The view could not be saved.');
    await user.click(confirmButton());
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onChoose).toHaveBeenCalledTimes(2);
  });

  it('controlled pending and error props, and the generic error message', async () => {
    const { unmount } = render(<Visibility open pending error="Saving failed." />);
    expect(confirmButton()).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('alert')).toHaveTextContent('Saving failed.');
    unmount();
    render(<Visibility defaultOpen onChoose={() => Promise.reject('nope')} />);
    await userEvent.setup().click(confirmButton());
    expect(await screen.findByRole('alert')).toHaveTextContent('That did not work. Try again.');
  });

  it('starts from the default choice every time it opens', async () => {
    render(<Visibility />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Save view' }));
    await user.click(screen.getByText('Everyone in the team'));
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: 'Save view' }));
    expect(radio('Only me')).toBeChecked();
  });

  it('renders on the server', () => {
    expect(renderToString(<Visibility />)).toContain('aria-haspopup="dialog"');
    expect(() => renderToString(<Visibility defaultOpen />)).not.toThrow();
  });
});

describe('useChoice', () => {
  function Share({ onResult }: { onResult: (r: string | null) => void }) {
    const { choose, element } = useChoice();
    return (
      <>
        <Button
          onClick={async () =>
            onResult(
              await choose({
                title: 'Who can see this view?',
                choices: visibility.slice(0, 2),
                defaultValue: 'private',
                confirmLabel: 'Save view',
              }),
            )
          }
        >
          Share
        </Button>
        {element}
      </>
    );
  }

  it('resolves with the value on confirm and null on cancel, returning focus', async () => {
    const onResult = vi.fn();
    render(<Share onResult={onResult} />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Share' });
    await user.click(trigger);
    await screen.findByRole('dialog', { name: 'Who can see this view?' });
    await user.keyboard('{ArrowDown}');
    await user.click(screen.getByRole('button', { name: 'Save view' }));
    await waitFor(() => expect(onResult).toHaveBeenCalledWith('team'));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await waitFor(() => expect(trigger).toHaveFocus());

    await user.click(trigger);
    await screen.findByRole('dialog');
    expect(radio('Only me')).toBeChecked();
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(onResult).toHaveBeenLastCalledWith(null));
  });

  it('settles the earlier call with null when replaced or unmounted', async () => {
    const results: (string | null)[] = [];
    let api: ReturnType<typeof useChoice> | null = null;
    function Host() {
      api = useChoice();
      return api.element;
    }
    const { unmount } = render(<Host />);
    const first = api!.choose({ title: 'First', choices: visibility }).then((r) => results.push(r));
    const second = api!
      .choose({ title: 'Second', choices: visibility })
      .then((r) => results.push(r));
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
      const { choose, element } = useChoice();
      return (
        <>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button>View actions</Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent>
              <DropdownMenuItem
                onSelect={async () =>
                  onResult(
                    await choose({
                      title: 'Who can see this view?',
                      choices: visibility,
                      defaultValue: 'team',
                    }),
                  )
                }
              >
                Change visibility
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
          {element}
        </>
      );
    }
    render(<Row />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'View actions' });
    await user.click(trigger);
    await user.click(await screen.findByRole('menuitem', { name: 'Change visibility' }));
    await screen.findByRole('dialog');
    await waitFor(() => expect(radio('Everyone in the team')).toHaveFocus());
    await user.keyboard('{Escape}');
    await waitFor(() => expect(onResult).toHaveBeenCalledWith(null));
    await waitFor(() => expect(trigger).toHaveFocus());
    expect(document.body.style.pointerEvents).not.toBe('none');
  });
});
