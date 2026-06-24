package com.bank.api.service;

import com.bank.api.model.Transaction;
import com.bank.api.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public List<Transaction> getTransactions(
            int accountId,
            int page,
            int size,
            String type) {

        List<Transaction> filtered = repository.findByAccountId(accountId);

        if (type != null) {
            filtered = filtered.stream()
                    .filter(t -> t.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        int start = page * size;
        int end = Math.min(start + size, filtered.size());

        if (start >= filtered.size()) {
            return List.of();
        }

        return filtered.subList(start, end);
    }
}
