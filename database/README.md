# Database scripts

Run scripts in this order on a clean local development setup:
1. `01_schema.sql` — creates `unified_bank_management` and all tables. It does not drop tables.
2. `02_procedures_views.sql` — installs EMI calculation, loan processing and repayment procedures, plus reporting views.
3. `03_seed.sql` — adds the loan product catalogue only.

The schema is intentionally separate from `bank_management_system` and `loan_approval_db`. Do not change the `USE unified_bank_management` statement unless you intentionally want to target a different database.
