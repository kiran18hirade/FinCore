USE unified_bank_management;

DROP FUNCTION IF EXISTS calculate_emi;
DROP PROCEDURE IF EXISTS process_loan_application;
DROP PROCEDURE IF EXISTS pay_emi;
DROP EVENT IF EXISTS ev_mark_overdue;

CREATE FUNCTION calculate_emi(p_principal DECIMAL(14,2), p_rate DECIMAL(5,2), p_months INT)
RETURNS DECIMAL(14,2) DETERMINISTIC
RETURN CASE 
    WHEN p_principal <= 0 OR p_months <= 0 THEN 0
    WHEN p_rate = 0 THEN ROUND(p_principal / p_months, 2)
    ELSE ROUND((p_principal * (p_rate / 1200) * POW(1 + (p_rate / 1200), p_months)) / (POW(1 + (p_rate / 1200), p_months) - 1), 2)
END;

DELIMITER $$

CREATE PROCEDURE process_loan_application(IN p_application_id INT)
BEGIN
 DECLARE v_customer INT; DECLARE v_type_id INT; DECLARE v_amount DECIMAL(14,2); DECLARE v_tenure INT;
 DECLARE v_existing_emi DECIMAL(14,2); DECLARE v_credit INT; DECLARE v_income DECIMAL(14,2);
 DECLARE v_min_income DECIMAL(14,2); DECLARE v_min_score INT; DECLARE v_max_amount DECIMAL(14,2);
 DECLARE v_min_amount DECIMAL(14,2); DECLARE v_max_tenure INT; DECLARE v_rate DECIMAL(5,2); DECLARE v_max_ratio DECIMAL(5,4);
 DECLARE v_kyc VARCHAR(20); DECLARE v_emi DECIMAL(14,2); DECLARE v_total_emi DECIMAL(14,2);
 DECLARE b_income BOOLEAN; DECLARE b_score BOOLEAN; DECLARE b_emi BOOLEAN; DECLARE b_amount BOOLEAN; DECLARE b_tenure BOOLEAN; DECLARE b_kyc BOOLEAN;
 DECLARE v_reason VARCHAR(1000) DEFAULT '';
 DECLARE v_loan_id INT; DECLARE v_i INT DEFAULT 1; DECLARE v_balance DECIMAL(14,2); DECLARE v_interest DECIMAL(14,2); DECLARE v_principal DECIMAL(14,2);
 DECLARE v_due DATE;

 START TRANSACTION;
 SELECT la.customer_id,la.loan_type_id,la.requested_amount,la.tenure_months,la.existing_emi,la.credit_score,
        e.monthly_income,lp.min_monthly_income,lp.min_credit_score,lp.max_amount,lp.min_amount,lp.max_tenure_months,
        lp.annual_interest_rate,lp.max_emi_ratio,COALESCE(k.kyc_status,'PENDING')
 INTO v_customer,v_type_id,v_amount,v_tenure,v_existing_emi,v_credit,v_income,v_min_income,v_min_score,v_max_amount,
      v_min_amount,v_max_tenure,v_rate,v_max_ratio,v_kyc
 FROM loan_applications la
 JOIN employment_details e ON e.customer_id=la.customer_id
 JOIN loan_products lp ON lp.loan_type_id=la.loan_type_id
 LEFT JOIN kyc_details k ON k.customer_id=la.customer_id
 WHERE la.application_id=p_application_id FOR UPDATE;

 SET v_emi=calculate_emi(v_amount,v_rate,v_tenure);
 SET v_total_emi=v_existing_emi+v_emi;
 SET b_income=(v_income>=v_min_income);
 SET b_score=(v_credit>=v_min_score);
 SET b_emi=(v_total_emi<=v_income*v_max_ratio);
 SET b_amount=(v_amount BETWEEN v_min_amount AND v_max_amount);
 SET b_tenure=(v_tenure BETWEEN 6 AND v_max_tenure);
 SET b_kyc=(v_kyc='VERIFIED');

 IF NOT b_income THEN SET v_reason=CONCAT(v_reason,'Monthly income is below the minimum requirement. '); END IF;
 IF NOT b_score THEN SET v_reason=CONCAT(v_reason,'Credit score is below the required minimum. '); END IF;
 IF NOT b_emi THEN SET v_reason=CONCAT(v_reason,'Existing EMI plus proposed EMI exceeds the permitted income ratio. '); END IF;
 IF NOT b_amount THEN SET v_reason=CONCAT(v_reason,'Requested loan amount is outside the allowed range. '); END IF;
 IF NOT b_tenure THEN SET v_reason=CONCAT(v_reason,'Requested tenure exceeds the allowed limit. '); END IF;
 IF NOT b_kyc THEN SET v_reason=CONCAT(v_reason,'KYC is not verified. '); END IF;

 INSERT INTO loan_evaluations(application_id,income_check,credit_score_check,emi_check,amount_check,tenure_check,kyc_check,overall_result,reason)
 VALUES(p_application_id,b_income,b_score,b_emi,b_amount,b_tenure,b_kyc,IF(b_income AND b_score AND b_emi AND b_amount AND b_tenure AND b_kyc,'APPROVED','REJECTED'),NULLIF(TRIM(v_reason),''))
 ON DUPLICATE KEY UPDATE income_check=VALUES(income_check),credit_score_check=VALUES(credit_score_check),emi_check=VALUES(emi_check),amount_check=VALUES(amount_check),tenure_check=VALUES(tenure_check),kyc_check=VALUES(kyc_check),overall_result=VALUES(overall_result),reason=VALUES(reason),evaluated_at=CURRENT_TIMESTAMP;

 IF b_income AND b_score AND b_emi AND b_amount AND b_tenure AND b_kyc THEN
   UPDATE loan_applications SET status='APPROVED',rejection_reason=NULL,decided_at=NOW() WHERE application_id=p_application_id;
   INSERT INTO loans(application_id,customer_id,principal_amount,annual_interest_rate,tenure_months,monthly_emi,outstanding_principal,start_date)
   VALUES(p_application_id,v_customer,v_amount,v_rate,v_tenure,v_emi,v_amount,CURDATE());
   SET v_loan_id=LAST_INSERT_ID(); SET v_balance=v_amount; SET v_due=DATE_ADD(CURDATE(),INTERVAL 1 MONTH);
   WHILE v_i<=v_tenure DO
     SET v_interest=ROUND(v_balance*(v_rate/1200),2);
     SET v_principal=ROUND(v_emi-v_interest,2);
     IF v_i=v_tenure OR v_principal>v_balance THEN SET v_principal=v_balance; SET v_emi=ROUND(v_principal+v_interest,2); END IF;
     INSERT INTO repayment_schedule(loan_id,installment_no,due_date,emi_amount,principal_component,interest_component)
     VALUES(v_loan_id,v_i,v_due,v_emi,v_principal,v_interest);
     SET v_balance=GREATEST(0,v_balance-v_principal); SET v_i=v_i+1; SET v_due=DATE_ADD(v_due,INTERVAL 1 MONTH);
   END WHILE;
   INSERT INTO audit_logs(application_id,action_type,old_status,new_status,description) VALUES(p_application_id,'AUTOMATIC_DECISION','PENDING','APPROVED','Application approved automatically by MySQL eligibility rules; loan and EMI schedule created.');
 ELSE
   UPDATE loan_applications SET status='REJECTED',rejection_reason=TRIM(v_reason),decided_at=NOW() WHERE application_id=p_application_id;
   INSERT INTO audit_logs(application_id,action_type,old_status,new_status,description) VALUES(p_application_id,'AUTOMATIC_DECISION','PENDING','REJECTED',TRIM(v_reason));
 END IF;
 COMMIT;
 SELECT la.application_id,la.status,la.rejection_reason,l.loan_id,l.monthly_emi
 FROM loan_applications la LEFT JOIN loans l ON l.application_id=la.application_id WHERE la.application_id=p_application_id;
END $$

CREATE PROCEDURE pay_emi(IN p_schedule_id BIGINT)
BEGIN
 DECLARE v_loan INT; DECLARE v_amount DECIMAL(14,2); DECLARE v_principal DECIMAL(14,2);
 START TRANSACTION;
 SELECT loan_id,emi_amount,principal_component INTO v_loan,v_amount,v_principal FROM repayment_schedule WHERE schedule_id=p_schedule_id AND status='PENDING' FOR UPDATE;
 UPDATE repayment_schedule SET status='PAID',paid_at=NOW() WHERE schedule_id=p_schedule_id;
 UPDATE loans SET outstanding_principal=GREATEST(0,outstanding_principal-v_principal),loan_status=IF(outstanding_principal-v_principal<=0,'CLOSED','ACTIVE') WHERE loan_id=v_loan;
 COMMIT;
 SELECT * FROM repayment_schedule WHERE schedule_id=p_schedule_id;
END $$

DELIMITER ;

CREATE OR REPLACE VIEW vw_application_details AS
SELECT la.application_id,la.customer_id,c.full_name,c.email,c.phone,ed.monthly_income,ed.employment_type,k.kyc_status,
       lp.loan_type,lp.annual_interest_rate,la.requested_amount,la.tenure_months,la.existing_emi,la.credit_score,la.purpose,
       la.status,la.rejection_reason,la.applied_at,la.decided_at,le.income_check,le.credit_score_check,le.emi_check,
       le.amount_check,le.tenure_check,le.kyc_check,le.overall_result,l.loan_id,l.monthly_emi,l.outstanding_principal,l.loan_status
FROM loan_applications la JOIN customers c ON c.customer_id=la.customer_id
JOIN employment_details ed ON ed.customer_id=c.customer_id JOIN loan_products lp ON lp.loan_type_id=la.loan_type_id
LEFT JOIN kyc_details k ON k.customer_id=c.customer_id LEFT JOIN loan_evaluations le ON le.application_id=la.application_id
LEFT JOIN loans l ON l.application_id=la.application_id;

CREATE OR REPLACE VIEW vw_customer_dashboard AS
SELECT c.customer_id,c.full_name,c.email,la.application_id,lp.loan_type,la.requested_amount,la.status,la.rejection_reason,
       l.loan_id,l.monthly_emi,l.outstanding_principal,l.loan_status
FROM customers c LEFT JOIN loan_applications la ON la.customer_id=c.customer_id
LEFT JOIN loan_products lp ON lp.loan_type_id=la.loan_type_id LEFT JOIN loans l ON l.application_id=la.application_id;

CREATE EVENT IF NOT EXISTS ev_mark_overdue ON SCHEDULE EVERY 1 DAY DO
UPDATE repayment_schedule SET status='OVERDUE' WHERE status='PENDING' AND due_date<CURDATE();
