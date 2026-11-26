import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  EditableCell,
  parseEditableCellDraft,
  type EditableCellProps,
  type EditableCellValue,
} from './index.js';

function Controlled(props: Partial<EditableCellProps> & { initial?: EditableCellValue }) {
  const { initial = 'Draft plan', onCommit, ...rest } = props;
  const [value, setValue] = useState<EditableCellValue>(initial);
  return (
    <EditableCell
      label="Name"
      value={value}
      onCommit={async (next) => {
        await onCommit?.(next);
        setValue(next);
      }}
      {...rest}
    />
  );
}

describe('EditableCell', () => {
  it('opens with Enter, saves with Enter and returns focus to the cell', async () => {
    const user = userEvent.setup();
    const onCommit = vi.fn();
    render(<Controlled onCommit={onCommit} data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-editable-cell');
    const view = screen.getByRole('button', { name: /^Draft plan ?, Edit Name$/ });
    await user.tab();
    expect(view).toHaveFocus();
    await user.keyboard('{Enter}');
    const input = screen.getByRole('textbox', { name: 'Name' });
    expect(input).toHaveFocus();
    expect(input).toHaveValue('Draft plan');
    await user.clear(input);
    await user.type(input, 'Final plan{Enter}');
    expect(onCommit).toHaveBeenCalledWith('Final plan');
    const next = await screen.findByRole('button', { name: /^Final plan ?, Edit Name$/ });
    expect(next).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('opens with F2 and cancels with Escape without saving', async () => {
    const user = userEvent.setup();
    const onCommit = vi.fn();
    const onCancel = vi.fn();
    render(<Controlled onCommit={onCommit} onCancel={onCancel} />);
    await user.tab();
    await user.keyboard('{F2}');
    await user.type(screen.getByRole('textbox'), ' changed{Escape}');
    expect(onCommit).not.toHaveBeenCalled();
    expect(onCancel).toHaveBeenCalled();
    expect(screen.getByRole('button', { name: /Draft plan/ })).toHaveFocus();
  });

  it('validates numbers, bounds and required values, keeping the editor open', async () => {
    const user = userEvent.setup();
    const onCommit = vi.fn();
    render(
      <Controlled
        type="number"
        label="Quantity"
        initial={4}
        min={1}
        max={10}
        required
        onCommit={onCommit}
      />,
    );
    await user.click(screen.getByRole('button', { name: /^4 ?, Edit Quantity$/ }));
    const input = screen.getByRole('textbox', { name: 'Quantity' });
    await user.clear(input);
    await user.type(input, 'abc{Enter}');
    expect(screen.getByRole('alert')).toHaveTextContent('Enter a number.');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('Enter a number.');
    await user.clear(input);
    await user.type(input, '12{Enter}');
    expect(screen.getByRole('alert')).toHaveTextContent('Enter 10 or less.');
    await user.clear(input);
    await user.keyboard('{Enter}');
    expect(screen.getByRole('alert')).toHaveTextContent('Enter a value.');
    await user.type(input, '7{Enter}');
    expect(onCommit).toHaveBeenCalledWith(7);
    await expectNoA11yViolations();
  });

  it('runs custom validation and keeps a failed save open with its message', async () => {
    const user = userEvent.setup();
    const onCommit = vi.fn().mockRejectedValueOnce(new Error('Name already used.'));
    render(
      <Controlled
        validate={(v) => (v === 'x' ? 'Too short.' : null)}
        onCommit={onCommit}
        messages={{ edit: 'Change' }}
      />,
    );
    await user.click(screen.getByRole('button', { name: /^Draft plan ?, Change Name$/ }));
    const input = screen.getByRole('textbox');
    await user.clear(input);
    await user.type(input, 'x{Enter}');
    expect(screen.getByRole('alert')).toHaveTextContent('Too short.');
    await user.clear(input);
    await user.type(input, 'Taken{Enter}');
    expect(await screen.findByText('Name already used.')).toBeInTheDocument();
    expect(input).toHaveFocus();
    await user.keyboard('{Enter}');
    await screen.findByRole('button', { name: /Taken/ });
  });

  it('returns focus to a select editor after a failed save', async () => {
    const user = userEvent.setup();
    const onCommit = vi.fn().mockRejectedValueOnce(new Error('Not allowed.'));
    render(
      <Controlled
        type="select"
        label="Status"
        initial="open"
        options={[
          { value: 'open', label: 'Open' },
          { value: 'done', label: 'Done' },
        ]}
        onCommit={onCommit}
      />,
    );
    await user.click(screen.getByRole('button', { name: /^Open ?, Edit Status$/ }));
    const select = screen.getByRole('combobox', { name: 'Status' });
    await user.selectOptions(select, 'done');
    await user.keyboard('{Enter}');
    expect(await screen.findByText('Not allowed.')).toBeInTheDocument();
    // The select is disabled while saving; focus comes back once it is enabled again.
    expect(screen.getByRole('combobox', { name: 'Status' })).toBeEnabled();
    expect(screen.getByRole('combobox', { name: 'Status' })).toHaveFocus();
  });

  it('edits a select and a date, and saves on blur', async () => {
    const user = userEvent.setup();
    const onCommit = vi.fn();
    render(
      <>
        <Controlled
          type="select"
          label="Status"
          initial="open"
          options={[
            { value: 'open', label: 'Open' },
            { value: 'done', label: 'Done' },
          ]}
          onCommit={onCommit}
        />
        <Controlled type="date" label="Due" initial="2026-01-31" onCommit={onCommit} />
        <button type="button">Elsewhere</button>
      </>,
    );
    await user.click(screen.getByRole('button', { name: /^Open ?, Edit Status$/ }));
    const select = screen.getByRole('combobox', { name: 'Status' });
    expect(select).toHaveFocus();
    await user.selectOptions(select, 'done');
    await user.click(screen.getByRole('button', { name: 'Elsewhere' }));
    expect(onCommit).toHaveBeenCalledWith('done');
    expect(
      await screen.findByRole('button', { name: /^Done ?, Edit Status$/ }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /^2026-01-31 ?, Edit Due$/ }));
    const date = screen.getByLabelText('Due');
    expect(date).toHaveAttribute('type', 'date');
    await user.clear(date);
    await user.type(date, '2026-02-15');
    await user.keyboard('{Enter}');
    expect(onCommit).toHaveBeenLastCalledWith('2026-02-15');
  });

  it('follows a parent with trigger="none" and starts from a typed character', async () => {
    const user = userEvent.setup();
    const onEditingChange = vi.fn();
    const { rerender } = render(
      <EditableCell
        trigger="none"
        label="Name"
        value="Alpha"
        onCommit={() => {}}
        onEditingChange={onEditingChange}
      />,
    );
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getByText('Alpha')).toBeInTheDocument();
    rerender(
      <EditableCell
        trigger="none"
        label="Name"
        value="Alpha"
        editing
        initialDraft="B"
        onCommit={() => {}}
        onEditingChange={onEditingChange}
      />,
    );
    const input = screen.getByRole('textbox', { name: 'Name' });
    await waitFor(() => expect(input).toHaveValue('B'));
    await user.keyboard('{Escape}');
    expect(onEditingChange).toHaveBeenCalledWith(false, 'cancel');
  });

  it('is plain text when disabled', () => {
    render(<EditableCell label="Name" value="Alpha" disabled onCommit={() => {}} />);
    expect(screen.getByRole('button', { name: 'Alpha' })).toBeDisabled();
  });

  it('parses drafts by type', () => {
    expect(parseEditableCellDraft('number', ' 1 200.5 ')).toEqual({ value: 1200.5 });
    expect(parseEditableCellDraft('number', 'x')).toEqual({ error: 'invalidNumber' });
    expect(parseEditableCellDraft('date', '2026-02-30')).toEqual({ error: 'invalidDate' });
    expect(parseEditableCellDraft('date', '2026-02-28')).toEqual({ value: '2026-02-28' });
    expect(parseEditableCellDraft('text', '  ')).toEqual({ value: null });
  });

  it('renders on the server', () => {
    const html = renderToString(<EditableCell label="Name" value="Alpha" onCommit={() => {}} />);
    expect(html).toContain('mn-editable-cell');
    expect(html).toContain('Alpha');
  });
});
