package com.creditflow.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ApiModels {
    private ApiModels() {
    }

    public record RegistrationRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Pattern(regexp = "^[0-9]{10,15}$") String phone,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotNull @Past LocalDate dateOfBirth,
            @NotBlank @Size(max = 255) String address,
            @NotBlank @Size(max = 80) String city,
            @NotBlank String employmentType,
            @Size(max = 150) String companyName,
            @NotNull @DecimalMin(value = "0.01") BigDecimal monthlyIncome,
            @NotNull @DecimalMin(value = "0.0") BigDecimal experienceYears,
            @NotBlank @Size(max = 10) String panNumber,
            @NotBlank @Pattern(regexp = "^[0-9]{4}$") String aadhaarLast4) {
    }

    public record ApplicationRequest(
            @NotNull @Min(1) Integer customerId,
            @NotNull @Min(1) Integer loanTypeId,
            @NotNull @DecimalMin(value = "1.00") BigDecimal requestedAmount,
            @NotNull @Min(6) @Max(600) Integer tenureMonths,
            @NotBlank @Size(max = 255) String purpose,
            @NotNull @DecimalMin(value = "0.00") BigDecimal existingEmi,
            @NotNull @Min(300) @Max(900) Integer creditScore) {
    }

    public record CustomerCreated(int customerId, String fullName) {
    }

    public record CustomerProfile(
            int customerId,
            String fullName,
            String email,
            String phone,
            String employmentType,
            BigDecimal monthlyIncome) {
    }

    public record LoanProduct(
            int loanTypeId,
            String loanType,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            BigDecimal annualInterestRate,
            int maxTenureMonths,
            int minCreditScore,
            BigDecimal minMonthlyIncome) {
    }

    public record DashboardStats(long customerCount, long applicationCount, long approvedLoanCount) {
    }

    public record RepaymentItem(
            long scheduleId,
            int loanId,
            int installmentNo,
            LocalDate dueDate,
            BigDecimal emiAmount,
            String status,
            String loanType) {
    }

    public record ApplicationProfile(
            int applicationId,
            int customerId,
            String fullName,
            String email,
            String phone,
            BigDecimal monthlyIncome,
            String employmentType,
            String kycStatus,
            String loanType,
            BigDecimal annualInterestRate,
            BigDecimal requestedAmount,
            int tenureMonths,
            BigDecimal existingEmi,
            int creditScore,
            String purpose,
            String status,
            String rejectionReason,
            BigDecimal monthlyEmi,
            BigDecimal outstandingPrincipal,
            String loanStatus) {
    }

    public record ErrorResponse(String message) {
    }
}
