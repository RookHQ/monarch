import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../form-field/index.js';
import { Select, SelectContent, SelectItem, SelectTrigger } from './index.js';

describe('Select', () => {
  it('trigger inside an invalid required Field', () => {
    render(
      <Field invalid required>
        <FieldLabel>S</FieldLabel>
        <Select>
          <SelectTrigger placeholder="Pick" />
          <SelectContent>
            <SelectItem value="a">A</SelectItem>
          </SelectContent>
        </Select>
      </Field>,
    );
    const t = screen.getByRole('combobox', { name: /S/ });
    expect(t).toHaveAttribute('aria-invalid', 'true');
    expect(t).toHaveAttribute('aria-required', 'true');
    expect(t).not.toHaveAttribute('required');
  });

  it('renders a labelled trigger accessibly', async () => {
    render(
      <Select defaultValue="open">
        <SelectTrigger aria-label="Status" placeholder="Status" />
        <SelectContent>
          <SelectItem value="open">Open</SelectItem>
        </SelectContent>
      </Select>,
    );
    expect(screen.getByRole('combobox', { name: 'Status' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <Select>
        <SelectTrigger placeholder="Pick" aria-label="Pick" />
      </Select>,
    );
    expect(html).toContain('mn-select');
  });
});
