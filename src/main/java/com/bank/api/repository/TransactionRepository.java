package com.bank.api.repository;

import com.bank.api.model.Transaction;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class TransactionRepository {

    private static final List<Transaction> transactions = new ArrayList<>();

    static {
        transactions.add(new Transaction(1, 1, "DEPOSIT", 500));
        transactions.add(new Transaction(2, 1, "WITHDRAW", 200));
        transactions.add(new Transaction(3, 1, "DEPOSIT", 1000));
        transactions.add(new Transaction(4, 1, "WITHDRAW", 300));
        transactions.add(new Transaction(5, 2, "DEPOSIT", 2000));
    }

    public List<Transaction> findByAccountId(int accountId) {
        return transactions.stream()
                .filter(t -> t.getAccountId() == accountId)
                .collect(Collectors.toList());
    }
}
