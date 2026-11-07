import { describe, expect, it } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiAgentAvatar, aiAgentInitials } from './index.js';

const agent = { id: 'research', name: 'Research agent', role: 'Finds sources' };

describe('AiAgentAvatar', () => {
  it('names itself as an AI agent, forwards ref and props', async () => {
    const ref = createRef<HTMLSpanElement>();
    render(<AiAgentAvatar ref={ref} agent={agent} data-testid="root" className="extra" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-agent-avatar', 'extra');
    expect(ref.current).toBe(root);
    expect(root).toHaveAttribute('data-state', 'idle');
    expect(root).toHaveAttribute('data-size', 'md');
    expect(screen.getByRole('img', { name: 'Research agent, AI agent' })).toBeInTheDocument();
    expect(root).toHaveTextContent('RA');
    await expectNoA11yViolations();
  });

  it('puts a non-idle state in the accessible name and optionally as visible text', async () => {
    const { rerender } = render(<AiAgentAvatar agent={agent} state="working" />);
    expect(screen.getByRole('img', { name: 'Research agent, AI agent, Working' })).toBeVisible();
    rerender(<AiAgentAvatar agent={agent} state="waiting" showState />);
    expect(screen.getByRole('img', { name: 'Research agent, AI agent, Waiting for you' }));
    expect(screen.getByText('Waiting for you')).toHaveAttribute('aria-hidden', 'true');
    rerender(<AiAgentAvatar agent={agent} state="error" data-testid="root" />);
    expect(screen.getByTestId('root')).toHaveAttribute('data-state', 'error');
    await expectNoA11yViolations();
  });

  it('is hidden when decorative but still reads a non-idle state', () => {
    const { container, rerender } = render(<AiAgentAvatar agent={agent} decorative />);
    expect(screen.queryByRole('img')).toBeNull();
    expect(container.textContent).toBe('RA');
    rerender(<AiAgentAvatar agent={agent} decorative state="working" />);
    expect(container.querySelector('.mn-visually-hidden')).toHaveTextContent('Working');
    rerender(<AiAgentAvatar agent={agent} decorative state="working" showState />);
    expect(screen.getByText('Working')).not.toHaveAttribute('aria-hidden');
  });

  it('shows the image and falls back to initials when it fails', () => {
    const { container } = render(
      <AiAgentAvatar agent={{ ...agent, avatar: 'https://example.com/agent.png' }} />,
    );
    const img = container.querySelector('img')!;
    expect(img).toHaveAttribute('alt', '');
    fireEvent.error(img);
    expect(container.querySelector('img')).toBeNull();
    expect(container).toHaveTextContent('RA');
  });

  it('uses the codename for initials, numeric sizes and translated strings', () => {
    expect(aiAgentInitials({ name: 'Research agent', codename: 'Scout' })).toBe('S');
    render(
      <AiAgentAvatar
        agent={agent}
        size={44}
        state="working"
        data-testid="root"
        messages={{ kind: 'Agent IA', states: { working: 'En cours' } }}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('data-size', 'custom');
    expect(root.style.getPropertyValue('--mn-ai-agent-avatar-size')).toBe('44px');
    expect(screen.getByRole('img', { name: 'Research agent, Agent IA, En cours' }));
  });

  it('renders on the server', () => {
    const html = renderToString(<AiAgentAvatar agent={agent} state="working" />);
    expect(html).toContain('mn-ai-agent-avatar');
    expect(html).toContain('Research agent, AI agent, Working');
  });
});
