import { describe, expect, it, vi } from 'vitest';
import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiSafetyNotice } from './index.js';

describe('AiSafetyNotice', () => {
  it('renders a blocked notice as a note named by its title, forwarding ref and props', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<AiSafetyNotice ref={ref} kind="blocked" data-testid="root" className="extra" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-safety-notice', 'extra');
    expect(root).toHaveAttribute('data-kind', 'blocked');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('note', { name: 'This request wasn’t answered' })).toBe(root);
    expect(root).toHaveTextContent(/rephrase it and try again/);
    expect(root.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('words modified and filtered notices differently', () => {
    const { rerender } = render(<AiSafetyNotice kind="modified" />);
    expect(screen.getByRole('note', { name: 'This response was adjusted' })).toBeInTheDocument();
    rerender(<AiSafetyNotice kind="filtered" />);
    expect(screen.getByRole('note', { name: 'Some content was hidden' })).toBeInTheDocument();
  });

  it('shows the reason, next steps and an Edit prompt action', async () => {
    const onEditPrompt = vi.fn();
    render(
      <AiSafetyNotice
        kind="blocked"
        reason="Personal data"
        nextSteps={<p>Remove names and try again.</p>}
        onEditPrompt={onEditPrompt}
        learnMoreHref="https://example.com/guidelines"
      />,
    );
    expect(screen.getByRole('note')).toHaveTextContent('Reason: Personal data');
    expect(screen.getByText('Remove names and try again.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Edit prompt' }));
    expect(onEditPrompt).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('link', { name: 'Content guidelines' })).toHaveAttribute(
      'href',
      'https://example.com/guidelines',
    );
    await expectNoA11yViolations();
  });

  it('drops unsafe guideline links', () => {
    render(<AiSafetyNotice kind="filtered" learnMoreHref="javascript:alert(1)" />);
    expect(screen.queryByRole('link')).toBeNull();
  });

  it('accepts a custom title, body and messages', () => {
    const { rerender } = render(
      <AiSafetyNotice kind="blocked" title="Not answered">
        Try a different question.
      </AiSafetyNotice>,
    );
    expect(screen.getByRole('note', { name: 'Not answered' })).toHaveTextContent(
      'Try a different question.',
    );
    rerender(
      <AiSafetyNotice
        kind="blocked"
        onEditPrompt={() => {}}
        messages={{ blockedTitle: 'Demande refusée', editPrompt: 'Modifier' }}
      />,
    );
    expect(screen.getByRole('note', { name: 'Demande refusée' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Modifier' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiSafetyNotice kind="modified" reason="Tone" />);
    expect(html).toContain('mn-ai-safety-notice');
    expect(html).toContain('data-kind="modified"');
  });
});
