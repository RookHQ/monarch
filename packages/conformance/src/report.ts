import type { CheckResult } from './check.js';
import { RULES, type RuleId } from './rules.js';
import type { Fix, Severity } from './types.js';

const ORDER: Severity[] = ['error', 'warn', 'info'];

/** The docs page that explains the conformance rules. */
export const RULES_DOCS_URL = 'https://monarchui.dev/conformance/';

/** Help text and docs link of a rule, for reports and agents. */
export function ruleInfo(rule: string): { help: string; docs: string } {
  const meta = RULES[rule as RuleId];
  return { help: meta ? `${meta.summary}. ${meta.help}` : rule, docs: RULES_DOCS_URL };
}

/** The docs page scrolled to one rule's row in the rules table (a text fragment; it falls back to the section). */
export function ruleDocsUrl(rule: string): string {
  return `${RULES_DOCS_URL}#rules:~:text=${encodeURIComponent(rule)}`;
}

/**
 * The check result as JSON. Each finding carries `help` and `docs`, and `fix`
 * when a machine-applicable change is known (`monarch check --fix`).
 */
export function toJson(result: CheckResult): string {
  const findings = result.findings.map((f) => ({ ...f, ...ruleInfo(f.rule) }));
  return JSON.stringify({ ...result, findings }, null, 2) + '\n';
}

export function toText(result: CheckResult, opts: { maxPerRule?: number } = {}): string {
  const max = opts.maxPerRule ?? 5;
  const out: string[] = [];
  const c = result.counts;
  out.push(
    `Monarch conformance ${result.tool.version}${result.product ? ` · ${result.product}` : ''}`,
    `${result.root}${result.git.commit ? ` @ ${result.git.commit.slice(0, 12)}${result.git.dirty ? ' (dirty)' : ''}` : ''}`,
    `${result.filesScanned} files, ${result.linesScanned.toLocaleString('en-US')} lines scanned`,
    '',
  );
  const rules = Object.keys(c.byRule).sort((a, b) => {
    const sa = result.findings.find((f) => f.rule === a)?.severity ?? 'info';
    const sb = result.findings.find((f) => f.rule === b)?.severity ?? 'info';
    return ORDER.indexOf(sa) - ORDER.indexOf(sb) || (c.byRule[b] ?? 0) - (c.byRule[a] ?? 0);
  });
  for (const rule of rules) {
    const list = result.findings.filter((f) => f.rule === rule);
    const fresh = list.filter((f) => !f.baselined).length;
    const sev = list[0]?.severity ?? 'info';
    const meta = RULES[rule as RuleId];
    out.push(
      `${sev.toUpperCase().padEnd(5)} ${rule}  ${list.length}${result.baseline ? ` (${fresh} new)` : ''}  ${result.densityPer1kLines[rule] ?? 0}/1k lines`,
    );
    if (meta) out.push(`      ${meta.summary}. ${meta.help}`);
    const show = (result.baseline ? list.filter((f) => !f.baselined) : list).slice(0, max);
    for (const f of show)
      out.push(
        `      ${f.file}:${f.line}:${f.column}  ${f.message}${f.fix ? `  [fix${f.fix.safety === 'safe' ? '' : ' (review)'}: ${f.fix.description}]` : ''}`,
      );
    const rest = (result.baseline ? fresh : list.length) - show.length;
    if (rest > 0) out.push(`      … ${rest} more`);
    out.push('');
  }
  if (result.exceptions.length) {
    out.push('Exceptions');
    for (const e of result.exceptions) {
      out.push(
        `  ${e.status.padEnd(7)} ${e.rule} on ${e.paths.join(', ')} · owner ${e.owner} · expires ${e.expires} · ${e.suppressed} suppressed`,
      );
    }
    out.push('');
  }
  out.push(
    `Totals: ${c.bySeverity.error} errors, ${c.bySeverity.warn} warnings, ${c.bySeverity.info} info` +
      (result.baseline ? ` · new vs baseline: ${c.new.error} errors, ${c.new.warn} warnings` : ''),
  );
  if (result.suppressed.inline || result.suppressed.exceptions) {
    out.push(
      `Suppressed: ${result.suppressed.inline} inline, ${result.suppressed.exceptions} by registry exceptions`,
    );
  }
  if (result.baseline?.improved) {
    out.push(
      `${result.baseline.improved} file/rule groups are below baseline; run --write-baseline to lock in the improvement.`,
    );
  }
  out.push(result.exitCode === 0 ? 'Result: pass' : 'Result: fail (new error-severity findings)');
  return out.join('\n') + '\n';
}

const LEVEL: Record<Severity, 'error' | 'warning' | 'note'> = {
  error: 'error',
  warn: 'warning',
  info: 'note',
};

/** SARIF 2.1.0 for GitHub code scanning (upload-sarif). */
export function toSarif(result: CheckResult): string {
  const usedRules = [...new Set(result.findings.map((f) => f.rule))].sort();
  const sarif = {
    $schema: 'https://json.schemastore.org/sarif-2.1.0.json',
    version: '2.1.0',
    runs: [
      {
        tool: {
          driver: {
            name: 'monarch',
            fullName: result.tool.name,
            version: result.tool.version,
            semanticVersion: result.tool.version,
            informationUri: RULES_DOCS_URL,
            rules: usedRules.map((id) => {
              const meta = RULES[id as RuleId];
              return {
                id,
                name: id.replace(/[/-](\w)/g, (_m, ch: string) => ch.toUpperCase()),
                shortDescription: { text: meta?.summary ?? id },
                fullDescription: { text: meta?.help ?? id },
                help: { text: meta?.help ?? id },
                helpUri: ruleDocsUrl(id),
                defaultConfiguration: {
                  level: LEVEL[(meta?.severity as Severity | undefined) ?? 'warn'],
                },
              };
            }),
          },
        },
        ...(result.git.commit
          ? {
              versionControlProvenance: [
                { repositoryUri: result.root, revisionId: result.git.commit },
              ],
            }
          : {}),
        results: result.findings.map((f) => ({
          ruleId: f.rule,
          ruleIndex: usedRules.indexOf(f.rule),
          level: LEVEL[f.severity],
          message: { text: f.message },
          baselineState: result.baseline ? (f.baselined ? 'unchanged' : 'new') : undefined,
          locations: [
            {
              physicalLocation: {
                artifactLocation: { uri: f.file },
                region: { startLine: f.line, startColumn: f.column, snippet: { text: f.snippet } },
              },
            },
          ],
          partialFingerprints: { 'monarch/v1': fingerprint(`${f.rule}|${f.file}|${f.snippet}`) },
          ...(f.fix ? { fixes: [sarifFix(f.fix)] } : {}),
        })),
      },
    ],
  };
  return JSON.stringify(sarif, null, 2) + '\n';
}

interface SarifReplacement {
  readonly deletedRegion: {
    readonly startLine: number;
    readonly startColumn: number;
    readonly endLine: number;
    readonly endColumn: number;
  };
  readonly insertedContent: { readonly text: string };
}

export interface SarifFix {
  readonly description: { readonly text: string };
  readonly artifactChanges: readonly {
    readonly artifactLocation: { readonly uri: string };
    readonly replacements: readonly SarifReplacement[];
  }[];
}

/**
 * A finding's fix as a SARIF 2.1.0 `fix` object (§3.55): one artifact change
 * per file, one replacement per edit. An insertion is a replacement whose
 * deleted region is empty. Fixes that need review say so in the description,
 * since SARIF has no field for it.
 */
export function sarifFix(fix: Fix): SarifFix {
  const byFile = new Map<string, SarifReplacement[]>();
  for (const e of fix.edits) {
    const replacement: SarifReplacement = {
      deletedRegion: {
        startLine: e.line,
        startColumn: e.column,
        endLine: e.line,
        endColumn: e.column + e.length,
      },
      insertedContent: { text: e.text },
    };
    byFile.set(e.file, [...(byFile.get(e.file) ?? []), replacement]);
  }
  return {
    description: {
      text: fix.safety === 'safe' ? fix.description : `${fix.description} (review before applying)`,
    },
    artifactChanges: [...byFile].map(([uri, replacements]) => ({
      artifactLocation: { uri },
      replacements,
    })),
  };
}

/** FNV-1a: stable across line moves, so code scanning doesn't reopen alerts. */
function fingerprint(s: string): string {
  let h = 0x811c9dc5;
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i);
    h = Math.imul(h, 0x01000193) >>> 0;
  }
  return h.toString(16).padStart(8, '0');
}
