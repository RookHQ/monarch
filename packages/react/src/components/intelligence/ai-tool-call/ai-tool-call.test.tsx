import { describe, expect, it } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { AiToolCallPart } from '../ai-conversation/index.js';
import {
  AiToolCall,
  formatToolValue,
  humanizeToolName,
  toolCallDuration,
  toolLabel,
} from './index.js';

const part = (
  overrides: { [K in keyof AiToolCallPart]?: AiToolCallPart[K] | undefined } = {},
): AiToolCallPart =>
  ({
    type: 'tool-call',
    id: 't1',
    name: 'search_documents',
    input: { query: 'Q3 plan', limit: 5 },
    state: 'succeeded',
    output: { hits: 2 },
    startedAt: '2026-01-01T10:00:00.000Z',
    endedAt: '2026-01-01T10:00:12.000Z',
    ...overrides,
  }) as AiToolCallPart;

describe('tool helpers', () => {
  it('humanizes tool names', () => {
    expect(humanizeToolName('search_documents')).toBe('Search documents');
    expect(humanizeToolName('searchDocuments')).toBe('Search documents');
    expect(humanizeToolName('crm.create-record')).toBe('Crm create record');
    expect(toolLabel('x_y', { x_y: 'Custom' })).toBe('Custom');
  });

  it('formats values', () => {
    expect(formatToolValue({ a: 1 })).toEqual({ text: '{\n  "a": 1\n}', json: true });
    expect(formatToolValue('{"a":1}')).toEqual({ text: '{\n  "a": 1\n}', json: true });
    expect(formatToolValue('plain text')).toEqual({ text: 'plain text', json: false });
    const circular: Record<string, unknown> = {};
    circular.self = circular;
    expect(formatToolValue(circular).json).toBe(false);
  });

  it('computes durations', () => {
    expect(toolCallDuration(part())).toBe(12000);
    expect(toolCallDuration({ startedAt: 'x', endedAt: 'y' })).toBeNull();
    expect(toolCallDuration({})).toBeNull();
  });
});

describe('AiToolCall', () => {
  it('renders the summary, state and duration; forwards props and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<AiToolCall ref={ref} data-testid="root" className="x" part={part()} />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-tool-call', 'x');
    expect(root).toHaveAttribute('data-state', 'succeeded');
    expect(ref.current).toBe(root);
    expect(screen.getByText('Search documents')).toBeInTheDocument();
    expect(screen.getByText('Done')).toBeInTheDocument();
    expect(screen.getByText('12s')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('prefers summary over describeTool over toolLabels', () => {
    const { rerender } = render(
      <AiToolCall part={part()} toolLabels={{ search_documents: 'Document search' }} />,
    );
    expect(screen.getByText('Document search')).toBeInTheDocument();
    rerender(
      <AiToolCall
        part={part()}
        describeTool={(p) => `Searched documents for “${(p.input as { query: string }).query}”`}
      />,
    );
    expect(screen.getByText('Searched documents for “Q3 plan”')).toBeInTheDocument();
    rerender(<AiToolCall part={part()} summary="Custom summary" describeTool={() => 'no'} />);
    expect(screen.getByText('Custom summary')).toBeInTheDocument();
  });

  it('expands to show pretty-printed input and output', async () => {
    const user = userEvent.setup();
    const { container } = render(<AiToolCall data-testid="root-x" part={part()} />);
    expect(screen.queryByText('Input')).toBeNull();
    const summary = container.querySelector('summary')!;
    await user.tab();
    expect(summary).toHaveFocus();
    await user.click(summary);
    await waitFor(() => expect(container.querySelector('details')).toHaveAttribute('open'));
    await waitFor(() => expect(screen.getByTestId('root-x')).toHaveAttribute('data-open'));
    expect(screen.getAllByText('Input').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Output').length).toBeGreaterThan(0);
    expect(container.textContent).toContain('"query": "Q3 plan"');
    expect(container.querySelector('.mn-ai-tool-call__name')).toHaveTextContent('search_documents');
    await expectNoA11yViolations();
  });

  it('shows the error of a failed call without expanding', () => {
    render(
      <AiToolCall
        part={part({ state: 'failed', error: 'Index unavailable', output: undefined })}
      />,
    );
    expect(screen.getByText('Failed')).toBeInTheDocument();
    expect(screen.getByText('Index unavailable')).toBeVisible();
  });

  it('labels every state with text', () => {
    const states = ['pending-approval', 'running', 'denied', 'cancelled'] as const;
    const labels = ['Waiting for approval', 'Running', 'Denied', 'Cancelled'];
    states.forEach((state, i) => {
      const { unmount } = render(<AiToolCall part={part({ state, endedAt: undefined })} />);
      expect(screen.getByText(labels[i]!)).toBeInTheDocument();
      unmount();
    });
  });

  it('announces state changes but not the initial state', () => {
    const { rerender } = render(
      <AiToolCall part={part({ state: 'running', endedAt: undefined })} />,
    );
    expect(screen.getByRole('status')).toHaveTextContent('');
    rerender(<AiToolCall part={part()} />);
    expect(screen.getByRole('status')).toHaveTextContent('Search documents: Done');
  });

  it('accepts message overrides', () => {
    render(
      <AiToolCall
        part={part()}
        messages={{ states: { succeeded: 'Fertig' }, duration: () => 'zwölf' }}
      />,
    );
    expect(screen.getByText('Fertig')).toBeInTheDocument();
    expect(screen.getByText('zwölf')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiToolCall part={part()} defaultOpen />);
    expect(html).toContain('mn-ai-tool-call');
    expect(html).toContain('Search documents');
  });
});
