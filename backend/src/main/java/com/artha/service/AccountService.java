package com.artha.service;

import com.artha.domain.Account;
import com.artha.domain.AccountStatus;
import com.artha.domain.Person;
import com.artha.dto.AccountRequest;
import com.artha.dto.AccountResponse;
import com.artha.repository.AccountRepository;
import com.artha.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * Service for Account lifecycle operations.
 * SPEC REF: FR-3.2
 */
@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;

    public AccountService(AccountRepository accountRepository, PersonRepository personRepository) {
        this.accountRepository = accountRepository;
        this.personRepository = personRepository;
    }

    public AccountResponse create(UUID personId, AccountRequest request) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Person not found: " + personId));

        Account account = new Account();
        account.setOwner(person);
        account.setInstitutionName(request.institutionName());
        account.setAccountNumberMasked(request.accountNumberMasked());
        account.setAccountType(request.accountType());
        account.setCurrency(request.currency() != null ? request.currency() : "INR");
        account.setStatus(AccountStatus.ACTIVE);

        return toResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(UUID id) {
        return toResponse(accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findByPersonId(UUID personId) {
        return accountRepository.findByOwnerId(personId).stream()
                .map(this::toResponse).toList();
    }

    public AccountResponse update(UUID id, AccountRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found: " + id));
        account.setInstitutionName(request.institutionName());
        account.setAccountNumberMasked(request.accountNumberMasked());
        account.setAccountType(request.accountType());
        if (request.currency() != null) account.setCurrency(request.currency());
        return toResponse(accountRepository.save(account));
    }

    public void delete(UUID id) {
        if (!accountRepository.existsById(id)) throw new RuntimeException("Account not found: " + id);
        accountRepository.deleteById(id);
    }

    private AccountResponse toResponse(Account a) {
        return new AccountResponse(a.getId(), a.getOwner().getId(), a.getInstitutionName(),
                a.getAccountNumberMasked(), a.getAccountType().name(), a.getCurrency(),
                a.getStatus().name(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
