import { describe, expect, it } from 'vitest';
import { StrictMode, useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Input, Textarea } from '../text-field/index.js';
import { Switch } from '../switch/index.js';
import { Field, FieldDescription, FieldError, FieldLabel, NativeSelect } from './index.js';

const ROLES = [
  { value: 'admin', label: 'Admin' },
  { value: 'member', label: 'Member' },
  { value: 'guest', label: 'Guest', disabled: true },
];

describe('Field', () => {
  it('wires label, description and error to the control', async () => {
    render(
      <Field invalid required>
        <FieldLabel>Email</FieldLabel>
        <Input type="email" />
        <FieldDescription>We only use this for sign-in codes.</FieldDescription>
        <FieldError>Enter a valid email address.</FieldError>
      </Field>,
    );
    const input = screen.getByRole('textbox', { name: /email/i });
    expect(input).toBeRequired();
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription(
      'We only use this for sign-in codes. Enter a valid email address.',
    );
    await expectNoA11yViolations();
  });

  it('only references descriptions that exist', () => {
    render(
      <Field>
        <FieldLabel>Name</FieldLabel>
        <Input />
        <FieldError>{null}</FieldError>
      </Field>,
    );
    expect(screen.getByRole('textbox', { name: 'Name' })).not.toHaveAttribute('aria-describedby');
  });

  it('adds and removes the error reference as validation changes', async () => {
    function Form() {
      const [value, setValue] = useState('');
      const error = value.length > 0 && value.length < 3 ? 'Use at least 3 characters.' : null;
      return (
        <Field invalid={!!error}>
          <FieldLabel>Handle</FieldLabel>
          <Input value={value} onChange={(e) => setValue(e.target.value)} />
          <FieldError>{error}</FieldError>
        </Field>
      );
    }
    render(<Form />);
    const input = screen.getByRole('textbox', { name: 'Handle' });
    const user = userEvent.setup();
    await user.type(input, 'ab');
    expect(input).toHaveAccessibleDescription('Use at least 3 characters.');
    await user.type(input, 'c');
    expect(input).not.toHaveAttribute('aria-describedby');
    expect(input).not.toHaveAttribute('aria-invalid');
  });

  it('propagates disabled to the control', () => {
    render(
      <Field disabled>
        <FieldLabel>Notes</FieldLabel>
        <Textarea />
      </Field>,
    );
    expect(screen.getByRole('textbox', { name: 'Notes' })).toBeDisabled();
  });

  it('works in StrictMode (double effects)', () => {
    render(
      <StrictMode>
        <Field>
          <FieldLabel>N</FieldLabel>
          <Input />
          <FieldDescription>desc</FieldDescription>
        </Field>
      </StrictMode>,
    );
    expect(screen.getByRole('textbox', { name: 'N' })).toHaveAccessibleDescription('desc');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <Field invalid>
        <FieldLabel>Name</FieldLabel>
        <Input />
        <FieldDescription>Help</FieldDescription>
        <FieldError>Required</FieldError>
      </Field>,
    );
    expect(html).toContain('mn-field');
    expect(html).toContain('mn-field__label');
  });
});

describe('NativeSelect', () => {
  it('is wired by Field: name, description, error, required placeholder', async () => {
    render(
      <Field invalid required>
        <FieldLabel>Role</FieldLabel>
        <NativeSelect options={ROLES} placeholder="Choose a role" defaultValue="" />
        <FieldDescription>Admins can invite people.</FieldDescription>
        <FieldError>Choose a role.</FieldError>
      </Field>,
    );
    const select = screen.getByRole('combobox', { name: /role/i });
    expect(select).toBeRequired();
    expect(select).toHaveAttribute('aria-invalid', 'true');
    expect(select).toHaveAccessibleDescription('Admins can invite people. Choose a role.');
    expect(screen.getByRole('option', { name: 'Choose a role' })).toBeDisabled();
    expect(screen.getByRole('option', { name: 'Guest' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('works uncontrolled and reports changes', async () => {
    const seen: string[] = [];
    render(
      <Field>
        <FieldLabel>Role</FieldLabel>
        <NativeSelect
          options={ROLES}
          defaultValue="member"
          onChange={(e) => seen.push(e.target.value)}
        />
      </Field>,
    );
    const select = screen.getByRole('combobox', { name: 'Role' });
    expect(select).toHaveValue('member');
    await userEvent.setup().selectOptions(select, 'admin');
    expect(select).toHaveValue('admin');
    expect(seen).toEqual(['admin']);
  });

  it('works controlled, with option children after data options', async () => {
    function Controlled() {
      const [v, setV] = useState('week');
      return (
        <>
          <NativeSelect aria-label="Range" value={v} onChange={(e) => setV(e.target.value)}>
            <option value="day">Day</option>
            <option value="week">Week</option>
          </NativeSelect>
          <output>{v}</output>
        </>
      );
    }
    render(<Controlled />);
    const select = screen.getByRole('combobox', { name: 'Range' });
    await userEvent.setup().selectOptions(select, 'day');
    expect(screen.getByRole('status')).toHaveTextContent('day');
  });

  it('compact inline label names the control and is reachable by keyboard', async () => {
    render(
      <NativeSelect
        size="sm"
        label="Sort"
        options={[
          { value: 'new', label: 'Newest' },
          { value: 'old', label: 'Oldest' },
        ]}
      />,
    );
    const select = screen.getByRole('combobox', { name: 'Sort' });
    expect(select).toHaveAttribute('data-size', 'sm');
    expect(select.closest('.mn-native-select')).toHaveAttribute('data-inline-label');
    const user = userEvent.setup();
    await user.tab();
    expect(select).toHaveFocus();
    await user.click(screen.getByText('Sort'));
    expect(select).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('takes disabled from Field', () => {
    render(
      <Field disabled>
        <FieldLabel>Plan</FieldLabel>
        <NativeSelect options={ROLES} />
      </Field>,
    );
    const select = screen.getByRole('combobox', { name: 'Plan' });
    expect(select).toBeDisabled();
    expect(select.closest('.mn-native-select')).toHaveAttribute('data-disabled');
  });

  it('renders on the server', () => {
    const html = renderToString(<NativeSelect label="Sort" options={ROLES} />);
    expect(html).toContain('mn-native-select');
    expect(html).toContain('<select');
  });
});

describe('Field layout="row"', () => {
  it('labels and describes a switch row', async () => {
    render(
      <Field layout="row">
        <FieldLabel>Weekly digest</FieldLabel>
        <FieldDescription>A summary every Monday.</FieldDescription>
        <Switch />
      </Field>,
    );
    const sw = screen.getByRole('switch', { name: 'Weekly digest' });
    expect(sw).toHaveAccessibleDescription('A summary every Monday.');
    expect(sw.closest('.mn-field')).toHaveAttribute('data-layout', 'row');
    await userEvent.setup().click(screen.getByText('Weekly digest'));
    expect(sw).toBeChecked();
    await expectNoA11yViolations();
  });

  it('keeps the stacked markup unchanged by default', () => {
    const html = renderToString(
      <Field>
        <FieldLabel>Name</FieldLabel>
        <Input />
      </Field>,
    );
    expect(html).not.toContain('data-layout');
  });
});
