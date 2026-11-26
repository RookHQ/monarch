import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldDescription, FieldLabel } from '../form-field/index.js';
import { ColorPicker, isHexColor, normalizeHex } from './index.js';

const SWATCHES = [
  { value: '#2563EB', label: 'Blue' },
  { value: '#16a34a', label: 'Green' },
  { value: '#f00', label: 'Red' },
  { value: 'url(javascript:alert(1))', label: 'Broken' },
];

describe('hex helpers', () => {
  it('validates and normalises hex colours', () => {
    expect(isHexColor('#abc')).toBe(true);
    expect(isHexColor('#AABBCC')).toBe(true);
    expect(isHexColor('abc')).toBe(false);
    expect(isHexColor('#abcd')).toBe(false);
    expect(normalizeHex(' #ABC ')).toBe('#aabbcc');
    expect(normalizeHex('2563EB')).toBe('#2563eb');
    expect(normalizeHex('red')).toBeNull();
    expect(normalizeHex('#12345')).toBeNull();
  });
});

describe('ColorPicker', () => {
  it('is a group named by its Field with named swatch radios', async () => {
    render(
      <Field>
        <FieldLabel>Label colour</FieldLabel>
        <ColorPicker swatches={SWATCHES} defaultValue="#16A34A" />
        <FieldDescription>Shown on tags.</FieldDescription>
      </Field>,
    );
    const group = screen.getByRole('group', { name: 'Label colour' });
    expect(group).toHaveAccessibleDescription('Shown on tags.');
    const radios = screen.getAllByRole('radio');
    expect(radios).toHaveLength(3); // the invalid swatch is skipped
    expect(screen.getByRole('radio', { name: 'Green, #16a34a' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    expect(screen.getByRole('radio', { name: 'Red, #ff0000' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Hex colour' })).toHaveValue('#16a34a');
    expect(screen.getByLabelText('Custom colour')).toHaveValue('#16a34a');
    await expectNoA11yViolations();
  });

  it('moves and selects with arrow keys, Home and End (roving focus)', async () => {
    const onValueChange = vi.fn();
    render(
      <ColorPicker
        aria-label="Colour"
        swatches={SWATCHES}
        defaultValue="#2563eb"
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    await user.tab();
    const blue = screen.getByRole('radio', { name: /Blue/ });
    expect(blue).toHaveFocus();
    expect(screen.getByRole('radio', { name: /Green/ })).toHaveAttribute('tabindex', '-1');
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('radio', { name: /Green/ })).toHaveFocus();
    expect(onValueChange).toHaveBeenLastCalledWith('#16a34a');
    await user.keyboard('{End}');
    expect(onValueChange).toHaveBeenLastCalledWith('#ff0000');
    await user.keyboard('{ArrowRight}');
    expect(blue).toHaveFocus();
    await user.keyboard('{ArrowLeft}{Home}');
    expect(onValueChange).toHaveBeenLastCalledWith('#2563eb');
    // Tab leaves the swatches for the custom row.
    await user.tab();
    expect(screen.getByLabelText('Custom colour')).toHaveFocus();
  });

  it('accepts typed hex, normalises it, and flags invalid text', async () => {
    const onValueChange = vi.fn();
    render(<ColorPicker aria-label="Colour" swatches={SWATCHES} onValueChange={onValueChange} />);
    const hex = screen.getByRole('textbox', { name: 'Hex colour' });
    const user = userEvent.setup();
    await user.type(hex, 'ABC{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith('#aabbcc');
    expect(hex).toHaveValue('#aabbcc');
    await user.clear(hex);
    await user.type(hex, 'blue');
    await user.tab();
    expect(hex).toHaveAttribute('aria-invalid', 'true');
    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Enter a hex colour like #2563eb.');
    expect(hex).toHaveAccessibleDescription('Enter a hex colour like #2563eb.');
    expect(onValueChange).toHaveBeenCalledTimes(1);
    hex.focus();
    await user.keyboard('{Escape}');
    expect(hex).toHaveValue('#aabbcc');
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('takes a colour from the native picker and follows a controlled value', async () => {
    function Controlled() {
      const [color, setColor] = useState<string | null>('#2563eb');
      return (
        <>
          <ColorPicker aria-label="Colour" name="color" value={color} onValueChange={setColor} />
          <output>{color}</output>
        </>
      );
    }
    const { container } = render(<Controlled />);
    fireEvent.change(screen.getByLabelText('Custom colour'), { target: { value: '#123456' } });
    expect(screen.getByRole('status')).toHaveTextContent('#123456');
    expect(container.querySelector('input[name="color"]')).toHaveValue('#123456');
    expect(
      screen.getAllByRole('radio').every((r) => r.getAttribute('aria-checked') === 'false'),
    ).toBe(true);
    await userEvent.setup().click(screen.getByRole('radio', { name: 'Teal, #0d9488' }));
    expect(screen.getByRole('status')).toHaveTextContent('#0d9488');
  });

  it('hides the custom row and disables everything when asked', () => {
    render(
      <Field disabled>
        <FieldLabel>Colour</FieldLabel>
        <ColorPicker allowCustom={false} defaultValue="#2563eb" />
      </Field>,
    );
    expect(screen.queryByRole('textbox')).toBeNull();
    for (const radio of screen.getAllByRole('radio')) expect(radio).toBeDisabled();
  });

  it('uses translated text', () => {
    render(
      <ColorPicker
        aria-label="Farbe"
        swatches={[{ value: '#2563eb', label: 'Blau' }]}
        messages={{ swatches: 'Vorgaben', hex: 'Hex-Farbe', custom: 'Eigene Farbe' }}
      />,
    );
    expect(screen.getByRole('radiogroup', { name: 'Vorgaben' })).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Hex-Farbe' })).toBeInTheDocument();
    expect(screen.getByLabelText('Eigene Farbe')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<ColorPicker aria-label="Colour" defaultValue="#2563eb" />);
    expect(html).toContain('mn-color-picker');
    expect(html).toContain('aria-checked="true"');
  });

  it('does not submit its hidden input while disabled', () => {
    const { container } = render(
      <ColorPicker aria-label="Colour" name="colour" defaultValue="#2563eb" disabled />,
    );
    expect(container.querySelector('input[type="hidden"][name="colour"]')).toBeDisabled();
  });
});
