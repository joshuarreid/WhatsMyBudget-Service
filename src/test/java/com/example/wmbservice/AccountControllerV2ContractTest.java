package com.example.wmbservice;

import com.example.wmbservice.controller.AccountControllerV2;
import com.example.wmbservice.model.Account;
import com.example.wmbservice.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerV2ContractTest {

    @Mock
    private AccountService accountService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AccountControllerV2(accountService)).build();
    }

    @Test
    void getAccounts_returnsAllAccountsWithIdAndName() throws Exception {
        when(accountService.getAllAccounts()).thenReturn(List.of(
                new Account(1L, "josh"),
                new Account(2L, "anna"),
                new Account(3L, "joint")
        ));

        mockMvc.perform(get("/api/v2/accounts")
                        .header("X-Transaction-ID", "tx-acc"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Transaction-ID", "tx-acc"))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].accountName").value("josh"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].accountName").value("anna"))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[2].accountName").value("joint"));
    }

    @Test
    void getAccounts_emptyTable_returnsEmptyArray() throws Exception {
        when(accountService.getAllAccounts()).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

