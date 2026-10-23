# Azure deployment: protected library website

This deploys the existing Java website, not a native app. The target is Linux Azure App Service running the Maven WAR in Tomcat 10.1, with Azure Database for MySQL Flexible Server and Azure Key Vault references for secrets. The MySQL endpoint has public networking enabled only with a firewall allow-list for the setup IP and the App Service's possible outbound IPs. HTTPS is required at the website; database JDBC connections verify TLS.

No cloud resources are created by this repository until you deploy the Bicep template in your own Azure subscription. Azure costs apply for App Service, MySQL, Container Registry, and Key Vault.

## 1. Provision Azure infrastructure

Prerequisites: an Azure subscription where you can create a resource group, App Service, Container Registry, Key Vault, and MySQL Flexible Server. In the Azure portal, open **Deploy a custom template**, load `deployment/azure/main.bicep`, and enter:

- A region that supports both App Service and MySQL Flexible Server.
- Globally unique lowercase names for the website, registry, and MySQL server.
- A MySQL administrator login and strong administrator password. The Bicep parameter is marked secure; enter the password only in Azure's deployment form.
- Your current public IPv4 address as `setupClientIp` (the MySQL firewall permits this one address for initial schema setup).

The template creates an HTTPS-only App Service, a managed-identity-pull ACR, Azure Database for MySQL with public networking restricted by firewall rules, and a Key Vault with an app managed identity granted secret-read access. Save the deployment outputs: website URL, resource group, registry name/login server, MySQL host, and Key Vault name.

## 2. Build and publish the site image

The GitHub Actions workflow in `.github/workflows/azure-deploy.yml` tests the project, builds the Tomcat container, pushes it to ACR, and updates App Service to the new image. Push the project to a private GitHub repository on `main`, then configure GitHub Actions:

**Repository secrets (OIDC identity values, not passwords):**

- `AZURE_CLIENT_ID`
- `AZURE_TENANT_ID`
- `AZURE_SUBSCRIPTION_ID`

**Repository variables:**

- `AZURE_RESOURCE_GROUP`
- `AZURE_WEBAPP_NAME`
- `AZURE_ACR_NAME`
- `AZURE_ACR_LOGIN_SERVER`

Create a Microsoft Entra application/service principal with a GitHub Actions federated credential for this repository and branch. Grant its identity `AcrPush` on the registry and an appropriate Web App deployment role on the App Service. Do not use a publish profile or store database credentials in GitHub.

The app may show an unavailable sign-in until the database and staff bootstrap steps below are finished; protected requests remain inaccessible.

## 3. Initialize MySQL and create the least-privilege app account

Connect to the MySQL host from the setup machine using MySQL Workbench and the administrator credentials entered into Azure. If your public IP changed after provisioning, update the `setup-client` firewall rule to your current IP first. Run `src/main/resources/db/schema.sql`.

Create a separate app account; do not use the MySQL administrator for the website:

```sql
CREATE USER 'library_app'@'%' IDENTIFIED BY 'REPLACE_WITH_A_NEW_RANDOM_SECRET' REQUIRE SSL;
GRANT SELECT, INSERT, UPDATE, DELETE ON alliance_library.* TO 'library_app'@'%';
```

Use a new, unique password. Do not put it in source files or commit it. Add these two secrets in the provisioned Key Vault using the Azure portal:

- `library-db-user` — `library_app`
- `library-db-password` — the password just assigned to that account

For password rotation, update both the MySQL account and Key Vault secret, then restart the Web App.

## 4. Configure first administrator and start the protected site

In App Service **Configuration → Application settings**, add:

- `LIBRARY_DB_USER` = `@Microsoft.KeyVault(SecretUri=https://<key-vault-name>.vault.azure.net/secrets/library-db-user)`
- `LIBRARY_DB_PASSWORD` = `@Microsoft.KeyVault(SecretUri=https://<key-vault-name>.vault.azure.net/secrets/library-db-password)`
- `LIBRARY_BOOTSTRAP_ADMIN_EMAIL` = the first library administrator's email
- `LIBRARY_BOOTSTRAP_ADMIN_PASSWORD` = a Key Vault reference to secret `library-bootstrap-admin-password`

Create `library-bootstrap-admin-password` in Key Vault with a newly generated password of at least 14 characters. Do not reuse your Microsoft or MySQL administrator password. Save the App Service settings; the app uses the secret once to create a salted PBKDF2 administrator account. Check App Service logs for **Bootstrap administrator is ready** and sign in at the website URL.

After the first administrator account is created, remove **both** `LIBRARY_BOOTSTRAP_ADMIN_EMAIL` and `LIBRARY_BOOTSTRAP_ADMIN_PASSWORD` from App Service settings, then restart the website. Removing these settings prevents bootstrap values from remaining in the runtime configuration. The stored administrator account is not removed. The primary administrator can create/disable librarian accounts and manage settings. Staff can change their passwords from **My account**.

Key Vault references resolve through the App Service's system-assigned identity. If the reference status is unresolved, check the Key Vault secret names, the `Key Vault Secrets User` role assignment, and the Web App's identity before retrying.

## 5. Update schema and deploy future changes

For future pushes to `main`, GitHub Actions runs `mvn clean verify` before publishing a container image. Database schema changes are not silently applied by the app: review and execute the relevant SQL migration before deploying code that depends on it. Keep MySQL backups enabled and test a restore before production use.

The generated App Service outbound IP list is included in MySQL firewall rules. If App Service plan/region or outbound addresses change, update and redeploy the Bicep template so the MySQL allow-list remains accurate. The template intentionally does not open MySQL to all Azure services.

## Security boundaries and limitations

- `/`, protected library operations, and reports require a server-side staff session. The only unauthenticated paths are sign-in, static assets, and the legacy registration redirect; there is no student self-registration.
- Admin and librarian roles are supported; Settings and Staff Accounts are administrator-only.
- Passwords use salted PBKDF2-HMAC-SHA256; sessions rotate on login, expire after 20 minutes of inactivity, use HTTP-only/SameSite cookies, and become Secure behind HTTPS.
- CSRF validation covers all POST forms, account state is rechecked on protected requests, and repeated sign-in attempts are throttled per web worker.
- The application requires `APP_ENV=production` in Azure, rejects missing production database configuration, and never falls back to the local H2 file.
- The included login throttle is in-memory and per worker. For a high-traffic production service, add an Azure WAF/rate-limit policy or a shared distributed limiter. Configure monitoring, alerting, backups, and a documented admin recovery process before institutional launch.
- User-managed browser-to-App-Service traffic is HTTPS-only. MySQL uses a per-IP firewall allow-list and TLS certificate/hostname verification.
