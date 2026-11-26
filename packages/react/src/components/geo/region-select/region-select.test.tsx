import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../../forms/form-field/index.js';
import { BUILT_IN_REGIONS, RegionSelect, getBuiltInRegions, type RegionOption } from './index.js';

const labelOf = (o: Element) => o.querySelector('.mn-menu__text')?.firstChild?.textContent;

describe('built-in regions', () => {
  it('ships compact lists with ISO 3166-2 codes', () => {
    expect(Object.keys(BUILT_IN_REGIONS).sort()).toEqual(['AU', 'CA', 'GB', 'IN', 'US']);
    expect(getBuiltInRegions('us')?.regions).toHaveLength(51);
    expect(getBuiltInRegions('IN')?.regions).toHaveLength(36);
    expect(getBuiltInRegions('CA')?.label).toBe('Province or territory');
    for (const [country, { regions }] of Object.entries(BUILT_IN_REGIONS)) {
      for (const r of regions) expect(r.code).toMatch(new RegExp(`^${country}-[A-Z]{2,3}$`));
    }
    expect(getBuiltInRegions('FR')).toBeNull();
  });
});

describe('RegionSelect', () => {
  it('lists a built-in country, searches by name or code and selects', async () => {
    const onValueChange = vi.fn();
    render(
      <Field>
        <FieldLabel>Province</FieldLabel>
        <RegionSelect country="ca" onValueChange={onValueChange} />
      </Field>,
    );
    const input = screen.getByRole('combobox', { name: 'Province' });
    const user = userEvent.setup();
    await user.type(input, 'new');
    expect(screen.getAllByRole('option').map(labelOf)).toEqual([
      'New Brunswick',
      'Newfoundland and Labrador',
    ]);
    await user.clear(input);
    await user.type(input, 'ca-qc');
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith('CA-QC');
    expect(input).toHaveValue('Quebec');
    await expectNoA11yViolations();
  });

  it('is disabled with a prompt until a country is chosen', async () => {
    render(<RegionSelect aria-label="Region" country={null} />);
    const input = screen.getByRole('combobox', { name: 'Region' });
    expect(input).toBeDisabled();
    expect(input).toHaveAttribute('placeholder', 'Choose a country first');
    await expectNoA11yViolations();
  });

  it('uses the regions prop over built-in lists', async () => {
    const regions: RegionOption[] = [
      { code: 'US-NORTH', name: 'North office' },
      { code: 'US-SOUTH', name: 'South office' },
    ];
    render(<RegionSelect aria-label="Region" country="US" regions={regions} showCode />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Show options' }));
    const options = await screen.findAllByRole('option');
    expect(options.map(labelOf)).toEqual(['North office', 'South office']);
    expect(options[0]).toHaveTextContent('NORTH');
  });

  it('becomes a text field for a country with no list', async () => {
    const onValueChange = vi.fn();
    render(
      <Field>
        <FieldLabel>Region</FieldLabel>
        <RegionSelect country="FR" onValueChange={onValueChange} />
      </Field>,
    );
    const input = screen.getByRole('textbox', { name: 'Region' });
    expect(input).toHaveClass('mn-region-select');
    expect(input).toHaveAttribute('autocomplete', 'address-level1');
    const user = userEvent.setup();
    await user.type(input, 'Bretagne');
    expect(input).toHaveValue('Bretagne');
    expect(onValueChange).toHaveBeenLastCalledWith('Bretagne');
    await expectNoA11yViolations();
  });

  it('loads regions once per country, retries after an error and aborts on unmount', async () => {
    const signals: AbortSignal[] = [];
    let fail = true;
    const loadRegions = vi.fn((country: string, signal: AbortSignal) => {
      signals.push(signal);
      if (fail) return Promise.reject(new Error('offline'));
      return Promise.resolve([{ code: `${country}-75`, name: 'Paris' }]);
    });
    function Demo() {
      const [value, setValue] = useState<string | null>(null);
      return (
        <RegionSelect
          aria-label="Region"
          country="FR"
          loadRegions={loadRegions}
          value={value}
          onValueChange={setValue}
        />
      );
    }
    const { unmount } = render(<Demo />);
    const user = userEvent.setup();
    const input = screen.getByRole('combobox', { name: 'Region' });
    await user.click(input);
    await user.keyboard('{ArrowDown}');
    const retry = await screen.findByRole('button', { name: 'Try again' });
    fail = false;
    await user.click(retry);
    const option = await screen.findByRole('option', { name: 'Paris' });
    await user.click(option);
    await waitFor(() => expect(input).toHaveValue('Paris'));
    // The retried load is kept: the field is a plain list again, not still "loading".
    await waitFor(() =>
      expect(document.querySelector('[data-source]')).toHaveAttribute('data-source', 'list'),
    );
    expect(loadRegions.mock.calls.every(([c]) => c === 'FR')).toBe(true);
    const calls = loadRegions.mock.calls.length;
    await user.click(screen.getByRole('button', { name: 'Show options' }));
    expect(loadRegions).toHaveBeenCalledTimes(calls);
    unmount();
    expect(signals.every((s) => s.aborted)).toBe(true);
  });

  it('renders on the server', () => {
    const html = renderToString(
      <RegionSelect aria-label="State" country="US" defaultValue="US-NY" />,
    );
    expect(html).toContain('mn-region-select');
    expect(html).toContain('New York');
  });
});
