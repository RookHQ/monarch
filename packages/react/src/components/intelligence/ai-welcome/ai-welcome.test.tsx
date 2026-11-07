import { describe, expect, it } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiWelcome } from './index.js';

describe('AiWelcome', () => {
  it('renders a region named by its heading, with agent, description and capabilities', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <AiWelcome
        ref={ref}
        data-testid="root"
        className="extra"
        agent={{ name: 'Assistant', role: 'Workspace assistant' }}
        title="How can I help?"
        description="I can read the pages you have open."
        capabilities={['Summarise documents', 'Draft replies']}
        starters={<button type="button">Start here</button>}
        footer="Check important information."
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-welcome', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('region', { name: 'How can I help?' })).toBe(root);
    expect(screen.getByRole('heading', { level: 2, name: 'How can I help?' })).toBeInTheDocument();
    expect(root).toHaveTextContent('AI assistant: Assistant');
    expect(root).toHaveTextContent('Workspace assistant');
    expect(root).toHaveTextContent('I can read the pages you have open.');
    const list = screen.getByRole('list', { name: 'What I can help with' });
    expect(within(list).getAllByRole('listitem')).toHaveLength(2);
    expect(screen.getByRole('button', { name: 'Start here' })).toBeInTheDocument();
    expect(root).toHaveTextContent('Check important information.');
    await expectNoA11yViolations();
  });

  it('shows the Bot mark without an agent and a safe avatar image with one', () => {
    const { container, rerender } = render(<AiWelcome title="Hello" />);
    expect(container.querySelector('.mn-ai-welcome__glyph')).not.toBeNull();
    expect(container.querySelector('img')).toBeNull();
    expect(screen.queryByRole('list')).toBeNull();
    rerender(<AiWelcome title="Hello" agent={{ name: 'A', avatar: '/agent.png' }} />);
    expect(container.querySelector('img')).toHaveAttribute('src', '/agent.png');
    rerender(<AiWelcome title="Hello" agent={{ name: 'A', avatar: 'javascript:alert(1)' }} />);
    expect(container.querySelector('img')).toBeNull();
  });

  it('supports heading levels and messages', () => {
    render(
      <AiWelcome
        title="Hallo"
        headingLevel={3}
        capabilities={['Zusammenfassen']}
        messages={{ capabilities: 'Was ich kann' }}
      />,
    );
    expect(screen.getByRole('heading', { level: 3, name: 'Hallo' })).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Was ich kann' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiWelcome title="How can I help?" />);
    expect(html).toContain('mn-ai-welcome');
    expect(html).toContain('How can I help?');
  });
});
