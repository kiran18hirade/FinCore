package com.creditflow.api;

import com.creditflow.service.BankingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BankingApiController {
    private final BankingService banking;
    public BankingApiController(BankingService banking) { this.banking = banking; }

    public record MoneyRequest(@NotNull @DecimalMin(value="0.01") @Digits(integer=13,fraction=2) BigDecimal amount, String description) {}
    public record TransferRequest(@NotNull Long fromAccountId, @NotNull Long toAccountId, @NotNull @DecimalMin(value="0.01") @Digits(integer=13,fraction=2) BigDecimal amount, String description) {}

    @GetMapping("/accounts")
    public List<Map<String,Object>> accounts(@RequestParam(required=false) Integer customerId) { return banking.getAccounts(customerId); }
    @GetMapping("/transactions")
    public List<Map<String,Object>> transactions(@RequestParam Long accountId) { return banking.getTransactions(accountId); }
    @PostMapping("/accounts/{accountId}/deposit")
    public Map<String,Object> deposit(@PathVariable long accountId, @Valid @RequestBody MoneyRequest request) { return banking.deposit(accountId,request.amount(),request.description()); }
    @PostMapping("/accounts/{accountId}/withdraw")
    public Map<String,Object> withdraw(@PathVariable long accountId, @Valid @RequestBody MoneyRequest request) { return banking.withdraw(accountId,request.amount(),request.description()); }
    @PostMapping("/transfers")
    public Map<String,Object> transfer(@Valid @RequestBody TransferRequest request) { return banking.transfer(request.fromAccountId(),request.toAccountId(),request.amount(),request.description()); }
}
