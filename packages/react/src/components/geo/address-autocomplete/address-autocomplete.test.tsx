import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AddressAutocomplete,
  toAddressValue,
  type AddressSuggestion,
  type AddressValue,
} from './index.js';

const suggestions: AddressSuggestion[] = [
  {
    id: 'a1',
    label: '10 Market Street',
    description: 'Springfield, United States',
    address: {
      line1: '10 Market Street',
      city: 'Springfield',
      regionName: 'Oregon',
      postalCode: '97477',
      country: 'US',
    },
  },
  { id: 'a2', label: '10 Market Square', description: 'Springfield, United States' },
];

function searchOk(query: string) {
  return Promise.resolve(
    suggestions.filter((s) => s.label.toLowerCase().includes(query.toLowerCase())),
  );
}

describe('AddressAutocomplete', () => {
  it('looks up defaultQuery at once and shows its suggestions', async () => {
    const search = vi.fn(searchOk);
    render(<AddressAutocomplete search={search} defaultQuery="Market" debounce={10_000} />);
    expect(screen.getByRole('combobox')).toHaveValue('Market');
    expect(await screen.findByRole('option', { name: /10 Market Street/ })).toBeTruthy();
    expect(search).toHaveBeenCalledWith('Market', expect.anything());
  });

  it('searches after the minimum length, picks with the keyboard and fills the fields', async () => {
    const search = vi.fn(searchOk);
    const onValueChange = vi.fn();
    render(
      <AddressAutocomplete
        search={search}
        debounce={0}
        locale="en"
        onValueChange={onValueChange}
      />,
    );
    const input = screen.getByRole('combobox', { name: 'Address' });
    const user = userEvent.setup();
    await user.type(input, '10');
    expect(screen.getByText('Type at least 3 characters to search.')).toBeInTheDocument();
    expect(search).not.toHaveBeenCalled();
    await user.type(input, ' Mar');
    const list = await screen.findByRole('listbox', { name: 'Address suggestions' });
    expect(input).toHaveAttribute('aria-expanded', 'true');
    expect(within(list).getAllByRole('option')).toHaveLength(3); // two suggestions + manual entry
    await user.keyboard('{ArrowDown}');
    const active = input.getAttribute('aria-activedescendant');
    expect(document.getElementById(active!)).toHaveTextContent('10 Market Street');
    await user.keyboard('{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith(
      expect.objectContaining({ line1: '10 Market Street', city: 'Springfield', country: 'US' }),
    );
    const line1 = await screen.findByRole('textbox', { name: 'Street address' });
    await waitFor(() => expect(line1).toHaveFocus());
    expect(line1).toHaveValue('10 Market Street');
    expect(screen.getByRole('status', { name: '' })).toHaveTextContent(
      'Address filled in. Check the details.',
    );
    await expectNoA11yViolations();
  });

  it('resolves a suggestion without an address through resolve()', async () => {
    const resolve = vi.fn(() =>
      Promise.resolve<Partial<AddressValue>>({ city: 'Springfield', postalCode: '97478' }),
    );
    const onSuggestionSelect = vi.fn();
    render(
      <AddressAutocomplete
        search={searchOk}
        resolve={resolve}
        debounce={0}
        onSuggestionSelect={onSuggestionSelect}
      />,
    );
    const user = userEvent.setup();
    await user.type(screen.getByRole('combobox'), 'square');
    await user.click(
      await screen.findByRole('option', { name: '10 Market Square Springfield, United States' }),
    );
    expect(resolve).toHaveBeenCalledWith(suggestions[1], { signal: expect.any(AbortSignal) });
    await waitFor(() => expect(onSuggestionSelect).toHaveBeenCalled());
    expect(onSuggestionSelect.mock.calls[0]![1]).toMatchObject({
      line1: '10 Market Square',
      city: 'Springfield',
      postalCode: '97478',
    });
  });

  it('leaves Enter to the input method while it is composing', async () => {
    const onSuggestionSelect = vi.fn();
    render(
      <AddressAutocomplete
        search={searchOk}
        debounce={0}
        onSuggestionSelect={onSuggestionSelect}
      />,
    );
    const user = userEvent.setup();
    const input = screen.getByRole('combobox');
    await user.type(input, 'market');
    await screen.findAllByRole('option');
    await user.keyboard('{ArrowDown}');
    fireEvent.keyDown(input, { key: 'Enter', isComposing: true });
    expect(onSuggestionSelect).not.toHaveBeenCalled();
    expect(screen.getByRole('combobox')).toHaveAttribute('aria-expanded', 'true');
    fireEvent.keyDown(input, { key: 'Enter' });
    expect(onSuggestionSelect).toHaveBeenCalledTimes(1);
  });

  it('withdraws a pick whose details are still loading when the person types again', async () => {
    let finish: (address: Partial<AddressValue>) => void = () => undefined;
    const signals: AbortSignal[] = [];
    const resolve = vi.fn((_s: AddressSuggestion, { signal }: { signal: AbortSignal }) => {
      signals.push(signal);
      return new Promise<Partial<AddressValue>>((done) => (finish = done));
    });
    const onSuggestionSelect = vi.fn();
    render(
      <AddressAutocomplete
        search={searchOk}
        resolve={resolve}
        debounce={0}
        onSuggestionSelect={onSuggestionSelect}
      />,
    );
    const user = userEvent.setup();
    await user.type(screen.getByRole('combobox'), 'square');
    await user.click(await screen.findByRole('option', { name: /10 Market Square/ }));
    expect(screen.getByText('Getting the address details…')).toBeInTheDocument();
    await user.type(screen.getByRole('combobox'), '{Backspace}');
    expect(signals[0]?.aborted).toBe(true);
    expect(screen.queryByText('Getting the address details…')).not.toBeInTheDocument();
    finish({ city: 'Springfield' });
    await Promise.resolve();
    expect(onSuggestionSelect).not.toHaveBeenCalled();
    expect(screen.getByRole('combobox')).toBeInTheDocument();
  });

  it('shows the empty state with a manual entry option', async () => {
    render(<AddressAutocomplete search={() => Promise.resolve([])} debounce={0} />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('combobox'), 'Nowhere 1');
    await waitFor(() =>
      expect(document.querySelector('.mn-address-autocomplete__status')).toHaveTextContent(
        'No addresses found',
      ),
    );
    await user.keyboard('{ArrowDown}{Enter}');
    expect(await screen.findByRole('textbox', { name: 'Street address' })).toHaveValue('Nowhere 1');
    await user.click(screen.getByRole('button', { name: 'Search for an address' }));
    await waitFor(() => expect(screen.getByRole('combobox')).toHaveFocus());
  });

  it('shows an error with a retry that searches again', async () => {
    let offline = true;
    const search = vi.fn((q: string) =>
      offline ? Promise.reject(new Error('offline')) : searchOk(q),
    );
    render(<AddressAutocomplete search={search} debounce={0} />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('combobox'), 'Market');
    expect(await screen.findByRole('alert')).toHaveTextContent('Couldn’t search addresses.');
    offline = false;
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByRole('option', { name: /10 Market Street/ })).toBeInTheDocument();
  });

  it('aborts the previous search when the query changes', async () => {
    const signals: AbortSignal[] = [];
    const search = vi.fn((_q: string, { signal }: { signal: AbortSignal }) => {
      signals.push(signal);
      return new Promise<AddressSuggestion[]>(() => {});
    });
    render(<AddressAutocomplete search={search} debounce={0} />);
    const user = userEvent.setup();
    const input = screen.getByRole('combobox');
    await user.type(input, 'abc');
    await waitFor(() => expect(search).toHaveBeenCalledTimes(1));
    await user.type(input, 'd');
    await waitFor(() => expect(search).toHaveBeenCalledTimes(2));
    expect(signals[0]!.aborted).toBe(true);
    expect(input).toHaveAttribute('aria-busy', 'true');
  });

  it('closes on Escape, then clears on a second Escape', async () => {
    render(<AddressAutocomplete search={searchOk} debounce={0} />);
    const user = userEvent.setup();
    const input = screen.getByRole('combobox');
    await user.type(input, 'Market');
    await screen.findByRole('listbox');
    await user.keyboard('{Escape}');
    expect(input).toHaveAttribute('aria-expanded', 'false');
    expect(input).toHaveValue('Market');
    await user.keyboard('{Escape}');
    expect(input).toHaveValue('');
  });

  it('starts in manual mode with a controlled value and edits the street line', async () => {
    function Controlled() {
      const [value, setValue] = useState(toAddressValue({ line1: '5 Hill Road', country: 'GB' }));
      return (
        <>
          <AddressAutocomplete
            search={searchOk}
            value={value}
            onValueChange={setValue}
            defaultMode="manual"
            locale="en"
            required
          />
          <output>{value.line1}</output>
        </>
      );
    }
    render(<Controlled />);
    const line1 = screen.getByRole('textbox', { name: /Street address/ });
    const user = userEvent.setup();
    await user.type(line1, ', Unit 2');
    expect(document.querySelector('output')).toHaveTextContent('5 Hill Road, Unit 2');
    await expectNoA11yViolations();
  });

  it('reports a failed resolve and stays in search mode', async () => {
    render(
      <AddressAutocomplete
        search={searchOk}
        resolve={() => Promise.reject(new Error('gone'))}
        debounce={0}
      />,
    );
    const user = userEvent.setup();
    await user.type(screen.getByRole('combobox'), 'square');
    await screen.findByRole('listbox');
    await user.keyboard('{ArrowDown}{Enter}');
    expect(await screen.findByRole('alert')).toHaveTextContent('Couldn’t get that address');
    expect(screen.getByRole('combobox')).toHaveValue('10 Market Square');
    expect(screen.getByRole('button', { name: 'Enter the address manually' })).toBeEnabled();
  });

  it('renders on the server', () => {
    const html = renderToString(<AddressAutocomplete search={searchOk} />);
    expect(html).toContain('mn-address-autocomplete');
    expect(html).toContain('role="combobox"');
  });
});
