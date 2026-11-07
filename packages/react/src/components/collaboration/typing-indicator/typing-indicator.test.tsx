import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { TypingIndicator, formatTyping } from './index.js';

const ana = { id: 'u1', name: 'Ana' };
const ben = { id: 'u2', name: 'Ben' };
const chloe = { id: 'u3', name: 'Chloé' };

describe('TypingIndicator', () => {
  it('words one, two and several people', () => {
    expect(formatTyping([])).toBe('');
    expect(formatTyping([ana])).toBe('Ana is typing…');
    expect(formatTyping([ana, ben])).toBe('Ana and Ben are typing…');
    expect(formatTyping([ana, ben, chloe])).toBe('Several people are typing…');
  });

  it('is a polite status region that announces the sentence', async () => {
    render(<TypingIndicator people={[ana]} />);
    const status = screen.getByRole('status');
    expect(status).toHaveClass('mn-typing-indicator');
    expect(status).toHaveAttribute('aria-live', 'polite');
    expect(status).toHaveAttribute('data-active');
    expect(status).toHaveTextContent('Ana is typing…');
    expect(status.querySelector('.mn-typing-indicator__dots')).toHaveAttribute('aria-hidden');
    await expectNoA11yViolations();
  });

  it('keeps the live region mounted while nobody types, then fills it', () => {
    const { rerender } = render(<TypingIndicator people={[]} />);
    const status = screen.getByRole('status');
    expect(status).toBeEmptyDOMElement();
    expect(status).not.toHaveAttribute('data-active');
    expect(status).toHaveAttribute('data-reserve-space');
    rerender(<TypingIndicator people={[ana, ben]} />);
    expect(screen.getByRole('status')).toBe(status);
    expect(status).toHaveTextContent('Ana and Ben are typing…');
  });

  it('can drop the live region and the reserved space', () => {
    render(
      <TypingIndicator data-testid="t" people={[ana]} announce={false} reserveSpace={false} />,
    );
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(screen.getByTestId('t')).not.toHaveAttribute('data-reserve-space');
  });

  it('takes translated text', () => {
    render(
      <TypingIndicator
        people={[ana, ben, chloe]}
        messages={{ several: (n) => `${n} personnes écrivent…` }}
      />,
    );
    expect(screen.getByRole('status')).toHaveTextContent('3 personnes écrivent…');
  });

  it('renders on the server', () => {
    const html = renderToString(<TypingIndicator people={[ana]} />);
    expect(html).toContain('mn-typing-indicator');
    expect(html).toContain('Ana is typing…');
  });
});
