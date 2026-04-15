RELEASE CHECKLIST

This document describes the steps to create a release for edusync-backend-dev.

1. Update CHANGELOG.md under the "Unreleased" section with the changes for this release.
2. Bump the version in `pom.xml` (e.g., 0.0.1-SNAPSHOT -> 0.0.2).
3. Run full CI locally or in CI:

   ```bash
   ./mvnw clean verify
   ./mvnw -Pintegration-test verify
   ```

4. Run database migrations against a staging environment and verify application health.
5. Tag the release:

   ```bash
   git tag -a v0.0.2 -m "Release v0.0.2"
   git push origin --tags
   ```

6. Create a GitHub release using the tag and include the changelog entry from step 1.

Release PR template

- [ ] Changelog updated
- [ ] All tests passing
- [ ] Integration smoke tests signed off
- [ ] Tag created and pushed

Example release notes format

### v0.0.2 - YYYY-MM-DD

#### Added
- 

#### Changed
- 

#### Fixed
- 


