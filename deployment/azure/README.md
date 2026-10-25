# Azure deployment: public library demo

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

The root-level `Dockerfile` builds the Tomcat 10.1 container image. The GitHub Actions workflow in `.github/workflows/azure-deploy.yml` tests the project, builds that image, pushes it to ACR, and updates App Service. Push the project to a private GitHub repository on `main`, then configure GitHub Actions:

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

The deployed website is intentionally public and has no staff bootstrap or sign-in flow. All library records and operations are accessible to anyone who can reach the website; do not use real personal data.

## 3. Initialize MySQL and create the least-privilege app account

Connect to the MySQL host from the setup machine using MySQL Workbench and the administrator credentials entered into Azure. If your public IP changed after provisioning, update the `setup-client` firewall rule to your current IP first. Run `src/main/resources/db/schema.sql`.

Create a separate app account; do not use the MySQL administrator for the website:

```sql
CREATE USER 'library_app'@'%' IDENTIFIED BY 'REPLACE_WITH_A_NEW_RANDOM_SECRET' REQUIRE SSL;
GRANT CREATE, ALTER, INDEX, REFERENCES, SELECT, INSERT, UPDATE, DELETE ON alliance_library.* TO 'library_app'@'%';
```

Use new, unique passwords. Do not put them in source files or commit them. Add these secrets in the provisioned Key Vault using the Azure portal:

- `library-db-user` — `library_app`
- `library-db-password` — the password just assigned to that account
- `library-staff-password` — a unique initial staff password with at least 12 characters

For password rotation, update both the MySQL account and Key Vault secret, then restart the Web App.

## 4. Configure the database connection and start the demo

In App Service **Configuration → Application settings**, add:

- `LIBRARY_DB_USER` = `@Microsoft.KeyVault(SecretUri=https://<key-vault-name>.vault.azure.net/secrets/library-db-user)`
- `LIBRARY_DB_PASSWORD` = `@Microsoft.KeyVault(SecretUri=https://<key-vault-name>.vault.azure.net/secrets/library-db-password)`
- `LIBRARY_STAFF_EMAIL` = the email for the initial library staff account
- `LIBRARY_STAFF_PASSWORD` = `@Microsoft.KeyVault(SecretUri=https://<key-vault-name>.vault.azure.net/secrets/library-staff-password)`

The `APP_ENV=production` setting and HTTPS-only website configuration are provided by the template. The root Docker image directs visitors to sign-in. On first connection, the app initializes missing tables from `src/main/resources/db/schema.sql` using non-destructive `CREATE TABLE IF NOT EXISTS` statements and safe seeds; ensure the app user has the schema permissions above. Existing tables and records are retained.

Key Vault references resolve through the App Service's system-assigned identity. If the reference status is unresolved, check the Key Vault secret names, the `Key Vault Secrets User` role assignment, and the Web App's identity before retrying.

## 5. Update schema and deploy future changes

For future pushes to `main`, GitHub Actions runs `mvn clean verify` before publishing a container image. The application creates missing tables during startup; review any changes to existing-table structure and execute required SQL migrations before deploying dependent code. Keep MySQL backups enabled and test a restore before production use.

The generated App Service outbound IP list is included in MySQL firewall rules. If App Service plan/region or outbound addresses change, update and redeploy the Bicep template so the MySQL allow-list remains accurate. The template intentionally does not open MySQL to all Azure services.

## Security boundaries and limitations

- Sign-in and role checks are required. Public registration creates student accounts only; staff accounts are provisioned using secret environment variables. Student identity is not independently verified, so institutional SSO/email verification is required before using real student records.
- CSRF validation covers POST forms. HTTPS, role checks, password hashing, and input validation are enabled.
- The application requires `APP_ENV=production` and the MySQL environment variables, rejects missing production database configuration, and never falls back to the local H2 file.
- User-managed browser-to-App-Service traffic is HTTPS-only. MySQL uses a per-IP firewall allow-list and TLS certificate/hostname verification.
- This configuration is for a development/demo deployment only. Complete an institutional security and privacy review, identity verification, and operational hardening before production student data or workloads.
