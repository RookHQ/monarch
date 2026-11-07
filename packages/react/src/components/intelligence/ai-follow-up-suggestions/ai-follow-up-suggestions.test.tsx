import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiFollowUpSuggestions } from './index.js';

const SUGGESTIONS = ['Make it shorter', 'Add a timeline', 'Who owns each goal?'];

describe('AiFollowUpSuggestions', () => {
  it('renders a named toolbar with one tab stop', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiFollowUpSuggestions
        ref={ref}
        data-testid="root"
        className="extra"
        suggestions={SUGGESTIONS}
        onSelect={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-follow-up-suggestions', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('toolbar', { name: 'Suggested follow-ups' })).toBe(root);
    const chips = screen.getAllByRole('button');
    expect(chips.map((c) => c.getAttribute('tabindex'))).toEqual(['0', '-1', '-1']);
    await expectNoA11yViolations();
  });

  it('moves focus with arrows, Home and End, wrapping around', async () => {
    const user = userEvent.setup();
    render(<AiFollowUpSuggestions suggestions={SUGGESTIONS} onSelect={() => {}} />);
    const [a, b, c] = screen.getAllByRole('button');
    await user.tab();
    expect(a).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(b).toHaveFocus();
    expect(b).toHaveAttribute('tabindex', '0');
    expect(a).toHaveAttribute('tabindex', '-1');
    await user.keyboard('{ArrowDown}');
    expect(c).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(a).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(c).toHaveFocus();
    await user.keyboard('{Home}');
    expect(a).toHaveFocus();
    await user.keyboard('{End}');
    expect(c).toHaveFocus();
    await user.tab({ shift: true });
    await user.tab();
    expect(c).toHaveFocus();
  });

  it('calls onSelect with Enter, Space and click', async () => {
    const onSelect = vi.fn();
    const user = userEvent.setup();
    render(<AiFollowUpSuggestions suggestions={SUGGESTIONS} onSelect={onSelect} />);
    await user.tab();
    await user.keyboard('{Enter}');
    expect(onSelect).toHaveBeenLastCalledWith('Make it shorter');
    await user.keyboard('{ArrowRight} ');
    expect(onSelect).toHaveBeenLastCalledWith('Add a timeline');
    await user.click(screen.getByRole('button', { name: 'Who owns each goal?' }));
    expect(onSelect).toHaveBeenLastCalledWith('Who owns each goal?');
  });

  it('keeps a valid tab stop when the suggestions shrink', async () => {
    const user = userEvent.setup();
    const { rerender } = render(
      <AiFollowUpSuggestions suggestions={SUGGESTIONS} onSelect={() => {}} />,
    );
    await user.tab();
    await user.keyboard('{End}');
    rerender(<AiFollowUpSuggestions suggestions={['Only one']} onSelect={() => {}} />);
    expect(screen.getByRole('button', { name: 'Only one' })).toHaveAttribute('tabindex', '0');
  });

  it('can be disabled, localised, and renders nothing when empty', () => {
    const { container, rerender } = render(
      <AiFollowUpSuggestions
        suggestions={SUGGESTIONS}
        onSelect={() => {}}
        disabled
        messages={{ label: 'Rückfragen' }}
      />,
    );
    expect(screen.getByRole('toolbar', { name: 'Rückfragen' })).toBeInTheDocument();
    for (const chip of screen.getAllByRole('button')) expect(chip).toBeDisabled();
    rerender(<AiFollowUpSuggestions suggestions={[]} onSelect={() => {}} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiFollowUpSuggestions suggestions={SUGGESTIONS} onSelect={() => {}} />,
    );
    expect(html).toContain('mn-ai-follow-up-suggestions');
    expect(html).toContain('Add a timeline');
  });
});
