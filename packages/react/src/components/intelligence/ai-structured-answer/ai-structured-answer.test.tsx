import { describe, expect, it } from 'vitest';
import { createRef } from 'react';
import { render, screen, within } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiStructuredAnswer, type AiStructuredField } from './index.js';

const schema: AiStructuredField[] = [
  { key: 'name', label: 'Candidate', type: 'text' },
  { key: 'score', label: 'Score', type: 'number', format: { maximumFractionDigits: 1 } },
  {
    key: 'stage',
    label: 'Stage',
    type: 'badge',
    tone: (v) => (v === 'Onsite' ? 'info' : 'neutral'),
  },
  { key: 'strengths', label: 'Strengths', type: 'list' },
  { key: 'summary', label: 'Summary', type: 'markdown' },
];

const data = {
  name: 'Alex Morgan',
  score: 4.25,
  stage: 'Onsite',
  strengths: ['System design', 'Clear writing'],
  summary: 'A **strong** fit for the role.',
};

describe('AiStructuredAnswer', () => {
  it('renders a labelled AI answer with typed fields, forwarding ref and props', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <AiStructuredAnswer
        ref={ref}
        data-testid="root"
        title="Interview brief"
        schema={schema}
        data={data}
        locale="en-US"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-structured-answer');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('region', { name: 'AI answer Interview brief' })).toBe(root);
    expect(screen.getByRole('heading', { level: 3, name: 'Interview brief' })).toBeInTheDocument();
    expect(screen.getByText('Candidate').nextElementSibling).toHaveTextContent('Alex Morgan');
    expect(screen.getByText('4.3')).toBeInTheDocument();
    expect(screen.getByText('Onsite')).toHaveAttribute('data-tone', 'info');
    const list = screen.getByRole('list');
    expect(within(list).getAllByRole('listitem')).toHaveLength(2);
    expect(screen.getByText('strong').tagName).toBe('STRONG');
    await expectNoA11yViolations();
  });

  it('shows placeholders for missing fields while streaming, then announces completion', async () => {
    const { rerender } = render(
      <AiStructuredAnswer schema={schema} data={{ name: 'Alex Morgan' }} streaming />,
    );
    const region = screen.getByRole('region', { name: 'AI answer' });
    expect(region).toHaveAttribute('aria-busy', 'true');
    expect(screen.getAllByText('Still generating')).toHaveLength(4);
    rerender(<AiStructuredAnswer schema={schema} data={{ name: 'Alex Morgan', score: 3 }} />);
    expect(region).not.toHaveAttribute('aria-busy');
    expect(screen.queryByText('Still generating')).toBeNull();
    expect(screen.getAllByText('Not provided')).toHaveLength(3);
    expect(screen.getByRole('status')).toHaveTextContent('Response complete.');
    await expectNoA11yViolations();
  });

  it('never renders model HTML', () => {
    const { container } = render(
      <AiStructuredAnswer
        schema={[
          { key: 'x', label: 'X', type: 'markdown' },
          { key: 'y', label: 'Y', type: 'text' },
        ]}
        data={{ x: '<img src=x onerror=alert(1)>', y: '<b>bold</b>' }}
      />,
    );
    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelector('b')).toBeNull();
    expect(screen.getByText('<b>bold</b>')).toBeInTheDocument();
  });

  it('supports the grid layout and localised messages', () => {
    render(
      <AiStructuredAnswer
        data-testid="root"
        layout="grid"
        schema={schema.slice(0, 1)}
        data={{}}
        messages={{ aiAnswer: 'Réponse IA', empty: 'Non fourni' }}
      />,
    );
    expect(screen.getByTestId('root')).toHaveAttribute('data-layout', 'grid');
    expect(screen.getByRole('region', { name: 'Réponse IA' })).toBeInTheDocument();
    expect(screen.getByText('Non fourni')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AiStructuredAnswer schema={schema} data={data} streaming />);
    expect(html).toContain('mn-ai-structured-answer');
    expect(html).toContain('aria-busy="true"');
  });
});
