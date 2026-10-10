# Loan Management API

Spring Boot + JDBC backend for the Loan Management System.

## 1. Requirements

- Java 17 or later
- MySQL 8.x
- Maven 3.9+ only if rebuilding the source

## 2. First-time database setup

Open MySQL Workbench and run **`backend/00_SETUP_DATABASE.sql`** once. It applies the required files in this order:

1. `01_schema.sql` — creates the database and tables
2. `02_procedures_views.sql` — creates the EMI function, stored procedures, views and overdue event
3. `03_seed.sql` — inserts loan products and demo customers

The schema script recreates the project tables, so do not run it against data you need to preserve.

## 3. MySQL credentials used by this project

For this local/demo copy, the default configuration is:

- Username: `root`
- Password: `12345678`
- Host: `localhost`
- Port: `3306`
- Database: `loan_approval_db`

Environment variables can still override these defaults (`DB_USERNAME`, `DB_PASSWORD`, `DB_URL`, `SERVER_PORT`).

## 4. Start the backend

The easiest Windows method is to double-click **`START_BACKEND.bat`** from this folder.

Or run:

```powershell
java -jar target\loan-management-api-1.0.0.jar
```

The API runs at `http://localhost:8080`.

If you rebuild from source:

```powershell
mvn clean package
java -jar target\loan-management-api-1.0.0.jar
```

## 5. Test the backend before opening the frontend

Open:

`http://localhost:8080/api/dashboard`

You should receive JSON containing `customerCount`, `applicationCount`, and `approvedLoanCount`.

## 6. Start the frontend

Open the `frontend` folder in VS Code and use Live Server on `index.html`. The JavaScript automatically uses the same hostname as the browser and port `8080` for the API.

## Important

The password `12345678` is included only because this is a local/classroom project. Do not commit real database credentials or use this password in production.
