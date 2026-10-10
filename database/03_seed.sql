USE unified_bank_management;

INSERT INTO loan_products(loan_type,min_amount,max_amount,annual_interest_rate,max_tenure_months,min_credit_score,max_emi_ratio,min_monthly_income) VALUES
('PERSONAL',50000,1000000,12.50,60,650,0.50,20000),
('EDUCATION',100000,2000000,9.50,120,600,0.55,15000),
('HOME',500000,5000000,8.50,360,700,0.45,40000),
('VEHICLE',100000,2500000,10.00,84,650,0.50,25000),
('BUSINESS',200000,3000000,11.50,84,675,0.50,30000)
ON DUPLICATE KEY UPDATE min_amount=VALUES(min_amount),max_amount=VALUES(max_amount),annual_interest_rate=VALUES(annual_interest_rate),max_tenure_months=VALUES(max_tenure_months),min_credit_score=VALUES(min_credit_score),max_emi_ratio=VALUES(max_emi_ratio),min_monthly_income=VALUES(min_monthly_income);
