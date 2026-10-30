package com.bank.api.repository;
import com.bank.api.model.CustomerOnlineAccess;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CustomerOnlineAccessRepository extends JpaRepository<CustomerOnlineAccess, Long> { Optional<CustomerOnlineAccess> findByCustomer_CustomerNumber(String customerNumber); }
