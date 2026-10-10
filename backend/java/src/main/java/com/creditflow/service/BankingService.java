package com.creditflow.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.EmptyResultDataAccessException;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BankingService {
    private final JdbcTemplate jdbc;
    public BankingService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String,Object>> getAccounts(Integer customerId) {
        if (customerId == null) {
            return jdbc.queryForList("SELECT account_id,account_number,customer_id,account_type,balance,currency,status,created_at FROM accounts ORDER BY account_id DESC");
        }
        return jdbc.queryForList("SELECT account_id,account_number,customer_id,account_type,balance,currency,status,created_at FROM accounts WHERE customer_id=? ORDER BY account_id DESC", customerId);
    }

    public List<Map<String,Object>> getTransactions(Long accountId) {
        if (accountId == null) throw new IllegalArgumentException("accountId is required.");
        return jdbc.queryForList("SELECT transaction_id,reference_code,account_id,related_account_id,transaction_type,amount,balance_after,description,status,created_at FROM transactions WHERE account_id=? ORDER BY created_at DESC,transaction_id DESC", accountId);
    }

    @Transactional
    public Map<String,Object> deposit(long accountId, BigDecimal amount, String description) {
        validateAmount(amount);
        Map<String,Object> account = lockAccount(accountId);
        ensureActive(account);
        BigDecimal newBalance = ((BigDecimal)account.get("balance")).add(amount);
        jdbc.update("UPDATE accounts SET balance=? WHERE account_id=?", newBalance, accountId);
        record(accountId, null, "DEPOSIT", amount, newBalance, description);
        return balanceResult(accountId, newBalance);
    }

    @Transactional
    public Map<String,Object> withdraw(long accountId, BigDecimal amount, String description) {
        validateAmount(amount);
        Map<String,Object> account = lockAccount(accountId);
        ensureActive(account);
        BigDecimal balance = (BigDecimal)account.get("balance");
        if (balance.compareTo(amount) < 0) throw new IllegalArgumentException("Insufficient balance.");
        BigDecimal newBalance = balance.subtract(amount);
        jdbc.update("UPDATE accounts SET balance=? WHERE account_id=?", newBalance, accountId);
        record(accountId, null, "WITHDRAWAL", amount, newBalance, description);
        return balanceResult(accountId, newBalance);
    }

    @Transactional
    public Map<String,Object> transfer(long fromId, long toId, BigDecimal amount, String description) {
        validateAmount(amount);
        if (fromId == toId) throw new IllegalArgumentException("Source and destination accounts must be different.");
        // Lock in ascending order to reduce deadlock risk.
        long first = Math.min(fromId,toId), second = Math.max(fromId,toId);
        Map<String,Object> firstAccount = lockAccount(first);
        Map<String,Object> secondAccount = lockAccount(second);
        Map<String,Object> from = fromId == first ? firstAccount : secondAccount;
        Map<String,Object> to = toId == first ? firstAccount : secondAccount;
        ensureActive(from); ensureActive(to);
        BigDecimal fromBalance = (BigDecimal)from.get("balance");
        BigDecimal toBalance = (BigDecimal)to.get("balance");
        if (fromBalance.compareTo(amount) < 0) throw new IllegalArgumentException("Insufficient balance.");
        BigDecimal fromNew = fromBalance.subtract(amount), toNew = toBalance.add(amount);
        jdbc.update("UPDATE accounts SET balance=? WHERE account_id=?", fromNew, fromId);
        jdbc.update("UPDATE accounts SET balance=? WHERE account_id=?", toNew, toId);
        record(fromId,toId,"TRANSFER_OUT",amount,fromNew,description == null ? "Transfer to account " + toId : description);
        record(toId,fromId,"TRANSFER_IN",amount,toNew,description == null ? "Transfer from account " + fromId : description);
        return Map.of("status","COMPLETED","fromAccountId",fromId,"toAccountId",toId,"amount",amount,"fromBalance",fromNew,"toBalance",toNew);
    }

    private Map<String,Object> lockAccount(long accountId) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT account_id,customer_id,balance,status FROM accounts WHERE account_id=? FOR UPDATE", accountId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Account not found.");
        return rows.get(0);
    }
    private void ensureActive(Map<String,Object> account) {
        if (!"ACTIVE".equals(String.valueOf(account.get("status")))) throw new IllegalArgumentException("Account is not active.");
    }
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || amount.scale() > 2) throw new IllegalArgumentException("Amount must be positive and have at most two decimal places.");
    }
    private void record(long accountId, Long related, String type, BigDecimal amount, BigDecimal balance, String description) {
        jdbc.update("INSERT INTO transactions(reference_code,account_id,related_account_id,transaction_type,amount,balance_after,description) VALUES(?,?,?,?,?,?,?)",
            "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0,20).toUpperCase(), accountId, related, type, amount, balance, description == null || description.isBlank() ? type.replace('_',' ') : description.trim());
    }
    private Map<String,Object> balanceResult(long accountId, BigDecimal balance) {
        return Map.of("status","COMPLETED","accountId",accountId,"balance",balance);
    }
}
