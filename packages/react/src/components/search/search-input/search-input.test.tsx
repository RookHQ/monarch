import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldError, FieldLabel } from '../../forms/form-field/index.js';
import { SearchInput } from './index.js';

describe('SearchInput', () => {
  it('renders a labelled searchbox with its root class', async () => {
    render(<SearchInput aria-label="Search documents" className="extra" placeholder="Search" />);
    const box = screen.getByRole('searchbox', { name: 'Search documents' });
    expect(box).toHaveAttribute('type', 'search');
    expect(box.closest('.mn-search-input')).toHaveClass('mn-input', 'extra');
    await expectNoA11yViolations();
  });

  it('debounces onSearch and reports every keystroke through onValueChange', async () => {
    const onSearch = vi.fn();
    const onValueChange = vi.fn();
    render(
      <SearchInput
        aria-label="Search"
        debounce={300}
        onSearch={onSearch}
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    await user.type(screen.getByRole('searchbox'), 'inv');
    expect(onValueChange).toHaveBeenCalledTimes(3);
    expect(onValueChange).toHaveBeenLastCalledWith('inv');
    expect(onSearch).not.toHaveBeenCalled();
    await waitFor(() => expect(onSearch).toHaveBeenCalledTimes(1));
    expect(onSearch).toHaveBeenCalledWith('inv');
  });

  it('searches at once on Enter and cancels the pending debounce', async () => {
    const onSearch = vi.fn();
    render(<SearchInput aria-label="Search" debounce={500} onSearch={onSearch} />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('searchbox'), 'q3{Enter}');
    expect(onSearch).toHaveBeenCalledTimes(1);
    expect(onSearch).toHaveBeenCalledWith('q3');
    await new Promise((r) => setTimeout(r, 550));
    expect(onSearch).toHaveBeenCalledTimes(1);
  });

  it('calls onSearch on every change when there is no debounce', async () => {
    const onSearch = vi.fn();
    render(<SearchInput aria-label="Search" onSearch={onSearch} />);
    await userEvent.setup().type(screen.getByRole('searchbox'), 'ab');
    expect(onSearch.mock.calls).toEqual([['a'], ['ab']]);
  });

  it('clears with the clear button and returns focus to the input', async () => {
    const onSearch = vi.fn();
    const onClear = vi.fn();
    render(
      <SearchInput
        aria-label="Search"
        defaultValue="report"
        onSearch={onSearch}
        onClear={onClear}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Clear search' }));
    const box = screen.getByRole('searchbox');
    expect(box).toHaveValue('');
    expect(box).toHaveFocus();
    expect(onSearch).toHaveBeenCalledWith('');
    expect(onClear).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('button', { name: 'Clear search' })).not.toBeInTheDocument();
  });

  it('clears on Escape, and lets a second Escape through to the page', async () => {
    const outer = vi.fn();
    render(
      // eslint-disable-next-line jsx-a11y/no-static-element-interactions
      <div onKeyDown={(e) => e.key === 'Escape' && outer()}>
        <SearchInput aria-label="Search" defaultValue="abc" />
      </div>,
    );
    const user = userEvent.setup();
    const box = screen.getByRole('searchbox');
    await user.click(box);
    await user.keyboard('{Escape}');
    expect(box).toHaveValue('');
    expect(outer).not.toHaveBeenCalled();
    await user.keyboard('{Escape}');
    expect(outer).toHaveBeenCalledTimes(1);
  });

  it('works controlled', async () => {
    function Controlled() {
      const [q, setQ] = useState('start');
      return (
        <>
          <SearchInput aria-label="Search" value={q} onValueChange={setQ} />
          <output>{q}</output>
        </>
      );
    }
    render(<Controlled />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('searchbox'), 'ed');
    expect(screen.getByRole('status')).toHaveTextContent('started');
    await user.click(screen.getByRole('button', { name: 'Clear search' }));
    expect(screen.getByRole('status')).toHaveTextContent('');
  });

  it('shows a pending state with aria-busy', () => {
    const { container } = render(<SearchInput aria-label="Search" pending />);
    expect(screen.getByRole('searchbox')).toHaveAttribute('aria-busy', 'true');
    expect(container.querySelector('.mn-search-input')).toHaveAttribute('data-pending');
    expect(container.querySelector('.mn-spinner')).toBeInTheDocument();
  });

  it('focuses from a single-key shortcut, but not while typing elsewhere', async () => {
    render(
      <>
        <input aria-label="Other" />
        <SearchInput aria-label="Search" shortcut="/" shortcutKeys="/" />
      </>,
    );
    const box = screen.getByRole('searchbox');
    expect(box).toHaveAttribute('aria-keyshortcuts', '/');
    expect(document.querySelector('.mn-kbd')).toHaveTextContent('/');
    const user = userEvent.setup();
    await user.click(screen.getByRole('textbox', { name: 'Other' }));
    await user.keyboard('/');
    expect(box).not.toHaveFocus();
    expect(screen.getByRole('textbox', { name: 'Other' })).toHaveValue('/');
    (document.activeElement as HTMLElement).blur();
    await user.keyboard('/');
    expect(box).toHaveFocus();
    expect(box).toHaveValue('');
  });

  it('focuses from a modifier shortcut even inside another field', async () => {
    render(
      <>
        <input aria-label="Other" />
        <SearchInput aria-label="Search" shortcutKeys="Meta+K Control+K" />
      </>,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('textbox', { name: 'Other' }));
    await user.keyboard('{Control>}k{/Control}');
    expect(screen.getByRole('searchbox')).toHaveFocus();
  });

  it('reads Field context and hides the clear button when disabled', async () => {
    render(
      <Field invalid>
        <FieldLabel>Find a member</FieldLabel>
        <SearchInput defaultValue="ana" />
        <FieldError>Search is unavailable.</FieldError>
      </Field>,
    );
    const box = screen.getByRole('searchbox', { name: 'Find a member' });
    expect(box).toHaveAttribute('aria-invalid', 'true');
    expect(box).toHaveAccessibleDescription('Search is unavailable.');
    await expectNoA11yViolations();

    render(<SearchInput aria-label="Disabled search" defaultValue="x" disabled />);
    expect(screen.getAllByRole('button', { name: 'Clear search' })).toHaveLength(1);
  });

  it('renders on the server', () => {
    const html = renderToString(<SearchInput aria-label="Search" defaultValue="a" shortcut="/" />);
    expect(html).toContain('mn-search-input');
    expect(html).toContain('type="search"');
  });
});
