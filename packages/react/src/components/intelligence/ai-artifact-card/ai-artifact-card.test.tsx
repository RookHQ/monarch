import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { AiArtifactPart } from '../ai-conversation/index.js';
import { AiArtifactCard } from './index.js';

const part: AiArtifactPart = {
  type: 'artifact',
  id: 'a1',
  kind: 'document',
  title: 'Q3 summary',
  version: 3,
};

describe('AiArtifactCard', () => {
  it('shows title, kind and version and names the button with all three', async () => {
    const ref = createRef<HTMLButtonElement>();
    render(<AiArtifactCard ref={ref} part={part} onOpen={() => {}} className="extra" />);
    const button = screen.getByRole('button', {
      name: /^Q3 summary Document\s*, version 3\s*, Open$/,
    });
    expect(button).toHaveClass('mn-ai-artifact-card', 'extra');
    expect(button).toHaveAttribute('data-kind', 'document');
    expect(button).toHaveAttribute('type', 'button');
    expect(button).toHaveTextContent('Document · v3');
    expect(ref.current).toBe(button);
    await expectNoA11yViolations();
  });

  it('opens with the part on click, Enter and Space', async () => {
    const user = userEvent.setup();
    const onOpen = vi.fn();
    render(<AiArtifactCard part={part} onOpen={onOpen} />);
    await user.click(screen.getByRole('button'));
    await user.keyboard('{Enter}');
    await user.keyboard(' ');
    expect(onOpen).toHaveBeenCalledTimes(3);
    expect(onOpen).toHaveBeenCalledWith(part);
    expect(screen.getByRole('button')).toHaveFocus();
  });

  it('marks the active artifact and localises strings', () => {
    render(
      <AiArtifactCard
        part={{ ...part, kind: 'html' }}
        onOpen={() => {}}
        active
        description="Updated today"
        messages={{ kinds: { html: 'Page' }, opened: 'Viewing', version: (v) => `rev ${v}` }}
      />,
    );
    const button = screen.getByRole('button');
    expect(button).toHaveAttribute('aria-current', 'true');
    expect(button).toHaveAttribute('data-active', 'true');
    expect(button).toHaveTextContent('Page · rev 3');
    expect(button).toHaveTextContent('Viewing');
    expect(button).toHaveTextContent('Updated today');
  });

  it('renders on the server', () => {
    const html = renderToString(<AiArtifactCard part={part} onOpen={() => {}} />);
    expect(html).toContain('mn-ai-artifact-card');
    expect(html).toContain('Q3 summary');
  });
});
