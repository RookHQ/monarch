import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SegmentedControl, ToggleGroup, type SegmentedOption } from './index.js';

const views: SegmentedOption<'list' | 'board' | 'calendar'>[] = [
  { value: 'list', label: 'List' },
  { value: 'board', label: 'Board' },
  { value: 'calendar', label: 'Calendar' },
];

describe('SegmentedControl', () => {
  it('is a labelled radio group with one checked radio', async () => {
    render(<SegmentedControl aria-label="View" options={views} defaultValue="board" />);
    expect(screen.getByRole('radiogroup', { name: 'View' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Board' })).toHaveAttribute('aria-checked', 'true');
    expect(screen.getByRole('radio', { name: 'List' })).toHaveAttribute('aria-checked', 'false');
    await expectNoA11yViolations();
  });

  it('has one Tab stop: the selected option', async () => {
    const user = userEvent.setup();
    render(
      <>
        <SegmentedControl aria-label="View" options={views} defaultValue="board" />
        <button type="button">After</button>
      </>,
    );
    await user.tab();
    expect(screen.getByRole('radio', { name: 'Board' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'After' })).toHaveFocus();
  });

  it('with nothing selected, the first enabled option is the Tab stop', async () => {
    const user = userEvent.setup();
    render(
      <SegmentedControl
        aria-label="View"
        options={[{ ...views[0]!, disabled: true }, views[1]!, views[2]!]}
      />,
    );
    await user.tab();
    expect(screen.getByRole('radio', { name: 'Board' })).toHaveFocus();
  });

  it('arrow keys move focus and selection, wrap, and skip disabled options', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <SegmentedControl
        aria-label="View"
        options={[views[0]!, { ...views[1]!, disabled: true }, views[2]!]}
        defaultValue="list"
        onValueChange={onValueChange}
      />,
    );
    await user.tab();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('radio', { name: 'Calendar' })).toHaveFocus();
    expect(screen.getByRole('radio', { name: 'Calendar' })).toHaveAttribute('aria-checked', 'true');
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('radio', { name: 'List' })).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(screen.getByRole('radio', { name: 'Calendar' })).toHaveFocus();
    await user.keyboard('{Home}');
    expect(screen.getByRole('radio', { name: 'List' })).toHaveFocus();
    await user.keyboard('{End}');
    expect(screen.getByRole('radio', { name: 'Calendar' })).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(screen.getByRole('radio', { name: 'List' })).toHaveFocus();
    expect(onValueChange.mock.calls.map((c) => c[0])).toEqual([
      'calendar',
      'list',
      'calendar',
      'list',
      'calendar',
      'list',
    ]);
  });

  it('reverses Left and Right in RTL', async () => {
    const user = userEvent.setup();
    render(<SegmentedControl aria-label="View" options={views} defaultValue="list" dir="rtl" />);
    await user.tab();
    await user.keyboard('{ArrowLeft}');
    expect(screen.getByRole('radio', { name: 'Board' })).toHaveFocus();
  });

  it('selects on click and on Space', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<SegmentedControl aria-label="View" options={views} onValueChange={onValueChange} />);
    await user.click(screen.getByRole('radio', { name: 'Calendar' }));
    expect(onValueChange).toHaveBeenLastCalledWith('calendar');
    screen.getByRole('radio', { name: 'List' }).focus();
    await user.keyboard(' ');
    expect(onValueChange).toHaveBeenLastCalledWith('list');
  });

  it('controlled: follows the value prop only', async () => {
    const user = userEvent.setup();
    function Demo() {
      const [v, setV] = useState<'list' | 'board' | 'calendar'>('list');
      return (
        <>
          <SegmentedControl aria-label="View" options={views} value={v} onValueChange={setV} />
          <output>{v}</output>
        </>
      );
    }
    render(<Demo />);
    await user.click(screen.getByRole('radio', { name: 'Board' }));
    expect(screen.getByText('board', { selector: 'output' })).toBeInTheDocument();
    const fixed = vi.fn();
    render(
      <SegmentedControl aria-label="Fixed" options={views} value="list" onValueChange={fixed} />,
    );
    const group = screen.getByRole('radiogroup', { name: 'Fixed' });
    await user.click(group.querySelector('[data-index="2"]')!);
    expect(fixed).toHaveBeenCalledWith('calendar');
    expect(group.querySelector('[data-index="0"]')).toHaveAttribute('aria-checked', 'true');
  });

  it('icon-only options keep the label as the name; counts join the name', () => {
    render(
      <SegmentedControl
        aria-label="Layout"
        options={[
          { value: 'grid', label: 'Grid', icon: <svg />, hideLabel: true },
          { value: 'open', label: 'Open', count: 12 },
        ]}
      />,
    );
    expect(screen.getByRole('radio', { name: 'Grid' })).toHaveAttribute('data-icon-only');
    expect(screen.getByRole('radio', { name: 'Open 12' })).toBeInTheDocument();
  });

  it('submits with a form through name', () => {
    render(
      <form data-testid="f">
        <SegmentedControl aria-label="Billing" name="period" options={views} defaultValue="board" />
      </form>,
    );
    const data = new FormData(screen.getByTestId('f') as HTMLFormElement);
    expect(data.get('period')).toBe('board');
  });

  it('disabled group: nothing is focusable or selectable', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <SegmentedControl aria-label="View" options={views} disabled onValueChange={onValueChange} />,
    );
    await user.tab();
    expect(document.body).toHaveFocus();
    for (const radio of screen.getAllByRole('radio')) expect(radio).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <SegmentedControl aria-label="View" options={views} defaultValue="list" size="sm" />,
    );
    expect(html).toContain('role="radiogroup"');
    expect(html).toContain('aria-checked="true"');
  });
});

const filters: SegmentedOption<'bold' | 'italic' | 'underline'>[] = [
  { value: 'bold', label: 'Bold' },
  { value: 'italic', label: 'Italic' },
  { value: 'underline', label: 'Underline' },
];

describe('ToggleGroup', () => {
  it('is a labelled group of toggle buttons', async () => {
    render(<ToggleGroup aria-label="Formatting" options={filters} defaultValue={['italic']} />);
    expect(screen.getByRole('group', { name: 'Formatting' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Italic' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Bold' })).toHaveAttribute('aria-pressed', 'false');
    await expectNoA11yViolations();
  });

  it('toggles several options; value keeps option order', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<ToggleGroup aria-label="Formatting" options={filters} onValueChange={onValueChange} />);
    await user.click(screen.getByRole('button', { name: 'Underline' }));
    await user.click(screen.getByRole('button', { name: 'Bold' }));
    expect(onValueChange).toHaveBeenLastCalledWith(['bold', 'underline']);
    await user.click(screen.getByRole('button', { name: 'Underline' }));
    expect(onValueChange).toHaveBeenLastCalledWith(['bold']);
  });

  it('arrow keys move focus without toggling; Space and Enter toggle', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<ToggleGroup aria-label="Formatting" options={filters} onValueChange={onValueChange} />);
    await user.tab();
    expect(screen.getByRole('button', { name: 'Bold' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: 'Italic' })).toHaveFocus();
    expect(onValueChange).not.toHaveBeenCalled();
    await user.keyboard(' ');
    expect(onValueChange).toHaveBeenLastCalledWith(['italic']);
    await user.keyboard('{End}{Enter}');
    expect(onValueChange).toHaveBeenLastCalledWith(['italic', 'underline']);
  });

  it('remembers the last focused option as the Tab stop', async () => {
    const user = userEvent.setup();
    render(
      <>
        <ToggleGroup aria-label="Formatting" options={filters} />
        <button type="button">After</button>
      </>,
    );
    await user.tab();
    await user.keyboard('{ArrowRight}{ArrowRight}');
    await user.tab();
    await user.tab({ shift: true });
    expect(screen.getByRole('button', { name: 'Underline' })).toHaveFocus();
  });

  it('controlled value', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <ToggleGroup
        aria-label="Formatting"
        options={filters}
        value={['bold']}
        onValueChange={onValueChange}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Italic' }));
    expect(onValueChange).toHaveBeenCalledWith(['bold', 'italic']);
    expect(screen.getByRole('button', { name: 'Italic' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <ToggleGroup aria-label="Formatting" options={filters} size="lg" />,
    );
    expect(html).toContain('aria-pressed="false"');
  });
});
