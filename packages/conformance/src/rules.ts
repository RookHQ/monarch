/**
 * Conformance rules. Each scanner is a pure function over one file's text, so
 * rules are easy to test with fixtures and cheap to run across 20+ repos.
 *
 * Scanning is lexical (regular expressions plus a small JSX tag reader), not a
 * full parse. Rules are written to prefer missing a case over inventing one.
 */
import type { Deprecation, ImportPathDeprecation, RuleMeta, Severity } from './types.js';
import { colourFix, publicPathOf, type RawFix } from './fixes.js';
import { scanImportPaths } from './import-paths.js';
import { MONARCH_COMPONENT_STEMS } from './monarch-components.js';
import { isPublicMonarchVariable } from './public-variables.js';

export const RULES = {
  'color/no-raw-palette': {
    id: 'color/no-raw-palette',
    severity: 'error',
    summary: 'Raw Tailwind palette colour or arbitrary colour class',
    help: 'Use a semantic colour (bg-mn-surface, text-mn-fg-muted, bg-mn-danger-subtle, or the legacy bg-primary / text-muted-foreground). Palette classes ignore theme, contrast mode and product accent.',
  },
  'color/no-hard-coded-color': {
    id: 'color/no-hard-coded-color',
    severity: 'error',
    summary: 'Hard-coded colour literal (hex, rgb(), hsl())',
    help: 'Reference a token: var(--mn-color-…) in CSS, or a semantic class. For charts use var(--mn-color-data-1…5).',
  },
  'typography/no-arbitrary-type': {
    id: 'typography/no-arbitrary-type',
    severity: 'warn',
    summary: 'Arbitrary font size, weight, family, line height or letter spacing',
    help: 'Use the Monarch type scale (text-2xs…text-5xl, font-medium, leading-snug, tracking-tight) and var(--mn-font-sans|mono).',
  },
  'spacing/off-grid': {
    id: 'spacing/off-grid',
    severity: 'warn',
    summary: 'Spacing value off the 4px grid',
    help: 'Use the spacing scale (p-1 = 4px, p-1.5 = 6px, p-2 = 8px …). Arbitrary values must be multiples of 2px or a var(--mn-space-…).',
  },
  'tokens/local-token-definitions': {
    id: 'tokens/local-token-definitions',
    severity: 'error',
    summary: 'Design tokens defined locally instead of imported from Monarch',
    help: 'Import @rookcom/monarch-tokens/css (plus /css/compat during migration) and set data-mn-product on <html>. Delete the local copy.',
  },
  'components/duplicated-primitive': {
    id: 'components/duplicated-primitive',
    severity: 'warn',
    summary: 'Local copy of a component Monarch provides',
    help: 'Import the component from @rookcom/monarch-react and delete the local file once call sites are migrated.',
  },
  'a11y/img-alt': {
    id: 'a11y/img-alt',
    severity: 'error',
    summary: '<img> without alt',
    help: 'Add alt text, or alt="" for decorative images (WCAG 1.1.1).',
  },
  'a11y/click-without-key': {
    id: 'a11y/click-without-key',
    severity: 'error',
    summary: 'Click handler on a non-interactive element without role and keyboard handler',
    help: 'Use <button> (preferred), or add role and onKeyDown plus tabIndex={0} (WCAG 2.1.1).',
  },
  'a11y/positive-tabindex': {
    id: 'a11y/positive-tabindex',
    severity: 'error',
    summary: 'Positive tabIndex',
    help: 'Use tabIndex={0} or -1. Positive values break the natural focus order (WCAG 2.4.3).',
  },
  'a11y/outline-none-without-ring': {
    id: 'a11y/outline-none-without-ring',
    severity: 'error',
    summary: 'Focus outline removed without a visible replacement',
    help: 'Pair outline-none with focus-visible:ring-2 focus-visible:ring-ring (or a Monarch component). WCAG 2.4.7 needs a visible focus indicator.',
  },
  'overrides/monarch-internals': {
    id: 'overrides/monarch-internals',
    severity: 'error',
    summary: 'Overrides Monarch internals (.mn-* selectors or --mn-* variables)',
    help: 'Do not restyle Monarch components through their classes or redefine --mn-* variables. The root and part `mn-*` class names listed in the manifest (`className`) are public and stable within a minor; internal modifier classes and `--_*` or undocumented variables are not. Use component props, the variables a component page documents as public (such as --mn-toast-offset-*), or request a variant or an exception through governance.',
  },
  'deps/monarch-version': {
    id: 'deps/monarch-version',
    severity: 'error',
    summary: 'Monarch package version status',
    help: 'Upgrade to a supported version listed in registry/compatibility.json. See the migration guide for each release.',
  },
  'api/deprecated': {
    id: 'api/deprecated',
    severity: 'warn',
    summary: 'Deprecated Monarch API',
    help: 'Move to the replacement named in the message before the removal version.',
  },
  'icons/no-sparkles': {
    id: 'icons/no-sparkles',
    severity: 'error',
    summary: 'Sparkle icon used',
    help: 'Monarch does not use sparkle icons (Sparkles, Sparkle, WandSparkles and its old name Wand2) for AI: they say "magic" rather than what the feature does. For AI features use the lucide `Bot` icon; elsewhere use a plain icon that names the thing, such as Inbox or Bell.',
  },
  'dialogs/no-native-dialog': {
    id: 'dialogs/no-native-dialog',
    severity: 'error',
    summary: 'Native browser dialog (window.confirm, alert or prompt)',
    help: 'Native dialogs are unthemed, cannot show a destructive tone, are blocked in iframes and cannot be translated. Use `useConfirm()` for confirm, `usePrompt()` for prompt, `useChoice()` when OK/Cancel stands for two options, and a toast or inline Alert for alert. For reversible deletes, act and offer `useUndoToast()` instead of asking.',
  },
  'styles/subpath-css': {
    id: 'styles/subpath-css',
    severity: 'warn',
    summary: 'Subpath components used without their stylesheet',
    help: "Components from @rookcom/monarch-react/<entry> (patterns, data, charts, ai, shell …) are not in styles/core.css. Load them once in the root layout: import '@rookcom/monarch-react/styles/<entry>.css', or one styles/components/<id>.css per component, or the full styles.css.",
  },
  'imports/package-path': {
    id: 'imports/package-path',
    severity: 'warn',
    summary: "Deep import into a Monarch package's dist folder",
    help: "Import the public subpath instead (`@rookcom/monarch-tokens/css`, `@rookcom/monarch-react/styles.css`, `@rookcom/monarch-react/ai` …). Paths inside dist/ are not part of the package's exports and can move in any release. `monarch check --fix` rewrites them.",
  },
  'legacy/bridge-variable': {
    id: 'legacy/bridge-variable',
    severity: 'info',
    summary: 'Legacy shadcn variable used directly (var(--primary) …)',
    help: 'Tracked for migration. Prefer var(--mn-color-…) so the legacy bridge can eventually be removed.',
  },
  'governance/suppression-without-reason': {
    id: 'governance/suppression-without-reason',
    severity: 'warn',
    summary: 'Suppression comment without a reason',
    help: 'Write `monarch-ignore-next-line <rule> -- <reason>`. Suppressions without a reason are ignored.',
  },
  'governance/expired-exception': {
    id: 'governance/expired-exception',
    severity: 'error',
    summary: 'Registry exception has expired',
    help: 'Fix the findings it covered, or renew it in the product registry entry with a new expiry and approver.',
  },
} as const satisfies Record<string, RuleMeta>;

export type RuleId = keyof typeof RULES;
export const RULE_IDS = Object.keys(RULES) as RuleId[];

export interface RawFinding {
  readonly rule: RuleId;
  readonly line: number;
  readonly column: number;
  readonly message: string;
  /** Overrides the rule's default severity for this finding. */
  readonly severity?: Severity;
  /** A machine-applicable fix (fixes.ts). */
  readonly fix?: RawFix;
}

export interface FileContext {
  /** Path relative to scan root. */
  readonly file: string;
  readonly ext: string;
  readonly text: string;
  readonly lines: readonly string[];
  readonly deprecations: readonly Deprecation[];
  /** Exports that moved to another entry (registry `importPaths`). */
  readonly importPaths?: readonly ImportPathDeprecation[];
}

// ---------------------------------------------------------------- shared

const PALETTE =
  'slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose';
const COLOR_PROPS =
  'text|bg|border(?:-[xytrblse])?|ring(?:-offset)?|outline|from|via|to|fill|stroke|divide|placeholder|caret|accent|decoration|shadow';
const VARIANTS = '(?:(?:[\\w-]+|\\[[^\\]\\s]*\\]|[\\w-]+-\\[[^\\]\\s]*\\]):)*';

export const PALETTE_RE = new RegExp(
  `(?<![\\w-])${VARIANTS}!?(?:${COLOR_PROPS})-(?:(?:${PALETTE})-(?:50|[1-9]00|950)|white|black)(?:\\/(?:\\d{1,3}|\\[[^\\]]+\\]))?(?![\\w-])`,
  'g',
);
export const ARBITRARY_COLOR_RE = new RegExp(
  `(?<![\\w-])${VARIANTS}!?(?:${COLOR_PROPS})-\\[(?:#[0-9a-fA-F]{3,8}|(?:rgba?|hsla?|oklch|oklab)\\(\\s*[\\d.])[^\\]]*\\]`,
  'g',
);
export const HEX_RE =
  /(?<=[\s'"`(,:=_])#([0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{4}|[0-9a-fA-F]{3})(?![\w-])/g;
export const FUNC_COLOR_RE = /\b(?:rgba?|hsla?|oklch|oklab|lab|lch|hwb)\(\s*-?[\d.]/g;

/** Legacy variables from the shadcn-era token copies. */
export const LEGACY_TOKEN_NAMES = new Set([
  'background',
  'foreground',
  'card',
  'card-foreground',
  'popover',
  'popover-foreground',
  'primary',
  'primary-foreground',
  'secondary',
  'secondary-foreground',
  'muted',
  'muted-foreground',
  'accent',
  'accent-foreground',
  'destructive',
  'destructive-foreground',
  'success',
  'success-foreground',
  'warning',
  'warning-foreground',
  'info',
  'info-foreground',
  'border',
  'input',
  'ring',
  'radius',
]);

export function isLegacyToken(name: string): boolean {
  return LEGACY_TOKEN_NAMES.has(name) || name.startsWith('sidebar-') || /^chart-\d+$/.test(name);
}

const isCode = (ext: string) => /^\.(?:[cm]?[jt]sx?)$/.test(ext);
const isJsx = (ext: string) => ext === '.tsx' || ext === '.jsx';
const isCss = (ext: string) => ext === '.css' || ext === '.scss';

/** True for lines that are only a comment (we don't scan commented-out code). */
function isCommentLine(line: string): boolean {
  const t = line.trimStart();
  return t.startsWith('//') || t.startsWith('/*') || t.startsWith('*') || t.startsWith('{/*');
}

function eachMatch(re: RegExp, s: string, fn: (m: RegExpExecArray) => void): void {
  re.lastIndex = 0;
  let m: RegExpExecArray | null;
  while ((m = re.exec(s)) !== null) {
    fn(m);
    if (m[0].length === 0) re.lastIndex++;
  }
}

/** Index → 1-based line/column. */
function locator(text: string): (index: number) => { line: number; column: number } {
  const starts = [0];
  for (let i = 0; i < text.length; i++) if (text.charCodeAt(i) === 10) starts.push(i + 1);
  return (index) => {
    let lo = 0;
    let hi = starts.length - 1;
    while (lo < hi) {
      const mid = (lo + hi + 1) >> 1;
      if ((starts[mid] as number) <= index) lo = mid;
      else hi = mid - 1;
    }
    return { line: lo + 1, column: index - (starts[lo] as number) + 1 };
  };
}

/** Characters after which a quote opens a string literal rather than being JSX text. */
const EXPRESSION_START = new Set([
  '(',
  ',',
  '=',
  ':',
  '[',
  '!',
  '&',
  '|',
  '?',
  '{',
  '}',
  ';',
  '+',
  '-',
  '*',
  '%',
  '<',
  '>',
  '~',
  '^',
]);
const KEYWORD_BEFORE =
  /(?:^|[^\w$])(?:return|case|typeof|void|await|yield|throw|in|of|else|default|from|import|export)\s*$/;

/**
 * Blanks comments and the contents of string and template literals, keeping
 * the quotes, line breaks and every offset. Text scans for JSX tags and calls
 * then skip markup quoted in docs strings, comments and test payloads (an
 * `<img>` in a note, a sanitiser demo's `onerror=alert(1)`).
 *
 * A heuristic, not a parser: a quote opens a string only where an expression
 * can start, so an apostrophe in JSX text ("Don't") stays text, and '…' or
 * "…" ends at the line break. Template `${…}` expressions stay code.
 */
export function maskNonCode(text: string): string {
  const out = text.split('');
  const blank = (i: number) => {
    if (text[i] !== '\n') out[i] = ' ';
  };
  /** Brace depth inside each open template `${…}`. */
  const templates: number[] = [];
  let i = 0;
  const lastCode = (from: number): string | undefined => {
    for (let j = from; j >= 0; j--) {
      const c = out[j] as string;
      if (!/\s/.test(c)) return c;
    }
    return undefined;
  };
  const opensString = (at: number) => {
    const prev = lastCode(at - 1);
    if (prev === undefined || EXPRESSION_START.has(prev)) return true;
    return KEYWORD_BEFORE.test(out.slice(Math.max(0, at - 12), at).join(''));
  };
  const readTemplate = () => {
    // At the character after an opening ` or a closing } of `${…}`.
    while (i < text.length) {
      const c = text[i];
      if (c === '\\') {
        blank(i);
        blank(i + 1);
        i += 2;
      } else if (c === '`') {
        i++;
        return;
      } else if (c === '$' && text[i + 1] === '{') {
        templates.push(0);
        i += 2;
        return;
      } else {
        blank(i);
        i++;
      }
    }
  };
  while (i < text.length) {
    const c = text[i] as string;
    const next = text[i + 1];
    if (c === '/' && next === '/' && text[i - 1] !== ':') {
      while (i < text.length && text[i] !== '\n') blank(i++);
    } else if (c === '/' && next === '*') {
      const end = text.indexOf('*/', i + 2);
      const stop = end === -1 ? text.length : end + 2;
      while (i < stop) blank(i++);
    } else if ((c === "'" || c === '"') && opensString(i)) {
      i++;
      while (i < text.length && text[i] !== c && text[i] !== '\n') {
        if (text[i] === '\\' && text[i + 1] !== '\n') blank(i++);
        blank(i++);
      }
      if (text[i] === c) i++;
    } else if (c === '`' && opensString(i)) {
      i++;
      readTemplate();
    } else if (templates.length > 0 && (c === '{' || c === '}')) {
      const depth = templates.pop() as number;
      i++;
      if (c === '{') templates.push(depth + 1);
      else if (depth > 0) templates.push(depth - 1);
      else readTemplate();
    } else {
      i++;
    }
  }
  return out.join('');
}

// ---------------------------------------------------------------- colour

function scanColor(ctx: FileContext, out: RawFinding[]): void {
  ctx.lines.forEach((line, i) => {
    if (isCommentLine(line)) return;
    const n = i + 1;
    // Design-token definitions are reported once per block by tokens/local-token-definitions
    // (or overrides/monarch-internals for --mn-*); app-local variables are still checked.
    const decl = isCss(ctx.ext) ? /^\s*--([\w-]+)\s*:/.exec(line) : null;
    const cssDecl = decl
      ? isLegacyToken(decl[1] as string) || (decl[1] as string).startsWith('mn-')
      : false;

    // Classes appear in TS strings and in CSS @apply.
    if (isCode(ctx.ext) || (isCss(ctx.ext) && /@apply\b/.test(line))) {
      eachMatch(PALETTE_RE, line, (m) =>
        out.push({
          rule: 'color/no-raw-palette',
          line: n,
          column: m.index + 1,
          message: `Palette class \`${m[0]}\``,
        }),
      );
      eachMatch(ARBITRARY_COLOR_RE, line, (m) =>
        out.push({
          rule: 'color/no-raw-palette',
          line: n,
          column: m.index + 1,
          message: `Arbitrary colour class \`${m[0]}\``,
        }),
      );
    }

    if (cssDecl) return; // reported by tokens/local-token-definitions
    if (isCss(ctx.ext) && /^\s*@import\b/.test(line)) return;

    eachMatch(HEX_RE, line, (m) => {
      const before = line.slice(0, m.index);
      // Anchors, routes and SVG references, not colours.
      if (/(?:href|to|hash|anchor|id|path|url)\s*[=:]\s*\{?\s*['"`]?$/i.test(before)) return;
      if (/url\(\s*['"]?$/.test(before)) return;
      if (/&$/.test(before)) return;
      const hex = m[1] as string;
      const after = line[m.index + m[0].length] ?? '';
      // "Ticket #1234 created" / 'Order #1042': all-digit, followed by prose → an
      // issue number. A 4-digit all-numeric token is never a colour in practice.
      if (/^\d{4}$/.test(hex)) return;
      if (/^\d+$/.test(hex) && !/^['"`;),\]}]?$/.test(after)) return;
      if (/^\d+$/.test(hex) && /\s$/.test(before) && after === '') return;
      const fix = colourFix(hex, n, m.index + 1);
      out.push({
        rule: 'color/no-hard-coded-color',
        line: n,
        column: m.index + 1,
        message: `Colour literal \`#${hex}\``,
        ...(fix ? { fix } : {}),
      });
    });
    eachMatch(FUNC_COLOR_RE, line, (m) => {
      out.push({
        rule: 'color/no-hard-coded-color',
        line: n,
        column: m.index + 1,
        message: `Colour function \`${m[0].replace(/\s+$/, '')}…)\``,
      });
    });
  });
}

// ---------------------------------------------------------------- typography & spacing

export const ARB_TYPE_RE = new RegExp(
  `(?<![\\w-])${VARIANTS}!?(text|font|leading|tracking)-\\[([^\\]\\s]+)\\]`,
  'g',
);
export const ARB_SPACE_RE = new RegExp(
  `(?<![\\w-])${VARIANTS}!?-?(p[xytrblse]?|m[xytrblse]?|gap(?:-[xy])?|space-[xy]|inset(?:-[xy])?|scroll-[mp][xytrbl]?)-\\[([^\\]\\s]+)\\]`,
  'g',
);

/** px value on the 2px half-grid (the scale has 0.5 = 2px steps), or not a length. */
export function onGrid(value: string): boolean {
  if (/var\(|calc\(|env\(|%|vh|vw|dvh|svh|lvh|fr|auto|min\(|max\(|clamp\(/.test(value)) return true;
  const lengths = value.split(/[_\s,]+/).filter(Boolean);
  return lengths.every((len) => {
    // em is relative to the element's font (prose rhythm), so only px/rem are checked.
    const m = /^(-?\d*\.?\d+)(px|rem)?$/.exec(len);
    if (!m) return true;
    const num = Number(m[1]);
    const px = m[2] === 'rem' ? num * 16 : num;
    if (px === 0 || Math.abs(px) === 1) return true; // hairlines
    return Math.abs(px) % 2 === 0;
  });
}

function scanTypeAndSpace(ctx: FileContext, out: RawFinding[]): void {
  ctx.lines.forEach((line, i) => {
    if (isCommentLine(line)) return;
    const n = i + 1;
    if (isCode(ctx.ext) || (isCss(ctx.ext) && /@apply\b/.test(line))) {
      eachMatch(ARB_TYPE_RE, line, (m) => {
        const value = m[2] as string;
        if (/^(?:#|rgba?\(|hsla?\(|oklch|color-mix|var\(|length:var)/.test(value)) return;
        out.push({
          rule: 'typography/no-arbitrary-type',
          line: n,
          column: m.index + 1,
          message: `Arbitrary type value \`${m[0]}\``,
        });
      });
      eachMatch(ARB_SPACE_RE, line, (m) => {
        if (onGrid(m[2] as string)) return;
        out.push({
          rule: 'spacing/off-grid',
          line: n,
          column: m.index + 1,
          message: `Off-grid spacing \`${m[0]}\``,
        });
      });
    }
    if (isCode(ctx.ext)) {
      const ff = /\bfontFamily\s*:\s*['"`]([^'"`]+)['"`]/.exec(line);
      if (ff && !/var\(--/.test(ff[1] as string) && !/^inherit$/.test(ff[1] as string)) {
        out.push({
          rule: 'typography/no-arbitrary-type',
          line: n,
          column: ff.index + 1,
          message: `Inline font family \`${ff[1]}\``,
        });
      }
    }
    if (isCss(ctx.ext)) {
      // Global matching: some hand-written CSS packs many rules on one line.
      eachMatch(/(?:^|[;{\s])font-family\s*:\s*([^;}]+)/g, line, (ff) => {
        if (/^\s*(?:inherit|initial|unset|var\(--)/.test(ff[1] as string)) return;
        out.push({
          rule: 'typography/no-arbitrary-type',
          line: n,
          column: ff.index + 1,
          message: `font-family declared locally: \`${(ff[1] as string).trim().slice(0, 60)}\``,
        });
      });
      eachMatch(
        /(?:^|[;{\s])(padding|margin|gap|row-gap|column-gap)(?:-[a-z]+)?\s*:\s*([^;}]+)/g,
        line,
        (sp) => {
          if (onGrid((sp[2] as string).trim())) return;
          out.push({
            rule: 'spacing/off-grid',
            line: n,
            column: sp.index + 1,
            message: `Off-grid ${sp[1]}: \`${(sp[2] as string).trim()}\``,
          });
        },
      );
    }
  });
}

// ---------------------------------------------------------------- tokens, overrides, legacy

function scanCssStructure(ctx: FileContext, out: RawFinding[]): void {
  if (!isCss(ctx.ext)) return;
  let selector = '';
  let pendingSelector = '';
  let group: { selector: string; line: number; names: string[] } | null = null;
  const flush = () => {
    if (group && group.names.length) {
      out.push({
        rule: 'tokens/local-token-definitions',
        line: group.line,
        column: 1,
        message: `${group.names.length} design-token variable${group.names.length === 1 ? '' : 's'} defined in \`${group.selector || '(top level)'}\` (${group.names
          .slice(0, 4)
          .map((x) => '--' + x)
          .join(', ')}${group.names.length > 4 ? ', …' : ''})`,
      });
    }
    group = null;
  };
  ctx.lines.forEach((line, i) => {
    const n = i + 1;
    if (isCommentLine(line)) return;
    const open = line.indexOf('{');
    if (open !== -1) {
      flush();
      selector = (pendingSelector + ' ' + line.slice(0, open)).trim().replace(/\s+/g, ' ');
      pendingSelector = '';
      if (/(^|[\s,>+~(])\.mn-[a-z]/.test(selector)) {
        out.push({
          rule: 'overrides/monarch-internals',
          line: n,
          column: 1,
          message: `Selector targets Monarch internals: \`${selector.slice(0, 80)}\``,
        });
      }
    } else if (/[,]\s*$/.test(line) && !line.includes(':')) {
      pendingSelector += ' ' + line.trim();
    }
    if (line.includes('}')) flush();
    const def = /^\s*--([\w-]+)\s*:/.exec(line);
    if (def) {
      const name = def[1] as string;
      if (name.startsWith('mn-') && !isPublicMonarchVariable(name)) {
        out.push({
          rule: 'overrides/monarch-internals',
          line: n,
          column: (def.index ?? 0) + 1,
          message: `Redefines Monarch variable \`--${name}\``,
        });
      } else if (isLegacyToken(name)) {
        if (!group) group = { selector, line: n, names: [] };
        group.names.push(name);
      }
    }
  });
  flush();
}

const LEGACY_VAR_RE = /var\(\s*--([\w-]+)/g;
const INLINE_MN_VAR_RE = /--(mn-[\w-]+)['"]?\s*:/g;
const MN_SELECTOR_IN_CODE_RE = /(?:['"`\s&_>+~,(]|^)\.mn-[a-z][\w-]*/g;

function scanLegacyAndOverrides(ctx: FileContext, out: RawFinding[]): void {
  ctx.lines.forEach((line, i) => {
    if (isCommentLine(line)) return;
    const n = i + 1;
    eachMatch(LEGACY_VAR_RE, line, (m) => {
      if (isLegacyToken(m[1] as string)) {
        out.push({
          rule: 'legacy/bridge-variable',
          line: n,
          column: m.index + 1,
          message: `Legacy variable \`--${m[1]}\``,
        });
      }
    });
    if (isCode(ctx.ext)) {
      eachMatch(MN_SELECTOR_IN_CODE_RE, line, (m) => {
        out.push({
          rule: 'overrides/monarch-internals',
          line: n,
          column: m.index + 1,
          message: `Targets Monarch class \`${m[0].trim().replace(/^['"`&_>+~,(]/, '')}\``,
        });
      });
      if (/--mn-[\w-]+['"]?\s*:/.test(line) && !/var\(--mn-/.test(line.split(':')[0] ?? '')) {
        // One finding per line, at the first variable that is not public API.
        let first = -1;
        eachMatch(INLINE_MN_VAR_RE, line, (m) => {
          if (first === -1 && !isPublicMonarchVariable(m[1] as string)) first = m.index;
        });
        if (first !== -1) {
          out.push({
            rule: 'overrides/monarch-internals',
            line: n,
            column: first + 1,
            message: 'Sets a Monarch variable inline',
          });
        }
      }
    }
  });
}

// ---------------------------------------------------------------- components

export function kebab(stem: string): string {
  return stem
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .replace(/_/g, '-')
    .toLowerCase();
}

function scanDuplicate(ctx: FileContext, out: RawFinding[]): void {
  const m =
    /(?:^|\/)components\/(?:ui|primitives|common)\/(?:([A-Za-z][\w-]*)\.(?:tsx|jsx|ts|js)|([A-Za-z][\w-]*)\/index\.(?:tsx|jsx|ts|js))$/.exec(
      ctx.file,
    );
  if (!m) return;
  const stem = kebab((m[1] ?? m[2]) as string);
  const name = MONARCH_COMPONENT_STEMS.get(stem);
  if (!name) return;
  out.push({
    rule: 'components/duplicated-primitive',
    line: 1,
    column: 1,
    message: `Local \`${stem}\` duplicates Monarch \`${name}\``,
  });
}

// ---------------------------------------------------------------- accessibility (JSX)

interface Tag {
  readonly name: string;
  readonly start: number;
  readonly text: string;
}

/** Reads opening JSX tags, balancing braces so arrow functions don't end a tag. */
export function readTags(text: string, names: RegExp): Tag[] {
  const tags: Tag[] = [];
  const re = new RegExp(`<(${names.source})(?=[\\s/>])`, 'g');
  let m: RegExpExecArray | null;
  while ((m = re.exec(text)) !== null) {
    let depth = 0;
    let quote: string | null = null;
    let end = -1;
    for (let i = m.index + m[0].length; i < text.length && i < m.index + 4000; i++) {
      const c = text[i];
      if (quote) {
        if (c === quote && text[i - 1] !== '\\') quote = null;
        continue;
      }
      if (c === '{') depth++;
      else if (c === '}') depth--;
      else if (depth === 0 && (c === '"' || c === "'")) quote = c;
      else if (depth > 0 && (c === '"' || c === "'" || c === '`')) quote = c;
      else if (c === '>' && depth === 0) {
        end = i;
        break;
      }
    }
    if (end !== -1)
      tags.push({ name: m[1] as string, start: m.index, text: text.slice(m.index, end + 1) });
  }
  return tags;
}

const NON_INTERACTIVE =
  /div|span|li|td|tr|p|section|article|header|footer|main|nav|aside|ul|ol|img|h[1-6]|label/;

/** Items of composite widgets whose keyboard handling lives on the container. */
const COMPOSITE_ITEM_ROLE =
  /\srole\s*=\s*["'{]\s*['"]?(?:option|menuitem|tab|treeitem|gridcell)['"]?/;

function scanA11y(ctx: FileContext, out: RawFinding[]): void {
  if (!isJsx(ctx.ext)) return;
  const at = locator(ctx.text);
  // aria-activedescendant: focus stays on the input/container, which handles
  // every key, so its items legitimately take only pointer events.
  const activeDescendant = /aria-activedescendant/.test(ctx.text);
  // Find tags in code only; read their attributes from the source.
  const code = maskNonCode(ctx.text);
  const tagsOf = (names: RegExp) =>
    readTags(code, names).map((t) => ({
      ...t,
      text: ctx.text.slice(t.start, t.start + t.text.length),
    }));
  for (const tag of tagsOf(/img/)) {
    if (/\{\s*\.\.\./.test(tag.text)) continue;
    if (!/\balt\s*=/.test(tag.text)) {
      const { line, column } = at(tag.start);
      out.push({ rule: 'a11y/img-alt', line, column, message: '<img> has no alt attribute' });
    }
  }
  for (const tag of tagsOf(NON_INTERACTIVE)) {
    if (!/\sonClick\s*=/.test(tag.text)) continue;
    if (/\{\s*\.\.\./.test(tag.text)) continue;
    if (/\saria-hidden(?!\s*=\s*(?:"false"|\{false\}))(?=[\s=/>])/.test(tag.text)) continue;
    // Wrappers that only stop propagation are not controls.
    const handler = /\sonClick\s*=\s*\{([\s\S]*?)\}\s*(?:\w+=|\/?>|\{)/.exec(tag.text)?.[1] ?? '';
    if (
      /stopPropagation\(\)/.test(handler) &&
      !/[;,]\s*\w+\(/.test(handler.replace(/\w+\.stopPropagation\(\)/, ''))
    )
      continue;
    const hasRole = /\srole\s*=/.test(tag.text);
    const hasKey = /\sonKey(?:Down|Up|Press)\s*=/.test(tag.text);
    if (hasRole && hasKey) continue;
    if (activeDescendant && COMPOSITE_ITEM_ROLE.test(tag.text)) continue;
    const { line, column } = at(tag.start);
    const missing = [hasRole ? null : 'role', hasKey ? null : 'onKeyDown']
      .filter(Boolean)
      .join(' and ');
    out.push({
      rule: 'a11y/click-without-key',
      line,
      column,
      message: `<${tag.name} onClick> without ${missing}`,
    });
  }
  eachMatch(/\btabIndex\s*=\s*(?:\{\s*(\d+)\s*\}|["'](\d+)["'])/g, ctx.text, (m) => {
    const v = Number(m[1] ?? m[2]);
    if (v > 0) {
      const { line, column } = at(m.index);
      out.push({ rule: 'a11y/positive-tabindex', line, column, message: `tabIndex=${v}` });
    }
  });
}

const OUTLINE_NONE_RE = new RegExp(`(?<![\\w-])${VARIANTS}outline-none(?![\\w-])`, 'g');
/** A focus style on a wrapper, e.g. an input group that rings on `focus-within`. */
const FOCUS_WITHIN_RE =
  /focus-within:(?:ring-(?!0(?![\w.]))|ring(?![\w-])|outline-(?!none|0(?![\w.]))|outline(?![\w-])|shadow|border)/;
/** How far up to look for that wrapper. A heuristic: the ancestor is usually in the same JSX block. */
const FOCUS_WITHIN_LOOKBACK = 15;

const FOCUS_REPLACEMENT_RE =
  /(?:focus|focus-visible|focus-within|group-focus(?:-visible)?|peer-focus(?:-visible)?|data-\[focus[^\]]*\]|data-\[highlighted\]|data-highlighted|aria-selected|data-\[selected[^\]]*\]|has-\[:focus-visible\]):(?:ring-(?!0(?![\w.]))|ring(?![\w-])|outline-(?!none|0(?![\w.]))|outline(?![\w-])|shadow|border|bg-|underline)/;

function scanOutline(ctx: FileContext, out: RawFinding[]): void {
  if (!isCode(ctx.ext)) return;
  ctx.lines.forEach((line, i) => {
    if (isCommentLine(line)) return;
    eachMatch(OUTLINE_NONE_RE, line, (m) => {
      // Classes for one element are often split across a few lines (cva, cn()).
      const window = ctx.lines.slice(Math.max(0, i - 3), i + 4).join('\n');
      if (FOCUS_REPLACEMENT_RE.test(window)) return;
      const above = ctx.lines.slice(Math.max(0, i - FOCUS_WITHIN_LOOKBACK), i).join('\n');
      if (FOCUS_WITHIN_RE.test(above)) return;
      out.push({
        rule: 'a11y/outline-none-without-ring',
        line: i + 1,
        column: m.index + 1,
        message: `\`${m[0]}\` with no focus-visible replacement nearby`,
      });
    });
  });
}

// ---------------------------------------------------------------- icons

/** Sparkle-style glyphs from icon libraries Rook apps use. */
const SPARKLE_ICONS = [
  'Sparkles',
  'Sparkle',
  'WandSparkles',
  'Wand2',
  'SparklesIcon',
  'SparkleIcon',
];
const ICON_IMPORT_RE =
  /import\s*\{([^}]*)\}\s*from\s*['"](lucide-react|@radix-ui\/react-icons|react-icons\/[\w-]+|@heroicons\/react[\w/-]*)['"]/g;

function scanSparkles(ctx: FileContext, out: RawFinding[]): void {
  if (!isCode(ctx.ext)) return;
  ICON_IMPORT_RE.lastIndex = 0;
  const imported = new Set<string>();
  for (let m = ICON_IMPORT_RE.exec(ctx.text); m; m = ICON_IMPORT_RE.exec(ctx.text)) {
    for (const part of (m[1] ?? '').split(',')) {
      const [name, alias] = part.trim().split(/\s+as\s+/);
      if (name && SPARKLE_ICONS.includes(name)) imported.add(alias ?? name);
    }
  }
  if (imported.size === 0) return;
  const useRe = new RegExp(`(?<![\\w$.])(${[...imported].join('|')})(?![\\w$])`, 'g');
  ctx.lines.forEach((line, i) => {
    if (isCommentLine(line)) return;
    eachMatch(useRe, line, (m) => {
      out.push({
        rule: 'icons/no-sparkles',
        line: i + 1,
        column: m.index + 1,
        message: `Sparkle icon \`${m[1]}\`; use \`Bot\` for AI features or a plain icon elsewhere`,
      });
    });
  });
}

// ---------------------------------------------------------------- native dialogs

const NATIVE_DIALOGS = ['confirm', 'alert', 'prompt'] as const;
const GLOBAL_DIALOG_RE =
  /(?<![\w$.])(?:window|globalThis|self)\s*\.\s*(confirm|alert|prompt)\s*\(/g;
const BARE_DIALOG_RE = /(?<![\w$.])(confirm|alert|prompt)\s*\(/g;
/** `confirm(…) {` or `prompt(input: string): T {`: a method definition, not a call. */
const METHOD_DEF_RE = /^(?:confirm|alert|prompt)\s*\([^)]*\)\s*(?::[^{=;]*)?\{/;

/**
 * Names the file binds itself (`const { confirm } = useConfirm()`, an import,
 * a parameter or a local function). A bare call to one of those is the app's
 * own function, such as Monarch's `confirm()`, so it is not reported.
 */
function locallyBound(text: string): Set<string> {
  const bound = new Set<string>();
  for (const name of NATIVE_DIALOGS) {
    const declared = new RegExp(
      [
        `\\b(?:const|let|var|function)\\s+${name}\\b`,
        `[{,]\\s*(?:\\w+\\s*:\\s*)?${name}\\s*(?:=[^,}]*)?[,}]`,
        `\\b\\w+\\s+as\\s+${name}\\b`,
        `\\(\\s*${name}\\s*[,):]`,
        `,\\s*${name}\\s*[,):]`,
      ].join('|'),
    );
    if (declared.test(text)) bound.add(name);
  }
  return bound;
}

/** True when `index` sits inside a '…', "…" or `…` literal on this line. */
function inStringLiteral(line: string, index: number): boolean {
  let quote: string | null = null;
  for (let i = 0; i < index; i++) {
    const c = line[i];
    if (c === '\\') {
      i++;
      continue;
    }
    if (quote) {
      if (c === quote) quote = null;
    } else if (c === "'" || c === '"' || c === '`') quote = c;
  }
  return quote !== null;
}

function scanNativeDialogs(ctx: FileContext, out: RawFinding[]): void {
  if (!isCode(ctx.ext)) return;
  if (!/\b(?:confirm|alert|prompt)\s*\(/.test(ctx.text)) return;
  const bound = locallyBound(ctx.text);
  const report = (line: number, column: number, name: string, global: boolean) =>
    out.push({
      rule: 'dialogs/no-native-dialog',
      line,
      column,
      message: `\`${global ? 'window.' : ''}${name}()\` opens a native browser dialog; use ${
        name === 'confirm'
          ? '`useConfirm()`'
          : name === 'prompt'
            ? '`usePrompt()`'
            : 'a toast or an inline `Alert`'
      } from @rookcom/monarch-react`,
    });
  // Calls in comments and strings (a sanitiser demo's `onerror=alert(1)`) are not calls.
  const codeLines = maskNonCode(ctx.text).split('\n');
  ctx.lines.forEach((source, i) => {
    if (isCommentLine(source)) return;
    const line = codeLines[i] ?? '';
    eachMatch(GLOBAL_DIALOG_RE, line, (m) => {
      if (!inStringLiteral(line, m.index)) report(i + 1, m.index + 1, m[1] ?? '', true);
    });
    eachMatch(BARE_DIALOG_RE, line, (m) => {
      const name = m[1] ?? '';
      if (bound.has(name) || inStringLiteral(line, m.index)) return;
      if (METHOD_DEF_RE.test(source.slice(m.index))) return;
      report(i + 1, m.index + 1, name, false);
    });
  });
}

// ---------------------------------------------------------------- deprecated API

function scanDeprecated(ctx: FileContext, out: RawFinding[]): void {
  if (!isCode(ctx.ext) || ctx.deprecations.length === 0) return;
  for (const d of ctx.deprecations) {
    const importRe = new RegExp(
      `from\\s+['"]${d.package.replace(/[/.]/g, '\\$&')}(?:/[^'"]*)?['"]`,
    );
    if (!importRe.test(ctx.text)) continue;
    const nameRe = new RegExp(`(?<![\\w$.])${d.name.replace(/[.$]/g, '\\$&')}(?![\\w$])`, 'g');
    ctx.lines.forEach((line, i) => {
      if (isCommentLine(line)) return;
      eachMatch(nameRe, line, (m) => {
        const fix: RawFix | undefined = d.replacement
          ? {
              description: `Rename \`${d.name}\` to \`${d.replacement}\` (check the import too)`,
              safety: 'review',
              edits: [
                { line: i + 1, column: m.index + 1, length: d.name.length, text: d.replacement },
              ],
            }
          : undefined;
        out.push({
          rule: 'api/deprecated',
          line: i + 1,
          column: m.index + 1,
          ...(fix ? { fix } : {}),
          message: `\`${d.name}\` from ${d.package} is deprecated since ${d.since}${d.replacement ? `; use \`${d.replacement}\`` : ''}${d.message ? `. ${d.message}` : ''}`,
        });
      });
    });
  }
}

/** Imports of exports that moved to another entry, from the path they moved away from. */
function scanMovedImports(ctx: FileContext, out: RawFinding[]): void {
  if (!isCode(ctx.ext) || !ctx.importPaths?.length) return;
  if (!ctx.importPaths.some((d) => ctx.text.includes(d.from))) return;
  for (const f of scanImportPaths(ctx.text, ctx.importPaths)) {
    if (isCommentLine(ctx.lines[f.line - 1] ?? '')) continue;
    out.push({ rule: 'api/deprecated', ...f });
  }
}

// ---------------------------------------------------------------- package paths

const MONARCH_SPECIFIER_RE = /(['"])(@rookcom\/monarch-(?:react|tokens)\/dist\/[^'"]+)\1/g;

function scanPackagePaths(ctx: FileContext, out: RawFinding[]): void {
  if (!ctx.text.includes('@rookcom/monarch-')) return;
  ctx.lines.forEach((line, i) => {
    if (isCommentLine(line)) return;
    if (!/\b(?:import|from|require)\b|@import/.test(line)) return;
    eachMatch(MONARCH_SPECIFIER_RE, line, (m) => {
      const specifier = m[2] as string;
      const to = publicPathOf(specifier);
      const column = m.index + 2;
      out.push({
        rule: 'imports/package-path',
        line: i + 1,
        column,
        message: to
          ? `\`${specifier}\` reaches into dist/; import \`${to}\``
          : `\`${specifier}\` reaches into dist/, which is not part of the package's exports`,
        ...(to
          ? {
              fix: {
                description: `Import ${to}`,
                safety: 'safe',
                edits: [{ line: i + 1, column, length: specifier.length, text: to }],
              },
            }
          : {}),
      });
    });
  });
}

/** Runs every file-level rule. Package-level rules live in check.ts. */
export function scanFile(ctx: FileContext): RawFinding[] {
  const out: RawFinding[] = [];
  scanColor(ctx, out);
  scanTypeAndSpace(ctx, out);
  scanCssStructure(ctx, out);
  scanLegacyAndOverrides(ctx, out);
  scanDuplicate(ctx, out);
  scanA11y(ctx, out);
  scanOutline(ctx, out);
  scanDeprecated(ctx, out);
  scanMovedImports(ctx, out);
  scanSparkles(ctx, out);
  scanNativeDialogs(ctx, out);
  scanPackagePaths(ctx, out);
  return out;
}

// ---------------------------------------------------------------- suppressions

export interface Suppression {
  /** Line the suppression applies to (1-based). */
  readonly target: number;
  readonly rules: readonly string[];
  readonly reason: string | null;
  readonly line: number;
  readonly column: number;
}

const SUPPRESS_RE =
  /monarch-ignore-next-line\s+([\w/*,\s-]*?)(?:\s+--\s*(.*?))?\s*(?:\*\/\s*\}?)?\s*$/;

// A line holding only a comment: line comment, block comment, JSX comment, or a doc-comment continuation.
const COMMENT_ONLY = /^\s*(?:\/\/.*|\/\*.*\*\/\s*|\{\s*\/\*.*\*\/\s*\}\s*|\*.*)$/;

export function readSuppressions(lines: readonly string[]): Suppression[] {
  const out: Suppression[] = [];
  lines.forEach((line, i) => {
    const idx = line.indexOf('monarch-ignore-next-line');
    if (idx === -1) return;
    const m = SUPPRESS_RE.exec(line.slice(idx));
    if (!m) return;
    const rules = (m[1] ?? '')
      .split(/[\s,]+/)
      .map((r) => r.trim())
      .filter(Boolean);
    const reason = m[2]?.replace(/\*\/\s*\}?\s*$/, '').trim() || null;
    // Target the next line of code, skipping comment-only lines, so the
    // directive can sit alongside eslint-disable-next-line and similar.
    let t = i + 1;
    while (t < lines.length && COMMENT_ONLY.test(lines[t] ?? '')) t++;
    out.push({ target: t + 1, rules, reason, line: i + 1, column: idx + 1 });
  });
  return out;
}

export function ruleMatches(pattern: string, rule: string): boolean {
  if (pattern === '*' || pattern === rule) return true;
  if (pattern.endsWith('/*')) return rule.startsWith(pattern.slice(0, -1));
  return false;
}
