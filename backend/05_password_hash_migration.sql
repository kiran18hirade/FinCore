USE loan_approval_db;
ALTER TABLE customers MODIFY password_hash VARCHAR(255) NOT NULL;
