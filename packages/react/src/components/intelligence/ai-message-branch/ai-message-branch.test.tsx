import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiMessageBranch } from './index.js';

function Controlled({ start = 0, count = 3 }: { start?: number; count?: number }) {
  const [index, setIndex] = useState(start);
  return <AiMessageBranch index={index} count={count} onChange={setIndex} />;
}

describe('AiMessageBranch', () => {
  it('renders a named group with labelled buttons and the position', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiMessageBranch
        ref={ref}
        data-testid="root"
        className="extra"
        index={1}
        count={3}
        onChange={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-message-branch', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Versions' })).toBe(root);
    expect(root).toHaveTextContent('2 / 3');
    expect(screen.getByRole('button', { name: 'Previous version' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Next version' })).toBeEnabled();
    await expectNoA11yViolations();
  });

  it('renders nothing for a single version unless showSingle', () => {
    const { container, rerender } = render(
      <AiMessageBranch index={0} count={1} onChange={() => {}} />,
    );
    expect(container).toBeEmptyDOMElement();
    rerender(<AiMessageBranch index={0} count={1} onChange={() => {}} showSingle />);
    expect(screen.getByRole('group')).toHaveTextContent('1 / 1');
    expect(screen.getByRole('button', { name: 'Previous version' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next version' })).toBeDisabled();
  });

  it('calls onChange with the 0-based index and disables at the ends', async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();
    const { rerender } = render(<AiMessageBranch index={0} count={2} onChange={onChange} />);
    expect(screen.getByRole('button', { name: 'Previous version' })).toBeDisabled();
    await user.click(screen.getByRole('button', { name: 'Next version' }));
    expect(onChange).toHaveBeenCalledWith(1);
    rerender(<AiMessageBranch index={1} count={2} onChange={onChange} />);
    expect(screen.getByRole('button', { name: 'Next version' })).toBeDisabled();
  });

  it('announces the new position politely and keeps focus when reaching an end', async () => {
    const user = userEvent.setup();
    render(<Controlled start={1} count={3} />);
    const next = screen.getByRole('button', { name: 'Next version' });
    next.focus();
    await user.keyboard('{Enter}');
    expect(screen.getByRole('status')).toHaveTextContent('Version 3 of 3');
    expect(next).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Previous version' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(screen.getByRole('status')).toHaveTextContent('Version 2 of 3');
    expect(screen.getByRole('group')).toHaveTextContent('2 / 3');
  });

  it('does not announce on first render', () => {
    render(<AiMessageBranch index={1} count={3} onChange={() => {}} />);
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
  });

  it('can be disabled and localised', () => {
    render(
      <AiMessageBranch
        index={1}
        count={3}
        onChange={() => {}}
        disabled
        messages={{
          label: 'Versionen',
          previous: 'Vorherige',
          next: 'Nächste',
          position: (i, c) => `${i} von ${c}`,
        }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Versionen' })).toHaveTextContent('2 von 3');
    expect(screen.getByRole('button', { name: 'Vorherige' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Nächste' })).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiMessageBranch index={0} count={2} onChange={() => {}} />);
    expect(html).toContain('mn-ai-message-branch');
    expect(html).toContain('1 / 2');
  });
});
