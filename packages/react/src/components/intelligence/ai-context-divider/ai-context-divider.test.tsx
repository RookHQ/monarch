import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiContextDivider } from './index.js';

const NOW = '2026-03-04T12:00:00Z';

describe('AiContextDivider', () => {
  it('labels a day as a separator with a time element', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiContextDivider
        ref={ref}
        data-testid="root"
        className="extra"
        kind="day"
        date="2026-03-04T08:00:00Z"
        now={NOW}
        timeZone="UTC"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-context-divider', 'extra');
    expect(root).toHaveAttribute('data-kind', 'day');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('separator', { name: 'Today' })).toBe(root);
    expect(root.querySelector('time')).toHaveAttribute('datetime', '2026-03-04T08:00:00.000Z');
    await expectNoA11yViolations();
  });

  it('uses Yesterday and full dates', () => {
    const { rerender } = render(
      <AiContextDivider kind="day" date="2026-03-03T08:00:00Z" now={NOW} timeZone="UTC" />,
    );
    expect(screen.getByRole('separator', { name: 'Yesterday' })).toBeInTheDocument();
    rerender(
      <AiContextDivider
        kind="day"
        date="2026-03-01T08:00:00Z"
        now={NOW}
        timeZone="UTC"
        locale="en-GB"
      />,
    );
    expect(screen.getByRole('separator')).toHaveTextContent('Sunday 1 March');
  });

  it('marks a context reset with a description', async () => {
    render(<AiContextDivider kind="context-reset" />);
    const sep = screen.getByRole('separator', {
      name: 'Context reset. The assistant does not see the messages above this line.',
    });
    expect(sep).toHaveAttribute('data-kind', 'context-reset');
    expect(sep).toHaveTextContent('Context reset');
    await expectNoA11yViolations();
  });

  it('is a plain separator for a summary with nothing to show', () => {
    render(<AiContextDivider kind="summarized" />);
    expect(
      screen.getByRole('separator', { name: 'Earlier messages were summarized' }),
    ).toBeInTheDocument();
  });

  it('shows the summary in a disclosure, marked as AI-generated, and an expand button', async () => {
    const onExpand = vi.fn();
    const user = userEvent.setup();
    const { container } = render(
      <AiContextDivider
        kind="summarized"
        summary="We agreed on three goals and a launch in week 9."
        onExpand={onExpand}
      />,
    );
    expect(
      screen.getByRole('group', { name: 'Earlier messages were summarized' }),
    ).toBeInTheDocument();
    const details = container.querySelector('details')!;
    expect(details.open).toBe(false);
    // jsdom toggles <details> on click only (browsers also use Enter/Space).
    await user.click(screen.getByText('Show summary'));
    expect(details.open).toBe(true);
    expect(details).toHaveTextContent(
      'AI-generated summary: We agreed on three goals and a launch in week 9.',
    );
    await user.click(screen.getByRole('button', { name: 'Show earlier messages' }));
    expect(onExpand).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it('is localised', () => {
    render(<AiContextDivider kind="context-reset" messages={{ contextReset: 'Neustart' }} />);
    expect(screen.getByRole('separator')).toHaveTextContent('Neustart');
  });

  it('renders on the server', () => {
    const html = renderToString(<AiContextDivider kind="context-reset" />);
    expect(html).toContain('mn-ai-context-divider');
    expect(html).toContain('Context reset');
  });
});
