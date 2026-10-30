# Runtime configuration

BankVerse does not store database credentials or a JWT signing key in source control. The runtime process must supply the following environment variables.

| Variable | Purpose | Example format |
| --- | --- | --- |
| `BANKVERSE_DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/bankverse` |
| `BANKVERSE_DB_USERNAME` | Database user | `postgres` |
| `BANKVERSE_DB_PASSWORD` | Database password | Your local PostgreSQL password |
| `BANKVERSE_JWT_SECRET` | HS256 signing key | A unique, random value of at least 32 UTF-8 bytes |

Do not commit a `.env` file, real password, access token, or JWT secret. The repository ignores `.env` files to help prevent accidental disclosure.

## Windows PowerShell: local development

Set values for the current PowerShell window, then start the application from that same window:

```powershell
$env:BANKVERSE_DB_URL = "jdbc:postgresql://localhost:5432/bankverse"
$env:BANKVERSE_DB_USERNAME = "postgres"
$env:BANKVERSE_DB_PASSWORD = "replace-with-your-postgres-password"
$env:BANKVERSE_JWT_SECRET = "replace-with-a-random-secret-at-least-32-characters-long"
mvn spring-boot:run
```

Generate a suitable local JWT secret without displaying or saving it in the repository:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

Copy the generated output into `BANKVERSE_JWT_SECRET`. Restart the application after changing it; all JWTs issued with the previous key become invalid by design.

## Eclipse

Open **Run Configurations** → your Spring Boot / Java Application configuration → **Environment**. Add the same four variables, then run the application. Environment variables defined in one terminal window are not automatically available to Eclipse.

## Test configuration

`mvn test` uses the isolated H2 configuration under `src/test/resources`. It contains a non-production test key so tests do not need access to your PostgreSQL credentials or real JWT secret.
