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
  /** Set when an existing baseline covers this finding. */
  baselined?: boolean;
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

export interface CompatibilityPackage {
  readonly current: string;
  readonly supported: string;
  readonly deprecated?: readonly { readonly range: string; readonly message: string }[];
}

export interface Compatibility {
  readonly packages: Readonly<Record<string, CompatibilityPackage>>;
  readonly peers: Readonly<Record<string, string>>;
}
