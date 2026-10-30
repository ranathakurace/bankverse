package com.bank.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

import com.bank.api.dto.CardIssueRequest;
import com.bank.api.dto.CardResponse;
import com.bank.api.dto.CardStatusChangeRequest;
import com.bank.api.exception.CardWorkflowException;
import com.bank.api.model.Account;
import com.bank.api.model.AccountStatus;
import com.bank.api.model.BankCard;
import com.bank.api.model.CardAuditEvent;
import com.bank.api.model.CardStatus;
import com.bank.api.model.CardType;
import com.bank.api.model.Customer;
import com.bank.api.model.CustomerStatus;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.BankCardRepository;
import com.bank.api.repository.CardAuditEventRepository;
import com.bank.api.repository.CustomerRepository;
import java.util.Optional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CardServiceTest {
    private CustomerRepository customerRepository; private AccountRepository accountRepository;
    private BankCardRepository bankCardRepository; private CardAuditEventRepository cardAuditEventRepository; private CardService cardService;
    @BeforeMethod public void setUp() { customerRepository = mock(CustomerRepository.class); accountRepository = mock(AccountRepository.class); bankCardRepository = mock(BankCardRepository.class); cardAuditEventRepository = mock(CardAuditEventRepository.class); cardService = new CardService(customerRepository, accountRepository, bankCardRepository, cardAuditEventRepository); when(bankCardRepository.save(any(BankCard.class))).thenAnswer(i -> i.getArgument(0)); }
    @Test public void issuesMaskedCardForActiveCustomerAndAccount() {
        Customer customer = customer(); Account account = account(customer); when(customerRepository.findByCustomerNumber("CUST000003")).thenReturn(Optional.of(customer)); when(accountRepository.findByAccountNumber("ACC000101")).thenReturn(Optional.of(account));
        CardResponse response = cardService.issueCard(issueRequest());
        assertEquals(response.getStatus(), CardStatus.PENDING_ACTIVATION.name()); assertEquals(response.getMaskedCardNumber().length(), 19); verify(cardAuditEventRepository).save(any(CardAuditEvent.class));
    }
    @Test public void preventsCardForDifferentCustomerAccount() {
        Customer customer = customer(); Customer other = customer(); other.setId(4L); Account account = account(other); when(customerRepository.findByCustomerNumber("CUST000003")).thenReturn(Optional.of(customer)); when(accountRepository.findByAccountNumber("ACC000101")).thenReturn(Optional.of(account));
        expectThrows(CardWorkflowException.class, () -> cardService.issueCard(issueRequest()));
    }
    @Test public void activatesPendingCard() {
        BankCard card = card(CardStatus.PENDING_ACTIVATION); when(bankCardRepository.findByCardReference("CARD-1")).thenReturn(Optional.of(card));
        CardResponse response = cardService.activateCard("CARD-1", statusRequest("Activated by customer."));
        assertEquals(response.getStatus(), CardStatus.ACTIVE.name()); verify(cardAuditEventRepository).save(any(CardAuditEvent.class));
    }
    @Test public void blocksOnlyActiveCard() {
        BankCard card = card(CardStatus.ACTIVE); when(bankCardRepository.findByCardReference("CARD-1")).thenReturn(Optional.of(card));
        assertEquals(cardService.blockCard("CARD-1", statusRequest("Lost card.")).getStatus(), CardStatus.BLOCKED.name());
    }
    @Test public void preventsUnblockingCardThatIsNotBlocked() {
        BankCard card = card(CardStatus.ACTIVE); when(bankCardRepository.findByCardReference("CARD-1")).thenReturn(Optional.of(card));
        expectThrows(CardWorkflowException.class, () -> cardService.unblockCard("CARD-1", statusRequest("Incorrect state.")));
    }
    private Customer customer() { Customer customer = new Customer(); customer.setId(3L); customer.setCustomerNumber("CUST000003"); customer.setCustomerStatus(CustomerStatus.ACTIVE); return customer; }
    private Account account(Customer customer) { Account account = new Account(); account.setId(101L); account.setAccountNumber("ACC000101"); account.setCustomer(customer); account.setAccountStatus(AccountStatus.ACTIVE); return account; }
    private BankCard card(CardStatus status) { BankCard card = new BankCard(); card.setCardReference("CARD-1"); card.setLastFour("1234"); card.setCustomer(customer()); card.setLinkedAccount(account(card.getCustomer())); card.setType(CardType.DEBIT); card.setStatus(status); return card; }
    private CardIssueRequest issueRequest() { CardIssueRequest request = new CardIssueRequest(); request.setCustomerNumber("CUST000003"); request.setLinkedAccountNumber("ACC000101"); request.setCardType(CardType.DEBIT); return request; }
    private CardStatusChangeRequest statusRequest(String reason) { CardStatusChangeRequest request = new CardStatusChangeRequest(); request.setPerformedBy("OPS-4001"); request.setReason(reason); return request; }
}
