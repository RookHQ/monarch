import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiChainOfSteps, type AiStep } from './index.js';

const running: AiStep[] = [
  { id: 'a', label: 'Read the attached file', state: 'done' },
  { id: 'b', label: 'Search earlier notes', state: 'active', detail: '3 matches so far' },
  { id: 'c', label: 'Draft a summary', state: 'pending' },
  { id: 'd', label: 'Check the figures', state: 'pending' },
];

const finished: AiStep[] = [
  { id: 'a', label: 'Read the attached file', state: 'done' },
  { id: 'b', label: 'Search earlier notes', state: 'done' },
  { id: 'c', label: 'Draft a summary', state: 'done' },
  { id: 'd', label: 'Check the figures', state: 'skipped' },
];

describe('AiChainOfSteps', () => {
  it('renders an ordered, labelled list with state text on every step', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<AiChainOfSteps ref={ref} steps={running} data-testid="root" className="extra" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-chain-of-steps', 'extra');
    expect(ref.current).toBe(root);
    const list = screen.getByRole('list', { name: 'Steps' });
    expect(list.tagName).toBe('OL');
    const items = within(list).getAllByRole('listitem');
    expect(items).toHaveLength(4);
    expect(items[0]).toHaveTextContent('Read the attached file, Done');
    expect(items[1]).toHaveTextContent('Search earlier notes, In progress');
    expect(items[1]).toHaveTextContent('3 matches so far');
    expect(items[1]).toHaveAttribute('aria-current', 'step');
    expect(items[2]).toHaveTextContent('Not started');
    expect(items[0]).not.toHaveAttribute('aria-current');
    await expectNoA11yViolations();
  });

  it('shows failed and skipped state as visible text, not colour alone', () => {
    const { container } = render(
      <AiChainOfSteps
        collapsible={false}
        steps={[
          { id: 'a', label: 'Fetch data', state: 'failed' },
          { id: 'b', label: 'Chart it', state: 'skipped' },
        ]}
      />,
    );
    const visible = container.querySelectorAll('.mn-ai-chain-of-steps__state');
    expect([...visible].map((n) => n.textContent)).toEqual([', Failed', ', Skipped']);
    expect(container.firstElementChild).toHaveAttribute('data-failed');
  });

  it('announces a step starting, then completion, but not the initial state', () => {
    const { rerender } = render(<AiChainOfSteps steps={running} />);
    const status = screen.getByRole('status');
    expect(status).toBeEmptyDOMElement();
    rerender(
      <AiChainOfSteps
        steps={running.map((s) =>
          s.id === 'b' ? { ...s, state: 'done' } : s.id === 'c' ? { ...s, state: 'active' } : s,
        )}
      />,
    );
    expect(status).toHaveTextContent('Step 3 of 4: Draft a summary');
    rerender(
      <AiChainOfSteps
        steps={running.map((s) =>
          s.id === 'd' ? { ...s, state: 'failed' } : { ...s, state: 'done' },
        )}
      />,
    );
    expect(status).toHaveTextContent('Step failed: Check the figures. Finished 4 steps, 1 failed');
  });

  it('folds under a summary once every step has finished, and opens on click', async () => {
    const user = userEvent.setup();
    const { container } = render(<AiChainOfSteps steps={finished} />);
    const details = container.querySelector('details') as HTMLDetailsElement;
    expect(details.open).toBe(false);
    const summary = container.querySelector('summary') as HTMLElement;
    expect(summary).toHaveTextContent('4 steps');
    expect(container.firstElementChild).toHaveAttribute('data-finished');
    await user.click(summary);
    expect(details.open).toBe(true);
    await expectNoA11yViolations();
  });

  it('can stay expanded, start open, and be translated', () => {
    const first = render(<AiChainOfSteps steps={finished} collapsible={false} />);
    expect(first.container.querySelector('details')).toBeNull();
    first.unmount();
    const { container } = render(
      <AiChainOfSteps
        steps={finished}
        defaultOpen
        messages={{ summary: (n) => `${n} étapes`, label: 'Étapes', states: { done: 'Terminé' } }}
      />,
    );
    expect((container.querySelector('details') as HTMLDetailsElement).open).toBe(true);
    expect(container.querySelector('summary')).toHaveTextContent('4 étapes');
    expect(screen.getByRole('list', { name: 'Étapes' })).toHaveTextContent('Terminé');
    // Other states keep their English defaults.
    expect(screen.getByRole('list', { name: 'Étapes' })).toHaveTextContent('Skipped');
  });

  it('omits the live region when announce is false', () => {
    render(<AiChainOfSteps steps={running} announce={false} />);
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiChainOfSteps steps={running} />);
    expect(html).toContain('mn-ai-chain-of-steps');
    expect(html).toContain('aria-current="step"');
  });
});
