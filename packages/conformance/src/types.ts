export type Severity = 'error' | 'warn' | 'info';
export type RuleSetting = Severity | 'off';

export interface Finding {
  readonly rule: string;
  readonly severity: Severity;
  /** Path relative to the scan root, forward slashes. */
  readonly file: string;
  readonly line: number;
  readonly column: number;
  readonly message: string;
  readonly snippet: string;
  /** A machine-applicable change that resolves the finding, when one is known. */
  readonly fix?: Fix;
  /** Set when an existing baseline covers this finding. */
  baselined?: boolean;
}

/** Replace `length` characters at line/column (1-based) with `text`; `length` 0 inserts. */
export interface TextEdit {
  /** Path relative to the scan root, forward slashes. */
  readonly file: string;
  readonly line: number;
  readonly column: number;
  readonly length: number;
  readonly text: string;
}

export interface Fix {
  readonly description: string;
  /**
   * `safe`: keeps what the code does today (`monarch check --fix` applies it).
   * `review`: right in most cases but changes behaviour somewhere, such as a
   * colour that differs in dark mode (`--fix=all` applies it too).
   */
  readonly safety: 'safe' | 'review';
  readonly edits: readonly TextEdit[];
}

export interface RuleMeta {
  readonly id: string;
  readonly severity: Severity;
  readonly summary: string;
  readonly help: string;
}

export interface ProductException {
  readonly rule: string;
  readonly paths: readonly string[];
  readonly owner: string;
  readonly reason: string;
  readonly expires: string;
  readonly approvedBy?: string;
}

export interface MonarchConfig {
  readonly product?: string;
  readonly include?: readonly string[];
  readonly exclude?: readonly string[];
  readonly rules?: Readonly<Record<string, RuleSetting>>;
}

export interface Deprecation {
  readonly name: string;
  readonly package: string;
  readonly since: string;
  readonly replacement?: string;
  readonly message?: string;
}

/**
 * Exports that moved to another entry of the same package. The old path keeps
 * working until `removeNoEarlierThan`; `api/deprecated` reports an import of one of
 * `names` (or of a name matching `pattern`, which also covers their types)
 * from `from` and rewrites it to `to`.
 */
export interface ImportPathDeprecation {
  readonly names: readonly string[];
  /** A regular expression source; a name it matches moved too (`RookRailProps`). */
  readonly pattern?: string;
  /** The deprecated module specifier, `@rookcom/monarch-react/shell`. */
  readonly from: string;
  /** The module to import from instead, `@rookcom/monarch-react/rook`. */
  readonly to: string;
  readonly since: string;
  /** The date (YYYY-MM-DD) before which the old path is not removed: `since` plus the 90-day window. */
  readonly removeNoEarlierThan?: string;
  readonly message?: string;
}

export interface CompatibilityPackage {
  readonly current: string;
  readonly supported: string;
  readonly deprecated?: readonly { readonly range: string; readonly message: string }[];
}

export interface Compatibility {
  readonly packages: Readonly<Record<string, CompatibilityPackage>>;
  readonly peers: Readonly<Record<string, string>>;
}
