import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { InlineEditText } from './index.js';

function deferred() {
  let resolve!: () => void;
  let reject!: (e: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

describe('InlineEditText', () => {
  it('view mode is a button named by label and value, with an edit hint', async () => {
    render(<InlineEditText label="Title" defaultValue="Quarterly report" onSave={() => {}} />);
    const view = screen.getByRole('button', { name: 'Title: Quarterly report' });
    expect(view).toHaveAccessibleDescription('Edit');
    await expectNoA11yViolations();
  });

  it('click to edit, Enter saves, focus returns to the text (uncontrolled)', async () => {
    const onSave = vi.fn();
    render(<InlineEditText label="Title" defaultValue="Draft" onSave={onSave} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Title/ }));
    const input = screen.getByRole('textbox', { name: 'Title' });
    expect(input).toHaveFocus();
    expect(input).toHaveValue('Draft');
    await user.clear(input);
    await user.type(input, '  Final  {Enter}');
    expect(onSave).toHaveBeenCalledWith('Final');
    const view = screen.getByRole('button', { name: 'Title: Final' });
    expect(view).toHaveFocus();
  });

  it('Enter on the text starts editing; Escape cancels and restores', async () => {
    const onSave = vi.fn();
    const onCancel = vi.fn();
    render(
      <InlineEditText label="Name" defaultValue="Alpha" onSave={onSave} onCancel={onCancel} />,
    );
    const user = userEvent.setup();
    await user.tab();
    await user.keyboard('{Enter}');
    const input = screen.getByRole('textbox', { name: 'Name' });
    expect(input).toHaveFocus();
    await user.type(input, ' changed');
    await user.keyboard('{Escape}');
    expect(onSave).not.toHaveBeenCalled();
    expect(onCancel).toHaveBeenCalledOnce();
    expect(screen.getByRole('button', { name: 'Name: Alpha' })).toHaveFocus();
  });

  it('saves on blur without stealing focus back', async () => {
    const onSave = vi.fn();
    render(
      <>
        <InlineEditText label="Title" defaultValue="A" onSave={onSave} />
        <button type="button">Elsewhere</button>
      </>,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Title/ }));
    await user.type(screen.getByRole('textbox'), 'B');
    await user.click(screen.getByRole('button', { name: 'Elsewhere' }));
    expect(onSave).toHaveBeenCalledWith('AB');
    expect(screen.getByRole('button', { name: 'Elsewhere' })).toHaveFocus();
  });

  it('does not call onSave when the value is unchanged', async () => {
    const onSave = vi.fn();
    render(<InlineEditText label="Title" defaultValue="Same" onSave={onSave} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Title/ }));
    await user.keyboard('{End}  {Enter}');
    expect(onSave).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Title: Same' })).toBeInTheDocument();
  });

  it('async save: pending state, then error kept in edit mode with the draft', async () => {
    const pending = deferred();
    const onSaveError = vi.fn();
    render(
      <InlineEditText
        label="Title"
        defaultValue="Old"
        onSave={() => pending.promise}
        onSaveError={onSaveError}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Title/ }));
    const input = screen.getByRole('textbox', { name: 'Title' });
    await user.clear(input);
    await user.type(input, 'New{Enter}');
    expect(input).toHaveAttribute('aria-busy', 'true');
    expect(input).toHaveAttribute('readonly');
    expect(screen.getByRole('status', { name: 'Saving' })).toBeInTheDocument();
    const err = new Error('Someone else changed this title.');
    await act(async () => {
      pending.reject(err);
      await pending.promise.catch(() => {});
    });
    expect(input).toHaveValue('New');
    expect(input).not.toHaveAttribute('readonly');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('Someone else changed this title.');
    expect(screen.getByRole('alert')).toHaveTextContent('Someone else changed this title.');
    expect(input).toHaveFocus();
    expect(onSaveError).toHaveBeenCalledWith(err, 'New');
    await expectNoA11yViolations();
  });

  it('a failed draft is not resaved on blur, but is retried after an edit', async () => {
    const onSave = vi.fn().mockRejectedValueOnce('nope').mockResolvedValueOnce(undefined);
    render(
      <>
        <InlineEditText label="Title" defaultValue="A" onSave={onSave} />
        <button type="button">Elsewhere</button>
      </>,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Title/ }));
    await user.type(screen.getByRole('textbox'), 'B{Enter}');
    expect(await screen.findByRole('alert')).toHaveTextContent('Couldn’t save. Try again.');
    await user.click(screen.getByRole('button', { name: 'Elsewhere' }));
    expect(onSave).toHaveBeenCalledTimes(1);
    await user.type(screen.getByRole('textbox'), 'C{Enter}');
    expect(onSave).toHaveBeenLastCalledWith('ABC');
    expect(await screen.findByRole('button', { name: 'Title: ABC' })).toBeInTheDocument();
  });

  it('validate blocks the save and shows the message', async () => {
    const onSave = vi.fn();
    render(
      <InlineEditText
        label="Title"
        defaultValue="Ok"
        onSave={onSave}
        validate={(v) => (v.length === 0 ? 'Title can’t be empty.' : null)}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Title/ }));
    const input = screen.getByRole('textbox');
    await user.clear(input);
    await user.keyboard('{Enter}');
    expect(onSave).not.toHaveBeenCalled();
    expect(input).toHaveAccessibleDescription('Title can’t be empty.');
    await user.type(input, 'Better{Enter}');
    expect(onSave).toHaveBeenCalledWith('Better');
  });

  it('controlled value and editing, with an external error (conflict path)', async () => {
    function Controlled() {
      const [value, setValue] = useState('v1');
      const [editing, setEditing] = useState(false);
      return (
        <>
          <InlineEditText
            label="Title"
            value={value}
            onSave={setValue}
            editing={editing}
            onEditingChange={setEditing}
            error={editing ? 'This title was changed by someone else.' : null}
          />
          <button type="button" onClick={() => setValue('v2 from server')}>
            Server update
          </button>
          <output>{editing ? 'editing' : 'viewing'}</output>
        </>
      );
    }
    render(<Controlled />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Server update' }));
    expect(screen.getByRole('button', { name: 'Title: v2 from server' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Title/ }));
    expect(screen.getByRole('status')).toHaveTextContent('editing');
    expect(screen.getByRole('alert')).toHaveTextContent('changed by someone else');
    await user.type(screen.getByRole('textbox'), '!{Enter}');
    expect(screen.getByRole('button', { name: 'Title: v2 from server!' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('viewing');
  });

  it('multiline: Enter adds a line, Ctrl+Enter saves; Save and Cancel buttons work', async () => {
    const onSave = vi.fn();
    render(
      <InlineEditText label="Description" defaultValue="Line one" onSave={onSave} multiline />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Description/ }));
    const textarea = screen.getByRole('textbox', { name: 'Description' });
    expect(textarea.tagName).toBe('TEXTAREA');
    await user.type(textarea, '{Enter}Line two');
    expect(onSave).not.toHaveBeenCalled();
    await user.keyboard('{Control>}{Enter}{/Control}');
    expect(onSave).toHaveBeenCalledWith('Line one\nLine two');

    await user.click(screen.getByRole('button', { name: /Description/ }));
    await user.type(screen.getByRole('textbox'), ' more');
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onSave).toHaveBeenCalledTimes(1);
    await user.click(screen.getByRole('button', { name: /Description/ }));
    await user.type(screen.getByRole('textbox'), '!');
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSave).toHaveBeenLastCalledWith('Line one\nLine two!');
    await expectNoA11yViolations();
  });

  it('tabbing to the action buttons does not save', async () => {
    const onSave = vi.fn();
    render(<InlineEditText label="Notes" defaultValue="x" onSave={onSave} showActions />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Notes/ }));
    await user.type(screen.getByRole('textbox'), 'y');
    await user.tab();
    expect(screen.getByRole('button', { name: 'Save' })).toHaveFocus();
    expect(onSave).not.toHaveBeenCalled();
  });

  it('disabled shows plain text and empty values show the placeholder', () => {
    render(
      <>
        <InlineEditText label="Title" defaultValue="Locked" disabled onSave={() => {}} />
        <InlineEditText label="Summary" placeholder="Add a summary" onSave={() => {}} />
      </>,
    );
    expect(screen.queryByRole('button', { name: /Locked/ })).toBeNull();
    expect(screen.getByText('Locked')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Summary: Add a summary' })).toBeInTheDocument();
  });

  it('renders on the server in both modes', () => {
    const html = renderToString(
      <>
        <InlineEditText label="Title" defaultValue="Hello" onSave={() => {}} />
        <InlineEditText label="Title" defaultValue="Hello" defaultEditing onSave={() => {}} />
      </>,
    );
    expect(html).toContain('data-state="viewing"');
    expect(html).toContain('data-state="editing"');
    expect(html).toContain('<input');
  });
});
