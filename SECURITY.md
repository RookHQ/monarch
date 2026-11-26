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

Monarch has no telemetry, no analytics and no calls home. The only network
code is in the AI transports of `@rookcom/monarch-react/ai`:
`createSseTransport` and `rookIntelligenceTransport` (built on it) call
`fetch` on the URL the app passes in, and only when the app uses them. Both
accept a custom `fetch`. Components that show a URL the app passes in (an
image, or a frame in `ChatDock`, `EmbeddedAppFrame`, `EmbedBlock`,
`FilePreview` or `AiArtifactCanvas`) let the browser load it as it would any
element. `@rookcom/monarch-tokens` and `@rookcom/monarch-conformance` make no
network requests.

The main risks are supply chain (`npm audit` and a secret scan run in CI, the
lockfile is committed, and releases are published from GitHub Actions with
npm trusted publishing and provenance, so no npm token is stored) and the
inline `appearanceScript`, which validates and JSON-encodes all input before
emitting it. To allow it under a Content Security Policy, see
[frameworks](docs/guides/frameworks.md#content-security-policy).
