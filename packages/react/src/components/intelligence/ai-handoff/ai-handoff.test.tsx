import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiHandoff, isAiHandoffHuman } from './index.js';

const research = { id: 'research', name: 'Research agent' };
const writer = { id: 'writer', name: 'Writing agent' };
const now = new Date('2026-03-02T12:00:00Z');

describe('AiHandoff', () => {
  it('is a labelled note naming both agents as AI agents', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiHandoff
        ref={ref}
        from={research}
        to={writer}
        reason="Sources are ready to draft from"
        at={new Date('2026-03-02T11:57:00Z')}
        now={now}
        locale="en"
        data-testid="root"
        className="extra"
      />,
    );
    const note = screen.getByRole('note', { name: 'Handoff' });
    expect(note).toBe(ref.current);
    expect(note).toHaveClass('mn-ai-handoff', 'extra');
    expect(note).toHaveAttribute('data-to', 'agent');
    expect(note).toHaveTextContent(
      'Research agent (AI agent) handed off to Writing agent (AI agent)',
    );
    expect(note).toHaveTextContent('Reason: Sources are ready to draft from');
    expect(note.querySelector('time')).toHaveTextContent('3 minutes ago');
    // Pictures are decorative: the sentence carries the meaning.
    expect(screen.queryByRole('img')).toBeNull();
    await expectNoA11yViolations();
  });

  it('hands off to a person without calling them an agent', async () => {
    render(<AiHandoff from={research} to={{ kind: 'human', name: 'Sam Lee' }} />);
    const note = screen.getByRole('note');
    expect(note).toHaveAttribute('data-to', 'human');
    expect(note).toHaveTextContent('Research agent (AI agent) handed off to Sam Lee');
    expect(note).not.toHaveTextContent('Sam Lee (AI agent)');
    expect(note.querySelector('time')).toBeNull();
    expect(note.querySelector('.mn-avatar')).not.toBeNull();
    await expectNoA11yViolations();
  });

  it('accepts translated strings, including the sentence order', () => {
    render(
      <AiHandoff
        from={research}
        to={{ kind: 'human', name: 'Sam Lee' }}
        reason="Besoin d’une validation"
        messages={{
          label: 'Transfert',
          agentKind: 'agent IA',
          reason: 'Motif :',
          handedOff: (from, to, toHuman) => (
            <>
              {to} {toHuman ? 'reprend' : 'prend'} la main de {from}
            </>
          ),
        }}
      />,
    );
    const note = screen.getByRole('note', { name: 'Transfert' });
    expect(note).toHaveTextContent('Sam Lee reprend la main de Research agent (agent IA)');
    expect(note).toHaveTextContent('Motif : Besoin d’une validation');
  });

  it('tells people from agents', () => {
    expect(isAiHandoffHuman({ kind: 'human', name: 'Sam' })).toBe(true);
    expect(isAiHandoffHuman(writer)).toBe(false);
  });

  it('renders on the server', () => {
    const html = renderToString(<AiHandoff from={research} to={writer} at={now} now={now} />);
    expect(html).toContain('mn-ai-handoff');
    expect(html).toContain('Writing agent');
  });
});
