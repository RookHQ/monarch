import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiGeneratedBadge } from './index.js';

describe('AiGeneratedBadge', () => {
  it('renders the generated badge, forwards props and the ref', async () => {
    const ref = createRef<HTMLSpanElement>();
    render(<AiGeneratedBadge ref={ref} data-testid="root" className="extra" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-generated-badge', 'extra');
    expect(root).toHaveAttribute('data-kind', 'generated');
    expect(root).toHaveAttribute('data-size', 'md');
    expect(root).toHaveTextContent('AI generated');
    expect(root.querySelector('svg')).toHaveAttribute('aria-hidden');
    expect(ref.current).toBe(root);
    // Nothing to show on hover, so it is not a tab stop.
    expect(root).not.toHaveAttribute('tabindex');
    await expectNoA11yViolations();
  });

  it.each([
    ['generated', 'AI generated'],
    ['assisted', 'AI assisted'],
    ['edited', 'Edited after AI'],
  ] as const)('labels kind %s as "%s"', (kind, label) => {
    render(<AiGeneratedBadge kind={kind} data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveTextContent(label);
    expect(screen.getByTestId('root')).toHaveAttribute('data-kind', kind);
  });

  it('reads the model and time after the label and shows them in a tooltip on focus', async () => {
    const user = userEvent.setup();
    render(
      <AiGeneratedBadge
        data-testid="root"
        model="model-x"
        at="2026-03-04T10:00:00Z"
        messages={{ formatDate: () => '4 Mar 2026' }}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveTextContent('AI generated, By model-x, 4 Mar 2026');
    expect(root).toHaveAttribute('tabindex', '0');
    await user.tab();
    expect(root).toHaveFocus();
    expect(await screen.findByRole('tooltip')).toHaveTextContent('By model-x, 4 Mar 2026');
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('ignores an invalid time', () => {
    render(<AiGeneratedBadge data-testid="root" at="not a date" />);
    expect(screen.getByTestId('root')).toHaveTextContent(/^AI generated$/);
  });

  it('icon-only keeps the label for assistive technology and in the tooltip', async () => {
    const user = userEvent.setup();
    render(<AiGeneratedBadge data-testid="root" iconOnly kind="assisted" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('data-icon-only');
    expect(root.querySelector('.mn-visually-hidden')).toHaveTextContent('AI assisted');
    await user.tab();
    expect(await screen.findByRole('tooltip')).toHaveTextContent('AI assisted');
    await expectNoA11yViolations();
  });

  it('accepts a custom tooltip, label and messages', async () => {
    const user = userEvent.setup();
    render(
      <AiGeneratedBadge
        data-testid="root"
        kind="edited"
        tooltip="Drafted by the assistant, then reviewed"
        messages={{ labels: { edited: 'Revised' } }}
        size="sm"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveTextContent('Revised');
    expect(root).toHaveAttribute('data-size', 'sm');
    await user.tab();
    expect(await screen.findByRole('tooltip')).toHaveTextContent(
      'Drafted by the assistant, then reviewed',
    );
  });

  it('label wins over messages', () => {
    render(<AiGeneratedBadge data-testid="root" label="Suggested" />);
    expect(screen.getByTestId('root')).toHaveTextContent('Suggested');
  });

  it('renders on the server', () => {
    const html = renderToString(<AiGeneratedBadge model="model-x" />);
    expect(html).toContain('mn-ai-generated-badge');
    expect(html).toContain('AI generated');
  });
});
