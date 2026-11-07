import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiTypingDots } from './index.js';

describe('AiTypingDots', () => {
  it('renders three decorative dots, forwards props and the ref', async () => {
    const ref = createRef<HTMLSpanElement>();
    render(<AiTypingDots ref={ref} data-testid="root" className="extra" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-typing-dots', 'extra');
    expect(root).toHaveAttribute('data-size', 'md');
    expect(ref.current).toBe(root);
    const dots = root.querySelectorAll('.mn-ai-typing-dots__dot');
    expect(dots).toHaveLength(3);
    dots.forEach((d) => expect(d).toHaveAttribute('aria-hidden', 'true'));
    await expectNoA11yViolations();
  });

  it('announces the default label through a polite status', () => {
    render(<AiTypingDots />);
    expect(screen.getByRole('status')).toHaveTextContent('Assistant is typing');
  });

  it('accepts a custom label and the sm size', () => {
    render(<AiTypingDots label="Écrit…" size="sm" data-testid="root" />);
    expect(screen.getByRole('status')).toHaveTextContent('Écrit…');
    expect(screen.getByTestId('root')).toHaveAttribute('data-size', 'sm');
  });

  it('keeps the label as plain hidden text when announce is false', async () => {
    render(<AiTypingDots announce={false} data-testid="root" />);
    expect(screen.queryByRole('status')).toBeNull();
    expect(screen.getByTestId('root')).toHaveTextContent('Assistant is typing');
    await expectNoA11yViolations();
  });

  it('renders on the server without the announcement text', () => {
    const html = renderToString(<AiTypingDots />);
    expect(html).toContain('mn-ai-typing-dots');
    expect(html).toContain('role="status"');
    expect(html).not.toContain('Assistant is typing');
  });
});
