# Security policy

## Reporting a vulnerability

Please report vulnerabilities **privately** using GitHub's private vulnerability
reporting: open the repository's **Security** tab and choose **Report a
vulnerability**. Do not open a public issue or pull request for a suspected
vulnerability.

Include what you found, the affected package and version, and steps to
reproduce. We aim to acknowledge reports within 5 working days, and will keep
you informed while we investigate and fix. Please give us a reasonable time to
release a fix before disclosing publicly.

## Supported versions

Security fixes are released for the current minor version of each published
package (`@rookcom/monarch-tokens`, `@rookcom/monarch-react` and
`@rookcom/monarch-conformance`). See
[docs/architecture/COMPATIBILITY.md](docs/architecture/COMPATIBILITY.md).

## Scope and notes

Monarch ships no network code. The main risks are supply chain (`npm audit`
and a secret scan run in CI, the lockfile is committed, and releases are
published from GitHub Actions with npm trusted publishing and provenance, so no
npm token is stored) and the inline `appearanceScript`, which validates and
JSON-encodes all input before emitting it.
