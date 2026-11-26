import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldError, FieldLabel } from '../form-field/index.js';
import { CheckboxGroup, type CheckboxGroupOption } from './index.js';

const channels: CheckboxGroupOption[] = [
  { value: 'email', label: 'Email', description: 'To your sign-in address.' },
  { value: 'sms', label: 'Text message' },
  { value: 'push', label: 'Push notification' },
  { value: 'post', label: 'Post', disabled: true },
];

describe('CheckboxGroup', () => {
  it('is a labelled group of named, described checkboxes', async () => {
    render(<CheckboxGroup label="Notify me by" description="Pick any." options={channels} />);
    const group = screen.getByRole('group', { name: 'Notify me by' });
    expect(group).toHaveAccessibleDescription('Pick any.');
    expect(screen.getAllByRole('checkbox')).toHaveLength(4);
    expect(screen.getByRole('checkbox', { name: 'Email' })).toHaveAccessibleDescription(
      'To your sign-in address.',
    );
    expect(screen.getByRole('checkbox', { name: 'Post' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('toggles with click and Space and reports values in option order', async () => {
    const onValueChange = vi.fn();
    render(<CheckboxGroup label="Notify me by" options={channels} onValueChange={onValueChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('checkbox', { name: 'Push notification' }));
    expect(onValueChange).toHaveBeenLastCalledWith(['push']);
    await user.click(screen.getByText('Email'));
    expect(onValueChange).toHaveBeenLastCalledWith(['email', 'push']);
    await user.tab();
    expect(screen.getByRole('checkbox', { name: 'Text message' })).toHaveFocus();
    await user.keyboard(' ');
    expect(onValueChange).toHaveBeenLastCalledWith(['email', 'sms', 'push']);
    await user.keyboard('{Shift>}{Tab}{/Shift} ');
    expect(onValueChange).toHaveBeenLastCalledWith(['sms', 'push']);
  });

  it('has a tri-state select all that skips disabled choices', async () => {
    const onValueChange = vi.fn();
    render(
      <CheckboxGroup
        label="Notify me by"
        options={channels}
        selectAll
        defaultValue={['email']}
        onValueChange={onValueChange}
      />,
    );
    const all = screen.getByRole('checkbox', { name: 'Select all' });
    expect(all).toHaveAttribute('aria-checked', 'mixed');
    const user = userEvent.setup();
    await user.click(all);
    expect(onValueChange).toHaveBeenLastCalledWith(['email', 'sms', 'push']);
    expect(all).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Post' })).not.toBeChecked();
    await user.keyboard(' ');
    expect(onValueChange).toHaveBeenLastCalledWith([]);
    expect(all).toHaveAttribute('aria-checked', 'false');
    await expectNoA11yViolations();
  });

  it('keeps checked disabled choices when select all clears', async () => {
    const onValueChange = vi.fn();
    render(
      <CheckboxGroup
        aria-label="Channels"
        options={channels}
        selectAll
        messages={{ selectAll: 'All channels' }}
        defaultValue={['email', 'sms', 'push', 'post']}
        onValueChange={onValueChange}
      />,
    );
    const all = screen.getByRole('checkbox', { name: 'All channels' });
    expect(all).toBeChecked();
    await userEvent.setup().click(all);
    expect(onValueChange).toHaveBeenLastCalledWith(['post']);
  });

  it('follows the value prop when controlled', async () => {
    function Controlled() {
      const [value, setValue] = useState<string[]>(['sms']);
      return (
        <>
          <CheckboxGroup
            label="Notify me by"
            options={channels}
            value={value}
            onValueChange={setValue}
          />
          <output>{value.join(',')}</output>
        </>
      );
    }
    render(<Controlled />);
    await userEvent.setup().click(screen.getByRole('checkbox', { name: 'Email' }));
    expect(screen.getByRole('status')).toHaveTextContent('email,sms');
  });

  it('takes its label, error, invalid and disabled from a Field without duplicate ids', async () => {
    const { container } = render(
      <Field invalid>
        <FieldLabel>Topics</FieldLabel>
        <CheckboxGroup
          options={[
            { value: 'a', label: 'Product news' },
            { value: 'b', label: 'Events' },
          ]}
        />
        <FieldError>Choose at least one topic.</FieldError>
      </Field>,
    );
    const group = screen.getByRole('group', { name: 'Topics' });
    expect(group).toHaveAttribute('data-invalid', 'true');
    for (const box of screen.getAllByRole('checkbox'))
      expect(box).toHaveAttribute('aria-invalid', 'true');
    expect(group).toHaveAccessibleDescription('Choose at least one topic.');
    const ids = [...container.querySelectorAll('[id]')].map((el) => el.id);
    expect(new Set(ids).size).toBe(ids.length);
    for (const box of screen.getAllByRole('checkbox')) expect(box).not.toBeRequired();
    await expectNoA11yViolations();
  });

  it('submits every checked value under `name`', () => {
    let submitted: FormData | null = null;
    render(
      <form
        aria-label="Preferences"
        onSubmit={(event) => {
          event.preventDefault();
          submitted = new FormData(event.currentTarget);
        }}
      >
        <CheckboxGroup
          label="Notify me by"
          name="channel"
          options={channels}
          defaultValue={['email', 'push']}
        />
      </form>,
    );
    fireEvent.submit(screen.getByRole('form', { name: 'Preferences' }));
    expect(submitted!.getAll('channel')).toEqual(['email', 'push']);
  });

  it('renders on the server', () => {
    const html = renderToString(
      <CheckboxGroup label="Notify me by" options={channels} selectAll orientation="horizontal" />,
    );
    expect(html).toContain('mn-checkbox-group');
    expect(html).toContain('data-orientation="horizontal"');
  });
});
