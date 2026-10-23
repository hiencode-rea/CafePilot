# API test cases

## Automated contract tests

Run from the repository root on Windows with a JDK installed:

```powershell
.\QuanLiQuanCafeAPI\run-tests.ps1
```

The API module has no Maven/Gradle test setup, so `run-tests.ps1` compiles the API and a small JDK-only test harness against the already-bundled Gson/JDBC libraries. The tests do not connect to SQL Server or make network requests.

Automated cases:

| Case | Expected |
|---|---|
| Query string absent | Query helper returns `null` |
| Existing query parameter | Returns its value |
| UTF-8 URL-encoded query value | Decodes to the expected Unicode text |
| Requested key missing | Returns `null` |
| Duplicate query key | Returns the first matching value |
| `GET /api/dangnhap` | JSON response with HTTP 405 |
| `GET /api/donhang` | JSON response with HTTP 405 |
| `GET /api/chamcong/vao` | JSON response with HTTP 405 |
| `POST /api/xulymon` collection path | JSON response with HTTP 404 |

## Important security/integration cases — not yet automated

These cases require the API authentication/authorization boundary and an isolated test database. They are intentionally marked **BLOCKED / currently expected to expose missing controls**, not passed:

| Case | Expected secure behavior | Current status |
|---|---|---|
| Anonymous request to a protected order/attendance/kitchen endpoint | HTTP 401 | BLOCKED: server-side API authentication is not implemented |
| Authenticated employee with wrong role mutates a privileged resource | HTTP 403 | BLOCKED: server-side role authorization is not implemented |
| Employee A supplies Employee B's username/ID | Denied; identity comes from the authenticated principal | BLOCKED: handlers accept client-supplied identity/IDs |
| Client changes `maBan`, `maDH`, `maSP`, or `maCC` to another record | Ownership/role check denies access | BLOCKED: requires server-side object authorization |
| Invalid login credentials | Generic authentication failure; no identity details returned | Not covered by this DB-free suite |
| Login with each allowed and disallowed role | Only explicitly allowed roles receive a session/token | Not covered by this DB-free suite |
| SQL injection strings in login and query inputs | Treated as data; no query structure changes | Not integration-tested; inspected login query uses parameters |
| Missing, malformed, wrong-type, or oversized JSON body | HTTP 400/413 with safe error body; no stack/SQL details | Not covered; handlers need consistent validation/body limits |
| SQL outage or constraint error | Safe generic HTTP 5xx; internal details are not returned | Not covered; current handlers may expose database exception messages |
| Attendance checkout for another employee's record | Denied by ownership check | BLOCKED: requires authenticated identity and isolated DB |
| Payment confirmation by mobile or non-cashier role | Denied server-side; UI restrictions alone are insufficient | BLOCKED: authorization integration test needed |

Do not point integration tests at a production or personal database. Add a dedicated disposable test database and non-production accounts before implementing the blocked integration suite.