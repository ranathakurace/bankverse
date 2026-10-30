package com.bank.api.service;

import com.bank.api.dto.CardIssueRequest;
import com.bank.api.dto.CardResponse;
import com.bank.api.dto.CardStatusChangeRequest;
import com.bank.api.exception.AccountNotFoundException;
import com.bank.api.exception.CardWorkflowException;
import com.bank.api.exception.CustomerNotFoundException;
import com.bank.api.exception.InvalidCardException;
import com.bank.api.model.Account;
import com.bank.api.model.AccountStatus;
import com.bank.api.model.BankCard;
import com.bank.api.model.CardAuditEvent;
import com.bank.api.model.CardStatus;
import com.bank.api.model.Customer;
import com.bank.api.model.CustomerStatus;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.BankCardRepository;
import com.bank.api.repository.CardAuditEventRepository;
import com.bank.api.repository.CustomerRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {
    private static final String CUSTOMER_NUMBER_REGEX = "^CUST\\d{6}$";
    private static final String ACCOUNT_NUMBER_REGEX = "^ACC\\d{6}$";
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final BankCardRepository bankCardRepository;
    private final CardAuditEventRepository cardAuditEventRepository;

    public CardService(CustomerRepository customerRepository, AccountRepository accountRepository,
            BankCardRepository bankCardRepository, CardAuditEventRepository cardAuditEventRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.bankCardRepository = bankCardRepository;
        this.cardAuditEventRepository = cardAuditEventRepository;
    }

    @Transactional
    public CardResponse issueCard(CardIssueRequest request) {
        if (request == null || request.getCardType() == null) {
            throw new InvalidCardException("Customer number, linked account number, and card type are mandatory.");
        }
        Customer customer = findCustomer(request.getCustomerNumber());
        if (customer.getCustomerStatus() != CustomerStatus.ACTIVE) {
            throw new CardWorkflowException("Only an ACTIVE customer can receive a card.");
        }
        Account account = findAccount(request.getLinkedAccountNumber());
        if (!account.getCustomer().getId().equals(customer.getId())) {
            throw new CardWorkflowException("Linked account does not belong to the customer.");
        }
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new CardWorkflowException("Card can only be linked to an ACTIVE account.");
        }
        LocalDateTime now = LocalDateTime.now();
        BankCard card = new BankCard();
        card.setCardReference("CARD-" + UUID.randomUUID());
        card.setLastFour(String.format("%04d", Math.floorMod(UUID.randomUUID().hashCode(), 10000)));
        card.setCustomer(customer);
        card.setLinkedAccount(account);
        card.setType(request.getCardType());
        card.setStatus(CardStatus.PENDING_ACTIVATION);
        card.setIssuedAt(now);
        BankCard savedCard = bankCardRepository.save(card);
        recordAudit(savedCard, "CARD_ISSUED", null, CardStatus.PENDING_ACTIVATION, "SYSTEM",
                "Card issued in pending activation state.", now);
        return toResponse(savedCard);
    }

    @Transactional
    public CardResponse activateCard(String cardReference, CardStatusChangeRequest request) {
        return changeStatus(cardReference, request, CardStatus.PENDING_ACTIVATION, CardStatus.ACTIVE,
                "CARD_ACTIVATED", false);
    }

    @Transactional
    public CardResponse blockCard(String cardReference, CardStatusChangeRequest request) {
        return changeStatus(cardReference, request, CardStatus.ACTIVE, CardStatus.BLOCKED, "CARD_BLOCKED", true);
    }

    @Transactional
    public CardResponse unblockCard(String cardReference, CardStatusChangeRequest request) {
        return changeStatus(cardReference, request, CardStatus.BLOCKED, CardStatus.ACTIVE, "CARD_UNBLOCKED", true);
    }

    @Transactional
    public CardResponse closeCard(String cardReference, CardStatusChangeRequest request) {
        BankCard card = findCard(cardReference);
        validateStatusRequest(request, true);
        if (card.getStatus() != CardStatus.ACTIVE && card.getStatus() != CardStatus.BLOCKED) {
            throw new CardWorkflowException("Only ACTIVE or BLOCKED cards can be closed.");
        }
        CardStatus previousStatus = card.getStatus();
        card.setStatus(CardStatus.CLOSED);
        bankCardRepository.save(card);
        recordAudit(card, "CARD_CLOSED", previousStatus, CardStatus.CLOSED, request.getPerformedBy().trim(),
                request.getReason().trim(), LocalDateTime.now());
        return toResponse(card);
    }

    @Transactional(readOnly = true)
    public CardResponse getCard(String cardReference) { return toResponse(findCard(cardReference)); }

    @Transactional(readOnly = true)
    public List<CardResponse> getCustomerCards(String customerNumber) {
        Customer customer = findCustomer(customerNumber);
        return bankCardRepository.findByCustomer_CustomerNumberOrderByIssuedAtDesc(customer.getCustomerNumber()).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    private CardResponse changeStatus(String reference, CardStatusChangeRequest request, CardStatus expected,
            CardStatus target, String action, boolean reasonMandatory) {
        BankCard card = findCard(reference);
        validateStatusRequest(request, reasonMandatory);
        if (card.getStatus() != expected) {
            throw new CardWorkflowException("Card must be in " + expected + " status for this operation.");
        }
        card.setStatus(target);
        if (target == CardStatus.ACTIVE && card.getActivatedAt() == null) { card.setActivatedAt(LocalDateTime.now()); }
        bankCardRepository.save(card);
        recordAudit(card, action, expected, target, request.getPerformedBy().trim(), trimOptional(request.getReason()), LocalDateTime.now());
        return toResponse(card);
    }

    private Customer findCustomer(String number) {
        String normalized = normalize(number, CUSTOMER_NUMBER_REGEX, "customer number");
        return customerRepository.findByCustomerNumber(normalized)
                .orElseThrow(() -> new CustomerNotFoundException("customer number", normalized));
    }
    private Account findAccount(String number) {
        String normalized = normalize(number, ACCOUNT_NUMBER_REGEX, "account number");
        return accountRepository.findByAccountNumber(normalized)
                .orElseThrow(() -> new AccountNotFoundException(normalized));
    }
    private BankCard findCard(String reference) {
        if (isBlank(reference)) { throw new InvalidCardException("Card reference is mandatory."); }
        return bankCardRepository.findByCardReference(reference.trim())
                .orElseThrow(() -> new CardWorkflowException("Card was not found."));
    }
    private void validateStatusRequest(CardStatusChangeRequest request, boolean reasonMandatory) {
        if (request == null || isBlank(request.getPerformedBy())) { throw new InvalidCardException("Performed by is mandatory."); }
        if (request.getPerformedBy().trim().length() > 100) { throw new InvalidCardException("Performed by cannot exceed 100 characters."); }
        if (reasonMandatory && isBlank(request.getReason())) { throw new InvalidCardException("Reason is mandatory for this action."); }
        if (request.getReason() != null && request.getReason().trim().length() > 500) { throw new InvalidCardException("Reason cannot exceed 500 characters."); }
    }
    private void recordAudit(BankCard card, String action, CardStatus previous, CardStatus target,
            String by, String reason, LocalDateTime now) {
        CardAuditEvent event = new CardAuditEvent(); event.setCard(card); event.setAction(action);
        event.setPreviousStatus(previous); event.setNewStatus(target); event.setPerformedBy(by);
        event.setReason(reason); event.setOccurredAt(now); cardAuditEventRepository.save(event);
    }
    private CardResponse toResponse(BankCard card) {
        CardResponse response = new CardResponse(); response.setCardReference(card.getCardReference());
        response.setMaskedCardNumber("**** **** **** " + card.getLastFour());
        response.setCustomerNumber(card.getCustomer().getCustomerNumber());
        response.setLinkedAccountNumber(card.getLinkedAccount().getAccountNumber());
        response.setCardType(card.getType().name()); response.setStatus(card.getStatus().name()); return response;
    }
    private String normalize(String value, String regex, String label) {
        if (isBlank(value)) { throw new InvalidCardException(label + " is mandatory."); }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches(regex)) { throw new InvalidCardException("Invalid " + label + " format."); }
        return normalized;
    }
    private String trimOptional(String value) { return value == null ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
