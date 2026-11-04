import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ErrorPage } from './index.js';

describe('ErrorPage', () => {
  it('full screen: a main landmark labelled by its h1, with a decorative numeral', async () => {
    const { container } = render(
      <ErrorPage
        kind="not-found"
        title="Page not found"
        description="The link may be broken, or the page may have moved."
        actions={<a href="/">Go to dashboard</a>}
      />,
    );
    const main = screen.getByRole('main', { name: 'Page not found' });
    expect(main).toHaveAttribute('data-kind', 'not-found');
    expect(main).toHaveAttribute('data-full-screen');
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Page not found');
    const numeral = container.querySelector('.mn-error-page__numeral')!;
    expect(numeral).toHaveAttribute('data-numeral', '404');
    expect(numeral).toHaveTextContent('');
    expect(numeral).toHaveAttribute('aria-hidden', 'true');
    expect(screen.getByRole('link', { name: 'Go to dashboard' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('defaults the numeral by kind and lets the product override or hide it', () => {
    const { container, rerender } = render(<ErrorPage kind="forbidden" title="No access" />);
    const numeral = () => container.querySelector('.mn-error-page__numeral');
    expect(numeral()).toHaveAttribute('data-numeral', '403');
    rerender(<ErrorPage kind="server" title="Something went wrong" />);
    expect(numeral()).toHaveAttribute('data-numeral', '500');
    rerender(<ErrorPage kind="offline" title="You're offline" />);
    expect(numeral()).toBeNull();
    rerender(<ErrorPage kind="maintenance" title="Back soon" />);
    expect(numeral()).toBeNull();
    rerender(<ErrorPage kind="server" title="Bad gateway" numeral="502" />);
    expect(numeral()).toHaveAttribute('data-numeral', '502');
    rerender(<ErrorPage kind="not-found" title="Gone" numeral={false} />);
    expect(numeral()).toBeNull();
  });

  it('inside the app shell: a section, so it never nests a second main', async () => {
    render(
      <main>
        <ErrorPage fullScreen={false} kind="not-found" title="Project not found" />
      </main>,
    );
    expect(screen.getAllByRole('main')).toHaveLength(1);
    expect(screen.getByRole('region', { name: 'Project not found' })).not.toHaveAttribute(
      'data-full-screen',
    );
    await expectNoA11yViolations();
  });

  it('gives every instance its own title id', () => {
    render(
      <>
        <ErrorPage as="section" fullScreen={false} title="First" />
        <ErrorPage as="section" fullScreen={false} title="Second" />
      </>,
    );
    const [a, b] = screen.getAllByRole('heading');
    expect(a!.id).not.toBe(b!.id);
    expect(screen.getByRole('region', { name: 'Second' })).toBeInTheDocument();
  });

  it('retry comes first, is keyboard operable and shows a busy state', async () => {
    const onRetry = vi.fn();
    const { rerender } = render(
      <ErrorPage
        kind="server"
        title="Something went wrong"
        onRetry={onRetry}
        actions={<a href="/">Home</a>}
      />,
    );
    const user = userEvent.setup();
    await user.tab();
    const retry = screen.getByRole('button', { name: 'Try again' });
    expect(retry).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onRetry).toHaveBeenCalledOnce();
    rerender(<ErrorPage kind="server" title="Something went wrong" onRetry={onRetry} retrying />);
    expect(screen.getByRole('button', { name: 'Try again' })).toHaveAttribute('aria-busy', 'true');
  });

  it('offline: retries once when the browser comes back online', () => {
    const onRetry = vi.fn();
    const { rerender } = render(
      <ErrorPage kind="offline" title="You're offline" onRetry={onRetry} />,
    );
    act(() => {
      window.dispatchEvent(new Event('online'));
      window.dispatchEvent(new Event('online'));
    });
    expect(onRetry).toHaveBeenCalledOnce();
    onRetry.mockClear();
    rerender(
      <ErrorPage kind="offline" title="You're offline" onRetry={onRetry} retryWhenOnline={false} />,
    );
    act(() => {
      window.dispatchEvent(new Event('online'));
    });
    expect(onRetry).not.toHaveBeenCalled();
  });

  it('focusOnMount moves focus to the title', () => {
    render(<ErrorPage title="Page not found" focusOnMount />);
    expect(screen.getByRole('heading')).toHaveFocus();
  });

  it('renders brand, support, request id and forwards its ref', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <ErrorPage
        ref={ref}
        title="Something went wrong"
        brand={<span>Acme</span>}
        support={
          <>
            Still stuck? <a href="mailto:help@example.com">Email support</a>
          </>
        }
        requestId="req_42"
      />,
    );
    expect(ref.current?.tagName).toBe('MAIN');
    expect(screen.getByText('Acme').parentElement).toHaveClass('mn-error-page__brand');
    expect(screen.getByRole('link', { name: 'Email support' })).toBeInTheDocument();
    expect(screen.getByText('req_42').tagName).toBe('CODE');
    expect(screen.getByRole('button', { name: /Copy Request ID/ })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('as="div" drops the landmark label (no aria-labelledby on a generic element)', () => {
    const { container } = render(<ErrorPage as="div" fullScreen={false} title="Oops" />);
    expect(container.firstElementChild).not.toHaveAttribute('aria-labelledby');
  });

  it('renders on the server without the app shell', () => {
    const html = renderToString(<ErrorPage kind="not-found" title="Page not found" />);
    expect(html).toContain('<main');
    expect(html).toContain('mn-error-page');
    expect(html).toContain('404');
  });
});
