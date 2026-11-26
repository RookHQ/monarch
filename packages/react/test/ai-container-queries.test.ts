import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

/**
 * AI components sit in narrow side panels on wide screens, so their narrow
 * layout is written twice: for a narrow screen (`@media`) and for a narrow
 * surrounding AI surface (`@container mn-ai`). CSS cannot share one rule body
 * between the two, so this keeps the copies identical.
 */
const ROOT = join(__dirname, '../src/components/intelligence');
const MEDIA = '@media (max-width: 30rem) {';
const CONTAINER = '@container mn-ai (max-width: 30rem) {';

function blocks(css: string, opener: string): string[] {
  const out: string[] = [];
  let from = css.indexOf(opener);
  while (from >= 0) {
    let i = from + opener.length;
    let depth = 1;
    while (depth > 0 && i < css.length) {
      if (css[i] === '{') depth += 1;
      else if (css[i] === '}') depth -= 1;
      i += 1;
    }
    out.push(css.slice(from + opener.length, i - 1).trim());
    from = css.indexOf(opener, i);
  }
  return out;
}

const sheets = readdirSync(ROOT, { withFileTypes: true })
  .filter((d) => d.isDirectory())
  .flatMap((d) =>
    readdirSync(join(ROOT, d.name))
      .filter((f) => f.endsWith('.css'))
      .map((f) => join(d.name, f)),
  );

describe('AI narrow layouts', () => {
  it('finds the stylesheets', () => {
    expect(sheets.length).toBeGreaterThan(50);
  });

  it.each(sheets)('%s: narrow-screen rules are repeated for a narrow AI surface', (file) => {
    const css = readFileSync(join(ROOT, file), 'utf8');
    expect(blocks(css, CONTAINER)).toEqual(blocks(css, MEDIA));
  });

  it('AiAssistantPanel provides the container', () => {
    const css = readFileSync(join(ROOT, 'ai-assistant-panel/ai-assistant-panel.css'), 'utf8');
    expect(css).toContain('container: mn-ai / inline-size;');
  });
});
