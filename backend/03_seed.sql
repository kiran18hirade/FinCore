USE loan_approval_db;
INSERT INTO loan_products(loan_type,min_amount,max_amount,annual_interest_rate,max_tenure_months,min_credit_score,max_emi_ratio,min_monthly_income) VALUES
('PERSONAL',50000,1000000,12.50,60,650,0.50,20000),
('EDUCATION',100000,2000000,9.50,120,600,0.55,15000),
('HOME',500000,5000000,8.50,360,700,0.45,40000),
('VEHICLE',100000,2500000,10.00,84,650,0.50,25000),
('BUSINESS',200000,3000000,11.50,84,675,0.50,30000);

-- Password for all demo users is: password123 (SHA-256)
INSERT INTO customers(full_name,email,phone,password_hash,date_of_birth,address,city) VALUES
('Aarav Sharma','aarav@example.com','9876543210','ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f','2001-05-10','Pune, Maharashtra','Pune'),
('Priya Patil','priya@example.com','9876543211','ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f','1999-08-22','Nashik, Maharashtra','Nashik');
-- Replace demo hashes during first real registration; these rows are only sample records.
INSERT INTO employment_details(customer_id,employment_type,company_name,monthly_income,experience_years) VALUES
(1,'SALARIED','ABC Technologies',65000,3),(2,'SELF_EMPLOYED','Patil Enterprises',45000,4);
INSERT INTO kyc_details(customer_id,pan_number,aadhaar_last4,kyc_status) VALUES
(1,'ABCDE1234F','1234','VERIFIED'),(2,'PQRSX5678K','5678','VERIFIED');
