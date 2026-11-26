# Native release and maintenance

How Monarch for Android and Monarch for iOS are versioned, released, upgraded
and kept current. The web packages keep their own rules
([versioning](versioning.md), [branching](branching.md),
[dependencies](dependencies.md)); this page states what differs for
`platforms/android` and `platforms/ios`. The architecture is in
[NATIVE-PLATFORMS](../architecture/NATIVE-PLATFORMS.md).

## Status

Read this first. The policy below is in force for work in the repository.
The release machinery is being set up; a row says "Decided" when the rule is
fixed but no release has used it yet.

| Part                                                   | Status                                                                                                                                    |
| ------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------- |
| Version number                                         | Decided: the native version equals the `@rookcom/monarch-react` version, in the `0.14.x` patch series (see [Versions](#versions))         |
| Publishing Android to a local Maven directory          | In place, for verification                                                                                                                |
| Publishing Android to Maven Central                    | Decided: the Central Portal, from `main` only, through the `maven-central` environment ([Publishing](#publishing))                        |
| Distributing iOS through Swift Package Manager by URL  | Decided: a root `Package.swift` generated from `platforms/ios/Package.swift`, resolved from plain `0.14.N` tags                           |
| Release tags                                           | Decided: plain `0.14.N` tags (see [Version tags](#version-tags)). No native release has been tagged yet                                   |
| `platforms/<platform>/CHANGELOG.md`                    | In place, written by hand under the [changelog standard](#changelog-standard)                                                             |
| Native jobs in the required CI checks                  | Decided: required on `uat` and `main` (see [Required CI checks](#required-ci-checks))                                                     |
| Automated dependency updates for Gradle and Swift      | In place. Dependabot opens weekly pull requests against `uat` for Gradle and Swift, as it does for npm and GitHub Actions                 |
| Release automation for native (build, verify, publish) | In place in `.github/workflows/release.yml` (see [Publishing](#publishing)). Until a run has published, verify by hand with the checklist |
| A native release of any kind                           | None yet. The first native release ships alongside the next `@rookcom/monarch-react` patch release                                        |

Nothing on this page should be read as a claim that a native artefact can be
installed from a public registry today.

## Versions

The native version is the `@rookcom/monarch-react` version. When
`@rookcom/monarch-react` releases `0.14.N`, Monarch for Android and Monarch for
iOS release `0.14.N` too, from the same commit. A version number therefore
means the same Monarch on the web, on Android and on iOS. A release in which
one platform has no change still carries the version, with a changelog entry
that says so.

| Platform | Where the version is written                                                                                                                                              |
| -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Source   | `version` in `packages/react/package.json`, the one place the number is chosen                                                                                            |
| Android  | `monarch` in `platforms/android/gradle/libs.versions.toml`, written from the source by the version-sync script and checked in CI; it is the Maven version                 |
| iOS      | `MonarchUI.version` in `platforms/ios/Sources/MonarchUI/MonarchUI.swift`, written the same way, and the plain `0.14.N` git tag, which is what Swift Package Manager reads |

Both Android modules (`monarch-tokens`, `monarch-compose`, group
`com.rookhq.monarch`) and both Swift products (`MonarchTokens`, `MonarchUI`)
share that one version.

What a version number promises:

- Monarch is before 1.0 and stays in the `0.14.x` patch series. Semantic
  versioning allows a change of any kind before 1.0; Monarch limits itself to
  these rules:
  - a patch carries fixes and additive API;
  - a patch removes an API only after its
    [deprecation window](native-api-stability.md#deprecation-policy) of 90 days
    and two patch releases has passed, and its changelog says so in its first
    line;
  - there is no `0.15.0`. Nothing is announced as "removed in 0.15.0".
- From 1.0 additive API ships in a minor and a breaking change only in a
  major.
- What counts as public API and as breaking is defined in
  [native API stability](native-api-stability.md).
- Raising a minimum OS version, the minimum Kotlin or Swift version an app
  needs, or the Compose BOM in a way that forces apps to upgrade is a breaking
  change ([platform evolution](native-platform-evolution.md#raising-a-minimum-os-version)),
  announced and given the same window.
- Token values are design, not API, exactly as on the web. A token rename or
  removal is API ([native tokens](native-tokens.md#deprecate-or-remove-a-token)).

A token change released in `@rookcom/monarch-tokens` reaches a platform when
the release that carries the regenerated file ships. The changelog entry
names the tokens package version it was generated from.

## Release tracks

A track describes what a build promises, not where its code lives. All work
still travels `feat|fix|docs|chore/…` → `uat` → `main`
([branching](branching.md)).

| Track       | What it is                              | Version form               | What it promises                                                                                                                             |
| ----------- | --------------------------------------- | -------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| Development | The head of `uat`, or a feature branch  | The next version, untagged | Nothing. API can change between commits. Not for product use                                                                                 |
| Preview     | A build of `uat` attached to its CI run | The next version, untagged | It builds and passes the full validation. Android AARs are workflow artefacts only; nothing is published to Maven Central from `uat`         |
| Stable      | A tagged release from `main`            | `0.14.N`                   | The API rules of the version policy, the tested range in [NATIVE-COMPATIBILITY](../architecture/NATIVE-COMPATIBILITY.md), and security fixes |

Rules:

- A product ships to its users on a stable version only. A preview build may
  be used on a product branch to test an upgrade.
- Fixes, including security fixes, ship in the next `0.14.N` patch. Older
  patch versions are not patched again: an app takes the newest patch.
- Artefacts of every published version stay available, so old builds keep
  resolving.
- Experimental API is not a track. It ships inside any release behind an
  opt-in ([native API stability](native-api-stability.md#experimental-api)).
- Until the first stable release, every native build is on the development
  track.

## Ownership

Roles, not people. One person may hold several; the repository's
`.github/CODEOWNERS` file decides who must review.

| Role                   | Responsible for                                                                                                         |
| ---------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Monarch maintainers    | Design authority: tokens, the shared vocabulary, governance documents, the decision to release or to break an API       |
| Android platform owner | `platforms/android`: the version catalog, API dumps, the Android changelog, the Android rows of the compatibility table |
| iOS platform owner     | `platforms/ios`: `Package.swift`, the API baseline, the iOS changelog, the iOS rows of the compatibility table          |
| Release manager        | The maintainer who performs one release: runs the checklist, verifies the artefacts, tags, writes the release notes     |
| Security contact       | Receives private reports under [SECURITY.md](../../SECURITY.md) and coordinates the fix across the affected platforms   |

A release needs the platform owner's approval and one maintainer's review of
the version, changelog and API diff. The person who wrote a breaking change
does not approve its release alone.

No per-platform line exists in `CODEOWNERS` yet; the default owner reviews
everything under `platforms/`. Add a line per platform when a platform owner
other than the default is named.

## Changelog standard

Each platform keeps `platforms/<platform>/CHANGELOG.md`. It is written by
hand, in the same pull request as the change. Changesets manage only the npm
packages and do not touch these files.

```markdown
# Changelog: Monarch for Android

## Unreleased

### Added

### Changed

### Deprecated

### Removed

### Fixed

### Security

## 0.14.7 - 2027-01-12

Tokens: generated from `@rookcom/monarch-tokens` 0.5.6.
Toolchain: see the 0.14.7 row of NATIVE-COMPATIBILITY.md.

### Added

- …
```

The versions and dates in that block illustrate the format. They are not
releases.

Rules:

- Newest first. `Unreleased` is always present and is renamed to the version
  and date at release.
- Use only the six headings shown; leave out the empty ones at release.
- Every entry is one sentence about what changed for an app, naming the public
  symbol. Internal refactors are not listed.
- A `Deprecated` entry names the replacement and the earliest removal date:
  "removed no earlier than <date> (90-day window)", where the date is 90 days
  after the release that deprecates. A `Removed` or breaking `Changed` entry
  links to its migration guide.
- Every change in behaviour that an app can observe (rendering, a default, an
  accessibility role, label or announcement, timing) carries a
  `MONARCH-COMPAT:` note in its entry, in one line: what changed and what an
  app does about it. "None needed" is a valid answer; leaving the note out is
  not.
- Each release states the tokens package version it was generated from and
  points to its compatibility row. A release that changes the tested range or
  a minimum OS version says so in its first line.
- Known issues that are open at release are listed under a `Known issues`
  heading in that release's entry (see [Known issues](#known-issues)).

## Version tags

- One plain semantic-version tag per release, `0.14.N`, with no prefix. It is
  the same tag for Android and iOS and matches the `@rookcom/monarch-react`
  version.
- Swift Package Manager reads that tag on this repository, through the root
  `Package.swift`. The root manifest is generated from
  `platforms/ios/Package.swift` (its target paths prefixed with
  `platforms/ios/`) and a drift check fails when the two disagree. Edit
  `platforms/ios/Package.swift` only.
- The Maven version of `com.rookhq.monarch:monarch-tokens` and
  `com.rookhq.monarch:monarch-compose` is the same `0.14.N`.
- Tags are annotated, point at the release commit on `main`, are created
  after CI has passed on it, and are never moved or deleted.
- The earlier proposal of `android-v…` and `ios-v…` prefixes is withdrawn. No
  tag of that form exists, and none is created.

## Release artefacts and their verification

| Platform | Artefact                                                                             | State                                                                                                                    |
| -------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------ |
| Android  | `com.rookhq.monarch:monarch-tokens` (JAR, sources, POM)                              | Published to Maven Central by the release workflow, signed, when the `maven-central` environment has its secrets (below) |
| Android  | `com.rookhq.monarch:monarch-compose` (AAR, sources, POM)                             | Same                                                                                                                     |
| iOS      | The Swift package source at the `0.14.N` tag (products `MonarchTokens`, `MonarchUI`) | Resolved by URL from the root `Package.swift` once the tag exists                                                        |
| Both     | The changelog entry, the compatibility row and, if needed, the migration guide       | Written by hand                                                                                                          |

### Publishing

- Android publishes through the Sonatype Central Portal from `main` only. The
  job runs in the GitHub environment `maven-central`, whose branch policy
  allows `main`.
- Artefacts are signed in memory during the job. No key is written to disk or
  to the repository.
- The job reads four environment secrets, named `MAVEN_CENTRAL_USERNAME`,
  `MAVEN_CENTRAL_PASSWORD`, `MAVEN_SIGNING_KEY` and
  `MAVEN_SIGNING_KEY_PASSWORD`. When they are absent, the job ends with a
  notice and publishes nothing. No workflow step prints them.
- Every one of the three publishing jobs below runs in `release.yml`, after
  the `stable` job, for the commit CI passed on:

  | Job name                 | When             | What it does                                                                                                                                                                                                        |
  | ------------------------ | ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
  | `Native / uat AARs`      | A push to `uat`  | Builds both libraries unsigned into `build/repo` and attaches them to the run. Nothing is published                                                                                                                 |
  | `Native / Maven Central` | A push to `main` | Skips with a notice when a secret is absent, when changesets are pending, or when the version is already on Maven Central. Otherwise waits for the `Native` run of the same commit, publishes and attests the files |
  | `Native / SwiftPM tag`   | A push to `main` | When `@rookcom/monarch-react@0.14.N` is tagged and `0.14.N` is not, waits for the `Native` run of the same commit and creates the plain `0.14.N` tag                                                                |

- The iOS release is the `0.14.N` tag; nothing is uploaded. The job never
  moves an existing tag.

### Required CI checks

The native workflow runs on every pull request and every push to `uat` and
`main`. Path detection happens inside the workflow: a job with nothing to
check ends early and passes, so a required check never waits for a job that
was not started. The job names are fixed, because the branch rulesets of
`uat` and `main` require them by name:

| Job name           | What it runs                                                                                                                                                             |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `Native / tokens`  | `npm run native:tokens:check`, `native:brand:check`, `native:package:check` (the root `Package.swift` drift check), `native:version:check` and `native:components:check` |
| `Native / android` | `./gradlew verifyMonarch` in `platforms/android`                                                                                                                         |
| `Native / ios`     | The root `Package.swift` loads, then `Scripts/verify.sh` in `platforms/ios`, on macOS (the job runs on Linux and ends early when no iOS input changed)                   |

A fourth job, `Native / changes`, only decides which of the three has work
to do. It is not a required check. A platform job fails, and so blocks the
merge, when `Native / changes` itself fails.

Renaming one of these jobs is a change to the rulesets as well, and is made
in the same pull request.

### After the single native pull request merges

The owner does these once, in the repository settings. None of them is in
the repository, because the repository is public and a secret value never
belongs in it.

1. Set the four deploy secrets of the docs site again, by name, in the
   `production` environment (limited to `main`): `DEPLOY_HOST`,
   `DEPLOY_USER`, `DEPLOY_SSH_KEY` and `DEPLOY_KNOWN_HOSTS`, and the
   repository variable `DEPLOY_ENABLED`
   ([deploy/docs-site](../../deploy/docs-site/README.md)). A new repository
   starts with none of them.
2. Point npm trusted publishing of `@rookcom/monarch-tokens`,
   `@rookcom/monarch-react` and `@rookcom/monarch-conformance` at the new
   repository and the workflow file `release.yml`, on npmjs.com. Until then the
   `stable` job cannot publish.
3. Create the `maven-central` environment, limited to `main`, with required
   reviewers if wanted. Add the four secrets only when Maven Central
   publishing is wanted: `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`,
   `MAVEN_SIGNING_KEY` and `MAVEN_SIGNING_KEY_PASSWORD`. Before that, verify
   the `com.rookhq` namespace in the Central Portal with the DNS TXT record the
   portal shows, and publish the public half of the signing key to a key
   server. Without the secrets, the Maven job ends with a notice and the
   release is complete for npm and SwiftPM.
4. Apply the rulesets on `uat` and `main` with the three required checks
   above, by their exact names, next to the checks listed in
   [branching](branching.md).
5. Merge a release: `npm run version-packages`, review, push. Check that
   the npm release, the `0.14.N` tag and, if enabled, the Maven artefacts
   carry the same version, and that `/ios/api/` and `/android/api/` on the
   docs site show it.

Before an artefact is called a release, the release manager verifies, from a
clean checkout of the commit to be tagged:

1. The generated token files match the token source
   (`npm run native:tokens:check`).
2. The platform's full check passes: build with warnings as errors, static
   analysis, unit and UI tests, accessibility tests, and the sample app build.
   A successful compilation alone does not approve a release.
3. The public API matches its committed record, and any difference is the one
   the changelog describes
   ([native API stability](native-api-stability.md#compatibility-checks)).
4. The artefact is consumed the way an app would consume it. Android: publish
   to the local repository and build the sample app, or a scratch project,
   against those coordinates rather than against the project modules. iOS:
   resolve the package from a second, empty package or app by path and build
   it.
5. The artefact contains what it should: the POM names the Apache License 2.0 and
   only the intended dependencies; the AAR and JAR have no test code or
   sample resources; the Swift package builds with no dependency outside the
   Apple SDK.
6. The version in the build, the changelog heading and the tag are the same
   string.
7. The compatibility table has a row for the release with the toolchain that
   actually built it.

Once a release has been published to Maven Central, the list gains
signature and checksum verification of the uploaded files and a
resolve-from-the-registry build. Do not describe those steps as done until a
release has run them.

The exact commands for each step are in the
[upgrade playbook](../../MONARCH_UPGRADE_PLAYBOOK.md).

## Known issues

- A defect in a released version is recorded as a GitHub issue, and listed
  under `Known issues` in the changelog entry of the newest affected release,
  with the issue link, the affected versions
  and a workaround if one exists.
- The entry is removed from `Unreleased` when the fix merges and appears under
  `Fixed` in the release that carries it. Entries in already published
  release notes are not rewritten, only marked with the fixing version.
- Limits that are by design, not defects, stay in
  [Known limitations](../architecture/NATIVE-PLATFORMS.md#known-limitations).
- A known issue caused by a platform bug names the OS or toolchain version and
  links to the workaround's record
  ([platform evolution](native-platform-evolution.md#compatibility-workarounds)).
- A security defect is never recorded as a public known issue before its fix
  is released.

## Security response

[SECURITY.md](../../SECURITY.md) is the policy: report privately through
GitHub's private vulnerability reporting, never in a public issue. For native,
add to the report the platform, the Monarch version, and the OS and toolchain
versions.

What differs for native:

- **Supported versions.** `SECURITY.md` lists the npm packages. No native
  version is released, so none is supported yet. From the first stable
  release, security fixes ship in the next `0.14.N` patch, as they do for the
  npm packages. `SECURITY.md` must be updated in the pull request that makes
  the first stable release.
- **Attack surface.** The native libraries are UI code with no networking
  code and no telemetry. The realistic risks are supply chain (a build
  plugin or dependency, the Gradle wrapper, a compromised release) and
  defects in how untrusted text or images passed by the app are displayed.
- **Supply-chain controls in place.** Versions are written once, in the
  version catalog; the Gradle wrapper pins the distribution checksum;
  repositories are declared centrally and project-level repositories fail the
  build; the iOS package has no third-party dependency.
- **Not yet set up.** Dependency verification metadata for Gradle and
  automated vulnerability scanning of native dependencies. Artefacts are
  signed once Maven Central publishing has run.

Process for a confirmed vulnerability:

1. The security contact acknowledges within the time `SECURITY.md` states and
   opens a private advisory.
2. The platform owner confirms the affected versions on each platform. A
   defect in a shared token or rule is checked on web, Android and iOS.
3. The fix is prepared on a private branch, with a test, and released as a
   patch. A security patch does not carry unrelated changes.
4. The changelog `Security` entry and the advisory are published together,
   after the fixed artefacts are available.
5. A vulnerable dependency is upgraded through the
   [upgrade process](#upgrade-process) on a shortened timetable; the review
   step is not skipped.

## Upgrade process

How a new version of Kotlin, Swift, Gradle, the Android Gradle Plugin, the
Compose BOM, Xcode, an SDK or any other dependency enters Monarch. The step by
step commands are in the
[upgrade playbook](../../MONARCH_UPGRADE_PLAYBOOK.md); this is the rule.

1. **Monitor.** The platform owner follows the stable release channels of the
   toolchain and of every dependency in the version catalog and
   `Package.swift`. The [calendar](#maintenance-calendar) sets the minimum
   rhythm. Dependabot opens weekly update pull requests for Gradle and Swift
   (majors excluded); they are a starting point, not a replacement for
   reading the release notes.
2. **Identify deprecations and removals.** Read the release notes of the new
   version. Build with it and collect every new deprecation warning. Each one
   is fixed or recorded with its removal deadline; none is suppressed.
3. **Identify vulnerabilities and unsupported versions.** Check advisories for
   the dependencies in use, and check that each is still maintained and still
   supported by its vendor.
4. **Evaluate new capabilities** under the
   [adoption procedure](native-platform-evolution.md#adopting-a-new-platform-api).
   An upgrade pull request does not also adopt new APIs.
5. **Upgrade on a branch.** One `chore/…` branch and one pull request into
   `uat` per upgrade. Versions that must move together move together (Kotlin
   with the Compose compiler plugin, the Android Gradle Plugin with Gradle and
   the JDK, Xcode with the Swift toolchain and SDK); unrelated upgrades stay
   in separate pull requests so a failure can be traced and reverted.
6. **Run the full validation**, not only the build: unit, UI and
   accessibility tests, static analysis, the public API check, the token
   check, the sample app build and the artefact verification above.
7. **Write the upgrade report** in the pull request description: what moved
   from which version to which, the constraints checked, new deprecations
   found and what was done about each, the API diff (expected: none), test
   results as counts, any change in behaviour or rendering, and how to roll
   back.
8. **Review.** The platform owner and one maintainer review the report and
   the diff. Nothing merges on a green build alone.
9. **Release** through the normal release steps, with the compatibility table
   updated in the same pull request.
10. **Document** breaking changes and migration steps
    ([native API stability](native-api-stability.md#migration-guide-template)).

Rules:

- **No automatic merge of a major version**, on any track. A major upgrade of
  a toolchain or dependency is always a planned, reviewed pull request. This
  matches the web rule ([dependencies](dependencies.md#updating-dependencies)).
- Minor and patch updates are grouped and proposed by Dependabot. They still
  need review and the full validation.
- **Stable inputs for stable outputs.** A stable Monarch release is built only
  with stable versions of the toolchain and of every dependency.
- **Preview, beta, release-candidate and experimental inputs** (an Android
  preview SDK, an Xcode beta, an alpha AndroidX library, a compiler feature
  behind a flag) are used only on a branch that is not released, or in a
  Monarch preview release whose changelog names them. They never appear in
  the version catalog or manifest on `main`.
- An API that the vendor marks experimental may be used inside an
  implementation only when isolated and recorded
  ([platform evolution](native-platform-evolution.md#rules-that-prevent-upgrade-lock-in)).
  The public API check uses the stable binary-compatibility-validator plugin
  (`org.jetbrains.kotlinx.binary-compatibility-validator`), not the Kotlin
  Gradle plugin's experimental ABI validation.
- A failed upgrade is reverted, not patched forward under time pressure. The
  report records why it failed.

## Maintenance calendar

The minimum rhythm. Vendor dates move from year to year; when they do, the
task moves with the event, not with the month.

Routine work:

| When                                | Task                                                                                                                                                                                                                                                            |
| ----------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Every month                         | Dependency review on both platforms: new stable versions (Compose BOM and AndroidX; Xcode and Swift point releases), advisories, and deprecation warnings in the build. Open upgrade pull requests                                                              |
| Every quarter                       | Platform compatibility check: build and test on the oldest and newest supported OS and toolchain. Review Kotlin, Android Gradle Plugin and Gradle together and plan their upgrade. Review open known issues, and every workaround against its removal condition |
| Once a year, after both OS releases | Review the minimum OS versions, the supported Xcode range, the roles and this page                                                                                                                                                                              |

The annual OS cycle. In recent years Android has published previews around
the turn of the year and its stable release around the middle of the year;
Apple publishes its first beta at its developer conference in June and
releases in September.

| Stage                 | Android (new Android release)                                                                                                                                                                                           | Apple (new iOS and Xcode release)                                                                                                                           |
| --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| First preview or beta | Read the behaviour changes and API differences. On a branch, build the sample app against the preview SDK and run the tests on a preview system image. List behaviour changes and new deprecations. No change to `main` | Read the release notes. On a branch, build with the Xcode beta and run the tests on the beta simulator. List deprecations and new APIs. No change to `main` |
| Later betas           | Repeat on each beta. At platform stability, when the APIs are final, decide which new APIs to evaluate and prepare the compile SDK upgrade on the branch                                                                | Repeat on each beta that changes API. Fix what the new SDK breaks behind availability checks, and keep the branch building with the stable Xcode as well    |
| Release candidate     | Run the full validation. The upgrade pull request and the draft compatibility row are reviewed, and wait for the stable SDK                                                                                             | Run the full validation with the Xcode release candidate. The upgrade pull request and the draft compatibility row are reviewed                             |
| Public release        | Raise the compile SDK. Raise the target SDK once the behaviour changes it switches on are tested. Add the new OS to the tested range and release                                                                        | Add the new Xcode, SDK and OS to the tested range and release                                                                                               |
| After the release     | Note the Google Play target API deadline for apps, and make sure a Monarch release that supports that API level exists well before it                                                                                   | Note the App Store minimum Xcode and SDK requirement for apps, and make sure a Monarch release built with that SDK exists well before it                    |

Monarch states the support status of a new OS or toolchain once it has been
tested: `tested`, `known issues` (with links) or `not supported`. Until then
the status is `not evaluated`. Monarch does not promise that a version built
today works on an OS released later.

## Related

- [Native API stability](native-api-stability.md)
- [Native platform evolution](native-platform-evolution.md)
- [Native tokens](native-tokens.md)
- [Contributing to the native platforms](../guides/native-contributing.md)
- [NATIVE-COMPATIBILITY](../architecture/NATIVE-COMPATIBILITY.md)
- [Upgrade playbook](../../MONARCH_UPGRADE_PLAYBOOK.md)
