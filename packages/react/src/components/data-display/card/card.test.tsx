import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Card, CardContent, CardHeader, CardTitle } from './index.js';

describe('Card', () => {
  it('renders accessibly with an h3 title by default', async () => {
    render(
      <main>
        <Card>
          <CardHeader>
            <CardTitle>Open tickets</CardTitle>
          </CardHeader>
          <CardContent>128</CardContent>
        </Card>
      </main>,
    );
    expect(screen.getByRole('heading', { name: 'Open tickets', level: 3 })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <Card>
        <CardContent>Body</CardContent>
      </Card>,
    );
    expect(html).toContain('mn-card');
  });
});
