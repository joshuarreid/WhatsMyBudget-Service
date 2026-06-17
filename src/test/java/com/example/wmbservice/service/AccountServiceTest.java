package com.example.wmbservice.service;

import com.example.wmbservice.model.Account;
import com.example.wmbservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository);
    }

    @Test
    void resolveByName_knownName_returnsAccount() {
        Account account = new Account(1L, "josh");
        when(accountRepository.findByAccountNameIgnoreCase("josh")).thenReturn(Optional.of(account));

        Account result = accountService.resolveByName("josh");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAccountName()).isEqualTo("josh");
    }

    @Test
    void resolveByName_caseInsensitive_returnsAccount() {
        Account account = new Account(2L, "joint");
        when(accountRepository.findByAccountNameIgnoreCase("joint")).thenReturn(Optional.of(account));

        Account result = accountService.resolveByName("JOINT");

        assertThat(result.getId()).isEqualTo(2L);
        verify(accountRepository).findByAccountNameIgnoreCase("joint");
    }

    @Test
    void resolveByName_withWhitespace_trimsBeforeLookup() {
        Account account = new Account(1L, "josh");
        when(accountRepository.findByAccountNameIgnoreCase("josh")).thenReturn(Optional.of(account));

        Account result = accountService.resolveByName("  Josh  ");

        assertThat(result.getId()).isEqualTo(1L);
        verify(accountRepository).findByAccountNameIgnoreCase("josh");
    }

    @Test
    void resolveByName_unknownName_throwsUnknownAccountException() {
        when(accountRepository.findByAccountNameIgnoreCase("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.resolveByName("unknown"))
                .isInstanceOf(AccountService.UnknownAccountException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void resolveByName_nullInput_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> accountService.resolveByName(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveByName_blankInput_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> accountService.resolveByName("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

