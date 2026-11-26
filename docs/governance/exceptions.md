# Exceptions

A conformance exception is a recorded, time-limited permission to deviate.

```json
{
  "rule": "color/no-hard-coded-color",
  "paths": ["src/app/opengraph-image.tsx"],
  "owner": "@someone",
  "reason": "OG images render outside the DOM; no CSS variables available.",
  "expires": "2027-05-01",
  "approvedBy": "@monarch-maintainer"
}
```

Rules: an owner and a reason are required; expiry is at most 12 months;
expired exceptions fail CI (`governance/expired-exception`); exceptions live
in `registry/products/<id>.json` so they are reviewed by Monarch
maintainers, not hidden in app code. Inline `monarch-ignore-next-line`
comments are for single lines and also require a reason.
