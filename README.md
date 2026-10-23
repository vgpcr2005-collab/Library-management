# Alliance University — Smart Library Management System

A responsive staff website for Alliance University library services. The project uses Java 17, JSP, Jakarta Servlets, JDBC, MySQL, and Apache Tomcat 10.1, with an embedded H2 database for local development. It includes a protected staff dashboard, collection and student management, issue/return transactions, overdue fines, reports/CSV export, configuration, and academic project pages. Student self-registration is disabled; an administrator provisions librarian accounts.

> **Production note:** Production mode requires MySQL settings, HTTPS, and a configured staff account; it will not fall back to embedded H2. The local development launcher restricts requests to this computer.

## Folder structure

```text
smart-library/
├── pom.xml
├── README.md
├── docs/
│   ├── PRESENTATION_OUTLINE.md
│   └── PROJECT_REPORT_OUTLINE.md
└── src/
    ├── main/
    │   ├── java/edu/alliance/library/
    │   │   ├── dao/          # PreparedStatement-based JDBC access
    │   │   ├── filter/       # Local-only access restriction and CSRF
    │   │   ├── model/        # User, Student, Librarian, LibraryItem, Book
    │   │   ├── service/      # Library application logic
    │   │   ├── servlet/      # HTTP controllers
    │   │   └── util/         # JDBC connection and input validation
    │   ├── resources/db/
    │   │   ├── schema.sql
    │   │   └── sample-data.sql
    │   └── webapp/
    │       ├── WEB-INF/views/ # JSP pages and shared fragments
    │       ├── assets/css/
    │       └── assets/js/
    └── test/java/edu/alliance/library/
```

## Requirements

- JDK 17 (or newer; Maven compiles to Java 17 bytecode)
- Apache Maven 3.9+
- Apache Tomcat 10.1 (Jakarta Servlet 6 / JSP 3), or Docker for Azure deployment
- IDE: IntelliJ IDEA, Eclipse Enterprise Java, or VS Code with Extension Pack for Java

## Local database setup

For local development, the app creates its H2 schema and starter library/student records on first launch. The file-backed database is stored at `%USERPROFILE%\.alliance-smart-library\library.mv.db` and remains available after Tomcat restarts.

The first launch includes sample books and two sample student records for demonstration. Additions, edits, loans, returns, and settings are persisted in the local database. Stop Tomcat before backing up or moving the database file.

Production uses Azure Database for MySQL Flexible Server. The app requires all three database environment variables and will refuse to start database operations in `APP_ENV=production` if they are incomplete. Apply `src/main/resources/db/schema.sql` before deploying the application.

## Build and run locally

1. In a terminal at the project root, run:

   ```powershell
   mvn clean test package
   ```

   This runs the unit tests and creates `target/smart-library.war`.

2. Copy `target/smart-library.war` into Tomcat's `webapps` directory.
3. For the bundled Windows setup, run this from PowerShell. The process-only policy bypass does not change the computer's PowerShell execution policy:

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\run-library-local.ps1"
   ```

   The script asks for the initial administrator email and a hidden password (minimum 14 characters), stops Tomcat, deploys the WAR, and starts the locally restricted website. Existing administrator passwords are never reset by this bootstrap prompt. Keep the PowerShell window open while using the site.
4. Alternatively, deploy the WAR to Tomcat from your IDE with no `APP_ENV=production` value; the app uses the embedded local database. Set `LIBRARY_BOOTSTRAP_ADMIN_EMAIL` and `LIBRARY_BOOTSTRAP_ADMIN_PASSWORD` for the first launch to provision an administrator.
5. Open `http://localhost:8080/smart-library/` and sign in with your staff account.

### IDE notes

- **IntelliJ IDEA:** Open `pom.xml` as a Maven project, select a JDK 17 project SDK, add a local Tomcat 10.1 server run configuration, and deploy the `smart-library:war exploded` artifact.
- **Eclipse:** Import as **Existing Maven Projects**, set the project JRE to JDK 17, install/configure Tomcat 10.1 in the Servers view, and add the project to that server.
- **VS Code:** Open the folder, install Extension Pack for Java and Community Server Connectors, let Maven import dependencies, select JDK 17, then deploy the WAR to a configured Tomcat 10.1 server.

## Authentication and access control

- Only enabled `ADMIN` and `LIBRARIAN` accounts can sign in. Student accounts are library member records, not login accounts; public registration is disabled.
- Passwords are salted PBKDF2-HMAC-SHA256 hashes. Login rotates the session ID, sessions use HTTP-only and SameSite=Lax cookies, and repeated attempts are throttled.
- All state-changing forms use CSRF tokens. Protected pages revalidate the staff account status; disabling a librarian ends access on the next request.
- Administrators can create/disable librarian accounts and manage settings. Staff can manage books, students, issues, returns, and reports. Signed-in staff can change their passwords.
- The local launcher enforces loopback-only access. Azure production mode requires HTTPS and MySQL and disables the embedded-database fallback.
- There is no public student registration or forgot-password workflow. A configured administrator provisions accounts; an authenticated user can change their own password.

## Features

- Database-calculated dashboard statistics, six-month activity, categories, recently issued books, and most-borrowed leaderboard.
- Search by title/author/ISBN/category, category and availability filters, visual stock status, and low-stock hints.
- Student directory search and validated profile updates; student records are managed by authorized staff.
- Issue transactions lock the selected inventory row, enforce reference-only rules and configured student limits, and commit availability and loan records atomically.
- Return transactions prevent duplicate returns, restore inventory, and calculate the fine from configured daily rate and overdue days in one transaction.
- Available/issued/overdue/history/popularity/monthly reports, with CSV download.
- Configurable loan period, daily fine, and borrowing limit.
- Responsive, navy/white/gold interface; charts use the Maven-packaged Chart.js WebJar.

## Database tables

`users`, `students`, `books`, `book_issues`, `book_returns`, and `library_settings` are created by the application on first local use or by `schema.sql` in MySQL. Foreign keys, uniqueness rules, indexes, and check constraints protect core records. Issue and return records identify the signed-in staff member, and the workflow uses explicit JDBC transactions and row locks.

## Java concepts mapping

| Concept | Implementation | Demonstration |
|---|---|---|
| Inheritance | `model/User`, `model/Student`, `model/Librarian` | Shared user identity and role-specific models |
| Polymorphism | `model/LibraryItem`, `model/Book`, `model/ReferenceBook` | `BookDao` creates the runtime subtype; catalogue invokes overridden `getDetails()` |
| Arrays | `service/LibraryService.SEARCH_FIELDS` | Fixed labels for supported catalogue search fields; inventory is still database-backed |
| Strings | `util/InputValidator` | Trim, normalize case, validate email/ISBN inputs |
| Encapsulation | Domain model private fields and validated constructors/business methods | Copy count invariants in `model/Book`; required user and item details |

The in-app **Java concepts** page explains these examples. **About Project** and **Architecture** describe the system flow.

## Tests and acceptance checklist

Run automated tests with `mvn clean verify`. Tests cover domain validation, runtime polymorphism, password hashing, staff authentication/disable/password change, embedded database initialization, and issue/return transactions.

| Scenario | Expected result |
|---|---|
| Open a protected page while signed out | Redirects to staff sign-in |
| Register as a student | Public registration is unavailable; staff manage student member records |
| Sign in as student/disabled user or use wrong password | Generic rejection; protected pages remain inaccessible |
| Sign in as an administrator | Session ID rotates; admin-only staff/settings screens are available |
| Sign out | Session invalidated and sign-in page displayed |
| Repeated invalid sign-in | Attempts throttled temporarily |
| Issue/return a book | The authenticated staff account is recorded; inventory updates atomically |
| Add/edit book with duplicate ISBN | Database uniqueness prevents duplicate; useful error is shown |
| Issue available book | Availability decreases and issue/due dates persist together |
| Issue unavailable/reference book or exceed limit | Request rejected without inventory mutation |
| Return an overdue loan | Return and inventory update once; fine is daily rate × overdue days |
| Repeat a return | Rejected; no duplicate return or copy increment |
| CSV export | Filtered rows download with escaped cells |
| Database constraint | Invalid quantities, duplicate identifiers, and orphan records are rejected |

### Screenshots

- `[Screenshot placeholder — add docs/screenshots/local-dashboard.png]`
- `[Screenshot placeholder — add docs/screenshots/catalogue.png]`
- `[Screenshot placeholder — add docs/screenshots/circulation.png]`
- `[Screenshot placeholder — add docs/screenshots/mobile.png]`

See [docs/PROJECT_REPORT_OUTLINE.md](docs/PROJECT_REPORT_OUTLINE.md) and [docs/PRESENTATION_OUTLINE.md](docs/PRESENTATION_OUTLINE.md) for the academic report and demonstration structure.

## Azure deployment

See [deployment/azure/README.md](deployment/azure/README.md) for the Azure App Service, managed MySQL, secret configuration, schema setup, and deployment workflow. This repository does not deploy cloud resources automatically; deployment requires your Azure subscription and credentials in Azure/GitHub, never in chat or source control.
