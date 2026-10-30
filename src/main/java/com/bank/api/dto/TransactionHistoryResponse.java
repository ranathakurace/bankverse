package com.bank.api.dto;

import java.util.List;
import lombok.Data;

@Data
public class TransactionHistoryResponse {

    private List<LedgerTransactionResponse> transactions;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
