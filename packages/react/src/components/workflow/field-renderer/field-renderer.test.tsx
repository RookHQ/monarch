import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  FieldRenderer,
  isFormFieldValueEmpty,
  isFormFieldVisible,
  validateFormField,
  type FieldRendererProps,
  type FormFieldDefinition,
  type FormFieldValue,
} from './index.js';

const OPTIONS = [
  { value: 'remote', label: 'Remote' },
  { value: 'office', label: 'Office' },
];

function Harness({
  field,
  initial = null,
  onValue,
  ...props
}: Partial<FieldRendererProps> & {
  field: FormFieldDefinition;
  initial?: FormFieldValue;
  onValue?: (v: FormFieldValue) => void;
}) {
  const [value, setValue] = useState<FormFieldValue>(initial);
  return (
    <FieldRenderer
      {...props}
      field={field}
      value={value}
      onChange={(v) => {
        setValue(v);
        onValue?.(v);
      }}
    />
  );
}

describe('FieldRenderer', () => {
  it('renders a labelled text input with help, required marker and error, without axe violations', async () => {
    render(
      <Harness
        field={{
          id: 'email',
          type: 'email',
          label: 'Work email',
          required: true,
          description: 'We reply here.',
        }}
        error="Enter an email address."
      />,
    );
    const input = screen.getByRole('textbox', { name: /Work email/ });
    expect(input).toHaveAttribute('type', 'email');
    expect(input).toBeRequired();
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription(/We reply here\./);
    expect(input).toHaveAccessibleDescription(/Enter an email address\./);
    expect(input.closest('.mn-field-renderer')).toHaveAttribute('data-type', 'email');
    await expectNoA11yViolations();
  });

  it('types into text, long text and number fields', async () => {
    const user = userEvent.setup();
    const onValue = vi.fn();
    const { unmount } = render(
      <Harness
        field={{ id: 'n', type: 'number', label: 'Years of experience', min: 0 }}
        onValue={onValue}
      />,
    );
    await user.type(screen.getByRole('spinbutton', { name: 'Years of experience' }), '7');
    expect(onValue).toHaveBeenLastCalledWith(7);
    unmount();
    render(
      <Harness
        field={{ id: 'l', type: 'long-text', label: 'About you', rows: 3 }}
        onValue={onValue}
      />,
    );
    await user.type(screen.getByRole('textbox', { name: 'About you' }), 'Hi');
    expect(onValue).toHaveBeenLastCalledWith('Hi');
  });

  it('radio, yes / no and checkboxes are fieldsets operated from the keyboard', async () => {
    const user = userEvent.setup();
    const onValue = vi.fn();
    const { unmount } = render(
      <Harness
        field={{ id: 'w', type: 'radio', label: 'Where do you work?', options: OPTIONS }}
        onValue={onValue}
      />,
    );
    expect(screen.getByRole('group', { name: 'Where do you work?' })).toBeInTheDocument();
    await user.click(screen.getByRole('radio', { name: 'Remote' }));
    await user.keyboard('{ArrowDown}');
    expect(onValue).toHaveBeenLastCalledWith('office');
    await expectNoA11yViolations();
    unmount();
    const { unmount: u2 } = render(
      <Harness field={{ id: 'y', type: 'yes-no', label: 'Can you relocate?' }} onValue={onValue} />,
    );
    await user.click(screen.getByRole('radio', { name: 'No' }));
    expect(onValue).toHaveBeenLastCalledWith(false);
    u2();
    render(
      <Harness
        field={{ id: 'c', type: 'checkboxes', label: 'Places', options: OPTIONS }}
        onValue={onValue}
      />,
    );
    const remote = screen.getByRole('checkbox', { name: 'Remote' });
    remote.focus();
    await user.keyboard(' ');
    await user.click(screen.getByRole('checkbox', { name: 'Office' }));
    expect(onValue).toHaveBeenLastCalledWith(['remote', 'office']);
  });

  it('select, consent and rating', async () => {
    const user = userEvent.setup();
    const onValue = vi.fn();
    render(
      <>
        <Harness
          field={{ id: 's', type: 'select', label: 'Team', options: OPTIONS }}
          onValue={onValue}
        />
        <Harness
          field={{
            id: 'k',
            type: 'consent',
            label: 'Consent',
            consentText: 'I agree to be contacted.',
            required: true,
          }}
          onValue={onValue}
        />
        <Harness field={{ id: 'r', type: 'rating', label: 'How was it?' }} onValue={onValue} />
      </>,
    );
    await user.selectOptions(screen.getByRole('combobox', { name: 'Team' }), 'office');
    expect(onValue).toHaveBeenLastCalledWith('office');
    await user.click(screen.getByRole('checkbox', { name: /I agree to be contacted/ }));
    expect(onValue).toHaveBeenLastCalledWith(true);
    expect(screen.getByRole('radiogroup', { name: 'How was it?' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('shows the automatic-fill note with the assistant glyph, and asks for a check when unsure', () => {
    const { rerender } = render(
      <FieldRenderer
        field={{ id: 'name', type: 'short-text', label: 'Full name' }}
        value="Alex Morgan"
        onChange={() => {}}
        confidence={0.9}
      />,
    );
    const input = screen.getByRole('textbox', { name: 'Full name' });
    expect(input).toHaveAccessibleDescription('Filled in automatically. Check it is right.');
    expect(input.closest('.mn-field-renderer')).toHaveAttribute('data-ai', 'filled');
    expect(document.querySelector('.mn-field-renderer__ai svg')).not.toBeNull();
    rerender(
      <FieldRenderer
        field={{ id: 'name', type: 'short-text', label: 'Full name' }}
        value="Alex Morgan"
        onChange={() => {}}
        confidence={0.3}
      />,
    );
    expect(input).toHaveAccessibleDescription('Filled in automatically, but unsure. Please check.');
    expect(input.closest('.mn-field-renderer')).toHaveAttribute('data-ai', 'check');
  });

  it('renders a section heading and read-only fields', () => {
    render(
      <>
        <FieldRenderer
          field={{ id: 'h', type: 'section', label: 'About you' }}
          value={null}
          onChange={() => {}}
        />
        <FieldRenderer
          field={{ id: 'e', type: 'email', label: 'Email', readOnly: true }}
          value="a@example.com"
          onChange={() => {}}
        />
      </>,
    );
    expect(screen.getByRole('heading', { level: 3, name: 'About you' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Email' })).toHaveAttribute('readonly');
  });

  it('lists chosen files and removes them', async () => {
    const user = userEvent.setup();
    const onValue = vi.fn();
    render(
      <Harness
        field={{ id: 'f', type: 'file', label: 'CV', maxFiles: 2 }}
        initial={[{ name: 'cv.pdf', size: 1200 }]}
        onValue={onValue}
      />,
    );
    expect(screen.getByText('cv.pdf')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Remove cv.pdf' }));
    expect(onValue).toHaveBeenLastCalledWith([]);
  });

  it('helpers validate answers and decide visibility', () => {
    expect(validateFormField({ id: 'a', type: 'short-text', label: 'A', required: true }, '')).toBe(
      'This field is required.',
    );
    expect(validateFormField({ id: 'a', type: 'email', label: 'A' }, 'nope')).toMatch(/email/);
    expect(validateFormField({ id: 'a', type: 'url', label: 'A' }, 'ftp://x.y')).toMatch(
      /web address/,
    );
    expect(validateFormField({ id: 'a', type: 'number', label: 'A', max: 5 }, 9)).toBe(
      'Enter 5 or less.',
    );
    expect(
      validateFormField({ id: 'a', type: 'checkboxes', label: 'A', minSelections: 2 }, ['x']),
    ).toBe('Choose at least 2.');
    expect(validateFormField({ id: 'a', type: 'consent', label: 'A', required: true }, false)).toBe(
      'Tick this to continue.',
    );
    expect(validateFormField({ id: 'a', type: 'yes-no', label: 'A', required: true }, null)).toBe(
      'This field is required.',
    );
    expect(isFormFieldValueEmpty([])).toBe(true);
    const field: FormFieldDefinition = {
      id: 'why',
      type: 'long-text',
      label: 'Why?',
      conditions: [{ id: 'c', field: 'w', operator: 'eq', value: 'office' }],
    };
    expect(isFormFieldVisible(field, { w: 'office' })).toBe(true);
    expect(isFormFieldVisible(field, { w: 'remote' })).toBe(false);
  });

  it('renders on the server', () => {
    const html = renderToString(
      <FieldRenderer
        field={{ id: 'd', type: 'date', label: 'Start date' }}
        value="2026-04-01"
        onChange={() => {}}
      />,
    );
    expect(html).toContain('mn-field-renderer');
    expect(html).toContain('Start date');
  });
});
