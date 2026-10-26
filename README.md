# Alliance University — Smart Library Management System

A responsive library website for Alliance University. The project uses Java 17, JSP, Jakarta Servlets, JDBC, MySQL, and Apache Tomcat 10.1, with an embedded H2 database for local development. The landing page opens the sign-in screen; students can register, and staff sign-in is provisioned privately.

> **Security note:** Student registration checks email syntax but does not verify university identity. Provision staff accounts privately and require institutional identity verification/SSO, operational monitoring, and security review before using real student data in production. Facial recognition and biometric data collection are not included.
>
> **Local note:** Local development remains restricted to this computer. **Deployed note:** Production mode requires MySQL settings and HTTPS and will not fall back to embedded H2.

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
- Apache Tomcat 10.1 (Jakarta Servlet 6 / JSP 3), or Docker for deployment
- IDE: IntelliJ IDEA, Eclipse Enterprise Java, or VS Code with Extension Pack for Java

## Local database setup

For local development, the app creates its H2 schema and starter library/student records on first launch. The file-backed database is stored at `%USERPROFILE%\.alliance-smart-library\library.mv.db` and remains available after Tomcat restarts.

The first launch includes sample books and two sample student records for demonstration. Additions, edits, loans, returns, and settings are persisted in the local database. Stop Tomcat before backing up or moving the database file.

To create the initial staff account, configure `LIBRARY_STAFF_EMAIL` and `LIBRARY_STAFF_PASSWORD` in the Tomcat process environment before starting the application. Use a unique staff email and a password of at least 12 characters. The account is created as a librarian on first database initialization; an existing staff account with no password can be provisioned once this way. These variables are only used to establish an account and do not reset a non-empty password. Keep the password in a local secret store and never commit it.

Production uses MySQL. The app requires all three database environment variables and will refuse to start database operations in `APP_ENV=production` if they are incomplete. The app initializes required tables from `src/main/resources/db/schema.sql` on first connection; the database itself must already exist.

## Render deployment

The root `render.yaml` describes a Docker web service and declares the database settings as secrets to enter in the Render dashboard. Create a persistent, remotely accessible MySQL database with your database provider first; Render does not supply this application's MySQL server. Create the database and a dedicated app user, then give that user `CREATE`, `ALTER`, `INDEX`, `REFERENCES`, `SELECT`, `INSERT`, `UPDATE`, and `DELETE` privileges on that database. The app runs the bundled `src/main/resources/db/schema.sql` against the selected database on first connection using `CREATE TABLE IF NOT EXISTS` and safe seed statements. It skips the script's `CREATE DATABASE` and `USE alliance_library` commands, so create the database with your provider first. Existing tables and records are not dropped or reset.

Set these values under **Render → web service → Environment** (the Blueprint declares the names, but you must supply the actual provider values):

| Variable | Value |
|---|---|
| `APP_ENV` | `production` |
| `LIBRARY_DB_URL` | `jdbc:mysql://<host>:<port>/<database>?sslMode=VERIFY_IDENTITY&serverTimezone=UTC` |
| `LIBRARY_DB_USER` | The dedicated MySQL application username |
| `LIBRARY_DB_PASSWORD` | The application user's password, stored as a Render secret |
| `LIBRARY_STAFF_EMAIL` | The initial staff sign-in email, stored as a Render environment variable |
| `LIBRARY_STAFF_PASSWORD` | A strong initial staff password, stored as a Render secret |

Use the exact hostname, port, and database name from your MySQL provider's connection details; do not use `localhost`, the sample hostname, or a Windows path. Keep credentials only in Render's environment settings. If your provider does not support certificate hostname verification, use `sslMode=REQUIRED` instead; TLS is mandatory. Allow the Render service to reach the database using your provider's network/allow-list settings. Redeploy or restart after setting variables. The service fails closed instead of using local H2 when it detects Render or `APP_ENV=production`. The Docker entrypoint binds Tomcat to Render's `PORT`, with 8080 as the local/container default.

The Docker image disables Tomcat's shutdown socket so Render's HTTP health checks cannot be mistaken for shutdown commands; Tomcat serves HTTP on Render's injected `PORT` (8080 when running locally). The Blueprint health check requests `/`. If the service's Render settings specify a custom port, it must match the port the container listens on; never set the web service port to Tomcat's shutdown port (8005).

The database host and credentials have not been verified by this repository build. A successful Maven build does not test Render networking or connect to your hosted MySQL instance.

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

   The script stops Tomcat, deploys the WAR, and starts the local-only website. Keep the PowerShell window open while using the site.
4. Alternatively, deploy the WAR to Tomcat from your IDE with no `APP_ENV=production` value; the app uses the embedded local database.
5. Open `http://localhost:8080/smart-library/`. Sign in with the provisioned staff account or register as a student.

### IDE notes

- **IntelliJ IDEA:** Open `pom.xml` as a Maven project, select a JDK 17 project SDK, add a local Tomcat 10.1 server run configuration, and deploy the `smart-library:war exploded` artifact.
- **Eclipse:** Import as **Existing Maven Projects**, set the project JRE to JDK 17, install/configure Tomcat 10.1 in the Servers view, and add the project to that server.
- **VS Code:** Open the folder, install Extension Pack for Java and Community Server Connectors, let Maven import dependencies, select JDK 17, then deploy the WAR to a configured Tomcat 10.1 server.

## Access and security

- Local mode restricts requests to this computer. Sign-in is required for all application pages. Student accounts can browse books, update their own profile, view their loans, and request borrowing, renewal, and physical returns; staff accounts can manage students, books, circulation, requests, reports, and settings.
- Public registration creates student accounts only; staff accounts must be provisioned with `LIBRARY_STAFF_EMAIL` and `LIBRARY_STAFF_PASSWORD`. Passwords are stored as PBKDF2-HMAC-SHA256 hashes, and successful sign-in rotates the session.
- All state-changing forms use CSRF tokens. HTTPS, security headers, database constraints, role checks, and input validation are enabled. For institutional use, configure identity verification/SSO and operational controls appropriate to student records.
- Production mode requires HTTPS and MySQL and disables the embedded-database fallback.

## Features

- Database-calculated dashboard statistics, six-month activity, categories, recently issued books, and most-borrowed leaderboard.
- Search by title/author/ISBN/category, category and availability filters, visual stock status, and low-stock hints.
- Student self-registration, sign-in, profile updates, and a private borrowing history.
- Student borrowing, renewal, and physical return requests with staff review; pending borrows do not become loans until approved, and return requests remain active loans until staff confirms receipt.
- Student-visible library rules that display the configured loan period, daily overdue fine, and borrowing limit.
- Role-based access: students browse the catalogue and see their own records; staff manage students, books, circulation, reports, and settings.
- Student directory search and validated staff profile updates.
- Issue transactions lock the selected inventory row, enforce reference-only rules and configured student limits, and commit availability and loan records atomically.
- Return transactions prevent duplicate returns, restore inventory, and calculate the fine from the configured daily rate and overdue days in one transaction, including staff-confirmed student returns.
- Available/issued/overdue/history/popularity/monthly reports, with CSV download.
- Configurable loan period, daily fine, and borrowing limit.
- Responsive forest-green, ivory, and brass interface; charts use the Maven-packaged Chart.js WebJar.

## Database tables

`users`, `students`, `books`, `book_issues`, `book_returns`, `book_requests`, `book_return_requests`, and `library_settings` are created by the application on first local use or by `schema.sql` in MySQL. Foreign keys, uniqueness rules, indexes, and check constraints protect core records. Issue and return records are attributed to the signed-in staff account. Circulation and request decisions use explicit JDBC transactions and row locks. The local starter catalogue contains 25 books; SQL seeds add new titles by ISBN without duplicating existing records.

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

Run automated tests with `mvn clean verify`. Tests cover domain validation, password hashing, embedded database initialization, student registration/sign-in, request approval, and issue/return transactions, including staff-confirmed student returns.

| Scenario | Expected result |
|---|---|
| Open the site root | Opens the sign-in page |
| Register as a student | Creates a student-only account with a unique email and student ID |
| Sign in as a student | Shows the student's own profile and loan history; staff-only pages are denied |
| Sign in as staff | Opens the staff dashboard and management tools |
| Student request or renewal | Appears in the staff request queue and is processed atomically with the configured borrowing rules |
| Issue/return a book | The signed-in staff account is recorded; inventory updates atomically |
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
