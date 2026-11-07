import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createRef, forwardRef, memo, type ComponentType } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import type { AiUiPart } from '../ai-conversation/index.js';
import { AiGenerativeUi, createAiUiRegistry, sanitizeAiUiProps } from './index.js';

const received: Record<string, unknown>[] = [];

function WeatherCard(props: { city?: string; temperature?: number }) {
  received.push(props as Record<string, unknown>);
  return (
    <p data-testid="weather">
      {props.city}: {props.temperature}°
    </p>
  );
}
function LinkCard(props: Record<string, unknown>) {
  received.push(props);
  return (
    <a href={typeof props.href === 'string' ? props.href : '/fallback'}>{String(props.label)}</a>
  );
}
function Echo(props: Record<string, unknown>) {
  received.push(props);
  return (
    <span data-testid="echo">{typeof props.children === 'string' ? props.children : 'echo'}</span>
  );
}
function Thrower(): never {
  throw new Error('boom');
}

const registry = createAiUiRegistry({ WeatherCard, LinkCard, Echo, Thrower });
const part = (component: string, props: unknown): AiUiPart => ({ type: 'ui', component, props });

afterEach(() => {
  received.length = 0;
  delete (window as unknown as Record<string, unknown>).__pwned;
});

describe('createAiUiRegistry', () => {
  it('returns a frozen allow-list with no prototype', () => {
    expect(Object.getPrototypeOf(registry)).toBeNull();
    expect(Object.isFrozen(registry)).toBe(true);
    expect(Object.keys(registry)).toEqual(['WeatherCard', 'LinkCard', 'Echo', 'Thrower']);
  });

  it('accepts memo and forwardRef components', () => {
    const Memo = memo(WeatherCard);
    const Fwd = forwardRef<HTMLSpanElement>(function Fwd(_, ref) {
      return <span ref={ref}>fwd</span>;
    });
    expect(() => createAiUiRegistry({ Memo, Fwd })).not.toThrow();
  });

  it('refuses intrinsic element names and other non-components', () => {
    for (const value of ['div', 'iframe', 'script', 42, null, {}]) {
      expect(() => createAiUiRegistry({ Bad: value as unknown as ComponentType<unknown> })).toThrow(
        TypeError,
      );
    }
  });
});

describe('AiGenerativeUi: allow-list', () => {
  it('renders an allow-listed component with its props as data', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiGenerativeUi
        ref={ref}
        part={part('WeatherCard', { city: 'Lisbon', temperature: 21 })}
        registry={registry}
        data-testid="root"
      />,
    );
    expect(screen.getByTestId('weather')).toHaveTextContent('Lisbon: 21°');
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-generative-ui');
    expect(root).toHaveAttribute('data-component', 'WeatherCard');
    expect(root).toHaveAttribute('data-state', 'rendered');
    expect(ref.current).toBe(root);
    await expectNoA11yViolations();
  });

  it.each([
    'UnknownWidget',
    'script',
    'iframe',
    'div',
    'constructor',
    '__proto__',
    'toString',
    'hasOwnProperty',
    'valueOf',
    '',
  ])('renders a safe notice for the unknown name %j', async (name) => {
    const onReject = vi.fn();
    const p = part(name, { src: 'https://example.com' });
    const { container } = render(
      <AiGenerativeUi part={p} registry={registry} onReject={onReject} data-testid="root" />,
    );
    expect(screen.getByText('This part of the answer can’t be shown here.')).toBeInTheDocument();
    expect(screen.getByTestId('root')).toHaveAttribute('data-state', 'rejected');
    expect(container.querySelector('script, iframe, img, object, embed')).toBeNull();
    expect(received).toHaveLength(0);
    expect(onReject).toHaveBeenCalledWith('unknown-component', p);
    await expectNoA11yViolations();
  });

  it('ignores a plain object registry’s inherited names', () => {
    render(<AiGenerativeUi part={part('toString', {})} registry={{ WeatherCard }} />);
    expect(screen.getByText('This part of the answer can’t be shown here.')).toBeInTheDocument();
  });

  it('refuses a string in a hand-built registry, so no raw HTML element can render', () => {
    const unsafe = { Frame: 'iframe' } as unknown as Record<string, ComponentType<unknown>>;
    const { container } = render(
      <AiGenerativeUi part={part('Frame', { srcDoc: '<script></script>' })} registry={unsafe} />,
    );
    expect(container.querySelector('iframe')).toBeNull();
    expect(screen.getByText('This part of the answer can’t be shown here.')).toBeInTheDocument();
  });

  it('never renders model text as HTML', () => {
    const html = '<img src=x onerror="window.__pwned=1"><b id="injected">x</b>';
    const { container } = render(
      <AiGenerativeUi part={part('Echo', { children: html })} registry={registry} />,
    );
    expect(screen.getByTestId('echo')).toHaveTextContent(html);
    expect(container.querySelector('img, b')).toBeNull();
    expect(document.getElementById('injected')).toBeNull();
    expect((window as unknown as Record<string, unknown>).__pwned).toBeUndefined();
  });
});

describe('AiGenerativeUi: props', () => {
  it('strips HTML injection, handlers, styling and unsafe URLs before the component sees them', () => {
    const { container } = render(
      <AiGenerativeUi
        part={part('LinkCard', {
          label: 'Docs',
          href: 'javascript:alert(1)',
          dangerouslySetInnerHTML: { __html: '<img src=x onerror=alert(1)>' },
          onClick: 'alert(1)',
          onerror: 'alert(1)',
          style: { position: 'fixed', inset: 0 },
          className: 'mn-dialog',
          id: 'login',
          ref: 'x',
          srcDoc: '<script></script>',
          nested: { src: 'data:text/html,<script>', ok: 'yes', onLoad: 'x', deep: [1, 'two'] },
        })}
        registry={registry}
      />,
    );
    expect(received).toHaveLength(1);
    expect(received[0]).toEqual({ label: 'Docs', nested: { ok: 'yes', deep: [1, 'two'] } });
    const link = screen.getByRole('link', { name: 'Docs' });
    expect(link).toHaveAttribute('href', '/fallback');
    expect(container.querySelector('img')).toBeNull();
  });

  it('keeps safe URLs', () => {
    expect(sanitizeAiUiProps({ href: 'https://example.com/a', src: '/img.png' })).toEqual({
      href: 'https://example.com/a',
      src: '/img.png',
    });
  });

  it('drops functions, symbols, React elements, class instances and non-finite numbers', () => {
    const el = <b>x</b>;
    expect(
      sanitizeAiUiProps({
        fn: () => 1,
        sym: Symbol('s'),
        el,
        date: new Date(),
        nan: Number.NaN,
        inf: Infinity,
        list: [el, 'a', () => 1],
        children: el,
        n: 3,
      }),
    ).toEqual({ list: ['a'], n: 3 });
  });

  it('stops at a maximum depth', () => {
    let deep: Record<string, unknown> = { v: 'bottom' };
    for (let i = 0; i < 20; i++) deep = { d: deep };
    const out = JSON.stringify(sanitizeAiUiProps(deep));
    expect(out).not.toContain('bottom');
  });

  it('treats non-object props as invalid', () => {
    for (const props of ['<b>x</b>', 42, ['a'], new Date()]) {
      const { unmount } = render(
        <AiGenerativeUi part={part('WeatherCard', props)} registry={registry} />,
      );
      expect(
        screen.getByText('This part of the answer had data that couldn’t be shown.'),
      ).toBeInTheDocument();
      unmount();
    }
    expect(received).toHaveLength(0);
  });

  it('runs validators on the sanitised props', () => {
    const onReject = vi.fn();
    const validate = {
      WeatherCard: (p: Record<string, unknown>) =>
        typeof p.city === 'string' && typeof p.temperature === 'number',
      Echo: () => {
        throw new Error('validator bug');
      },
    };
    const { rerender } = render(
      <AiGenerativeUi
        part={part('WeatherCard', { city: 'Oslo', temperature: 'hot' })}
        registry={registry}
        validate={validate}
        onReject={onReject}
      />,
    );
    expect(
      screen.getByText('This part of the answer had data that couldn’t be shown.'),
    ).toBeInTheDocument();
    expect(onReject).toHaveBeenCalledWith(
      'invalid-props',
      expect.objectContaining({ component: 'WeatherCard' }),
    );
    rerender(<AiGenerativeUi part={part('Echo', {})} registry={registry} validate={validate} />);
    expect(screen.queryByTestId('echo')).toBeNull();
    rerender(
      <AiGenerativeUi
        part={part('WeatherCard', { city: 'Oslo', temperature: 4 })}
        registry={registry}
        validate={validate}
      />,
    );
    expect(screen.getByTestId('weather')).toHaveTextContent('Oslo: 4°');
    expect(received).toHaveLength(1);
  });
});

describe('AiGenerativeUi: failures', () => {
  it('contains a component that throws', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const onReject = vi.fn();
    render(
      <>
        <AiGenerativeUi part={part('Thrower', {})} registry={registry} onReject={onReject} />
        <p>Rest of the thread</p>
      </>,
    );
    expect(screen.getByText('This part of the answer couldn’t be displayed.')).toBeInTheDocument();
    expect(screen.getByText('Rest of the thread')).toBeInTheDocument();
    expect(onReject).toHaveBeenCalledWith('render-error', expect.anything(), expect.any(Error));
    spy.mockRestore();
  });

  it('uses renderFallback and localised messages', () => {
    const { rerender } = render(
      <AiGenerativeUi
        part={part('Nope', {})}
        registry={registry}
        renderFallback={(reason, p) => <em>{`${reason}: ${p.component}`}</em>}
      />,
    );
    expect(screen.getByText('unknown-component: Nope')).toBeInTheDocument();
    rerender(
      <AiGenerativeUi
        part={part('Nope', {})}
        registry={registry}
        messages={{ unknown: (name) => `Not available: ${name}` }}
      />,
    );
    expect(screen.getByText('Not available: Nope')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(
      renderToString(
        <AiGenerativeUi
          part={part('WeatherCard', { city: 'Rome', temperature: 18 })}
          registry={registry}
        />,
      ),
    ).toContain('Rome');
    const html = renderToString(
      <AiGenerativeUi part={part('script', { children: 'alert(1)' })} registry={registry} />,
    );
    expect(html).not.toContain('<script');
    expect(html).toContain('can’t be shown here');
  });
});
