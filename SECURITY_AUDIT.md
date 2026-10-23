# Security Release Audit

- **Repository:** `hiencode-rea/CafePilot` (private)
- **Commit audited:** `5a8dc1fcbf28838f5c6963ea99c06760e91dcda6`
- **Audit date:** 2026-10-23
- **Scope:** Current `main` tree and reachable history (2 commits); Java HTTP API, Android and JavaFX clients, SQL Server scripts, Android Gradle dependency declarations, bundled JAR inventory, root documentation and ignore rules.
- **Tools:** Git/GitHub CLI; PowerShell source and history inspection; code review. Gitleaks, OSV-Scanner, Semgrep, OWASP Dependency-Check, and Trivy were not installed. No external code-upload scanner was used.
- **Overall result: FAIL for public application release.** The repository is private. The explicitly authorized push in this task is limited to this audit and the skill document; no application source changes are included.

## Summary

- Critical: 0 confirmed
- High: 3
- Medium: 2
- Low: 0 confirmed
- Informational: 2
- Unresolved: 3 (dependency vulnerability scan, database credential/runtime configuration validation, deployment exposure)
- Checks not completed: dedicated secret/dependency/SAST scanners; running API against isolated SQL test database; automated anonymous/role/IDOR tests; verification of any deployed network boundary.

## Findings

### SEC-001 — Unauthenticated API permits sensitive order and attendance actions

- **Severity/confidence:** HIGH / High
- **Location:** `QuanLiQuanCafeAPI/src/ApiServer.java:10-26`; `QuanLiQuanCafeAPI/src/ChiTietDonHangHandler.java:25-52, 174-...`; `QuanLiQuanCafeAPI/src/ChamCongHandler.java:17-55`; `QuanLiQuanCafeAPI/src/XuLyMonHandler.java:16-29, 78-101`
- **Evidence:** The server registers business handlers directly. The reviewed handler entry points dispatch by HTTP method/path and query/body data; no server-side authenticated principal or authorization check is applied before database mutations. The mobile-specific payment endpoint restriction is a handler response, not a general authenticated identity/role boundary.
- **Scenario/impact:** A network-reachable caller can invoke order, order-detail, kitchen-status, or attendance operations without first presenting an authenticated session/token. Depending on endpoint inputs and database state, this enables unauthorized business-state changes and attendance tampering.
- **Remediation:** Put authentication and server-side role/ownership authorization at a common API boundary; derive employee identity from a validated session/token rather than request fields; enforce least privilege per operation and object; add negative authorization tests.
- **Verification:** Not remediated. Local anonymous/role/ownership tests were not run because no isolated API/database test environment was available.

### SEC-002 — Request-supplied identity enables impersonation / object-level authorization failures

- **Severity/confidence:** HIGH / High
- **Location:** `QuanLiQuanCafeAPI/src/DonHangHandler.java:26-46`; `QuanLiQuanCafeAPI/src/CaLamViecHandler.java:25-43`; `QuanLiQuanCafeAPI/src/ChamCongChuaRaHandler.java:25-44`; `QuanLiQuanCafeAPI/src/ChiTietDonHangHandler.java` (request body/query-derived employee, table, order, and item identifiers across handlers)
- **Evidence:** Employee identity is accepted as `tenDangNhap` in a request and mapped directly to a database employee. Other state-changing operations identify tables/orders/items using client-supplied numeric IDs. No authenticated principal or ownership check was observed at these entry points.
- **Scenario/impact:** A caller can claim another employee's username or change identifiers to query or modify a different employee's attendance/order/table, subject to endpoint logic and database state.
- **Remediation:** Ignore client-supplied identity for authorization; bind each request to a validated principal; verify ownership/role for every object and operation in SQL/service logic; test cross-user and cross-role requests.
- **Verification:** Not remediated; integration tests require an isolated test database.

### SEC-003 — SQL scripts establish/reset predictable account passwords and store credentials as plaintext

- **Severity/confidence:** HIGH / High
- **Location:** `SQL/ThemTaiKhoanBep.sql:5-8, 22-43`; `SQL/SuaTaiKhoanPhaChe.sql:11-24, 40-41`; `SQL/QuanLyCafe_Full.sql:25-33, 216-220`; `QuanLiQuanCafeAPI/src/DangNhapHandler.java:30-39`; `QuanLiQuanCafeDesktop/src/LoginApp.java:129-136`
- **Evidence:** Tracked setup scripts document and assign fixed demonstration credentials, reset an account password to a fixed value, and the schema stores a `MatKhau` column that login handlers compare directly. Credential values are intentionally omitted from this report.
- **Scenario/impact:** Anyone running the scripts inherits known credentials; anyone who can read the database or its backups can recover passwords, potentially enabling account takeover if reused.
- **Remediation:** Remove fixed credentials from deployable scripts; provision unique secrets out-of-band with forced first-login change; store salted password hashes using a suitable adaptive password-hashing algorithm; migrate existing accounts safely and remove password-returning SQL output.
- **Verification:** Not remediated. The scripts were inspected but not executed against a database.

### SEC-004 — Cleartext HTTP and permissive Android cleartext traffic

- **Severity/confidence:** MEDIUM / High
- **Location:** `QuanLiQuanCafeAPI/src/ApiServer.java:9-10`; `QuanLiQuanCafeAndroid/app/src/main/AndroidManifest.xml:7-10`; `QuanLiQuanCafeAndroid/app/src/main/java/com/example/nlcsn2/ApiClient.java:13`
- **Evidence:** API uses the JDK `HttpServer` without TLS in the reviewed entry point. Android manifest explicitly enables cleartext traffic; client base URL uses `http://` and a machine-specific address.
- **Scenario/impact:** If traffic crosses an untrusted network, login credentials and business data can be intercepted or modified. The literal address appears development-specific; actual deployment exposure is unknown.
- **Remediation:** Use HTTPS with certificate validation, remove global cleartext permission, configure environment-specific API endpoints without developer LAN addresses, and prevent production startup without TLS.
- **Verification:** Static inspection only; deployed topology was not available.

### SEC-005 — SQL Server certificate validation is disabled

- **Severity/confidence:** MEDIUM / High
- **Location:** `QuanLiQuanCafeAPI/src/DBConnection.java:15-22`; `QuanLiQuanCafeDesktop/src/DBConnection.java:17-24`
- **Evidence:** JDBC URL enables encryption while setting `trustServerCertificate=true`.
- **Scenario/impact:** TLS encryption does not authenticate the SQL Server certificate, allowing a network attacker capable of interception to impersonate the database endpoint.
- **Remediation:** Configure a trusted server certificate/CA and hostname validation; do not bypass certificate verification in production.
- **Verification:** Static inspection only; certificate deployment was not available.

### SEC-006 — SQL schema pins database files to a developer-specific machine path

- **Severity/confidence:** INFO / High
- **Location:** `SQL/QuanLyCafe_Full.sql:4-12`
- **Evidence:** Database creation script specifies a local SQL Server Express filesystem path.
- **Impact:** Portability and deployment reliability issue; it may disclose a developer environment path. No direct exploit was established.
- **Remediation:** Use deployment parameters or logical/default data paths rather than a machine-specific absolute path.
- **Verification:** Confirmed by source inspection.

### SEC-007 — Bundled third-party JARs lack a verified vulnerability inventory

- **Severity/confidence:** REVIEW / High
- **Location:** `QuanLiQuanCafeAPI/lib/`; `QuanLiQuanCafeDesktop/lib/`
- **Evidence:** Repository contains bundled Gson, Microsoft JDBC, and PDFBox JARs. Dedicated dependency scanners were unavailable; the Android version catalog was inspected, but no complete vulnerability assessment of bundled JAR contents/transitives was performed.
- **Impact:** Known vulnerable or unsupported components cannot be ruled out from this evidence.
- **Remediation:** Record exact component versions/SBOM, scan with a current local dependency database, update affected components, and establish repeatable dependency scanning.
- **Verification:** Not completed.

## Required remediation

1. **Before any public release:** implement API authentication plus per-route role and object-ownership authorization; do not trust usernames/IDs from request data.
2. Replace fixed/demo passwords and plaintext password comparison/storage; handle any reused or operational credentials through their owner and rotate them if exposure or reuse is confirmed.
3. Deploy API and database traffic with certificate-validated TLS; remove production cleartext settings and environment-specific endpoint literals.
4. Run authorization, anonymous-access, and IDOR tests against an isolated test database; run current secret, SAST, and dependency scanners.
5. Review seed data and all commits for personal/production data and validate database least-privilege grants.

## Checks completed

- Confirmed remote `hiencode-rea/CafePilot` is private and cloned its `main` branch to an isolated audit directory.
- Inspected the current tree and the two reachable commits; reviewed `.gitignore`, root README, API entry point/handlers, Android manifest/client/login/build declarations, desktop login/JDBC configuration, SQL schema and account scripts.
- Reviewed Java SQL construction in inspected handlers; parameterized statements are common, and no SQL injection was confirmed in the reviewed paths. This is not proof that every query is safe.
- Searched tracked source/history for secret-related patterns and sensitive path names. This was a manual pattern review, not a dedicated secret scanner; no raw credential values are reproduced here.
- Confirmed there are no tracked `.github/workflows` files in the inspected tree.
- No project build/test was run: API/desktop source has no root build descriptor and build scripts were not executed before completing the security assessment. No SQL scripts or production endpoints were run or tested.

## Limitations

- Repository history available to this clone contains only two commits; prior external copies, deleted remote refs, forks, backups, and other repositories were not examined.
- Gitleaks, OSV-Scanner, Semgrep, OWASP Dependency-Check, and Trivy were unavailable. No vulnerability database timestamp is applicable; transitive/bundled JAR vulnerabilities remain unresolved.
- No SQL Server test instance, deployed API, network topology, CI configuration, or production database grants were available. Anonymous/role/IDOR runtime tests and certificate validation could not be performed.
- Git ignore rules were reviewed, but no dedicated content scanner was available for all binary files. Bundled JARs were inventoried, not decompiled or exhaustively inspected.
- Findings are based on the commit above; deployment configuration can change exposure materially.

## Final decision

**FAIL for public release of the application** because confirmed high-severity authentication/authorization and credential-management weaknesses remain unresolved. Keeping the repository private does not remediate them. This task only adds the audit skill and this report to the already-private repository; it does not change application code or claim the application is secure. Do not change repository visibility or publish an application release until the blockers are remediated and the relevant tests/scanners are rerun.