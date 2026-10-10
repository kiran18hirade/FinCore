USE loan_approval_db;
-- Apply a test application manually:
-- INSERT INTO loan_applications(customer_id,loan_type_id,requested_amount,tenure_months,purpose,existing_emi,credit_score) VALUES(1,1,300000,36,'Education expenses',5000,760);
-- SET @id=LAST_INSERT_ID(); CALL process_loan_application(@id);
SELECT * FROM vw_application_details ORDER BY application_id DESC;
SELECT status,COUNT(*) total FROM loan_applications GROUP BY status;
SELECT loan_type,COUNT(*) applications,SUM(requested_amount) requested_amount FROM vw_application_details GROUP BY loan_type;
SELECT * FROM audit_logs ORDER BY created_at DESC;
SELECT calculate_emi(300000,12.5,36) AS monthly_emi;
