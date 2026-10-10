package com.creditflow.service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.creditflow.api.ApiModels.ApplicationProfile;
import com.creditflow.api.ApiModels.ApplicationRequest;
import com.creditflow.api.ApiModels.CustomerCreated;
import com.creditflow.api.ApiModels.CustomerProfile;
import com.creditflow.api.ApiModels.DashboardStats;
import com.creditflow.api.ApiModels.LoanProduct;
import com.creditflow.api.ApiModels.RegistrationRequest;
import com.creditflow.api.ApiModels.RepaymentItem;

@Service
public class LoanService {
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoanService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public CustomerCreated register(RegistrationRequest request) {
        String employmentType = normalizeEmploymentType(request.employmentType());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        PreparedStatementCreator customerInsert = connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO customers(full_name,email,phone,password_hash,date_of_birth,address,city) VALUES(?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.fullName().trim());
            statement.setString(2, request.email().trim().toLowerCase(Locale.ROOT));
            statement.setString(3, request.phone().trim());
            statement.setString(4, passwordEncoder.encode(request.password()));
            statement.setObject(5, request.dateOfBirth());
            statement.setString(6, request.address().trim());
            statement.setString(7, request.city().trim());
            return statement;
        };
        jdbc.update(customerInsert, keyHolder);

        Number generatedKey = keyHolder.getKey();
        if (generatedKey == null) {
            throw new IllegalStateException("The database did not return a customer ID.");
        }
        int customerId = generatedKey.intValue();

        jdbc.update(
                "INSERT INTO employment_details(customer_id,employment_type,company_name,monthly_income,experience_years) VALUES(?,?,?,?,?)",
                customerId,
                employmentType,
                blankToNull(request.companyName()),
                request.monthlyIncome(),
                request.experienceYears());
        jdbc.update(
                "INSERT INTO kyc_details(customer_id,pan_number,aadhaar_last4) VALUES(?,?,?)",
                customerId,
                request.panNumber().toUpperCase(Locale.ROOT),
                request.aadhaarLast4());

        // Every newly registered customer receives an empty savings account.
        String accountNumber = "UB" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        jdbc.update("INSERT INTO accounts(account_number,customer_id,account_type,balance) VALUES(?,?,'SAVINGS',0.00)", accountNumber, customerId);

        return new CustomerCreated(customerId, request.fullName().trim());
    }

    public CustomerProfile getCustomer(int customerId) {
        return jdbc.queryForObject(
                "SELECT c.customer_id,c.full_name,c.email,c.phone,e.employment_type,e.monthly_income " +
                        "FROM customers c JOIN employment_details e ON e.customer_id=c.customer_id WHERE c.customer_id=?",
                (result, rowNumber) -> new CustomerProfile(
                        result.getInt("customer_id"),
                        result.getString("full_name"),
                        result.getString("email"),
                        result.getString("phone"),
                        result.getString("employment_type"),
                        result.getBigDecimal("monthly_income")),
                customerId);
    }


    public List<CustomerProfile> getCustomers() {
    return jdbc.query(
            "SELECT c.customer_id,c.full_name,c.email,c.phone," +
                    "e.employment_type,e.monthly_income " +
                    "FROM customers c " +
                    "JOIN employment_details e " +
                    "ON e.customer_id=c.customer_id " +
                    "ORDER BY c.customer_id",
            (result, rowNumber) -> new CustomerProfile(
                    result.getInt("customer_id"),
                    result.getString("full_name"),
                    result.getString("email"),
                    result.getString("phone"),
                    result.getString("employment_type"),
                    result.getBigDecimal("monthly_income")
            )
    );
}

    public List<LoanProduct> getLoanProducts() {
        return jdbc.query(
                "SELECT loan_type_id,loan_type,min_amount,max_amount,annual_interest_rate,max_tenure_months,min_credit_score,min_monthly_income " +
                        "FROM loan_products ORDER BY loan_type",
                (result, rowNumber) -> new LoanProduct(
                        result.getInt("loan_type_id"),
                        result.getString("loan_type"),
                        result.getBigDecimal("min_amount"),
                        result.getBigDecimal("max_amount"),
                        result.getBigDecimal("annual_interest_rate"),
                        result.getInt("max_tenure_months"),
                        result.getInt("min_credit_score"),
                        result.getBigDecimal("min_monthly_income")));
    }

    public DashboardStats getDashboardStats() {
        return jdbc.queryForObject(
                "SELECT (SELECT COUNT(*) FROM customers) AS customer_count, " +
                        "(SELECT COUNT(*) FROM loan_applications) AS application_count, " +
                        "(SELECT COUNT(*) FROM loans) AS approved_loan_count",
                (result, rowNumber) -> new DashboardStats(
                        result.getLong("customer_count"),
                        result.getLong("application_count"),
                        result.getLong("approved_loan_count")));
    }

    public List<RepaymentItem> getRepayments(int customerId) {
        return jdbc.query(
                "SELECT rs.schedule_id,rs.loan_id,rs.installment_no,rs.due_date,rs.emi_amount,rs.status,lp.loan_type " +
                        "FROM repayment_schedule rs " +
                        "JOIN loans l ON l.loan_id=rs.loan_id " +
                        "JOIN loan_applications la ON la.application_id=l.application_id " +
                        "JOIN loan_products lp ON lp.loan_type_id=la.loan_type_id " +
                        "WHERE la.customer_id=? ORDER BY rs.due_date,rs.installment_no",
                this::mapRepayment,
                customerId);
    }

    public RepaymentItem payInstallment(long scheduleId) {
        String status = jdbc.queryForObject(
                "SELECT status FROM repayment_schedule WHERE schedule_id=?",
                String.class,
                scheduleId);
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("This installment is not pending payment.");
        }

        jdbc.execute((Connection connection) -> {
            try (CallableStatement statement = connection.prepareCall("{CALL pay_emi(?)}")) {
                statement.setLong(1, scheduleId);
                boolean hasResults = statement.execute();
                while (hasResults || statement.getUpdateCount() != -1) {
                    if (hasResults) {
                        try (ResultSet result = statement.getResultSet()) {
                            while (result.next()) {
                                // Consume the procedure result so the connection can be reused.
                            }
                        }
                    }
                    hasResults = statement.getMoreResults();
                }
            }
            return null;
        });

        return jdbc.queryForObject(
                "SELECT rs.schedule_id,rs.loan_id,rs.installment_no,rs.due_date,rs.emi_amount,rs.status,lp.loan_type " +
                        "FROM repayment_schedule rs " +
                        "JOIN loans l ON l.loan_id=rs.loan_id " +
                        "JOIN loan_applications la ON la.application_id=l.application_id " +
                        "JOIN loan_products lp ON lp.loan_type_id=la.loan_type_id " +
                        "WHERE rs.schedule_id=?",
                this::mapRepayment,
                scheduleId);
    }

    private RepaymentItem mapRepayment(ResultSet result, int rowNumber) throws SQLException {
        return new RepaymentItem(
                result.getLong("schedule_id"),
                result.getInt("loan_id"),
                result.getInt("installment_no"),
                result.getDate("due_date").toLocalDate(),
                result.getBigDecimal("emi_amount"),
                result.getString("status"),
                result.getString("loan_type"));
    }

    public ApplicationProfile submitApplication(ApplicationRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        PreparedStatementCreator applicationInsert = connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO loan_applications(customer_id,loan_type_id,requested_amount,tenure_months,purpose,existing_emi,credit_score) VALUES(?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, request.customerId());
            statement.setInt(2, request.loanTypeId());
            statement.setBigDecimal(3, request.requestedAmount());
            statement.setInt(4, request.tenureMonths());
            statement.setString(5, request.purpose().trim());
            statement.setBigDecimal(6, request.existingEmi());
            statement.setInt(7, request.creditScore());
            return statement;
        };
        jdbc.update(applicationInsert, keyHolder);

        Number generatedKey = keyHolder.getKey();
        if (generatedKey == null) {
            throw new IllegalStateException("The database did not return an application ID.");
        }
        int applicationId = generatedKey.intValue();

        jdbc.execute((Connection connection) -> {
            try (CallableStatement statement = connection.prepareCall("{CALL process_loan_application(?)}")) {
                statement.setInt(1, applicationId);
                boolean hasResults = statement.execute();
                while (hasResults || statement.getUpdateCount() != -1) {
                    if (hasResults) {
                        try (ResultSet result = statement.getResultSet()) {
                            while (result.next()) {
                                // Consume the procedure result so the connection can be reused.
                            }
                        }
                    }
                    hasResults = statement.getMoreResults();
                }
            }
            return null;
        });

        return getApplication(applicationId);
    }

    public ApplicationProfile getApplication(int applicationId) {
        return jdbc.queryForObject(
                "SELECT application_id,customer_id,full_name,email,phone,monthly_income,employment_type,kyc_status,loan_type,annual_interest_rate,requested_amount,tenure_months,existing_emi,credit_score,purpose,status,rejection_reason,monthly_emi,outstanding_principal,loan_status " +
                        "FROM vw_application_details WHERE application_id=?",
                applicationProfileRowMapper(),
                applicationId);
    }

    public List<ApplicationProfile> getApplications() {
    return jdbc.query(
            "SELECT application_id,customer_id,full_name,email,phone," +
                    "monthly_income,employment_type,kyc_status,loan_type," +
                    "annual_interest_rate,requested_amount,tenure_months," +
                    "existing_emi,credit_score,purpose,status,rejection_reason," +
                    "monthly_emi,outstanding_principal,loan_status " +
                    "FROM vw_application_details " +
                    "ORDER BY application_id DESC",
            applicationProfileRowMapper()
    );
    }

    private RowMapper<ApplicationProfile> applicationProfileRowMapper() {
        return (result, rowNumber) -> new ApplicationProfile(
                result.getInt("application_id"),
                result.getInt("customer_id"),
                result.getString("full_name"),
                result.getString("email"),
                result.getString("phone"),
                result.getBigDecimal("monthly_income"),
                result.getString("employment_type"),
                result.getString("kyc_status"),
                result.getString("loan_type"),
                result.getBigDecimal("annual_interest_rate"),
                result.getBigDecimal("requested_amount"),
                result.getInt("tenure_months"),
                result.getBigDecimal("existing_emi"),
                result.getInt("credit_score"),
                result.getString("purpose"),
                result.getString("status"),
                result.getString("rejection_reason"),
                result.getBigDecimal("monthly_emi"),
                result.getBigDecimal("outstanding_principal"),
                result.getString("loan_status"));
    }

    private String normalizeEmploymentType(String employmentType) {
        String normalized = employmentType.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!List.of("SALARIED", "SELF_EMPLOYED", "BUSINESS", "OTHER").contains(normalized)) {
            throw new IllegalArgumentException("Select a valid employment type.");
        }
        return normalized.equals("BUSINESS_OWNER") ? "BUSINESS" : normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
