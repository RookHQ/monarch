import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiDisclaimer } from './index.js';

describe('AiDisclaimer', () => {
  it('renders the low-risk default as a note, forwarding props and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<AiDisclaimer ref={ref} data-testid="root" className="extra" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-disclaimer', 'extra');
    expect(ref.current).toBe(root);
    expect(root).toHaveAttribute('data-risk', 'low');
    expect(screen.getByRole('note')).toHaveTextContent(
      'AI can make mistakes. Check important info.',
    );
    expect(root.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('words each risk level differently', async () => {
    const { rerender } = render(<AiDisclaimer risk="medium" />);
    expect(screen.getByRole('note')).toHaveTextContent(/before you rely on them/);
    expect(screen.getByRole('note')).toHaveAttribute('data-risk', 'medium');
    rerender(<AiDisclaimer risk="high" />);
    expect(screen.getByRole('note')).toHaveTextContent(/Have a qualified person review it/);
    await expectNoA11yViolations();
  });

  it('accepts custom wording through children or messages', () => {
    const { rerender } = render(<AiDisclaimer>Answers come from your files only.</AiDisclaimer>);
    expect(screen.getByRole('note')).toHaveTextContent('Answers come from your files only.');
    rerender(<AiDisclaimer messages={{ low: 'L’IA peut se tromper.' }} />);
    expect(screen.getByRole('note')).toHaveTextContent('L’IA peut se tromper.');
  });

  it('allows the role to be overridden', () => {
    render(<AiDisclaimer role="presentation" data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveAttribute('role', 'presentation');
  });

  it('renders on the server', () => {
    const html = renderToString(<AiDisclaimer risk="high" />);
    expect(html).toContain('mn-ai-disclaimer');
    expect(html).toContain('data-risk="high"');
  });
});
