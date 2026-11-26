/**
 * The manifest-driven parts of a component page's Markdown copy
 * (`/components/<id>/index.md`): the stylesheet line, and a properties section
 * with defaults and allowed values. The HTML page shows the same props; the
 * Markdown adds what an assistant needs to write correct code in one read.
 *
 * Pure functions over manifest entries (`@rookcom/monarch-react/manifest.json`),
 * tested in component-markdown.test.ts.
 */

/** Manifest exports whose docs page is this route (`/components/button`). */
export function exportsOfRoute(manifest, route) {
  const path = `${route.replace(/\/$/, '')}/`;
  return manifest.components.filter((c) => c.docs && new URL(c.docs).pathname === path);
}

/** `import '…';` lines as one inline list. */
const imports = (sheets) => sheets.map((s) => `\`import '${s}';\``).join(' ');

/** The "Styles" paragraph: what to import so the exports render styled. */
export function stylesLine(exports) {
  const first = exports[0];
  if (!first) return '';
  const sheets = first.css ?? [];
  const smallest = [...new Set(exports.map((e) => e.componentCss).filter(Boolean))];
  const lines = [
    `**Styles:** load \`@rookcom/monarch-react/styles.css\` once in the root layout, or, with the smaller core sheet: ${imports(sheets)}`,
  ];
  if (smallest.length)
    lines.push(
      // Tokens and core, then the per-component sheets instead of the entry sheet.
      `Smallest option: ${imports([...sheets.slice(0, 2), ...smallest])}`,
    );
  return lines.join('\n\n');
}

const cell = (s) =>
  String(s ?? '')
    .replace(/\|/g, '\\|')
    .replace(/\s*\n\s*/g, ' ')
    .trim();

/**
 * A doc comment as Markdown: indented examples (two or more spaces after a
 * blank line) become fenced code blocks.
 */
export function docComment(text) {
  const lines = String(text ?? '').split('\n');
  const out = [];
  for (let i = 0; i < lines.length;) {
    const prevBlank = i === 0 || lines[i - 1].trim() === '';
    if (prevBlank && /^ {2,}\S/.test(lines[i])) {
      const block = [];
      while (i < lines.length && (/^ {2,}/.test(lines[i]) || lines[i] === ''))
        block.push(lines[i++]);
      while (block.length && block.at(-1).trim() === '') block.pop();
      const indent = Math.min(
        ...block.filter((l) => l.trim()).map((l) => l.length - l.trimStart().length),
      );
      out.push('```tsx', ...block.map((l) => l.slice(indent)), '```');
      if (i < lines.length) out.push('');
      continue;
    }
    out.push(lines[i++]);
  }
  return out.join('\n').trim();
}

/** The `## Properties` section, from the manifest, with Default and Values columns. */
export function propertiesMarkdown(exports) {
  const parts = exports.filter((e) => e.kind === 'component');
  if (!parts.length) return '';
  const out = ['## Properties'];
  for (const p of parts) {
    out.push(`### \`${p.name}\``);
    if (p.description) out.push(docComment(p.description));
    if (p.className) out.push(`Root class: \`.${p.className}\``);
    if (!p.props.length) {
      out.push("Accepts the underlying element's props.");
      continue;
    }
    out.push(
      [
        '| Prop | Type | Default | Values | Description |',
        '| --- | --- | --- | --- | --- |',
        ...p.props.map(
          (prop) =>
            `| \`${prop.name}\`${prop.required ? ' (required)' : ''} | \`${cell(prop.type)}\` | ${
              prop.default !== undefined ? `\`${cell(prop.default)}\`` : '–'
            } | ${
              prop.values
                ? prop.values.map((v) => `\`${cell(JSON.stringify(v))}\``).join(', ')
                : '–'
            } | ${cell(prop.description)} |`,
        ),
      ].join('\n'),
    );
  }
  return out.join('\n\n');
}

/**
 * Adds the manifest facts to a component page's Markdown: the Styles line after
 * the import block of the API reference (or after the title), and the
 * properties section replaced by the manifest's.
 */
export function enrichComponentMarkdown(md, exports) {
  if (!exports.length) return md;
  let out = md;
  const props = propertiesMarkdown(exports);
  if (props) {
    const re = /^## Properties\n[\s\S]*?(?=^## |(?![\s\S]))/m;
    out = re.test(out) ? out.replace(re, () => `${props}\n\n`) : out;
  }
  const styles = stylesLine(exports);
  const importBlock = /```tsx\nimport \{[^\n]*\} from '@rookcom\/monarch-react[^\n]*\n```\n/;
  if (importBlock.test(out)) out = out.replace(importBlock, (m) => `${m}\n${styles}\n`);
  else out = out.replace(/^(# .+\n)/m, (m) => `${m}\n${styles}\n`);
  return out;
}

/** `./ai` → `ai`; `.` → `core`. */
export const entryFileName = (entry) => (entry === '.' ? 'core' : entry.replace(/^\.\//, ''));
