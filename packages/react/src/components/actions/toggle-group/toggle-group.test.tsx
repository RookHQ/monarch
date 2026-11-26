import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DirectionProvider } from '../../../internal/direction.js';
import { ToggleGroup, ToggleGroupItem } from './index.js';

const press = (name: string) => screen.getByRole('button', { name });

describe('ToggleGroup', () => {
  it('is a labelled group of pressed buttons with multiple selection by default', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <ToggleGroup aria-label="Text style" defaultValue={['bold']} onValueChange={onValueChange}>
        <ToggleGroupItem value="bold">Bold</ToggleGroupItem>
        <ToggleGroupItem value="italic">Italic</ToggleGroupItem>
        <ToggleGroupItem value="underline">Underline</ToggleGroupItem>
      </ToggleGroup>,
    );
    expect(screen.getByRole('group', { name: 'Text style' })).toHaveAttribute(
      'data-type',
      'multiple',
    );
    expect(press('Bold')).toHaveAttribute('aria-pressed', 'true');
    await user.click(press('Italic'));
    expect(onValueChange).toHaveBeenLastCalledWith(['bold', 'italic']);
    expect(press('Italic')).toHaveAttribute('aria-pressed', 'true');
    await user.click(press('Bold'));
    expect(onValueChange).toHaveBeenLastCalledWith(['italic']);
    await expectNoA11yViolations();
  });

  it('has one Tab stop and moves with arrow keys, Home and End, skipping disabled items', async () => {
    const user = userEvent.setup();
    render(
      <>
        <button type="button">Before</button>
        <ToggleGroup aria-label="Filters">
          <ToggleGroupItem value="a">Open</ToggleGroupItem>
          <ToggleGroupItem value="b" disabled>
            Closed
          </ToggleGroupItem>
          <ToggleGroupItem value="c">Mine</ToggleGroupItem>
          <ToggleGroupItem value="d">Starred</ToggleGroupItem>
        </ToggleGroup>
        <button type="button">After</button>
      </>,
    );
    press('Before').focus();
    await user.tab();
    expect(press('Open')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(press('Mine')).toHaveFocus();
    await user.keyboard('{End}');
    expect(press('Starred')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(press('Open')).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(press('Starred')).toHaveFocus();
    await user.keyboard('{Home}');
    expect(press('Open')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    await user.keyboard(' ');
    expect(press('Mine')).toHaveAttribute('aria-pressed', 'true');
    await user.tab();
    expect(press('After')).toHaveFocus();
    // Shift+Tab returns to the last focused item, not the first.
    await user.tab({ shift: true });
    expect(press('Mine')).toHaveFocus();
    expect(press('Open')).toHaveAttribute('tabindex', '-1');
  });

  it('single type keeps at most one pressed and releases unless required', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    const { rerender } = render(
      <ToggleGroup type="single" aria-label="Align" onValueChange={onValueChange}>
        <ToggleGroupItem value="left">Left</ToggleGroupItem>
        <ToggleGroupItem value="center">Center</ToggleGroupItem>
      </ToggleGroup>,
    );
    await user.click(press('Left'));
    expect(onValueChange).toHaveBeenLastCalledWith('left');
    await user.click(press('Center'));
    expect(onValueChange).toHaveBeenLastCalledWith('center');
    expect(press('Left')).toHaveAttribute('aria-pressed', 'false');
    await user.click(press('Center'));
    expect(onValueChange).toHaveBeenLastCalledWith('');
    rerender(
      <ToggleGroup
        type="single"
        required
        value="left"
        aria-label="Align"
        onValueChange={onValueChange}
      >
        <ToggleGroupItem value="left">Left</ToggleGroupItem>
        <ToggleGroupItem value="center">Center</ToggleGroupItem>
      </ToggleGroup>,
    );
    onValueChange.mockClear();
    await user.click(press('Left'));
    expect(onValueChange).not.toHaveBeenCalled();
    expect(press('Left')).toHaveAttribute('aria-pressed', 'true');
  });

  it('follows the vertical orientation and right-to-left direction', async () => {
    const user = userEvent.setup();
    const { unmount } = render(
      <ToggleGroup aria-label="Layers" orientation="vertical">
        <ToggleGroupItem value="a">Roads</ToggleGroupItem>
        <ToggleGroupItem value="b">Rivers</ToggleGroupItem>
      </ToggleGroup>,
    );
    press('Roads').focus();
    await user.keyboard('{ArrowRight}');
    expect(press('Roads')).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(press('Rivers')).toHaveFocus();
    unmount();
    render(
      <ToggleGroup aria-label="Tools" dir="rtl" loop={false}>
        <ToggleGroupItem value="a">First</ToggleGroupItem>
        <ToggleGroupItem value="b">Second</ToggleGroupItem>
      </ToggleGroup>,
    );
    press('First').focus();
    await user.keyboard('{ArrowLeft}');
    expect(press('Second')).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(press('Second')).toHaveFocus();
  });

  it('honours a DirectionProvider with no dir attribute in the DOM', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <DirectionProvider dir="rtl">
        <ToggleGroup aria-label="Tools" loop={false}>
          <ToggleGroupItem value="a">First</ToggleGroupItem>
          <ToggleGroupItem value="b">Second</ToggleGroupItem>
        </ToggleGroup>
      </DirectionProvider>,
    );
    expect(container.querySelector('[dir]')).toBeNull();
    press('First').focus();
    await user.keyboard('{ArrowLeft}');
    expect(press('Second')).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(press('First')).toHaveFocus();
  });

  it('renders the options shorthand with icon-only labels and option order', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(
      <ToggleGroup
        aria-label="Columns"
        defaultValue={['c']}
        onValueChange={onValueChange}
        options={[
          { value: 'a', label: 'Name' },
          { value: 'b', label: 'Owner', count: 3 },
          { value: 'c', label: 'Grid', icon: <svg />, hideLabel: true },
        ]}
      />,
    );
    expect(press('Grid')).toHaveAttribute('data-icon-only');
    expect(press('Owner 3')).toBeInTheDocument();
    await user.click(press('Name'));
    expect(onValueChange).toHaveBeenLastCalledWith(['a', 'c']);
  });

  it('disables every item with disabled', () => {
    render(
      <ToggleGroup aria-label="Filters" disabled>
        <ToggleGroupItem value="a">Open</ToggleGroupItem>
      </ToggleGroup>,
    );
    expect(press('Open')).toBeDisabled();
    expect(screen.getByRole('group')).toHaveAttribute('aria-disabled', 'true');
  });

  it('renders on the server with every item reachable', () => {
    const html = renderToString(
      <ToggleGroup type="single" aria-label="Align" defaultValue="left">
        <ToggleGroupItem value="left">Left</ToggleGroupItem>
        <ToggleGroupItem value="right">Right</ToggleGroupItem>
      </ToggleGroup>,
    );
    expect(html).toContain('mn-toggle-group');
    expect(html).toContain('aria-pressed="true"');
    expect(html).not.toContain('tabindex="-1"');
  });
});
