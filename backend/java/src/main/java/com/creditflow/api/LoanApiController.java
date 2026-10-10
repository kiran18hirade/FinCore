package com.creditflow.api;

import com.creditflow.api.ApiModels.ApplicationProfile;
import com.creditflow.api.ApiModels.ApplicationRequest;
import com.creditflow.api.ApiModels.CustomerCreated;
import com.creditflow.api.ApiModels.CustomerProfile;
import com.creditflow.api.ApiModels.DashboardStats;
import com.creditflow.api.ApiModels.LoanProduct;
import com.creditflow.api.ApiModels.RepaymentItem;
import com.creditflow.api.ApiModels.RegistrationRequest;
import com.creditflow.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LoanApiController {

    private final LoanService loanService;

    public LoanApiController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping("/dashboard")
    public DashboardStats getDashboardStats() {
        return loanService.getDashboardStats();
    }

    @PostMapping("/customers")
    public CustomerCreated registerCustomer(
            @Valid @RequestBody RegistrationRequest request) {
        return loanService.register(request);
    }

    // Get all customers
    @GetMapping("/customers")
    public List<CustomerProfile> getCustomers() {
        return loanService.getCustomers();
    }

    // Get one customer
    @GetMapping("/customers/{customerId}")
    public CustomerProfile getCustomer(
            @PathVariable int customerId) {
        return loanService.getCustomer(customerId);
    }

    @GetMapping("/loan-products")
    public List<LoanProduct> getLoanProducts() {
        return loanService.getLoanProducts();
    }

    // Get all applications
    @GetMapping("/applications")
    public List<ApplicationProfile> getApplications() {
        return loanService.getApplications();
    }

    // Get one application
    @GetMapping("/applications/{applicationId}")
    public ApplicationProfile getApplication(
            @PathVariable int applicationId) {
        return loanService.getApplication(applicationId);
    }

    @GetMapping("/customers/{customerId}/repayments")
    public List<RepaymentItem> getRepayments(
            @PathVariable int customerId) {
        return loanService.getRepayments(customerId);
    }

    @PostMapping("/repayments/{scheduleId}/pay")
    public RepaymentItem payInstallment(
            @PathVariable long scheduleId) {
        return loanService.payInstallment(scheduleId);
    }

    @PostMapping("/applications")
    public ApplicationProfile submitApplication(
            @Valid @RequestBody ApplicationRequest request) {
        return loanService.submitApplication(request);
    }
}