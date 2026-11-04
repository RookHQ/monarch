import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel, FieldDescription } from '../../forms/form-field/index.js';
import { Dialog, DialogContent, DialogTitle } from '../../overlays/dialog/index.js';
import { DatePicker } from './index.js';

const base = { locale: 'en-GB', today: '2026-11-04' };
const day = (re: RegExp) =>
  screen.getByRole('button', { name: new RegExp(`^(\\p{L}+,? )?${re.source}`, 'u') });

function Labelled(props: Parameters<typeof DatePicker>[0]) {
  return (
    <Field>
      <FieldLabel>Due date</FieldLabel>
      <DatePicker {...base} {...props} />
    </Field>
  );
}

describe('DatePicker', () => {
  it('is a labelled text field with a calendar button, wired to Field', async () => {
    render(
      <Field required>
        <FieldLabel>Due date</FieldLabel>
        <FieldDescription>When the task should be finished.</FieldDescription>
        <DatePicker {...base} defaultValue="2026-11-10" />
      </Field>,
    );
    const input = screen.getByRole('textbox', { name: 'Due date' });
    expect(input).toHaveValue('10/11/2026');
    expect(input).toHaveAttribute('aria-required', 'true');
    expect(input).toHaveAccessibleDescription('When the task should be finished.');
    expect(input).toHaveAttribute('placeholder', 'DD/MM/YYYY');
    const trigger = screen.getByRole('button', {
      name: /^Change date, Tuesday,? 10 November 2026$/,
    });
    expect(trigger).toHaveAttribute('aria-haspopup', 'dialog');
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    await expectNoA11yViolations();
  });

  it('accepts a typed date in the locale format on blur and on Enter', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<Labelled onValueChange={onValueChange} />);
    const input = screen.getByRole('textbox', { name: 'Due date' });
    await user.type(input, '4 dec');
    await user.tab();
    expect(onValueChange).toHaveBeenLastCalledWith('2026-12-04');
    expect(input).toHaveValue('04/12/2026');
    await user.clear(input);
    await user.type(input, '31/12/2026{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith('2026-12-31');
  });

  it('uses the US order for en-US', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<Labelled locale="en-US" onValueChange={onValueChange} />);
    const input = screen.getByRole('textbox', { name: 'Due date' });
    expect(input).toHaveAttribute('placeholder', 'MM/DD/YYYY');
    await user.type(input, '12/31/2026{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith('2026-12-31');
    expect(input).toHaveValue('12/31/2026');
  });

  it('keeps invalid text, flags it and explains the format, without changing the value', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    const onValidationChange = vi.fn();
    render(
      <Labelled
        defaultValue="2026-11-10"
        onValueChange={onValueChange}
        onValidationChange={onValidationChange}
      />,
    );
    const input = screen.getByRole('textbox', { name: 'Due date' });
    await user.clear(input);
    await user.type(input, '31/02/2026');
    await user.tab();
    expect(input).toHaveValue('31/02/2026');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('Enter a date as DD/MM/YYYY.');
    expect(onValueChange).not.toHaveBeenCalled();
    expect(onValidationChange).toHaveBeenLastCalledWith('invalid');
    await expectNoA11yViolations();
    await user.clear(input);
    await user.type(input, '1/12/2026{Enter}');
    expect(input).not.toHaveAttribute('aria-invalid');
    expect(onValidationChange).toHaveBeenLastCalledWith(null);
  });

  it('rejects dates outside min and max with a message naming the bound', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<Labelled min="2026-11-04" max="2026-12-31" onValueChange={onValueChange} />);
    const input = screen.getByRole('textbox', { name: 'Due date' });
    await user.type(input, '01/11/2026{Enter}');
    expect(input).toHaveAccessibleDescription('Choose 4 Nov 2026 or later.');
    await user.clear(input);
    await user.type(input, '01/01/2027{Enter}');
    expect(input).toHaveAccessibleDescription('Choose 31 Dec 2026 or earlier.');
    expect(onValueChange).not.toHaveBeenCalled();
  });

  it('rejects disabled dates', async () => {
    const user = userEvent.setup();
    render(<Labelled isDateDisabled={(d) => d === '2026-11-07'} />);
    await user.type(screen.getByRole('textbox', { name: 'Due date' }), '7/11/2026{Enter}');
    expect(screen.getByText('That date is not available.')).toBeInTheDocument();
  });

  it('opens a calendar dialog focused on the value, picks a day, closes and returns focus', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<Labelled defaultValue="2026-11-10" onValueChange={onValueChange} />);
    await user.click(screen.getByRole('button', { name: /^Change date/ }));
    const dialog = screen.getByRole('dialog', { name: 'Choose date' });
    expect(dialog).toBeInTheDocument();
    expect(day(/10 November 2026/)).toHaveFocus();
    await user.keyboard('{ArrowRight}{ArrowRight}{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith('2026-11-12');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    const input = screen.getByRole('textbox', { name: 'Due date' });
    expect(input).toHaveFocus();
    expect(input).toHaveValue('12/11/2026');
  });

  it('opens with ArrowDown from the input, showing the typed month; Escape closes', async () => {
    const user = userEvent.setup();
    render(<Labelled />);
    const input = screen.getByRole('textbox', { name: 'Due date' });
    await user.type(input, '15/03/2027{ArrowDown}');
    expect(screen.getByRole('grid', { name: 'March 2027' })).toBeInTheDocument();
    expect(day(/15 March 2027/)).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(input).toHaveFocus();
  });

  it('offers presets relative to today, disabling those out of range', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <Labelled
        max="2026-11-08"
        onValueChange={onValueChange}
        presets={[
          { label: 'Today', days: 0 },
          { label: 'Tomorrow', days: 1 },
          { label: 'In a week', days: 7 },
          { label: 'Year end', date: '2026-12-31' },
        ]}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Choose date' }));
    const presets = screen.getByRole('group', { name: 'Quick picks' });
    expect(presets).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^In a week/ })).toBeDisabled();
    expect(screen.getByRole('button', { name: /^Year end/ })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: /^Tomorrow/ }));
    expect(onValueChange).toHaveBeenLastCalledWith('2026-11-05');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('clears with the clear button, keeping focus in the field', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<Labelled defaultValue="2026-11-10" onValueChange={onValueChange} />);
    await user.click(screen.getByRole('button', { name: 'Clear date' }));
    expect(onValueChange).toHaveBeenLastCalledWith(null);
    const input = screen.getByRole('textbox', { name: 'Due date' });
    expect(input).toHaveValue('');
    expect(input).toHaveFocus();
    expect(screen.queryByRole('button', { name: 'Clear date' })).not.toBeInTheDocument();
  });

  it('clears when the text is emptied', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<Labelled defaultValue="2026-11-10" clearable={false} onValueChange={onValueChange} />);
    expect(screen.queryByRole('button', { name: 'Clear date' })).not.toBeInTheDocument();
    await user.clear(screen.getByRole('textbox', { name: 'Due date' }));
    await user.tab();
    expect(onValueChange).toHaveBeenLastCalledWith(null);
  });

  it('controlled: shows the value prop and follows its changes', async () => {
    const user = userEvent.setup();
    function Controlled() {
      const [value, setValue] = useState<string | null>('2026-11-10');
      return (
        <>
          <Labelled value={value} onValueChange={setValue} />
          <button type="button" onClick={() => setValue('2027-01-01')}>
            New year
          </button>
          <output>{value ?? 'none'}</output>
        </>
      );
    }
    render(<Controlled />);
    await user.click(screen.getByRole('button', { name: 'New year' }));
    expect(screen.getByRole('textbox', { name: 'Due date' })).toHaveValue('01/01/2027');
    await user.click(screen.getByRole('button', { name: 'Clear date' }));
    expect(screen.getByText('none')).toBeInTheDocument();
  });

  it('submits the ISO value through name', () => {
    const { container } = render(<Labelled name="due" defaultValue="2026-11-10" />);
    expect(container.querySelector('input[type="hidden"]')).toHaveAttribute('value', '2026-11-10');
    expect(screen.getByRole('textbox', { name: 'Due date' })).not.toHaveAttribute('name');
  });

  it('disabled and read-only fields cannot open the calendar', () => {
    const { unmount } = render(
      <Field disabled>
        <FieldLabel>Due date</FieldLabel>
        <DatePicker {...base} defaultValue="2026-11-10" />
      </Field>,
    );
    expect(screen.getByRole('textbox', { name: 'Due date' })).toBeDisabled();
    expect(screen.getByRole('button', { name: /^Change date/ })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Clear date' })).not.toBeInTheDocument();
    unmount();
    render(<Labelled readOnly defaultValue="2026-11-10" />);
    expect(screen.getByRole('button', { name: /^Change date/ })).toBeDisabled();
  });

  it('works inside a Dialog: Escape closes only the calendar', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <Dialog open>
        <DialogContent>
          <DialogTitle>New task</DialogTitle>
          <Labelled onValueChange={onValueChange} />
        </DialogContent>
      </Dialog>,
    );
    await user.click(screen.getByRole('button', { name: 'Choose date' }));
    expect(screen.getByRole('dialog', { name: 'Choose date' })).toBeInTheDocument();
    expect(day(/4 November 2026/)).toHaveFocus();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog', { name: 'Choose date' })).not.toBeInTheDocument();
    expect(screen.getByRole('dialog', { name: 'New task' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Choose date' }));
    await user.click(day(/20 November 2026/));
    expect(onValueChange).toHaveBeenLastCalledWith('2026-11-20');
    expect(screen.getByRole('dialog', { name: 'New task' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<Labelled defaultValue="2026-11-10" />);
    expect(html).toContain('mn-date-picker');
    expect(html).toContain('value="10/11/2026"');
  });
});
