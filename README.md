# FinCore

An academic full-stack bank management project built from a clean, separate MySQL database. It combines customer management and loan processing with bank accounts, deposits, withdrawals, transfers, transaction history, and repayment tracking.

## Stack
- Frontend: HTML, CSS, JavaScript
- Backend: Java 17, Spring Boot, JDBC
- Database: MySQL 8+

## Important safety note
This is an educational project, not production banking software. Do not use real customer data or real money. Before production use, implement and independently test authentication/authorization, audit controls, idempotent payments, rate limiting, secrets management, encryption, and operational monitoring.

## Fresh setup (do not import either old database)
1. Back up your old databases if you need them. This project creates a separate database named `unified_bank_management`; it does not modify `bank_management_system` or `loan_approval_db`.
2. Open MySQL Workbench and run `database/01_schema.sql`. It creates the new database and tables without dropping existing data.
3. Run `database/02_procedures_views.sql`.
4. Run `database/03_seed.sql` to add sample loan products. This seed script does not insert sample customer records.
5. In `backend/java`, copy `.env.example` values into your environment or set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Do not commit credentials. The application reads these variables.
6. Run the backend from `backend/java` using `mvn spring-boot:run` (requires Java 17+ and Maven).
7. Serve the `frontend` folder using VS Code Live Server or another static file server. Open `frontend/index.html`.

## Environment variables
- `DB_URL` defaults to `jdbc:mysql://localhost:3306/unified_bank_management?serverTimezone=UTC`
- `DB_USERNAME` defaults to `root`
- `DB_PASSWORD` has no hard-coded default; set it in your environment.
- `SERVER_PORT` defaults to `8080`

## Main API endpoints
- `GET /api/health`
- `GET /api/dashboard`
- `POST /api/customers`
- `GET /api/customers` and `GET /api/customers/{id}`
- `GET /api/accounts?customerId={id}`
- `GET /api/transactions?accountId={id}`
- `POST /api/accounts/{id}/deposit` with `{ "amount": 1000, "description": "Cash deposit" }`
- `POST /api/accounts/{id}/withdraw` with `{ "amount": 500, "description": "ATM withdrawal" }`
- `POST /api/transfers` with `{ "fromAccountId": 1, "toAccountId": 2, "amount": 500, "description": "Transfer" }`
- `GET /api/loan-products`
- `POST /api/applications`
- `GET /api/applications`
- `GET /api/customers/{id}/repayments`
- `POST /api/repayments/{scheduleId}/pay`

## Test checklist
1. Register a test customer and verify an account is created.
2. Deposit a positive amount and confirm balance and transaction history change.
3. Attempt a withdrawal above the balance; it must fail without changing the balance.
4. Transfer between two accounts and verify both balances and both transaction entries.
5. Apply for a loan and inspect application status, loan record, and repayment schedule.
6. Try invalid amounts and nonexistent IDs.

## Notes
- Bank operations use database transactions and row locks for balance updates.
- The current application is a development baseline. Sensitive endpoints must be protected with authentication and role-based authorization before real use.
- Use the new schema scripts only for the new database. Never run destructive schema scripts against your old databases.
