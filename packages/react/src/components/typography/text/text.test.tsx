import { describe, expect, it } from 'vitest';
import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Text } from './index.js';

describe('Text', () => {
  it('renders a paragraph at base size by default and forwards ref and props', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <Text ref={ref} data-testid="root" id="hint">
        Changes are saved automatically.
      </Text>,
    );
    const p = screen.getByTestId('root');
    expect(p).toBe(ref.current);
    expect(p.tagName).toBe('P');
    expect(p).toHaveClass('mn-text');
    expect(p).toHaveAttribute('data-size', 'base');
    expect(p).not.toHaveAttribute('data-tone');
    expect(p).not.toHaveAttribute('data-weight');
    expect(p).not.toHaveAttribute('data-truncate');
    await expectNoA11yViolations();
  });

  it('exposes size, tone, weight, alignment and tabular digits', () => {
    render(
      <Text data-testid="t" size="sm" tone="danger" weight="semibold" align="end" tabular>
        3 errors
      </Text>,
    );
    const t = screen.getByTestId('t');
    expect(t).toHaveAttribute('data-size', 'sm');
    expect(t).toHaveAttribute('data-tone', 'danger');
    expect(t).toHaveAttribute('data-weight', 'semibold');
    expect(t).toHaveAttribute('data-align', 'end');
    expect(t).toHaveAttribute('data-tabular');
  });

  it('inline elements inherit the size unless it is set', () => {
    render(
      <>
        <Text as="span" data-testid="span">
          inline
        </Text>
        <Text as="small" size="xs" data-testid="small">
          small
        </Text>
      </>,
    );
    expect(screen.getByTestId('span').tagName).toBe('SPAN');
    expect(screen.getByTestId('span')).not.toHaveAttribute('data-size');
    expect(screen.getByTestId('small')).toHaveAttribute('data-size', 'xs');
  });

  it('truncates to one line or clamps to a number of lines', () => {
    render(
      <>
        <Text data-testid="one" truncate title="Full text">
          Full text
        </Text>
        <Text data-testid="single" truncate={1}>
          One
        </Text>
        <Text data-testid="three" truncate={3} style={{ color: 'inherit' }}>
          Three
        </Text>
      </>,
    );
    expect(screen.getByTestId('one')).toHaveAttribute('data-truncate', 'line');
    expect(screen.getByTestId('one')).toHaveAttribute('title', 'Full text');
    expect(screen.getByTestId('single')).toHaveAttribute('data-truncate', 'line');
    const three = screen.getByTestId('three');
    expect(three).toHaveAttribute('data-truncate', 'lines');
    expect(three.style.getPropertyValue('--mn-text-lines')).toBe('3');
    expect(three.style.color).toBe('inherit');
  });

  it('renders as a label tied to its control', () => {
    render(
      <>
        <Text as="label" htmlFor="name" size="sm" weight="medium">
          Name
        </Text>
        <input id="name" />
      </>,
    );
    expect(screen.getByLabelText('Name')).toHaveAttribute('id', 'name');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <Text as="span" truncate={2}>
        Server
      </Text>,
    );
    expect(html).toContain('<span');
    expect(html).toContain('mn-text');
    expect(html).toContain('--mn-text-lines:2');
  });
});
